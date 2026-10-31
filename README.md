# Galaxy Update Guard

Samsung GalaxyのOSアップデートを停止し、元の状態へ復元するroot向けアプリです。Google Playシステム更新とアプリ更新は対象に含めません。

## 対応環境

Android 16・17のGalaxyで、SamsungのOS更新用サービスがシステムアプリとして存在する環境が対象です。メインユーザーで使用してください。XposedやVectorは不要です。

## 使い方

1. [Releases](https://github.com/OneUI-Mods/galaxy-update-guard/releases)から署名済みrelease APKをインストールします。
2. アプリを開き、root管理アプリで権限を許可します。
3. 「OSアップデートをブロック」をONにすると更新用サービスを停止します。
4. OFFにすると、このアプリが保存した停止前の状態へ復元します。元から別の方法で無効になっていたサービスは、その状態へ戻ります。

## 解除・注意点

一時rootが外れても停止状態は維持されます。解除にはrootが必要です。アプリの削除やデータ消去の前に、必ず停止前の状態へ復元してください。

すでに適用中の更新を中断する機能ではありません。異なる更新サービスを使うキャリア版、管理端末、USB・リカバリー経由の更新は対象外です。常駐サービスや定期ポーリング、インターネット接続は使用しません。

## 開発者向け・ライセンス

ビルド、内部構造、検証方法は[開発者向けガイド](docs/development.md)に分けています。不具合報告は[Issues](https://github.com/OneUI-Mods/galaxy-update-guard/issues)へ、端末・OS・対象アプリの版と再現手順を添えてください。

本体のライセンスは[LICENSE](LICENSE)、依存ライブラリの表記はアプリ内または同梱の第三者ライセンスを参照してください。Samsungの公式アプリではありません。

[OneUI Modsのアプリ一覧](https://github.com/OneUI-Mods)

## 文字列の互換性

ライセンス一覧の検索・バージョン表示で、一部の空白文字の扱いを従来どおりに戻しました。
