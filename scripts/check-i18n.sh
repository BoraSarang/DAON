#!/bin/bash
# i18n 키 패리티 검사: values(영어) vs values-ko(한국어)
# DoD: 키 누락 0 (rules/i18n.md §6)
set -euo pipefail
cd "$(dirname "$0")/.."

EN="app/src/main/res/values/strings.xml"
KO="app/src/main/res/values-ko/strings.xml"

keys() {
    sed -n 's/.*<string name="\([^"]*\)".*/\1/p' "$1" | sort -u
}

en_keys=$(mktemp)
ko_keys=$(mktemp)
trap 'rm -f "$en_keys" "$ko_keys"' EXIT
keys "$EN" > "$en_keys"
keys "$KO" > "$ko_keys"

missing_ko=$(comm -23 "$en_keys" "$ko_keys" || true)
missing_en=$(comm -13 "$en_keys" "$ko_keys" || true)

status=0
if [ -n "$missing_ko" ]; then
    echo "❌ values-ko/strings.xml 에 빠진 키:"
    echo "$missing_ko" | sed 's/^/   - /'
    status=1
fi
if [ -n "$missing_en" ]; then
    echo "❌ values/strings.xml 에 빠진 키:"
    echo "$missing_en" | sed 's/^/   - /'
    status=1
fi

count=$(wc -l < "$en_keys" | tr -d ' ')
if [ "$status" -eq 0 ]; then
    echo "✅ i18n OK — en/ko 키 ${count}개 일치"
fi
exit "$status"
