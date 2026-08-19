#!/usr/bin/env bash
# Upload one built jar to the CurseForge project.
#
#   CURSEFORGE_TOKEN=xxx CURSEFORGE_PROJECT_ID=123456 \
#     ./scripts/curseforge-upload.sh <jar> <changelog-file> [minecraft-version] [release-type]
#
# Normally run for you by .github/workflows/curseforge.yml when a GitHub release is published, so
# publishing to GitHub publishes to CurseForge too. Runnable by hand for a re-upload.
#
# The Minecraft version defaults to `minecraft_version` in gradle.properties, so a single-version
# mod needs no argument and cannot drift from what was actually built. Pass one explicitly if the
# jar was built for something else.
#
# CurseForge wants numeric game-version IDs rather than names, and those IDs change as new versions
# are added, so they are looked up from the API every run instead of being hardcoded here.
#
# Uses python3 rather than jq: jq is not installed on the dev box, and python3 is, so this stays
# runnable locally as well as on a CI runner.
#
# NOTE: the CurseForge upload API can only add files to a project that already exists. Unlike
# Modrinth there is no create-project endpoint - make the project on the website first, then put its
# numeric ID in CURSEFORGE_PROJECT_ID.
#
# API reference: https://support.curseforge.com/en/support/solutions/articles/9000197321
set -euo pipefail

BASE="https://minecraft.curseforge.com"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

JAR="${1:?usage: curseforge-upload.sh <jar> <changelog-file> [minecraft-version] [release-type]}"
CHANGELOG_FILE="${2:?missing changelog file}"
MC_VERSION="${3:-$(sed -n 's/^minecraft_version=\(.*\)$/\1/p' "$HERE/gradle.properties")}"
RELEASE_TYPE="${4:-release}"

: "${CURSEFORGE_TOKEN:?set CURSEFORGE_TOKEN (create one at https://legacy.curseforge.com/account/api-tokens)}"
: "${CURSEFORGE_PROJECT_ID:?set CURSEFORGE_PROJECT_ID (the numeric ID on the CurseForge project page)}"

[ -n "$MC_VERSION" ] || { echo "!! Could not determine a Minecraft version." >&2; exit 1; }
[ -f "$JAR" ] || { echo "!! No such jar: $JAR" >&2; exit 1; }
[ -f "$CHANGELOG_FILE" ] || { echo "!! No such changelog: $CHANGELOG_FILE" >&2; exit 1; }

echo ">> Resolving CurseForge version IDs for Minecraft $MC_VERSION"
# Written to a file, not a shell variable: CurseForge lists every game version it has ever known
# and the response is hundreds of KB. Passing that to python through an environment variable or an
# argument dies with "Argument list too long" - the per-variable limit is around 128 KB - and the
# failure surfaces as a bogus "is the token valid?" because the resolver simply produced nothing.
VERSIONS_FILE="$(mktemp)"
trap 'rm -f "$VERSIONS_FILE"' EXIT
HTTP_CODE="$(curl -sS --max-time 120 -o "$VERSIONS_FILE" -w '%{http_code}' \
    -H "X-Api-Token: $CURSEFORGE_TOKEN" "$BASE/api/game/versions")"

if [ "$HTTP_CODE" != "200" ]; then
    echo "!! $BASE/api/game/versions returned HTTP $HTTP_CODE - is the token valid?" >&2
    head -c 400 "$VERSIONS_FILE" >&2; echo >&2; exit 1
fi

set +e
IDS="$(MC="$MC_VERSION" VERSIONS_FILE="$VERSIONS_FILE" python3 <<'PY'
import json, os, sys
try:
    with open(os.environ["VERSIONS_FILE"], encoding="utf-8") as fh:
        versions = json.load(fh)
except Exception as exc:
    print("PARSE_ERROR:%s" % exc); sys.exit(0)
if not isinstance(versions, list):
    print("PARSE_ERROR:expected a JSON array, got %s" % type(versions).__name__); sys.exit(0)
mc = os.environ["MC"]
def find(name):
    return next((v["id"] for v in versions if v.get("name") == name), None)
out = {"mc": find(mc), "loader": find("NeoForge"),
       "client": find("Client"), "server": find("Server"),
       # The 1.21 line ships the Java 21 runtime. Newer Minecraft lines ship 25.
       "java": find("Java 21" if mc.startswith("1.") else "Java 25")}
if out["mc"] is None:
    near = sorted({v["name"] for v in versions if v.get("name", "").startswith(mc.split(".")[0])})
    print("NO_MC:" + ", ".join(near[-12:]))
else:
    print(json.dumps(out))
PY
)"
PY_STATUS=$?
set -e

case "$IDS" in
    "")
        echo "!! The version resolver produced nothing (python exit $PY_STATUS)." >&2
        echo "!! Response was $(wc -c < "$VERSIONS_FILE") bytes; first 400:" >&2
        head -c 400 "$VERSIONS_FILE" >&2; echo >&2; exit 1 ;;
    PARSE_ERROR:*)
        echo "!! Could not parse $BASE/api/game/versions: ${IDS#PARSE_ERROR:}" >&2
        head -c 400 "$VERSIONS_FILE" >&2; echo >&2; exit 1 ;;
    NO_MC:*)
        echo "!! CurseForge does not list Minecraft '$MC_VERSION' yet." >&2
        echo "!! Closest names it does know: ${IDS#NO_MC:}" >&2
        echo "!! This is the expected failure right after a Minecraft release - CurseForge has to" >&2
        echo "!! add the version before anything can be uploaded against it." >&2
        exit 1 ;;
esac

read -r MC_ID LOADER_ID CLIENT_ID SERVER_ID JAVA_ID <<<"$(python3 -c '
import json,sys
d=json.loads(sys.argv[1])
print(*( "" if d[k] is None else d[k] for k in ("mc","loader","client","server","java")))' "$IDS")"

# CurseForge rejects an upload that names no environment ("You must select at least one version
# from the environment group of versions"). ZombieMod is tagged for both: it does its work on the
# server, and the same jar is a valid - optional - client install that adds the ZombieDex screen.
[ -n "$CLIENT_ID" ] && [ -n "$SERVER_ID" ] || {
    echo "!! Could not find the Client/Server environment tags CurseForge requires." >&2; exit 1; }
[ -n "$LOADER_ID" ] || echo "!! Warning: no 'NeoForge' modloader tag found; uploading without it." >&2
# Optional, unlike the environment: a Java version CurseForge has not added yet must not block a
# release, so warn and carry on.
[ -n "$JAVA_ID" ]   || echo "!! Warning: no matching Java tag found; uploading without one." >&2

GAME_VERSIONS="[$MC_ID${LOADER_ID:+,$LOADER_ID},$CLIENT_ID,$SERVER_ID${JAVA_ID:+,$JAVA_ID}]"
# Diagnostic mode: the smallest request CurseForge could possibly accept, to tell a content problem
# apart from a project-state one. Keeps the environment tags, which are mandatory.
if [ -n "${CURSEFORGE_MINIMAL:-}" ]; then
    GAME_VERSIONS="[$MC_ID${LOADER_ID:+,$LOADER_ID},$CLIENT_ID,$SERVER_ID]"
    CHANGELOG_FILE="$(mktemp)"; printf 'Initial release.' > "$CHANGELOG_FILE"
    echo "   (minimal mode: one-line changelog, no Java tag)"
fi
echo "   Minecraft $MC_VERSION = $MC_ID${LOADER_ID:+, NeoForge = $LOADER_ID}, Client = $CLIENT_ID, Server = $SERVER_ID${JAVA_ID:+, Java = $JAVA_ID}"

# "ZombieMod 3.0.0" reads better in the file list than the raw filename. Taken from the filename so
# it cannot disagree with the jar; falls back to the basename if a jar is ever named differently.
MOD_VERSION="$(basename "$JAR" .jar | sed -n 's/^zombiemod-\(.*\)$/\1/p')"
DISPLAY_NAME="${MOD_VERSION:+ZombieMod $MOD_VERSION}"
DISPLAY_NAME="${DISPLAY_NAME:-$(basename "$JAR" .jar)}"

METADATA="$(CHANGELOG="$CHANGELOG_FILE" DISPLAY="$DISPLAY_NAME" RTYPE="$RELEASE_TYPE" \
    GV="$GAME_VERSIONS" python3 -c '
import json,os,re
# CurseForge sanitises the changelog as HTML, and a Markdown angle-bracket autolink - <https://x> -
# reads to that sanitiser as a malformed tag. A changelog containing one made the upload spend 30
# seconds and then return HTTP 500, "An unhandled exception occurred", while the same upload with a
# one-line changelog succeeded instantly. Unwrap them; the bare URL still renders as a link.
text = open(os.environ["CHANGELOG"], encoding="utf-8").read()
text = re.sub(r"<((?:https?|ftp)://[^>\s]+)>", r"\1", text)
print(json.dumps({
  "changelog": text,
  "changelogType": "markdown",
  "displayName": os.environ["DISPLAY"],
  "releaseType": os.environ["RTYPE"],
  "gameVersions": json.loads(os.environ["GV"]),
}))')"

if [ -n "${CURSEFORGE_DEBUG:-}" ]; then
    # The metadata carries no credentials, so it is safe to print when diagnosing a rejection.
    echo ">> metadata:"; python3 -m json.tool <<<"$METADATA" | sed 's/^/     /'
fi

echo ">> Uploading $(basename "$JAR") as '$DISPLAY_NAME' to project $CURSEFORGE_PROJECT_ID ($RELEASE_TYPE)"
# --form-string, not -F: curl gives ';', a leading '@' and a leading '<' special meaning inside an
# -F value, and a changelog containing any of them silently mangles the JSON. CurseForge then
# answers "Error in field `metadata`: Invalid JSON", which reads like a bug in the JSON we built.
# --form-string sends the value literally. The jar still needs -F, since @ there is the point.
RESPONSE="$(curl -sS --max-time 600 -w '\n%{http_code}' \
    -H "X-Api-Token: $CURSEFORGE_TOKEN" \
    --form-string "metadata=$METADATA" \
    -F "file=@$JAR" \
    "$BASE/api/projects/$CURSEFORGE_PROJECT_ID/upload-file")"

STATUS="$(tail -n1 <<<"$RESPONSE")"
BODY="$(sed '$d' <<<"$RESPONSE")"

if [ "$STATUS" = "200" ]; then
    FILE_ID="$(python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("id",""))
except Exception: pass' <<<"$BODY" 2>/dev/null || true)"
    echo ">> Uploaded${FILE_ID:+ as file $FILE_ID}"
    # A 200 means CurseForge accepted the file, NOT that it is published. Moderation runs afterwards
    # and can still reject it - most often as a duplicate, because CurseForge dedupes by file
    # content and will not host the same jar twice on one project. Re-running an upload for a
    # release that is already up therefore produces rejections, not duplicates. Rejected files are
    # hidden from the authors file list by default, so they look like they simply never arrived.
    echo ">> Note: moderation runs after this. The authors file list is the authoritative view -"
    echo "   the public Files tab lags behind it:"
    echo "   https://authors.curseforge.com/#/projects/$CURSEFORGE_PROJECT_ID/files"
    exit 0
fi

echo "!! CurseForge rejected the upload (HTTP $STATUS)" >&2
echo "$BODY" >&2
if [ "$STATUS" = "500" ]; then
    echo "!!" >&2
    echo "!! A 500 is CurseForge failing, not a bad request - a rejected field gives a 400 naming" >&2
    echo "!! it. The known cause is the CHANGELOG, not the project: CurseForge sanitises it as HTML," >&2
    echo "!! and constructs it cannot parse throw server-side after a long pause. An angle-bracket" >&2
    echo "!! autolink <https://x> did exactly this once; those are now unwrapped automatically, so" >&2
    echo "!! the next suspect is the Markdown table. Re-run with -f minimal=true to confirm it is" >&2
    echo "!! the changelog: minimal sends a one-line one and nothing else changes." >&2
fi
exit 1
