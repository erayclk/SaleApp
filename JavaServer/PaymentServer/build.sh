#!/bin/bash

echo "Compiling PaymentServer..."
mkdir -p bin

javac -cp lib/json.jar -d bin src/PaymentServer.java

echo "Compilation complete. To run the server, use:"
echo "java -cp bin:lib/json.jar PaymentServer" 