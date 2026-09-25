#!/usr/bin/env python3
"""modukit のアプリを Google Drive の builds/<app>/ に置く（GitHub Actions の CD 用）。

nox-apk-manager の scripts/publish-apk.ps1 と同じ規約で置く:

    builds/<app>/<app>-<versionName>-<variant>.apk
    builds/<app>/meta.json   … package 名・版・variant・sha256・説明
    builds/<app>/icon.png    … 任意。未インストールの行に出す

publish-apk.ps1 はフォルダの APK を全部読み直して meta.json を作るが、CI では古い APK を
ダウンロードしたくないので、Drive にある meta.json に新しい版を足して書き直す。
Drive へのアクセスは rclone（リモートの場所は --remote か環境変数 NOX_DRIVE_BUILDS）。
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]


def fail(message: str) -> None:
    print(f"::error::{message}", file=sys.stderr)
    sys.exit(1)


def find_aapt2() -> str:
    if os.environ.get("AAPT2"):
        return os.environ["AAPT2"]
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        fail("ANDROID_HOME が設定されていない（aapt2 を探せない）")
    tools = sorted(
        (p for p in (Path(sdk) / "build-tools").iterdir() if (p / "aapt2").exists()),
        key=lambda p: [int(x) if x.isdigit() else 0 for x in re.split(r"[.-]", p.name)],
    )
    if not tools:
        fail(f"aapt2 が build-tools に無い: {sdk}")
    return str(tools[-1] / "aapt2")


def read_badging(apk: Path) -> dict:
    """APK 自身から package 名・版・既定のアプリ名を読む（引数で嘘を書けない）。"""
    out = subprocess.run(
        [find_aapt2(), "dump", "badging", str(apk)], capture_output=True, text=True, encoding="utf-8"
    )
    if out.returncode != 0:
        fail(f"aapt2 dump badging に失敗: {apk}\n{out.stderr}")
    package = next((line for line in out.stdout.splitlines() if line.startswith("package:")), None)
    if package is None:
        fail(f"package 行が無い: {apk}")

    def attr(key: str) -> str | None:
        m = re.search(rf"\b{key}='([^']*)'", package)
        return m.group(1) if m else None

    label = re.search(r"^application-label:'([^']*)'", out.stdout, re.MULTILINE)
    return {
        "packageName": attr("name"),
        "versionCode": int(attr("versionCode") or 0),
        "versionName": attr("versionName"),
        "label": label.group(1) if label else None,
    }


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def rclone(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    cmd = [os.environ.get("RCLONE", "rclone"), *args]
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    if check and result.returncode != 0:
        fail(f"rclone {' '.join(args)} に失敗:\n{result.stderr}")
    return result


def list_remote(remote_dir: str) -> set[str]:
    """フォルダのファイル名。まだフォルダが無ければ空。"""
    result = rclone("lsjson", "--files-only", "--no-modtime", "--no-mimetype", remote_dir, check=False)
    if result.returncode == 3:  # directory not found
        return set()
    if result.returncode != 0:
        fail(f"Drive のフォルダを読めない: {remote_dir}\n{result.stderr}")
    return {entry["Name"] for entry in json.loads(result.stdout or "[]")}


def iso(timestamp: float | None = None) -> str:
    when = dt.datetime.fromtimestamp(timestamp, dt.timezone.utc) if timestamp else dt.datetime.now(dt.timezone.utc)
    return when.isoformat()


def build_meta(app: str, info: dict, entry: dict, existing: dict | None, remote_files: set[str], description: str | None) -> dict:
    """既存の meta.json に新しいビルドを足す。Drive から消えたファイルの行は落とす。"""
    old_builds = (existing or {}).get("builds") or []
    builds = [b for b in old_builds if b.get("file") in remote_files and b.get("file") != entry["file"]]
    builds.append(entry)
    builds.sort(key=lambda b: b["file"].lower())

    known = {b["file"] for b in builds}
    for name in sorted(remote_files):
        if name.lower().endswith(".apk") and name not in known:
            # manager はファイル名から版を推定して一覧に出すので、ここでは載せないだけにする
            print(f"::warning::meta.json に無い APK は載せない（ファイル名から推定される）: {name}")

    return {
        "schema": 1,
        "project": app,
        "packageName": info["packageName"],
        "label": info["label"] or (existing or {}).get("label"),
        "description": description if description else (existing or {}).get("description") or "",
        "builds": builds,
        "updatedAt": iso(),
    }


def summary(lines: list[str]) -> None:
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--app", default="wake-update", help="apps/ のフォルダ名（= builds/ の project 名）")
    parser.add_argument("--variant", default="release", choices=["release", "debug"])
    parser.add_argument("--remote", default=os.environ.get("NOX_DRIVE_BUILDS", "gdrive:builds"),
                        help="rclone で見た builds/ の場所（既定: gdrive:builds）")
    parser.add_argument("--force", action="store_true", help="同じ versionCode の版が Drive にあっても置き直す")
    parser.add_argument("--dry-run", action="store_true", help="meta.json を表示するだけで Drive には書かない")
    args = parser.parse_args()

    app_dir = REPO / "apps" / args.app
    apk = app_dir / "build" / "outputs" / "apk" / args.variant / f"{args.app}-{args.variant}.apk"
    if not apk.exists():
        fail(f"APK が無い（先にビルドする）: {apk}")

    info = read_badging(apk)
    dest_name = f"{args.app}-{info['versionName']}-{args.variant}.apk"
    remote_dir = f"{args.remote.rstrip('/')}/{args.app}"
    remote_files = list_remote(remote_dir)

    existing = None
    if "meta.json" in remote_files:
        text = rclone("cat", f"{remote_dir}/meta.json").stdout
        try:
            existing = json.loads(text)
        except json.JSONDecodeError:
            print("::warning::Drive の meta.json が壊れているので作り直す")

    if existing and existing.get("packageName") not in (None, info["packageName"]):
        fail(f"同じフォルダに別 package の APK は置けない: {existing.get('packageName')} / {info['packageName']}")

    published = [
        b for b in (existing or {}).get("builds") or []
        if b.get("variant") == args.variant and b.get("file") in remote_files
        and int(b.get("versionCode") or 0) >= info["versionCode"]
    ]
    if published and not args.force:
        latest = max(published, key=lambda b: int(b.get("versionCode") or 0))
        message = (f"{args.app} {info['versionName']}（versionCode {info['versionCode']}）は置かない: "
                   f"Drive に versionCode {latest['versionCode']} の {latest['file']} がある。"
                   "配布するときは build.gradle.kts の versionCode を上げる")
        print(message)
        summary([f"### {args.app}: 置かなかった", "", message])
        return 0

    entry = {
        "file": dest_name,
        "variant": args.variant,
        "versionName": info["versionName"],
        "versionCode": info["versionCode"],
        "size": apk.stat().st_size,
        "sha256": sha256(apk),
        "builtAt": iso(apk.stat().st_mtime),
    }
    description_file = app_dir / "distribution" / "description.txt"
    description = description_file.read_text(encoding="utf-8").strip() if description_file.exists() else None
    meta = build_meta(args.app, info, entry, existing, remote_files | {dest_name}, description)
    meta_text = json.dumps(meta, ensure_ascii=False, indent=2) + "\n"

    if args.dry_run:
        print(meta_text)
        return 0

    # APK を先に置く（meta.json が無いファイルを指さないように）
    rclone("copyto", str(apk), f"{remote_dir}/{dest_name}")
    with tempfile.TemporaryDirectory() as tmp:
        meta_path = Path(tmp) / "meta.json"
        meta_path.write_text(meta_text, encoding="utf-8")  # BOM 無し（manager は BOM があると読めない）
        rclone("copyto", str(meta_path), f"{remote_dir}/meta.json")
    icon = app_dir / "distribution" / "icon.png"
    if icon.exists():
        rclone("copyto", "--checksum", str(icon), f"{remote_dir}/icon.png")

    print(f"placed: {remote_dir}/{dest_name} ({info['packageName']} v{info['versionName']} code {info['versionCode']})")
    summary([
        f"### {args.app}: {info['versionName']}（versionCode {info['versionCode']}）を置いた",
        "",
        f"- `{remote_dir}/{dest_name}`（{entry['size'] / 1_000_000:.1f} MB）",
        f"- sha256 `{entry['sha256']}`",
        "- nox-apk-manager を開くと更新として出る",
    ])
    return 0


if __name__ == "__main__":
    sys.exit(main())
