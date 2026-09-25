#!/system/bin/sh
# modukit: hibernation.sh
#
# Android 12 以降の「使用していないアプリの休止状態（App hibernation）」を
# ADB 権限でまとめて確認・解除し、今後も休止させないように設定します。
# 休止中のアプリは Play ストアの更新対象から外れるため、その対策用です。
#
#   check (既定)  休止中のアプリなどを表示するだけ（何も変更しない）
#   fix           休止中のアプリを起こし、アプリを自動休止の対象外にする
#                 （設定アプリで「使用していないアプリを一時停止する」を
#                   OFF にするのと同じ処理）
#   undo          fix で変更した設定を初期値に戻す
#
# 対象はランチャーに表示されるアプリ（休止状態になり得るアプリ）全部です。
# パッケージ名を指定すると、そのアプリだけを対象にします。
#
# 実行例（PC から）:
#   adb push hibernation.sh /data/local/tmp/
#   adb shell sh /data/local/tmp/hibernation.sh fix
#
# Windows のコンソールでも文字化けしないよう、出力は英語にしています。

OP=AUTO_REVOKE_PERMISSIONS_IF_UNUSED
nl='
'

usage() {
	printf '%s\n' \
		'Usage: sh hibernation.sh [check|fix|undo] [--user ID] [PACKAGE...]' \
		'  check      show hibernating apps (default, changes nothing)' \
		'  fix        wake hibernating apps and exempt apps from auto-hibernation' \
		'  undo       reset the auto-hibernation setting to the default' \
		'  --user ID  target user (default: current user)' \
		'  PACKAGE    only process the given packages (default: all launcher apps)'
}

die() {
	printf 'Error: %s\n' "$*" >&2
	exit 1
}

mode=check
user=
targets=
while [ $# -gt 0 ]; do
	case $1 in
	check | fix | undo) mode=$1 ;;
	--user)
		[ $# -ge 2 ] || die '--user needs a user id'
		case $2 in '' | *[!0-9]*) die "invalid user id: $2" ;; esac
		user=$2
		shift
		;;
	-h | --help)
		usage
		exit 0
		;;
	-*) die "unknown option: $1 (see --help)" ;;
	*) targets="$targets $1" ;;
	esac
	shift
done

# 通常アプリの権限では appops / app_hibernation を操作できない
case $(id -u 2>/dev/null) in
0 | 2000) ;;
*) die 'run this via "adb shell" (or rish / root)' ;;
esac

sdk=$(getprop ro.build.version.sdk)
case $sdk in '' | *[!0-9]*) die 'this script must run on an Android device' ;; esac
[ "$sdk" -ge 30 ] || die "Android 11 or later is required (SDK $sdk)"

if [ -z "$user" ]; then
	user=$(am get-current-user 2>/dev/null)
	case $user in '' | *[!0-9]*) user=0 ;; esac
fi

# Android 11 には休止状態がない（権限の自動削除だけ）
has_hib=0
case $(cmd app_hibernation get-state --user "$user" android 2>/dev/null) in
true* | false*) has_hib=1 ;;
esac

# ランチャーに表示されないアプリはシステムが休止の対象外にしているので、
# ランチャーに表示されるアプリ（プリインストールの YouTube なども含む）を
# 対象にする。設定アプリなど OS 本体の UID（appId < 10000）は除く。
list_packages() {
	apps=$(pm list packages -U --user "$user" 2>/dev/null |
		sed -n 's/^package:\([^[:space:]]*\) uid:\([0-9][0-9]*\).*/\1 \2/p' |
		while read -r name id; do
			[ $((id % 100000)) -ge 10000 ] && printf '%s\n' "$name"
		done)
	pm query-activities --components --user "$user" \
		-a android.intent.action.MAIN -c android.intent.category.LAUNCHER 2>/dev/null |
		sed -n 's#^\([^/[:space:]]*\)/.*#\1#p' | sort -u |
		while read -r name; do
			case "$nl$apps$nl" in *"$nl$name$nl"*) printf '%s\n' "$name" ;; esac
		done
}

# Galaxy の「ディープスリープ中のアプリ」は無効化状態として見える
list_disabled() {
	pm list packages -3 -d --user "$user" 2>/dev/null |
		sed -n 's/^package:\([^[:space:]]*\).*/\1/p' | sort
}

hibernating() { # $1: package, $2: "--global" で端末全体の休止状態を見る
	[ "$has_hib" = 1 ] || return 1
	if [ "${2-}" = --global ]; then
		state=$(cmd app_hibernation get-state --global "$1" 2>/dev/null)
	else
		state=$(cmd app_hibernation get-state --user "$user" "$1" 2>/dev/null)
	fi
	case $state in true*) return 0 ;; esac
	return 1
}

# uid モード → パッケージモードの順で出力されるので最初の行が実効値
op_mode() {
	appops get --user "$user" "$1" "$OP" 2>/dev/null |
		sed -n "s/.*$OP: \([a-z]*\).*/\1/p" | head -n 1
}

set -f # パッケージ名をグロブ展開させない
if [ -n "$targets" ]; then
	pkgs=$(for p in $targets; do printf '%s\n' "$p"; done)
else
	pkgs=$(list_packages)
	[ -n "$pkgs" ] || die 'no launcher apps found'
fi
count=$(printf '%s\n' "$pkgs" | grep -c .)

printf 'Device: %s %s, Android %s (SDK %s), user %s\n' \
	"$(getprop ro.product.manufacturer)" "$(getprop ro.product.model)" \
	"$(getprop ro.build.version.release)" "$sdk" "$user"
[ "$has_hib" = 1 ] ||
	printf 'Note: app hibernation is not available on this device.\n'

done_count=0
failed=0
hib_count=0
eligible=0
case $mode in
check)
	printf '\nHibernating apps (Play Store does not update these):\n'
	for p in $pkgs; do
		if hibernating "$p"; then
			printf '  %s\n' "$p"
			hib_count=$((hib_count + 1))
		elif hibernating "$p" --global; then
			printf '  %s (global only)\n' "$p"
			hib_count=$((hib_count + 1))
		fi
		# ignore（= 設定アプリで OFF）以外は自動休止の対象
		case $(op_mode "$p") in
		allow | default | '') eligible=$((eligible + 1)) ;;
		esac
	done
	[ "$hib_count" -gt 0 ] || printf '  (none)\n'
	printf '\n%s of %s app(s) can still be hibernated automatically.\n' \
		"$eligible" "$count"
	if [ "$hib_count" -gt 0 ] || [ "$eligible" -gt 0 ]; then
		printf 'Run "sh %s fix" to wake them up and stop auto-hibernation.\n' "$0"
	fi
	;;
fix)
	printf 'Processing %s app(s)...\n' "$count"
	for p in $pkgs; do
		# 設定アプリの「使用していないアプリを一時停止する」を OFF にしたときと
		# 同じく、uid モードを ignore にしてから休止状態を解除する
		if ! err=$(appops set --user "$user" --uid "$p" "$OP" ignore 2>&1); then
			printf 'Failed: %s %s\n' "$p" "$err"
			failed=$((failed + 1))
			continue
		fi
		done_count=$((done_count + 1))
		woke=0
		if hibernating "$p"; then
			cmd app_hibernation set-state --user "$user" "$p" false && woke=1
		fi
		if hibernating "$p" --global; then
			cmd app_hibernation set-state --global "$p" false && woke=1
		fi
		if [ "$woke" = 1 ]; then
			printf 'Woke up: %s\n' "$p"
			hib_count=$((hib_count + 1))
		fi
	done
	appops write-settings >/dev/null 2>&1
	printf '\nExempted %s app(s) from auto-hibernation, woke up %s app(s).\n' \
		"$done_count" "$hib_count"
	[ "$failed" -eq 0 ] || printf 'Failed for %s app(s).\n' "$failed"
	printf '%s\n' \
		'Next: Play Store > profile icon > Manage apps & device > Check for updates.' \
		'Apps installed later are not exempted; run "fix" again after installing apps.'
	;;
undo)
	for p in $pkgs; do
		if err=$(appops set --user "$user" --uid "$p" "$OP" default 2>&1); then
			done_count=$((done_count + 1))
		else
			printf 'Failed: %s %s\n' "$p" "$err"
			failed=$((failed + 1))
		fi
	done
	appops write-settings >/dev/null 2>&1
	printf 'Reset the auto-hibernation setting of %s app(s) to the default.\n' \
		"$done_count"
	[ "$failed" -eq 0 ] || printf 'Failed for %s app(s).\n' "$failed"
	;;
esac

if [ "$mode" != undo ] && [ -z "$targets" ]; then
	disabled=$(list_disabled)
	if [ -n "$disabled" ]; then
		printf '\nDisabled user apps (Play Store does not update these either):\n'
		for p in $disabled; do printf '  %s\n' "$p"; done
		printf '%s\n' \
			'On Galaxy these are "Deep sleeping apps". Remove them in Settings >' \
			'Battery > Background usage limits > Deep sleeping apps.'
	fi
fi

[ "$failed" -eq 0 ]
