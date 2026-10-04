@echo off
setlocal
set _JAVA_OPTIONS=
call mvnw.cmd -DskipTests package
