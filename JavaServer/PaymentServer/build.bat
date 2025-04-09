@echo off
echo Compiling PaymentServer...
mkdir bin 2>nul

javac -cp lib\json.jar -d bin src\PaymentServer.java

echo Compilation complete. To run the server, use:
echo java -cp bin;lib\json.jar PaymentServer 