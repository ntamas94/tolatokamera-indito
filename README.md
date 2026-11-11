# Tolatókamera indító (FYT UIS7870)

Kis Android-alkalmazás FYT UIS7870-es fejegységekhez (pl. Junsun V3 Plus), amelyeken a rendszer- vagy MCU-frissítés után **nem kapcsol be magától a tolatókamera**, pedig a kamera képe megvan (a gyári felvevőben kézzel előhozható).

> 🇬🇧 Small helper app for FYT UIS7870 head units (e.g. Junsun V3 Plus) where the reverse camera no longer pops up automatically after a firmware update, although the camera image itself works. It watches the system log for the rear camera's video signal and brings up the stock recorder's rear view; when the signal disappears it returns to the home screen.

## Hogyan működik

- A tolatókamera a tolatólámpáról kap áramot, így hátramenetben megjelenik a videójele.
- A fejegység ezt naplózza (`Chip6854 … index : 3 … hasSignal:true` / `zl: ==Back==U_SIGNAL_ON===ints[0]=1`).
- Az alkalmazás háttérszolgáltatása figyeli ezeket a sorokat, és jelre előhozza a gyári felvevő (`com.syu.doublecamera`) nézetét. A jel megszűnésekor visszavisz a főképernyőre.
- A fejegység bekapcsolásakor magától elindul.

A 14-es kamerát a gyártó rendszere csak a saját alkalmazásainak engedi megnyitni, ezért az alkalmazás nem saját képet mutat, hanem a gyári felvevőt hozza előre.

## Feltételek

- FYT UIS7870 fejegység, Android 13, gyári felvevővel (`com.syu.doublecamera`).
- Tolatólámpáról táplált tolatókamera (a jel csak hátramenetben jelenik meg).
- A gyári felvevőben a **hátsó kamera nézet** legyen kiválasztva (a felvevő megjegyzi).
- Számítógép ADB-vel (Wi-Fi-s hibakeresés), mert a naplóolvasási engedélyt csak ADB-n lehet megadni.

## Telepítés

```bash
adb install TolatoIndito.apk
adb shell pm grant com.tolato.indito android.permission.READ_LOGS
adb shell pm grant com.tolato.indito android.permission.CAMERA
adb shell appops set com.tolato.indito SYSTEM_ALERT_WINDOW allow
adb shell am start -n com.tolato.indito/.MainActivity
```

Eltávolítás: `adb uninstall com.tolato.indito`

## Fordítás (Windows, Gradle nélkül)

A `build.ps1` a JDK 17-et, az Android build-tools 34-et és az Android 13 platformot (`android.jar`) a szomszédos `android-build` mappából várja, és aapt2 + javac + d8 + apksigner lánccal építi az APK-t. Az első futáskor saját aláírókulcsot generál (`tolato.keystore`, ez nincs a repóban).

## Korlátok

- A tolatókép kb. 1–2 másodperccel később jelenik meg, mint rendesen bekötött tolatásjelnél.
- Rendszerfrissítés megváltoztathatja a naplósorokat vagy a felvevő alkalmazást.
- Ha a fejegység tolatásérzékelése rendben működik, nincs szükség rá.

Saját felelősségre használd. Nem hivatalos, a FYT-hez vagy a Junsunhoz nincs köze.
