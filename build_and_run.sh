#!/bin/bash
# usage: ./build_and_run.sh [debug|build|test|lint|clean|install] [--offline]
# AGENTS.md 빌드 디스패처 — DAON (android)
#
# set -o pipefail 이 없으면 `gradle … | tail` 의 종료코드는 tail(0) 이라 실패가 조용히 통과한다.
set -eo pipefail

CMD="${1:-debug}"
OFFLINE=""
for a in "$@"; do [ "$a" = "--offline" ] && OFFLINE="--offline"; done
APK_DIR="app/build/outputs/apk/debug"

# JDK: Android Studio 내장 JBR 우선 (Temurin 27 은 AGP 9 검증 범위 밖일 수 있다)
if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
elif [ -z "${JAVA_HOME:-}" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
fi
[ -n "${JAVA_HOME:-}" ] && echo "JAVA_HOME=$JAVA_HOME"

pick_device() {
    if [ -n "${TARGET_DEVICE:-}" ]; then
        echo "$TARGET_DEVICE"
        return
    fi
    local count
    count=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
    if [ "$count" -eq 1 ]; then
        adb devices | awk 'NR>1 && $2=="device"{print $1; exit}'
    fi
}

newest_apk() {
    local apk=""
    for f in "$APK_DIR"/*.apk; do
        [ -e "$f" ] || continue
        if [ -z "$apk" ] || [ "$f" -nt "$apk" ]; then apk="$f"; fi
    done
    echo "$apk"
}

case "$CMD" in
  debug|build)
    echo "🔨 assembleDebug (offline='${OFFLINE}')"
    ./gradlew assembleDebug $OFFLINE 2>&1 | tail -12

    APK=$(newest_apk)
    if [ -z "$APK" ]; then
        echo "❌ APK 생성 실패"; exit 1
    fi
    # APK 가 소스보다 오래됐다면 빌드 실패를 숨겼을 수 있다.
    STALE=$(find app/src/main app/build.gradle.kts build.gradle.kts settings.gradle.kts \
        gradle.properties gradle/libs.versions.toml -type f -newer "$APK" -print -quit 2>/dev/null)
    if [ -n "$STALE" ]; then
        echo "❌ APK 이 소스보다 오래됐다: $APK"
        echo "    최신 소스 예: $STALE"; exit 1
    fi
    echo "✅ 빌드 완료: $APK ($(du -h "$APK" | cut -f1))"

    DEVICE=$(pick_device)
    if [ -n "$DEVICE" ]; then
        echo "📲 $DEVICE 에 설치 중…"
        adb -s "$DEVICE" install -r "$APK" 2>&1 | tail -1
    else
        echo "⚠️  설치 생략 — 기기 1대가 아니거나 TARGET_DEVICE 미지정"
        echo "    예: TARGET_DEVICE=192.168.86.30:5555 ./build_and_run.sh debug android"
    fi
    ;;
  test)
    echo "🧪 단위 테스트"
    ./gradlew test $OFFLINE 2>&1 | tail -15
    ;;
  lint)
    echo "🔎 lint"
    ./gradlew lintDebug $OFFLINE 2>&1 | tail -15
    ;;
  install)
    APK=$(newest_apk)
    [ -n "$APK" ] || { echo "❌ APK 없음 — 먼저 build"; exit 1; }
    DEVICE=$(pick_device)
    [ -n "$DEVICE" ] || { echo "❌ 대상 기기 미지정 (TARGET_DEVICE)"; exit 1; }
    adb -s "$DEVICE" install -r "$APK"
    ;;
  clean)
    ./gradlew clean
    ;;
  *)
    echo "usage: $0 [debug|build|test|lint|clean|install] [--offline]"; exit 2
    ;;
esac
