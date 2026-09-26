# publish-apk: GitHub Actions から nox-apk-manager に配布する

Android アプリをテストしてビルドし、Google Drive の `builds/<project>/` に置く共通のワークフローです。
modukit のアプリのほか、ほかのリポジトリからも呼べます（modukit は公開リポジトリなので、非公開のリポジトリからも使えます）。

置くものは PC の `publish-apk.ps1` と同じ 3 つ（APK・`meta.json`・`icon.png`）です。
あとは端末の [nox-apk-manager](https://github.com/noxitro/nox-apk-manager) で「全て更新」を押すだけです。

| ファイル | 役割 |
| --- | --- |
| [`.github/workflows/publish-apk.yml`](../../workflows/publish-apk.yml) | テストと release ビルド（PC と同じ debug 鍵で署名）のジョブと、Drive に置くジョブ。ふだんはこれを呼ぶ |
| [`action.yml`](action.yml) | ビルド済みの APK を Drive に置くところだけ。独自の手順でビルドするときに使う |
| [`publish_drive.py`](publish_drive.py) | `meta.json` の更新と rclone でのアップロード |
| [`scripts/set-ci-secrets.ps1`](../../../scripts/set-ci-secrets.ps1) | Secrets を登録する PC 用のスクリプト |

## ほかのリポジトリで使う

### 1. アプリの準備

nox-apk-manager で入れるための決まりは、PC から配布するときと同じです。

- release も debug 鍵で署名する（端末に入っている版と同じ鍵でないと上書きできない）

  ```kotlin
  android {
      buildTypes {
          release {
              signingConfig = signingConfigs.getByName("debug")
          }
      }
  }
  ```

- 配布のたびに `versionCode` を上げる（Drive にある版より大きいときだけ置く）
- 一覧に出る名前は、APK の既定のアプリ名（`values/strings.xml` の `app_name`）

### 2. Secrets を登録する（PC で 1 回）

modukit のフォルダで次を実行します。ブラウザが開くので、`builds/` のある Google アカウントで rclone を許可します。

```powershell
winget install GitHub.cli     # 初回だけ。そのあと gh auth login
winget install Rclone.Rclone  # 初回だけ
pwsh scripts\set-ci-secrets.ps1 -Repo <リポジトリ名>
```

次の 2 つが登録されます。値は画面にもファイルにも出ません。

| Secret | 中身 |
| --- | --- |
| `DEBUG_KEYSTORE_BASE64` | この PC の `~/.android/debug.keystore` を Base64 にしたもの |
| `RCLONE_DRIVE_TOKEN` | あなたの Google アカウントとして Drive に書き込むトークン |

`-Repo modukit, <リポジトリ名>` のように並べると、1 つのトークンをまとめて入れます。トークンを入れ直すときも同じです。

<details>
<summary>スクリプトを使わずに登録する</summary>

リポジトリの Settings → Secrets and variables → Actions → New repository secret で登録します。

- `DEBUG_KEYSTORE_BASE64`: PowerShell で次を実行すると、クリップボードに入ります

  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore")) | Set-Clipboard
  ```

- `RCLONE_DRIVE_TOKEN`: `rclone authorize "drive"` を実行し、ブラウザで許可します。ターミナルの `Paste the following into your remote machine --->` と `<---End paste` の間に出る `{"access_token":...}` の 1 行を貼り付けます

</details>

### 3. ワークフローを置く

`.github/workflows/cd.yml` を作ります。`project` は `builds/` 直下のフォルダ名です。

```yaml
name: CD

on:
  push:
    branches: [main]
  workflow_dispatch:
    inputs:
      force:
        description: 同じ versionCode の版が Drive にあっても置き直す
        type: boolean
        default: false

permissions:
  contents: read

jobs:
  publish:
    uses: noxitro/modukit/.github/workflows/publish-apk.yml@main
    with:
      project: <アプリ名>
      gradle-tasks: ":app:testDebugUnitTest :app:assembleRelease"
      apk: app/build/outputs/apk/release/app-release.apk
      force: ${{ inputs.force == true }}
    secrets:
      DEBUG_KEYSTORE_BASE64: ${{ secrets.DEBUG_KEYSTORE_BASE64 }}
      RCLONE_DRIVE_TOKEN: ${{ secrets.RCLONE_DRIVE_TOKEN }}
```

`versionCode` を上げて `main` に push すると置かれます。同じ版を置き直したいときは、Actions の「Run workflow」で「置き直す」にチェックします。

### 入力

| 入力 | 既定 | 内容 |
| --- | --- | --- |
| `project` | （必須） | `builds/` 直下のフォルダ名。APK のファイル名 `<project>-<versionName>-<variant>.apk` にも使う |
| `gradle-tasks` | （必須） | テストとビルドの Gradle タスク（スペース区切り） |
| `apk` | （必須） | ビルドした APK のパス（リポジトリのルートから） |
| `variant` | `release` | `release` か `debug` |
| `description-file` | なし | 一覧に出す説明文（UTF-8 のテキスト）。省くと Drive にある説明を残す |
| `icon` | なし | 一覧に出すアイコン（512px くらいの PNG。manager が角丸に切り抜く） |
| `sdk-packages` | なし | ビルドの前に入れる Android SDK のパッケージ（例 `platforms;android-37.2`） |
| `java-version` | `21` | ビルドに使う JDK |
| `signing-cert-sha256` | なし | 端末の版の署名の SHA-256。Secrets の鍵がこれと違えば、置く前に止める |
| `drive-folder` | `builds` | マイドライブから見た `builds/` の場所 |
| `force` | `false` | 同じ versionCode の版が Drive にあっても置き直す |

`signing-cert-sha256` は、リポジトリの Variables に `SIGNING_CERT_SHA256` として登録して `${{ vars.SIGNING_CERT_SHA256 }}` を渡すと便利です。値は `keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android` の SHA256 です。

### 独自の鍵で署名するアプリ

book-log のように release 専用の鍵で署名するアプリは、ビルドと署名を自分のジョブで行い、APK を別のジョブに渡して Action だけを呼びます。
トークンをビルドと同じジョブに入れないためです（下の「トークンについて」）。
ランナーは `ubuntu-latest`（Android SDK・Java・Python 3 が入っている）を使います。

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      # ...ビルドと署名...
      - uses: actions/upload-artifact@v7
        with:
          name: apk
          path: app/build/outputs/apk/release/app-release.apk
          retention-days: 1

  publish:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v8
        with:
          name: apk
      - uses: noxitro/modukit/.github/actions/publish-apk@main
        with:
          project: book-log
          apk: app-release.apk
          drive-token: ${{ secrets.RCLONE_DRIVE_TOKEN }}
```

## 仕組みと注意

- 置く前に、APK の署名と Secrets の鍵が一致するか、Drive の `meta.json` と同じ package か、versionCode が上がっているかを確かめます
- 過去の版の APK は消さずに残します（manager から古い版も入れ直せます）
- Actions の実行時間は、公開リポジトリなら無料です。非公開リポジトリでは無料枠（Free プランは月 2,000 分、アカウント全体で共有）から引かれます

### トークンについて

nox-apk-manager が Drive を読むのに使っているサービスアカウントは、マイドライブにファイルを作れません（容量を持たないので、アップロードが 403 になります）。
そのため、書き込みはあなたの Google アカウントとして行います。

- rclone の既定のアプリを使うので、テスト中の自作 OAuth アプリのようにトークンが 7 日で切れることはありません
- 6 か月使わないと失効します。同じトークンを使い回していれば、どれかのリポジトリで 6 か月以内に 1 回配布するだけで保たれます。失効したら `set-ci-secrets.ps1` に配布しているリポジトリを並べて実行し直します
- 既にある `builds/` フォルダに書き込むため、トークンは Drive 全体を読み書きできます。Secrets 以外には置かないでください
- トークンを使うのは、ビルドとは別のジョブ（リポジトリのコードを動かさず、ビルド済みの APK を Drive に置くだけのジョブ）です。Gradle のビルドやテスト（外部のプラグインが動く部分）からは見えません。同じジョブだと、ビルド中に動いたコードが `$GITHUB_ENV` などを書き換えて、あとのステップからトークンを盗めるためです
- 呼び出す側は、上の例のように `main` への push と手動実行だけで動かします（プルリクエストでは動かさない）
- 漏れた場合は、Google アカウントの「セキュリティ」→「サードパーティ製のアプリとサービス」から rclone のアクセスを削除すると無効になります。そのあと `set-ci-secrets.ps1` で入れ直します
