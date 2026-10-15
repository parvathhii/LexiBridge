@echo off
echo ========================================================
echo   Starting LexiBridge Spring Boot Web Application...
echo ========================================================
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot
set MAVEN_HOME=C:\Users\PARVATHI\tools\apache-maven-3.9.9
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

mvn spring-boot:run
