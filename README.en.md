# Galaxy Update Guard

[日本語](README.md) · English

A root app that disables Samsung Galaxy OS updates and restores their previous state. Google Play system updates and app updates are excluded.

## Requirements

Targets Android 16/17 Galaxy devices whose Samsung OS-update services are installed as system apps. Use it in the main user. Xposed and Vector are not required.

## Usage

1. Install the signed release APK from [Releases](https://github.com/OneUI-Mods/galaxy-update-guard/releases).
2. Open the app and grant permission in your root manager.
3. Enable **Block OS updates** to stop update services.
4. Disable it to restore the pre-block state saved by this app. Services already disabled by another method return to that prior disabled state.

## Restoring and limitations

The disabled state remains even after temporary root is lost. Root is required to restore it. Always restore the saved pre-block state before uninstalling the app or clearing its data.

This does not interrupt an update already being applied. Carrier variants using different update services, managed devices, and USB/recovery-based updates are excluded. There is no resident service, periodic polling, or internet access.

## Development and licenses

Build instructions, architecture, and verification are in the [developer guide (Japanese)](docs/development.md). Report problems through [Issues](https://github.com/OneUI-Mods/galaxy-update-guard/issues), including device, OS and target-app versions, and reproduction steps.

See [LICENSE](LICENSE) for the app's license and the in-app or bundled notices for dependencies. This is not an official Samsung app.

[OneUI Mods app catalog](https://github.com/OneUI-Mods)

## String compatibility

The previous handling of certain whitespace in license search and version display has been restored.
