# PLAN_v0.1_android.md
> 생성일: 2026-10-31 | 플랫폼: android | 작성자: AI agent | 상태: **v0.1 구현·실기기 검증 완료**

## 1. 목표 (1줄)
Amazon Fire HD 10(2019)을 벽시계처럼 세워두는 **전체화면 키오스크 대시보드**: 큰 시계 + 이번 달 달력 + 현재/예보 날씨를 한 화면에 상시 표시한다.

## 2. 범위
- 플랫폼: android (Fire OS 7 / API 28, 1920×1200 가로)
- 기술 스택: Kotlin + Jetpack Compose(Material 3) + Coroutines + java.time + org.json
  - 선정 이유: 단일 화면 상시 렌더링에 Compose 가 적합하고, GMS 없이도 동작하는 최소 의존성 구성이 가능
- design_profile: custom (다크 우선, `docs/DESIGN.md`)
- 지원 언어: en(기본) + ko (`res/values/strings.xml`, `res/values-ko/strings.xml`)

### 포함 (v0.1)
1. **시계**: 시:분 대형 표시 + 초 + 요일/날짜, 12/24시간 설정
2. **달력**: 이번 달 6×7 그리드, 오늘 강조, 월 이동(이전/다음), 주 시작 요일 시스템 로케일
3. **날씨**: Open-Meteo(무키) 현재 기온·체감·습도·풍속 + 3일 예보, 15분 주기 갱신, 수동 새로고침, 실패 시 마지막 성공값 유지 + 오류 표시
4. **키오스크 셸**: 가로 고정, 전체화면 몰입형(시스템 바 숨김), 화면 꺼짐 방지, 다크 테마
5. **설정 패널(인앱)**: 도시 선택, 12/24시간, 언어(시스템/English/한국어), 날씨 단위(°C/°F)
6. **i18n**: en/ko 키 패리티 스크립트 `scripts/check-i18n.sh`

### 제외 (v0.1 이후)
- 일정(이벤트) CRUD 및 기기 캘린더 연동 → T-003
- 완전 키오스크 잠금(Lock Task/Device Owner), 부팅 자동 시작 → T-008
- 상세 날씨(시간별 그래프·강수 확률), 위젯 → T-005
- Amazon Appstore 등록 메타데이터 → T-009
- Fire HD 8/기타 기기 최적화 (레이아웃은 반응형으로 두되 검증은 HD 10만)

### 전제 체인 (깨지는 지점)
- Fire OS 에는 GMS 가 없음 → 위치·Play 결제·Firebase 금지, 도시 수동 선택으로 우회
- Fire OS 7 = API 28 → 동적 색상/최신 API 사용 불가
- 실기기 검증 완료(2026-10-31): Fire HD 10 KFMAWI / Fire OS 7.3.1.2 / API 28. 1시간 상시 실행은 미측정(T-006)
- 날씨는 네트워크 의존 → 오프라인 시 시계/달력은 계속 동작해야 함(부분 실패 허용)

## 3. 문서 위치
- PLAN: 본 문서
- TODO: `docs/TODO.md` (T-001~)
- DESIGN: `docs/DESIGN.md`
- API: `docs/api/ENDPOINTS.md` (Open-Meteo 요청/응답 필드 기록)
- CHANGELOG: `docs/CHANGELOG.md`

## 4. 성능 예산
- `rules/budgets.json` 기본값: Cold Start ≤2.0s, 메모리 ≤250MB, 60fps
- 초시계 갱신은 1초 주기 단일 코루틴. 날씨 갱신은 15분 + 수동.
- 1시간 연속 실행 메모리 증가 ≤10MB

## 5. 에러 코드
- 범위: `E-AND-NET-0001~` (날씨 네트워크/파싱), `E-AND-STORE-0001~` (설정 저장), `E-AND-PERM-0001~` (권한, v0.1 은 해당 없음)
- 사용자 메시지: `res/values/strings.xml` / `values-ko/strings.xml` 의 `error_*` 키

## 6. 빌드 & 검증 계획
- 빌드: `./build_and_run.sh debug android` (내부 `./gradlew assembleDebug`)
- 테스트: `./gradlew test` — ErrorCode 형식(3), 달력 그리드(4). WMO 매핑은 리소스 참조라 JVM 단위 테스트에서 제외
- i18n: `scripts/check-i18n.sh` (키 패리티 63키)
- 검증 항목: 콜드 스타트 로그 `[PERF] cold_start`, ERROR 0, 가로/전체화면, 1시간 메모리
- 실기기: Fire HD 10 (KFMAWI, Fire OS 7.3.1.2) — §9 실측표

## 7. 예외 규칙 (있으면)
- 없음. 공통 규칙과 충돌하는 요청 발생 시 `docs/DESIGN.md` §예외에 1줄 기록.

## 8. 마일스톤
| # | 내용 | 산출물 |
|---|------|--------|
| M1 | 스캐폴드 + 키오스크 셸 + 시계 | 빌드 성공 APK, 실기기 캡처 |
| M2 | 달력 그리드 + 월 이동 | 단위 테스트 통과 |
| M3 | 날씨 연동 + 설정 패널 + i18n 패리티 | 오프라인/온라인 동작 확인 |
| M4 | 실기기(Fire HD 10) 검증 + 1시간 상시 실행 점검 | 세션 로그, 스크린샷 |

## 9. 실측 결과 (2026-10-31, Fire HD 10 KFMAWI / Fire OS 7.3.1.2 / API 28)
| 항목 | 예산 | 실측 | 판정 |
|---|---|---|---|
| Cold start | ≤2.0s | **1.54s** (앱 재실행 1.60~1.63s) | 통과 |
| PSS 메모리 | ≤250MB | **65.5MB** | 통과 |
| 화면 | 1920×1200 가로 | 1920×1200, 시스템 바 숨김 | 통과 |
| i18n | en/ko 키 누락 0 | 63키 일치 | 통과 |
| 단위 테스트 | - | 7건 통과 (달력 4, 에러코드 3) | 통과 |
| 1시간 상시 실행 | +10MB 이하 | 미측정 | T-006 |

