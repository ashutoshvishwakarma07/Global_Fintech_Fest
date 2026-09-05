@echo off
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "DB_URL=jdbc:postgresql://18.60.179.46:5432/visiting_card_db"
set "DB_USERNAME=visiting_card"
set "DB_PASSWORD=qc_visiting_card#$2026"
set "SMTP_USERNAME=jyotilakhidhar96@gmail.com"
set "SMTP_PASSWORD=niwvyxmtutzxkdsi"
set "MAIL_FROM=jyotilakhidhar96@gmail.com"
java -Duser.timezone=UTC -jar target\gff-backend-0.0.1-SNAPSHOT.jar
