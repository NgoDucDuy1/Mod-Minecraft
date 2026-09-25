#!/usr/bin/env python3
"""Synthesises the custom mono OGG sound effects for Celestial Arts and writes sounds.json.

    python3 tools/gen_sounds.py

Requires numpy + soundfile (libsndfile with Vorbis support).
"""
import json
import math
import os
import random

import numpy as np
import soundfile as sf

SR = 44100
ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "celestialarts")
OUT = os.path.join(ASSETS, "sounds")

rng = np.random.default_rng(1234)


# ------------------------------------------------------------------ building blocks

def t(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, attack, decay, sustain=0.0, release=None, curve=2.0):
    """Simple ADSR-ish envelope over n samples (times in seconds)."""
    a = int(attack * SR)
    d = int(decay * SR)
    r = int((release if release is not None else 0) * SR)
    e = np.zeros(n)
    a = min(a, n)
    e[:a] = np.linspace(0, 1, a) ** (1 / curve) if a > 0 else 1
    end_d = min(a + d, n)
    if end_d > a:
        e[a:end_d] = 1 - (1 - sustain) * (np.linspace(0, 1, end_d - a) ** curve)
    e[end_d:] = sustain
    if r > 0 and n - r > 0:
        e[n - r:] *= np.linspace(1, 0, r) ** curve
    return e


def fade(x, fin=0.005, fout=0.02):
    n = len(x)
    i = min(n, int(fin * SR))
    o = min(n, int(fout * SR))
    if i > 0:
        x[:i] *= np.linspace(0, 1, i)
    if o > 0:
        x[-o:] *= np.linspace(1, 0, o)
    return x


def noise(dur):
    return rng.standard_normal(int(SR * dur))


def bandpass(x, lo, hi):
    """FFT brick-wall-ish bandpass with soft edges."""
    n = len(x)
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1 / SR)
    lo_e = 1 / (1 + np.exp(np.clip(-(f - lo) / max(1.0, lo * 0.15), -60, 60)))
    hi_e = 1 / (1 + np.exp(np.clip((f - hi) / max(1.0, hi * 0.15), -60, 60)))
    return np.fft.irfft(X * lo_e * hi_e, n)


def sweep_bandpass(x, lo0, hi0, lo1, hi1, blocks=48):
    """Time-varying bandpass by processing overlapping blocks."""
    n = len(x)
    out = np.zeros(n)
    win_len = n // blocks * 2
    hop = win_len // 2
    window = np.hanning(win_len)
    for i in range(blocks + 1):
        s = i * hop
        e = min(n, s + win_len)
        if e - s < 16:
            break
        u = i / blocks
        seg = x[s:e] * window[: e - s]
        lo = lo0 * (lo1 / lo0) ** u
        hi = hi0 * (hi1 / hi0) ** u
        out[s:e] += bandpass(seg, lo, hi)
    return out


def sine(freq, dur, phase=0.0):
    return np.sin(2 * np.pi * freq * t(dur) + phase)


def glide(f0, f1, dur, shape=1.0):
    tt = t(dur)
    u = (tt / dur) ** shape
    f = f0 * (f1 / f0) ** u
    phase = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(phase)


def saw(freq, dur):
    tt = t(dur)
    return 2 * ((tt * freq) % 1.0) - 1


def bell(freq, dur, partials=((1.0, 1.0), (2.0, 0.5), (3.01, 0.3), (4.2, 0.2), (5.4, 0.12))):
    tt = t(dur)
    x = np.zeros_like(tt)
    for ratio, amp in partials:
        x += amp * np.sin(2 * np.pi * freq * ratio * tt) * np.exp(-tt * (2.0 + ratio * 1.2))
    return x


def clang(freq, dur):
    tt = t(dur)
    x = np.zeros_like(tt)
    for ratio, amp in ((1.0, 1.0), (1.48, 0.7), (2.13, 0.5), (2.9, 0.35), (3.7, 0.25), (5.1, 0.15)):
        x += amp * np.sin(2 * np.pi * freq * ratio * tt) * np.exp(-tt * (3.0 + ratio * 2.0))
    return x


def rumble(dur, cutoff=120.0):
    x = np.cumsum(noise(dur))
    x -= np.linspace(x[0], x[-1], len(x))
    x = bandpass(x, 15, cutoff)
    return x / (np.max(np.abs(x)) + 1e-9)


def crackle(dur, density=40, lo=1500, hi=7000):
    x = np.zeros(int(SR * dur))
    n = int(density * dur)
    for _ in range(n):
        p = rng.integers(0, len(x) - 400)
        L = rng.integers(40, 400)
        x[p:p + L] += rng.standard_normal(L) * np.exp(-np.arange(L) / (L / 3)) * rng.uniform(0.3, 1.0)
    return bandpass(x, lo, hi)


def mix(*parts):
    n = max(len(p) for p, _ in parts)
    out = np.zeros(n)
    for p, g in parts:
        out[: len(p)] += p * g
    return out


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def pad(x, dur):
    n = int(SR * dur)
    if len(x) >= n:
        return x[:n]
    return np.concatenate([x, np.zeros(n - len(x))])


def write(name, x):
    os.makedirs(os.path.dirname(os.path.join(OUT, name + ".ogg")), exist_ok=True)
    x = np.clip(x, -1, 1).astype(np.float32)
    sf.write(os.path.join(OUT, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
    print("%-32s %.2fs" % (name, len(x) / SR))


# ------------------------------------------------------------------------ sounds

def s_sword_qi():
    d = 0.7
    swoosh = sweep_bandpass(noise(d), 600, 4000, 2500, 9000) * env(int(SR * d), 0.02, 0.45)
    ring = mix((glide(2400, 1900, d), 1.0), (glide(3600, 3100, d), 0.4)) * env(int(SR * d), 0.005, 0.6)
    return norm(fade(mix((swoosh, 1.0), (ring, 0.45))))


def s_sword_hum():
    d = 1.6
    tt = t(d)
    vib = 1 + 0.01 * np.sin(2 * np.pi * 5.5 * tt)
    base = np.sin(2 * np.pi * np.cumsum(110 * vib) / SR) + 0.5 * np.sin(2 * np.pi * np.cumsum(220 * vib) / SR) + 0.2 * np.sin(2 * np.pi * np.cumsum(440 * vib) / SR)
    shimmer = bandpass(noise(d), 3000, 8000) * (0.5 + 0.5 * np.sin(2 * np.pi * 3 * tt))
    x = mix((base, 1.0), (shimmer, 0.12)) * env(int(SR * d), 0.25, 0.4, 0.8, 0.5)
    return norm(fade(x, 0.05, 0.1), 0.6)


def s_sword_launch():
    d = 0.9
    whoosh = sweep_bandpass(noise(d), 300, 1500, 2500, 10000) * env(int(SR * d), 0.15, 0.5)
    ping = bell(1800, 0.6) * 0.6
    ping = np.concatenate([np.zeros(int(0.12 * SR)), ping])
    return norm(fade(mix((whoosh, 1.0), (ping, 0.5))))


def s_fire_whoosh():
    d = 0.9
    body = sweep_bandpass(noise(d), 120, 900, 400, 2500) * env(int(SR * d), 0.08, 0.6)
    crack = crackle(d, 30, 2000, 6000) * env(int(SR * d), 0.1, 0.6)
    return norm(fade(mix((body, 1.0), (crack, 0.35))))


def s_fire_explosion():
    d = 1.8
    boom = glide(140, 35, d, 0.5) * env(int(SR * d), 0.005, 1.2)
    burst = bandpass(noise(d), 80, 5000) * env(int(SR * d), 0.002, 0.35)
    tail = rumble(d, 200) * env(int(SR * d), 0.05, 1.5)
    crack = crackle(d, 25, 1500, 5000) * env(int(SR * d), 0.05, 1.2)
    return norm(fade(mix((boom, 1.0), (burst, 0.8), (tail, 0.6), (crack, 0.3))))


def s_ice_cast():
    d = 1.0
    x = np.zeros(int(SR * d))
    for i, f in enumerate([2093, 2637, 3136, 3951, 4699]):
        b = bell(f, 0.7) * 0.5
        s = int(0.06 * i * SR)
        x[s:s + len(b)] += b[: len(x) - s]
    shimmer = bandpass(noise(d), 5000, 12000) * env(int(SR * d), 0.1, 0.7)
    return norm(fade(mix((x, 1.0), (shimmer, 0.25))))


def s_ice_shatter():
    d = 0.8
    burst = bandpass(noise(d), 1500, 12000) * env(int(SR * d), 0.001, 0.25)
    x = burst.copy()
    for i in range(10):
        f = rng.uniform(2500, 6500)
        b = bell(f, 0.4) * rng.uniform(0.2, 0.5)
        s = int(rng.uniform(0.0, 0.3) * SR)
        x[s:s + len(b)] += b[: len(x) - s]
    return norm(fade(x))


def s_thunder_strike():
    d = 2.4
    crack = bandpass(noise(d), 500, 12000) * env(int(SR * d), 0.001, 0.12)
    body = rumble(d, 150) * env(int(SR * d), 0.01, 2.0)
    mid = bandpass(noise(d), 100, 800) * env(int(SR * d), 0.005, 0.6)
    return norm(fade(mix((crack, 1.0), (body, 1.2), (mid, 0.5))))


def s_thunder_charge():
    d = 1.4
    tt = t(d)
    buzz = np.zeros_like(tt)
    f = 60 * (8.0 ** (tt / d))
    phase = np.cumsum(f) / SR
    buzz += 2 * (phase % 1.0) - 1
    buzz = bandpass(buzz, 80, 6000)
    crack = crackle(d, 60, 2000, 9000) * (tt / d) ** 2
    x = mix((buzz, 0.6), (crack, 0.5)) * env(int(SR * d), 0.2, 0.2, 1.0, 0.05)
    return norm(fade(x))


def s_lightning_step():
    d = 0.45
    zap = bandpass(noise(d), 2000, 9000) * env(int(SR * d), 0.001, 0.15)
    whoosh = sweep_bandpass(noise(d), 800, 4000, 200, 1200) * env(int(SR * d), 0.02, 0.35)
    tone = glide(1200, 300, d) * env(int(SR * d), 0.001, 0.2)
    return norm(fade(mix((zap, 1.0), (whoosh, 0.6), (tone, 0.3))))


def s_wind_slash():
    d = 0.5
    x = sweep_bandpass(noise(d), 1500, 6000, 400, 2500) * env(int(SR * d), 0.03, 0.4)
    return norm(fade(x))


def s_earth_quake():
    d = 2.2
    body = rumble(d, 90) * env(int(SR * d), 0.05, 1.8)
    cracks = crackle(d, 12, 300, 2000) * env(int(SR * d), 0.02, 1.5)
    hit = bandpass(noise(d), 60, 600) * env(int(SR * d), 0.002, 0.3)
    return norm(fade(mix((body, 1.2), (cracks, 0.6), (hit, 0.8))))


def s_void_drain():
    d = 1.5
    tt = t(d)
    drone = np.sin(2 * np.pi * 55 * tt) + np.sin(2 * np.pi * 55.7 * tt) + 0.4 * np.sin(2 * np.pi * 110.5 * tt)
    suck = sweep_bandpass(noise(d), 200, 800, 1500, 6000) * env(int(SR * d), 0.3, 0.6, 0.6, 0.3)
    x = mix((drone, 0.6), (suck, 0.8)) * env(int(SR * d), 0.2, 0.3, 0.9, 0.3)
    return norm(fade(x, 0.02, 0.1))


def s_shield_up():
    d = 1.0
    chord = mix((glide(330, 440, d), 1.0), (glide(415, 554, d), 0.7), (glide(494, 659, d), 0.5)) * env(int(SR * d), 0.05, 0.6, 0.2, 0.3)
    shimmer = bandpass(noise(d), 4000, 10000) * env(int(SR * d), 0.2, 0.5)
    thud = bandpass(noise(d), 80, 400) * env(int(SR * d), 0.002, 0.2)
    return norm(fade(mix((chord, 0.8), (shimmer, 0.2), (thud, 0.6))))


def s_shield_hit():
    d = 0.6
    x = mix((clang(620, d), 1.0), (bandpass(noise(d), 800, 6000) * env(int(SR * d), 0.001, 0.08), 0.6))
    return norm(fade(x))


def s_formation():
    d = 2.0
    x = np.zeros(int(SR * d))
    for i, f in enumerate([261.6, 329.6, 392.0, 523.3, 659.3]):
        b = bell(f, 1.6) * 0.5
        s = int(0.12 * i * SR)
        x[s:s + len(b)] += b[: len(x) - s]
    pad_ = mix((sine(130.8, d), 1.0), (sine(196.0, d), 0.6), (sine(261.6, d), 0.4)) * env(int(SR * d), 0.5, 0.8, 0.6, 0.6)
    shimmer = bandpass(noise(d), 5000, 11000) * env(int(SR * d), 0.6, 0.8, 0.3, 0.5)
    return norm(fade(mix((x, 1.0), (pad_, 0.35), (shimmer, 0.15)), 0.02, 0.2))


def s_beam_loop():
    d = 1.0  # exactly 1 s so integer-Hz components loop seamlessly
    tt = t(d)
    x = np.zeros_like(tt)
    for f, a in ((90, 1.0), (180, 0.6), (271, 0.4), (360, 0.3), (543, 0.25), (905, 0.15)):
        x += a * np.sin(2 * np.pi * f * tt)
    buzz = 2 * ((tt * 90) % 1.0) - 1
    n = bandpass(noise(d), 2000, 8000)
    # make the noise loop by cross-fading its ends
    k = int(0.05 * SR)
    ramp = np.linspace(0, 1, k)
    n[:k] = n[:k] * ramp + n[-k:] * (1 - ramp)
    x = mix((x, 0.6), (buzz, 0.25), (n, 0.15))
    return norm(x, 0.55)


def s_qi_gather():
    d = 1.2
    whoosh = sweep_bandpass(noise(d), 200, 800, 1200, 5000) * env(int(SR * d), 0.4, 0.5, 0.5, 0.3)
    tones = mix((glide(440, 880, d), 1.0), (glide(660, 1320, d), 0.5)) * env(int(SR * d), 0.3, 0.5, 0.4, 0.3)
    return norm(fade(mix((whoosh, 1.0), (tones, 0.3)), 0.05, 0.1))


def s_breakthrough():
    d = 3.2
    boom = glide(110, 30, d, 0.5) * env(int(SR * d), 0.005, 1.5)
    burst = bandpass(noise(d), 100, 8000) * env(int(SR * d), 0.002, 0.4)
    body = rumble(d, 160) * env(int(SR * d), 0.02, 2.5)
    chord = np.zeros(int(SR * d))
    for i, f in enumerate([523.3, 659.3, 784.0, 1046.5, 1318.5, 1568.0]):
        b = bell(f, 2.4) * 0.5
        s = int((0.25 + 0.1 * i) * SR)
        chord[s:s + len(b)] += b[: len(chord) - s]
    shimmer = bandpass(noise(d), 5000, 12000) * env(int(SR * d), 0.5, 1.5, 0.2, 0.8)
    return norm(fade(mix((boom, 1.0), (burst, 0.7), (body, 0.7), (chord, 0.9), (shimmer, 0.15)), 0.01, 0.3))


def s_heaven_sword_fall():
    d = 2.6
    whistle = glide(2200, 500, d, 1.3) * env(int(SR * d), 0.3, 1.0, 0.7, 0.4)
    wind = sweep_bandpass(noise(d), 300, 1500, 1500, 8000) * env(int(SR * d), 0.6, 1.0, 1.0, 0.2)
    rum = rumble(d, 120) * (t(d) / d) ** 2
    return norm(fade(mix((whistle, 0.4), (wind, 1.0), (rum, 0.9)), 0.05, 0.05))


def s_heaven_sword_impact():
    d = 3.0
    boom = glide(120, 28, d, 0.5) * env(int(SR * d), 0.003, 1.6)
    burst = bandpass(noise(d), 80, 10000) * env(int(SR * d), 0.001, 0.5)
    ring = clang(880, 2.2) * 0.6
    body = rumble(d, 140) * env(int(SR * d), 0.02, 2.5)
    cracks = crackle(d, 20, 400, 3000) * env(int(SR * d), 0.05, 1.5)
    return norm(fade(mix((boom, 1.0), (burst, 0.9), (ring, 0.5), (body, 0.8), (cracks, 0.4)), 0.005, 0.3))


def s_sword_flight():
    d = 2.0
    tt = t(d)
    wind = bandpass(noise(d), 300, 3000) * (0.7 + 0.3 * np.sin(2 * np.pi * 1.5 * tt))
    hum = np.sin(2 * np.pi * 165 * tt) + 0.4 * np.sin(2 * np.pi * 330 * tt)
    k = int(0.1 * SR)
    ramp = np.linspace(0, 1, k)
    wind[:k] = wind[:k] * ramp + wind[-k:] * (1 - ramp)
    x = mix((wind, 1.0), (hum, 0.15))
    return norm(x, 0.5)


def s_learn_skill():
    d = 1.4
    x = np.zeros(int(SR * d))
    for i, f in enumerate([659.3, 784.0, 987.8, 1318.5]):
        b = bell(f, 1.0) * 0.6
        s = int(0.11 * i * SR)
        x[s:s + len(b)] += b[: len(x) - s]
    shimmer = bandpass(noise(d), 6000, 12000) * env(int(SR * d), 0.3, 0.6)
    return norm(fade(mix((x, 1.0), (shimmer, 0.12)), 0.005, 0.2))


def s_spirit_stone():
    d = 0.8
    pop = bandpass(noise(d), 800, 5000) * env(int(SR * d), 0.001, 0.06)
    x = mix((pop, 0.6), (bell(1568, 0.7), 0.7), (bell(2349, 0.6), 0.4))
    return norm(fade(x))


SOUNDS = {
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
    "skill/shield_up": s_shield_up,
    "skill/shield_hit": s_shield_hit,
    "skill/formation": s_formation,
    "skill/beam_loop": s_beam_loop,
    "skill/qi_gather": s_qi_gather,
    "skill/breakthrough": s_breakthrough,
    "skill/heaven_sword_fall": s_heaven_sword_fall,
    "skill/heaven_sword_impact": s_heaven_sword_impact,
    "skill/sword_flight": s_sword_flight,
    "item/learn_skill": s_learn_skill,
    "item/spirit_stone": s_spirit_stone,
}


def write_sounds_json():
    data = {}
    for path in SOUNDS:
        event = path.replace("/", ".")
        data[event] = {
            "category": "player",
            "subtitle": "subtitles.celestialarts." + event,
            "sounds": [{"name": "celestialarts:" + path, "stream": False}],
        }
    with open(os.path.join(ASSETS, "sounds.json"), "w") as f:
        json.dump(data, f, indent="\t")
        f.write("\n")
    print("sounds.json (%d events)" % len(data))


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in SOUNDS.items():
        write(name, fn())
    write_sounds_json()


if __name__ == "__main__":
    main()
