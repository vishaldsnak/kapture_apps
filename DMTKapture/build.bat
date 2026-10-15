@echo off
REM ===========================================================================
REM  Build script for DMT for Kapture (Data Migration Tool)  ->  dmtgt.war
REM
REM  TARGET RUNTIME:  OpenJDK 25  +  Apache Tomcat 11.x   (MC Dev policy)
REM
REM  Usage:   build.bat LOCAL | DEV | QA | PROD
REM
REM  ENVIRONMENTS - the parameter picks the configuration packaged into the WAR:
REM     LOCAL  workstation: src\application.properties, src\log.properties, WebContent\META-INF\context.xml
REM     DEV    MC Dev   : env\DEV\   (application.properties, log.properties, context.xml)
REM     QA     MC QA    : env\QA\
REM     PROD   PROD     : env\PROD\  (refused while a __TBC__ value is left)
REM  The env files replace WEB-INF\classes\application.properties / log.properties and META-INF\context.xml.
REM  Their values come from Environment_Config\Kapture_Environment_Config.xlsx - change
REM  them there AND in env\<ENV>\. Before packaging, env\EnvConfigCheck.java checks that
REM  every key of src\application.properties is in the env file and no __TBC__ is left.
REM  Output: dist\<ENV>\dmtgt.war (META-INF\build-env.txt inside says which environment).
REM
REM  This script DELIBERATELY IGNORES %JAVA_HOME% AND %CATALINA_HOME%. Those are
REM  left pointing at JDK 1.8 / Tomcat 9 on this workstation because the one-time
REM  data migration jobs (Jobs 1-10) still build and run there. Web applications
REM  are a separate runtime and are pinned here instead. Override per machine:
REM
REM     SET DMT_JDK_HOME=D:\Softwares\jdk-25.0.4+7
REM     SET DMT_TOMCAT_HOME=D:\Softwares\apache-tomcat-11.0.24
REM
REM  WHY THE BUILD HAS TWO PHASES
REM  ---------------------------------------------------------------------------
REM  The source is written against javax.servlet.*, and Tomcat 10 renamed that
REM  namespace to jakarta.servlet.*. Rather than rewrite the servlet source files and go
REM  hunting for Jakarta builds of JSTL and commons-fileupload, the build compiles
REM  as-is and then runs the WAR through the APACHE TOMCAT MIGRATION TOOL FOR
REM  JAKARTA EE, which ships inside Tomcat 11 itself (lib\jakartaee-migration-*.jar).
REM  It rewrites javax -> jakarta in the bytecode of the application AND of every
REM  jar in WEB-INF\lib, including the TLDs, so the JSP taglib URIs keep working.
REM
REM     phase 1  ->  build\dmtgt-javax.war    (javax  - runs on Tomcat 7/8/9)
REM     phase 2  ->  dmtgt.war                (jakarta - runs on Tomcat 11)
REM
REM  DEPLOY dmtgt.war. The javax WAR is kept only so a failure can be compared
REM  against a known-good Tomcat 9 deployment.
REM
REM  The -profile=TOMCAT switch converts ONLY the Servlet/JSP/EL/WebSocket APIs.
REM  It deliberately leaves javax.mail, javax.xml.soap and javax.activation alone:
REM  those come from jars inside this WAR (mail, saaj, axis, activation), nothing
REM  in Tomcat 11 conflicts with them, and converting them would be churn with no
REM  benefit. Do not change it to -profile=EE without retesting the e-mail and
REM  web-service paths.
REM ===========================================================================

SETLOCAL ENABLEDELAYEDEXPANSION

SET APP_DIR=%~dp0
IF "%APP_DIR:~-1%"=="\" SET APP_DIR=%APP_DIR:~0,-1%

REM ---------------------------------------------------------------- environment
SET ENV=%~1
IF /I "%ENV%"=="LOCAL" SET ENV=LOCAL
IF /I "%ENV%"=="DEV" SET ENV=DEV
IF /I "%ENV%"=="QA" SET ENV=QA
IF /I "%ENV%"=="PROD" SET ENV=PROD
IF NOT "%ENV%"=="LOCAL" IF NOT "%ENV%"=="DEV" IF NOT "%ENV%"=="QA" IF NOT "%ENV%"=="PROD" (
    echo Usage: build.bat LOCAL ^| DEV ^| QA ^| PROD
    ENDLOCAL & exit /b 1
)
SET ENV_DIR=%APP_DIR%\env\%ENV%

REM ---------------------------------------------------------------- JDK 25
IF "%DMT_JDK_HOME%"=="" SET DMT_JDK_HOME=D:\Softwares\jdk-25.0.4+7
IF NOT EXIST "%DMT_JDK_HOME%\bin\javac.exe" (
    echo ERROR: javac not found under DMT_JDK_HOME=%DMT_JDK_HOME%
    echo        Set DMT_JDK_HOME to an OpenJDK 25 installation and run again.
    echo        Do NOT change JAVA_HOME - the migration jobs still need 1.8.
    ENDLOCAL & exit /b 1
)
SET JAVAC="%DMT_JDK_HOME%\bin\javac.exe"
SET JAR_TOOL="%DMT_JDK_HOME%\bin\jar.exe"
SET JAVA_TOOL="%DMT_JDK_HOME%\bin\java.exe"

REM ---------------------------------------------------------------- Tomcat 11
REM Used for ONE thing: the Jakarta EE migration tool in its lib folder. Nothing
REM from Tomcat is packaged into the WAR.
IF "%DMT_TOMCAT_HOME%"=="" SET DMT_TOMCAT_HOME=D:\Softwares\apache-tomcat-11.0.24
SET MIGRATE_JAR=
FOR %%J IN ("%DMT_TOMCAT_HOME%\lib\jakartaee-migration-*-shaded.jar") DO SET MIGRATE_JAR=%%~fJ
IF "%MIGRATE_JAR%"=="" (
    echo ERROR: jakartaee-migration-*-shaded.jar not found under
    echo        %DMT_TOMCAT_HOME%\lib
    echo        Set DMT_TOMCAT_HOME to a Tomcat 11.x installation and run again.
    ENDLOCAL & exit /b 1
)

REM ---------------------------------------------------------------- layout
SET SRC=%APP_DIR%\src
SET WEBCONTENT=%APP_DIR%\WebContent
SET BUILD=%APP_DIR%\build
SET STAGE=%BUILD%\war
SET CLASSES=%STAGE%\WEB-INF\classes
SET LIB=%WEBCONTENT%\WEB-INF\lib
SET BUILDLIB=%APP_DIR%\build-lib
REM BOTH WARS ARE CALLED dmtgt.war ON PURPOSE - Tomcat takes the context path from the
REM file name, so anything else deploys the application at the wrong URL. The javax
REM fallback therefore lives in its own folder rather than carrying a different name:
REM naming it dmtgt-javax.war would deploy it at /dmtgt-javax and every bookmarked
REM link, and the iv-user header path, would be wrong.
SET JAVAX_DIR=%BUILD%\tomcat9
SET JAVAX_WAR=%JAVAX_DIR%\dmtgt.war
SET DIST_DIR=%APP_DIR%\dist\%ENV%
SET OUT_WAR=%DIST_DIR%\dmtgt.war

REM build-lib holds the javax servlet/jsp/el API jars, compile-time only, never
REM packaged. They are vendored so the build needs no Tomcat 9 on the machine.
IF NOT EXIST "%BUILDLIB%\servlet-api.jar" (
    echo ERROR: %BUILDLIB%\servlet-api.jar is missing.
    echo        Restore build-lib\ from source control - it holds the javax
    echo        servlet/jsp/el API jars this source compiles against.
    ENDLOCAL & exit /b 1
)

REM Non-LOCAL: the env folder must be complete and consistent BEFORE anything is built.
IF "%ENV%"=="LOCAL" GOTO env_checked
IF NOT EXIST "%ENV_DIR%\application.properties" (
    echo ERROR: %ENV_DIR%\application.properties is missing.
    ENDLOCAL & exit /b 1
)
IF NOT EXIST "%ENV_DIR%\log.properties" (
    echo ERROR: %ENV_DIR%\log.properties is missing.
    ENDLOCAL & exit /b 1
)
IF NOT EXIST "%ENV_DIR%\context.xml" (
    echo ERROR: %ENV_DIR%\context.xml is missing.
    ENDLOCAL & exit /b 1
)
echo [0/6] Checking env\%ENV%...
%JAVA_TOOL% "%APP_DIR%\env\EnvConfigCheck.java" "%SRC%\application.properties" "%ENV_DIR%"
IF ERRORLEVEL 1 (
    echo Environment check FAILED - nothing was built.
    ENDLOCAL & exit /b 1
)
:env_checked

echo.
echo   ENVIRONMENT %ENV%
echo   JDK        %DMT_JDK_HOME%
echo   TOMCAT     %DMT_TOMCAT_HOME%   (migration tool only)
echo   OUTPUT     %OUT_WAR%
echo.

REM ---------------------------------------------------------------- clean
IF EXIST "%BUILD%" rmdir /S /Q "%BUILD%"
mkdir "%CLASSES%" 2>NUL
mkdir "%JAVAX_DIR%" 2>NUL
IF NOT EXIST "%DIST_DIR%" mkdir "%DIST_DIR%"

REM ---------------------------------------------------------------- web content
echo [1/6] Copying WebContent...
xcopy "%WEBCONTENT%\*" "%STAGE%\" /S /E /I /Y >NUL
IF ERRORLEVEL 1 (
    echo Copy of WebContent FAILED.
    ENDLOCAL & exit /b 1
)
IF NOT "%ENV%"=="LOCAL" (
    copy /Y "%ENV_DIR%\context.xml" "%STAGE%\META-INF\context.xml" >NUL
    IF ERRORLEVEL 1 (
        echo Copy of env\%ENV%\context.xml FAILED.
        ENDLOCAL & exit /b 1
    )
)

REM ---------------------------------------------------------------- compile
echo [2/6] Collecting sources...
DEL /Q "%BUILD%\sources.txt" 2>NUL
FOR /R "%SRC%" %%F IN (*.java) DO (
    echo %%F>> "%BUILD%\sources.txt"
)

echo [3/6] Compiling with JDK 25...
%JAVAC% -encoding UTF-8 -nowarn ^
    -cp "%LIB%\*;%BUILDLIB%\*" ^
    -d "%CLASSES%" @"%BUILD%\sources.txt"
IF ERRORLEVEL 1 (
    echo.
    echo Compilation FAILED.
    ENDLOCAL & exit /b 1
)
DEL /Q "%BUILD%\sources.txt" 2>NUL

REM ---------------------------------------------------------------- resources
REM Everything on the classpath that is NOT a .java file: application.properties,
REM log.properties, the nl\ resource bundles and the e-mail HTML templates.
echo [4/6] Copying classpath resources...
xcopy "%SRC%\*.properties" "%CLASSES%\" /S /I /Y >NUL
xcopy "%SRC%\*.html" "%CLASSES%\" /S /I /Y >NUL
IF NOT EXIST "%CLASSES%\application.properties" (
    echo ERROR: application.properties did not reach WEB-INF\classes.
    ENDLOCAL & exit /b 1
)
REM Non-LOCAL: the environment's files replace the workstation ones copied above.
IF NOT "%ENV%"=="LOCAL" (
    copy /Y "%ENV_DIR%\application.properties" "%CLASSES%\application.properties" >NUL
    IF ERRORLEVEL 1 (
        echo Copy of env\%ENV%\application.properties FAILED.
        ENDLOCAL & exit /b 1
    )
    copy /Y "%ENV_DIR%\log.properties" "%CLASSES%\log.properties" >NUL
    IF ERRORLEVEL 1 (
        echo Copy of env\%ENV%\log.properties FAILED.
        ENDLOCAL & exit /b 1
    )
)
REM Which environment this WAR was built for - read it with: jar xf dmtgt.war META-INF/build-env.txt
(
    echo environment=%ENV%
    echo built=%DATE% %TIME%
    echo machine=%COMPUTERNAME%
) > "%STAGE%\META-INF\build-env.txt"

REM ---------------------------------------------------------------- package
echo [5/6] Packaging javax WAR...
%JAR_TOOL% cf "%JAVAX_WAR%" -C "%STAGE%" .
IF ERRORLEVEL 1 (
    echo WAR packaging FAILED.
    ENDLOCAL & exit /b 1
)

REM ---------------------------------------------------------------- jakarta
echo [6/6] Converting javax -^> jakarta for Tomcat 11...
IF EXIST "%OUT_WAR%" DEL /Q "%OUT_WAR%"
%JAVA_TOOL% -jar "%MIGRATE_JAR%" -profile=TOMCAT "%JAVAX_WAR%" "%OUT_WAR%" > "%BUILD%\jakarta-migration.log" 2>&1
IF ERRORLEVEL 1 (
    echo Jakarta conversion FAILED - see %BUILD%\jakarta-migration.log
    ENDLOCAL & exit /b 1
)
IF NOT EXIST "%OUT_WAR%" (
    echo Jakarta conversion produced no WAR - see %BUILD%\jakarta-migration.log
    ENDLOCAL & exit /b 1
)

echo.
echo Build OK.
echo   Tomcat 11 (deploy this)   %OUT_WAR%
echo   Tomcat 9  (fallback)      %JAVAX_WAR%
echo   conversion log            %BUILD%\jakarta-migration.log
echo.
IF "%ENV%"=="LOCAL" (echo Configuration packaged: src\application.properties, src\log.properties, WebContent\META-INF\context.xml)
IF NOT "%ENV%"=="LOCAL" (echo Configuration packaged: env\%ENV%\application.properties, log.properties, context.xml)
ENDLOCAL
