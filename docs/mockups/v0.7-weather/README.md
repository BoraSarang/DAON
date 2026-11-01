# v0.7 날씨 배경·움직임 목업

> 기준: 1920×1200 (Fire HD 10), 1dp = 1.5px · 카드·활자·색은 v0.6 토큰 그대로
> 파일: `index.html`(전체 시트) · `screens/{day,rain,night,photo}.png`

## 1. 참고 앱(Weawow 7.1.7)에서 확인한 것

APK(`com.weawow_7.1.7-717`)의 `res/` 4,247개와 `resources.arsc` 이름을 복원해 확인했다.

| 항목 | 확인된 사실 | 근거 |
|---|---|---|
| 배경 | **위치 기반 실사 사진**을 Glide 로 불러 깐다. 사진 위에 위→아래 검정 스크림 | `drawable/background_gradient` = `#99000000 → #00000000`, angle 90 · `bg_dark_trans` = `#DD000000` · 문자열 `quadKeyPhoto`, `/photos_wa/`, `photo` |
| 카드 | 반투명 판 + 20dp 라운드. Android 12+ 에서만 블러(`bg_blur`) | `bg_blur`: corners 20dp, v31 변형 별도 |
| 아이콘 | `ic_w_<OWM 코드>` PNG 6밀도 + `ic_w_s_*` 벡터 551개. **정적** | `res/…png`, anydpi-v24 |
| 애니메이션 | **날씨 애니메이션 없음.** animated-vector 6개는 전부 Material 체크박스·라디오·비밀번호 토글. Lottie·ExoPlayer·파티클 클래스 없음 | `$avd_hide_password__0`, `anim/abc_*`, dex 문자열 검색 |
| 결론 | Weawow 의 "예쁨"은 애니메이션이 아니라 **① 실사 사진 ② 스크림 ③ 반투명 카드 ④ 큰 활자** |  |

사진은 Weawow 자체 CDN(`weather.weawow.com`)과 사용자 업로드라 **그대로 가져다 쓸 수 없다**(라이선스).
목업 D 의 사진은 분위기 확인용 Unsplash 이미지이며, 실제 적용 시 출처·라이선스를 다시 정해야 한다.

## 2. DAON 에서 가능한가

| 항목 | 판단 | 비용 |
|---|---|---|
| 하늘 그라디언트·해·달·구름 | **가능.** Compose `Canvas` + `Brush.verticalGradient`/`radialGradient`. 에셋 0KB | 프레임당 도형 10개 미만, PSS 영향 사실상 없음 |
| 비·눈·천둥 | **이미 있음**(`WeatherEffect.kt`, 64개 입자). 하늘만 깔면 그대로 어울린다 | 0 |
| 유리 카드(블러) | **이 기기에서는 불가**(API 28, `RenderEffect` 는 31+). 배경이 매끈한 그라디언트면 **반투명만으로 유리처럼** 보인다 | 0 |
| 사진 배경 | 가능하지만 ① 라이선스 ② 대비 재검증 ③ 디코드 메모리(1920×1080 ≈ 8MB) | 300KB + 8MB |
| 상시 켜짐 | 60fps 금지. 하늘·해는 **1분 단위**로만 갱신, 구름은 아주 느린 이동(90~240초 주기) | 프레임 시계 1개 재사용 |

즉 **"Weawow 처럼" = 사진이 아니라 '배경 + 유리 카드'** 로 만드는 편이 이 기기·이 사용자에게 맞다.

## 3. 제안

| 안 | 내용 | 장점 | 주의 |
|---|---|---|---|
| **A. 낮 하늘** (권장) | 시간대별 하늘 그라디언트 + 해(느린 광선) + 구름 3겹 | 에셋 0, 지금 카드 그대로, 달력 화면과 톤이 이어짐 | 상단 여백에서만 보이므로 "화려함"은 약함 |
| **B. 비** | 하늘을 어둡게 + 구름 짙게 + 기존 빗줄기(앞 레이어, alpha 0.2) | 비 오는 날이 한눈에 구분됨 | 빗줄기가 카드 위를 지나므로 alpha 상한 필요 |
| **C. 밤** | 하늘·달·별 + 조명 톤 낮추기 | 상시 켜짐 기기의 밤 눈부심 감소 | 별은 정적(반짝임은 7초 주기, 선택) |
| **D. 사진** | 지방 사진 + 스크림 + 반투명 카드 | 가장 "예쁨", Weawow 와 가장 비슷 | 라이선스·용량·대비. API 28 은 블러 없음 |

**권장:** A + C 를 기본(시간대 자동), B 는 기존 입자 위에 하늘만, D 는 설정에서 끄고 켜는 선택으로.
70대 사용자에게는 D 의 사진이 글자 대비를 흔들 수 있어 기본값으로 두지 않는 편이 안전하다.

## 4. 다시 렌더하기

```bash
cd docs/mockups/v0.7-weather
CH="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
for v in day rain night photo; do
  "$CH" --headless=new --disable-gpu --hide-scrollbars \
    --force-device-scale-factor=1 --window-size=1920,1200 --virtual-time-budget=1500 \
    --screenshot="screens/$v.png" "file://$PWD/index.html?shot=$v"
done
```

`index.html` 을 브라우저로 열면 네 장면을 캡션과 함께 볼 수 있다.
`?shot=` 없이 열면 전체 시트, `?shot=day` 처럼 열면 한 장면만 1920×1200 으로 렌더된다.
