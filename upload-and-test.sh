#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
#  upload-and-test.sh
#
#  Uploads an APK to Firebase App Distribution and immediately
#  triggers the smoke test suite on GitHub Actions.
#
# ───────────────────────────────────────────────────────────────────
#  ONE-TIME SETUP
#
#  1. Install Firebase CLI (if not already installed):
#       npm install -g firebase-tools
#
#  2. Log in to Firebase:
#       firebase login
#
#  3. Create a GitHub Personal Access Token (PAT):
#       → github.com → Settings → Developer settings
#       → Personal access tokens → Tokens (classic)
#       → Generate new token → check "repo" scope → copy token
#
#  4. Export your token (add to ~/.bashrc or ~/.zshrc to make permanent):
#       export GH_PAT="ghp_xxxxxxxxxxxxxxxxxxxx"
#
# ───────────────────────────────────────────────────────────────────
#  USAGE
#
#   ./upload-and-test.sh <path-to-apk> [version-label]
#
#  EXAMPLES
#   ./upload-and-test.sh app-release.apk
#   ./upload-and-test.sh app-release.apk "v2.1.0"
#   ./upload-and-test.sh "Downloads/app (1).apk" "v2.1.0"
#
# ═══════════════════════════════════════════════════════════════════

# ── Project config (do not change) ─────────────────────────────────
FIREBASE_APP_ID="1:201489390325:android:d73b9e70f8a250f3c403bc"
GITHUB_REPO="marathi1990/appium"
GITHUB_EVENT="firebase-new-build"

# ── Colors ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

APK_PATH="${1}"
VERSION="${2:-$(date '+v%Y-%m-%d')}"
APK_NAME="$(basename "$APK_PATH")"

echo ""
echo -e "${BOLD}╔══════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║      Firebase Upload + Smoke Tests       ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════════╝${RESET}"
echo ""

# ── Validate: APK path provided ────────────────────────────────────
if [ -z "$APK_PATH" ]; then
  echo -e "${RED}ERROR: No APK path provided.${RESET}"
  echo ""
  echo "Usage:   ./upload-and-test.sh <apk-path> [version]"
  echo "Example: ./upload-and-test.sh app-release.apk \"v2.1.0\""
  exit 1
fi

# ── Validate: APK file exists ───────────────────────────────────────
if [ ! -f "$APK_PATH" ]; then
  echo -e "${RED}ERROR: APK file not found:${RESET} $APK_PATH"
  exit 1
fi

# ── Validate: GH_PAT is set ────────────────────────────────────────
if [ -z "$GH_PAT" ]; then
  echo -e "${RED}ERROR: GH_PAT is not set.${RESET}"
  echo ""
  echo "Run this first:"
  echo -e "  ${CYAN}export GH_PAT=\"ghp_xxxxxxxxxxxxxxxxxxxx\"${RESET}"
  echo ""
  echo "To make it permanent, add it to your ~/.bashrc or ~/.zshrc"
  exit 1
fi

# ── Validate: Firebase CLI installed ───────────────────────────────
if ! command -v firebase &> /dev/null; then
  echo -e "${RED}ERROR: Firebase CLI not found.${RESET}"
  echo ""
  echo "Install it with:"
  echo -e "  ${CYAN}npm install -g firebase-tools${RESET}"
  exit 1
fi

echo -e "  ${CYAN}APK${RESET}     : $APK_PATH"
echo -e "  ${CYAN}Version${RESET} : $VERSION"
echo -e "  ${CYAN}Repo${RESET}    : $GITHUB_REPO"
echo ""

# ══════════════════════════════════════════════════════════════════
#  STEP 1 — Upload to Firebase App Distribution
# ══════════════════════════════════════════════════════════════════
echo -e "${BOLD}[ 1/2 ] Uploading to Firebase App Distribution...${RESET}"
echo ""

firebase appdistribution:distribute "$APK_PATH" \
  --app "$FIREBASE_APP_ID" \
  --release-notes "$VERSION"

if [ $? -ne 0 ]; then
  echo ""
  echo -e "${RED}✖  Firebase upload failed. Smoke tests not triggered.${RESET}"
  exit 1
fi

echo ""
echo -e "${GREEN}✔  Upload complete.${RESET}"
echo ""

# ══════════════════════════════════════════════════════════════════
#  STEP 2 — Trigger GitHub Actions smoke tests
# ══════════════════════════════════════════════════════════════════
echo -e "${BOLD}[ 2/2 ] Triggering smoke tests on GitHub Actions...${RESET}"
echo ""

HTTP_STATUS=$(curl -s -o /tmp/gh_response.txt -w "%{http_code}" \
  -X POST \
  -H "Authorization: Bearer $GH_PAT" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${GITHUB_REPO}/dispatches" \
  -d "{
    \"event_type\": \"${GITHUB_EVENT}\",
    \"client_payload\": {
      \"display_version\": \"${VERSION}\",
      \"apk_name\": \"${APK_NAME}\",
      \"triggered_by\": \"upload-and-test-script\"
    }
  }")

if [ "$HTTP_STATUS" = "204" ]; then
  echo -e "${GREEN}✔  Smoke tests triggered!${RESET}"
  echo ""
  echo -e "  ${BOLD}Build   :${RESET} $VERSION"
  echo -e "  ${BOLD}Actions :${RESET} https://github.com/${GITHUB_REPO}/actions"
  echo ""
  echo -e "${BOLD}╔══════════════════════════════════════════╗${RESET}"
  echo -e "${BOLD}║  Done! Check GitHub Actions for results. ║${RESET}"
  echo -e "${BOLD}╚══════════════════════════════════════════╝${RESET}"
  echo ""
else
  echo -e "${RED}✖  Failed to trigger GitHub Action (HTTP $HTTP_STATUS)${RESET}"
  echo ""
  echo "Response from GitHub:"
  cat /tmp/gh_response.txt
  echo ""
  exit 1
fi
