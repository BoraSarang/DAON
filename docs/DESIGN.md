# DESIGN.md — DAON(다온) 디자인 시스템
> design_profile: **custom** · 플랫폼: android · 다크 우선(dark-first)
> 대상: Fire HD 10 (2019) 1920×1200 가로, 거치형 상시 표시

## 1. 원칙
- **원거리 가독성**: 1~3m 거리에서 읽힌다. 본문 최소 18sp, 핵심 수치는 72sp 이상.
- **무소음(quiet)**: 장식 최소. 그림자·그라데이션 없음. 위계는 크기·굵기·1dp 선으로 만든다.
- **정보 3분할**: 왼쪽 = 시간, 가운데 = 달력, 오른쪽 = 날씨. 시선이 한 번에 흐른다.
- **색은 의미만**: 강조색 1개(amber)와 상태색(ok/warn/danger)만. 배경은 거의 검정.
- 시스템 UI(권한·다이얼로그·설정 시트)는 Material 3 기본 규칙 유지.
- 최소 접근성: 본문 대비 AA 이상, 터치 타깃 48dp 이상, 텍스트 크기 조절 대응(고정 dp 대신 sp).

## 2. 토큰 (v0.1)
단일 출처는 `app/src/main/java/com/borasarang/androidapp/ui/theme/Tokens.kt`.

### 색 (다크)
| 토큰 | 값 | 용도 |
|------|-----|------|
| `color.bg` | `#0B0D10` | 전체 배경 |
| `color.surface` | `#12151A` | 패널/시트 |
| `color.surfaceAlt` | `#171B21` | 선택/강조 면 |
| `color.text.primary` | `#F2F4F7` | 시계·기온·날짜 |
| `color.text.secondary` | `#A7AFBB` | 설명·예보 |
| `color.text.muted` | `#6E7683` | 라벨·비활성 |
| `color.rule` | `#232932` | 1dp 구분선 |
| `color.accent` | `#E8B45A` | 오늘 강조·강조 포인트(amber) |
| `color.accentSoft` | `#26E8B45A` | 오늘 셀 배경 |
| `color.ok` | `#57C99B` | 정상/갱신 성공 |
| `color.danger` | `#E2695C` | 오류/오프라인 |

- 라이트 테마는 v0.1 범위 아님(키오스크 상시 표시). 토큰 구조는 라이트 확장 가능하게 유지.
- 대비: `text.primary` on `bg` ≈ 16:1, `text.secondary` on `bg` ≈ 8:1.

### 타이포그래피
| 토큰 | 값 | 용도 |
|------|-----|------|
| `type.clock` | 132sp / W300 / tabular | 시:분 |
| `type.clockSmall` | 40sp / W300 / tabular | 초·AM/PM |
| `type.temp` | 88sp / W300 / tabular | 현재 기온 |
| `type.date` | 30sp / W500 | 요일·날짜 |
| `type.section` | 15sp / W600 / +0.12em / 대문자 | 섹션 라벨 |
| `type.body` | 20sp / W400 | 예보·설명 |
| `type.caption` | 16sp / W400 | 보조 정보 |
| `type.num` | tabular-nums (`fontFeatureSettings = "tnum"`) | 모든 수치 |

- v0.1 은 시스템 폰트(Roboto) 사용. 커스텀 폰트는 `res/font` 추가로 교체(토큰만 바꾼다).
- 표는 숫자 정렬을 위해 반드시 tabular 숫자를 쓴다.

### 간격 / 형태
| 토큰 | 값 | 용도 |
|------|-----|------|
| `space.xs/sm/md/lg/xl/xxl` | 4/8/16/24/32/48dp | 4dp 배수 |
| `screen.padding` | 40dp | 화면 외곽 |
| `panel.gap` | 32dp | 패널 사이 |
| `radius.sm/md/lg` | 10/16/24dp | 셀·시트·강조 |
| `stroke.hairline` | 1dp | 구분선 (그림자 대신) |
| `target.min` | 48dp | 터치 타깃 |

### 레이아웃 (1920×1200 기준)
```
┌──────────────────────────────────────────────────────────────┐
│ 2026년 10월 31일 토요일                    Seoul · 갱신 1분 전 ⚙ │
│                                                              │
│  14:08      ┌ 이번 달 ─────────────┐   ┌ 지금 날씨 ────────┐ │
│   :32       │  월  화  수  목  금  토  일 │  18°  맑음        │ │
│             │  … 6×7 그리드 …        │  체감 17° · 습도 42% │ │
│  토요일      │                        │  바람 2.1 m/s        │ │
│             └────────────────────────┘   ── 3일 예보 ──    │ │
│  (일정 없음)                              11/1  19°/9°       │ │
│                                           11/2  17°/8°       │ │
└──────────────────────────────────────────────────────────────┘
```
- 좌 34% : 시계 / 중 40% : 달력 / 우 26% : 날씨 (1920 기준, 태블릿 세로 회전 없음)
- 창 크기가 달라져도 비율 유지. 최소 1024×768 에서 레이아웃이 겹치지 않아야 한다.

## 3. 아이콘
- 사용자 제공 세트(`daon_android_icons.zip`) 적용. 크림 배경 + 검정 워드마크 + 노란 점(·).
- adaptive: `drawable-xxxhdpi/ic_launcher_background.png`, `ic_launcher_foreground.png` (1080×1080, 108dp 캔버스)
- 레거시: `mipmap-{mdpi..xxxhdpi}/ic_launcher.png`, `ic_launcher_round.png` + manifest `roundIcon`
- 스토어용 512px: `docs/icon-512.png`

## 4. 화면 (실기기 캡처)
| 파일 | 내용 |
|---|---|
| `docs/screenshots/01-dashboard-ko.png` | 한국어 대시보드 (시계·달력·날씨) |
| `docs/screenshots/02-dashboard-en.png` | 영어 대시보드 |
| `docs/screenshots/03-settings-ko.png` | 설정 인앱 패널 (도시·시간형식·단위·언어) |

- 설정은 다이얼로그가 아닌 **인앱 우측 패널**로 그린다. 다이얼로그 창을 쓰면 키오스크에서 시스템 바가 다시 나타난다.

## 5. 예외 기록
- (공통 규칙과 충돌하는 승인된 예외를 1줄씩 기록)
