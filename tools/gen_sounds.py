#!/usr/bin/env python3
"""Synthesises the custom mono OGG sound effects for Celestial Arts and writes sounds.json.

    python3 tools/gen_sounds.py            # render everything
    python3 tools/gen_sounds.py --analyse  # also print loudness / spectral centroid curves

Requires numpy, scipy and soundfile (libsndfile with Vorbis support).

Design notes (v2 - the "no more bells" pass)
--------------------------------------------
Every sound is built from *physical* ingredients instead of decaying sine partials:

* air  - filtered noise with time-varying resonant band-passes (whooshes, wind, fire, suction);
* impact - a sub-bass drop, a broadband transient and a body resonance, with saturation;
* metal - very short inharmonic modal bursts (sword "shing"), FM scrapes, Karplus-Strong strings;
* electricity - saw/square buzz with random dropouts, sparse high-frequency crackle impulses;
* earth - brown-noise rumble, low resonant rock cracks, gravel;
* ritual - a temple gong (slow-swelling inharmonic modal mass, NOT a bell ping) and detuned
  drone pads, used only for arrays / breakthroughs where a ceremonial tone is wanted.

Sounds that are heard often (sword slashes, fire, wind, thunder, launches, hits) are rendered in
2-3 variations; Minecraft picks one at random per play so repetition does not grate.
"""
import json
import os
import sys

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "celestialarts")
SOUND_ROOT = os.path.join(ASSETS, "sounds")
OUT = os.path.join(SOUND_ROOT, "synth")  # synthesised renders; real recordings live in sounds/real
TOOLS = os.path.dirname(os.path.abspath(__file__))
SYNTH_MANIFEST = os.path.join(TOOLS, "sounds_synth.json")
REAL_MANIFEST = os.path.join(TOOLS, "sounds_real.json")

rng = np.random.default_rng(20260926)


# ------------------------------------------------------------------ primitives

def n_samples(dur):
    return int(round(SR * dur))


def t(dur):
    return np.arange(n_samples(dur)) / SR


def zeros(dur):
    return np.zeros(n_samples(dur))


def noise(dur):
    return rng.standard_normal(n_samples(dur))


def pink(dur):
    """Pink noise via Paul Kellet's filter."""
    w = noise(dur)
    b = [0.049922035, -0.095993537, 0.050612699, -0.004408786]
    a = [1, -2.494956002, 2.017265875, -0.522189400]
    return signal.lfilter(b, a, w)


def brown(dur, hp=8.0):
    x = np.cumsum(noise(dur))
    x -= np.linspace(x[0], x[-1], len(x))
    x = highpass(x, hp)
    return x / (np.max(np.abs(x)) + 1e-9)


def sine(freq, dur, phase=0.0):
    return np.sin(2 * np.pi * freq * t(dur) + phase)


def osc_from_freq(f, wave="sine"):
    """Oscillator driven by an instantaneous-frequency array."""
    phase = np.cumsum(f) / SR
    if wave == "sine":
        return np.sin(2 * np.pi * phase)
    if wave == "saw":
        return 2 * (phase % 1.0) - 1
    if wave == "square":
        return np.sign(np.sin(2 * np.pi * phase))
    if wave == "tri":
        return 2 * np.abs(2 * (phase % 1.0) - 1) - 1
    raise ValueError(wave)


def glide(f0, f1, dur, shape=1.0, wave="sine"):
    u = (t(dur) / dur) ** shape
    return osc_from_freq(f0 * (f1 / f0) ** u, wave)


def supersaw(freq, dur, voices=7, detune=0.012, wave="saw"):
    x = zeros(dur)
    for i in range(voices):
        d = (i - (voices - 1) / 2) / max(1, (voices - 1) / 2)
        f = freq * (1 + detune * d)
        x += osc_from_freq(np.full(n_samples(dur), f), wave) * (1.0 - 0.3 * abs(d))
    return x / voices


def env(dur, attack, decay, sustain=0.0, release=0.0, curve=2.0):
    """ADSR over the whole duration (times in seconds)."""
    n = n_samples(dur)
    a = min(n, n_samples(attack))
    d = n_samples(decay)
    r = min(n, n_samples(release))
    e = np.full(n, float(sustain))
    if a > 0:
        e[:a] = np.linspace(0, 1, a) ** (1 / curve)
    end_d = min(n, a + d)
    if end_d > a:
        e[a:end_d] = 1 - (1 - sustain) * (np.linspace(0, 1, end_d - a) ** curve)
    if a == 0 and d == 0:
        e[:] = sustain
    if r > 0:
        e[n - r:] *= np.linspace(1, 0, r) ** curve
    return e


def expdecay(dur, tau, delay=0.0):
    tt = t(dur) - delay
    e = np.exp(-np.maximum(tt, 0) / tau)
    e[tt < 0] = 0
    return e


def ramp(dur, v0, v1, shape=1.0):
    return v0 + (v1 - v0) * (t(dur) / dur) ** shape


def fade(x, fin=0.004, fout=0.03):
    x = x.copy()
    i = min(len(x), n_samples(fin))
    o = min(len(x), n_samples(fout))
    if i > 0:
        x[:i] *= np.linspace(0, 1, i)
    if o > 0:
        x[-o:] *= np.linspace(1, 0, o) ** 1.5
    return x


# ------------------------------------------------------------------ filters

def _sos(kind, f, order=4, f2=None):
    nyq = SR / 2
    if kind == "band":
        lo = max(10.0, min(f, nyq - 20)) / nyq
        hi = max(lo * 1.01, min(f2, nyq - 10) / nyq)
        return signal.butter(order, [lo, hi], btype="band", output="sos")
    return signal.butter(order, max(10.0, min(f, nyq - 20)) / nyq, btype=kind, output="sos")


def lowpass(x, f, order=4):
    return signal.sosfilt(_sos("low", f, order), x)


def highpass(x, f, order=4):
    return signal.sosfilt(_sos("high", f, order), x)


def bandpass(x, lo, hi, order=4):
    return signal.sosfilt(_sos("band", lo, order, hi), x)


def resonant(x, f, q):
    """Two-pole resonator (peaking band-pass) at f with quality q."""
    f = float(np.clip(f, 20, SR / 2 - 100))
    b, a = signal.iirpeak(f / (SR / 2), q)
    return signal.lfilter(b, a, x)


def sweep(x, f0, f1, q=2.0, shape=1.0, blocks=64, mode="res"):
    """Time-varying resonant band-pass: centre glides f0 -> f1 (log), overlapping windows."""
    n = len(x)
    out = np.zeros(n)
    win = max(256, (n // blocks) * 2)
    hop = win // 2
    w = np.hanning(win)
    i = 0
    k = 0
    total = max(1, (n - win) // hop + 1)
    while i < n:
        seg = x[i:i + win]
        L = len(seg)
        if L < 32:
            break
        u = min(1.0, k / total) ** shape
        f = f0 * (f1 / f0) ** u
        if mode == "res":
            y = resonant(seg * w[:L], f, q)
        elif mode == "low":
            y = lowpass(seg * w[:L], f)
        else:  # "high"
            y = highpass(seg * w[:L], f)
        out[i:i + L] += y
        i += hop
        k += 1
    return out


def sweep_curve(x, freqs, q=2.0, blocks=64):
    """Resonant sweep following an arbitrary centre-frequency curve (array of len(x))."""
    n = len(x)
    out = np.zeros(n)
    win = max(256, (n // blocks) * 2)
    hop = win // 2
    w = np.hanning(win)
    i = 0
    while i < n:
        seg = x[i:i + win]
        L = len(seg)
        if L < 32:
            break
        f = float(freqs[min(n - 1, i + L // 2)])
        out[i:i + L] += resonant(seg * w[:L], f, q)
        i += hop
    return out


# ------------------------------------------------------------------ effects

def reverb(x, size=0.6, decay=0.5, wet=0.35, predelay=0.0):
    """Schroeder reverb: 4 parallel combs + 2 series allpasses. `size` scales the comb lengths
    (0.3 = small room, 1.4 = a valley), `decay` sets RT60 = 2.2 * decay seconds. Tail appended."""
    rt60 = 2.2 * decay
    tail = n_samples(rt60 + 0.3)
    y = np.concatenate([fade(x, 0.0, 0.03), np.zeros(tail)])  # no click at the end of the dry part
    combs = [int(SR * (0.4 + size) * s) for s in (0.0297, 0.0371, 0.0411, 0.0437)]
    acc = np.zeros_like(y)
    for d in combs:
        g = 10 ** (-3.0 * d / (SR * rt60))
        g = float(np.clip(g, 0.2, 0.97))
        b = np.zeros(d + 1)
        b[0] = 1.0
        a = np.zeros(d + 1)
        a[0] = 1.0
        a[-1] = -g
        acc += signal.lfilter(b, a, y)
    acc /= len(combs)
    for d in (int(SR * 0.005), int(SR * 0.0017)):
        g = 0.7
        b = np.zeros(d + 1)
        a = np.zeros(d + 1)
        b[0] = -g
        b[-1] = 1.0
        a[0] = 1.0
        a[-1] = -g
        acc = signal.lfilter(b, a, acc)
    acc = lowpass(acc, 6000)
    if predelay > 0:
        acc = np.concatenate([np.zeros(n_samples(predelay)), acc])[: len(y)]
    return y * (1 - wet) + acc * wet


def delay(x, time_s, feedback=0.4, mixv=0.3, damp=4000):
    d = n_samples(time_s)
    y = np.concatenate([x, np.zeros(d * 6)])
    out = y.copy()
    buf = y.copy()
    for i in range(6):
        buf = np.concatenate([np.zeros(d), buf[:-d]])
        buf = lowpass(buf, damp) * feedback
        out += buf * mixv / feedback if feedback > 0 else 0
    return out


def saturate(x, drive=2.0):
    return np.tanh(x * drive) / np.tanh(drive)


def glue(x, thresh=0.6, ratio=3.0):
    """Gentle bus compression on a normalised signal (avoids pumping the reverb tail up)."""
    return compress(norm(x, 0.95), thresh, ratio)


def compress(x, thresh=0.5, ratio=4.0, attack=0.003, release=0.3):
    """Feed-forward peak compressor (mono)."""
    envl = np.zeros_like(x)
    ga = np.exp(-1 / (SR * attack))
    gr = np.exp(-1 / (SR * release))
    lvl = 0.0
    ax = np.abs(x)
    for i in range(len(x)):
        v = ax[i]
        lvl = ga * lvl + (1 - ga) * v if v > lvl else gr * lvl + (1 - gr) * v
        envl[i] = lvl
    gain = np.ones_like(x)
    over = envl > thresh
    gain[over] = (thresh + (envl[over] - thresh) / ratio) / envl[over]
    return x * gain


def resample(x, ratio):
    """Playback-rate change (pitch and speed together)."""
    n = int(len(x) / ratio)
    return np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x)


def stretch_pitch(x, curve):
    """Variable playback rate; curve = ratio per output sample (array)."""
    pos = np.cumsum(curve)
    pos = pos[pos < len(x) - 1]
    return np.interp(pos, np.arange(len(x)), x)


def at(x, offset, total):
    """Place x at offset seconds inside a buffer of total seconds."""
    out = zeros(total)
    s = n_samples(offset)
    L = min(len(x), len(out) - s)
    if L > 0:
        out[s:s + L] += fade(x[:L], 0.0, 0.012)  # never let a placed layer stop abruptly (clicks)
    return out


def mix(*parts):
    n = max(len(p) for p, _ in parts)
    out = np.zeros(n)
    for p, g in parts:
        out[: len(p)] += p * g
    return out


def norm(x, peak=0.9):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def trim(x, dur):
    return x[: n_samples(dur)]


def loopable(x, xfade=0.08):
    """Cross-fade the tail into the head so the buffer loops seamlessly."""
    k = n_samples(xfade)
    r = np.linspace(0, 1, k)
    y = x.copy()
    y[:k] = x[:k] * r + x[-k:] * (1 - r)
    return y[:-k]


# ------------------------------------------------------------------ instruments

def modal(freq, dur, modes, decay=1.0, brightness=1.0):
    """Sum of exponentially decaying resonances. modes = [(ratio, amp, tau_scale), ...]."""
    tt = t(dur)
    x = np.zeros_like(tt)
    for ratio, amp, taus in modes:
        f = freq * ratio
        if f > SR / 2 - 200:
            continue
        x += amp * np.sin(2 * np.pi * f * tt + rng.uniform(0, 6.28)) * np.exp(-tt / (decay * taus)) * (brightness ** (ratio - 1))
    return x


METAL_MODES = [(1.0, 1.0, 1.0), (1.72, 0.7, 0.7), (2.61, 0.55, 0.5), (3.42, 0.4, 0.4), (4.87, 0.3, 0.3), (6.31, 0.22, 0.22), (8.05, 0.15, 0.18)]
GLASS_MODES = [(1.0, 1.0, 1.0), (2.32, 0.6, 0.6), (3.91, 0.45, 0.4), (5.28, 0.3, 0.3), (7.1, 0.2, 0.22)]
GONG_MODES = [(1.0, 1.0, 1.0), (1.19, 0.6, 1.1), (1.53, 0.7, 0.9), (2.0, 0.5, 0.8), (2.41, 0.6, 0.7), (2.96, 0.45, 0.6), (3.55, 0.35, 0.5),
              (4.31, 0.3, 0.45), (5.12, 0.25, 0.4), (6.02, 0.2, 0.35), (7.4, 0.15, 0.3), (9.1, 0.1, 0.25)]


def metal_shing(freq, dur=0.5, decay=0.12):
    """A blade "shing": a metallic modal burst excited by a noise scrape, high and short."""
    body = modal(freq, dur, METAL_MODES, decay=decay, brightness=0.9)
    scrape = bandpass(noise(dur), freq * 0.8, min(18000, freq * 6)) * expdecay(dur, 0.05)
    return mix((body, 1.0), (scrape, 0.5))


def gong(freq, dur=4.0, swell=0.35):
    """Temple gong: an inharmonic modal mass that swells after the strike, with a noisy wash."""
    body = modal(freq, dur, GONG_MODES, decay=1.6, brightness=0.85)
    tt = t(dur)
    sw = 1.0 + 1.4 * np.exp(-((tt - swell) ** 2) / (2 * 0.18 ** 2))  # crescendo after the hit
    wash = bandpass(noise(dur), freq * 2, freq * 14) * np.exp(-tt / 1.2)
    hit = bandpass(noise(dur), 200, 3000) * expdecay(dur, 0.02)
    return mix((body * sw, 1.0), (wash, 0.12), (hit, 0.5))


def karplus(freq, dur, bright=0.5, pluck_noise=1.0):
    """Karplus-Strong plucked string (guzheng-ish when bright)."""
    n = n_samples(dur)
    p = int(SR / freq)
    buf = rng.uniform(-1, 1, p) * pluck_noise
    buf = lowpass(buf, 2000 + 8000 * bright)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % p]
        buf[i % p] = 0.5 * (buf[i % p] + buf[(i + 1) % p]) * 0.996
    return out


def impact(dur=1.2, sub_f0=90, sub_f1=32, body_f=180, crack=1.0, weight=1.0):
    """Generic heavy impact: sub drop + broadband crack + body resonance."""
    sub = glide(sub_f0, sub_f1, dur, 0.4) * expdecay(dur, 0.35 * weight)
    crk = bandpass(noise(dur), 300, 9000) * expdecay(dur, 0.012) * crack
    body = resonant(noise(dur), body_f, 6) * expdecay(dur, 0.09 * weight)
    thump = lowpass(noise(dur), 250) * expdecay(dur, 0.06)
    return mix((sub, 0.7), (crk, 1.0), (body, 1.4), (thump, 1.0))


def whoosh(dur, f_start, f_peak, f_end, q=3.0, peak_at=0.4, gain_curve=1.0):
    """Air displacement: resonant band-pass on noise following a rise-then-fall curve."""
    n = n_samples(dur)
    u = np.linspace(0, 1, n)
    k = int(peak_at * n)
    f = np.empty(n)
    f[:k] = f_start * (f_peak / f_start) ** (u[:k] / max(1e-6, peak_at))
    f[k:] = f_peak * (f_end / f_peak) ** ((u[k:] - peak_at) / max(1e-6, 1 - peak_at))
    x = sweep_curve(noise(dur), f, q)
    a = np.sin(np.pi * u) ** gain_curve
    return x * a


def crackle(dur, density=40, lo=1500, hi=7000, tail=0.004):
    """Sparse bursts (fire crackle, electric snaps, rock cracks) band-limited to lo..hi."""
    x = zeros(dur)
    n = int(density * dur)
    L = max(8, n_samples(tail))
    for _ in range(n):
        p = int(rng.integers(0, max(1, len(x) - L)))
        x[p:p + L] += rng.standard_normal(L) * np.exp(-np.arange(L) / (L / 3)) * rng.uniform(0.3, 1.0)
    return bandpass(x, lo, hi)


def electric_buzz(dur, f0, f1, dropout=0.3, shape=1.0):
    """Arcing electricity: saw+square buzz with random dropouts and jitter."""
    n = n_samples(dur)
    u = (t(dur) / dur) ** shape
    jitter = 1 + 0.03 * lowpass(noise(dur), 30)
    f = f0 * (f1 / f0) ** u * jitter
    x = 0.6 * osc_from_freq(f, "saw") + 0.4 * osc_from_freq(f * 1.003, "square")
    gate = lowpass(noise(dur), 60)
    gate = (gate > np.quantile(gate, dropout)).astype(float)
    gate = lowpass(gate, 400)
    return bandpass(x * gate, 90, 9000)


def roar(dur, f0, f1, formants=((600, 8), (1100, 6), (2400, 5)), growl=35.0, drive=3.0):
    """A creature roar: saw source with sub-audio growl modulation through formant resonators."""
    tt = t(dur)
    f = f0 * (f1 / f0) ** (tt / dur)
    f = f * (1 + 0.02 * np.sin(2 * np.pi * growl * tt) + 0.01 * lowpass(noise(dur), 20))
    src = osc_from_freq(f, "saw") + 0.3 * noise(dur)
    y = np.zeros_like(src)
    for fc, q in formants:
        y += resonant(src, fc, q)
    y = saturate(y / len(formants) * 3, drive)
    return y


def breath(dur, f_lo, f_hi, q=1.2):
    """Soft airy band-limited noise (qi / wind bed)."""
    return sweep(noise(dur), f_lo, f_hi, q=q, mode="res")


def sub_boom(dur, f0=70, f1=28, tau=0.6):
    return saturate(glide(f0, f1, dur, 0.5) * expdecay(dur, tau), 1.5)


def tonal_swell(freq, dur, voices=6, detune=0.01, lp0=300, lp1=3000):
    """Detuned saw pad that opens up over its duration (ritual drone, not a bell)."""
    x = supersaw(freq, dur, voices, detune)
    return sweep(x, lp0, lp1, q=0.9, mode="low")


# ------------------------------------------------------------------------ sounds
# Each function returns a list of variations (mono float arrays).

def s_sword_qi():
    """Sword-qi slash: air cut + steel shing + short energy sizzle. Three cuts of differing length."""
    out = []
    for i, (dur, f_peak, shing_f) in enumerate([(0.55, 5200, 3300), (0.65, 4200, 2700), (0.5, 6500, 4100)]):
        cut = whoosh(dur, 500, f_peak, 900, q=6, peak_at=0.22, gain_curve=0.6) * expdecay(dur, 0.25)
        shing = metal_shing(shing_f, dur, decay=0.10) * 0.7
        shing = at(shing, 0.03, dur)
        sizzle = bandpass(noise(dur), 6000, 14000) * expdecay(dur, 0.18, delay=0.04)
        thump = lowpass(noise(dur), 220) * expdecay(dur, 0.04)
        x = mix((cut, 1.0), (shing, 0.55), (sizzle, 0.25), (thump, 1.6))
        out.append(norm(fade(reverb(x, 0.3, 0.4, 0.18))))
    return out


def s_sword_hum():
    """Spirit swords resonating: low bowed-metal drone with vibrato, tremolo shimmer, no ping."""
    d = 1.8
    tt = t(d)
    vib = 1 + 0.006 * np.sin(2 * np.pi * 5.2 * tt)
    f = 146.8 * vib
    src = 0.5 * osc_from_freq(f, "saw") + 0.5 * osc_from_freq(f * 2.0, "tri")
    body = resonant(src, 293.6, 4) + 0.8 * resonant(src, 587, 8) + 0.9 * resonant(src, 1180, 12) + 0.6 * resonant(src, 2350, 14)
    air = bandpass(noise(d), 2500, 9000) * (0.5 + 0.5 * np.sin(2 * np.pi * 7.0 * tt))
    x = mix((body, 1.0), (air, 0.08)) * env(d, 0.35, 0.3, 0.85, 0.6)
    return [norm(fade(reverb(x, 0.5, 0.5, 0.25), 0.05, 0.2), 0.7)]


def s_sword_launch():
    """A sword shooting off: sharp air rip rising in pitch, steel whistle, tail of wind."""
    out = []
    for i, (dur, f0, f1) in enumerate([(0.7, 700, 5000), (0.8, 500, 4200), (0.6, 900, 6000)]):
        rip = sweep(noise(dur), f0, f1, q=8, shape=0.6) * env(dur, 0.01, 0.45)
        whistle = glide(f1 * 0.45, f1 * 0.9, dur, 0.5) * expdecay(dur, 0.12, delay=0.02) * 0.4
        tail = sweep(noise(dur), f1, f0 * 0.7, q=2, shape=1.2) * env(dur, 0.2, 0.5)
        crack = bandpass(noise(dur), 1500, 9000) * expdecay(dur, 0.008)
        x = mix((rip, 1.0), (whistle, 0.35), (tail, 0.5), (crack, 0.6))
        out.append(norm(fade(reverb(x, 0.35, 0.4, 0.15))))
    return out


def s_fire_whoosh():
    """Flame claw / fire cast: a gout of flame - turbulent low roar, air rush, crackle."""
    out = []
    for i, dur in enumerate([0.85, 0.95, 0.75]):
        turb = 1 + 0.6 * lowpass(noise(dur), 14 + 4 * i)
        roar_ = lowpass(pink(dur), 700) * turb * env(dur, 0.05, 0.6, 0.0, 0.0)
        rush = whoosh(dur, 250, 1800 + 300 * i, 500, q=2.5, peak_at=0.3)
        crk = crackle(dur, 45, 2000, 7000, 0.003) * env(dur, 0.05, 0.7)
        hiss = bandpass(noise(dur), 3000, 9000) * turb * env(dur, 0.08, 0.6) * 0.3
        x = mix((saturate(roar_, 2.5), 1.0), (rush, 0.9), (crk, 0.45), (hiss, 0.4))
        out.append(norm(fade(x)))
    return out


def s_fire_explosion():
    """Fire lotus detonation: sub drop, fireball burst, rolling debris crackle, long tail."""
    d = 2.4
    x = impact(d, 110, 30, 150, crack=1.0, weight=1.4)
    fireball = sweep(noise(d), 6000, 150, q=1.2, shape=0.5, mode="low") * expdecay(d, 0.35)
    roll = lowpass(pink(d), 500) * (1 + 0.7 * lowpass(noise(d), 9)) * expdecay(d, 0.9, delay=0.05)
    crk = crackle(d, 30, 1200, 6000, 0.005) * expdecay(d, 1.0, delay=0.08)
    x = mix((x, 0.8), (saturate(fireball, 2.0), 1.3), (roll, 0.9), (crk, 0.5))
    x = glue(reverb(x, 0.8, 0.55, 0.3))
    return [norm(fade(x, 0.002, 0.4))]


def s_ice_cast():
    """Ice arrows forming: crystals growing - rising glassy shimmer, fine crack ticks, frosty air."""
    d = 1.0
    grow = sweep(noise(d), 1200, 7000, q=12, shape=0.8) * env(d, 0.05, 0.5, 0.3, 0.3)
    ticks = crackle(d, 90, 2500, 11000, 0.0015) * env(d, 0.02, 0.6)
    frost = highpass(noise(d), 7000) * env(d, 0.2, 0.5) * 0.25
    tink = at(modal(4300, 0.35, GLASS_MODES, decay=0.06) * 0.6, 0.12, d)
    tink2 = at(modal(5700, 0.3, GLASS_MODES, decay=0.05) * 0.5, 0.26, d)
    air = whoosh(d, 400, 1800, 800, q=2, peak_at=0.35) * 0.5
    x = mix((grow, 1.0), (ticks, 0.7), (frost, 0.5), (tink, 0.5), (tink2, 0.45), (air, 0.6))
    return [norm(fade(reverb(x, 0.4, 0.45, 0.2)))]


def s_ice_shatter():
    """Ice shard bursting: brittle glass crack + a spray of tiny fragments."""
    out = []
    for i in range(2):
        d = 0.7
        crack = bandpass(noise(d), 2000, 14000) * expdecay(d, 0.02)
        body = resonant(noise(d), 900 + 200 * i, 5) * expdecay(d, 0.05)
        frags = crackle(d, 160, 3500, 13000, 0.002) * expdecay(d, 0.22, delay=0.01)
        x = mix((crack, 1.0), (body, 1.0), (frags, 0.8))
        for k in range(5):
            f = rng.uniform(3000, 7500)
            x += at(modal(f, 0.2, GLASS_MODES, decay=0.03) * rng.uniform(0.15, 0.35), rng.uniform(0.0, 0.12), d)
        out.append(norm(fade(reverb(x, 0.3, 0.4, 0.18))))
    return out


def s_thunder_strike():
    """Lightning strike: instantaneous crack, tearing mid-band, long rolling rumble."""
    out = []
    for i, d in enumerate([2.6, 3.0]):
        crack = bandpass(noise(d), 800, 16000) * expdecay(d, 0.006)
        tear = saturate(bandpass(noise(d), 150, 3000) * expdecay(d, 0.12), 3.0)
        rum = brown(d) * (1 + 0.8 * lowpass(noise(d), 6)) * expdecay(d, 0.9 + 0.3 * i, delay=0.03)
        sub = sub_boom(d, 80, 25, 0.5)
        echo = delay(tear * 0.4, 0.21 + 0.05 * i, 0.45, 0.5, 2500)
        x = mix((crack, 1.5), (tear, 1.3), (rum, 2.5), (sub, 0.6), (echo, 0.7))
        x = glue(reverb(x, 1.0, 0.6, 0.25))
        out.append(norm(fade(x, 0.001, 0.5)))
    return out


def s_thunder_charge():
    """Charging thunder: arcing buzz climbing in pitch and density, snaps, pressure rising."""
    d = 1.5
    buzz = electric_buzz(d, 45, 400, dropout=0.35, shape=1.3) * env(d, 0.15, 0.2, 1.0, 0.05)
    snaps = crackle(d, 70, 2500, 11000, 0.003) * (t(d) / d) ** 2
    pressure = sweep(noise(d), 200, 2500, q=3, shape=1.5) * (t(d) / d) ** 1.5
    x = mix((buzz, 0.6), (snaps, 0.9), (pressure, 0.8))
    return [norm(fade(x, 0.02, 0.02))]


def s_lightning_step():
    """Flash step: an electric snap, a very fast air displacement and a crackling trail."""
    d = 0.5
    snap = bandpass(noise(d), 2500, 15000) * expdecay(d, 0.004)
    zap = electric_buzz(d, 900, 120, dropout=0.4, shape=0.7) * expdecay(d, 0.09)
    disp = whoosh(d, 3000, 500, 200, q=4, peak_at=0.15) * 1.0
    trail = crackle(d, 60, 3000, 10000, 0.002) * expdecay(d, 0.2, delay=0.03)
    x = mix((snap, 1.0), (zap, 0.6), (disp, 0.9), (trail, 0.5))
    return [norm(fade(reverb(x, 0.3, 0.35, 0.15)))]


def s_wind_slash():
    """Wind blade: a keen, whistling air cut with a resonant edge."""
    out = []
    for i, (dur, fpk) in enumerate([(0.5, 4500), (0.55, 3600), (0.45, 5600)]):
        cut = whoosh(dur, 900, fpk, 700, q=9, peak_at=0.3, gain_curve=0.7)
        edge = whoosh(dur, fpk * 0.5, fpk * 1.4, fpk * 0.4, q=20, peak_at=0.32) * 0.5
        bed = bandpass(noise(dur), 300, 1500) * env(dur, 0.05, 0.4)
        x = mix((cut, 1.0), (edge, 0.5), (bed, 0.35))
        out.append(norm(fade(x)))
    return out


def s_earth_quake():
    """Earth shatter: ground splitting - deep rumble, rock cracks, gravel spray, thud."""
    out = []
    for i, d in enumerate([2.2, 1.6]):
        rum = brown(d) * (1 + 0.6 * lowpass(noise(d), 5)) * env(d, 0.03, d - 0.2, 0.0, 0.0)
        thud = impact(d, 70, 26, 120, crack=0.4, weight=1.2)
        cracks = zeros(d)
        for k in range(10 + 4 * i):
            f = rng.uniform(150, 600)
            c = resonant(noise(0.25), f, 12) * expdecay(0.25, 0.04)
            cracks += at(c * rng.uniform(0.4, 1.0), rng.uniform(0.0, 0.8), d)
        gravel = crackle(d, 120, 800, 4000, 0.004) * expdecay(d, 0.6, delay=0.05)
        x = mix((rum, 3.0), (thud, 0.9), (saturate(cracks, 2.0), 1.2), (gravel, 0.7))
        out.append(norm(fade(glue(reverb(x, 0.7, 0.5, 0.2)), 0.005, 0.4)))
    return out


def s_void_drain():
    """Devouring vortex: a hungry suction - reversed swell, detuned abyssal drone, rising whirl."""
    d = 1.6
    tt = t(d)
    drone = supersaw(41.2, d, 5, 0.02) + 0.5 * supersaw(82.4, d, 5, 0.015)
    drone = lowpass(drone, 300) * env(d, 0.3, 0.4, 0.9, 0.4)
    swell = sweep(noise(d), 300, 3000, q=5, shape=0.8) * (tt / d) ** 2 * env(d, 0.0, 0.0, 1.0, 0.25)
    whirl = sweep_curve(noise(d), 800 + 600 * np.sin(2 * np.pi * (3 + 6 * tt / d) * tt), q=6) * env(d, 0.4, 0.4, 0.8, 0.3)
    pulse = lowpass(noise(d), 120) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 2.5 * tt))) * expdecay(d, 1.0)
    x = mix((saturate(drone, 2), 0.55), (swell, 0.9), (whirl, 0.7), (pulse, 0.4))
    return [norm(fade(reverb(x, 0.9, 0.6, 0.3), 0.05, 0.15), 0.8)]


def s_void_collapse():
    """Vortex implosion: everything gets sucked in (reverse) then a hollow sub thump."""
    d = 1.4
    inhale = sweep(noise(0.6), 200, 4200, q=6, shape=0.5) * ramp(0.6, 0, 1, 2.0)
    thump = impact(0.9, 60, 24, 90, crack=0.3, weight=1.5)
    hollow = resonant(noise(0.9), 55, 20) * expdecay(0.9, 0.3)
    x = mix((at(inhale, 0.0, d), 0.9), (at(thump, 0.6, d), 1.0), (at(hollow, 0.6, d), 2.5))
    return [norm(fade(reverb(x, 0.8, 0.55, 0.3), 0.01, 0.3))]


def s_shield_up():
    """Shield forming: a rising energy 'vwoom' that locks in with a solid thump and a shimmer."""
    d = 1.1
    vwoom = tonal_swell(65, 0.9, 6, 0.012, 150, 2500) * env(0.9, 0.05, 0.5, 0.4, 0.3)
    vwoom = at(saturate(vwoom, 1.5), 0.0, d)
    air = whoosh(0.7, 300, 2500, 1200, q=2.5, peak_at=0.6)
    lock = at(impact(0.6, 120, 60, 260, crack=0.6, weight=0.6), 0.42, d)
    shimmer = at(highpass(noise(0.6), 5000) * expdecay(0.6, 0.15), 0.45, d) * 0.5
    x = mix((vwoom, 0.8), (at(air, 0, d), 1.0), (lock, 0.7), (shimmer, 0.6))
    return [norm(fade(reverb(x, 0.5, 0.45, 0.2), 0.01, 0.2))]


def s_shield_hit():
    """A blow absorbed by the shield: dull energy thud + short fizz, no ringing."""
    out = []
    for i in range(2):
        d = 0.55
        thud = impact(d, 150 + 40 * i, 70, 320 + 60 * i, crack=0.7, weight=0.5)
        fizz = bandpass(noise(d), 2000 + 500 * i, 9000) * expdecay(d, 0.07)
        tone = resonant(noise(d), 540 + 90 * i, 25) * expdecay(d, 0.09)
        x = mix((thud, 0.7), (fizz, 0.45), (tone, 4.5))
        out.append(norm(fade(reverb(x, 0.3, 0.35, 0.12))))
    return out


def s_formation():
    """An array awakening: a deep temple gong, a drone that opens, runes fizzing alight."""
    d = 2.6
    g = gong(130.8, d, swell=0.4)
    drone = tonal_swell(65.4, d, 7, 0.01, 150, 1800) * env(d, 0.6, 0.6, 0.6, 0.8)
    runes = crackle(d, 40, 3000, 10000, 0.004) * env(d, 0.3, 1.0, 0.2, 0.6)
    air = breath(d, 400, 2500) * env(d, 0.5, 0.8, 0.5, 0.7)
    x = mix((g, 1.0), (saturate(drone, 1.5), 0.35), (runes, 0.4), (air, 0.45))
    return [norm(fade(reverb(x, 1.2, 0.65, 0.35), 0.01, 0.4), 0.85)]


def s_beam_loop():
    """Purple thunder beam bed: dense electric hum + hiss, seamlessly looping (1 s)."""
    d = 1.2
    tt = t(d)
    hum = 0.5 * osc_from_freq(np.full(len(tt), 55.0), "saw") + 0.5 * osc_from_freq(np.full(len(tt), 110.0), "square")
    hum = bandpass(hum, 60, 4000)
    flutter = 1 + 0.25 * lowpass(noise(d), 25)
    hiss = bandpass(noise(d), 1500, 9000) * flutter
    arcs = crackle(d, 40, 2000, 9000, 0.003)
    x = mix((hum, 0.55), (hiss, 0.35), (arcs, 0.35))
    return [norm(loopable(x, 0.1), 0.55)]


def s_qi_gather():
    """Qi converging: airy inward rush with rising pitch, a soft breathy tone, no chime."""
    d = 1.3
    rush = sweep(noise(d), 250, 3000, q=4, shape=1.2) * env(d, 0.35, 0.5, 0.6, 0.3)
    tone = glide(180, 360, d, 1.3) * (1 + 0.02 * np.sin(2 * np.pi * 5 * t(d)))
    tone = lowpass(tone + 0.3 * glide(360, 720, d, 1.3), 1500) * env(d, 0.4, 0.4, 0.5, 0.3)
    swirl = sweep_curve(noise(d), 1200 + 700 * np.sin(2 * np.pi * 4 * t(d)), q=8) * env(d, 0.3, 0.6, 0.4, 0.3)
    x = mix((rush, 1.2), (tone, 0.16), (swirl, 0.5))
    return [norm(fade(reverb(x, 0.5, 0.5, 0.25), 0.05, 0.15), 0.8)]


def s_breakthrough():
    """Breaking through a realm: a heavens-splitting build, a colossal impact, a great gong bloom and
    a long shimmering wash - the biggest sound in the mod."""
    d = 4.2
    build = sweep(noise(1.0), 150, 5000, q=3, shape=0.7) * ramp(1.0, 0, 1, 2.5)
    crack = bandpass(noise(0.4), 500, 16000) * expdecay(0.4, 0.01)
    hit = impact(2.5, 120, 26, 140, crack=1.0, weight=1.8)
    g = gong(98.0, 3.2, swell=0.5)
    wash = highpass(noise(3.0), 4000) * (1 + 0.5 * lowpass(noise(3.0), 3)) * env(3.0, 0.3, 1.5, 0.15, 1.0)
    pad_ = tonal_swell(146.8, 3.0, 8, 0.009, 200, 2500) * env(3.0, 0.5, 1.0, 0.5, 1.2)
    rumble_ = brown(3.0) * expdecay(3.0, 1.2)
    x = mix((at(build, 0.0, d), 1.0), (at(crack, 1.0, d), 1.2), (at(hit, 1.0, d), 0.8), (at(g, 1.05, d), 1.0),
            (at(wash, 1.1, d), 0.35), (at(saturate(pad_, 1.5), 1.2, d), 0.4), (at(rumble_, 1.0, d), 1.5))
    x = glue(reverb(x, 1.4, 0.7, 0.35))
    return [norm(fade(x, 0.02, 0.6))]


def s_heaven_sword_fall():
    """Heaven sword descending: a falling whistle with vibrato, air roaring louder, ground shaking."""
    d = 2.6
    tt = t(d)
    fw = 2600 * (450 / 2600) ** ((tt / d) ** 1.4) * (1 + 0.01 * np.sin(2 * np.pi * 7 * tt))
    whistle = (osc_from_freq(fw) + 0.5 * osc_from_freq(fw * 2) + 0.25 * osc_from_freq(fw * 3.01)) * env(d, 0.3, 1.2, 0.7, 0.3)
    whistle = lowpass(whistle, 6000)
    wind = sweep(noise(d), 400, 3500, q=1.5, shape=0.9) * (tt / d) ** 1.3
    edge = sweep(noise(d), 4000, 900, q=10, shape=1.5) * (tt / d) ** 1.5
    rum = brown(d) * (tt / d) ** 2.5
    x = mix((whistle, 0.4), (wind, 1.0), (edge, 0.5), (rum, 1.5))
    return [norm(fade(x, 0.05, 0.02))]


def s_heaven_sword_impact():
    """Heaven sword landing: cataclysmic impact, a huge steel clang that does not ring like a bell,
    debris and a long rumble."""
    d = 3.4
    hit = impact(d, 130, 24, 110, crack=1.2, weight=2.0)
    steel = modal(410, 2.0, METAL_MODES, decay=0.35, brightness=0.8) * 0.8
    steel = saturate(steel + bandpass(noise(2.0), 1500, 8000) * expdecay(2.0, 0.05), 2.0)
    shock = sweep(noise(d), 8000, 120, q=1.0, shape=0.4, mode="low") * expdecay(d, 0.5)
    debris = crackle(d, 40, 400, 3500, 0.006) * expdecay(d, 1.2, delay=0.1)
    rum = brown(d) * expdecay(d, 1.5, delay=0.02)
    x = mix((hit, 0.8), (at(steel, 0.0, d), 0.9), (shock, 1.0), (debris, 0.6), (rum, 2.0))
    x = glue(reverb(x, 1.3, 0.65, 0.3))
    return [norm(fade(x, 0.002, 0.6))]


def s_sword_flight():
    """Riding the sword: wind rushing past with a faint steel whine (loops)."""
    d = 2.2
    tt = t(d)
    wind = bandpass(noise(d), 250, 2500) * (0.7 + 0.3 * lowpass(noise(d), 2))
    gust = sweep_curve(noise(d), 900 + 400 * np.sin(2 * np.pi * 0.9 * tt), q=3) * 0.5
    whine = resonant(noise(d), 1750, 40) * (0.8 + 0.2 * np.sin(2 * np.pi * 1.3 * tt))
    x = mix((wind, 1.0), (gust, 0.6), (whine, 0.9))
    return [norm(loopable(x, 0.15), 0.5)]


def s_learn_skill():
    """Comprehending a manual: a guzheng-like plucked pentatonic rise with a warm swell of qi."""
    d = 1.8
    x = zeros(d)
    for i, f in enumerate([293.7, 329.6, 392.0, 440.0, 587.3]):
        pl = karplus(f, 1.2, bright=0.7) * 0.7 * (0.9 - 0.08 * i)
        x += at(pl, 0.09 * i, d)
    swell = tonal_swell(146.8, d, 6, 0.008, 150, 1800) * env(d, 0.4, 0.6, 0.3, 0.6)
    air = breath(d, 500, 3000) * env(d, 0.3, 0.6, 0.3, 0.5)
    x = mix((x, 1.0), (saturate(swell, 1.3), 0.15), (air, 0.25))
    return [norm(fade(reverb(x, 0.7, 0.55, 0.3), 0.005, 0.4))]


def s_spirit_stone():
    """Absorbing a spirit stone: quick inward whoosh, a crystalline tick and a soft qi bloom."""
    d = 0.9
    inrush = sweep(noise(0.35), 300, 4000, q=5, shape=0.6) * ramp(0.35, 0, 1, 1.5)
    tick = modal(3900, 0.25, GLASS_MODES, decay=0.04) * 0.5
    bloom = lowpass(supersaw(220, 0.6, 5, 0.01), 1200) * env(0.6, 0.02, 0.4, 0.0, 0.0)
    air = breath(0.6, 600, 2500) * expdecay(0.6, 0.2)
    x = mix((at(inrush, 0, d), 1.0), (at(tick, 0.33, d), 0.6), (at(bloom, 0.34, d), 0.35), (at(air, 0.34, d), 0.5))
    return [norm(fade(reverb(x, 0.4, 0.45, 0.2)))]


def s_palm_strike():
    """Vajra palm: compressed air punched forward then a heavy, golden, saturated impact."""
    out = []
    for i in range(2):
        d = 0.9
        push = whoosh(0.35, 200, 1200 + 200 * i, 500, q=2, peak_at=0.5)
        hit = impact(0.7, 140, 40, 200 + 30 * i, crack=1.0, weight=1.0)
        gold = modal(620 + 40 * i, 0.5, METAL_MODES, decay=0.08, brightness=0.6) * 0.5
        shock = sweep(noise(0.6), 3000, 200, q=1.5, shape=0.5, mode="low") * expdecay(0.6, 0.2)
        x = mix((at(push, 0.0, d), 1.0), (at(saturate(hit, 2.5), 0.22, d), 0.7), (at(gold, 0.22, d), 0.7), (at(shock, 0.22, d), 0.9))
        out.append(norm(fade(reverb(x, 0.5, 0.45, 0.2), 0.005, 0.25)))
    return out


def s_golden_body():
    """Golden body hardening: metal skin tightening - rising metallic shimmer, a deep locked thud."""
    d = 1.8
    shimmer = sweep(noise(1.2), 800, 7000, q=15, shape=0.8) * env(1.2, 0.1, 0.6, 0.6, 0.3)
    scrape = zeros(1.2)
    for k in range(6):
        scrape += at(metal_shing(rng.uniform(1500, 3200), 0.4, decay=0.05) * 0.4, 0.15 * k, 1.2)
    lock = impact(1.0, 90, 40, 150, crack=0.6, weight=1.2)
    g = gong(130.8, 1.6, swell=0.25) * 0.5
    x = mix((at(shimmer, 0, d), 1.0), (at(scrape, 0, d), 0.8), (at(lock, 0.75, d), 0.7), (at(g, 0.75, d), 0.5))
    return [norm(fade(reverb(x, 0.8, 0.55, 0.3), 0.01, 0.4))]


def s_dragon_roar():
    """Thunder / wind dragon: a real roar - growling formant source, pitch falling, breath and thunder."""
    out = []
    for i, (f0, f1, d) in enumerate([(160, 70, 1.7), (190, 85, 1.5)]):
        r = roar(d, f0, f1, growl=30 + 8 * i) * env(d, 0.08, d - 0.5, 0.5, 0.35)
        breath_ = sweep(noise(d), 600, 2500, q=1.5, shape=0.5) * env(d, 0.05, d - 0.4, 0.4, 0.3)
        sub = lowpass(osc_from_freq(np.full(n_samples(d), f0 / 2), "saw"), 120) * env(d, 0.1, d - 0.5, 0.5, 0.3)
        x = mix((r, 1.0), (breath_, 0.5), (sub, 0.5))
        out.append(norm(fade(glue(reverb(x, 0.9, 0.55, 0.3)), 0.02, 0.4)))
    return out


def s_freeze_field():
    """Frozen domain: the world freezing over - a long crystalline growth with deep cold wind."""
    d = 2.6
    tt = t(d)
    growth = sweep(noise(d), 1200, 8000, q=14, shape=0.7) * env(d, 0.2, 1.2, 0.5, 0.8)
    ticks = crackle(d, 70, 2500, 12000, 0.002) * env(d, 0.1, 1.5, 0.3, 0.6)
    cold = lowpass(pink(d), 400) * (1 + 0.5 * lowpass(noise(d), 4)) * env(d, 0.3, 1.2, 0.6, 0.8)
    glass = zeros(d)
    for k in range(8):
        glass += at(modal(rng.uniform(2500, 6000), 0.4, GLASS_MODES, decay=0.06) * rng.uniform(0.2, 0.45), rng.uniform(0.1, 1.8), d)
    x = mix((growth, 1.0), (ticks, 0.6), (cold, 0.9), (glass, 0.5))
    return [norm(fade(reverb(x, 1.0, 0.6, 0.3), 0.05, 0.5), 0.85)]


def s_riser():
    """Anticipation before a big channelled skill: a 2 s reverse swell - filtered noise rising in
    pitch and density, a detuned drone climbing an octave, sparks thickening, cut dead at the top."""
    d = 2.0
    tt = t(d)
    u = tt / d
    swell = sweep(noise(d), 120, 6000, q=2.5, shape=0.8) * u ** 2.2
    drone = osc_from_freq(55 * 2 ** u, "saw") + osc_from_freq(55.4 * 2 ** u, "saw") + 0.5 * osc_from_freq(110 * 2 ** u, "square")
    drone = sweep(drone, 150, 3000, q=0.8, shape=1.0, mode="low") * u ** 1.8
    sparks = crackle(d, 60, 2500, 10000, 0.003) * u ** 3
    thump = np.zeros_like(tt)
    for k in range(8):  # accelerating pulses
        p = d * (1 - 0.55 * (1 - k / 8.0) ** 1.6)
        thump += at(lowpass(noise(0.12), 200) * expdecay(0.12, 0.03) * (0.3 + 0.1 * k), p, d)
    x = mix((swell, 1.0), (saturate(drone, 1.5), 0.35), (sparks, 0.5), (thump, 0.8))
    return [norm(fade(x, 0.05, 0.01), 0.9)]


def s_sub_drop():
    """Cinematic sub-drop layer for the biggest impacts: click + 90->25 Hz sine drop + saturated
    thump + a short broadband slap so it reads on small speakers too."""
    d = 1.1
    click = bandpass(noise(d), 1500, 8000) * expdecay(d, 0.004)
    drop = saturate(glide(95, 24, d, 0.45) * expdecay(d, 0.45), 1.8)
    thump = lowpass(noise(d), 180) * expdecay(d, 0.07)
    slap = bandpass(noise(d), 200, 2500) * expdecay(d, 0.03)
    x = mix((click, 0.7), (drop, 1.0), (thump, 1.2), (slap, 0.8))
    return [norm(fade(x, 0.001, 0.3))]


def s_cast_qi():
    """Qi release at the start of every skill: a short bright burst of air with a sparkle tail."""
    out = []
    for i, (fpk, d) in enumerate([(3800, 0.55), (3000, 0.6), (4600, 0.5)]):
        burst = whoosh(d, 400, fpk, 1200, q=4, peak_at=0.18, gain_curve=0.6) * expdecay(d, 0.28)
        spark = crackle(d, 90, 4000, 13000, 0.0015) * expdecay(d, 0.2, delay=0.02)
        tone = glide(fpk * 0.35, fpk * 0.6, d, 0.6) * expdecay(d, 0.12) * 0.3
        puff = lowpass(noise(d), 300) * expdecay(d, 0.05)
        x = mix((burst, 1.0), (spark, 0.35), (tone, 0.25), (puff, 1.0))
        out.append(norm(fade(reverb(x, 0.4, 0.35, 0.18))))
    return out


def _body_hit(d, crack_hi, tone_f, tail):
    """Generic synthesised body hit: click + mid thud + sub + tail (fallback when no recording)."""
    click = bandpass(noise(d), 800, crack_hi) * expdecay(d, 0.008)
    thud = bandpass(noise(d), 150, 900) * expdecay(d, 0.07)
    body = resonant(noise(d), 260, 5) * expdecay(d, 0.1)
    sub = saturate(glide(120, 50, d, 0.3) * expdecay(d, 0.07), 1.6)
    ring = resonant(noise(d), tone_f, 25) * expdecay(d, tail) * 0.3
    return mix((click, 0.7), (thud, 1.3), (body, 1.0), (sub, 0.35), (ring, 1.0))


def s_hit_slash():
    return [norm(fade(reverb(_body_hit(0.55, 9000, f, 0.12) + 0.3 * highpass(noise(0.55), 4000) * expdecay(0.55, 0.05), 0.25, 0.3, 0.12))) for f in (3200, 4100, 2600)]


def s_hit_blunt():
    return [norm(fade(reverb(_body_hit(0.5, 4000, f, 0.08), 0.3, 0.3, 0.1))) for f in (420, 300, 520)]


def s_hit_fire():
    out = []
    for f in (1800, 2400):
        d = 0.65
        x = _body_hit(d, 5000, f, 0.1) + 0.5 * crackle(d, 60, 1500, 8000, 0.003) * expdecay(d, 0.25) + 0.4 * bandpass(noise(d), 300, 3000) * expdecay(d, 0.2)
        out.append(norm(fade(reverb(x, 0.3, 0.35, 0.12))))
    return out


def s_hit_ice():
    out = []
    for f in (4200, 5600):
        d = 0.6
        x = _body_hit(d, 12000, f, 0.15) + 0.4 * crackle(d, 40, 3000, 12000, 0.002) * expdecay(d, 0.15)
        out.append(norm(fade(reverb(x, 0.35, 0.4, 0.15))))
    return out


def s_hit_shock():
    out = []
    for f in (2800, 3600):
        d = 0.5
        x = _body_hit(d, 10000, f, 0.08) + 0.5 * crackle(d, 200, 2000, 12000, 0.001) * expdecay(d, 0.12) + 0.3 * electric_buzz(d, 40, 300, dropout=0.4, shape=1.2) * expdecay(d, 0.15)
        out.append(norm(fade(reverb(x, 0.25, 0.3, 0.1))))
    return out


def s_dragon_flyby():
    out = []
    for fpk in (900, 1300):
        d = 1.1
        x = whoosh(d, 150, fpk, 200, q=3, peak_at=0.4) + 0.35 * lowpass(saturate(osc_from_freq(70 + 20 * np.sin(2 * np.pi * 3 * t(d)), "saw"), 2.0), 500) * env(d, 0.2, 0.4, 0.5, 0.2)
        out.append(norm(fade(x, 0.05, 0.2)))
    return out


SOUNDS = {
    "skill/hit_slash": s_hit_slash,
    "skill/hit_blunt": s_hit_blunt,
    "skill/hit_fire": s_hit_fire,
    "skill/hit_ice": s_hit_ice,
    "skill/hit_shock": s_hit_shock,
    "skill/dragon_flyby": s_dragon_flyby,
    "skill/cast_qi": s_cast_qi,
    "skill/riser": s_riser,
    "skill/sub_drop": s_sub_drop,
    "skill/sword_qi": s_sword_qi,
    "skill/sword_hum": s_sword_hum,
    "skill/sword_launch": s_sword_launch,
    "skill/fire_whoosh": s_fire_whoosh,
    "skill/fire_explosion": s_fire_explosion,
    "skill/ice_cast": s_ice_cast,
    "skill/ice_shatter": s_ice_shatter,
    "skill/thunder_strike": s_thunder_strike,
    "skill/thunder_charge": s_thunder_charge,
    "skill/lightning_step": s_lightning_step,
    "skill/wind_slash": s_wind_slash,
    "skill/earth_quake": s_earth_quake,
    "skill/void_drain": s_void_drain,
    "skill/void_collapse": s_void_collapse,
    "skill/shield_up": s_shield_up,
    "skill/shield_hit": s_shield_hit,
    "skill/formation": s_formation,
    "skill/beam_loop": s_beam_loop,
    "skill/qi_gather": s_qi_gather,
    "skill/breakthrough": s_breakthrough,
    "skill/heaven_sword_fall": s_heaven_sword_fall,
    "skill/heaven_sword_impact": s_heaven_sword_impact,
    "skill/sword_flight": s_sword_flight,
    "skill/palm_strike": s_palm_strike,
    "skill/golden_body": s_golden_body,
    "skill/dragon_roar": s_dragon_roar,
    "skill/freeze_field": s_freeze_field,
    "item/learn_skill": s_learn_skill,
    "item/spirit_stone": s_spirit_stone,
}

# Events whose files are played as loops (kept short and seamless, not streamed).
LOOPS = {"skill/beam_loop", "skill/sword_flight"}


def trim_silence(x, floor_db=-46.0, keep=0.05):
    """Cut the silent tail left by reverb/delay buffers (keeps `keep` seconds after the last sound)."""
    thr = 10 ** (floor_db / 20)
    idx = np.nonzero(np.abs(x) > thr)[0]
    if len(idx) == 0:
        return x
    end = min(len(x), idx[-1] + n_samples(keep))
    y = x[:end].copy()
    k = min(len(y), n_samples(0.12))
    y[-k:] *= np.linspace(1, 0, k) ** 2
    return y


# ------------------------------------------------------------------ mastering
# Target short-term loudness (A-weighted RMS of the loudest 300 ms window, dBFS) per sound class so
# that hits punch, casts sit underneath and loops stay in the background at equal in-game volume.
KIND_TARGET = {"big": -10.5, "impact": -12.5, "cast": -15.5, "soft": -18.5, "loop": -21.0}
EVENT_KIND = {
    "big": {"fire_explosion", "heaven_sword_impact", "breakthrough", "thunder_strike", "earth_quake", "void_collapse", "sub_drop"},
    "impact": {"sword_qi", "sword_launch", "ice_shatter", "shield_hit", "palm_strike", "lightning_step", "wind_slash", "fire_whoosh",
               "dragon_roar", "dragon_flyby", "hit_slash", "hit_blunt", "hit_fire", "hit_ice", "hit_shock", "golden_body"},
    "cast": {"ice_cast", "thunder_charge", "shield_up", "formation", "qi_gather", "freeze_field", "heaven_sword_fall", "void_drain",
             "cast_qi", "riser", "learn_skill", "spirit_stone"},
    "soft": {"sword_hum"},
    "loop": {"beam_loop", "sword_flight"},
}


def kind_of(path):
    base = os.path.basename(path)
    base = base.rsplit("_", 1)[0] if base[-1].isdigit() and "_" in base else base
    for k, names in EVENT_KIND.items():
        if base in names:
            return k
    return "cast"


def a_weighted(x):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    f2 = np.maximum(f, 1.0) ** 2
    ra = (12194 ** 2 * f2 ** 2) / ((f2 + 20.6 ** 2) * np.sqrt((f2 + 107.7 ** 2) * (f2 + 737.9 ** 2)) * (f2 + 12194 ** 2))
    return np.fft.irfft(spec * ra * 1.2589, n=len(x))


def short_term_db(x, win=0.3):
    """Loudest 300 ms A-weighted RMS, in dBFS."""
    w = min(len(x), n_samples(win))
    p = a_weighted(x) ** 2
    cs = np.cumsum(np.concatenate([[0.0], p]))
    rms = np.sqrt(np.max(cs[w:] - cs[:-w]) / w) if len(p) > w else np.sqrt(np.mean(p))
    return 20 * np.log10(rms + 1e-9)


def transient_shape(x, amount=0.6, fast=0.001, slow=0.05):
    """Emphasise attacks: boost where the fast envelope exceeds the slow one."""
    af = np.exp(-1 / (SR * fast))
    as_ = np.exp(-1 / (SR * slow))
    ax = np.abs(x)
    ef = signal.lfilter([1 - af], [1, -af], ax)
    es = signal.lfilter([1 - as_], [1, -as_], ax)
    gain = 1 + amount * np.clip(ef / (es + 1e-6) - 1, 0, 2.5)
    return x * gain


def soft_limit(x, ceiling=0.97, knee=0.6):
    """Soft-knee limiter: linear below `knee`, tanh-compressed above, never exceeding `ceiling`."""
    y = x.copy()
    over = np.abs(x) > knee
    span = ceiling - knee
    y[over] = np.sign(x[over]) * (knee + span * np.tanh((np.abs(x[over]) - knee) / span))
    return y


def master(x, path):
    """Final chain for every file (synth and real): rumble cut, transient emphasis on hits, a touch
    of 'air', loudness normalisation to the class target, soft limiting."""
    kind = kind_of(path)
    x = highpass(x, 30, 2)
    if kind in ("big", "impact"):
        x = transient_shape(x, 0.6 if kind == "impact" else 0.4)
    if kind != "loop":
        x = x + 0.3 * highpass(x, 6000, 2)
    target = KIND_TARGET[kind]
    gain = 10 ** ((target - short_term_db(x)) / 20)
    x = x * min(gain, 40.0)
    if kind in ("big", "impact") and np.max(np.abs(x)) > 1.3:
        # Spiky material (a click on top of a short body) cannot reach the target by gain alone:
        # a two-stage soft clip squashes the peak by up to ~8 dB - the classic game-SFX "punch" -
        # then the loudness is re-aimed at the target.
        x = soft_limit(x / np.max(np.abs(x)) * 2.2, 0.97, 0.3)
        x = x * min(10 ** ((target - short_term_db(x)) / 20), 40.0)
    x = soft_limit(x)
    if np.max(np.abs(x)) > 0.985:
        x = x / np.max(np.abs(x)) * 0.985
    return x


def write(path, x):
    full = os.path.join(OUT, path + ".ogg")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    if path not in LOOPS:
        x = trim_silence(x)
    x = master(x, path)
    x = np.clip(x, -1, 1).astype(np.float32)
    sf.write(full, x, SR, format="OGG", subtype="VORBIS")
    return len(x) / SR


def analyse(name, x):
    """Crude sanity metrics: RMS, power-weighted spectral centroid, energy share per band
    (<250, 250-1k, 1k-4k, 4k-10k, >10k Hz) and spectral flatness (0 = pure tone, 1 = white noise)."""
    rms = float(np.sqrt(np.mean(x ** 2)))
    spec = np.abs(np.fft.rfft(x * np.hanning(len(x)))) ** 2
    f = np.fft.rfftfreq(len(x), 1 / SR)
    # A-weighting so the shares reflect perceived loudness rather than raw sub-bass power.
    f2 = np.maximum(f, 1.0) ** 2
    ra = (12194 ** 2 * f2 ** 2) / ((f2 + 20.6 ** 2) * np.sqrt((f2 + 107.7 ** 2) * (f2 + 737.9 ** 2)) * (f2 + 12194 ** 2))
    spec = spec * (ra * 1.2589) ** 2
    cent = int(np.sum(spec * f) / (np.sum(spec) + 1e-9))
    edges = [0, 250, 1000, 4000, 10000, SR / 2]
    tot = np.sum(spec) + 1e-9
    bands = [int(round(100 * np.sum(spec[(f >= a) & (f < b)]) / tot)) for a, b in zip(edges[:-1], edges[1:])]
    sp = spec[(f > 60) & (f < 12000)] + 1e-12
    flat = float(np.exp(np.mean(np.log(sp))) / np.mean(sp))
    print("    %-30s ST %.1f dB peak %.2f cent %5d Hz  bands%% %-22s flat %.3f" % (name, short_term_db(x), float(np.max(np.abs(x))), cent, bands, flat))


def build_sounds_json():
    """Merge the synth and real manifests into assets/celestialarts/sounds.json.

    Manifests map "skill/name" -> ["sounds/...-relative paths without .ogg"]. When an event has real
    recordings (tools/sounds_real.json, produced by tools/fetch_real_sounds.py) those are used;
    otherwise the synthesised renders are. Files that do not exist are skipped with a warning so a
    partially failed download never produces a broken resource pack."""
    synth = json.load(open(SYNTH_MANIFEST)) if os.path.exists(SYNTH_MANIFEST) else {}
    real = json.load(open(REAL_MANIFEST)) if os.path.exists(REAL_MANIFEST) else {}
    data = {}
    total = 0
    for name in sorted(set(synth) | set(real)):
        variants = real.get(name) or synth.get(name) or []
        variants = [v for v in variants if os.path.exists(os.path.join(SOUND_ROOT, v + ".ogg"))]
        if not variants:
            print("WARNING: no files for %s" % name)
            continue
        event = name.replace("/", ".")
        data[event] = {
            "category": "player",
            "subtitle": "subtitles.celestialarts." + event,
            "sounds": [{"name": "celestialarts:" + v, "stream": False} for v in variants],
        }
        total += len(variants)
    with open(os.path.join(ASSETS, "sounds.json"), "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")
    print("sounds.json (%d events, %d files; %d events use real recordings)" % (len(data), total, len([n for n in data if n.replace(".", "/") in real])))
    return data


def main():
    do_analyse = "--analyse" in sys.argv
    os.makedirs(OUT, exist_ok=True)
    # Remove stale renders so the tree only holds what this script produces.
    for root, _, fs in os.walk(OUT):
        for f in fs:
            if f.endswith(".ogg"):
                os.remove(os.path.join(root, f))
    files = {}
    for name, fn in SOUNDS.items():
        variants = fn()
        paths = []
        for i, x in enumerate(variants):
            p = name if len(variants) == 1 else "%s_%d" % (name, i + 1)
            dur = write(p, x)
            paths.append(p)
            print("%-32s %.2fs" % (p, dur))
            if do_analyse:
                analyse(p, master(x if p in LOOPS else trim_silence(x), p))
        files[name] = ["synth/" + p for p in paths]
    with open(SYNTH_MANIFEST, "w") as f:
        json.dump(files, f, indent="\t")
        f.write("\n")
    build_sounds_json()


if __name__ == "__main__":
    main()
