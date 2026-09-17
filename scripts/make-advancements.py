#!/usr/bin/env python3
"""Write the shipped advancements to src/main/resources/data/zombiemod/advancement/.

Generated rather than hand-written for one reason: an icon that wears a genus's face takes the
texture from that genus's own file, so the two cannot drift. Everything else here is just the list.

Titles and descriptions are literal text on purpose. A vanilla client has no ZombieMod language
file, so a translation key would reach it as the key.

    python3 scripts/make-advancements.py
"""
import json
import pathlib
import shutil

ROOT = pathlib.Path(__file__).resolve().parent.parent
GENERA = ROOT / "src/main/resources/data/zombiemod/zombiemod/genus"
OUT = ROOT / "src/main/resources/data/zombiemod/advancement"


def face(genus, fallback="minecraft:zombie_head"):
    """A player head wearing this genus's face, or the fallback item if it has no texture of its own."""
    head = json.loads((GENERA / f"{genus}.json").read_text()).get("head")
    if isinstance(head, dict):
        return {"id": "minecraft:player_head", "components": {"minecraft:profile": head}}
    return {"id": fallback}


def item(name):
    return {"id": f"minecraft:{name}"}


# path, parent, icon, frame, title, description, criteria, hidden, xp
A = []


def add(path, parent, icon, title, description, criteria, frame="task", hidden=False, xp=0):
    A.append((path, parent, icon, frame, title, description, criteria, hidden, xp))


Z = "zombiemod:"

add("root", None, item("zombie_head"), "ZombieMod",
    "Something is wrong with the zombies. Trade blows with one that is not a zombie any more.",
    ["met_count/1"])

# ---- the dex, met
add("dex/met_10", "root", item("writable_book"), "Field Notes",
    "Meet 10 different genera. Hitting one counts. So does being hit.", ["met_count/10"])
add("dex/met_25", "dex/met_10", item("book"), "Naturalist of the Dead",
    "Meet 25 different genera.", ["met_count/25"], frame="goal")
add("dex/met_all", "dex/met_25", item("knowledge_book"), "Seen It All",
    "Meet every genus in your ZombieDex.", ["met_all"], frame="challenge", xp=100)

# ---- the dex, killed
add("dex/killed_1", "root", item("iron_sword"), "One Less",
    "Kill something that used to be a zombie.", ["killed_count/1"])
add("dex/killed_10", "dex/killed_1", item("diamond_sword"), "Thinning the Herd",
    "Kill 10 different genera.", ["killed_count/10"])
add("dex/killed_25", "dex/killed_10", item("netherite_sword"), "Pest Control",
    "Kill 25 different genera.", ["killed_count/25"], frame="goal")
add("dex/killed_all", "dex/killed_25", item("skeleton_skull"), "Extinction Event",
    "Kill at least one of every genus in your ZombieDex.", ["killed_all"], frame="challenge", xp=250)

# ---- bosses
add("boss/ritual", "root", item("wither_rose"), "Do Not Read From the Book",
    "Complete a summoning ritual. Whatever answers is your problem now.", ["ritual"], hidden=True)
add("boss/butcher", "boss/ritual", face("butcher"), "Closing Time",
    "Kill the Butcher.", ["kill/zombiemod:butcher"], frame="challenge", xp=100)
add("boss/patient_zero", "boss/ritual", face("patient_zero"), "Ground Zero",
    "Kill Patient Zero, where all of this started.", ["kill/zombiemod:patient_zero"],
    frame="challenge", xp=100)
add("boss/borg_queen", "niche/horde", face("borg_queen"), "Resistance Was Not Futile",
    "Kill the Borg Queen.", ["kill/zombiemod:borg_queen"], frame="challenge", hidden=True, xp=100)
add("boss/all", "boss/patient_zero", item("nether_star"), "Apex Predator",
    "Kill the Butcher, Patient Zero and the Borg Queen.",
    ["kill/zombiemod:butcher", "kill/zombiemod:patient_zero", "kill/zombiemod:borg_queen"],
    frame="challenge", hidden=True, xp=250)

# ---- the niche ones
add("niche/defuse", "dex/killed_1", face("boomer", "minecraft:tnt"), "Bomb Disposal",
    "Kill a Boomer or a Bloater after its fuse is lit and before it goes off.", ["defuse"], frame="goal")
add("niche/coward", "dex/killed_1", face("coward"), "It Was Bait",
    "Run down a Coward. Then look behind you.", ["kill/zombiemod:coward"])
add("niche/horde", "dex/killed_1", item("goat_horn"), "Last One Standing",
    "Clear a horde, to the last straggler.", ["horde_cleared"], frame="goal")
add("niche/cured", "root", item("milk_bucket"), "Got Milk?",
    "Cure an infection with a bucket of milk, before it finishes what it started.", ["cured"])
add("niche/own_corpse", "root", item("player_head"), "Pull Yourself Together",
    "Kill your own corpse and take your things back.", ["corpse/own"])
add("niche/other_corpse", "niche/own_corpse", item("bundle"), "Finders Keepers",
    "Put down somebody else's corpse. What you do with their things is between you and them.",
    ["corpse/other"])
add("niche/herobrine_met", "root", item("player_head"), "You Saw Him Too",
    "Meet Herobrine.", ["meet/zombiemod:herobrine"], hidden=True)
add("niche/herobrine_killed", "niche/herobrine_met", item("player_head"), "Removed Herobrine",
    "It has been in the patch notes for years. Somebody finally did it.",
    ["kill/zombiemod:herobrine"], frame="challenge", hidden=True, xp=100)
add("niche/weeping", "root", face("weeping"), "Don't Blink",
    "Meet a Weeping Zombie.", ["meet/zombiemod:weeping"], hidden=True)
add("niche/colossus", "dex/killed_10", item("iron_block"), "David",
    "Kill a Colossus.", ["kill/zombiemod:colossus"], frame="challenge", hidden=True, xp=100)

# ---- seasonal
add("seasonal/jack", "dex/killed_1", item("jack_o_lantern"), "Trick",
    "Kill Jack. He is only about near Halloween.", ["kill/zombiemod:jack"])
add("seasonal/krampus", "dex/killed_1", item("coal"), "Naughty List",
    "Kill Krampus. He is only about near Christmas.", ["kill/zombiemod:krampus"])


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    paths = {a[0] for a in A}
    for path, parent, icon, frame, title, description, criteria, hidden, xp in A:
        assert parent is None or parent in paths, f"{path}: no such parent {parent}"
        display = {
            "icon": icon,
            "title": {"text": title},
            "description": {"text": description},
            "frame": frame,
            "show_toast": True,
            # Chat announcements are the server's to decide, through the vanilla gamerule.
            "announce_to_chat": path != "root",
            "hidden": hidden,
        }
        body = {}
        if parent is None:
            display["background"] = "minecraft:gui/advancements/backgrounds/stone"
        else:
            body["parent"] = Z + parent
        body["display"] = display
        # Every criterion is granted by the mod, by name. See neoforge/Feats.
        body["criteria"] = {Z + c: {"trigger": "minecraft:impossible"} for c in criteria}
        body["requirements"] = [[Z + c] for c in criteria]
        if xp:
            body["rewards"] = {"experience": xp}
        body["sends_telemetry_event"] = False
        file = OUT / f"{path}.json"
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(json.dumps(body, indent=2) + "\n")
    print(f"{len(A)} advancements -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
