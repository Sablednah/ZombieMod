#!/usr/bin/env bash
# Build ZombieMod and copy the jar into the CurseForge NeoForge test instance's mods/ folder,
# then launch that instance from CurseForge to see the mod live.
#
# Usage: ./deploy.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
# This repo bundles no JDK; borrow the one from the first port in the series.
export JAVA_HOME="${JAVA_HOME:-/mnt/d/Repos/sable/MobHealth-Forge/tools/jdk21}"
export PATH="$JAVA_HOME/bin:$PATH"

INSTANCE="/mnt/c/Users/darre/curseforge/minecraft/Instances/MobHealth - Forge"
MODS="$INSTANCE/mods"

echo ">> Building ZombieMod..."
"$ROOT/gradlew" build --console=plain

if [ ! -d "$MODS" ]; then
    echo "!! Instance mods folder not found: $MODS" >&2
    exit 1
fi

JAR="$(ls -t "$ROOT"/build/libs/zombiemod-*.jar 2>/dev/null | grep -v -- '-sources' | head -1 || true)"
if [ -z "$JAR" ]; then
    echo "!! No built jar found in build/libs" >&2
    exit 1
fi

# A running instance holds the jar open, so Windows refuses to replace it. Say so plainly: this
# otherwise fails looking like a success, and you test a stale jar wondering why nothing changed.
instance_locked() {
    echo "!! Could not $1 the jar in the instance's mods folder." >&2
    echo "!! Is the '$(basename "$INSTANCE")' instance still running? Close Minecraft and retry." >&2
    exit 1
}

echo ">> Removing previous ZombieMod jars from the instance..."
rm -f "$MODS"/zombiemod-*.jar || instance_locked "remove"

cp "$JAR" "$MODS/" || instance_locked "copy"

# Confirm the jar really landed and matches: a half-written copy is worse than a loud failure.
if ! cmp -s "$JAR" "$MODS/$(basename "$JAR")"; then
    echo "!! The deployed jar does not match the one just built." >&2
    exit 1
fi

echo ">> Deployed: $(basename "$JAR") ($(stat -c%s "$JAR") bytes)"
echo ">> Launch the '$(basename "$INSTANCE")' instance in CurseForge to test."
