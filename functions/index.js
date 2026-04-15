"use strict";

/**
 * Firebase Cloud Function — App Distribution New Build Webhook
 *
 * Triggered by Eventarc whenever a new release is created in
 * Firebase App Distribution.  Immediately fires a GitHub
 * repository_dispatch event so firebase-tests.yml runs without
 * any polling delay.
 *
 * Required Firebase secrets (set once):
 *   firebase functions:secrets:set GH_PAT
 *   firebase functions:secrets:set GH_REPO_OWNER   (e.g. marathi1990)
 *   firebase functions:secrets:set GH_REPO_NAME    (e.g. Appium-JAVA-Firebase)
 */

const { onCustomEventPublished } = require("firebase-functions/v2/eventarc");
const { onRequest }              = require("firebase-functions/v2/https");
const { defineSecret }           = require("firebase-functions/params");
const { logger }                 = require("firebase-functions");
const https                      = require("https");

// ── Secrets (injected at runtime, never stored in source) ──────────
const GH_PAT        = defineSecret("GH_PAT");
const GH_REPO_OWNER = defineSecret("GH_REPO_OWNER");
const GH_REPO_NAME  = defineSecret("GH_REPO_NAME");

// ── Eventarc trigger: fires when a new build is uploaded ───────────
exports.onNewAppDistributionBuild = onCustomEventPublished(
  {
    eventType: "google.firebase.appdistribution.release.v1.created",
    secrets:   [GH_PAT, GH_REPO_OWNER, GH_REPO_NAME],
    region:    "us-central1",
  },
  async (event) => {
    const release = event.data || {};

    const releaseName    = release.name            || "unknown";
    const buildVersion   = release.buildVersion    || "0";
    const displayVersion = release.displayVersion  || "unknown";
    const createTime     = release.createTime      || new Date().toISOString();

    logger.info("New App Distribution release detected", {
      releaseName,
      displayVersion,
      buildVersion,
      createTime,
    });

    const owner = GH_REPO_OWNER.value();
    const repo  = GH_REPO_NAME.value();
    const token = GH_PAT.value();

    if (!owner || !repo || !token) {
      logger.error("Missing one or more required secrets: GH_PAT, GH_REPO_OWNER, GH_REPO_NAME");
      logger.error("Run: firebase functions:secrets:set GH_PAT && firebase functions:secrets:set GH_REPO_OWNER && firebase functions:secrets:set GH_REPO_NAME");
      return;
    }

    const payload = JSON.stringify({
      event_type: "firebase-new-build",
      client_payload: {
        release_name:    releaseName,
        build_version:   buildVersion,
        display_version: displayVersion,
        create_time:     createTime,
        triggered_by:    "firebase-eventarc",
      },
    });

    try {
      await dispatchToGitHub({ owner, repo, token, payload });
      logger.info(`Successfully dispatched firebase-new-build to ${owner}/${repo}`);
    } catch (err) {
      logger.error(`Failed to dispatch to GitHub: ${err.message}`);
      throw err;
    }
  }
);

// ── HTTP trigger: call manually to test the dispatch without a build ─
//
//   POST https://<region>-<project>.cloudfunctions.net/testDispatch
//   Authorization: Bearer <GH_PAT_value>     (simple auth guard)
//
exports.testDispatch = onRequest(
  {
    secrets: [GH_PAT, GH_REPO_OWNER, GH_REPO_NAME],
    region:  "us-central1",
  },
  async (req, res) => {
    if (req.method !== "POST") {
      return res.status(405).json({ error: "POST only" });
    }

    const owner = GH_REPO_OWNER.value();
    const repo  = GH_REPO_NAME.value();
    const token = GH_PAT.value();

    if (!owner || !repo || !token) {
      logger.error("testDispatch: missing secrets");
      return res.status(500).json({
        error: "Missing secrets. Set GH_PAT, GH_REPO_OWNER, GH_REPO_NAME via firebase functions:secrets:set",
      });
    }

    const payload = JSON.stringify({
      event_type: "firebase-new-build",
      client_payload: {
        release_name:    "manual-test",
        build_version:   "0",
        display_version: "manual",
        create_time:     new Date().toISOString(),
        triggered_by:    "manual-http-test",
      },
    });

    try {
      await dispatchToGitHub({ owner, repo, token, payload });
      logger.info(`testDispatch: successfully dispatched to ${owner}/${repo}`);
      return res.status(200).json({
        success: true,
        message: `Dispatched firebase-new-build to ${owner}/${repo}`,
      });
    } catch (err) {
      logger.error(`testDispatch failed: ${err.message}`);
      return res.status(500).json({ error: err.message });
    }
  }
);

// ── Helper: call GitHub repository_dispatch API ────────────────────
function dispatchToGitHub({ owner, repo, token, payload }) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "api.github.com",
      path:     `/repos/${owner}/${repo}/dispatches`,
      method:   "POST",
      headers: {
        "Content-Type":         "application/json",
        "Content-Length":       Buffer.byteLength(payload),
        "Accept":               "application/vnd.github+json",
        "Authorization":        `Bearer ${token}`,
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent":           "firebase-appdistribution-webhook/1.0",
      },
    };

    const req = https.request(options, (res) => {
      let body = "";
      res.on("data", (chunk) => { body += chunk; });
      res.on("end", () => {
        if (res.statusCode === 204) {
          resolve();
        } else {
          reject(new Error(
            `GitHub API returned HTTP ${res.statusCode}: ${body}`
          ));
        }
      });
    });

    req.on("error", reject);
    req.write(payload);
    req.end();
  });
}
