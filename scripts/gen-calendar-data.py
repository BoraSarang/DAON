#!/usr/bin/env python3
"""DAON 달력 데이터 자산 생성기.

앱에 번들하는 음력·24절기·공휴일·절기 설명 JSON을 만든다.

준비:
    python3 -m venv .venv-calendar
    .venv-calendar/bin/pip install korean-lunar-calendar ephem
    .venv-calendar/bin/python scripts/gen-calendar-data.py

검증만 하고 파일을 쓰지 않으려면:
    .venv-calendar/bin/python scripts/gen-calendar-data.py --check

출력:
    app/src/main/assets/calendar/lunar_2025_2040.json
    app/src/main/assets/calendar/solar_terms_2025_2040.json
    app/src/main/assets/calendar/holidays_2026_2027.json
    app/src/main/assets/calendar/term_details_ko.json
    app/src/main/assets/calendar/term_details_en.json

주의:
- 음력은 korean-lunar-calendar의 KASI 기반 테이블을 쓴다.
- 24절기는 태양 겉보기 황경이 15° 배수가 되는 순간을 계산한다.
  ephem의 apparent RA/Dec(of date)를 황도 좌표로 바꿔 구하며,
  2026년 KASI 월력요항 값과 분 단위까지 대조한다.
- 공휴일은 scripts/calendar-src/holidays_2026_2027.json 의 확정 값만 쓴다.
  임시공휴일·선거일은 발표 후 원본을 갱신하고 다시 생성한다.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import math
import sys
from pathlib import Path

try:
    import ephem
    from korean_lunar_calendar import KoreanLunarCalendar
except ImportError as exc:  # pragma: no cover - 실행 환경 안내
    sys.exit(
        "필수 패키지가 없습니다. `pip install korean-lunar-calendar ephem` 후 다시 실행하세요.\n"
        f"원인: {exc}"
    )

START_YEAR = 2025
END_YEAR = 2040
KST = dt.timezone(dt.timedelta(hours=9))

REPO_ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = REPO_ROOT / "scripts" / "calendar-src"
ASSET_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "calendar"

# 달력 순서(소한부터 동지까지). index는 term_details의 키와 같다.
TERMS = [
    ("소한", "Sohan", "小寒", 285),
    ("대한", "Daehan", "大寒", 300),
    ("입춘", "Ipchun", "立春", 315),
    ("우수", "Usu", "雨水", 330),
    ("경칩", "Gyeongchip", "驚蟄", 345),
    ("춘분", "Chunbun", "春分", 0),
    ("청명", "Cheongmyeong", "淸明", 15),
    ("곡우", "Gogu", "穀雨", 30),
    ("입하", "Ipha", "立夏", 45),
    ("소만", "Soman", "小滿", 60),
    ("망종", "Mangjong", "芒種", 75),
    ("하지", "Haji", "夏至", 90),
    ("소서", "Soseo", "小暑", 105),
    ("대서", "Daeseo", "大暑", 120),
    ("입추", "Ipchu", "立秋", 135),
    ("처서", "Cheoseo", "處暑", 150),
    ("백로", "Baengno", "白露", 165),
    ("추분", "Chubun", "秋分", 180),
    ("한로", "Hallo", "寒露", 195),
    ("상강", "Sanggang", "霜降", 210),
    ("입동", "Ipdong", "立冬", 225),
    ("소설", "Soseol", "小雪", 240),
    ("대설", "Daeseol", "大雪", 255),
    ("동지", "Dongji", "冬至", 270),
]

# KASI 2026년 월력요항 대조값 (분 단위).
KASI_2026 = {
    "소한": "2026-01-05 17:23",
    "대한": "2026-01-20 10:44",
    "입춘": "2026-02-04 05:02",
    "우수": "2026-02-19 00:51",
    "경칩": "2026-03-05 22:59",
    "춘분": "2026-03-20 23:45",
    "청명": "2026-04-05 03:40",
    "곡우": "2026-04-20 10:39",
    "입하": "2026-05-05 20:48",
    "소만": "2026-05-21 09:36",
    "망종": "2026-06-06 00:48",
    "하지": "2026-06-21 17:24",
    "소서": "2026-07-07 10:56",
    "대서": "2026-07-23 04:13",
    "입추": "2026-08-07 20:42",
    "처서": "2026-08-23 11:18",
    "백로": "2026-09-07 23:41",
    "추분": "2026-09-23 09:05",
    "한로": "2026-10-08 15:29",
    "상강": "2026-10-23 18:37",
    "입동": "2026-11-07 18:52",
    "소설": "2026-11-22 16:23",
    "대설": "2026-12-07 11:52",
    "동지": "2026-12-22 05:50",
}

# 알려진 음력 날짜 (설날·추석·기준일).
KNOWN_LUNAR = {
    "2025-01-29": (2025, 1, 1, False),
    "2025-10-06": (2025, 8, 15, False),
    "2026-02-17": (2026, 1, 1, False),
    "2026-09-25": (2026, 8, 15, False),
    "2026-10-31": (2026, 9, 21, False),
    "2027-02-07": (2027, 1, 1, False),
    "2027-09-15": (2027, 8, 15, False),
}


def solar_longitude(when: ephem.Date) -> float:
    """태양의 겉보기 황경(도). of-date apparent RA/Dec를 황도로 변환한다."""
    sun = ephem.Sun()
    sun.compute(when)
    equatorial = ephem.Equatorial(sun.ra, sun.dec, epoch=when)
    return math.degrees(ephem.Ecliptic(equatorial).lon) % 360.0


def angle_diff(a: float, b: float) -> float:
    """-180..180 범위의 각도 차."""
    return (a - b + 180.0) % 360.0 - 180.0


def solve_term(target: float, year: int) -> dt.datetime:
    """해당 연도에서 태양 황경이 target이 되는 KST 시각을 찾는다."""
    start = ephem.Date(f"{year}/01/01")
    end = ephem.Date(f"{year}/12/31 23:59")
    step = ephem.hour * 6
    when = start
    prev = angle_diff(solar_longitude(when), target)
    while when < end:
        nxt = when + step
        cur = angle_diff(solar_longitude(nxt), target)
        if prev <= 0 <= cur and prev != cur:
            lo, hi = when, nxt
            for _ in range(60):
                mid = (lo + hi) / 2
                if angle_diff(solar_longitude(mid), target) < 0:
                    lo = mid
                else:
                    hi = mid
            instant = ephem.Date((lo + hi) / 2).datetime()
            # ephem은 UTC 기준이므로 KST로 옮긴다.
            return instant + dt.timedelta(hours=9)
        prev = cur
        when = nxt
    raise RuntimeError(f"{year}년 {target}° 절기를 찾지 못했습니다.")


def build_lunar() -> dict:
    calendar = KoreanLunarCalendar()
    days: dict[str, list[int]] = {}
    day = dt.date(START_YEAR, 1, 1)
    end = dt.date(END_YEAR, 12, 31)
    while day <= end:
        calendar.setSolarDate(day.year, day.month, day.day)
        days[day.isoformat()] = [
            calendar.lunarYear,
            calendar.lunarMonth,
            calendar.lunarDay,
            1 if calendar.isIntercalation else 0,
        ]
        day += dt.timedelta(days=1)

    for key, expected in KNOWN_LUNAR.items():
        actual = tuple(days[key][:3]) + (bool(days[key][3]),)
        if actual != expected:
            raise AssertionError(f"음력 불일치 {key}: {actual} != {expected}")

    return {
        "version": 1,
        "source": "한국천문연구원 음양력 (korean-lunar-calendar)",
        "range": {"from": f"{START_YEAR}-01-01", "to": f"{END_YEAR}-12-31"},
        "days": days,
    }


def build_solar_terms() -> dict:
    days: dict[str, list[dict]] = {}
    for year in range(START_YEAR, END_YEAR + 1):
        for index, (name, name_en, hanja, longitude) in enumerate(TERMS):
            computed = solve_term(longitude, year)
            verified = "computed"
            if year == 2026:
                official = dt.datetime.strptime(KASI_2026[name], "%Y-%m-%d %H:%M")
                if abs((computed - official).total_seconds()) > 120:
                    raise AssertionError(f"2026 {name} 계산값이 KASI와 2분 이상 차이: {computed} vs {official}")
                instant = official
                verified = "KASI"
            else:
                # 분 단위로 반올림한다.
                instant = (computed + dt.timedelta(seconds=30)).replace(second=0, microsecond=0)

            key = instant.date().isoformat()
            if key in days:
                raise AssertionError(f"같은 날에 절기가 둘 이상 있습니다: {key}")
            days[key] = [
                {
                    "index": index,
                    "name": name,
                    "nameEn": name_en,
                    "hanja": hanja,
                    "time": instant.strftime("%H:%M"),
                    "longitude": longitude,
                    "verified": verified,
                }
            ]

    return {
        "version": 1,
        "source": "2026년은 우주항공청 월력요항, 그 외 연도는 태양 겉보기 황경 계산(PyEphem)",
        "range": {"from": START_YEAR, "to": END_YEAR},
        "days": dict(sorted(days.items())),
    }


def build_holidays() -> dict:
    with open(SRC_DIR / "holidays_2026_2027.json", encoding="utf-8") as fp:
        source = json.load(fp)
    allowed = {2026, 2027}
    for key, entries in source["days"].items():
        year = int(key[:4])
        if year not in allowed:
            raise AssertionError(f"공휴일 범위 밖: {key}")
        if not entries:
            raise AssertionError(f"공휴일 항목이 비었습니다: {key}")
        for entry in entries:
            for field in ("name", "nameEn", "type", "description", "descriptionEn"):
                if not entry.get(field):
                    raise AssertionError(f"{key} 필드 누락: {field}")
    return source


def build_term_details(locale: str) -> dict:
    with open(SRC_DIR / "terms.json", encoding="utf-8") as fp:
        source = json.load(fp)

    terms: dict[str, dict] = {}
    for index, (name, name_en, hanja, _) in enumerate(TERMS):
        raw = source["terms"].get(str(index))
        if raw is None:
            raise AssertionError(f"절기 설명 누락: {index} {name}")
        if raw["hanja"] != hanja:
            raise AssertionError(f"한자 불일치 {name}: {raw['hanja']} != {hanja}")
        if raw["nameEn"] != name_en:
            raise AssertionError(f"영문명 불일치 {name}: {raw['nameEn']} != {name_en}")
        tasks = raw["tasksKo"] if locale == "ko" else raw["tasksEn"]
        if len(tasks) < 3:
            raise AssertionError(f"농사일이 3개 미만입니다: {name}")
        terms[str(index)] = {
            "name": name if locale == "ko" else name_en,
            "hanja": hanja,
            "season": raw["seasonKo"] if locale == "ko" else raw["seasonEn"],
            "summary": raw["summaryKo"] if locale == "ko" else raw["summaryEn"],
            "description": raw["descriptionKo"] if locale == "ko" else raw["descriptionEn"],
            "tasks": tasks,
        }

    if len(terms) != len(TERMS):
        raise AssertionError("절기 설명 개수가 24개가 아닙니다.")
    return {
        "version": 1,
        "locale": locale,
        "source": "일반 영농 안내 (지역·품종·기상에 따라 다름)",
        "note": "농사일은 참고용입니다. 지역 농협·농업기술센터 안내를 함께 확인하세요."
        if locale == "ko"
        else "Farming notes are for reference only. Check local agricultural guidance.",
        "terms": terms,
    }


def dump(data: dict) -> str:
    return json.dumps(data, ensure_ascii=False, separators=(",", ":"), sort_keys=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="DAON 달력 데이터 자산 생성")
    parser.add_argument("--check", action="store_true", help="파일을 쓰지 않고 기존 자산과 비교만 한다")
    args = parser.parse_args()

    outputs = {
        f"lunar_{START_YEAR}_{END_YEAR}.json": build_lunar(),
        f"solar_terms_{START_YEAR}_{END_YEAR}.json": build_solar_terms(),
        "holidays_2026_2027.json": build_holidays(),
        "term_details_ko.json": build_term_details("ko"),
        "term_details_en.json": build_term_details("en"),
    }

    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    changed = []
    for filename, data in outputs.items():
        text = dump(data)
        path = ASSET_DIR / filename
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != text:
                changed.append(filename)
                print(f"DIFF {filename}")
        else:
            path.write_text(text, encoding="utf-8")
            print(f"WROTE {filename} ({len(text.encode('utf-8')):,} bytes)")

    if args.check:
        if changed:
            print(f"\n자산이 원본과 다릅니다: {', '.join(changed)}")
            return 1
        print("\n자산이 원본과 일치합니다.")
        return 0

    print("\n검증 통과:")
    print(f"- 음력 {len(outputs[f'lunar_{START_YEAR}_{END_YEAR}.json']['days']):,}일")
    print(f"- 24절기 {len(outputs[f'solar_terms_{START_YEAR}_{END_YEAR}.json']['days']):,}개")
    print(f"- 공휴일 {len(outputs['holidays_2026_2027.json']['days'])}일")
    print("- 2026년 24절기 KASI 값 일치")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
