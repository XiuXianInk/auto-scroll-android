#!/usr/bin/env bash
# 一键自测：Android Lint + 全部单元测试（逻辑 + Robolectric）
#
# 用法：
#   ANDROID_SDK_ROOT=/path/to/sdk bash run-checks.sh
#
# 说明：
#   逻辑测试与界面测试都在 test-jvm 这个 Gradle 工程里，
#   它们的源码目录指回 ../src 和 ../res，测的就是打包用的同一份代码。

set -e

PROJ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
  echo "错误：找不到 Android SDK，请设置 ANDROID_SDK_ROOT" >&2
  exit 1
fi

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) LINT="$SDK/cmdline-tools/latest/bin/lint.bat" ;;
  *) LINT="$SDK/cmdline-tools/latest/bin/lint" ;;
esac

echo "=========== 1/2  Android Lint ==========="
# lint 会把 --classpath 里的盘符当成分隔符，所以用项目内的相对路径引用 android.jar
cp "$SDK/platforms/android-34/android.jar" "$PROJ/aj.jar"
cd "$PROJ"
"$LINT" \
  --sdk-home "$SDK" \
  --sources src --resources res --classpath "aj.jar" \
  --text lint-out.txt . > /dev/null 2>&1 || true
tail -1 lint-out.txt
rm -f "$PROJ/aj.jar"

echo "=========== 2/2  单元测试 ==========="
cd "$PROJ/test-jvm"
if [ -f ./gradlew ]; then
  ./gradlew testDebugUnitTest --console=plain
else
  gradle testDebugUnitTest --console=plain
fi

echo "---------- 各测试类的通过情况 ----------"
for f in build/test-results/testDebugUnitTest/*.xml; do
  grep -aoE 'testsuite name="[^"]*" tests="[0-9]+" skipped="[0-9]+" failures="[0-9]+" errors="[0-9]+"' "$f"
done
