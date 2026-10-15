@echo off
echo ========================================================
echo   LexiBridge MySQL Server Helper
echo ========================================================

netstat -ano | findstr :3306 >nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] MySQL is ALREADY running on port 3306!
    echo Database 'lexibridge' is ready for connections.
    echo You can now start the application using:
    echo    .\run-app.bat
    echo or
    echo    mvn spring-boot:run
    pause
    exit /b 0
)

set MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe
set DATA_DIR=C:\Users\PARVATHI\tools\mysql-data

if not exist "%MYSQL_BIN%" (
    echo [ERROR] MySQL binary not found at: "%MYSQL_BIN%"
    pause
    exit /b 1
)

echo Starting MySQL Server on port 3306...
"%MYSQL_BIN%" --datadir="%DATA_DIR%" --console
