# CHANGELOG.md — DAON

형식: `[platform] version — 날짜 — 요약 (error_code / perf / cache 영향)`

## [android] 0.1.0 — 2026-10-31 — 스캐폴드 + 키오스크 대시보드 v0.1
- 프로젝트 생성: Gradle 9.5.0, AGP 9.3.1, Kotlin 2.3.20, Compose BOM 2025.10.00
- minSdk 26 / compileSdk 36 / targetSdk 36 (Fire OS 7 = API 28 동작 목표)
- 다크 전용 custom 테마 토큰(`ui/theme/Tokens.kt`) — 오늘 강조 amber, 1dp hairline
- 시계(초 단위) + 이번 달 달력(월 이동) + Open-Meteo 현재/3일 예보
- 몰입형 전체화면 + 가로 고정 + 화면 꺼짐 방지
- 설정 시트: 도시, 12/24시간, °C/°F, 언어(시스템/English/한국어)
- i18n en/ko + `scripts/check-i18n.sh` 키 패리티 검사
- error_code: E-AND-NET-*, E-AND-STORE-*
- perf: 콜드 스타트 `[PERF] cold_start` 로그 추가 (예산 2.0s)

## [android] 0.1.0 — 2026-10-31 — 실기기 검증 보강
- 런처 아이콘: 사용자 제공 아이콘 세트 적용 (adaptive foreground/background + mipmap mdpi~xxxhdpi + roundIcon)
- 설정 UI: ModalBottomSheet → 인앱 우측 패널로 변경 — 다이얼로그 창이 시스템 바를 다시 노출하던 문제 제거
- 날씨 수동 새로고침 버튼 추가 (`label_refresh`, en/ko)
- `onDraw` 안에서 `removeOnDrawListener` 호출 크래시 수정 (API 28 `IllegalStateException`)
- cold_start 로그를 프로세스당 1회로 제한 (언어 변경 재생성 시 28초 오탐 제거)
- 실기기 실측 (Fire HD 10, KFMAWI / Fire OS 7.3.1.2 / API 28):
  - cold_start **1544ms** (예산 2000ms) · PSS **65.5MB** (예산 250MB) · 1920×1200 가로 몰입형 확인
  - 아이콘·설정 패널·언어 전환(en/ko) 동작 확인
- error_code 추가 없음 / cache 영향 없음

