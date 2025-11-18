@ECHO OFF
SETLOCAL

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

set JAVA_EXE=java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

goto init
goto execute

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

:init
if exist "%JAVA_EXE%" goto okJava

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the

echo location of your Java installation.
goto fail

:okJava

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
goto end

:fail
exit /b 1

:end
ENDLOCAL
