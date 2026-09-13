/* ============================================================
 *  audio.js — 环境音（纯 WebAudio 合成，不加载任何音频文件）
 *  A slow pad + wind + sparse pentatonic bells. Off by default;
 *  browsers require a user gesture before audio may start.
 * ============================================================ */
window.AMBIENT = (function () {
  'use strict';

  let ac = null, master = null, windGain = null, padGain = null;
  let started = false, bellTimer = 0, chordTimer = 0;
  let nodes = [];

  /* A minor pentatonic-ish, warm and unresolved */
  const SCALE = [220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 587.33];
  const CHORDS = [[0, 2, 4], [1, 3, 5], [0, 3, 5], [2, 4, 6]];

  function noiseBuffer(ctx, seconds) {
    const len = Math.floor(ctx.sampleRate * seconds);
    const buf = ctx.createBuffer(1, len, ctx.sampleRate);
    const d = buf.getChannelData(0);
    let last = 0;
    for (let i = 0; i < len; i++) {
      const w = Math.random() * 2 - 1;
      last = (last + 0.02 * w) / 1.02;   /* brown-ish noise */
      d[i] = last * 3.2;
    }
    return buf;
  }

  function bell(freq, when, vel) {
    const ctx = ac;
    const osc = ctx.createOscillator();
    const osc2 = ctx.createOscillator();
    const g = ctx.createGain();
    const bp = ctx.createBiquadFilter();
    bp.type = 'bandpass'; bp.frequency.value = freq * 2; bp.Q.value = 1.2;
    osc.type = 'sine'; osc.frequency.value = freq;
    osc2.type = 'sine'; osc2.frequency.value = freq * 2.01;
    g.gain.setValueAtTime(0, when);
    g.gain.linearRampToValueAtTime(vel, when + 0.02);
    g.gain.exponentialRampToValueAtTime(0.0001, when + 2.6);
    osc.connect(g); osc2.connect(g); g.connect(bp); bp.connect(master);
    osc.start(when); osc2.start(when);
    osc.stop(when + 2.8); osc2.stop(when + 2.8);
  }

  function pad(chordIdx, when, dur) {
    const ctx = ac;
    const chord = CHORDS[chordIdx % CHORDS.length];
    chord.forEach((deg, i) => {
      const f = SCALE[deg] / 2;                       /* an octave down: soft bed */
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      const lp = ctx.createBiquadFilter();
      lp.type = 'lowpass';
      lp.frequency.setValueAtTime(420, when);
      lp.frequency.linearRampToValueAtTime(760, when + dur * 0.5);
      lp.frequency.linearRampToValueAtTime(380, when + dur);
      osc.type = i === 0 ? 'triangle' : 'sine';
      osc.frequency.value = f;
      osc.detune.value = (i - 1) * 6;
      g.gain.setValueAtTime(0, when);
      g.gain.linearRampToValueAtTime(0.055, when + dur * 0.35);
      g.gain.linearRampToValueAtTime(0, when + dur);
      osc.connect(g); g.connect(lp); lp.connect(padGain);
      osc.start(when); osc.stop(when + dur + 0.1);
      nodes.push(osc);
    });
  }

  function build() {
    ac = new (window.AudioContext || window.webkitAudioContext)();

    master = ac.createGain();
    master.gain.value = 0.0;
    master.connect(ac.destination);

    /* ---- wind ---- */
    const wind = ac.createBufferSource();
    wind.buffer = noiseBuffer(ac, 6);
    wind.loop = true;
    const wf = ac.createBiquadFilter();
    wf.type = 'bandpass'; wf.frequency.value = 480; wf.Q.value = 0.6;
    windGain = ac.createGain();
    windGain.gain.value = 0.16;
    /* slow gusts */
    const lfo = ac.createOscillator(), lfoG = ac.createGain();
    lfo.frequency.value = 0.055; lfoG.gain.value = 0.11;
    lfo.connect(lfoG); lfoG.connect(windGain.gain);
    wind.connect(wf); wf.connect(windGain); windGain.connect(master);
    wind.start(); lfo.start();

    /* ---- pad bed ---- */
    padGain = ac.createGain();
    padGain.gain.value = 0.9;
    padGain.connect(master);

    /* ---- sparse bells ---- */
    const scheduleBell = () => {
      if (!ac || ac.state !== 'running') return;
      const t = ac.currentTime + 0.05;
      const n = 1 + (Math.random() > 0.66 ? 1 : 0);
      for (let i = 0; i < n; i++) {
        const f = SCALE[(Math.random() * SCALE.length) | 0];
        bell(f, t + i * (0.36 + Math.random() * 0.5), 0.05 + Math.random() * 0.05);
      }
      bellTimer = setTimeout(scheduleBell, 2600 + Math.random() * 5200);
    };

    let ci = 0;
    const schedulePad = () => {
      if (!ac || ac.state !== 'running') return;
      pad(ci++, ac.currentTime + 0.05, 13);
      chordTimer = setTimeout(schedulePad, 11000);
    };

    pad(ci++, ac.currentTime + 0.05, 13);
    chordTimer = setTimeout(schedulePad, 11000);
    bellTimer = setTimeout(scheduleBell, 1800);
  }

  return {
    start: function () {
      if (!started) {
        try { build(); started = true; }
        catch (e) { return false; }
      }
      if (ac.state === 'suspended') ac.resume();
      master.gain.cancelScheduledValues(ac.currentTime);
      master.gain.setTargetAtTime(0.5, ac.currentTime, 1.4);
      return true;
    },
    stop: function () {
      if (!started) return;
      master.gain.cancelScheduledValues(ac.currentTime);
      master.gain.setTargetAtTime(0.0, ac.currentTime, 0.5);
      clearTimeout(bellTimer); clearTimeout(chordTimer);
    },
    isOn: function () { return started && ac && ac.state === 'running' && master.gain.value > 0.01; },
  };
})();
