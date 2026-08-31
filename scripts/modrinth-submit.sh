#!/usr/bin/env bash
# Submit the Modrinth project to moderation. THIS IS THE STEP THAT MAKES IT PUBLIC.
#
#   MODRINTH_TOKEN=xxx ./scripts/modrinth-submit.sh
#
# Kept out of the create and upload scripts on purpose: creating a draft and adding files is
# reversible and private, and this is neither. Run it only when the page reads the way you want.
#
# Modrinth will not accept a project with no versions, so upload at least one jar first.
set -euo pipefail

API="https://api.modrinth.com/v2"
SLUG="${MODRINTH_SLUG:-zombiemod-reforged}"
: "${MODRINTH_TOKEN:?set MODRINTH_TOKEN (a PAT with the PROJECT_WRITE scope)}"

UA="Sablednah/ZombieMod (sablecraft.co.uk)"
AUTH="Authorization: $MODRINTH_TOKEN"

INFO="$(curl -sS -H "$AUTH" -H "User-Agent: $UA" "$API/project/$SLUG")"
python3 -c '
import json, sys
p = json.load(sys.stdin)
print(">> %s (%s)" % (p.get("title"), p.get("slug")))
print("   status:   %s" % p.get("status"))
print("   versions: %d" % len(p.get("versions") or []))
print("   gallery:  %d" % len(p.get("gallery") or []))
print("   icon:     %s" % ("set" if p.get("icon_url") else "MISSING"))
if not p.get("versions"):
    print("!! No versions uploaded. Modrinth will not accept a project with no files.", file=sys.stderr)
    sys.exit(1)
' <<<"$INFO"

echo ">> Submitting for review"
STATUS="$(curl -sS --max-time 120 -o /tmp/modrinth-submit.out -w '%{http_code}' \
    -X PATCH -H "$AUTH" -H "User-Agent: $UA" -H "Content-Type: application/json" \
    --data '{"requested_status":"approved"}' "$API/project/$SLUG")"

case "$STATUS" in
    2*) echo ">> Submitted. Moderation runs now; the page stays private until it passes."
        echo "   https://modrinth.com/mod/$SLUG" ;;
    *)  echo "!! Submit failed (HTTP $STATUS)" >&2; cat /tmp/modrinth-submit.out >&2; exit 1 ;;
esac
