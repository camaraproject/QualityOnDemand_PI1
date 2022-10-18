#!/bin/bash

if [ ! -d "lib" ]; then
echo "lib folder not found"
exit 1
fi

LIB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/lib"
CORE_JAR=$(ls lib/senf-core-*.jar)

java -Dloader.path="${LIB_DIR}" -jar "${CORE_JAR}"