# AGENTS.local.md — 프로젝트별 확장 규칙
> 위치: /Users/lee/Documents/Apps/DAON/AGENTS.local.md
> 이 파일은 프로젝트 특화 규칙만 기록. 공통 가이드는 재작성 금지.

## 1. 프로젝트 정보
- **프로젝트명**: **DAON** (한국어 표기: 다온) — 코드 식별자 `DAON` / 패키지 `daon`
- **플랫폼**: android
- **앱 성격**: **키오스크형 상시 표시(always-on) 대시보드** — 시계 + 달력 + 날씨 조합
  - 대상 기기: **Amazon Fire HD 10 (2019, 9세대 / Fire OS 7 = Android 9, API 28)**
  - 10.1" 1920×1200, 가로(landscape) 고정, 전체화면 몰입형, 화면 꺼짐 방지
  - 거치형(벽시계/탁상) 사용을 전제로 원거리 가독성 우선
- **기술 스택**: Kotlin + Jetpack Compose (Material 3), Coroutines, java.time, org.json
  - **GMS 미사용**: Fire OS 에는 Google Play Services 가 없다 → Firebase·FusedLocation·Play 결제 사용 금지
  - 선정 이유: 단일 Activity + Compose 로 60fps 상시 렌더링 유지, 의존성 최소화로 콜드 스타트 예산 확보
- **design_profile**: **custom** — 자체 토큰을 `docs/DESIGN.md`에 정의. 다크 우선(dark-first)
- **지원 언어**: en(기본) + ko — `res/values/strings.xml`, `res/values-ko/strings.xml`
- **작업 모드 기본값**: 정식

## 2. 번들ID / 앱 ID
- Android applicationId / namespace: `com.borasarang.daon`
- 정식 명칭 확정 시 함께 변경 검토(첫 배포 전까지). 배포 후 변경 금지.

## 3. 성능 예산 Override (필요 시)
- 변경점 없음 → `rules/budgets.json` 기본값 적용
- Android: Cold Start ≤2.0s, 메모리 ≤250MB, 60fps
- 추가: 상시 표시 앱이므로 **1시간 연속 실행 후 메모리 증가 ≤10MB** (누수 확인), 배터리 충전 상태 상시 가정

## 4. 프로젝트 특화 예외 규칙
- **API 28 하한**: Fire OS 7 은 Android 9(API 28). API 29+ 전용 API(동적 색상 Material You, predictive back, SplashScreen API 등)는 런타임 분기 없이 사용 금지.
- **dynamic color 금지**: design_profile=custom 이며 대상 기기가 API 28 이므로 dynamicLightColorScheme 계열을 쓰지 않는다.
- **몰입형 키오스크**: 시스템 바 숨김(immersive sticky) + `FLAG_KEEP_SCREEN_ON`. 완전 잠금(Lock Task)은 기기 소유자(Device Owner) 또는 화면 고정 사용자 승인 필요 → T-008.
- **네트워크**: 날씨는 Open-Meteo(키 불필요 HTTPS). API 키 하드코딩 금지. 위치 권한 없이 도시 수동 선택.
- **실기기 검증**: 대상 기기는 Fire HD 10. adb 연결 시 `adb -s <serial>` 필수(동시 연결 기기 존재). 검증 불가 시 에뮬레이터 API 28 1920×1200 가로로 대체하고 그 사실을 세션 로그에 남긴다.
  - 실측(2026-10-31): `KFMAWI` / Fire OS **7.3.1.2** / API **28** / 물리 1200×1920·240dpi(가로 1920×1200)
  - 확인된 것: 콜드 스타트 1544ms, PSS 65.5MB, 몰입형 전체화면, 런처 아이콘, 설정 패널, 언어 전환
  - 남은 것: 1시간 연속 실행 메모리(T-006), 완전 키오스크 잠금·부팅 자동 시작(T-008)
- **광고/추적 없음**: 키오스크 상시 표시 특성상 분석 SDK·광고 SDK 를 넣지 않는다.

## 5. 디자인 토큰 (custom 프로필)
- 색/타입/간격/형태 토큰은 `docs/DESIGN.md` 단일 출처. 본 파일에는 프로필 확정만 기록.
