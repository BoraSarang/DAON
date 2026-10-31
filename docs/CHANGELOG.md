# CHANGELOG.md — DAON

형식: `[platform] version — 날짜 — 요약 (error_code / perf / cache 영향)`

## [android] 0.2.0-dev — 2026-10-31

### 설계 제안·목업 (T-013)
- v0.2 목표: 70대 어르신용 **농사 달력** 키오스크 — 달력 70% / 시계·날씨 30%
- 3화면 로테이션: 달력 30초 → 날씨 20초 → 시계 20초 (화면별 설정), 터치 동작 `대시보드로 복귀`(기본) / `그 화면에서 멈추기`
- 달력: 음력·한국 공휴일·24절기·농사일, 날짜 클릭 상세 패널
- 날씨: 현재/시간별/7일/농사 참고 + 비·눈·천둥 효과
- 시계: 둥근 아날로그(기본)·큰 디지털·혼합 선택
- 목업: `docs/mockups/v0.2/index.html` + 캡처 7장
- 계획: `docs/plans/PLAN_v0.2_android.md`, TODO T-013~T-023
- perf/error_code/cache 영향: 없음(설계 단계)

### 달력 데이터 자산 (T-014)
- `app/src/main/assets/calendar/` 번들 약 145KB (시작 시 1회 로드 예산 <300KB)
  - `lunar_2025_2040.json` 68KB — 5,844일 음력 연·월·일·윤달 (v2 압축 문자열, 날짜 차이 인덱스)
  - `solar_terms_2025_2040.json` 49KB — 24절기 384개(날짜·시각·한자·황경)
  - `holidays_2026_2027.json` 11KB — 확정 공휴일 46일 ko/en
  - `term_details_ko/en.json` 15.8KB — 24절기 설명·농사일 각 3개
- 생성기 `scripts/gen-calendar-data.py` + 원본 `scripts/calendar-src/` (재생성 `--check` 지원)
- 2026년 24절기는 우주항공청 월력요항과 분 단위 일치, 그 외 연도는 PyEphem 계산(±2분 대조)
- 공휴일은 월력요항 확정 값만 사용(대체공휴일 규칙 추정 없음). 노동절·제헌절 포함, 2027년 대체공휴일 반영
- 테스트 6건 추가 `CalendarAssetsTest` — 총 13건 통과
- 문서 `docs/api/CALENDAR_DATA.md` (출처·형식·검증·T-023 갱신 절차)
- perf/error_code/cache 영향: APK +145KB, 시작 시 자산 읽기+파싱 230ms(IO 스레드)

### 달력 홈 재설계 (T-015)
- 달력 70% / 오른쪽 시계·날씨 30%, 1920×1200 기준. 상단 바에 오늘 날짜·음력·활성 절기 요약
- 달력 셀: 양력 31sp(47px)·음력 17sp(25px)·공휴일/절기 14sp(21px), 6주 고정 그리드, 인접 월 32% 흐림
- 일요일·공휴일 빨강, 토요일 파랑, 오늘은 액센트 배경 + 진한 글자, 선택 셀 테두리
- 월 이동 `‹ ›` + `오늘` 버튼, 하단 농사 스트립(활성 절기 농사일 1건)
- 시계 카드 기본 **12시간제**(오전/오후), 설정에서 24시간제 전환
- 날씨 카드: 현재·체감·습도·바람·일교차·농사 참고 한 줄(눈/비/바람/추위/더위/일교차/건조/맑음 우선순위) + 3일 예보
- 새 파일: `CalendarPanel.kt`, `HomeTokens.kt`, `CalendarData.kt`, `CalendarRepository.kt`. `SettingsPanel.kt` 분리
- 문자열 en/ko 83키 일치, 테스트 14건 통과
- perf/error_code/cache 영향: 콜드 스타트 **1781–1825ms**(예산 2000ms, v0.1 1544ms), 달력 로드 230ms
- error_code: `E-AND-DATA-0001` (달력 자산 로드 실패 시 달력 없이 계속 동작)

### 날짜 상세 패널 (T-016)
- 달력 날짜 터치 → 오른쪽 **520dp 인앱 패널**(다이얼로그 없음). 스크림·닫기·뒤로가기로 닫힘
- 절기 당일: 절기명·한자·설명·농사일 3개, 이달의 절기 칩(해당 절기 강조), 이달의 공휴일
- 절기가 아닌 날: `지금 절기`로 가장 최근 절기의 설명·농사일 표시
- 공휴일: 이름·설명·이달의 공휴일, 일반 날짜: 음력·요일 안내와 절기 안내 문구
- 새 파일 `DateDetailPanel.kt`, 토큰 12종, 문자열 12키(en/ko **95키 일치**)
- 캡처: `docs/screenshots/05-detail-term-ko.png`, `06-detail-holiday-ko.png`, `07-detail-date-ko.png`
- perf 영향: 없음(터치 시에만 구성). 테스트 14건 유지

### 3화면 로테이션 (T-017)
- 달력 → 날씨 → 시계 자동 전환. 기본 30/20/20초, 화면별 10/20/30/60초 설정값을 `AppPrefs`에서 읽는다
- 250ms 크로스페이드. 화면 점 3개로 현재 위치 표시, 세 화면 모두 같은 위치의 설정 버튼
- 터치 동작: 기본은 `대시보드로 복귀`(날씨·시계에서 화면을 누르면 달력 + 타이머 리셋), 설정 시 `그 화면에서 멈추기`(첫 터치 정지, 두 번째 터치 재개)
- 설정 패널·날짜 상세 패널이 열려 있으면 자동 전환 정지. 뒤로가기로 패널 닫기/달력 복귀
- `Material3 Surface`가 포인터를 소비해 뒤쪽 배경 레이어가 터치를 못 받던 문제 → 루트 `pointerInput`으로 변경
- 날씨 메인(1차): 현재·오늘·일교차·체감·습도·바람·농사 참고·4일 예보, 업데이트 시각 표시
- 시계 메인(1차): 큰 디지털 시계·초·요일·음력·절기·현재 날씨. T-020에서 아날로그/혼합 선택 추가
- 새 파일 `ScreenMode.kt`, `ScreenChrome.kt`, `WeatherScreen.kt`, `ClockScreen.kt`. `MainActivity`는 `DashboardScreen(vm)`만 호출
- 문자열 en/ko **104키 일치**, 테스트 14건 통과, 한/영 실기기 확인
- 캡처: `docs/screenshots/08-v0.2-weather-ko.png`, `09-v0.2-clock-ko.png`
- perf: 콜드 스타트 **1856ms**(예산 2000ms), 달력 로드 235ms

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
