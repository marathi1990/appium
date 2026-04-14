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
 *   firebase functions:secrets:set GH_REPO_NAME    (e.g. appium)
 */

const { onCustomEventPublished } = require("firebase-functions/v2/eventarc");
const { defineSecret }           = require("firebase-functions/params");
const { logger }                 = require("firebase-functions");
const https                      = require("https");

// ── Secrets (injected at runtime, never stored in source) ──────────
const GH_PAT        = defineSecret("GH_PAT");
const GH_REPO_OWNER = defineSecret("GH_REPO_OWNER");
const GH_REPO_NAME  = defineSecret("GH_REPO_NAME");

// ── Cloud Function ─────────────────────────────────────────────────
exports.onNewAppDistributionBuild = onCustomEventPublished(
  {
    eventType: "google.firebase.appdistribution.release.v1.created",
    secrets:   [GH_PAT, GH_REPO_OWNER, GH_REPO_NAME],
    // Deploy to the same region as your Firebase project
    region: "us-central1",
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

    await dispatchToGitHub({ owner, repo, token, payload });

    logger.info(`Dispatched firebase-new-build event to ${owner}/${repo}`);
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
        "Content-Type":  "application/json",
        "Content-Length": Buffer.byteLength(payload),
        "Accept":         "application/vnd.github+json",
        "Authorization":  `Bearer ${token}`,
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent":    "firebase-appdistribution-webhook/1.0",
      },
    };

    const req = https.request(options, (res) => {
      let body = "";
      res.on("data", (chunk) => { body += chunk; });
      res.on("end", () => {
        if (res.statusCode === 204) {
          // 204 No Content = success for repository_dispatch
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
