@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------
@echo off
@setlocal

set ERROR_CODE=0

@REM check JAVA_HOME
if "%JAVA_HOME%"=="" goto no_java_home

if not exist "%JAVA_HOME%\bin\java.exe" goto no_java_home

set JAVA_EXE="%JAVA_HOME%\bin\java.exe"

set WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set WRAPPER_URL="https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%~dp0.mvn\wrapper\maven-wrapper.properties") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

if exist %WRAPPER_JAR% goto runm2

echo Could not find %WRAPPER_JAR%, downloading it ...
echo Downloading from: %WRAPPER_URL%
powershell -Command "&{"^
    "if (!(Test-Path '%~dp0.mvn\wrapper')) { New-Item -ItemType Directory -Path '%~dp0.mvn\wrapper' | Out-Null };"^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;"^
    "(New-Object System.Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
    "}"
if exist %WRAPPER_JAR% goto runm2
echo Failed to download %WRAPPER_JAR%
goto error

:runm2
%JAVA_EXE% %JAVA_OPTS% %MAVEN_OPTS% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%~dp0" %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 goto error
goto end

:no_java_home
echo Error: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
goto error

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

exit /B %ERROR_CODE%
