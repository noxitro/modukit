# modukit
android用モジュール式ユーティリティ

## アプリ

| アプリ | 内容 |
| --- | --- |
| [おこして更新](apps/wake-update/) | Galaxy のディープスリープ中のアプリを一時的に起こして、Play ストアで更新できるようにする |

アプリは [nox-apk-manager](https://github.com/noxitro/nox-apk-manager) で配布します。
`main` に push すると GitHub Actions が Google Drive の `builds/<アプリ名>/` に置きます（Secrets の登録は各アプリの README）。
PC から置くときは `pwsh scripts\publish.ps1 -App <アプリ名>` です。

## ツール

| ツール | 内容 |
| --- | --- |
| [hibernation](tools/hibernation/) | 休止中のアプリが Play ストアで自動更新されない問題を直す（休止状態の一括解除・無効化） |

## ほかのリポジトリから配布する

nox-apk-manager に配布する GitHub Actions のワークフロー（[publish-apk](.github/actions/publish-apk/)）は、ほかのリポジトリからも呼べます。
呼び出し用のワークフローを 1 つ置き、`pwsh scripts\set-ci-secrets.ps1 -Repo <リポジトリ名>` で Secrets を登録するだけです。
