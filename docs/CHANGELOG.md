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

### 설정 확장·시계 3종 (T-018)
- 설정 패널: 자동 전환 켜기/끄기, 화면별 10/20/30/60초, 터치 동작 2종(대시보드 복귀/그 화면에서 멈추기)+안내, 시계 모양 3종, 글씨 크기 90/100/115%(기본 100), 날씨 움직임, °C/°F·12/24시간·언어·도시
- 글씨 배율은 `LocalDensity`의 fontScale에 곱해 전체 화면에 적용. 상단 바를 `heightIn(min)`으로 바꿔 115%에서도 제목·부제가 잘리지 않는다
- 시계 모양: **둥근 아날로그(기본)** — Canvas 60눈금·12숫자·시/분/초침, 12·3·6·9 강조 · 큰 디지털 · 혼합(아날로그+디지털)
- 새 파일 `core/ClockFace.kt`. `ClockScreen`이 설정값을 읽어 세 모양을 그린다
- 문자열 en/ko **128키 일치**, 테스트 14건 통과. 100%·115% 실기기 확인
- 캡처: `docs/screenshots/09-v0.2-clock-ko.png`(아날로그 기본), `10-v0.2-settings-ko.png`
- 검증: 자동 전환 끄기 유지, 화면별 시간 20초 적용(`CALENDAR -> WEATHER after 20s`), 시계 모양 3종 전환
- perf: 콜드 스타트 **1830ms**, 달력 로드 242ms

### 날씨 메인 완성 (T-019)
- 현재(기온·체감·습도·바람) + 강수확률·예상 강수량, 오늘 최고/최저, 7일 예보(최고/최저·강수확률·아이콘), 3시간 간격 8칸(기온·강수량·막대)
- 농사 참고 배너: 눈 > 비 > 바람 > 추위 > 더위 > 일교차 > 건조 > 맑음 우선순위, 강수확률·강수량·바람 수치 함께 표시
- 날씨 그림은 Canvas: 맑음·구름조금·흐림·안개·비·눈·천둥. `is_day`를 받아 밤에는 해 대신 달과 별
- 비·눈·천둥 효과: 64줄기/64송이/6.5초 주기 두 번 번쩍임. `날씨 움직임`을 끄면 미표시. Compose 애니메이션 대신 프레임 시계(`withFrameNanos`)로 그려 기기의 애니메이션 끄기 설정과 무관하게 동작
- 오프라인: 마지막 스냅샷 유지 + `오프라인 — 마지막 값` 배지·부제. 실패 시 `E-AND-NET-0003`
- `WeatherRepository`가 7일·시간별(3시간)·`is_day`를 파싱. `WeatherUiState.isOffline` 추가
- 새 파일 `ui/WeatherEffect.kt`. `WeatherScreen.kt` 전면 재작성
- 문자열 en/ko **135키 일치**, 테스트 14건 통과. 100%·115%·한/영 실기기 확인
- 캡처: `docs/screenshots/11-v0.2-weather-ko.png`(밤, 달), `12-v0.2-weather-en.png` — **파일 미저장**, T-021에서 촬영·갱신 예정(현재 `08-v0.2-weather-ko.png`는 1차 버전)
- 검증: `fetched days=7 hours=8`, 비/눈/천둥 화면 녹화 확인, 움직임 끄기 미표시, 오프라인 배지 표시
- perf: 콜드 스타트 **1830–1864ms**(예산 2000ms), 달력 로드 235–242ms

### 날씨 지점: 현재 위치 → 동네 검색 저장 (T-024)
- 지점 결정 순서: 기기 위치(프레임워크 `LocationManager`, GMS 불필요) → 설정에서 검색해 저장한 동네 → 도시 칩
- 위치 권한(`ACCESS_COARSE_LOCATION`)은 첫 실행 때 한 번만 요청. 거부·실패해도 앱은 계속 동작하고 설정에 이유와 현재 지점을 표시
- 설정 → `날씨 지점`: 동네 이름 검색(Nominatim, 1초 디바운스·초당 1회 준수), 결과 `사용`, 저장된 곳 `지우기`, `다시 찾기`
- 새 파일 `data/DeviceLocation.kt`(20초 fix 제한·24시간 캐시), `data/Place.kt`(`auto`/저장한 곳/도시), `data/PlaceSearch.kt`, `ui/PlaceLabel.kt`
- `WeatherRepository.fetch`가 도시 대신 위·경도를 받는다. 요청 URL 은 그대로 `forecast_days=7`
- 저장 값은 SharedPreferences `place_name`/`place_lat`/`place_lon`. 도시 칩을 고르면 저장한 곳은 지워진다
- 실기기 실측: 이 Fire HD 10 은 GPS 하드웨어가 없고 HERE 네트워크 위치도 좌표를 주지 않는다(`location_mode` 유지 불가, 백엔드 TLS 사설 CA 실패) → **저장한 동네가 기본**
- 검증: `chungju` 검색 → `[PLACE] search results=1` → `[SETTINGS] place saved` → `[WEATHER] fetch lat=36.991 lon=127.926`. 세 화면 모두 `충주시, 충청북도` 표시
- 문자열 en/ko 11키 추가(현재 162키 일치), 테스트 3건 추가(Nominatim 파싱) 총 23건 통과, 오류 코드 `E-AND-PERM-0001`
- perf: 콜드 스타트 **1849–1896ms**(예산 2000ms), 달력 로드 249ms

### 일정: 로컬 저장 + 기기 캘린더 (T-003, T-004)
- 날짜 상세 패널에 `일정` 구역: 그 날의 일정 목록, 한 줄 입력 + `추가`, 로컬 일정 `×` 삭제
- 달력 셀에 일정 점(7dp) 표시, 달력 아래 `오늘 일정` 스트립(최대 2건 + `N개 더`)
- 로컬 일정은 SharedPreferences `events` + JSON 한 덩어리(`EventJson`). 일정 수가 적어 Room 없이 충분하고, 깨진 항목은 건너뛴다
- 기기 캘린더는 `CalendarContract.Instances` 로 오늘 기준 ±12개월을 읽는다(읽기 전용, 출처 `기기 캘린더 · 이름` 표시). `READ_CALENDAR` 권한은 provider 가 있는 기기에서 한 번만 요청
- Fire OS 에는 캘린더 앱/provider 가 없어 상태가 `이 기기에는 캘린더 앱이 없습니다` 로 표시되고 **로컬 일정만** 동작한다
- 새 파일 `data/event/CalendarEvent.kt`, `EventJson.kt`, `EventStore.kt`, `DeviceCalendar.kt`. `DashboardViewModel`이 로컬+기기 일정을 합쳐 정렬
- 문자열 en/ko **190키 일치**, 테스트 4건 추가(JSON 왕복·깨진 항목·정렬) 총 **27건 통과**
- error_code: `E-AND-PERM-0002`(캘린더 읽기 실패)

### 키오스크 잠금·부팅 자동 시작 (T-008)
- 설정 → `키오스크`: `이 앱에 고정`(Lock Task/화면 고정) + `부팅 시 자동 시작`
- Device Owner 로 등록하면 확인 창 없이 고정되고, 아니면 Android 화면 고정으로 동작한다(처음 1회 확인)
- `AdminReceiver`(Device Owner 등록용) + `BootReceiver`(BOOT_COMPLETED, 설정을 켠 경우에만). 부팅 자동 시작은 Fire OS 가 막을 수 있어 실패 시 `E-AND-BOOT-0001` 로그만 남긴다
- 새 파일 `core/KioskMode.kt`, `admin/AdminReceiver.kt`, `admin/BootReceiver.kt`, `res/xml/device_admin.xml`
- Device Owner 등록 명령은 `docs/appstore/data_safety.md` 와 README 에 적었다
- error_code: `E-AND-KIOSK-0001`

### Appstore 메타데이터 (T-009)
- `docs/appstore/listing_ko.md`, `listing_en.md`: 앱 이름·짧은 설명·전체 설명·키워드·카테고리·스크린샷 순서
- `docs/appstore/data_safety.md`: 수집·이용 데이터, 권한 사유(위치·캘린더·부팅), 제3자(Open-Meteo·Nominatim), 아동 대상 아님
- 개인정보처리방침 초안 포함(위치·동네 검색어·캘린더 일정은 기기 밖으로 나가지 않음)

### v0.2.0-dev 마감 정리
- `versionCode 2`, `versionName 0.2.0-dev` 로 올림
- T-020 시계 3종은 코드·설정 반영 완료. 100/115%·ko/en 실기기 캡처는 사용자 확인 대기
- T-005(시간별 날씨)는 T-019에서 이미 구현되어 TODO 중복 항목을 정리했다
- 남은 검증: T-006(1시간 메모리), T-007(상시 표시), T-021(70대 3과제·PSS·60fps·캡처), T-023(연 1회 공휴일 갱신)

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
