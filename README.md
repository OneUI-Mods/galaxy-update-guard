# Galaxy Update Guard

Samsung GalaxyのOSアップデートを停止・復元するroot向けアプリです。Android 16・17に対応し、SDK 37でビルドしています。

「OSアップデートをブロック」をONにすると、SamsungのOS更新用サービスを停止します。OFFでは、このアプリが保存した停止前の状態を復元します。すでにほかの方法で無効になっていたサービスは、その無効状態へ戻します。Google Playシステム更新とアプリ更新は対象に含めません。

一時rootが外れても、パッケージの停止状態は維持されます。解除にはrootが必要です。アプリの削除・データ消去前に停止前の状態を復元してください。すでに適用中の更新を中断する機能ではありません。

対象はシステムアプリとして存在する `com.wssyncmldm` と `com.sec.android.soagent` です。メインユーザーで使ってください。別の更新サービスを使うキャリア版や管理端末、USB・リカバリー経由の更新は対象外です。Xposed、常駐サービス、定期ポーリング、インターネット接続は使用しません。

Material 3、ダーク・ライトテーマ、日本語・英語、検索可能なライセンス一覧を備えています。

開発にはJDK 21、Android SDK Platform 37.0、Build Tools 37.0.0が必要です。

```sh
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
python3 tools/package_release.py --source-ref HEAD
```

配布方法は[releasing.md](docs/releasing.md)、検証記録は[testing.md](docs/testing.md)を参照してください。
