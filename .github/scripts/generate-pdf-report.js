#!/usr/bin/env node
'use strict';
/**
 * generate-pdf-report.js
 * Reads Allure *-result.json files → generates a styled HTML summary →
 * converts to PDF using puppeteer-core + system Chrome (pre-installed on GitHub Actions).
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

const RUN_NUMBER = process.env.GITHUB_RUN_NUMBER || 'N/A';
const REPO       = process.env.GITHUB_REPOSITORY || 'N/A';
const BRANCH     = process.env.GITHUB_REF_NAME   || 'N/A';
const ACTOR      = process.env.GITHUB_ACTOR      || 'N/A';
const SERVER     = process.env.GITHUB_SERVER_URL || '';
const RUN_ID     = process.env.GITHUB_RUN_ID     || '';
const RUN_URL    = SERVER && REPO && RUN_ID
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
        name:     r.name     || 'Unknown',
        status:   (r.status  || 'unknown').toLowerCase(),
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
  if (!ms)        return '—';
  if (ms < 1000)  return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  return `${Math.floor(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`;
}

const BADGE_COLOR = { passed: '#2E7D32', failed: '#B71C1C', broken: '#E65100', skipped: '#546E7A' };
const ROW_BG_A    = { passed: '#F1F8E9', failed: '#FFEBEE', broken: '#FFF3E0', skipped: '#FAFAFA' };
const ROW_BG_B    = { passed: '#F9FBF9', failed: '#FFF5F5', broken: '#FFFAF4', skipped: '#F5F5F5' };
const ICON        = { passed: '✓', failed: '✗', broken: '⚠', skipped: '○' };

const now = new Date().toLocaleString('en-IN', { timeZone: 'Asia/Kolkata', hour12: false });

// ── Progress bar segments ─────────────────────────────────────────────────────
function pPct(n) { return total > 0 ? `${((n / total) * 100).toFixed(2)}%` : '0%'; }
const progressBar = [
  `<div style="flex:${passed};background:#43A047;height:100%;min-width:${passed>0?'3px':'0'}"></div>`,
  `<div style="flex:${failed};background:#E53935;height:100%;min-width:${failed>0?'3px':'0'}"></div>`,
  `<div style="flex:${broken};background:#FB8C00;height:100%;min-width:${broken>0?'3px':'0'}"></div>`,
  `<div style="flex:${skipped};background:#9E9E9E;height:100%;min-width:${skipped>0?'3px':'0'}"></div>`,
].join('');

// ── Summary card ──────────────────────────────────────────────────────────────
function card(count, label, bg, icon) {
  return `
  <div style="flex:1;min-width:90px;background:${bg};color:white;border-radius:10px;
      padding:16px 10px;text-align:center;box-shadow:0 2px 6px rgba(0,0,0,0.18)">
    <div style="font-size:11px;text-transform:uppercase;letter-spacing:1.2px;
        opacity:0.85;margin-bottom:6px">${icon}&nbsp;${label}</div>
    <div style="font-size:36px;font-weight:700;line-height:1">${count}</div>
  </div>`;
}

// ── Test rows ─────────────────────────────────────────────────────────────────
const testRows = tests.map((t, i) => {
  const bg   = i % 2 === 0 ? (ROW_BG_A[t.status] || '#fff') : (ROW_BG_B[t.status] || '#fafafa');
  const bc   = BADGE_COLOR[t.status] || '#546E7A';
  const icon = ICON[t.status] || '?';

  const msgEl = t.message
    ? `<div style="margin-top:4px;padding:3px 7px;background:#fff0f0;
        border-left:3px solid #ef9a9a;font-size:10px;font-family:'Courier New',monospace;
        color:#B71C1C;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:400px">
        ${esc(t.message.substring(0, 200))}
       </div>` : '';

  const tagEls = t.tags.map(tg =>
    `<span style="display:inline-block;background:#E3F2FD;color:#1565C0;
        border-radius:3px;padding:0 5px;font-size:10px;margin:1px 0 0 5px;
        vertical-align:middle">${esc(tg)}</span>`
  ).join('');

  return `
  <tr style="background:${bg}">
    <td style="text-align:center;padding:8px 6px;width:110px;border-bottom:1px solid #E0E0E0">
      <span style="display:inline-block;background:${bc};color:white;
          border-radius:12px;padding:3px 12px;font-size:11px;font-weight:700;white-space:nowrap">
        ${icon} ${t.status.toUpperCase()}
      </span>
    </td>
    <td style="padding:8px 12px;border-bottom:1px solid #E0E0E0">
      <span style="font-size:12px;font-weight:600;color:#212121">${esc(t.name)}</span>${tagEls}${msgEl}
    </td>
    <td style="padding:8px 12px;font-size:11px;color:#546E7A;
        width:155px;border-bottom:1px solid #E0E0E0">${esc(t.suite)}</td>
    <td style="padding:8px 12px;text-align:center;font-size:11px;color:#546E7A;
        width:68px;border-bottom:1px solid #E0E0E0;white-space:nowrap">${fmtMs(t.duration)}</td>
  </tr>`;
}).join('');

// ── Full HTML ─────────────────────────────────────────────────────────────────
const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Test Report — Run #${RUN_NUMBER}</title>
</head>
<body style="font-family:Arial,Helvetica,sans-serif;color:#333;background:#ECEFF1;margin:0;padding:0;font-size:13px">

<!-- HEADER -->
<div style="background:linear-gradient(135deg,#0D47A1 0%,#1976D2 60%,#1E88E5 100%);
    color:white;padding:22px 30px 20px;border-bottom:5px solid #0A2F6B">
  <div style="font-size:23px;font-weight:700;letter-spacing:0.5px;margin-bottom:8px">
    Test Execution Report
  </div>
  <table style="border-collapse:collapse;color:white;font-size:11px;opacity:0.88">
    <tr>
      <td style="padding-right:22px">&#128313; Run <strong>#${RUN_NUMBER}</strong></td>
      <td style="padding-right:22px">&#128313; ${esc(REPO)}</td>
      <td style="padding-right:22px">&#128313; Branch: <strong>${esc(BRANCH)}</strong></td>
      <td style="padding-right:22px">&#128313; By: <strong>${esc(ACTOR)}</strong></td>
      <td>&#128313; ${now}</td>
    </tr>
  </table>
</div>

<!-- SUMMARY CARDS -->
<div style="display:flex;gap:14px;padding:20px 30px;background:white;
    border-bottom:1px solid #CFD8DC;flex-wrap:wrap">
  ${card(total,   'Total',   '#1565C0', '&#9672;')}
  ${card(passed,  'Passed',  '#2E7D32', '&#10003;')}
  ${card(failed,  'Failed',  '#B71C1C', '&#10007;')}
  ${card(broken,  'Broken',  '#E65100', '&#9888;')}
  ${card(skipped, 'Skipped', '#546E7A', '&#9711;')}
</div>

<!-- PASS RATE + PROGRESS BAR -->
<div style="padding:14px 30px 16px;background:white;border-bottom:1px solid #CFD8DC">
  <table style="width:100%;border-collapse:collapse;margin-bottom:8px">
    <tr>
      <td style="font-size:13px;color:#333">
        Pass Rate: <strong style="font-size:18px;color:#2E7D32">${passRate}%</strong>
        &nbsp;&nbsp;
        <span style="font-size:11px;color:#555">
          <span style="color:#2E7D32;font-weight:600">&#9632; ${passed} Passed</span> &nbsp;
          <span style="color:#B71C1C;font-weight:600">&#9632; ${failed} Failed</span> &nbsp;
          <span style="color:#E65100;font-weight:600">&#9632; ${broken} Broken</span> &nbsp;
          <span style="color:#546E7A;font-weight:600">&#9632; ${skipped} Skipped</span>
        </span>
      </td>
    </tr>
  </table>
  <div style="height:14px;background:#CFD8DC;border-radius:7px;overflow:hidden">
    <div style="height:100%;display:flex;border-radius:7px;overflow:hidden">
      ${total > 0 ? progressBar : '<div style="flex:1;background:#CFD8DC"></div>'}
    </div>
  </div>
</div>

<!-- TEST TABLE -->
<div style="padding:20px 30px 28px">
  <div style="font-size:15px;font-weight:700;color:#0D47A1;margin-bottom:12px;
      display:flex;align-items:center;gap:10px">
    Test Cases
    <span style="background:#0D47A1;color:white;border-radius:12px;
        padding:2px 12px;font-size:12px;font-weight:600">${total}</span>
  </div>

  ${total === 0
    ? `<div style="text-align:center;padding:50px;background:white;border-radius:10px;
          color:#90A4AE;font-size:15px;box-shadow:0 1px 4px rgba(0,0,0,0.08)">
         No test results found in: ${esc(resultsDir)}
       </div>`
    : `<table style="width:100%;border-collapse:collapse;background:white;
          border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1)">
        <thead>
          <tr style="background:#263238;color:white">
            <th style="padding:11px 6px;text-align:center;font-size:11px;
                text-transform:uppercase;letter-spacing:0.5px;width:110px">Status</th>
            <th style="padding:11px 12px;text-align:left;font-size:11px;
                text-transform:uppercase;letter-spacing:0.5px">Test Name</th>
            <th style="padding:11px 12px;text-align:left;font-size:11px;
                text-transform:uppercase;letter-spacing:0.5px;width:155px">Suite</th>
            <th style="padding:11px 12px;text-align:center;font-size:11px;
                text-transform:uppercase;letter-spacing:0.5px;width:68px">Time</th>
          </tr>
        </thead>
        <tbody>
          ${testRows}
        </tbody>
      </table>`
  }
</div>

<!-- FOOTER -->
${RUN_URL ? `
<div style="text-align:center;padding:12px;color:#90A4AE;font-size:10px;
    background:white;border-top:1px solid #ECEFF1">
  Full interactive Allure Report: &nbsp;
  <a href="${esc(RUN_URL)}" style="color:#1565C0;text-decoration:none">${esc(RUN_URL)}</a>
</div>` : ''}

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
const chromePath = CHROME_PATHS.find(p => { try { return fs.existsSync(p); } catch (_) { return false; } });

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
