@ECHO OFF
SETLOCAL

SET APP_HOME=%~dp0
SET CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%CLASSPATH%" (
  ECHO Gradle wrapper JAR not found: %CLASSPATH%
  EXIT /B 1
)

SET JAVA_CMD=java
IF NOT "%JAVA_HOME%"=="" SET JAVA_CMD="%JAVA_HOME%\bin\java"

%JAVA_CMD% -Xmx64m -Xms64m %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
ENDLOCAL
