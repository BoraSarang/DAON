# ENDPOINTS.md — 외부 API

## Open-Meteo (날씨)

- Base URL: `https://api.open-meteo.com/v1/forecast`
- 인증: 없음 (API 키 불필요)
- 호출 주기: 15분 + 사용자 수동 새로고침
- 타임아웃: 연결 10s / 읽기 10s

### 요청 (v0.2)

```
GET /v1/forecast
  ?latitude={lat}
  &longitude={lon}
  &current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m
  &hourly=temperature_2m,weather_code,precipitation_probability,precipitation,is_day
  &daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max
  &timezone=auto
  &forecast_days=7
  &wind_speed_unit=ms
  &temperature_unit=celsius|fahrenheit
```

> v0.1 은 `forecast_days=4` + 일별 3개 필드만 받았다. v0.2 는 7일·3시간 간격 시간별을 한 번에 받는다.
> 좌표는 기기 위치 또는 설정에서 검색해 저장한 동네 좌표를 쓴다.

### 응답 중 사용하는 필드

| 경로 | 의미 |
|---|---|
| `current.temperature_2m` | 현재 기온 |
| `current.apparent_temperature` | 체감 온도 |
| `current.relative_humidity_2m` | 상대 습도 % |
| `current.weather_code` | WMO 날씨 코드 |
| `current.is_day` | 낮/밤 (0/1) |
| `current.wind_speed_10m` | 풍속 m/s |
| `daily.time[]` | 날짜 (YYYY-MM-DD) |
| `daily.weather_code[]` | 일별 WMO 코드 |
| `daily.temperature_2m_max[]` / `_min[]` | 최고/최저 |
| `daily.precipitation_probability_max[]` | 일별 최대 강수확률 % |
| `daily.precipitation_sum[]` | 일별 강수 합 mm |
| `daily.wind_speed_10m_max[]` | 일별 최대 풍속 m/s |
| `hourly.time[]` | 시각 (YYYY-MM-DDTHH:mm) |
| `hourly.temperature_2m[]` | 시간별 기온 |
| `hourly.weather_code[]` | 시간별 WMO 코드 |
| `hourly.precipitation_probability[]` | 시간별 강수확률 % |
| `hourly.precipitation[]` | 시간별 강수량 mm |
| `hourly.is_day[]` | 시간별 낮/밤 (0/1) |

### 오류 처리

- 타임아웃 → `E-AND-NET-0001` · JSON 파싱 실패 → `E-AND-NET-0002` · HTTP 비정상/IO → `E-AND-NET-0003`
- 사용자 표시: 마지막 성공값 유지 + `weather_offline` 문자열 (무음 실패 금지)
- 로그: `[ERROR] E-AND-NET-0003 http_status=<code>` (응답 본문 전체 로깅 금지)

## Nominatim (동네 검색)

Fire HD 10 은 GPS 가 없고 HERE 네트워크 위치도 좌표를 주지 않는다(2026-10-31 실측).
그래서 설정에서 **동네 이름을 한 번 검색해 좌표를 저장**한다.

- Base URL: `https://nominatim.openstreetmap.org/search`
- 인증: 없음 (OpenStreetMap 공개 서비스)
- 호출 시점: 사용자가 검색어를 입력하고 1초 멈췄을 때 또는 `검색`을 눌렀을 때만
  - Nominatim 이용 정책(초당 1회 이하)을 지키기 위해 디바운스 1초 + 중복 요청 취소
- 필수 헤더: `User-Agent: DAON-kiosk/0.2 (Android tablet)` — 기본 UA 는 차단된다
- 타임아웃: 연결 8s / 읽기 8s

### 요청

```
GET /search
  ?q={검색어}
  &format=jsonv2
  &limit=5
  &accept-language=ko
  &countrycodes=kr
```

### 응답 중 사용하는 필드

| 경로 | 의미 |
|---|---|
| `name` | 지명 (예: 살미면) |
| `display_name` | 전체 주소 (예: 살미면, 충주시, 충청북도, 27493, 대한민국) |
| `lat` / `lon` | 좌표 (문자열 → Double) |

- 표시 이름은 `name` + 주소 첫 조각(`display_name`에서 우편번호 제외)으로 만든다 → "살미면, 충주시"
- 결과가 없으면 `place_search_empty`, 요청 실패는 `place_search_failed` 문자열로 안내한다
