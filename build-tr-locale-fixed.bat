@echo off
setlocal EnableDelayedExpansion

set "ROOT=%~dp0"
set "BASE=%ROOT%build\BuildGDX-locale-fixed.jar"
if not exist "%BASE%" set "BASE=%ROOT%build\BuildGDX-tr_TR-fixed.jar"
set "OUTPUT=%ROOT%build\BuildGDX-tr_TR-fixed-new.jar"
set "CLASSES=%TEMP%\buildgdx-locale-all"

if not exist "%BASE%" (
    echo Missing base JAR. Expected BuildGDX-locale-fixed.jar or BuildGDX-tr_TR-fixed.jar in build.
    exit /b 1
)

set "JAVAC=javac"
if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if not exist "%JAVA_HOME%\bin\javac.exe" if exist "C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javac.exe" set "JAVAC=C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javac.exe"
set "JAR=jar"
if exist "%JAVA_HOME%\bin\jar.exe" set "JAR=%JAVA_HOME%\bin\jar.exe"
if not exist "%JAVA_HOME%\bin\jar.exe" if exist "C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\jar.exe" set "JAR=C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\jar.exe"

if "%JAVAC%"=="javac" where javac >nul 2>&1
if "%JAVAC%"=="javac" if errorlevel 1 (
    echo javac was not found. Set JAVA_HOME to a JDK installation.
    exit /b 1
)
if not "%JAVAC%"=="javac" if not exist "%JAVAC%" (
    echo javac was not found. Set JAVA_HOME to a JDK installation.
    exit /b 1
)

if exist "%CLASSES%" rmdir /s /q "%CLASSES%"
mkdir "%CLASSES%"
mkdir "%ROOT%build" 2>nul

set "CP="
for /r "%ROOT%core\libs" %%F in (*.jar) do set "CP=!CP!;%%F"
for /r "%ROOT%lwjgl3\libs" %%F in (*.jar) do set "CP=!CP!;%%F"
set "SOURCEPATH=%ROOT%core\src;%ROOT%lwjgl3\src"

"%JAVAC%" -encoding UTF-8 -cp "%CP%" -sourcepath "%SOURCEPATH%" -d "%CLASSES%" ^
 "%ROOT%core\src\ru\m210projects\Build\Architecture\common\audio\BuildAudio.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\Cache.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\CacheResourceMap.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\fs\Directory.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\fs\FileEntry.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\grp\GrpEntry.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\grp\GrpFile.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\rff\RffEntry.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\rff\RffFile.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\zip\ZipEntry.java" ^
 "%ROOT%core\src\ru\m210projects\Build\filehandle\zip\ZipFile.java" ^
 "%ROOT%core\src\ru\m210projects\Build\net\WaifUPnp\Gateway.java" ^
 "%ROOT%core\src\ru\m210projects\Build\Pattern\LogSender.java" ^
 "%ROOT%core\src\ru\m210projects\Build\Pattern\MenuItems\MenuFileBrowser.java" ^
 "%ROOT%core\src\ru\m210projects\Build\Script\MapHackInfo.java" ^
 "%ROOT%core\src\ru\m210projects\Build\settings\InputContext.java" ^
 "%ROOT%core\src\ru\m210projects\Build\Types\MD4.java" ^
 "%ROOT%lwjgl3\src\com\badlogic\gdx\backends\lwjgl3\audio\OpenALAudio.java"
if errorlevel 1 exit /b 1

copy /y "%BASE%" "%OUTPUT%" >nul
pushd "%CLASSES%"
"%JAR%" uf "%OUTPUT%" ^
 "ru/m210projects/Build/Architecture/common/audio/BuildAudio.class" ^
 "ru/m210projects/Build/Architecture/common/audio/BuildAudio$DummyAudio.class" ^
 "ru/m210projects/Build/filehandle/Cache.class" ^
 "ru/m210projects/Build/filehandle/Cache$1.class" ^
 "ru/m210projects/Build/filehandle/Cache$CacheHolder.class" ^
 "ru/m210projects/Build/filehandle/Cache$GroupType.class" ^
 "ru/m210projects/Build/filehandle/CacheResourceMap.class" ^
 "ru/m210projects/Build/filehandle/CacheResourceMap$1.class" ^
 "ru/m210projects/Build/filehandle/CacheResourceMap$CachePriority.class" ^
 "ru/m210projects/Build/filehandle/CacheResourceMap$GroupNode.class" ^
 "ru/m210projects/Build/filehandle/fs/Directory.class" ^
 "ru/m210projects/Build/filehandle/fs/Directory$1.class" ^
 "ru/m210projects/Build/filehandle/fs/Directory$Game.class" ^
 "ru/m210projects/Build/filehandle/fs/FileEntry.class" ^
 "ru/m210projects/Build/filehandle/grp/GrpEntry.class" ^
 "ru/m210projects/Build/filehandle/grp/GrpFile.class" ^
 "ru/m210projects/Build/filehandle/rff/RffEntry.class" ^
 "ru/m210projects/Build/filehandle/rff/RffEntry$DictFlags.class" ^
 "ru/m210projects/Build/filehandle/rff/RffFile.class" ^
 "ru/m210projects/Build/filehandle/zip/ZipEntry.class" ^
 "ru/m210projects/Build/filehandle/zip/ZipFile.class" ^
 "ru/m210projects/Build/net/WaifUPnp/Gateway.class" ^
 "ru/m210projects/Build/Pattern/LogSender.class" ^
 "ru/m210projects/Build/Pattern/MenuItems/MenuFileBrowser.class" ^
 "ru/m210projects/Build/Pattern/MenuItems/MenuFileBrowser$1.class" ^
 "ru/m210projects/Build/Pattern/MenuItems/MenuFileBrowser$BackDirectory.class" ^
 "ru/m210projects/Build/Pattern/MenuItems/MenuFileBrowser$ExtProp.class" ^
 "ru/m210projects/Build/Script/MapHackInfo.class" ^
 "ru/m210projects/Build/settings/InputContext.class" ^
 "ru/m210projects/Build/Types/MD4.class" ^
 "ru/m210projects/Build/Types/MD4$MD4Digest.class" ^
 "com/badlogic/gdx/backends/lwjgl3/audio/OpenALAudio.class" ^
 "com/badlogic/gdx/backends/lwjgl3/audio/OpenALAudio$1.class"
popd
if errorlevel 1 exit /b 1

echo Created: "%OUTPUT%"