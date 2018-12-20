@echo off
:com
SET PATH=C:\Program Files\Java\jdk1.7.0\bin
cls
echo Compiling...
javac -d bin -Xlint *.java
echo Done!
pause
cls
goto com