@echo off
chcp 65001 > nul
title Batalla Naval 3D - jMonkeyEngine
cls
echo ======================================================================
echo             BATALLA NAVAL 3D - JMONKEYENGINE (JAVA 21)
echo ======================================================================
echo.
echo  Iniciando simulador tactico naval 3D...
echo.

rem --- Localizar un JDK moderno (17+). Se recomienda JDK 21. ---
if defined JAVA_HOME set "JAVA_HOME_ORIGINAL=%JAVA_HOME%"
set "JAVA_HOME="
set "JAVA_EXE="

rem Prioridad 1: una instalacion conocida de JDK 21 (evita un JAVA_HOME antiguo).
if not defined JAVA_EXE if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe"
)

if not defined JAVA_EXE for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
    if exist "%%D\bin\java.exe" (
        set "JAVA_HOME=%%D"
        set "JAVA_EXE=%%D\bin\java.exe"
    )
)

if not defined JAVA_EXE for /d %%D in ("C:\Program Files\Java\jdk-21*") do (
    if exist "%%D\bin\java.exe" (
        set "JAVA_HOME=%%D"
        set "JAVA_EXE=%%D\bin\java.exe"
    )
)

rem Prioridad 2: JDK 17 o un JAVA_HOME valido.
if not defined JAVA_EXE for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
    if exist "%%D\bin\java.exe" (
        set "JAVA_HOME=%%D"
        set "JAVA_EXE=%%D\bin\java.exe"
    )
)

if not defined JAVA_EXE if defined JAVA_HOME_ORIGINAL if exist "%JAVA_HOME_ORIGINAL%\bin\java.exe" (
    set "JAVA_HOME=%JAVA_HOME_ORIGINAL%"
    set "JAVA_EXE=%JAVA_HOME_ORIGINAL%\bin\java.exe"
)

if not defined JAVA_EXE (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%J"
    )
)

if not defined JAVA_EXE (
    echo  ERROR: no se encontro una instalacion de Java.
    echo  Instala JDK 21 desde https://adoptium.net y vuelve a intentarlo.
    echo.
    pause
    exit /b 1
)

echo  Java: %JAVA_EXE%
echo.

rem --- Compilar si el JAR no existe ---
if not exist "target\batalla-naval-jme-1.0.0.jar" (
    echo  Compilando el proyecto...
    echo.
    call "%~dp0apache-maven-3.9.6\bin\mvn.cmd" -q -DskipTests package
)

if exist "target\batalla-naval-jme-1.0.0.jar" (
    "%JAVA_EXE%" -jar "target\batalla-naval-jme-1.0.0.jar"
) else (
    echo.
    echo  No se pudo generar el JAR. Intentando ejecutar con Maven...
    call "%~dp0apache-maven-3.9.6\bin\mvn.cmd" exec:java
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Ha ocurrido un error al ejecutar el juego.
    pause
)
