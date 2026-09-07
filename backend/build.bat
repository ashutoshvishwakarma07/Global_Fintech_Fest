@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"C:\Users\Qualtech-Ashutosh\AppData\Local\Programs\apache-maven-3.9.6\bin\mvn.cmd" package -DskipTests
