#!/usr/bin/env python3
"""Downloads CC0 field recordings from Freesound and edits them into the mod's sound events.

    python3 tools/fetch_real_sounds.py            # download (cached), render, rebuild sounds.json
    python3 tools/fetch_real_sounds.py --analyse  # + loudness / spectrum report
    python3 tools/fetch_real_sounds.py --offline  # use only the download cache (no network)

Every source below is published under Creative Commons 0 (public domain dedication), so the edited
clips can ship inside the mod without restrictions; tools/SOUND_CREDITS.md lists them anyway.

Real recordings are used for everything that exists in the real world - thunder, fire, explosions,
sword steel and swings, wind, ice and glass, falling rock, electric arcs, gongs, a plucked zither.
Skills without a real-world counterpart (qi, void, shields, dragons...) keep the synthesised sounds
from tools/gen_sounds.py, sometimes with a real layer mixed in (rising wind under the falling
heaven sword, a gong and thunder inside the breakthrough).

The HQ preview (128 kbps MP3) of each sound is fetched from Freesound's CDN, decoded with ffmpeg,
trimmed automatically around its loudest onset, filtered/layered/pitched as each recipe describes
and written as mono OGG Vorbis into assets/celestialarts/sounds/real/. A manifest
(tools/sounds_real.json) tells gen_sounds.build_sounds_json which events are covered.
"""
import json
import os
import shutil
import subprocess
import sys
import urllib.request

import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_sounds as g  # noqa: E402  (DSP toolkit + synth layers)

SR = g.SR
CACHE = os.path.join(g.TOOLS, ".cache", "freesound")
OUT = os.path.join(g.SOUND_ROOT, "real")
CREDITS = os.path.join(g.TOOLS, "SOUND_CREDITS.md")

# --------------------------------------------------------------------------- sources
# key: (freesound id, uploader id, title, author) - all CC0.
SOURCES = {
    # thunder
    "thunder_netaj": (193170, 3012047, "thunder", "netaj"),
    "thunder_josh": (475094, 9608356, "thunder3.ogg", "Josh74000MC"),
    "thunder_strike3s": (195439, 3634316, "strike 3 sec.wav", "Littlebrojay"),
    "thunder_extreme": (615844, 13632374, "extreme_thunder.wav", "Hajisounds"),
    # fire
    "fire_whoosh_diesel": (244926, 3983630, "fire-whoosh.wav", "hnhnh"),
    "fire_combust": (412558, 6031117, "Combust.wav", "GrimGrum"),
    "fire_flare": (530355, 10106687, "Fire.wav", "danielpodlovics"),
    # explosions (fireworks / firecrackers, recorded)
    "boom_firework_sharp": (336011, 4921277, "Sharp Explosion 4 (of 5)", "Rudmer_Rotteveel"),
    "boom_firecracker": (195181, 464940, "explosion.wav", "insanity54"),
    "boom_triple": (109753, 1868848, "tre hit long.aiff", "SoundCollectah"),
    "boom_bag": (75330, 339378, "oddworld_bomb.wav", "Oddworld"),
    # sword swings & steel
    "swing_tube": (263595, 4946670, "swoosh.wav", "PorkMuncher"),
    "swing_metal": (522701, 11584853, "Weapon, Sword, Metal sword, Swing, fast, impact", "julianmateo_"),
    "swing_sword2": (353708, 5487341, "Sword Swings", "SamsterBirdies"),
    "swing_foam": (367182, 5065048, "swing.mp3", "GaussTheWizard"),
    "steel_unsheathe": (467291, 9895957, "Sword Unsheathed.wav", "XfiXy8"),
    "steel_machete": (581594, 5487341, "Sword draw unsheathe", "SamsterBirdies"),
    "steel_slide": (471147, 9938764, "Sword slide.wav", "AuDRoger"),
    "steel_slide2": (471148, 9938764, "Sword slide 2.wav", "AuDRoger"),
    # ice & glass
    "ice_foley": (342546, 3562222, "Ice cracking", "timbreknight"),
    "ice_piezo": (262635, 3175560, "Ice Crack 1", "j_p_higgins"),
    "ice_step": (624163, 13582756, "ice_cracking_01.flac", "wwstudioswastaken"),
    "glass_impact": (418194, 3656686, "Hard Glass Impact", "deleted_user_3656686"),
    # rock
    "rock_wall": (202098, 3756348, "Falling Rock.wav", "spookymodem"),
    "rock_tumble": (389618, 6068748, "Rock Tumble 2.wav", "_stubb"),
    "rock_thrown": (426318, 8522109, "Rocks Falling.wav", "MTJohnson"),
    # electricity
    "spark_zapper": (189630, 2816253, "Spark", "elliott.klein"),
    "arc_hv": (403252, 7809148, "Taser/High Voltage discharge", "The_Chemical_Workshop"),
    "arc_taser": (540012, 11920129, "taser.wav", "birdswkaren"),
    # wind
    "wind_gust": (381853, 4277312, "wind_gust_short_sqeeeek.wav", "sqeeeek"),
    "wind_woosh": (569109, 10612333, "Short Wind/Air Woosh | A gust of wind", "NIKOKnecht"),
    "wind_woosh2": (683096, 6253486, "Woosh", "florianreichelt"),
    # gongs & zither
    "gong_boss": (121800, 1525789, "gong.WAV", "BOSS MUSIC"),
    "gong_di": (127265, 1779874, "Gong.wav", "DiArchangeli"),
    "gong_paiste": (112507, 1985275, "Gong.wav (Paiste)", "cdiupe"),
    "zither_a2": (24551, 164315, "kayageum1_A2.wav", "spt3125"),
    # body / blade impacts
    "hit_sword_body": (411122, 1424100, "Sword hits the body", "vdovitsky"),
    "hit_sword_impact": (547042, 7614679, "Hit Impact Sword 3", "CogFireStudios"),
    "punch_newage": (348242, 4067257, "punch", "newagesoup"),
    "punch_taylor": (94778, 1533925, "punch", "taylorsyoung"),
    "punch_jew": (244513, 2756077, "punch", "JewTwinz"),
    "punch_insanity": (276600, 464940, "punch.wav", "insanity54"),
    "thud_metal": (244983, 3008343, "metallic thud", "ani_music"),
    "collision_fast": (332056, 71257, "collision", "qubodup"),
    # creature roars (recorded voice, slowed)
    "roar_mighty": (420466, 8379536, "Roar.m4a", "tahirahg1991"),
    "roar_beast": (267452, 3415022, "Beast Roar.wav", "cylon8472"),
}


def preview_url(fsid, uid):
    return "https://cdn.freesound.org/previews/%d/%d_%d-hq.mp3" % (fsid // 1000, fsid, uid)


def page_url(fsid, author):
    return "https://freesound.org/people/%s/sounds/%d/" % (author.replace(" ", "%20"), fsid)


_loaded = {}


def load(key, offline=False):
    """Return the decoded mono 44.1 kHz recording for a source key (downloads + caches)."""
    if key in _loaded:
        return _loaded[key]
    fsid, uid, title, author = SOURCES[key]
    os.makedirs(CACHE, exist_ok=True)
    mp3 = os.path.join(CACHE, "%d.mp3" % fsid)
    wav = os.path.join(CACHE, "%d.wav" % fsid)
    if not os.path.exists(wav):
        if not os.path.exists(mp3):
            if offline:
                raise FileNotFoundError("not cached: " + key)
            req = urllib.request.Request(preview_url(fsid, uid), headers={"User-Agent": "celestialarts-sound-fetch/1.0"})
            with urllib.request.urlopen(req, timeout=60) as r, open(mp3, "wb") as f:
                shutil.copyfileobj(r, f)
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", mp3, "-ac", "1", "-ar", str(SR), wav], check=True)
    x, sr = sf.read(wav, dtype="float64")
    if x.ndim > 1:
        x = x.mean(axis=1)
    assert sr == SR
    x = x - np.mean(x)
    _loaded[key] = x
    return x


# --------------------------------------------------------------------------- editing helpers

def envelope(x, hz=40.0):
    return g.lowpass(np.abs(x), hz, 2)


def onset(x, thresh_db=-24.0, pre=0.015, search_from=0.0):
    """Index of the start of the loudest event: walk back from the peak until the envelope has
    dropped `thresh_db` below it, then add a short pre-roll."""
    e = envelope(x)
    s0 = g.n_samples(search_from)
    if s0 >= len(e) - g.n_samples(0.2):
        s0 = 0
    peak = s0 + int(np.argmax(e[s0:]))
    thr = e[peak] * 10 ** (thresh_db / 20)
    i = peak
    while i > s0 and e[i] > thr:
        i -= 1
    return max(0, i - g.n_samples(pre))


def clip(x, start_s=None, dur=1.0, fade_in=0.004, fade_out=0.08, search_from=0.0):
    """Cut `dur` seconds from `start_s` (or from the automatic onset) with fades."""
    s = g.n_samples(start_s) if start_s is not None else onset(x, search_from=search_from)
    y = x[s:s + g.n_samples(dur)].copy()
    return g.fade(y, fade_in, min(fade_out, len(y) / SR / 2))


def segment(x, start_s, dur, fade_in=0.01, fade_out=0.05):
    return clip(x, start_s, dur, fade_in, fade_out)


def pitch(x, ratio):
    """Playback-rate change (ratio > 1 = higher and shorter)."""
    return g.resample(x, ratio)


def shape(x, attack, hold, release):
    """Impose a fast attack/hold/release window (turns a slow gust into a slash)."""
    n = len(x)
    e = np.zeros(n)
    a, h, r = (g.n_samples(v) for v in (attack, hold, release))
    a = min(a, n)
    e[:a] = np.linspace(0, 1, a) ** 0.7
    e[a:min(n, a + h)] = 1
    rs = min(n, a + h)
    re = min(n, rs + r)
    if re > rs:
        e[rs:re] = np.linspace(1, 0, re - rs) ** 1.5
    return x[:re] * e[:re]


def denoise_gate(x, floor_db=-40.0, hz=30.0):
    """Very light expander: attenuates the recording's noise floor between events."""
    e = envelope(x, hz)
    peak = np.max(e) + 1e-9
    gain = np.clip((e / peak) / 10 ** (floor_db / 20), 0, 1) ** 0.5
    return x * gain


def finish(x, peak=0.9, hp=30.0):
    x = g.highpass(x, hp, 2)
    return g.norm(g.trim_silence(x), peak)


# --------------------------------------------------------------------------- recipes
# Each returns a list of variations (arrays). Names match gen_sounds.SOUNDS keys.

def r_thunder_strike(L):
    out = []
    out.append(clip(L("thunder_netaj"), dur=4.0, fade_out=0.6))
    out.append(clip(L("thunder_josh"), dur=3.4, fade_out=0.5))
    out.append(clip(L("thunder_strike3s"), dur=3.6, fade_out=0.5))
    out.append(clip(L("thunder_extreme"), dur=4.5, fade_out=0.8))
    res = []
    for x in out:
        # a little synthetic sub weight so the crack hits on small speakers too
        sub = g.sub_boom(len(x) / SR, 70, 28, 0.35)
        res.append(finish(g.glue(g.mix((x, 1.0), (sub[: len(x)], 0.25)))))
    return res


def r_fire_whoosh(L):
    a = clip(L("fire_whoosh_diesel"), dur=1.2, fade_out=0.35)
    b = clip(L("fire_combust"), dur=1.1, fade_out=0.3)
    c = clip(L("fire_flare"), dur=0.7, fade_out=0.2)
    res = []
    for x in (a, b, c):
        crk = g.crackle(len(x) / SR, 25, 2000, 7000, 0.003) * g.expdecay(len(x) / SR, 0.5)
        res.append(finish(g.mix((x, 1.0), (crk[: len(x)], 0.12))))
    return res


def r_fire_explosion(L):
    d = 2.6
    t1 = clip(L("boom_firework_sharp"), dur=1.0)
    tail1 = g.lowpass(clip(L("thunder_netaj"), dur=2.4, fade_out=0.8), 900)
    x1 = g.mix((g.at(t1, 0, d), 1.0), (g.at(tail1, 0.02, d), 0.7), (g.at(g.sub_boom(1.5, 100, 28, 0.45), 0, d), 0.35))
    t2 = clip(L("boom_firecracker"), dur=0.9)
    bag = clip(L("boom_bag"), dur=2.2, fade_out=0.6)
    x2 = g.mix((g.at(t2, 0, d), 1.0), (g.at(bag, 0.01, d), 0.8), (g.at(g.lowpass(clip(L("thunder_josh"), dur=2.2, fade_out=0.7), 700), 0.05, d), 0.5))
    return [finish(g.glue(g.reverb(x, 0.6, 0.4, 0.15))) for x in (x1, x2)]


def r_heaven_sword_impact(L):
    d = 3.6
    boom = clip(L("boom_bag"), dur=2.5, fade_out=0.8)
    rock = clip(L("rock_wall"), dur=2.8, fade_out=0.8)
    rum = g.lowpass(clip(L("thunder_extreme"), dur=3.4, fade_out=1.0), 500)
    steel = g.modal(410, 2.0, g.METAL_MODES, decay=0.35, brightness=0.8)
    steel = g.saturate(steel + g.bandpass(g.noise(2.0), 1500, 8000) * g.expdecay(2.0, 0.05), 2.0)
    x = g.mix((g.at(boom, 0, d), 1.0), (g.at(rock, 0.05, d), 0.7), (g.at(rum, 0.0, d), 0.8), (g.at(steel, 0.0, d), 0.45),
              (g.at(g.sub_boom(1.6, 110, 26, 0.5), 0, d), 0.4))
    return [finish(g.glue(g.reverb(x, 1.2, 0.6, 0.25)))]


def _steel(L, key, dur=0.45, hp=1500):
    """Short bright piece of a real blade being drawn: the 'shing'."""
    x = L(key)
    # take the brightest ~dur window: maximise high-frequency energy (running sum via cumsum)
    hf = envelope(g.highpass(x, 3000), 60)
    w = g.n_samples(dur)
    if len(hf) > w:
        cs = np.cumsum(np.concatenate([[0.0], hf]))
        best = int(np.argmax(cs[w:] - cs[:-w]))
    else:
        best = 0
    y = x[best:best + w]
    return g.fade(g.highpass(y, hp), 0.003, 0.15) * g.expdecay(len(y) / SR, 0.18)


def r_sword_qi(L):
    swings = [clip(L("swing_tube"), dur=0.45, fade_out=0.2), clip(L("swing_metal"), dur=0.6, fade_out=0.25), clip(L("swing_sword2"), dur=0.55, fade_out=0.2)]
    steels = [_steel(L, "steel_unsheathe"), _steel(L, "steel_machete"), _steel(L, "steel_slide")]
    res = []
    for sw, st in zip(swings, steels):
        d = 0.85
        thump = g.lowpass(g.noise(0.3), 220) * g.expdecay(0.3, 0.04)
        x = g.mix((g.at(sw, 0, d), 1.0), (g.at(st, 0.04, d), 0.55), (g.at(thump, 0.0, d), 0.6))
        res.append(finish(g.reverb(x, 0.3, 0.35, 0.15)))
    return res


def r_sword_launch(L):
    res = []
    for sw_key, st_key, ratio in (("swing_tube", "steel_slide", 1.35), ("swing_foam", "steel_slide2", 1.25), ("swing_metal", "steel_unsheathe", 1.5)):
        d = 0.9
        sw = pitch(clip(L(sw_key), dur=0.6, fade_out=0.25), ratio)
        st = pitch(_steel(L, st_key, 0.5), 1.2)
        rip = g.sweep(g.noise(0.6), 700, 5000, q=8, shape=0.6) * g.env(0.6, 0.01, 0.45)
        x = g.mix((g.at(sw, 0, d), 1.0), (g.at(st, 0.02, d), 0.5), (g.at(rip, 0, d), 0.35))
        res.append(finish(g.reverb(x, 0.35, 0.4, 0.15)))
    return res


def r_wind_slash(L):
    res = []
    for key, ratio in (("wind_gust", 1.6), ("wind_woosh", 1.8), ("wind_woosh2", 1.2), ("swing_tube", 1.1)):
        x = pitch(L(key), ratio)
        x = shape(x[onset(x, -18):], 0.06, 0.12, 0.35)
        edge = g.whoosh(len(x) / SR, 1200, 4500, 900, q=14, peak_at=0.3) * 0.4
        res.append(finish(g.mix((x, 1.0), (edge[: len(x)], 0.35))))
    return res


def r_sword_flight(L):
    d = 2.35
    wind = g.bandpass(L("wind_woosh")[: g.n_samples(d)], 200, 3000)
    tt = g.t(len(wind) / SR)
    whine = g.resonant(g.noise(len(wind) / SR), 1750, 40)[: len(wind)] * (0.8 + 0.2 * np.sin(2 * np.pi * 1.3 * tt))
    x = g.mix((wind, 1.0), (whine, 0.35))
    return [g.norm(g.loopable(x, 0.15), 0.5)]


def r_ice_cast(L):
    d = 1.1
    ice = L("ice_foley")
    a = clip(ice, dur=0.9, fade_out=0.3)
    b = clip(L("ice_piezo"), dur=0.6, fade_out=0.2)
    grow = g.sweep(g.noise(d), 1500, 7000, q=12, shape=0.8) * g.env(d, 0.05, 0.5, 0.3, 0.3)
    x = g.mix((g.at(a, 0.0, d), 1.0), (g.at(b, 0.25, d), 0.8), (grow, 0.3))
    return [finish(g.reverb(x, 0.4, 0.45, 0.2))]


def r_ice_shatter(L):
    ice = L("ice_foley")
    res = []
    for k, (glass_key, seg) in enumerate((("glass_impact", 0.0), ("ice_step", 0.0))):
        d = 0.8
        gl = clip(L(glass_key), dur=0.6, fade_out=0.2)
        frag = clip(ice, dur=0.6, fade_out=0.25, search_from=1.5 + 2.0 * k)
        x = g.mix((g.at(gl, 0, d), 1.0), (g.at(g.highpass(frag, 2500), 0.03, d), 0.7))
        res.append(finish(g.reverb(x, 0.3, 0.4, 0.18)))
    return res


def r_freeze_field(L):
    d = 2.8
    ice = L("ice_foley")
    a = denoise_gate(ice[: g.n_samples(2.6)])
    a = g.fade(a, 0.05, 0.6)
    b = clip(L("ice_piezo"), dur=0.8, fade_out=0.3)
    cold = g.lowpass(g.pink(d), 400) * (1 + 0.5 * g.lowpass(g.noise(d), 4)) * g.env(d, 0.3, 1.2, 0.6, 0.8)
    growth = g.sweep(g.noise(d), 1200, 8000, q=14, shape=0.7) * g.env(d, 0.2, 1.2, 0.5, 0.8)
    x = g.mix((g.at(a, 0.0, d), 1.0), (g.at(b, 1.2, d), 0.7), (cold, 0.6), (growth, 0.35))
    return [finish(g.reverb(x, 1.0, 0.6, 0.3), 0.85)]


def r_earth_quake(L):
    res = []
    for wall_key, dur in (("rock_wall", 2.4), ("rock_thrown", 1.8)):
        d = dur + 0.4
        wall = clip(L(wall_key), dur=dur, fade_out=0.5)
        tum = clip(L("rock_tumble"), dur=1.1, fade_out=0.3)
        rum = g.lowpass(clip(L("thunder_netaj"), dur=dur, fade_out=0.6), 350)
        thud = g.impact(1.2, 70, 26, 120, crack=0.4, weight=1.2)
        x = g.mix((g.at(wall, 0.02, d), 1.0), (g.at(tum, 0.0, d), 0.8), (g.at(rum, 0.0, d), 0.9), (g.at(thud, 0.0, d), 0.6))
        res.append(finish(g.glue(g.reverb(x, 0.7, 0.5, 0.2))))
    return res


def r_thunder_charge(L):
    d = 1.5
    arc = clip(L("arc_hv"), dur=1.5, fade_out=0.05)
    arc = arc * (g.t(len(arc) / SR) / d) ** 0.8
    spark = clip(L("spark_zapper"), dur=0.15)
    sparks = g.zeros(d)
    for i in range(18):
        p = d * (1 - (1 - i / 18.0) ** 2)  # denser towards the end
        sparks += g.at(spark * g.rng.uniform(0.4, 1.0), min(d - 0.16, p), d)
    buzz = g.electric_buzz(d, 45, 400, dropout=0.35, shape=1.3) * g.env(d, 0.15, 0.2, 1.0, 0.05)
    x = g.mix((g.at(arc, 0, d), 1.0), (sparks, 0.8), (buzz, 0.25))
    return [g.norm(g.fade(g.highpass(x, 30, 2), 0.02, 0.02), 0.9)]


def r_lightning_step(L):
    d = 0.55
    spark = clip(L("spark_zapper"), dur=0.15)
    taser = clip(L("arc_taser"), dur=0.3, fade_out=0.15)
    gust = shape(pitch(L("wind_gust"), 2.2), 0.01, 0.05, 0.3)
    x = g.mix((g.at(spark, 0, d), 1.0), (g.at(taser, 0.01, d), 0.7), (g.at(gust, 0.0, d), 0.8))
    return [finish(g.reverb(x, 0.3, 0.35, 0.15))]


def r_beam_loop(L):
    arc = denoise_gate(L("arc_hv"), -34.0)
    e = envelope(arc, 8)
    w = g.n_samples(1.2)
    cs = np.cumsum(np.concatenate([[0.0], e]))
    best = int(np.argmax(cs[w:] - cs[:-w])) if len(e) > w else 0  # the densest 1.2 s of arcing
    x = arc[best:best + w]
    hum = g.bandpass(0.5 * g.osc_from_freq(np.full(len(x), 55.0), "saw") + 0.5 * g.osc_from_freq(np.full(len(x), 110.0), "square"), 60, 4000)
    y = g.mix((x, 1.0), (hum, 0.3))
    return [g.norm(g.loopable(y, 0.1), 0.55)]


def r_formation(L):
    d = 3.0
    gong = clip(L("gong_boss"), dur=3.0, fade_out=1.0)
    runes = g.crackle(d, 40, 3000, 10000, 0.004) * g.env(d, 0.3, 1.0, 0.2, 0.6)
    drone = g.tonal_swell(65.4, d, 7, 0.01, 150, 1800) * g.env(d, 0.6, 0.6, 0.6, 0.8)
    x = g.mix((g.at(gong, 0, d), 1.0), (runes, 0.08), (g.saturate(drone, 1.5), 0.15))
    return [finish(g.reverb(x, 1.0, 0.6, 0.3), 0.85)]


def r_breakthrough(L):
    d = 4.6
    build = g.sweep(g.noise(1.0), 150, 5000, q=3, shape=0.7) * g.ramp(1.0, 0, 1, 2.5)
    crack = clip(L("boom_firework_sharp"), dur=0.8)
    thunder = clip(L("thunder_strike3s"), dur=3.4, fade_out=0.8)
    gong = clip(L("gong_paiste"), dur=3.4, fade_out=1.0)
    pad = g.tonal_swell(146.8, 3.0, 8, 0.009, 200, 2500) * g.env(3.0, 0.5, 1.0, 0.5, 1.2)
    x = g.mix((g.at(build, 0.0, d), 0.8), (g.at(crack, 1.0, d), 1.0), (g.at(thunder, 1.0, d), 0.9), (g.at(gong, 1.05, d), 0.9),
              (g.at(g.saturate(pad, 1.5), 1.2, d), 0.3), (g.at(g.sub_boom(1.5, 120, 26, 0.5), 1.0, d), 0.4))
    return [finish(g.glue(g.reverb(x, 1.4, 0.7, 0.3)))]


def r_heaven_sword_fall(L):
    d = 2.6
    tt = g.t(d)
    wind = pitch(L("wind_woosh"), 0.8)
    wind = wind[onset(wind, -18):]
    wind = wind[: g.n_samples(d)] if len(wind) >= g.n_samples(d) else np.concatenate([wind, np.zeros(g.n_samples(d) - len(wind))])
    wind = wind * (tt / d) ** 1.2
    fw = 2600 * (450 / 2600) ** ((tt / d) ** 1.4) * (1 + 0.01 * np.sin(2 * np.pi * 7 * tt))
    whistle = (g.osc_from_freq(fw) + 0.5 * g.osc_from_freq(fw * 2) + 0.25 * g.osc_from_freq(fw * 3.01)) * g.env(d, 0.3, 1.2, 0.7, 0.3)
    whistle = g.lowpass(whistle, 6000)
    synth_wind = g.sweep(g.noise(d), 400, 3500, q=1.5, shape=0.9) * (tt / d) ** 1.3
    rum = g.brown(d) * (tt / d) ** 2.5
    x = g.mix((wind, 1.0), (whistle, 0.35), (synth_wind, 0.6), (rum, 1.2))
    return [g.norm(g.fade(g.highpass(x, 30, 2), 0.05, 0.02), 0.9)]


def r_heaven_hand_summon(L):
    """Formation opening: two real gongs (deep boss gong + Paiste) pitched down, a real wind gust
    swelling underneath, synth harmonic swell and rumble."""
    d = 4.5
    tt = g.t(d)
    gong1 = pitch(clip(L("gong_boss"), dur=4.2, fade_out=1.2), 0.7)
    gong2 = pitch(clip(L("gong_paiste"), dur=3.6, fade_out=1.0), 0.85)
    wind = g.at(clip(L("wind_gust"), dur=3.0, fade_in=0.4, fade_out=0.6), 0, 3.0) * g.ramp(3.0, 0.2, 1.0, 1.5)
    swell = g.tonal_swell(110, d, 8, 0.008, 150, 2400) * g.env(d, 1.6, 1.6, 0.8, 1.0)
    rum = g.brown(d) * g.env(d, 2.0, 1.5, 1.0, 0.8)
    x = g.mix((g.at(gong1, 0.0, d), 1.0), (g.at(gong2, 0.15, d), 0.6), (g.at(wind, 1.2, d), 0.8),
              (g.at(g.saturate(swell, 1.4), 0, d), 0.4), (rum, 1.3))
    return [finish(g.glue(g.reverb(x, 1.4, 0.7, 0.3)))]


def r_heaven_hand_pressure(L):
    """Pressure of the descending palm: a real thunder roll low-passed to a groan + synth sub throb."""
    d = 2.4
    tt = g.t(d)
    roll = g.lowpass(g.at(clip(L("thunder_josh"), dur=d, fade_in=0.3, fade_out=0.6), 0, d), 260)
    sub = g.sine(38, d) * (0.7 + 0.3 * np.sin(2 * np.pi * 2.3 * tt)) * g.env(d, 0.4, 0.8, 0.8, 0.8)
    groan = g.resonant(g.brown(d), 140, 6.0) * g.env(d, 0.5, 0.8, 0.7, 0.8)
    x = g.mix((roll, 1.0), (g.saturate(sub, 1.6), 0.9), (groan, 0.8))
    return [g.norm(g.fade(g.highpass(x, 24, 2), 0.1, 0.3), 0.9)]


def r_heaven_hand_slam(L):
    """The slam: real bomb boom + falling rock wall + extreme thunder, over the synth impact/sub drop."""
    d = 5.5
    boom = clip(L("boom_bag"), dur=2.8, fade_out=1.0)
    rock = clip(L("rock_wall"), dur=3.2, fade_out=1.0)
    thunder = clip(L("thunder_extreme"), dur=5.0, fade_out=1.5)
    hit = g.impact(2.5, 105, 18, 85, crack=1.4, weight=2.4)
    drop = g.saturate(g.glide(80, 20, 1.6, 0.4) * g.expdecay(1.6, 0.6), 2.0)
    rum = g.brown(d) * g.expdecay(d, 2.2, delay=0.03)
    x = g.mix((g.at(boom, 0, d), 1.0), (g.at(hit, 0, d), 0.7), (g.at(drop, 0, d), 0.8), (g.at(rock, 0.06, d), 0.8),
              (g.at(thunder, 0.1, d), 0.9), (rum, 1.8))
    return [finish(g.glue(g.reverb(x, 1.6, 0.7, 0.3)))]


def r_heaven_hand_rumble(L):
    """Distant rumble reaching far viewers after the slam: a real thunder roll low-passed hard,
    swelling in (no click) and dying over 4 s, with a 28 Hz sub."""
    d = 4.2
    tt = g.t(d)
    swell = g.env(d, 0.25, 1.0, 0.8, 2.4)
    roll = g.lowpass(g.at(clip(L("thunder_netaj"), dur=d, fade_in=0.2, fade_out=1.2), 0, d), 200) * swell
    sub = g.sine(28, d) * (0.8 + 0.2 * np.sin(2 * np.pi * 1.7 * tt)) * swell
    x = g.mix((roll, 1.4), (g.saturate(sub, 1.5), 0.8), (g.lowpass(g.brown(d, hp=12.0), 220) * swell, 1.0))
    return [g.norm(g.fade(g.highpass(x, 20, 2), 0.15, 1.0), 0.9)]


def r_learn_skill(L):
    """A real plucked zither string (kayageum A2 = 110 Hz) re-pitched into a rising pentatonic run."""
    d = 2.2
    src = L("zither_a2")
    src = src[onset(src, -30):]
    x = g.zeros(d)
    for i, ratio in enumerate((2.0, 2.2449, 2.5198, 3.0, 4.0)):  # A3 B3 C#4 E4 A4
        pl = pitch(src, ratio)
        pl = g.fade(pl[: g.n_samples(1.4)], 0.002, 0.5) * (0.95 - 0.1 * i)
        x += g.at(pl, 0.1 * i, d)
    swell = g.tonal_swell(110.0, d, 6, 0.008, 150, 1800) * g.env(d, 0.4, 0.6, 0.3, 0.6)
    air = g.breath(d, 500, 3000) * g.env(d, 0.3, 0.6, 0.3, 0.5)
    x = g.mix((x, 1.4), (g.saturate(swell, 1.3), 0.1), (air, 0.15))
    return [finish(g.reverb(x, 0.7, 0.55, 0.3))]


# ------------------------------------------------------------------ hit / feedback layer (1.3.7)

def _punch(L, key, dur=0.35):
    x = clip(L(key), dur=dur, fade_out=0.12)
    return g.highpass(x, 60, 2)


def _sub_thump(d=0.35, f0=110, f1=45, dec=0.09):
    return g.saturate(g.glide(f0, f1, d, dec * 2) * g.expdecay(d, dec), 1.6)


def r_hit_slash(L):
    """Blade meeting flesh: the real sword-body hit + a punch for weight + a bright steel tick."""
    res = []
    for body_key, punch_key, ratio in (("hit_sword_body", "punch_newage", 1.0), ("hit_sword_impact", "punch_taylor", 1.1), ("hit_sword_body", "punch_jew", 0.9)):
        d = 0.6
        body = pitch(clip(L(body_key), dur=0.5, fade_out=0.15), ratio)
        pun = _punch(L, punch_key)
        tick = g.highpass(pitch(_steel(L, "steel_slide", 0.25), 1.4), 3000) * 0.5
        x = g.mix((g.at(body, 0, d), 1.0), (g.at(pun, 0.0, d), 0.7), (g.at(tick, 0.0, d), 0.35), (g.at(_sub_thump(), 0.0, d), 0.5))
        res.append(finish(g.reverb(x, 0.25, 0.3, 0.12)))
    return res


def r_hit_blunt(L):
    """Palm / rock / wind body blows: real punches with a metallic thud or fast collision underneath."""
    res = []
    for punch_key, under_key, ratio in (("punch_insanity", "collision_fast", 1.0), ("punch_taylor", "thud_metal", 0.85), ("punch_jew", "collision_fast", 0.95)):
        d = 0.55
        pun = pitch(_punch(L, punch_key, 0.4), ratio)
        under = g.lowpass(clip(L(under_key), dur=0.45, fade_out=0.2), 900)
        x = g.mix((g.at(pun, 0, d), 1.0), (g.at(under, 0.005, d), 0.6), (g.at(_sub_thump(0.4, 120, 40, 0.11), 0.0, d), 0.8))
        res.append(finish(g.reverb(x, 0.3, 0.3, 0.1)))
    return res


def r_hit_fire(L):
    """Flame claw raking a body: punch + a snap of the real fire flare + ember hiss."""
    res = []
    for punch_key, ratio in (("punch_newage", 1.0), ("punch_insanity", 1.15)):
        d = 0.7
        pun = _punch(L, punch_key)
        flare = g.highpass(clip(L("fire_flare"), dur=0.5, fade_out=0.25), 400) * g.expdecay(0.5, 0.2)
        hiss = g.crackle(0.6, 60, 1500, 8000, 0.003) * g.expdecay(0.6, 0.25)
        x = g.mix((g.at(pun, 0, d), 1.0), (g.at(flare, 0.01, d), 0.9), (g.at(hiss, 0.02, d), 0.35), (g.at(_sub_thump(), 0.0, d), 0.5))
        res.append(finish(g.reverb(x, 0.3, 0.35, 0.12)))
    return res


def r_hit_ice(L):
    """Ice shard biting: glass impact + ice crack + a cold ring, not the big shatter."""
    res = []
    for ice_key, ratio in (("ice_step", 1.3), ("ice_piezo", 1.1)):
        d = 0.6
        glass = pitch(clip(L("glass_impact"), dur=0.35, fade_out=0.15), ratio)
        crack = clip(L(ice_key), dur=0.4, fade_out=0.2)
        ring = g.resonant(g.noise(0.5), 4200 * ratio, 40) * g.expdecay(0.5, 0.15) * 0.3
        x = g.mix((g.at(glass, 0, d), 1.0), (g.at(crack, 0.01, d), 0.8), (g.at(ring, 0.0, d), 0.3), (g.at(_sub_thump(0.3, 100, 50, 0.07), 0.0, d), 0.4))
        res.append(finish(g.reverb(x, 0.35, 0.4, 0.15)))
    return res


def r_hit_shock(L):
    """Lightning striking a body: spark snap + taser bite + punch."""
    res = []
    for punch_key, ratio in (("punch_taylor", 1.0), ("punch_newage", 1.2)):
        d = 0.55
        spark = clip(L("spark_zapper"), dur=0.12)
        taser = pitch(clip(L("arc_taser"), dur=0.3, fade_out=0.15), ratio)
        pun = _punch(L, punch_key)
        x = g.mix((g.at(spark, 0, d), 0.6), (g.at(taser, 0.005, d), 0.7), (g.at(pun, 0.0, d), 1.0), (g.at(_sub_thump(), 0.0, d), 0.6))
        res.append(finish(g.reverb(x, 0.25, 0.3, 0.1)))
    return res


def r_dragon_flyby(L):
    """A wind dragon rushing past: the big gust at natural pitch, a low synth throat, and a tail."""
    res = []
    for key, ratio in (("wind_gust", 0.9), ("wind_woosh2", 0.8)):
        x = pitch(L(key), ratio)
        x = g.bandpass(shape(x[onset(x, -20):], 0.15, 0.3, 0.6), 150, 7000)
        d = len(x) / SR
        edge = g.whoosh(d, 300, 1800, 400, q=4, peak_at=0.4)[: len(x)]
        throat = g.saturate(g.osc_from_freq(70 + 20 * np.sin(2 * np.pi * 3 * g.t(d)), "saw"), 2.0) * g.env(d, 0.2, 0.4, 0.5, 0.2)
        throat = g.lowpass(throat, 500)[: len(x)]
        res.append(finish(g.mix((x, 1.0), (edge, 0.5), (throat, 0.12))))
    return res


def r_cast_qi(L):
    """Qi release that opens every cast: the real gust pitched up and shaped short + synth sparkle."""
    res = []
    for key, ratio in (("wind_woosh", 2.4), ("wind_gust", 2.8), ("wind_woosh2", 2.0)):
        x = pitch(L(key), ratio)
        x = shape(x[onset(x, -18):], 0.03, 0.06, 0.4)
        d = max(len(x) / SR, 0.5)
        spark = g.crackle(d, 80, 4000, 13000, 0.0015) * g.expdecay(d, 0.2, delay=0.02)
        tone = g.glide(1200, 2200, d, 0.6) * g.expdecay(d, 0.12) * 0.25
        res.append(finish(g.reverb(g.mix((g.at(x, 0, d), 1.0), (spark, 0.35), (tone, 0.2)), 0.4, 0.35, 0.18)))
    return res


def r_riser(L):
    """Anticipation before the big channelled skills: a reversed real thunder swell under the
    synth riser, cut dead at the top."""
    d = 2.0
    th = clip(L("thunder_extreme"), dur=d, fade_out=0.02)[::-1].copy()
    th = th * (g.t(len(th) / SR) / d) ** 1.6
    synth = g.SOUNDS["skill/riser"]()[0]
    x = g.mix((g.at(th, 0, d), 1.0), (g.at(synth, 0, d), 0.8))
    return [g.norm(g.fade(g.highpass(x, 30, 2), 0.05, 0.005), 0.9)]


def r_palm_strike(L):
    """Vajra palm: the synthesised golden strike now sits on a real punch and a rock slap."""
    res = []
    for i, (punch_key, ratio) in enumerate((("punch_insanity", 0.9), ("punch_newage", 1.0))):
        synth = g.SOUNDS["skill/palm_strike"]()[i]
        d = len(synth) / SR
        pun = pitch(_punch(L, punch_key, 0.4), ratio)
        rock = g.lowpass(clip(L("rock_thrown"), dur=0.5, fade_out=0.25), 1200)
        x = g.mix((synth, 1.0), (g.at(pun, 0.0, d), 0.9), (g.at(rock, 0.01, d), 0.45))
        res.append(finish(x))
    return res


def r_shield_hit(L):
    """Something hitting the qi shield: synth fizz + real punch + metallic thud."""
    res = []
    for i, punch_key in enumerate(("punch_taylor", "punch_jew")):
        synth = g.SOUNDS["skill/shield_hit"]()[i]
        d = len(synth) / SR
        pun = _punch(L, punch_key)
        thud = g.lowpass(clip(L("thud_metal"), dur=0.4, fade_out=0.2), 1500)
        x = g.mix((synth, 1.0), (g.at(pun, 0.0, d), 0.6), (g.at(thud, 0.0, d), 0.5))
        res.append(finish(x))
    return res


def r_dragon_roar(L):
    """Dragon roar: the synthesised roar + a real recorded roar slowed down + thunder body."""
    res = []
    for i, (roar_key, ratio) in enumerate((("roar_mighty", 0.6), ("roar_beast", 0.85))):
        synth = g.SOUNDS["skill/dragon_roar"]()[i]
        d = len(synth) / SR
        roar = pitch(clip(L(roar_key), dur=1.6, fade_out=0.4), ratio)
        roar = g.bandpass(roar, 90, 3500)
        th = g.lowpass(clip(L("thunder_josh"), dur=d, fade_out=0.5), 600)
        x = g.mix((synth, 1.0), (g.at(roar, 0.05, d), 1.0), (g.at(th, 0.0, d), 0.6))
        res.append(finish(g.reverb(x, 0.8, 0.45, 0.2)))
    return res


def r_qi_gather(L):
    """Qi gathering: the synth shimmer with a reversed real gust drawing inward."""
    synth = g.SOUNDS["skill/qi_gather"]()[0]
    d = len(synth) / SR
    gust = clip(L("wind_gust"), dur=min(1.4, d), fade_out=0.01)[::-1].copy()
    gust = g.bandpass(gust, 300, 5000)
    x = g.mix((synth, 1.0), (g.at(gust, max(0.0, d - len(gust) / SR - 0.05), d), 0.7))
    return [finish(x)]


def r_void_drain(L):
    """Devouring vortex pull: synth drone + reversed thunder rolling in."""
    synth = g.SOUNDS["skill/void_drain"]()[0]
    d = len(synth) / SR
    th = clip(L("thunder_netaj"), dur=d, fade_out=0.01)[::-1].copy()
    th = g.lowpass(th, 900) * (g.t(len(th) / SR) / d) ** 1.2
    return [finish(g.mix((synth, 1.0), (g.at(th, 0, d), 0.6)))]


def r_void_collapse(L):
    """Vortex collapse: synth implosion + firework explosion + reversed thunder swallow."""
    synth = g.SOUNDS["skill/void_collapse"]()[0]
    d = len(synth) / SR
    boom = clip(L("boom_firework_sharp"), dur=1.2, fade_out=0.4)
    th = clip(L("thunder_josh"), dur=0.7, fade_out=0.01)[::-1].copy() * (g.t(0.7) / 0.7) ** 2
    x = g.mix((synth, 1.0), (g.at(boom, 0.7, d), 0.9), (g.at(th, 0.0, d), 0.6))
    return [finish(x)]


def r_sub_drop(L):
    """Cinematic sub-drop: synth drop + the real bomb's low body."""
    synth = g.SOUNDS["skill/sub_drop"]()[0]
    d = len(synth) / SR
    bag = g.lowpass(clip(L("boom_bag"), dur=0.9, fade_out=0.3), 220)
    return [finish(g.mix((synth, 1.0), (g.at(bag, 0.0, d), 0.8)))]


def r_golden_body(L):
    """Golden body: the synth bell/shell with a real gong strike ringing behind it."""
    synth = g.SOUNDS["skill/golden_body"]()[0]
    d = len(synth) / SR
    gong = pitch(clip(L("gong_di"), dur=min(2.4, d), fade_out=0.6), 1.3)
    thud = clip(L("thud_metal"), dur=0.4, fade_out=0.2)
    return [finish(g.mix((synth, 1.0), (g.at(gong, 0.02, d), 0.5), (g.at(thud, 0.0, d), 0.4)))]


RECIPES = {
    "skill/hit_slash": r_hit_slash,
    "skill/hit_blunt": r_hit_blunt,
    "skill/hit_fire": r_hit_fire,
    "skill/hit_ice": r_hit_ice,
    "skill/hit_shock": r_hit_shock,
    "skill/dragon_flyby": r_dragon_flyby,
    "skill/cast_qi": r_cast_qi,
    "skill/riser": r_riser,
    "skill/sub_drop": r_sub_drop,
    "skill/palm_strike": r_palm_strike,
    "skill/shield_hit": r_shield_hit,
    "skill/dragon_roar": r_dragon_roar,
    "skill/qi_gather": r_qi_gather,
    "skill/void_drain": r_void_drain,
    "skill/void_collapse": r_void_collapse,
    "skill/golden_body": r_golden_body,
    "skill/thunder_strike": r_thunder_strike,
    "skill/fire_whoosh": r_fire_whoosh,
    "skill/fire_explosion": r_fire_explosion,
    "skill/heaven_sword_impact": r_heaven_sword_impact,
    "skill/heaven_hand_summon": r_heaven_hand_summon,
    "skill/heaven_hand_pressure": r_heaven_hand_pressure,
    "skill/heaven_hand_slam": r_heaven_hand_slam,
    "skill/heaven_hand_rumble": r_heaven_hand_rumble,
    "skill/sword_qi": r_sword_qi,
    "skill/sword_launch": r_sword_launch,
    "skill/wind_slash": r_wind_slash,
    "skill/sword_flight": r_sword_flight,
    "skill/ice_cast": r_ice_cast,
    "skill/ice_shatter": r_ice_shatter,
    "skill/freeze_field": r_freeze_field,
    "skill/earth_quake": r_earth_quake,
    "skill/thunder_charge": r_thunder_charge,
    "skill/lightning_step": r_lightning_step,
    "skill/beam_loop": r_beam_loop,
    "skill/formation": r_formation,
    "skill/breakthrough": r_breakthrough,
    "skill/heaven_sword_fall": r_heaven_sword_fall,
    "item/learn_skill": r_learn_skill,
}

# Which sources each recipe uses (for the credits file and the report).
USES = {}


def write_credits(used):
    lines = ["# Sound credits", "",
             "Real-world recordings used by Celestial Arts. All of them are published on Freesound under the",
             "**Creative Commons 0** (public domain) dedication; they were trimmed, filtered, layered and pitched",
             "by `tools/fetch_real_sounds.py`. Attribution is not required by CC0 but is given with thanks.", "",
             "| Freesound ID | Title | Author | Used in |", "|---|---|---|---|"]
    for key in sorted(SOURCES, key=lambda k: SOURCES[k][0]):
        fsid, uid, title, author = SOURCES[key]
        events = sorted(e for e, ks in used.items() if key in ks)
        if not events:
            continue
        lines.append("| [%d](%s) | %s | %s | %s |" % (fsid, page_url(fsid, author), title.replace("|", "/"), author, ", ".join(e.replace("skill/", "").replace("item/", "") for e in events)))
    lines += ["", "Everything not listed here is synthesised from scratch by `tools/gen_sounds.py`.", ""]
    with open(CREDITS, "w") as f:
        f.write("\n".join(lines))


def main():
    do_analyse = "--analyse" in sys.argv
    offline = "--offline" in sys.argv
    os.makedirs(OUT, exist_ok=True)
    for root, _, fs in os.walk(OUT):
        for f in fs:
            if f.endswith(".ogg"):
                os.remove(os.path.join(root, f))
    manifest = {}
    used = {}
    failures = []
    for name, fn in RECIPES.items():
        keys = set()

        def L(key, _keys=keys):
            _keys.add(key)
            return load(key, offline)

        try:
            variants = fn(L)
        except Exception as e:  # keep going: the synth fallback covers this event
            print("FAILED %s: %r" % (name, e))
            failures.append((name, repr(e)))
            continue
        used[name] = keys
        paths = []
        for i, x in enumerate(variants):
            p = name if len(variants) == 1 else "%s_%d" % (name, i + 1)
            full = os.path.join(OUT, p + ".ogg")
            os.makedirs(os.path.dirname(full), exist_ok=True)
            x = np.clip(g.master(x, p), -1, 1).astype(np.float32)
            sf.write(full, x, SR, format="OGG", subtype="VORBIS")
            paths.append("real/" + p)
            print("%-32s %.2fs  <- %s" % (p, len(x) / SR, ", ".join(sorted(keys))))
            if do_analyse:
                g.analyse(p, x.astype(np.float64))
        manifest[name] = paths
    with open(g.REAL_MANIFEST, "w") as f:
        json.dump(manifest, f, indent="\t")
        f.write("\n")
    write_credits(used)
    g.build_sounds_json()
    if failures:
        print("\n%d recipe(s) failed:" % len(failures))
        for n, e in failures:
            print("  ", n, e)
        sys.exit(1)


if __name__ == "__main__":
    main()
