#!/usr/bin/env node
'use strict';
/**
 * generate-pdf-report.js
 *
 * Reads Allure *-result.json files → generates a styled, email-compatible HTML
 * (table-based layout — works in Gmail, Outlook, all email clients) →
 * converts to PDF via puppeteer-core + system Chrome (pre-installed on GitHub Actions).
 *
 * The HTML file is also used directly as the email html_body.
 *
 * Usage:
 *   NODE_PATH=/tmp/pdf-deps/node_modules node generate-pdf-report.js <resultsDir> <output.pdf>
 */

const fs   = require('fs');
const path = require('path');

// ── Config ────────────────────────────────────────────────────────────────────
const resultsDir = process.argv[2] || 'target/allure-results';
const outputPdf  = process.argv[3] || 'test-report.pdf';
const tmpHtml    = outputPdf.replace(/\.pdf$/i, '.html');

const RUN_NUMBER    = process.env.GITHUB_RUN_NUMBER || 'N/A';
const REPO          = process.env.GITHUB_REPOSITORY || 'N/A';
const BRANCH        = process.env.GITHUB_REF_NAME   || 'N/A';
const ACTOR         = process.env.GITHUB_ACTOR      || 'N/A';
const SERVER        = process.env.GITHUB_SERVER_URL || '';
const RUN_ID        = process.env.GITHUB_RUN_ID     || '';
const BUILD_VERSION = process.env.BUILD_VERSION     || '';
const RUN_URL       = SERVER && REPO && RUN_ID
  ? `${SERVER}/${REPO}/actions/runs/${RUN_ID}` : '';

// ── Parse Allure results ──────────────────────────────────────────────────────
const tests = [];
if (fs.existsSync(resultsDir)) {
  for (const file of fs.readdirSync(resultsDir).filter(f => f.endsWith('-result.json'))) {
    try {
      const r      = JSON.parse(fs.readFileSync(path.join(resultsDir, file), 'utf8'));
      const labels = r.labels || [];
      const suite  = (
        labels.find(l => l.name === 'testClass')?.value ||
        labels.find(l => l.name === 'suite')?.value ||
        r.fullName?.split('#')[0] || 'Unknown'
      ).split('.').pop();
      tests.push({
        name:     r.name    || 'Unknown',
        status:   (r.status || 'unknown').toLowerCase(),
        suite,
        duration: (r.stop && r.start) ? r.stop - r.start : 0,
        message:  r.statusDetails?.message?.split('\n')[0]?.trim() || '',
        tags:     labels.filter(l => l.name === 'tag').map(l => l.value),
      });
    } catch (_) { /* skip malformed files */ }
  }
}

// Sort: failed → broken → skipped → passed
const ORDER = { failed: 0, broken: 1, skipped: 2, passed: 3 };
tests.sort((a, b) => (ORDER[a.status] ?? 9) - (ORDER[b.status] ?? 9));

// ── Stats ─────────────────────────────────────────────────────────────────────
const total   = tests.length;
const passed  = tests.filter(t => t.status === 'passed').length;
const failed  = tests.filter(t => t.status === 'failed').length;
const broken  = tests.filter(t => t.status === 'broken').length;
const skipped = tests.filter(t => t.status === 'skipped').length;
const passRate = total > 0 ? Math.round((passed / total) * 100) : 0;

// ── Helpers ───────────────────────────────────────────────────────────────────
const esc = s => String(s ?? '')
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

function fmtMs(ms) {
  if (!ms)        return '&mdash;';
  if (ms < 1000)  return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`;
}

const BADGE_BG  = { passed: '#2E7D32', failed: '#B71C1C', broken: '#E65100', skipped: '#546E7A' };
const ROW_BG_A  = { passed: '#F1F8E9', failed: '#FFEBEE', broken: '#FFF3E0', skipped: '#FAFAFA' };
const ROW_BG_B  = { passed: '#E8F5E9', failed: '#FFCDD2', broken: '#FFE0B2', skipped: '#F5F5F5' };
const ICON      = { passed: '&#10003;', failed: '&#10007;', broken: '&#9888;', skipped: '&#9711;' };

const now = new Date().toLocaleString('en-IN', { timeZone: 'Asia/Kolkata', hour12: false });

// ── Progress bar (table-based — email-safe) ───────────────────────────────────
function barCell(n, color) {
  const w = total > 0 ? `${((n / total) * 100).toFixed(2)}%` : '0%';
  return n > 0
    ? `<td style="width:${w};background:${color};height:14px;padding:0;margin:0;line-height:0;font-size:0">&nbsp;</td>`
    : '';
}
const progressBarCells = [
  barCell(passed,  '#43A047'),
  barCell(failed,  '#E53935'),
  barCell(broken,  '#FB8C00'),
  barCell(skipped, '#9E9E9E'),
  total === 0 ? `<td style="width:100%;background:#CFD8DC;height:14px;padding:0"></td>` : '',
].join('');

// ── Summary card (table-cell — email-safe) ────────────────────────────────────
function card(count, label, bg) {
  return `
    <td style="padding:6px">
      <table style="width:120px;background:${bg};border-radius:10px;
          border-collapse:collapse;color:white;text-align:center">
        <tr><td style="padding:8px 10px 2px;font-size:10px;text-transform:uppercase;
            letter-spacing:1.2px;opacity:0.9;font-weight:600">${label}</td></tr>
        <tr><td style="padding:2px 10px 12px;font-size:34px;font-weight:700;
            line-height:1">${count}</td></tr>
      </table>
    </td>`;
}

// ── Test rows ─────────────────────────────────────────────────────────────────
const testRows = tests.map((t, i) => {
  const bg   = i % 2 === 0 ? (ROW_BG_A[t.status] || '#fff') : (ROW_BG_B[t.status] || '#f5f5f5');
  const bc   = BADGE_BG[t.status]  || '#546E7A';
  const icon = ICON[t.status] || '?';

  const msgEl = t.message
    ? `<div style="margin-top:4px;padding:3px 8px;background:#fff0f0;
        border-left:3px solid #EF9A9A;font-size:10px;font-family:Courier New,monospace;
        color:#B71C1C;overflow:hidden;max-width:420px">
        ${esc(t.message.substring(0, 200))}
       </div>` : '';

  const tagEls = t.tags.map(tg =>
    `<span style="display:inline-block;background:#E3F2FD;color:#1565C0;
        border-radius:3px;padding:1px 6px;font-size:10px;margin:0 0 0 5px;
        vertical-align:middle">${esc(tg)}</span>`
  ).join('');

  return `
  <tr style="background:${bg}">
    <td style="text-align:center;padding:9px 6px;border-bottom:1px solid #E0E0E0;width:115px">
      <span style="display:inline-block;background:${bc};color:white;
          border-radius:12px;padding:3px 12px;font-size:11px;font-weight:700;
          white-space:nowrap">${icon} ${t.status.toUpperCase()}</span>
    </td>
    <td style="padding:9px 12px;border-bottom:1px solid #E0E0E0">
      <span style="font-size:12px;font-weight:600;color:#212121">${esc(t.name)}</span>${tagEls}${msgEl}
    </td>
    <td style="padding:9px 12px;font-size:11px;color:#546E7A;
        white-space:nowrap;border-bottom:1px solid #E0E0E0;width:160px">${esc(t.suite)}</td>
    <td style="padding:9px 12px;text-align:center;font-size:11px;color:#546E7A;
        white-space:nowrap;border-bottom:1px solid #E0E0E0;width:70px">${fmtMs(t.duration)}</td>
  </tr>`;
}).join('');

// ── Full HTML (table-based, email-compatible) ─────────────────────────────────
const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Test Report &mdash; Run #${RUN_NUMBER}</title>
</head>
<body style="margin:0;padding:0;background:#ECEFF1;
    font-family:Arial,Helvetica,sans-serif;color:#333;font-size:13px">

<table width="100%" cellpadding="0" cellspacing="0" style="background:#ECEFF1">
<tr><td align="center" style="padding:0">

<!-- OUTER WRAPPER -->
<table width="700" cellpadding="0" cellspacing="0"
    style="width:700px;max-width:700px;background:#ECEFF1">

  <!-- ══ HEADER ══════════════════════════════════════════════════ -->
  <tr>
    <td style="background:linear-gradient(135deg,#0D47A1 0%,#1976D2 60%,#1E88E5 100%);
        padding:22px 28px 18px;border-bottom:5px solid #0A2F6B">
      <div style="font-size:22px;font-weight:700;color:white;
          letter-spacing:0.5px;margin-bottom:8px">
        Test Execution Report
      </div>
      <table cellpadding="0" cellspacing="0">
        <tr style="color:white;font-size:11px;opacity:0.88">
          <td style="padding-right:18px">&#9658; Run <strong>#${RUN_NUMBER}</strong></td>
          ${BUILD_VERSION ? `<td style="padding-right:18px">&#9658; Build: <strong>${esc(BUILD_VERSION)}</strong></td>` : ''}
          <td style="padding-right:18px">&#9658; ${esc(REPO)}</td>
          <td style="padding-right:18px">&#9658; Branch: <strong>${esc(BRANCH)}</strong></td>
          <td style="padding-right:18px">&#9658; By: <strong>${esc(ACTOR)}</strong></td>
          <td>&#9658; ${now}</td>
        </tr>
      </table>
    </td>
  </tr>

  <!-- ══ SUMMARY CARDS ════════════════════════════════════════════ -->
  <tr>
    <td style="background:white;padding:6px 16px 6px;
        border-bottom:1px solid #CFD8DC">
      <table cellpadding="0" cellspacing="0" width="100%">
        <tr>
          ${card(total,   'Total',   '#1565C0')}
          ${card(passed,  'Passed',  '#2E7D32')}
          ${card(failed,  'Failed',  '#B71C1C')}
          ${card(broken,  'Broken',  '#E65100')}
          ${card(skipped, 'Skipped', '#546E7A')}
        </tr>
      </table>
    </td>
  </tr>

  <!-- ══ PASS RATE + PROGRESS BAR ═════════════════════════════════ -->
  <tr>
    <td style="background:white;padding:12px 22px 14px;
        border-bottom:1px solid #CFD8DC">

      <!-- Pass rate text -->
      <table cellpadding="0" cellspacing="0" width="100%"
          style="margin-bottom:8px">
        <tr>
          <td style="font-size:13px;color:#333">
            Pass Rate:
            <strong style="font-size:18px;color:#2E7D32">${passRate}%</strong>
            &nbsp;&nbsp;
            <span style="font-size:11px;color:#2E7D32;font-weight:600">
              &#9632; ${passed} Passed
            </span>&nbsp;
            <span style="font-size:11px;color:#B71C1C;font-weight:600">
              &#9632; ${failed} Failed
            </span>&nbsp;
            <span style="font-size:11px;color:#E65100;font-weight:600">
              &#9632; ${broken} Broken
            </span>&nbsp;
            <span style="font-size:11px;color:#546E7A;font-weight:600">
              &#9632; ${skipped} Skipped
            </span>
          </td>
        </tr>
      </table>

      <!-- Progress bar (table-based, email-safe) -->
      <table width="100%" cellpadding="0" cellspacing="0"
          style="border-radius:7px;overflow:hidden;background:#CFD8DC;height:14px">
        <tr style="height:14px">
          ${progressBarCells}
        </tr>
      </table>

    </td>
  </tr>

  <!-- ══ SECTION TITLE ════════════════════════════════════════════ -->
  <tr>
    <td style="background:#ECEFF1;padding:16px 22px 8px">
      <table cellpadding="0" cellspacing="0">
        <tr>
          <td style="font-size:14px;font-weight:700;color:#0D47A1">
            Test Cases
          </td>
          <td style="padding-left:8px">
            <span style="background:#0D47A1;color:white;border-radius:12px;
                padding:2px 12px;font-size:12px;font-weight:600">${total}</span>
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <!-- ══ TEST TABLE ═══════════════════════════════════════════════ -->
  <tr>
    <td style="padding:0 22px 24px">
      ${total === 0
        ? `<table width="100%" cellpadding="0" cellspacing="0"
              style="background:white;border-radius:10px">
            <tr>
              <td style="text-align:center;padding:50px;color:#90A4AE;font-size:14px">
                No test results found in: ${esc(resultsDir)}
              </td>
            </tr>
          </table>`
        : `<table width="100%" cellpadding="0" cellspacing="0"
              style="background:white;border-radius:10px;overflow:hidden;
              box-shadow:0 2px 8px rgba(0,0,0,0.10)">
            <!-- Header row -->
            <tr style="background:#263238;color:white">
              <td style="padding:11px 6px;text-align:center;font-size:11px;
                  text-transform:uppercase;letter-spacing:0.5px;width:115px">Status</td>
              <td style="padding:11px 12px;text-align:left;font-size:11px;
                  text-transform:uppercase;letter-spacing:0.5px">Test Name</td>
              <td style="padding:11px 12px;text-align:left;font-size:11px;
                  text-transform:uppercase;letter-spacing:0.5px;width:160px">Suite</td>
              <td style="padding:11px 12px;text-align:center;font-size:11px;
                  text-transform:uppercase;letter-spacing:0.5px;width:70px">Time</td>
            </tr>
            ${testRows}
          </table>`}
    </td>
  </tr>

  <!-- ══ FOOTER ═══════════════════════════════════════════════════ -->
  ${RUN_URL ? `
  <tr>
    <td style="background:white;text-align:center;padding:12px;
        color:#90A4AE;font-size:10px;border-top:1px solid #ECEFF1">
      Full interactive Allure Report: &nbsp;
      <a href="${esc(RUN_URL)}" style="color:#1565C0;text-decoration:none">
        ${esc(RUN_URL)}
      </a>
    </td>
  </tr>` : ''}

</table>
<!-- /OUTER WRAPPER -->

</td></tr>
</table>

</body>
</html>`;

// ── Write HTML ────────────────────────────────────────────────────────────────
fs.writeFileSync(tmpHtml, html, 'utf8');
console.log(`HTML generated: ${tmpHtml}  (${total} tests: ${passed}P ${failed}F ${broken}B ${skipped}S)`);

// ── Convert HTML → PDF via puppeteer-core + system Chrome ─────────────────────
const CHROME_PATHS = [
  '/usr/bin/google-chrome-stable',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium-browser',
  '/usr/bin/chromium',
];
const chromePath = CHROME_PATHS.find(p => {
  try { return fs.existsSync(p); } catch (_) { return false; }
});

if (!chromePath) {
  console.warn('Chrome/Chromium not found — PDF skipped. HTML report saved at:', tmpHtml);
  process.exit(0);
}
console.log(`Using Chrome: ${chromePath}`);

let puppeteer;
try {
  puppeteer = require('puppeteer-core');
} catch (e) {
  console.warn('puppeteer-core not available — PDF skipped. HTML report saved at:', tmpHtml);
  process.exit(0);
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: chromePath,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  });
  const page = await browser.newPage();
  await page.goto(`file://${path.resolve(tmpHtml)}`, { waitUntil: 'networkidle0' });
  await page.pdf({
    path: outputPdf,
    format: 'A4',
    printBackground: true,
    margin: { top: '0mm', right: '0mm', bottom: '0mm', left: '0mm' },
  });
  await browser.close();
  console.log(`PDF generated: ${outputPdf}`);
})().catch(err => {
  console.error('PDF generation failed:', err.message, '— HTML report still available at:', tmpHtml);
  process.exit(0); // don't fail the workflow
});
