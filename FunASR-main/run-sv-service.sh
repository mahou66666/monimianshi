#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")" && pwd)

mvn -f "$ROOT/sv-service/pom.xml" -DskipTests package

java -jar "$ROOT/sv-service/target/sv-service-0.1.0.jar"
