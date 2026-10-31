# TODO.md — DAON

> 상태: `[ ]` 대기 · `[~]` 진행 · `[x]` 완료 · `[!]` 블로커

## v0.1 (M1~M4)
- [x] T-001 프로젝트 스캐폴드 (Gradle 9.5.0 / AGP 9.3.1 / Kotlin 2.3.20 / Compose BOM 2025.10.00, minSdk 26, compile·target 36)
- [x] T-002 문서 세트: AGENTS.local, PLAN, DESIGN, TODO, CHANGELOG, README(en/ko)
- [x] T-011 런처 아이콘 적용 (사용자 제공 세트, adaptive + round)
- [x] T-012 실기기 기본 검증 (Fire HD 10 KFMAWI: 전체화면·가로·콜드스타트 1544ms·PSS 65.5MB·아이콘·언어 전환)
- [ ] T-003 일정(이벤트) 로컬 저장 + 오늘의 일정 표시 (v0.1.1, 저장 방식 결정 필요: SharedPreferences JSON vs Room)
- [ ] T-004 기기 캘린더 연동 (READ_CALENDAR, Fire OS 캘린더 앱 부재 시 비활성 처리)
- [ ] T-005 시간별 날씨 막대 + 강수 확률
- [ ] T-006 1시간 상시 실행 메모리 측정 (`adb shell dumpsys meminfo`, 1회 측정값 65.5MB)
- [ ] T-007 실기기 1시간 상시 표시 + 화면 꺼짐/재개 점검
- [ ] T-008 키오스크 잠금: Lock Task / Device Owner(또는 화면 고정 안내) + 부팅 자동 시작
- [ ] T-009 Amazon Appstore 메타데이터(en/ko) + Data safety
- [x] T-010 앱 정식 명칭 확정: **DAON(다온)**, applicationId `com.borasarang.daon` (2026-10-31)

## 알려진 제약
- 실기기 검증 완료(Fire HD 10 KFMAWI · Fire OS 7.3.1.2 · API 28). 1시간 상시 실행은 T-006.
- Fire OS 에 GMS 없음: 위치 자동 감지 대신 도시 선택.
- 날씨 API 는 네트워크 필요: 오프라인 시 마지막 성공값 + 오류 배지.
- 런처 아이콘 워드마크가 아이콘 대비 작게 보임(제공 세트 그대로 적용). 확대가 필요하면 foreground 스케일 조정.
