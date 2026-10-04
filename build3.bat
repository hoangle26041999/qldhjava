@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-27"
set "PATH=C:\Program Files\Java\jdk-27\bin;%PATH%"
set "MAVEN_OPTS=-Xmx1024m"
cd /D "d:\LUMI-HOME\qldhjava"
"C:\Program Files\Java\jdk-27\bin\java.exe" -classpath "d:\LUMI-HOME\qldhjava\.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=d:\LUMI-HOME\qldhjava" org.apache.maven.wrapper.MavenWrapperMain clean package -DskipTests
exit /b %ERRORLEVEL%
