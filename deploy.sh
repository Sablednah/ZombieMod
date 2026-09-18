#!/usr/bin/env bash
# Put a built ZombieMod jar into a CurseForge instance's mods/ folder - and refuse to, if that
# instance's game is running.
#
# Usage: ./deploy.sh [--check] [--no-build] [instance name]
#
#   ./deploy.sh                      build this branch, deploy to "MobHealth - Forge"
#   ./deploy.sh 26.2                 deploy the jar for that instance's Minecraft version
#   ./deploy.sh --check 26.2         say what would happen, touch nothing
#
# The jar is chosen by the INSTANCE's Minecraft version, not by which file is newest: build/libs
# holds one jar per branch, and the newest is merely whichever branch was built last.
#
# THIS IS THE ONLY WAY A JAR GOES INTO AN INSTANCE. On 2026-09-17 a plain `cp` over the jar of a
# running game succeeded - Windows lets the bytes of an open file be replaced - and the JVM, still
# holding the OLD jar's zip index, read the NEW bytes through it:
#
#     NoClassDefFoundError: .../GoalSpecs$AvoidEntity
#     Caused by: java.util.zip.ZipException: ZipFile invalid LOC header (bad signature)
#
# and the game hung on "Preparing world". Nothing was damaged, a restart cured it, and it looked
# exactly like a bug in the mod. A copy that "fails if the game is running" is a belief about Linux.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
# Overridable so the write path can be exercised against a scratch folder instead of a real instance.
INSTANCES="${ZOMBIEMOD_INSTANCES:-/mnt/c/Users/darre/curseforge/minecraft/Instances}"

CHECK=0
BUILD=1
NAME="MobHealth - Forge"
for arg in "$@"; do
    case "$arg" in
        --check) CHECK=1 ;;
        --no-build) BUILD=0 ;;
        -*) echo "!! Unknown option: $arg" >&2; exit 2 ;;
        *) NAME="$arg" ;;
    esac
done

INSTANCE="$INSTANCES/$NAME"
MODS="$INSTANCE/mods"
if [ ! -d "$MODS" ]; then
    echo "!! Instance mods folder not found: $MODS" >&2
    exit 1
fi

# ---- which jar ---------------------------------------------------------------------------------

MC="$(python3 - "$INSTANCE/minecraftinstance.json" <<'PY' 2>/dev/null || true
import json, sys
j = json.load(open(sys.argv[1], encoding='utf-8-sig'))
print((j.get('baseModLoader') or {}).get('minecraftVersion') or j.get('gameVersion') or '')
PY
)"
if [ -z "$MC" ]; then
    echo "!! Could not read the Minecraft version from $INSTANCE/minecraftinstance.json" >&2
    exit 1
fi
case "$MC" in
    1.21.11) BRANCH=master ;;
    26.1*) BRANCH=mc26.1 ;;
    26.2*) BRANCH=mc26.2 ;;
    *) echo "!! No ZombieMod branch builds for Minecraft $MC" >&2; exit 1 ;;
esac

HERE="$(git -C "$ROOT" branch --show-current)"
if [ "$BUILD" = 1 ] && [ "$CHECK" = 0 ] && [ "$HERE" = "$BRANCH" ]; then
    # This repo bundles no JDK; borrow a sibling's. 26.x needs 25, the 1.21 line needs 21.
    if [ "$BRANCH" = master ]; then
        export JAVA_HOME="${JAVA_HOME:-/mnt/d/Repos/sable/MobHealth-Forge/tools/jdk21}"
    else
        export JAVA_HOME="${JAVA_HOME:-/mnt/d/Repos/sable/CityWorld-ReForged/tools/jdk25}"
    fi
    export PATH="$JAVA_HOME/bin:$PATH"
    echo ">> Building ZombieMod on $BRANCH..."
    "$ROOT/gradlew" build --console=plain
fi

JAR="$(ls -t "$ROOT"/build/libs/zombiemod-*+mc"$MC".jar 2>/dev/null | head -1 || true)"
if [ -z "$JAR" ]; then
    # 26.1.2's instance may report 26.1; take whatever that branch produced.
    JAR="$(ls -t "$ROOT"/build/libs/zombiemod-*+mc"${MC%.*}"*.jar 2>/dev/null | head -1 || true)"
fi
if [ -z "$JAR" ]; then
    echo "!! No jar for Minecraft $MC in build/libs. Check out $BRANCH and build it first." >&2
    exit 1
fi
JARNAME="$(basename "$JAR")"

# A jar that is not from that branch's head is a stale one wearing the right name. Not checked out
# there, so not rebuilt above - say so rather than quietly deploying last week's.
STAMP="$(unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null | tr -d '\r' | sed -n 's/^Build-Commit: //p')"
HEAD="$(git -C "$ROOT" rev-parse --short=8 "$BRANCH" 2>/dev/null || true)"
if [ -n "$HEAD" ] && [ "${STAMP%-dirty}" != "$HEAD" ]; then
    echo "!! $JARNAME was built from $STAMP, but $BRANCH is at $HEAD." >&2
    echo "!! Check out $BRANCH and build it, then deploy again." >&2
    exit 1
fi

# ---- is the game running from that instance? ---------------------------------------------------

# Ask Windows, because the filesystem will not say. `rm` failing is a good sign that something has
# the jar open, but the converse is worthless - and an in-place overwrite SUCCEEDS on a jar the game
# holds, which is the whole hazard. Verified against a running 26.2 game on 2026-09-17: the command
# line of the game's javaw.exe carries `--gameDir C:\...\Instances\<name>`.
#
# Prints one of: running / clear / unknown. "unknown" is a Java process, in an interactive session,
# whose command line could not be read - that is not a "no". A service in session 0 is ignored:
# some machines run a Java service whose command line is unreadable without elevation, and the game
# never runs as a service.
game_state() {
    if ! command -v powershell.exe >/dev/null 2>&1; then
        echo unknown
        return
    fi
    local want out
    want="\\Instances\\$NAME"
    out="$(powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='javaw.exe' OR Name='java.exe'\" | ForEach-Object { '{0}|{1}' -f \$_.SessionId, \$_.CommandLine }" 2>/dev/null | tr -d '\r')" || { echo unknown; return; }
    local state=clear line session cmd rest
    while IFS= read -r line; do
        [ -n "$line" ] || continue
        session="${line%%|*}"
        cmd="${line#*|}"
        if [ -z "$cmd" ]; then
            [ "$session" = 0 ] || state=unknown
            continue
        fi
        case "$cmd" in
            *"$want"*)
                # The name must END there: "26.2" is a prefix of "26.2.test".
                rest="${cmd#*"$want"}"
                case "${rest:0:1}" in
                    ''|' '|'"'|'\'|'/') echo running; return ;;
                esac ;;
        esac
    done <<<"$out"
    echo "$state"
}

STATE="$(game_state)"
echo ">> Instance: $NAME (Minecraft $MC)   jar: $JARNAME @ $STAMP   game: $STATE"

if [ "$STATE" = running ]; then
    echo "!! The '$NAME' game is running. Nothing was written." >&2
    echo "!! Replacing a jar under a live JVM corrupts its class loading; close Minecraft and retry." >&2
    exit 1
fi

if [ -f "$MODS/$JARNAME" ] && cmp -s "$JAR" "$MODS/$JARNAME" \
        && [ "$(find "$MODS" -maxdepth 1 -name 'zombiemod-*.jar' | wc -l)" = 1 ]; then
    echo ">> Already up to date."
    exit 0
fi

if [ "$CHECK" = 1 ]; then
    echo ">> Would deploy (game state: $STATE). Nothing was written."
    exit 0
fi

# ---- place it ----------------------------------------------------------------------------------

# Beside it under a temporary name, then remove the old, then rename. Never over the top: a locked
# jar fails on the remove, and at that point nothing has changed and nothing is half-written.
TMP="$MODS/.$JARNAME.tmp"
cleanup() { rm -f "$TMP" 2>/dev/null || true; }
trap cleanup EXIT

if ! cp "$JAR" "$TMP" 2>/dev/null; then
    echo "!! Cannot write to $MODS. Nothing was changed." >&2
    exit 1
fi

LOCKED=""
for old in "$MODS"/zombiemod-*.jar; do
    [ -e "$old" ] || continue
    rm -f "$old" 2>/dev/null || { LOCKED="$old"; break; }
done

if [ -n "$LOCKED" ]; then
    # Something holds the jar. The CurseForge launcher does, for a while after the game window
    # closes, and Windows will not unlink under it - but replacing the bytes is harmless then,
    # because no JVM has a zip index to be contradicted. Only when Windows positively says no game
    # is running from here; "unknown" does not qualify, and only for the jar of the same name.
    if [ "$STATE" = clear ] && [ "$LOCKED" = "$MODS/$JARNAME" ]; then
        echo ">> The old jar is held open, but no game is running from this instance - replacing its bytes."
        cp -f "$JAR" "$LOCKED"
    else
        echo "!! $(basename "$LOCKED") is locked and the game state is '$STATE'. Left exactly as it was." >&2
        echo "!! Close Minecraft (and, if that is not enough, the CurseForge launcher) and retry." >&2
        exit 1
    fi
else
    mv -f "$TMP" "$MODS/$JARNAME"
fi

# Confirm the jar really landed, matches, and is alone: two copies and NeoForge loads the mod twice.
if ! cmp -s "$JAR" "$MODS/$JARNAME"; then
    echo "!! The deployed jar does not match the one just built." >&2
    exit 1
fi
COUNT="$(find "$MODS" -maxdepth 1 -name 'zombiemod-*.jar' | wc -l)"
if [ "$COUNT" != 1 ]; then
    echo "!! $COUNT zombiemod jars are in $MODS - the instance will not start. Remove the extras." >&2
    exit 1
fi

echo ">> Deployed: $JARNAME ($(stat -c%s "$JAR") bytes, build $STAMP)"
echo ">> Launch the '$NAME' instance in CurseForge to test."
