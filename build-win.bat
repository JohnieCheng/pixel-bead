@echo off
REM Build Windows packages for Pixel Bead (Java 21 + JavaFX 21).
REM Run this ON WINDOWS with JDK 21 installed.
REM   build-win.bat app-image   -> portable folder, no extra tools needed
REM   build-win.bat all         -> app-image + portable zip + .exe installer
REM   build-win.bat exe         -> .exe installer, requires WiX Toolset 3.14+ (https://wixtoolset.org)
REM   build-win.bat msi         -> .msi installer, requires WiX Toolset 3.14+
setlocal
cd /d "%~dp0"

if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "MVN_REPO=%USERPROFILE%\.m2\repository"
set "TYPE=%1"
if "%TYPE%"=="" set "TYPE=app-image"

echo ==^> Building jar...
call mvnw.cmd -q clean package -DskipTests
if errorlevel 1 exit /b 1

echo ==^> Resolving version...
set "VERSION=%~2"
if "%VERSION%"=="" for /f "delims=" %%v in ('powershell -NoProfile -Command "[xml]$p=Get-Content pom.xml; $p.project.version"') do set "VERSION=%%v"
if "%VERSION%"=="" set "VERSION=1.0.0"
echo ==^> Version: %VERSION%

echo ==^> Assembling runtime libs...
if exist target\libs rmdir /s /q target\libs
mkdir target\libs
for /f "delims=" %%j in ('dir /b target\*.jar') do set "APP_JAR=%%j"
copy /y "target\%APP_JAR%" target\libs\ >nul
copy /y "%MVN_REPO%\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\controlsfx\controlsfx\11.2.1\controlsfx-11.2.1.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\kordamp\ikonli\ikonli-core\12.3.1\ikonli-core-12.3.1.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\kordamp\ikonli\ikonli-javafx\12.3.1\ikonli-javafx-12.3.1.jar" target\libs\ >nul
copy /y "%MVN_REPO%\com\fasterxml\jackson\core\jackson-databind\2.18.2\jackson-databind-2.18.2.jar" target\libs\ >nul
copy /y "%MVN_REPO%\com\fasterxml\jackson\core\jackson-core\2.18.2\jackson-core-2.18.2.jar" target\libs\ >nul
copy /y "%MVN_REPO%\com\fasterxml\jackson\core\jackson-annotations\2.18.2\jackson-annotations-2.18.2.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\apache\pdfbox\pdfbox\3.0.3\pdfbox-3.0.3.jar" target\libs\ >nul
copy /y "%MVN_REPO%\org\apache\pdfbox\fontbox\3.0.3\fontbox-3.0.3.jar" target\libs\ >nul
copy /y "%MVN_REPO%\commons-logging\commons-logging\1.3.3\commons-logging-1.3.3.jar" target\libs\ >nul

set "JPACKAGE=%JAVA_HOME%\bin\jpackage"
set "JPARAMS=--name "Pixel Bead" --app-version %VERSION% --input target\libs --main-jar %APP_JAR% --main-class com.johnie.pixelbead.Launcher --icon src\main\resources\icons\pixel-bead.ico --dest target\dist"
rem --win-menu/--win-shortcut are installer-only options, invalid for app-image.
set "INSTALLER_OPTS=--win-menu --win-shortcut"

if "%TYPE%"=="app-image" goto app_image
if "%TYPE%"=="all" goto app_image
if "%TYPE%"=="exe" goto exe
if "%TYPE%"=="msi" goto msi
echo Unknown type: %TYPE%
exit /b 1

:app_image
echo ==^> Running jpackage (app-image)...
"%JPACKAGE%" --type app-image %JPARAMS%
if errorlevel 1 exit /b 1
if "%TYPE%"=="app-image" goto done

echo ==^> Creating portable zip...
powershell -NoProfile -Command "Compress-Archive -Path 'target\dist\Pixel Bead' -DestinationPath 'target\dist\pixel-bead-portable.zip' -Force"
if errorlevel 1 exit /b 1
if "%TYPE%"=="all" goto exe_from_image
goto done

:exe_from_image
echo ==^> Running jpackage (exe from app-image)...
"%JPACKAGE%" --type exe --app-image "target\dist\Pixel Bead" --name "Pixel Bead" --app-version %VERSION% %INSTALLER_OPTS% --icon src\main\resources\icons\pixel-bead.ico --dest target\dist
if errorlevel 1 exit /b 1
goto done

:exe
echo ==^> Running jpackage (exe)...
"%JPACKAGE%" --type exe %JPARAMS% %INSTALLER_OPTS%
if errorlevel 1 exit /b 1
goto done

:msi
echo ==^> Running jpackage (msi)...
"%JPACKAGE%" --type msi %JPARAMS% %INSTALLER_OPTS%
if errorlevel 1 exit /b 1
goto done

:done
echo ==^> Done: target\dist
endlocal
