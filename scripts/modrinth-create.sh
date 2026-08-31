#!/usr/bin/env bash
# Create the Modrinth project, set its icon, and upload the gallery.
#
#   MODRINTH_TOKEN=xxx ./scripts/modrinth-create.sh
#
# Normally run for you by .github/workflows/modrinth.yml via "Run workflow" -> create-project, so
# the token never has to leave GitHub's secret store. Runnable by hand with a PAT that has the
# PROJECT_CREATE and PROJECT_WRITE scopes.
#
# Safe to re-run. If the project already exists it skips creation and still refreshes the icon and
# any gallery image that is missing, so a failed run halfway through is fixed by running it again.
#
# The project lands as a DRAFT - private, not in search. Nothing is public until a version exists
# and ./scripts/modrinth-submit.sh sends it to moderation, which is a deliberate second step.
#
# Copy comes from RELEASE.md, which is the single source for every store string. If a tagline
# changes, change it there first.
#
# API reference: https://docs.modrinth.com/api/  (spec: https://docs.modrinth.com/openapi.yaml)
set -euo pipefail

API="https://api.modrinth.com/v2"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SLUG="${MODRINTH_SLUG:-zombiemod-reforged}"
ICON="$HERE/docs/modrinth-icon.png"
BODY_FILE="$HERE/CURSEFORGE.md"

: "${MODRINTH_TOKEN:?set MODRINTH_TOKEN (create one at https://modrinth.com/settings/pats with the PROJECT_CREATE and PROJECT_WRITE scopes)}"

# Modrinth asks for a descriptive User-Agent and rate-limits anonymous-looking clients harder.
UA="Sablednah/ZombieMod (sablecraft.co.uk)"
# Note: no "Bearer" prefix. Modrinth takes the raw token as the Authorization header.
AUTH="Authorization: $MODRINTH_TOKEN"

[ -f "$ICON" ]      || { echo "!! No icon at $ICON - regenerate it, see RELEASE.md" >&2; exit 1; }
[ -f "$BODY_FILE" ] || { echo "!! No description at $BODY_FILE" >&2; exit 1; }

# The icon must be square and under 256 KiB. Checked here rather than discovered as a 400, because
# the failure reads as an auth problem otherwise.
ICON_BYTES="$(wc -c < "$ICON")"
[ "$ICON_BYTES" -le 262144 ] || {
    echo "!! $ICON is $ICON_BYTES bytes; Modrinth's icon limit is 262144 (256 KiB)." >&2
    echo "!! Re-quantise it - the generator is in RELEASE.md." >&2; exit 1; }

api() {  # api <method> <path> [curl args...] -> body on stdout, non-2xx is fatal
    local method="$1" path="$2"; shift 2
    local out
    out="$(curl -sS --max-time 300 -w '\n%{http_code}' -X "$method" \
        -H "$AUTH" -H "User-Agent: $UA" "$@" "$API$path")"
    local status body
    status="$(tail -n1 <<<"$out")"
    body="$(sed '$d' <<<"$out")"
    case "$status" in
        2*) printf '%s' "$body"; return 0 ;;
        *)  echo "!! $method $path failed (HTTP $status)" >&2
            echo "$body" >&2
            [ "$status" = 401 ] && echo "!! 401 means the token was rejected. Note Modrinth takes the raw token with NO 'Bearer ' prefix, and the PAT needs the PROJECT_CREATE / PROJECT_WRITE scopes." >&2
            return 1 ;;
    esac
}

# --- does it exist already? ---------------------------------------------------------------------
echo ">> Checking for an existing project at /$SLUG"
EXISTING="$(curl -sS -o /dev/null -w '%{http_code}' -H "$AUTH" -H "User-Agent: $UA" "$API/project/$SLUG")"

# The metadata, built once and used either way: POSTed to create the project, or PATCHed onto one
# that already exists so a re-run picks up an edited CURSEFORGE.md. Without that, fixing a typo in
# the description would mean editing it by hand on the website, and the file would stop being the
# source of truth.
#
# client_side / server_side are marked deprecated in favour of `environment`, but `environment` is a
# *version* field in v2, not a project one, and the deprecated pair is still required here.
# Server required + client optional is the exact truth: the mod does its work on the server, and a
# client that has it additionally gets the ZombieDex screen. Marking the client unsupported would be
# wrong now the client half exists; marking it required would send away the people the mod was
# built for.
DATA="$(BODY="$BODY_FILE" SLUG="$SLUG" python3 -c '
import json, os, sys
# The one-liner from RELEASE.md. Modrinth caps this at 256 characters; checked rather than
# discovered as a validation error naming the field but not the limit.
summary = ("61 zombie types with hand-built AI, and the JSON to write your own. "
           "Your players join with a vanilla client.")
if len(summary) > 256:
    sys.exit("summary is %d characters; Modrinth allows 256" % len(summary))
print(json.dumps({
  "slug": os.environ["SLUG"],
  "title": "ZombieMod ReForged",
  "description": summary,
  "body": open(os.environ["BODY"], encoding="utf-8").read(),
  "categories": ["mobs", "adventure", "game-mechanics"],
  "client_side": "optional",
  "server_side": "required",
  "license_id": "MIT",
  "project_type": "mod",
  "issues_url": "https://github.com/Sablednah/ZombieMod/issues",
  "source_url": "https://github.com/Sablednah/ZombieMod",
  "wiki_url": "https://sablecraft.co.uk/zombiemod-reforged/",
  # `initial_versions` and `is_draft` are marked DEPRECATED in the published spec, and the live v2
  # endpoint still REQUIRES them: leaving initial_versions out gives
  #   400 invalid_input "Error while parsing JSON: missing field `initial_versions`"
  # which reads like malformed JSON rather than a missing field. Send them empty and upload the
  # versions afterwards through /version, which is what the deprecation is steering you towards.
  "initial_versions": [],
  "is_draft": True,
}))')"

if [ "$EXISTING" = "200" ]; then
    echo "   Already exists - updating its description from $(basename "$BODY_FILE")."
    # PATCH takes JSON, not multipart, and rejects the fields that are create-only.
    PATCH_DATA="$(python3 -c '
import json, sys
d = json.load(sys.stdin)
# Create-only fields: PATCH rejects them.
for k in ("slug", "project_type", "client_side", "server_side",
          "initial_versions", "is_draft", "gallery_items"):
    d.pop(k, None)
print(json.dumps(d))' <<<"$DATA")"
    api PATCH "/project/$SLUG" -H "Content-Type: application/json" --data-binary "$PATCH_DATA" > /dev/null
else
    echo ">> Creating the project"
    # --form-string, not -F: curl gives ';', a leading '@' and a leading '<' special meaning inside
    # an -F value, and the description body is full-page Markdown that contains all three.
    api POST /project --form-string "data=$DATA" > /dev/null
    echo "   Created as a draft."
fi

# --- icon ---------------------------------------------------------------------------------------
# A dedicated endpoint rather than the `icon` part of the create call, so re-running this script
# updates the artwork on a project that already exists.
echo ">> Uploading the icon ($ICON_BYTES bytes)"
api PATCH "/project/$SLUG/icon?ext=png" \
    -H "Content-Type: image/png" --data-binary "@$ICON" > /dev/null

# --- gallery ------------------------------------------------------------------------------------
# Order and captions from RELEASE.md. The first two do the persuading, so they lead. Modrinth shows
# the featured image on the project card, so `giant.png` is featured: it is the only clean daylight
# shot with no HUD.
#
# file<TAB>featured<TAB>title<TAB>description
GALLERY=$(cat <<'ENTRIES'
giant.png	true	The Colossus	A giant zombie in a ruined high street, scaled against the tower blocks behind it.
boss.png	false	Patient Zero	Boss bar, darkened sky, a burning zombie beside him - and the tooltip reading minecraft:zombie, because that is all he ever was.
ZombieDex.png	false	The ZombieDex	A field guide to the dead: slain, met, and not yet found.
ZombieDex3.png	false	Every entry earns itself	The Corpse, wearing your own face and your own gear.
ZombieDex_book.png	false	The same dex on a vanilla client	Players who install the mod get the illustrated edition; players who do not get this written book.
Corpse.png	false	Your corpse gets up	Wearing your real skin, carrying your netherite. Kill it to get it back.
runners.png	false	Runners	Fast, fragile, and never alone.
swarmlings.png	false	Swarmlings	Half-size, trivial one at a time.
peekaboo.png	false	Climbers get close	The Hunter comes over the wall.
ENTRIES
)

# Whatever is already up, so a re-run adds only what is missing. Modrinth rejects a duplicate image
# with a 400 that does not say "duplicate", so this is worth doing rather than catching.
HAVE="$(curl -sS -H "$AUTH" -H "User-Agent: $UA" "$API/project/$SLUG" \
    | python3 -c 'import json,sys
try: print("\n".join(g.get("title") or "" for g in (json.load(sys.stdin).get("gallery") or [])))
except Exception: pass')"

ORDER=0
while IFS=$'\t' read -r FILE FEATURED TITLE DESC; do
    [ -n "${FILE:-}" ] || continue
    ORDER=$((ORDER + 1))
    IMG="$HERE/screenshots/$FILE"
    [ -f "$IMG" ] || { echo "   !! missing $IMG - skipping" >&2; continue; }
    if grep -Fxq "$TITLE" <<<"$HAVE"; then
        echo "   $ORDER. $FILE - already uploaded, skipping"
        continue
    fi
    echo "   $ORDER. $FILE  ($TITLE)"
    # Titles and captions are query parameters, so they must be percent-encoded - several contain
    # spaces, colons and commas.
    QS="$(F="$FEATURED" T="$TITLE" D="$DESC" O="$ORDER" python3 -c '
import os, urllib.parse
print(urllib.parse.urlencode({"ext": "png", "featured": os.environ["F"],
                              "title": os.environ["T"], "description": os.environ["D"],
                              "ordering": os.environ["O"]}))')"
    api POST "/project/$SLUG/gallery?$QS" \
        -H "Content-Type: image/png" --data-binary "@$IMG" > /dev/null
done <<<"$GALLERY"

echo
echo ">> Done. The project is a DRAFT and is not public yet:"
echo "   https://modrinth.com/mod/$SLUG"
echo ">> Next: upload a version (scripts/modrinth-upload.sh), then submit for review"
echo "   (scripts/modrinth-submit.sh). Modrinth will not accept a project with no files."
