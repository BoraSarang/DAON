# DAON (다온)

**English** · [한국어](README.ko.md)

**Amazon Fire HD 10 (2019)** 태블릿을 벽시계처럼 세워두는 **전체화면 상시 표시(키오스크) 대시보드**입니다. 큰 시계, 이번 달 달력, 실시간 날씨를 어두운 한 화면에 담았고, 2~3m 거리에서도 읽히도록 설계했습니다.

## 상태

**v0.2.0-dev** — 달력 중심 재설계 구현 완료, 실기기 검증 진행 중(T-006·T-007·T-021). v0.1 기준은 대상 기기에서 검증했습니다: Fire HD 10 (2019), Fire OS 7.3.1.2(API 28), 1920×1200 가로, 몰입형 전체화면. 제품명: **DAON(다온)**.

| 항목 | 예산 | v0.1 | v0.2-dev |
|---|---|---|---|
| 콜드 스타트 | ≤ 2.0s | 1.54s | 1.85–1.90s |
| 메모리(PSS) | ≤ 250MB | 65.5MB | 약 94MB (실행 중) |

## v0.2 — 농사 달력 키오스크

달력 70% + 시계·날씨 30%, 음력·한국 공휴일·24절기·농사일, 달력 → 날씨 → 시계 자동 전환. 날짜를 누르면 520dp 패널에 절기·농사일·공휴일·일정을 보여 줍니다. 날씨는 현재·3시간별·7일 예보와 강수확률, 비·눈·천둥 효과를 담았습니다. 시계는 둥근 아날로그·큰 디지털·혼합 중 고를 수 있습니다.

일정은 태블릿에서 직접 추가할 수 있고, **양력/음력 날짜 기준**과 **매주·매월·매년 반복**을 고를 수 있습니다(음력 일정은 매년 같은 음력 날짜에 반복). 기기에 캘린더 provider 가 있으면 기기 일정을 읽기 전용으로 함께 표시합니다. 달력 홈의 시계·날씨 카드를 누르면 해당 화면으로 이동하고, 그 화면의 `달력` 버튼으로 대시보드에 돌아옵니다(`그 화면에서 멈추기` 모드에서도). 설정의 키오스크 항목에서 화면 고정과 부팅 자동 시작을 켤 수 있습니다.

- 상호작용 목업: [`docs/mockups/v0.2/index.html`](docs/mockups/v0.2/index.html)
- v0.2 캡처: [`docs/screenshots/04-v0.2-dashboard-ko.png`](docs/screenshots/04-v0.2-dashboard-ko.png) … `10-v0.2-settings-ko.png`
- 로컬 일정: SharedPreferences JSON, 매주·매월·매년 반복과 음력 날짜 기준. 기기 캘린더: `READ_CALENDAR`, 오늘 ±12개월. Fire OS 7 에도 캘린더 provider 가 있어 권한을 허용하면 표시된다(계정이 없으면 0건).
- 키오스크: Android 화면 고정. Device Owner 등록은 선택(`adb shell dpm set-device-owner com.borasarang.daon/.admin.AdminReceiver`).

## 스크린샷

| v0.2 달력 | 날짜 상세 | 날씨 | 시계 | 설정 |
|---|---|---|---|---|
| ![달력](docs/screenshots/04-v0.2-dashboard-ko.png) | ![상세](docs/screenshots/05-detail-term-ko.png) | ![날씨](docs/screenshots/08-v0.2-weather-ko.png) | ![시계](docs/screenshots/09-v0.2-clock-ko.png) | ![설정](docs/screenshots/10-v0.2-settings-ko.png) |

v0.1: ![ko](docs/screenshots/01-dashboard-ko.png) · ![en](docs/screenshots/02-dashboard-en.png)


## 대상 기기

| 항목 | 값 |
|---|---|
| 기기 | Amazon Fire HD 10 (2019, 9세대) |
| OS | Fire OS 7 (Android 9, API 28) |
| 화면 | 1920×1200, 가로, 상시 켜짐 |
| Google 서비스 | **없음** (GMS 미탑재) |

Google Play Services 에 의존하지 않습니다. 날씨는 [Open-Meteo](https://open-meteo.com/)(키 불필요)를 씁니다. 기기가 위치를 알려 주면 그 자리를 쓰고, 못 찾으면 설정에서 한 번 검색해 저장한 동네를 씁니다([Nominatim](https://nominatim.openstreetmap.org/)). Fire HD 10(2019)은 GPS 가 없고 네트워크 위치도 좌표를 주지 않아, 이 기기에서는 저장한 동네가 날씨 지점이 됩니다.

## 기능

- 시계: 큰 시각 + 초, 요일·날짜, 12/24시간 선택, 둥근 아날로그·큰 디지털·혼합
- 달력: 음력·한국 공휴일·24절기·농사일이 함께 보이는 월 그리드, 오늘 강조, 이전/다음 달 이동
- 날짜 상세: 520dp 패널에 절기·농사일·공휴일·그날의 일정
- 일정: 날짜별 로컬 일정 추가·삭제, 양력/음력 기준과 매주·매월·매년 반복, 기기 캘린더 provider 가 있으면 기기 일정 읽기 전용 표시
- 날씨: 현재·체감·습도·바람·강수확률, 3시간별·7일 예보, 농사 참고, 비·눈·천둥 효과, 15분 주기 갱신
- 자동 전환: 달력 → 날씨 → 시계, 화면별 머무는 시간·터치 동작 설정, 카드 터치로 해당 화면 이동, 날씨·시계의 `달력` 버튼으로 홈 복귀
- 키오스크 셸: 가로 고정, 몰입형 전체화면, 화면 꺼짐 방지, 화면 고정·부팅 자동 시작 선택
- 설정 패널(인앱): 날씨 지점(기기 위치 또는 저장한 동네), 일정, 시계 모양, 글씨 크기, 자동 전환, °C/°F, 12/24시간, 언어(시스템 / English / 한국어)

## 요구 사항

- JDK 17+ (Temurin 27 로 빌드 확인)
- Android SDK: platform 36, build-tools 36
- Gradle wrapper 9.5.0 (저장소 포함)

## 빌드

```bash
./gradlew assembleDebug
# 또는
./build_and_run.sh debug android
```

연결된 기기에 설치:

```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## 테스트

```bash
./gradlew test
scripts/check-i18n.sh
```

## 문서

- `docs/plans/PLAN_v0.1_android.md` — v0.1 범위·예산·마일스톤
- `docs/plans/PLAN_v0.2_android.md` — v0.2 달력 재설계(구현 완료)
- `docs/mockups/v0.2/` — 상호작용 목업·캡처
- `docs/DESIGN.md` — 디자인 토큰·키오스크 레이아웃
- `docs/appstore/` — Amazon Appstore 등록 문안(en/ko)·Data safety
- `docs/TODO.md` — 후속 작업
- `docs/CHANGELOG.md` — 변경 기록

## 라이선스

비공개 프로젝트. 모든 권리 보유.
