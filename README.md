# Tolatókamera indító (FYT UIS7870)

Kis Android-alkalmazás FYT UIS7870-es fejegységekhez (pl. Junsun V3 Plus), amelyeken a rendszer- vagy MCU-frissítés után **nem kapcsol be magától a tolatókamera**, pedig a kamera képe megvan (a gyári felvevőben kézzel előhozható).

> 🇬🇧 Small helper app for FYT UIS7870 head units (e.g. Junsun V3 Plus) where the reverse camera no longer pops up automatically after a firmware update, although the camera image itself works. It detects the rear camera's video signal and brings up the stock recorder's rear view; when the signal disappears it returns to the home screen.

## Miért kell

A 2025–2026-os FYT 7870 frissítések után sok egységen a fejegység **már nem a fizikai tolatásvezetékről várja a hátramenet-jelet**, ezért a tolatókamera nem jön fel magától, hiába jó a kép. Ez szoftveres regresszió, a bekötés cseréje nem segít rajta. Ez az app megkerüli: a **kamera videójelére** indul, ami hátramenetben mindig megjelenik.

## Hogyan működik

- A tolatókamera a tolatólámpáról kap áramot, így hátramenetben megjelenik a videójele.
- Az app háttérszolgáltatása figyeli a rendszernaplót (`Chip6854 … hasSignal:true` / `zl: ==Back==U_SIGNAL_ON===ints[0]=1`), és jelre előhozza a gyári felvevő (`com.syu.doublecamera`) hátsó nézetét. A jel megszűnésekor visszavisz a főképernyőre.
- Ha a felvevő az első kamerával jönne fel, egy kisegítő szolgáltatás átvált a hátsóra (a felvevő „kamera megfordítása” gombjával).
- A naplóhozzáférési ablakot (`Allow … to access all device logs?`) a kisegítő szolgáltatás magától elfogadja — de csak ennek az appnak a kérésére.
- **Rootolt** készüléken az app rootként olvassa a naplót, így napló-hozzáférési ablak sincs, és auto-indítás után is azonnal működik.
- A fejegység bekapcsolásakor magától elindul.

A 14-es (tolató) kamerát a gyártó rendszere csak a saját alkalmazásainak engedi megnyitni, ezért az app nem saját képet mutat, hanem a gyári felvevőt hozza elő.

## Feltételek

- FYT UIS7870 fejegység, Android 13, gyári felvevővel (`com.syu.doublecamera`).
- Tolatólámpáról táplált tolatókamera (a jel csak hátramenetben jelenik meg).
- A gyári felvevőben a **hátsó kamera nézet** legyen kiválasztva (a felvevő megjegyzi).
- Számítógép ADB-vel (Wi-Fi-s hibakeresés), mert a naplóolvasási engedélyt csak ADB-n lehet megadni.

## Telepítés

Az APK a [Releases](../../releases) oldalon van.

```
adb install TolatoIndito.apk
adb shell pm grant com.tolato.indito android.permission.READ_LOGS
adb shell appops set com.tolato.indito SYSTEM_ALERT_WINDOW allow
adb shell am start -n com.tolato.indito/.MainActivity
```

Utána a fejegységen:

1. **Beállítások → General → Personal settings → Automatic start settings**: kapcsold be a „Tolatókamera indító”-t, különben a FYT-rendszer nem engedi autóindításkor elindulni.
2. **Beállítások → Kisegítő lehetőségek**: kapcsold be a „Tolatókamera indító” kisegítő szolgáltatást (ez fogadja el a naplóhozzáférési ablakot és vált hátsó kamerára).
3. Ha felugrik az Android „access all device logs” ablaka, válaszd az **„Allow one-time access”**-t. (A kisegítő szolgáltatás ezt automatikusan megnyomja.)

Eltávolítás: `adb uninstall com.tolato.indito`

## Root esetén (opcionális, de a legkényelmesebb)

Magisk-kal az app a naplót rootként olvassa, így napló-hozzáférési ablak sincs. A Magiskban add meg az appnak a root jogot (az első su-kérésnél engedélyezd, vagy a Superuser fülön).

## Fordítás (Windows, Gradle nélkül)

A `build.ps1` a JDK 17-et, az Android build-tools 34-et és az Android 13 platformot (`android.jar`) a szomszédos `android-build` mappából várja, és aapt2 + javac + d8 + apksigner lánccal építi az APK-t. Az első futáskor saját aláírókulcsot generál (`tolato.keystore`, ez nincs a repóban).

## Korlátok

- A tolatókép kb. 1–2 másodperccel később jelenik meg, mint rendesen bekötött tolatásjelnél.
- Rendszerfrissítés megváltoztathatja a naplósorokat vagy a felvevő alkalmazást.
- Ha a fejegység tolatásérzékelése rendben működik, nincs szükség rá.

Saját felelősségre használd. Nem hivatalos, a FYT-hez vagy a Junsunhoz nincs köze.
