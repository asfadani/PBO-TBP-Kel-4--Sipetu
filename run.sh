#!/usr/bin/env bash
set -e
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
exec java -cp out com.sipetu.Main
