"use strict";

/**
 * Firebase Cloud Functions
 *
 * 1. onNewAppDistributionBuild — Eventarc trigger: fires smoke tests
 *    when a new build is uploaded to Firebase App Distribution.
 *
 * 2. testDispatch — HTTP endpoint to manually test the dispatch.
 *
 * 3. slackTriggerTests — Slack slash command handler.
 *    POST from Slack → triggers "Dynamic Parallel Tests" workflow on GitHub.
 *    Usage in Slack:  /run-tests <tags> [parallel]
 *    Examples:
 *      /run-tests smoke
 *      /run-tests smoke,regression1 3
 *
 * Required Firebase secrets (set once):
 *   firebase functions:secrets:set GH_PAT
 *   firebase functions:secrets:set GH_REPO_OWNER        (e.g. marathi1990)
 *   firebase functions:secrets:set GH_REPO_NAME         (e.g. appium)
 *   firebase functions:secrets:set SLACK_SIGNING_SECRET  (from api.slack.com/apps)
 */

const { onCustomEventPublished } = require("firebase-functions/v2/eventarc");
const { onRequest }              = require("firebase-functions/v2/https");
const { defineSecret }           = require("firebase-functions/params");
const { logger }                 = require("firebase-functions");
const https                      = require("https");
const crypto                     = require("crypto");

// ── Secrets (injected at runtime, never stored in source) ──────────
const GH_PAT               = defineSecret("GH_PAT");
const GH_REPO_OWNER        = defineSecret("GH_REPO_OWNER");
const GH_REPO_NAME         = defineSecret("GH_REPO_NAME");
const SLACK_SIGNING_SECRET = defineSecret("SLACK_SIGNING_SECRET");

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

// ── Slack slash command: /run-tests <tags> [parallel] ─────────────
//
//   Set up once in Slack:
//     api.slack.com/apps → Your App → Slash Commands → Create New Command
//       Command         : /run-tests
//       Request URL     : https://us-central1-<project>.cloudfunctions.net/slackTriggerTests
//       Short Desc      : Trigger parallel Appium tests on GitHub Actions
//       Usage Hint      : <tags> [1-5]
//
//   Then copy the Signing Secret from Basic Information → App Credentials and run:
//     firebase functions:secrets:set SLACK_SIGNING_SECRET
//
exports.slackTriggerTests = onRequest(
  {
    secrets: [GH_PAT, GH_REPO_OWNER, GH_REPO_NAME, SLACK_SIGNING_SECRET],
    region:  "us-central1",
  },
  async (req, res) => {
    if (req.method !== "POST") {
      return res.status(405).send("Method Not Allowed");
    }

    // ── Verify the request came from Slack ──────────────────────────
    const slackTimestamp = req.headers["x-slack-request-timestamp"];
    const slackSignature = req.headers["x-slack-signature"];

    if (!slackTimestamp || !slackSignature) {
      return res.status(400).send("Missing Slack signature headers");
    }

    // Reject replayed requests older than 5 minutes
    if (Math.abs(Date.now() / 1000 - Number(slackTimestamp)) > 300) {
      return res.status(400).send("Request timestamp too old");
    }

    const rawBody     = req.rawBody ? req.rawBody.toString() : "";
    const sigBaseStr  = `v0:${slackTimestamp}:${rawBody}`;
    const hmac        = crypto.createHmac("sha256", SLACK_SIGNING_SECRET.value());
    hmac.update(sigBaseStr);
    const computedSig = `v0=${hmac.digest("hex")}`;

    if (computedSig !== slackSignature) {
      logger.warn("slackTriggerTests: invalid Slack signature");
      return res.status(401).send("Invalid signature");
    }

    // ── Parse slash command text: "<tags> [parallel]" ───────────────
    //   /run-tests smoke                → tags=smoke, parallel=5
    //   /run-tests smoke,regression1 3  → tags=smoke,regression1, parallel=3
    const parts    = (req.body.text || "").trim().split(/\s+/);
    const tags     = parts[0] || "regression1";
    const parallel = parts[1] || "5";

    const validParallel = ["1", "2", "3", "4", "5"];
    if (!validParallel.includes(parallel)) {
      return res.status(200).json({
        response_type: "ephemeral",
        text: `:warning: Invalid parallel value *${parallel}* — must be 1–5.\n` +
              `Usage: \`/run-tests <tags> [1-5]\`\n` +
              `Example: \`/run-tests smoke,regression1 3\``,
      });
    }

    const owner = GH_REPO_OWNER.value();
    const repo  = GH_REPO_NAME.value();
    const token = GH_PAT.value();

    if (!owner || !repo || !token) {
      logger.error("slackTriggerTests: missing GH secrets");
      return res.status(200).json({
        response_type: "ephemeral",
        text: ":x: Server misconfiguration — GitHub secrets are not set.",
      });
    }

    try {
      await dispatchWorkflow({ owner, repo, token, tags, parallel });
      logger.info(`slackTriggerTests: dispatched tags="${tags}" parallel=${parallel}`);

      const actionsUrl = `https://github.com/${owner}/${repo}/actions`;
      return res.status(200).json({
        response_type: "in_channel",
        text: `:rocket: *Parallel tests triggered!*\n` +
              `• Tags: \`${tags}\`\n` +
              `• Max concurrent runners: \`${parallel}\`\n` +
              `• <${actionsUrl}|View run on GitHub Actions>`,
      });
    } catch (err) {
      logger.error(`slackTriggerTests: dispatch failed — ${err.message}`);
      return res.status(200).json({
        response_type: "ephemeral",
        text: `:x: Failed to trigger tests: ${err.message}`,
      });
    }
  }
);

// ── Helper: call GitHub workflow_dispatch API ──────────────────────
//   Triggers parallel-tests.yml with the given tags and parallel inputs.
function dispatchWorkflow({ owner, repo, token, tags, parallel }) {
  const payload = JSON.stringify({
    ref: "main",
    inputs: { tags, parallel },
  });

  return new Promise((resolve, reject) => {
    const options = {
      hostname: "api.github.com",
      path:     `/repos/${owner}/${repo}/actions/workflows/parallel-tests.yml/dispatches`,
      method:   "POST",
      headers: {
        "Content-Type":         "application/json",
        "Content-Length":       Buffer.byteLength(payload),
        "Accept":               "application/vnd.github+json",
        "Authorization":        `Bearer ${token}`,
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent":           "firebase-slack-trigger/1.0",
      },
    };

    const req = https.request(options, (ghRes) => {
      let body = "";
      ghRes.on("data", (chunk) => { body += chunk; });
      ghRes.on("end", () => {
        if (ghRes.statusCode === 204) {
          resolve();
        } else {
          reject(new Error(`GitHub API returned HTTP ${ghRes.statusCode}: ${body}`));
        }
      });
    });

    req.on("error", reject);
    req.write(payload);
    req.end();
  });
}

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
