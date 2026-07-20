@echo off
mvn exec:java -Dexec.mainClass="com.prisma.Launcher" > error.log 2>&1
