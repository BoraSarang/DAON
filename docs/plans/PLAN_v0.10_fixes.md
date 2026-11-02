# PLAN v0.10 — 티커·TV 안정화 메모 (DAON)

> 작성: 2026-11-02 | 플랫폼: android (Fire HD 10, API 28) | 성격: 버그 픽스(S)

- **T-059 뉴스 티커**: 마퀴 내용 Row 를 `wrapContentWidth(unbounded = true)` 로 측정해 전체 제목 폭을 확보한다. 지금은 부모 폭 제약 안에서 Text 가 잘려 첫 1~2건만 반복된다. 두 벌 복제 + `halfWidth` 모듈로 그대로 유지하되, 측정 폭만 실제 콘텐츠 폭으로 바꾼다.
- **T-060 TV 클릭·화질**: 영상 위에 탭 차단 오버레이를 올려 싱글 탭은 소비(무동작), 더블 탭은 전체화면 토글로 쓴다(전체화면 버튼은 유지). YouTube 임베드는 `controls:0`·iframe `pointer-events:none`·`vq=small`/`setPlaybackQuality('small')`, HLS ExoPlayer 는 `DefaultTrackSelector` 로 640×360·800kbps 상한 + 최저 비트레이트 우선. 상한보다 낮은 화질이 없는 단일 720p 스트림은 재생이 끊기지 않도록 예외 허용.
- **검증**: 단위 테스트 92건 + `assembleDebug`, 실기기에서 티커 캡처 2장(다른 제목이 흐르는지), TV 싱글 탭 무반응·더블 탭 전체화면은 사용자 눈 확인. 화질 로그 `[PERF] [TV] video WxH` 추가.
