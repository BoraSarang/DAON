# Data safety · 권한 · 키오스크 설정 (T-009)

> 앱: DAON(다온) `com.borasarang.daon` · v0.2.0-dev
> Amazon Appstore 등록 시 `Data safety` 항목에 그대로 옮겨 적는다.

## 1. 수집·이용 요약
| 질문 | 답 |
|---|---|
| 개발자가 개인정보를 수집하는가? | **아니오.** 계정·광고·분석 SDK 가 없다. |
| 데이터가 기기 밖으로 나가는가? | 날씨 좌표(Open-Meteo)와 동네 검색어(Nominatim)만 전송된다. |
| 데이터를 판매하는가? | **아니오.** |
| 아동 대상인가? | **아니오.** 70대 성인 사용자를 위한 키오스크 앱이다. |
| 데이터 삭제 요청 방법 | 앱 데이터 삭제(설정 → 앱 → 저장공간 → 데이터 삭제) 또는 앱 삭제. |

## 2. 기기에 저장되는 데이터
| 항목 | 저장 위치 | 목적 |
|---|---|---|
| 언어·시간 형식·단위·화면 전환·글씨 크기·시계 모양 | SharedPreferences `settings` | 설정 유지 |
| 마지막 위치 좌표·정확도·시각 | SharedPreferences `settings` | 날씨 지점 캐시(24시간) |
| 검색해 저장한 동네 이름·좌표 | SharedPreferences `settings` | 기기 위치를 못 쓸 때 날씨 지점 |
| 직접 추가한 일정 | SharedPreferences `events` (JSON) | 달력 표시 |
| 마지막 날씨 값 | 메모리 | 오프라인 표시 |

- `android:allowBackup="false"` 로 두어 클라우드 백업에 포함되지 않는다.
- 기기 캘린더 일정은 **읽기만** 하며 앱이 저장·수정하지 않는다.

## 3. 제3자 전송
| 대상 | 보내는 값 | 목적 | 정책 |
|---|---|---|---|
| Open-Meteo `api.open-meteo.com` | 위·경도, 시간대, 단위 | 날씨 예보 | https://open-meteo.com/en/terms |
| OpenStreetMap Nominatim `nominatim.openstreetmap.org` | 사용자가 입력한 동네 이름 | 좌표 검색 | https://osmfoundation.org/wiki/Terms_of_Use |

- 광고·추적·분석 도구 없음.
- 개발자 서버 없음. 앱은 위 두 API 외에 통신하지 않는다.

## 4. 권한과 사유
| 권한 | 필수? | 사유 | 거부 시 |
|---|---|---|---|
| `INTERNET` | 필수 | 날씨·동네 검색 | 날씨를 못 받음(마지막 값 유지) |
| `ACCESS_NETWORK_STATE` | 필수 | 네트워크 상태 확인 | — |
| `ACCESS_COARSE_LOCATION` | 선택 | 현재 위치로 날씨 지점 자동 선택 | 저장한 동네 또는 도시 사용 |
| `READ_CALENDAR` | 선택 | 기기 캘린더 일정 읽기(읽기 전용) | 로컬 일정만 사용 |
| `RECEIVE_BOOT_COMPLETED` | 선택 | 부팅 시 대시보드 자동 시작(설정에서 켤 때) | 자동 시작 안 함 |

- 위치·캘린더 권한은 첫 사용 시 한 번만 요청하고, 거부하면 다시 자동으로 묻지 않는다.
- Fire OS 7 에도 캘린더 provider(`com.android.providers.calendar`)가 있어 `READ_CALENDAR` 를 허용하면 기기 일정을 읽는다. 캘린더 계정이 없으면 0건으로 표시되고, provider 자체가 없는 기기만 `UNAVAILABLE` 이다.

## 5. 키오스크 설정 (Device Owner, 선택)
일반 화면 고정(스크린 피닝)은 설정 → 키오스크 → `이 앱에 고정` 으로 켠다. 처음 한 번 시스템 확인 창이 뜬다.
확인 창 없이 고정하려면 기기를 초기화한 뒤 Device Owner 로 등록한다.

```bash
# 1) 기기 초기화(또는 계정 없는 새 기기) 후 USB 디버깅 허용
adb shell dpm set-device-owner com.borasarang.daon/.admin.AdminReceiver

# 2) 등록 확인
adb shell dpm list-owners

# 3) 해제(앱 삭제 전에 필요)
adb shell dpm remove-active-admin com.borasarang.daon/.admin.AdminReceiver
```

- Device Owner 등록 후에는 `이 앱에 고정` 이 확인 창 없이 동작한다.
- 부팅 자동 시작은 `RECEIVE_BOOT_COMPLETED` + 설정 토글로 동작하지만, Fire OS 가 백그라운드 Activity 시작을 막으면 열리지 않는다. 이때는 기기 설정의 자동 시작 허용을 확인한다.
- 고정 해제: 뒤로가기 + 최근 앱 버튼을 함께 길게 누른다(일반 화면 고정). Device Owner 모드에서는 설정에서 토글을 끈다.

## 6. 개인정보처리방침 초안
> DAON(다온)은 이용자의 개인정보를 수집하지 않습니다. 앱은 기기 안에서 달력·날씨·일정을 표시하며, 날씨 예보를 위해 위·경도를 Open-Meteo 에, 동네 검색어를 OpenStreetMap Nominatim 에 전송합니다. 위치·캘린더 정보는 기기 밖 개발자 서버로 전송되지 않으며, 앱을 삭제하면 기기에 저장된 설정·일정도 함께 삭제됩니다. 문의: [등록 시 입력할 개발자 연락처].

## 7. 등록 체크리스트
- [ ] 짧은 설명·전체 설명 (ko/en) — `listing_ko.md`, `listing_en.md`
- [ ] 스크린샷 5장(ko) + 필요 시 en
- [ ] 앱 아이콘 512×512 — `docs/icon-512.png`
- [ ] Data safety 답변 — 이 문서 §1~§4
- [ ] 개인정보처리방침 URL — §6 초안을 웹에 게시
- [ ] 콘텐츠 등급 설문 — 폭력·성·도박·광고 없음
- [ ] 대상 기기: Fire HD 10 (2019) / Fire OS 7 이상
