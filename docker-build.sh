#!/bin/bash
set -e

echo "1. 构建Docker镜像..."
docker build -t legado-builder -f Dockerfile.simple .

echo "2. 运行构建..."
docker run --rm \
  -v $(pwd):/workspace \
  -v gradle-cache:/root/.gradle \
  legado-builder \
  ./gradlew :app:assembleDebug --no-daemon --console=plain

echo "3. 检查生成的APK..."
find . -name "*.apk" -type f | head -5
