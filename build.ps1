param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$BuildDir = "$PSScriptRoot\..\..\work\bililight-build",
    [string]$SigningKeystore = "",
    [string]$SigningAlias = "honkaiqr"
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
function Run([string]$Exe,[string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed ($LASTEXITCODE)" }
}
$BuildDir = [IO.Path]::GetFullPath($BuildDir)
$tools = Join-Path $Sdk 'build-tools\36.0.0'
$android = Join-Path $Sdk 'platforms\android-34\android.jar'
foreach ($p in @($android, "$tools\aapt2.exe", "$tools\d8.bat", "$tools\apksigner.bat")) {
    if (!(Test-Path -LiteralPath $p)) { throw "Required Android SDK component missing: $p" }
}
$CompileDir = Join-Path $BuildDir ("compile-" + [Guid]::NewGuid().ToString("N"))
foreach ($p in @($BuildDir,$CompileDir,"$CompileDir\classes","$CompileDir\dex")) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
$jar = Join-Path $BuildDir 'zxing-core-3.5.3.jar'
if (!(Test-Path -LiteralPath $jar)) {
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/google/zxing/core/3.5.3/core-3.5.3.jar' -OutFile $jar
}
$expected = '8d8064c1636fdaef7189dd9055c7d59950a8940a12f2293956446ec3c109fd82'
if ((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'ZXing dependency SHA-256 mismatch' }
$sources = @(Get-ChildItem -LiteralPath "$PSScriptRoot\src" -Filter '*.java' -Recurse | ForEach-Object FullName)
Run 'javac' (@('-encoding','UTF-8','-source','8','-target','8','-classpath',"$android;$jar",'-d',"$CompileDir\classes") + $sources)
$classes = @(Get-ChildItem -LiteralPath "$CompileDir\classes" -Filter '*.class' -Recurse | ForEach-Object FullName)
Run 'java' (@('-cp',"$tools\lib\d8.jar",'com.android.tools.r8.D8','--min-api','26','--lib',$android,'--output',"$CompileDir\dex") + $classes + @($jar))
Run "$tools\aapt2.exe" @('compile','--dir',"$PSScriptRoot\res",'-o',"$BuildDir\resources.zip")
Run "$tools\aapt2.exe" @('link','-I',$android,'--manifest',"$PSScriptRoot\AndroidManifest.xml",'-A',"$PSScriptRoot\assets",'-o',"$BuildDir\base.apk","$BuildDir\resources.zip")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [IO.Compression.ZipFile]::Open("$BuildDir\base.apk",[IO.Compression.ZipArchiveMode]::Update)
try { foreach ($d in Get-ChildItem -LiteralPath "$CompileDir\dex" -Filter '*.dex') { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($z,$d.FullName,$d.Name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null } } finally { $z.Dispose() }
if ($SigningKeystore) {
    $keystore = [IO.Path]::GetFullPath($SigningKeystore)
    if (!(Test-Path -LiteralPath $keystore) -or !$env:HONKAIQR_STORE_PASS) { throw 'Release signing requires an existing keystore and HONKAIQR_STORE_PASS environment variable' }
    $alias = $SigningAlias; $pass = 'env:HONKAIQR_STORE_PASS'; $output = Join-Path $PSScriptRoot 'HONKAI-QR-0.7.0-beta.apk'
} else {
    $keystore = Join-Path $BuildDir 'debug.jks'
    if (!(Test-Path -LiteralPath $keystore)) { Run 'keytool' @('-genkeypair','-keystore',$keystore,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Local BiliLight Debug','-keyalg','RSA','-keysize','2048','-validity','3650') }
    $alias = 'androiddebugkey'; $pass = 'pass:android'; $output = Join-Path $PSScriptRoot 'BiliLight-debug.apk'
}
Run "$tools\zipalign.exe" @('-f','4',"$BuildDir\base.apk","$BuildDir\aligned.apk")
Run "$tools\apksigner.bat" @('sign','--ks',$keystore,'--ks-key-alias',$alias,'--ks-pass',$pass,'--key-pass',$pass,'--out',$output,"$BuildDir\aligned.apk")
Run "$tools\apksigner.bat" @('verify','--verbose',$output)
Get-FileHash -LiteralPath $output -Algorithm SHA256
Write-Output "Built: $output"
