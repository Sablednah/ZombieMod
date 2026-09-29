#!/usr/bin/env python3
"""Rewrite loot tables from the 1.21-26.2 dialect into the one Minecraft 26.3 reads.

    python3 scripts/loot-to-26.3.py [file-or-dir ...]     (default: the mod's loot_table folder)

26.3 changed the loot-table grammar, and an old-dialect table does not degrade - it fails to parse
and the server will not start ("Not a number: {min:2,max:4}"). Loot tables are therefore one of the
few things that are NOT identical across branches: author on master, then run this on mc26.3.

The rules, read off vanilla's own 26.3 tables (entities/zombie.json is a good specimen):

    "functions": [{"function": X, ...}]    ->  "modifier": {"type": X, ...}   (a list when several)
    "conditions": [{"condition": X, ...}]  ->  "condition": {"type": X, ...}  (all_of when several)
    {"min": a, "max": b}                   ->  {"type": "minecraft:uniform", "min": a, "max": b}

Conditions nest (inverted's "term", any_of/all_of's "terms", a modifier's own conditions), so the
rewrite recurses. A bare {min, max} inside a "predicate" is a range, not a number provider, and is
left alone. Idempotent: a table already in the 26.3 dialect comes out unchanged.
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT = ROOT / "src/main/resources/data/zombiemod/loot_table"


def condition(c):
    c = convert(c)
    if isinstance(c.get("condition"), str):
        c = {"type": c.pop("condition"), **c}
    return c


def one_condition(cs):
    cs = [condition(c) for c in cs]
    return cs[0] if len(cs) == 1 else {"type": "minecraft:all_of", "terms": cs}


def modifier(f):
    f = convert(f)
    if "function" in f:
        f = {"type": f.pop("function"), **f}
    return f


def convert(node, in_predicate=False):
    if isinstance(node, list):
        return [convert(n, in_predicate) for n in node]
    if not isinstance(node, dict):
        return node
    if not in_predicate and node and set(node) <= {"min", "max"}:
        return {"type": "minecraft:uniform", **node}
    out = {}
    for key, value in node.items():
        if key == "functions":
            mods = [modifier(f) for f in value]
            out["modifier"] = mods[0] if len(mods) == 1 else mods
        elif key == "conditions":
            out["condition"] = one_condition(value)
        elif key in ("term",):
            out[key] = condition(value)
        elif key == "terms":
            out[key] = [condition(t) for t in value]
        else:
            out[key] = convert(value, in_predicate or key == "predicate")
    return out


def main(args):
    targets = [pathlib.Path(a) for a in args] or [DEFAULT]
    files = [f for t in targets for f in (sorted(t.rglob("*.json")) if t.is_dir() else [t])]
    changed = 0
    for f in files:
        before = f.read_text()
        after = json.dumps(convert(json.loads(before)), indent=2) + "\n"
        if after != before:
            f.write_text(after)
            changed += 1
    print(f"{changed} of {len(files)} tables rewritten")


if __name__ == "__main__":
    main(sys.argv[1:])
