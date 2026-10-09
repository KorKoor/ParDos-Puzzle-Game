#!/usr/bin/env python3
"""Comprueba que el JSON real que entrega Kotlin se puede leer con las estructuras Decodable de Swift.

Uso:  python iosApp/tools/check_swift_contract.py [carpeta con los XML de pruebas]
Lee las muestras que imprime ContractDumpTest (líneas CONTRACT<TAB>nombre<TAB>json) y, para cada una, revisa que estén todas las
claves obligatorias y que los tipos coincidan (Int sin decimales, Double, String, Bool, listas, diccionarios y opcionales).
Sin Mac no se puede ejecutar Swift, pero esto atrapa el error más común: un estado que no se decodifica y deja la pantalla vacía.
"""
import glob
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SWIFT_DIR = ROOT / "iosApp" / "ParDos"
RESULTS = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "shared" / "build" / "test-results" / "testDebugUnitTest"

# muestra → tipo Swift que la lee
MAPPING = {
    "state": "MetaState", "stateLate": "MetaState", "albumState": "AlbumStateData", "open": "OpenAppResult",
    "openLate": "OpenAppResult", "openComeback": "OpenAppResult", "openComeback2": "OpenAppResult",
    "gift": "DailyGiftResult", "calendarClaim": "CalendarClaimResult", "calendarRecover": "CalendarClaimResult", "chestOpen": "ChestOpenResult", "wheel": "WheelResult", "win": "WinReward", "loss": "LossInfo",
    "missionClaim": "MissionClaimResult", "tierClaim": "TierClaimResult", "fail": "ActionResult",
    "skinCatalog": "[SkinItem]", "fxCatalog": "[FxItem]", "avatarCatalog": "[AvatarItem]", "bannerCatalog": "[BannerItem]",
    "albumCatalog": "AlbumCatalogData", "seasonTiers": "[SeasonTierInfo]", "wheelSlices": "[WheelSliceInfo]",
    "economy": "EconomyInfo", "store": "StoreCatalogData", "board": "BoardSnap", "board2": "BoardSnap",
    "levelCard": "LevelCard", "dailyCard": "LevelCard", "hint": "GuideHint",
    "assist": "AssistInfo", "towerStart": "TowerInfo", "towerNext": "TowerInfo", "towerWin": "TowerWinInfo", "towerLose": "TowerLossInfo",
    "raceStage": "RaceStageInfo", "raceFinish": "RaceEndInfo", "duelConfig": "DuelConfigInfo", "duelResult": "DuelResultInfo",
    "records": "RecordsInfo", "achCheck": "AchCheckResult", "achList": "AchListData", "prestige": "PrestigeData",
    "prestigeEvents": "[PrestigeEventInfo]", "reminders": "[ReminderInfo]", "runs": "[RunInfo]", "friends": "FriendsData", "friendAdd": "FriendAddResult", "openRepair": "OpenAppResult", "stateRepair": "MetaState", "stateHalloween": "MetaState", "remindersHalloween": "[ReminderInfo]", "stateLeague": "MetaState", "prestigeLate": "PrestigeData", "achListLate": "AchListData", "tables": "TablesInfo", "remoteCreate": "RemoteCreateInfo", "remoteDecode": "RemoteChallengeInfo", "remoteChallenged": "RemoteResultInfo", "remoteHistory": "RemoteHistoryData", "studioState": "StudioStateData", "studioPreview": "SkinItem",
}


def parse_structs():
    structs = {}
    for path in SWIFT_DIR.glob("*.swift"):
        text = path.read_text(encoding="utf-8")
        for m in re.finditer(r"struct\s+(\w+)\s*:\s*([^{]*?)\{", text):
            if "Decodable" not in m.group(2):
                continue
            depth = 1
            i = m.end()
            while i < len(text) and depth > 0:
                if text[i] == "{":
                    depth += 1
                elif text[i] == "}":
                    depth -= 1
                i += 1
            body = text[m.end():i - 1]
            fields = {}
            nested = 0
            for line in body.splitlines():
                stripped = line.strip()
                if nested == 0:
                    fm = re.match(r"let\s+(\w+)\s*:\s*(.+)$", stripped)
                    if fm:
                        fields[fm.group(1)] = fm.group(2).strip()
                nested += line.count("{") - line.count("}")
            structs[m.group(1)] = fields
    return structs


def check(structs, typ, value, path, errors):
    typ = typ.strip()
    if typ.endswith("?"):
        if value is None:
            return
        check(structs, typ[:-1], value, path, errors)
        return
    if typ.startswith("[") and typ.endswith("]"):
        inner = typ[1:-1]
        if ":" in inner:
            key, val = [p.strip() for p in inner.split(":", 1)]
            if not isinstance(value, dict):
                errors.append(f"{path}: se esperaba diccionario y hay {type(value).__name__}")
                return
            for k, v in value.items():
                check(structs, val, v, f"{path}[{k}]", errors)
        else:
            if not isinstance(value, list):
                errors.append(f"{path}: se esperaba lista y hay {type(value).__name__}")
                return
            for i, v in enumerate(value[:50]):
                check(structs, inner, v, f"{path}[{i}]", errors)
        return
    if typ in ("Int", "Int64", "Int32"):
        if isinstance(value, bool) or not isinstance(value, int):
            errors.append(f"{path}: Int pero llegó {value!r}")
    elif typ == "Double":
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            errors.append(f"{path}: Double pero llegó {value!r}")
    elif typ == "String":
        if not isinstance(value, str):
            errors.append(f"{path}: String pero llegó {value!r}")
    elif typ == "Bool":
        if not isinstance(value, bool):
            errors.append(f"{path}: Bool pero llegó {value!r}")
    elif typ in structs:
        if not isinstance(value, dict):
            errors.append(f"{path}: se esperaba objeto {typ} y hay {type(value).__name__}")
            return
        for name, ftype in structs[typ].items():
            if name not in value:
                if not ftype.strip().endswith("?"):
                    errors.append(f"{path}.{name}: falta la clave ({ftype})")
                continue
            check(structs, ftype, value[name], f"{path}.{name}", errors)
    else:
        errors.append(f"{path}: tipo desconocido {typ}")


def main():
    structs = parse_structs()
    samples = {}
    for f in glob.glob(str(RESULTS / "*ContractDumpTest.xml")):
        tree = ET.parse(f)
        for node in tree.getroot().iter("system-out"):
            for line in (node.text or "").splitlines():
                if line.startswith("CONTRACT\t"):
                    _, name, raw = line.split("\t", 2)
                    samples[name] = raw
    if not samples:
        print("No hay muestras: corre antes  ./gradlew :shared:testDebugUnitTest --tests '*ContractDumpTest'")
        return 2
    errors = []
    for name, typ in MAPPING.items():
        raw = samples.get(name)
        if raw is None:
            errors.append(f"{name}: no hay muestra")
            continue
        if raw.strip() == "":
            continue  # p. ej. una pista vacía
        try:
            value = json.loads(raw)
        except ValueError as e:
            errors.append(f"{name}: JSON inválido ({e})")
            continue
        check(structs, typ, value, name, errors)
    for e in errors:
        print("ERROR", e)
    print(f"{len(samples)} muestras, {len(structs)} estructuras Swift, {len(errors)} errores")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
