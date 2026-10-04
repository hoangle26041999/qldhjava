@echo off
setlocal
set TARGET=D:\LUMI-HOME\qldhjava\runtime-lib

rem Xoa cac version cu cua cac thu vien chinh (giu version moi nhat)
rem Xoa lombok cu (tru 1.18.48)
for %%f in ("%TARGET%\lombok-1.18.30.jar" "%TARGET%\lombok-1.18.32.jar" "%TARGET%\lombok-1.18.34.jar" "%TARGET%\lombok-1.18.38.jar") do if exist %%f del %%f

rem Spring Boot (3.2.0 va 3.2.5)
for %%f in ("%TARGET%\spring-boot-3.2.0.jar" "%TARGET%\spring-boot-starter-3.2.0.jar" "%TARGET%\spring-boot-starter-data-mongodb-3.2.0.jar" "%TARGET%\spring-boot-starter-thymeleaf-3.2.0.jar" "%TARGET%\spring-boot-starter-web-3.2.0.jar" "%TARGET%\spring-boot-starter-validation-3.2.0.jar" "%TARGET%\spring-boot-starter-json-3.2.0.jar" "%TARGET%\spring-boot-starter-tomcat-3.2.0.jar" "%TARGET%\spring-boot-starter-logging-3.2.0.jar" "%TARGET%\spring-boot-starter-jdbc-3.2.5.jar" "%TARGET%\spring-boot-starter-data-jpa-3.2.5.jar" "%TARGET%\spring-boot-starter-json-3.2.5.jar" "%TARGET%\spring-boot-starter-logging-3.2.5.jar" "%TARGET%\spring-boot-starter-web-3.2.5.jar" "%TARGET%\spring-boot-starter-tomcat-3.2.5.jar" "%TARGET%\spring-boot-starter-aop-3.2.5.jar" "%TARGET%\spring-boot-starter-3.2.5.jar" "%TARGET%\spring-boot-maven-plugin-3.2.0.jar" "%TARGET%\spring-boot-maven-plugin-3.2.5.jar" "%TARGET%\spring-boot-loader-tools-3.4.0.jar" "%TARGET%\spring-boot-loader-classic-3.2.5.jar") do if exist %%f del %%f

rem Spring Framework (6.1.x)
for %%f in ("%TARGET%\spring-aop-6.1.1.jar" "%TARGET%\spring-aop-6.1.6.jar" "%TARGET%\spring-aspects-6.1.6.jar" "%TARGET%\spring-beans-6.1.1.jar" "%TARGET%\spring-beans-6.1.6.jar" "%TARGET%\spring-context-6.1.1.jar" "%TARGET%\spring-context-6.1.6.jar" "%TARGET%\spring-core-6.1.1.jar" "%TARGET%\spring-core-6.1.6.jar" "%TARGET%\spring-expression-6.1.1.jar" "%TARGET%\spring-expression-6.1.6.jar" "%TARGET%\spring-jcl-6.1.1.jar" "%TARGET%\spring-jcl-6.1.6.jar" "%TARGET%\spring-jdbc-6.1.6.jar" "%TARGET%\spring-orm-6.1.6.jar" "%TARGET%\spring-tx-6.1.1.jar" "%TARGET%\spring-tx-6.1.6.jar" "%TARGET%\spring-web-6.1.1.jar" "%TARGET%\spring-web-6.1.6.jar" "%TARGET%\spring-webmvc-6.1.1.jar" "%TARGET%\spring-webmvc-6.1.6.jar") do if exist %%f del %%f

rem Spring Data (3.2.x va 4.2.x)
for %%f in ("%TARGET%\spring-data-commons-3.2.0.jar" "%TARGET%\spring-data-commons-3.2.5.jar" "%TARGET%\spring-data-jpa-3.2.5.jar" "%TARGET%\spring-data-mongodb-4.2.0.jar") do if exist %%f del %%f

rem Jackson (2.15.x)
for %%f in ("%TARGET%\jackson-annotations-2.15.3.jar" "%TARGET%\jackson-annotations-2.15.4.jar" "%TARGET%\jackson-core-2.15.3.jar" "%TARGET%\jackson-core-2.15.4.jar" "%TARGET%\jackson-databind-2.15.3.jar" "%TARGET%\jackson-databind-2.15.4.jar" "%TARGET%\jackson-datatype-jdk8-2.15.3.jar" "%TARGET%\jackson-datatype-jdk8-2.15.4.jar" "%TARGET%\jackson-datatype-jsr310-2.15.3.jar" "%TARGET%\jackson-datatype-jsr310-2.15.4.jar" "%TARGET%\jackson-module-parameter-names-2.15.3.jar" "%TARGET%\jackson-module-parameter-names-2.15.4.jar" "%TARGET%\classmate-1.6.0.jar") do if exist %%f del %%f

rem Slf4j (1.x)
if exist "%TARGET%\slf4j-api-1.7.36.jar" del "%TARGET%\slf4j-api-1.7.36.jar"

rem Logback (1.4.x, 1.5.12 OK)
for %%f in ("%TARGET%\logback-classic-1.4.11.jar" "%TARGET%\logback-classic-1.4.14.jar" "%TARGET%\logback-core-1.4.11.jar" "%TARGET%\logback-core-1.4.14.jar") do if exist %%f del %%f

rem Tomcat embed (10.1.20) - giu
rem micrometer (1.12.x)
for %%f in ("%TARGET%\micrometer-commons-1.12.0.jar" "%TARGET%\micrometer-commons-1.12.5.jar" "%TARGET%\micrometer-observation-1.12.0.jar" "%TARGET%\micrometer-observation-1.12.5.jar") do if exist %%f del %%f

rem Tomcat embed 10.1.20 nho hon 10.1.x? Giu 10.1.20 vi khong co version moi hon
rem spring-data-mongodb 4.4.0 la tot

echo After cleanup:
dir /b "%TARGET%" | find /c ".jar"
echo jars remain.