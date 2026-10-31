# CHANGELOG.md — DAON

형식: `[platform] version — 날짜 — 요약 (error_code / perf / cache 영향)`

## [android] 0.1.0 — 2026-10-31

### 구현
- 프로젝트 생성: Gradle 9.5.0, AGP 9.3.1, Kotlin 2.3.20, Compose BOM 2025.10.00 (minSdk 26 / compile·target 36)
- 다크 전용 custom 테마 토큰(`ui/theme/Tokens.kt`) — 오늘 강조 amber, 1dp hairline
- 시계(초 단위) + 이번 달 달력(월 이동) + Open-Meteo 현재/3일 예보
- 키오스크 셸: 가로 고정, 몰입형 전체화면, 화면 꺼짐 방지
- 설정 인앱 패널: 도시, 12/24시간, °C/°F, 언어(시스템/English/한국어)
- 런처 아이콘: 사용자 제공 세트(adaptive + mipmap mdpi~xxxhdpi + roundIcon)
- i18n en/ko 63키 + `scripts/check-i18n.sh` 키 패리티 검사
- error_code: `E-AND-NET-0001~0003`, `E-AND-STORE-0001`
- perf: `[PERF] cold_start` 로그 (프로세스당 1회)

### 수정 (검증 중 발견)
- API 28: `onDraw` 안에서 `removeOnDrawListener` 호출 → `IllegalStateException` 크래시 수정
- 설정: `ModalBottomSheet`(다이얼로그 창) → 인앱 우측 패널 — 키오스크에서 시스템 바가 다시 나타나던 문제 제거
- 언어 변경으로 Activity 재생성 시 cold_start 오탐(28s) → 프로세스당 1회로 제한
- 날씨 수동 새로고침 버튼 추가 (`label_refresh`, en/ko)

### 실기기 검증 (Fire HD 10 KFMAWI / Fire OS 7.3.1.2 / API 28)
- cold_start **1544ms** (예산 2000ms) · PSS **65.5MB** (예산 250MB) · 1920×1200 가로 몰입형
- 아이콘·설정 패널·언어 전환(en/ko) 동작 확인
- cache 영향 없음
