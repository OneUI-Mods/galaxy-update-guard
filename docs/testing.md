# 開発・検証記録

SDK 37、minSdk 36、AGP 9.3.2、Gradle 9.6.1、JDK 21。現在はAndroid 16・17のSamsung、user 0、OS更新用2パッケージのシステムアプリ識別を入場条件にしています。

停止前の0〜4の有効状態をdevice-protected SharedPreferencesへ同期commitし、保存失敗時は変更しません。停止・解除後にPackageManagerを読み直します。停止失敗時は全対象の復元を試み、復元未完了ならジャーナルを残します。プロセス終了・回転でも元の状態を失わず、復元の再試行ができます。シェル入力は固定パッケージと列挙済みコマンドだけで構成します。

12件のJVM試験で元の状態への復元、外部無効の保持、連続ON、途中失敗のrollback、保存失敗、復元失敗・再試行、プロセス終了相当、未管理状態、検証不一致、不正パッケージ・コマンド拒否、全有効状態、対象欠落を確認しました。release Lintとreleaseビルドも通過しています。

実機で確認した更新パッケージは `com.wssyncmldm` 4.6.20と `com.sec.android.soagent` 7.8.01です。`com.samsung.android.app.updatecenter` と `com.samsung.android.sdm.config` は変更対象から外しています。実機の変更試験と復元結果は追記します。

参考：[SamsungのFOTA管理におけるパッケージ名](https://docs.samsungknox.com/admin/knox-manage/kbas/kba-640-knox-efota-to-kiosk-devices-deployed-by-knox-manage/)、[AOSP PackageManager](https://android.googlesource.com/platform/frameworks/base/+/android17-release/core/java/android/content/pm/PackageManager.java)。端末APKとPackageManagerで更新用Activity・権限・システムアプリ属性を照合しました。

## Android 17実機（2026-10-21）

署名済みrelease APKをSM-S948Qへインストールし、M3の主画面を目視しました。ブロックでは更新用2パッケージがenabled=3、復元では元のenabled=0へ戻りました。停止中にアプリをforce-stopして開き直しても、OS側の停止状態と保存ジャーナルを保持しており復元できました。最終状態はブロックONです。Google Play module metadata / config updater、Samsung AppUpdateCenter / SDMConfigはいずれも元のenabled=0を維持しています。実際のアップデート要求やダウンロードは発行していません。端末・SystemUI・ホームの再起動は行っていません。Android 16実機の再試験は未実施です。
