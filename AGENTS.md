# AI エージェント向けのルール

Claude Code・GitHub Copilot・opencode など、このリポジトリで作業する AI エージェントに共通のルールです。
（Claude Code は `CLAUDE.md` から、Copilot は `.github/copilot-instructions.md` からこのルールを読みます）

## 言語

人が読む文章は、原則として日本語で書きます。

- PR のタイトルと本文
- レビューのコメントと返信、Issue とそのコメント
- コミットメッセージ（1 行目の要約も含む）
- コードのコメント、README などのドキュメント
- アプリの画面の文言（既定の `values/` は日本語）

次のものは英語のままでかまいません。

- クラス名・関数名・変数名などの識別子、コマンド、ファイルパス
- ログやエラーメッセージの引用、ライブラリや製品の名前
- 文字化けを避けるために英語にしている出力（`tools/hibernation/hibernation.sh` など）と、`values-en/` などの英語のリソース
- ツールが付ける定型の行（`Co-Authored-By:` などのトレーラー）

文体は、README と同じく「です・ます」で、短く具体的に書きます。
