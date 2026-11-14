#!/usr/bin/env bash
# Compila y empaqueta generate-pdf-java.jar (clases + fuentes) en dist/.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out dist
mkdir -p out dist
javac --release 11 -encoding UTF-8 -d out $(find src -name '*.java')
cp -r src/* out/
jar --create --file dist/generate-pdf-java.jar -C out .
rm -rf out
echo "Generado: dist/generate-pdf-java.jar"
