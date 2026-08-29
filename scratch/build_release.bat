@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio1\jbr"
echo JAVA_HOME is %JAVA_HOME%
call .\gradlew.bat clean
call .\gradlew.bat assembleRelease
