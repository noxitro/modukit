# modukit
android用モジュール式ユーティリティ

## アプリ

| アプリ | 内容 |
| --- | --- |
| [おこして更新](apps/wake-update/) | Galaxy のディープスリープ中のアプリを一時的に起こして、Play ストアで更新できるようにする |

アプリは [nox-apk-manager](https://github.com/noxitro/nox-apk-manager) で配布します。
`pwsh scripts\publish.ps1 -App <アプリ名>` で、ビルドして Google Drive の `builds/<アプリ名>/` に置きます。

## ツール

| ツール | 内容 |
| --- | --- |
| [hibernation](tools/hibernation/) | 休止中のアプリが Play ストアで自動更新されない問題を直す（休止状態の一括解除・無効化） |
