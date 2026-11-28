    #!/usr/bin/env python3
#
# FlightIntel
#
# Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""Builds the aircraft type designator names the PIREP screen shows.

Usage: update_aircraft_types.py [icao.json]

Fetches ICAO Doc 8643 aircraft type designators from ICAO's public service,
or reads a saved copy, and writes src/main/resources/aircraft_types.tsv as
"DESIGNATOR<tab>Manufacturer, Model" lines. ICAO lists each designator once
per model and licensee, so one name is chosen for each: the manufacturer
whose models best match the designator, and its first such model in ICAO's
order, which puts the main model first.

Designators the app had that ICAO no longer lists are kept from
EXTRA_TYPES, as pilots still report them.
"""

import collections
import json
import os
import re
import sys
import urllib.request

ICAO_URL = "https://doc8643.icao.int/external/aircrafttypes"
OUTPUT = os.path.join(os.path.dirname(__file__), "..", "..",
                      "src", "main", "resources", "aircraft_types.tsv")

# Kept although not in ICAO's list: general designators ICAO lists apart,
# common shorthand, and designators ICAO has withdrawn
EXTRA_TYPES = {
    "BALL": "Balloon",
    "GLID": "Glider",
    "GYRO": "Gyroplane",
    "ULAC": "Ultralight",
    "UHEL": "Ultralight helicopter",
    "ZZZZ": "Type not designated",
    "UNKN": "Unknown",
    "PA28": "Piper, PA-28 Cherokee",
    "E175": "Embraer, 175",
    "A225": "Antonov, An-225 Mriya",
    "ATP": "British Aerospace, ATP",
    "DC85": "Douglas, DC-8-50",
    "G250": "Gulfstream Aerospace, G250",
    "CNGP": "Cessna, NGP",
    "DJET": "Diamond, DJ-1 D-Jet",
}

SPECIAL = re.compile(r"\b(BBJ\d?|Prestige|Business|Cargomaster|Freighter|VIP|"
                     r"Executive|Corporate|ACJ|Combi|Tanker|Special|Elite|Combat)\b", re.I)
SMALL = {"de", "of", "and", "the", "van", "von", "da", "do", "del", "la"}
FIXES = {"Mcdonnell": "McDonnell", "Agustawestland": "AgustaWestland"}


def fetch():
    request = urllib.request.Request(ICAO_URL, data=b"", method="POST")
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.load(response)


def norm(text):
    return re.sub(r"[^A-Z0-9]", "", text.upper())


def match_score(designator, model):
    """How well a model name matches its designator: its digits, and its
    letters when there are two or more, but not special editions, nor
    military role names, such as HH-139 for AW-139, unless the designator
    starts with the same letter."""
    n = norm(model)
    score = sum(3 * len(d) for d in re.findall(r"\d+", designator) if d in n)
    letters = re.sub(r"\d", "", designator)
    if len(letters) >= 2 and letters in n:
        score += 2
    if SPECIAL.search(model):
        score -= 4
    prefix = re.match(r"([A-Z]{1,4})-\d", model)
    if prefix and prefix.group(1)[0] != designator[0]:
        score -= 1
    return score


def title(maker):
    maker = re.sub(r"\s*\(\d+\)\s*$", "", maker).strip()
    words = []
    for i, w in enumerate(re.split(r"(\s+|-|/)", maker)):
        if not w.strip() or w in "-/":
            words.append(w)
        elif w.lower() in SMALL:
            words.append(w.lower() if i > 0 else w.capitalize())
        elif len(w) <= 3 or not re.search(r"[AEIOU]", w) or re.search(r"\d", w):
            words.append(w)
        else:
            words.append(FIXES.get(w.capitalize(), w.capitalize()))
    return "".join(words)


def choose(designator, rows):
    count = collections.Counter(r["ManufacturerCode"] for r in rows)
    best = max(rows, key=lambda r: (match_score(designator, r["ModelFullName"]),
                                    count[r["ManufacturerCode"]]))
    maker = best["ManufacturerCode"]
    models = [r["ModelFullName"] for r in rows if r["ManufacturerCode"] == maker]
    top = max(match_score(designator, m) for m in models)
    model = next(m for m in models if match_score(designator, m) == top)
    return f"{title(maker)}, {model}"


def main():
    types = json.load(open(sys.argv[1])) if len(sys.argv) > 1 else fetch()
    by = collections.defaultdict(list)
    for row in types:
        by[row["Designator"]].append(row)
    names = {d: choose(d, rows) for d, rows in by.items()}
    for designator, name in EXTRA_TYPES.items():
        names.setdefault(designator, name)
    with open(OUTPUT, "w", encoding="utf-8") as out:
        for designator in sorted(names):
            out.write(f"{designator}\t{names[designator]}\n")
    print(f"Wrote {len(names)} designators to {os.path.normpath(OUTPUT)}")


if __name__ == "__main__":
    main()
