#!/usr/bin/env node
/* Minimal Chrome DevTools Protocol driver — real timing, real console,
 * real screenshots after animations settle. No npm dependencies.
 *
 *   node tools/cdp.mjs <url> [--wait ms] [--shot out.png] [--size WxH]
 *                              [--eval "js"] [--scroll p]
 */
import { spawn } from 'node:child_process';
import { writeFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const args = process.argv.slice(2);
const url = args[0];
const opt = (n, d) => { const i = args.indexOf('--' + n); return i > -1 ? args[i + 1] : d; };
const WAIT = +(opt('wait', 2200));
const SHOT = opt('shot', null);
const SIZE = opt('size', '1440x900');
const EVAL = opt('eval', null);
const SCROLL = opt('scroll', null);
const [VW, VH] = SIZE.split('x').map(Number);

const PORT = 9200 + (process.pid % 500);
const profile = mkdtempSync(join(tmpdir(), 'cdp-'));
const chrome = spawn('chromium', [
  '--headless=new', '--no-sandbox', '--hide-scrollbars', ...(process.env.CDP_GPU ? [] : ['--disable-gpu']),
  '--remote-debugging-port=' + PORT, '--user-data-dir=' + profile,
  `--window-size=${VW},${VH}`, 'about:blank',
], { stdio: ['ignore', 'ignore', 'ignore'] });

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function main() {
  let target = null;
  for (let i = 0; i < 80 && !target; i++) {
    await sleep(120);
    try {
      const r = await fetch(`http://127.0.0.1:${PORT}/json/list`);
      const list = await r.json();
      target = list.find(t => t.type === 'page');
    } catch { /* not up yet */ }
  }
  if (!target) throw new Error('chrome did not expose a page target');

  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });

  let id = 0;
  const pending = new Map();
  const logs = [];
  ws.onmessage = (ev) => {
    const m = JSON.parse(ev.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return; }
    if (m.method === 'Runtime.consoleAPICalled') {
      logs.push('[' + m.params.type + '] ' + m.params.args.map(a => a.value ?? a.description ?? a.type).join(' '));
    }
    if (m.method === 'Runtime.exceptionThrown') {
      const d = m.params.exceptionDetails;
      logs.push('[EXCEPTION] ' + (d.exception?.description || d.text) + ' @' + (d.lineNumber ?? '?'));
    }
    if (m.method === 'Log.entryAdded' && m.params.entry.level === 'error') {
      logs.push('[log.error] ' + m.params.entry.text + ' ' + (m.params.entry.url || ''));
    }
  };
  const send = (method, params = {}) => new Promise(res => {
    const n = ++id;
    pending.set(n, res);
    ws.send(JSON.stringify({ id: n, method, params }));
  });

  if (args.includes('--reduced')) {
    await send('Emulation.setEmulatedMedia', {
      features: [{ name: 'prefers-reduced-motion', value: 'reduce' }],
    });
  }
  if (args.includes('--mobile')) {
    await send('Emulation.setDeviceMetricsOverride', {
      width: VW, height: VH, deviceScaleFactor: 2, mobile: true,
    });
    await send('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 5 });
  }
  await send('Runtime.enable');
  await send('Log.enable');
  await send('Page.enable');
  await send('Page.navigate', { url });
  await sleep(WAIT);

  if (SCROLL !== null) {
    await send('Runtime.evaluate', {
      expression: `window.scrollTo(0, ${SCROLL} * (document.documentElement.scrollHeight - innerHeight));`,
      awaitPromise: true,
    });
    await sleep(1400);
  }
  if (EVAL) {
    const r = await send('Runtime.evaluate', { expression: EVAL, awaitPromise: true, returnByValue: true });
    console.log(JSON.stringify(r.result?.result?.value ?? r.result?.result ?? null, null, 2));
  }
  const POST = +(opt('post', 0));
  if (POST) await sleep(POST);

  if (SHOT) {
    const params = { format: 'png' };
    if (args.includes('--full')) {
      const m = await send('Runtime.evaluate', {
        expression: 'JSON.stringify({w:document.documentElement.scrollWidth,h:document.documentElement.scrollHeight})',
        returnByValue: true,
      });
      const { w, h } = JSON.parse(m.result.result.value);
      params.captureBeyondViewport = true;
      params.clip = { x: 0, y: 0, width: w, height: h, scale: +(opt('scale', 1)) };
    }
    const r = await send('Page.captureScreenshot', params);
    writeFileSync(SHOT, Buffer.from(r.result.data, 'base64'));
  }
  console.log(logs.length ? 'CONSOLE:\n' + logs.join('\n') : 'CONSOLE: clean');
  ws.close();
  chrome.kill();
  process.exit(0);
}
main().catch(e => { console.error('FAILED:', e.message); chrome.kill(); process.exit(1); });
