# ENDPOINTS.md — 외부 API

## Open-Meteo (날씨)

- Base URL: `https://api.open-meteo.com/v1/forecast`
- 인증: 없음 (API 키 불필요)
- 호출 주기: 15분 + 사용자 수동 새로고침
- 타임아웃: 연결 10s / 읽기 10s

### 요청 (v0.1)

```
GET /v1/forecast
  ?latitude={lat}
  &longitude={lon}
  &current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m
  &daily=weather_code,temperature_2m_max,temperature_2m_min
  &timezone=auto
  &forecast_days=4
  &temperature_unit=celsius|fahrenheit
  &wind_speed_unit=ms
```

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

### 오류 처리

- HTTP 비정상/타임아웃/JSON 파싱 실패 → `E-AND-NET-0001` / `E-AND-NET-0002`
- 사용자 표시: 마지막 성공값 유지 + `error_weather_offline` 배지 (무음 실패 금지)
- 로그: `[ERROR] E-AND-NET-0001 <code> <message>` (본문 전체 로깅 금지)
