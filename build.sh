#!/usr/bin/env bash
# 自动滚动 —— 手工打包 APK，不依赖 Gradle / Android Studio。
#
# 依赖：
#   JDK 17+                     （javac / jar / keytool）
#   Android SDK  platforms;android-34  +  build-tools;34.0.0
#
# 用法：
#   ANDROID_SDK_ROOT=/path/to/sdk bash build.sh
#   JAVA_HOME 未设置时会尝试从 PATH 里找 javac。
#
# 产物：build/AutoScroll.apk（首次运行会自动生成 keystore.jks）

set -e

PROJ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------- 平台差异 ----------
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    WIN=1
    # 转成 D:/path 形式：Win32 工具（aapt2/d8/javac）不认 /d/path 这种 MSYS 路径
    PROJ="$(cygpath -m "$PROJ")"
    ;;
  *) WIN=0 ;;
esac

# ---------- 定位 SDK ----------
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ]; then
  for c in "$LOCALAPPDATA/Android/Sdk" "$HOME/Android/Sdk" \
           "$HOME/Library/Android/sdk" "/usr/lib/android-sdk"; do
    if [ -d "$c" ]; then SDK="$c"; break; fi
  done
fi
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
  echo "错误：找不到 Android SDK，请设置环境变量 ANDROID_SDK_ROOT" >&2
  exit 1
fi

BT_DIR="${BUILD_TOOLS_VERSION:-34.0.0}"
BT="$SDK/build-tools/$BT_DIR"
AJAR="$SDK/platforms/android-34/android.jar"
if [ ! -d "$BT" ]; then
  echo "错误：缺少 build-tools;$BT_DIR，请用 sdkmanager 安装" >&2
  exit 1
fi
if [ ! -f "$AJAR" ]; then
  echo "错误：缺少 platforms;android-34，请用 sdkmanager 安装" >&2
  exit 1
fi

if [ "$WIN" = "1" ]; then
  AAPT2="$BT/aapt2.exe"
  D8="$BT/d8.bat"
  ZIPALIGN="$BT/zipalign.exe"
  APKSIGNER="$BT/apksigner.bat"
else
  AAPT2="$BT/aapt2"
  D8="$BT/d8"
  ZIPALIGN="$BT/zipalign"
  APKSIGNER="$BT/apksigner"
fi

# ---------- 定位 JDK ----------
if [ "$WIN" = "1" ] && [ -n "$JAVA_HOME" ]; then
  JAVA_HOME="$(cygpath -m "$JAVA_HOME" 2>/dev/null || echo "$JAVA_HOME")"
fi
if [ -z "$JAVA_HOME" ] || { [ ! -x "$JAVA_HOME/bin/javac" ] && [ ! -f "$JAVA_HOME/bin/javac.exe" ]; }; then
  JAVAC_PATH="$(command -v javac || true)"
  if [ -z "$JAVAC_PATH" ]; then
    echo "错误：找不到 javac，请设置 JAVA_HOME 或把 JDK 加进 PATH" >&2
    exit 1
  fi
  JAVA_HOME="$(cd "$(dirname "$JAVAC_PATH")/.." && pwd)"
fi
export JAVA_HOME

# 显式取路径，不依赖 PATH 拼接（Windows 下带空格的 JAVA_HOME 拼进 PATH 常常失效）
if [ "$WIN" = "1" ]; then
  JAVAC="$JAVA_HOME/bin/javac.exe"
  JAR="$JAVA_HOME/bin/jar.exe"
  KEYTOOL="$JAVA_HOME/bin/keytool.exe"
else
  JAVAC="$JAVA_HOME/bin/javac"
  JAR="$JAVA_HOME/bin/jar"
  KEYTOOL="$JAVA_HOME/bin/keytool"
fi

OUT="$PROJ/build"
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "[1/6] 编译资源"
"$AAPT2" compile --dir "$PROJ/res" -o "$OUT/res.zip"

echo "[2/6] 链接资源并生成 R.java"
"$AAPT2" link \
  -o "$OUT/base.apk" \
  -I "$AJAR" \
  --manifest "$PROJ/AndroidManifest.xml" \
  --java "$OUT/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  "$OUT/res.zip"

echo "[3/6] 编译 Java"
find "$PROJ/src" "$OUT/gen" -name "*.java" > "$OUT/sources.txt"
"$JAVAC" -encoding UTF-8 -source 8 -target 8 -nowarn \
  -bootclasspath "$AJAR" \
  -d "$OUT/classes" \
  @"$OUT/sources.txt"

echo "[4/6] 转 dex"
"$D8" --lib "$AJAR" --min-api 26 --release --output "$OUT/dex" \
  $(find "$OUT/classes" -name "*.class" | tr '\n' ' ')

echo "[5/6] 打包 dex"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
( cd "$OUT/dex" && "$JAR" uf "$OUT/unsigned.apk" classes.dex )

echo "[6/6] 对齐并签名"
if [ ! -f "$PROJ/keystore.jks" ]; then
  echo "  未找到 keystore.jks，生成一个自用签名（密码均为 autoscroll）"
  "$KEYTOOL" -genkeypair \
    -keystore "$PROJ/keystore.jks" -alias autoscroll \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass autoscroll -keypass autoscroll \
    -dname "CN=AutoScroll, OU=Personal, O=Personal, L=Local, ST=Local, C=CN"
fi
"$ZIPALIGN" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
rm -f "$OUT/AutoScroll.apk"
"$APKSIGNER" sign \
  --ks "$PROJ/keystore.jks" \
  --ks-pass pass:autoscroll \
  --key-pass pass:autoscroll \
  --out "$OUT/AutoScroll.apk" \
  "$OUT/aligned.apk"

echo "完成：$OUT/AutoScroll.apk"
