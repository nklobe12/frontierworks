#!/usr/bin/env python3
"""Generates every Frontierworks mod from the specs in tools/mods/.

Usage: python3 tools/build.py            (writes mods/<id>/gen, mods/<id>/resources, generated/merged, mods.txt)
"""
import importlib
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from gen import fw  # noqa: E402
from mods import ORDER  # noqa: E402


def main():
    t0 = time.time()
    for name in ORDER:
        mod = importlib.import_module('mods.' + name)
        mod.build()
    mods = fw.build_all()
    files = sum(len(m.files) for m in mods)
    print('generated %d mods, %d resource files in %.1fs' % (len(mods), files, time.time() - t0))


if __name__ == '__main__':
    main()
