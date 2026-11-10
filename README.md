# QRify (native Android, Java)

Open this folder in Android Studio (Hedgehog or newer), let Gradle sync, run on a device.
minSdk 24, targetSdk 34, AGP 8.2.2, Java 8.

## Flow
Welcome -> Home (Photo / Video / Camera / Files) -> Customize QR -> QR Ready (Share, Save, Open)
Bottom nav: Home, My QR, [Scan], History, Settings.
Scan: camera (CameraX + ZXing), gallery QR image, QR Detected sheet -> Media Viewer (photo zoom / video player).

## Cross-phone QR (important)
Settings -> Cloud Storage: enter your Cloudinary cloud name and an UNSIGNED upload preset
(Cloudinary dashboard -> Settings -> Upload -> Upload presets -> Signing mode: Unsigned).
Media is uploaded and the QR stores only the secure URL, so any phone can open it.
Without it the app creates a "local QR" (qrify://local/<id>) that works only on the same phone.

## Reused drawables
Icons (ic_*.xml) and shape backgrounds (bg_card, bg_btn_primary, bg_icon_tile, bg_chip_on/off,
bg_qr_card, bg_outline_selected ...) are defined once in res/drawable and reused across screens.
