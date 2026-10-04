@echo off
setlocal
set TARGET=D:\LUMI-HOME\qldhjava\runtime-lib
if exist "%TARGET%" rmdir /s /q "%TARGET%"
mkdir "%TARGET%"

rem Copy tất cả JAR từ .m2 (bỏ sources/javadoc/junit)
for /r "C:\Users\PC\.m2\repository" %%f in (*.jar) do (
    echo %%~nxf | findstr /i "sources javadoc junit mockito hamcrest" >nul
    if errorlevel 1 (
        copy /Y "%%f" "%TARGET%\" >nul
    )
)

rem Copy classes + resources
xcopy /Y /E /I /Q "D:\LUMI-HOME\qldhjava\target\classes\com" "%TARGET%\com"
xcopy /Y /E /I /Q "D:\LUMI-HOME\qldhjava\target\classes\templates" "%TARGET%\templates"
copy /Y "D:\LUMI-HOME\qldhjava\src\main\resources\application.properties" "%TARGET%\" >nul

echo Done.
dir "%TARGET%" | findstr "File(s)"