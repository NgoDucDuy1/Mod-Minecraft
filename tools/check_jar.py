#!/usr/bin/env python3
"""Sanity-check the *production* jar (post-remap) - things the dev-environment tests cannot catch.

* every mixin config listed in fabric.mod.json exists and names a refmap that exists in the jar,
* every mixin class in that config has an entry in that refmap (otherwise the class was compiled in a
  source set whose refmap is a different file and its injectors will fail to resolve in production),
* every mixin class file is present,
* refmap targets are intermediary names (net/minecraft/class_...), i.e. the jar was actually remapped.

Usage: python3 tools/check_jar.py build/libs/celestialarts-<version>.jar
"""
import json
import sys
import zipfile


def main(path: str) -> int:
    errors = []
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        fmj = json.loads(jar.read("fabric.mod.json"))
        configs = [e if isinstance(e, str) else e["config"] for e in fmj.get("mixins", [])]
        if not configs:
            errors.append("fabric.mod.json lists no mixin configs")
        for cfg_name in configs:
            if cfg_name not in names:
                errors.append(f"mixin config {cfg_name} missing from jar")
                continue
            cfg = json.loads(jar.read(cfg_name))
            refmap_name = cfg.get("refmap")
            if not refmap_name:
                errors.append(f"{cfg_name}: no 'refmap' key (Loom did not process it - is it in the right source set?)")
                continue
            if refmap_name not in names:
                errors.append(f"{cfg_name}: refmap {refmap_name} missing from jar")
                continue
            mappings = json.loads(jar.read(refmap_name)).get("mappings", {})
            package = cfg["package"].replace(".", "/")
            count = 0
            for key in ("mixins", "client", "server"):
                for mixin in cfg.get(key, []):
                    count += 1
                    cls = f"{package}/{mixin.replace('.', '/')}"
                    if cls + ".class" not in names:
                        errors.append(f"{cfg_name}: mixin class {cls} missing from jar")
                    entry = mappings.get(cls)
                    if not entry:
                        errors.append(f"{cfg_name}: {cls} has no entry in {refmap_name} - its injectors will not resolve in production")
                        continue
                    for member, target in entry.items():
                        if not any(tag in target for tag in ("net/minecraft/class_", "method_", "field_")):
                            errors.append(f"{refmap_name}: {cls}#{member} -> {target} is not remapped to intermediary")
            print(f"{cfg_name}: refmap {refmap_name}, {count} mixins checked")
    for e in errors:
        print("ERROR:", e)
    print("jar check", "FAILED" if errors else "OK")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
