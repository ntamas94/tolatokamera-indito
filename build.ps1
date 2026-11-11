# Tolatókamera indító – fordítás Gradle nélkül (aapt2 + javac + d8 + apksigner)
$ErrorActionPreference = 'Stop'
$P   = $PSScriptRoot
$T   = Join-Path (Split-Path $P) 'android-build'
$JDK = (Get-ChildItem "$T\jdk" -Directory | Select-Object -First 1).FullName
$BT  = "$T\bt\android-14"
$JAR = "$T\plat\android-13\android.jar"
$env:JAVA_HOME = $JDK
$env:PATH = "$JDK\bin;$env:PATH"
$O = "$P\out"
Remove-Item $O -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$O\res", "$O\classes", "$O\dex" | Out-Null

& "$BT\aapt2.exe" compile --dir "$P\res" -o "$O\res\res.zip"; if ($LASTEXITCODE) { throw 'aapt2 compile' }
& "$BT\aapt2.exe" link -o "$O\base.apk" -I $JAR --manifest "$P\AndroidManifest.xml" --java "$O\gen" "$O\res\res.zip"; if ($LASTEXITCODE) { throw 'aapt2 link' }

$src = Get-ChildItem "$P\src", "$O\gen" -Recurse -Filter *.java | ForEach-Object FullName
& javac -encoding UTF-8 -source 11 -target 11 -nowarn -classpath $JAR -d "$O\classes" $src; if ($LASTEXITCODE) { throw 'javac' }

$cls = Get-ChildItem "$O\classes" -Recurse -Filter *.class | ForEach-Object FullName
& "$BT\d8.bat" --min-api 29 --lib $JAR --output "$O\dex" $cls; if ($LASTEXITCODE) { throw 'd8' }

Copy-Item "$O\base.apk" "$O\unsigned.apk"
Push-Location "$O\dex"; & jar uf "$O\unsigned.apk" classes.dex; Pop-Location
& "$BT\zipalign.exe" -f 4 "$O\unsigned.apk" "$O\aligned.apk"; if ($LASTEXITCODE) { throw 'zipalign' }

$KS = "$P\tolato.keystore"
if (-not (Test-Path $KS)) {
    & keytool -genkeypair -keystore $KS -storepass tolato123 -keypass tolato123 -alias tolato -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Tolato Indito" 2>$null
}
& "$BT\apksigner.bat" sign --ks $KS --ks-pass pass:tolato123 --key-pass pass:tolato123 --out "$P\TolatoIndito.apk" "$O\aligned.apk"; if ($LASTEXITCODE) { throw 'apksigner' }
Get-Item "$P\TolatoIndito.apk" | Select-Object Name, Length
