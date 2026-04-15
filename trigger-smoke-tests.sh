#!/bin/bash
# ─────────────────────────────────────────────────────────────────
# trigger-smoke-tests.sh
#
# Run this after uploading a new build to Firebase App Distribution.
# It triggers the smoke test suite on GitHub Actions automatically.
#
# SETUP (one-time):
#   export GH_PAT="your_github_personal_access_token"
#
# USAGE:
#   ./trigger-smoke-tests.sh
#   ./trigger-smoke-tests.sh "v2.1.0"        ← optional version label
# ─────────────────────────────────────────────────────────────────

VERSION="${1:-unknown}"

if [ -z "$GH_PAT" ]; then
  echo "ERROR: GH_PAT environment variable is not set."
  echo "Run:  export GH_PAT=your_github_personal_access_token"
  exit 1
fi

echo "Triggering smoke tests for build: $VERSION ..."

HTTP_STATUS=$(curl -s -o /tmp/gh_response.txt -w "%{http_code}" \
  -X POST \
  -H "Authorization: Bearer $GH_PAT" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/marathi1990/appium/dispatches \
  -d "{
    \"event_type\": \"firebase-new-build\",
    \"client_payload\": {
      \"display_version\": \"$VERSION\",
      \"triggered_by\": \"dev-script\"
    }
  }")

if [ "$HTTP_STATUS" = "204" ]; then
  echo "✅ Smoke tests triggered successfully!"
  echo "   View run: https://github.com/marathi1990/appium/actions"
else
  echo "❌ Failed to trigger (HTTP $HTTP_STATUS)"
  cat /tmp/gh_response.txt
  exit 1
fi
