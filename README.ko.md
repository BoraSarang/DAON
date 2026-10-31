# DAON (다온)

**English** · [한국어](README.ko.md)

**Amazon Fire HD 10 (2019)** 태블릿을 벽시계처럼 세워두는 **전체화면 상시 표시(키오스크) 대시보드**입니다. 큰 시계, 이번 달 달력, 실시간 날씨를 어두운 한 화면에 담았고, 2~3m 거리에서도 읽히도록 설계했습니다.

## 상태

초기 스캐폴드(v0.1.0) — **대상 기기에서 검증 완료**: Fire HD 10 (2019), Fire OS 7.3.1.2(API 28), 1920×1200 가로, 몰입형 전체화면.

| 항목 | 예산 | 실측 |
|---|---|---|
| 콜드 스타트 | ≤ 2.0s | 1.54s |
| 메모리(PSS) | ≤ 250MB | 65.5MB |

## 스크린샷

| 한국어 | English | 설정 |
|---|---|---|
| ![ko](docs/screenshots/01-dashboard-ko.png) | ![en](docs/screenshots/02-dashboard-en.png) | ![설정](docs/screenshots/03-settings-ko.png) |


## 대상 기기

| 항목 | 값 |
|---|---|
| 기기 | Amazon Fire HD 10 (2019, 9세대) |
| OS | Fire OS 7 (Android 9, API 28) |
| 화면 | 1920×1200, 가로, 상시 켜짐 |
| Google 서비스 | **없음** (GMS 미탑재) |

Google Play Services 에 의존하지 않습니다. 날씨는 [Open-Meteo](https://open-meteo.com/)(키 불필요)를 쓰고, 위치 권한 대신 도시를 직접 선택합니다.

## 기능 (v0.1)

- 시계: 큰 HH:mm + 초, 요일·날짜, 12/24시간 선택
- 달력: 이번 달 그리드, 오늘 강조, 이전/다음 달 이동
- 날씨: 현재 기온·체감·습도·풍속 + 3일 예보, 15분 주기 갱신
- 키오스크 셸: 가로 고정, 몰입형 전체화면, 화면 꺼짐 방지
- 설정 시트: 도시, 12/24시간, °C/°F, 언어(시스템 / English / 한국어)

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

- `docs/plans/PLAN_v0.1_android.md` — 범위·예산·마일스톤
- `docs/DESIGN.md` — 디자인 토큰·키오스크 레이아웃
- `docs/TODO.md` — 후속 작업
- `docs/CHANGELOG.md` — 변경 기록

## 라이선스

비공개 프로젝트. 모든 권리 보유.
