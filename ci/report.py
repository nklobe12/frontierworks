#!/usr/bin/env python3
"""Condenses CI logs into ci-report/summary.md (+ trimmed logs) for the ci-results branch."""
import os
import re
import shutil

OUT = 'ci-report'
os.makedirs(OUT, exist_ok=True)


def read(path):
    try:
        with open(path, errors='replace') as f:
            return f.read()
    except OSError:
        return None


def trim_copy(src, dst, max_bytes=4_000_000):
    text = read(src)
    if text is None:
        return
    if len(text) > max_bytes:
        text = text[:max_bytes // 4] + '\n\n[... trimmed ...]\n\n' + text[-3 * max_bytes // 4:]
    with open(os.path.join(OUT, dst), 'w') as f:
        f.write(text)


lines = ['# Lodestar CI report', '', 'commit: %s  run: %s' % (os.environ.get('GITHUB_SHA', '?'), os.environ.get('GITHUB_RUN_ID', '?')), '']

compile_log = read('compile.log')
if compile_log is None:
    lines += ['## Compile', 'not run', '']
else:
    errs = []
    cl = compile_log.splitlines()
    for i, l in enumerate(cl):
        if re.search(r'\.java:\d+: error:', l):
            errs.append('\n'.join(cl[i:i + 3]))
    ok = 'BUILD SUCCESSFUL' in compile_log
    lines += ['## Compile: %s (%d errors)' % ('OK' if ok else 'FAILED', len(errs)), '']
    if errs:
        lines += ['```'] + errs[:400] + ['```', '']
    elif not ok:
        lines += ['```'] + cl[-80:] + ['```', '']

server = read('server/server.log')
if server is not None:
    sl = server.splitlines()
    st = [l for l in sl if 'LSSELFTEST' in l]
    done = [l for l in st if 'DONE' in l]
    lines += ['## Server self-test: %s' % (done[-1].split('LSSELFTEST')[-1].strip() if done else 'DID NOT FINISH'), '']
    fails = [l for l in st if 'FAIL' in l]
    lines += ['### Self-test failures (%d)' % len(fails), '```'] + fails[:300] + ['```', '']
    errors = []
    for i, l in enumerate(sl):
        if '/ERROR]' in l or '/FATAL]' in l or 'Exception' in l and not l.startswith('\t'):
            block = [l] + [x for x in sl[i + 1:i + 12] if x.startswith('\t') or x.startswith('Caused by')][:10]
            errors.append('\n'.join(block))
    seen = set()
    uniq = []
    for e in errors:
        key = re.sub(r'^\[[^\]]*\] ', '', e.splitlines()[0])[:200]
        if key not in seen:
            seen.add(key)
            uniq.append(e)
    lines += ['### Server errors (%d unique)' % len(uniq), '```'] + uniq[:250] + ['```', '']
    warn = [l for l in sl if '/WARN]' in l and ('ls_' in l or 'lodestar' in l.lower())]
    lines += ['### Lodestar warnings (%d)' % len(warn), '```'] + warn[:200] + ['```', '']

client = read('client.log')
if client is not None:
    cl = client.splitlines()
    reached = any('LSSELFTEST CLIENT reached title screen' in l for l in cl)
    lines += ['## Client boot: %s' % ('reached title screen' if reached else 'DID NOT REACH TITLE SCREEN'), '']
    pats = ('Unable to load model', 'Missing textures', 'Exception loading blockstate', 'Unable to resolve texture',
            'missing model', 'Exception caught', 'Failed to load', 'Couldn\'t', 'Unknown', 'ERROR]', 'FATAL]', 'Exception')
    probs = [l for l in cl if any(p in l for p in pats)]
    lines += ['### Client problems (%d)' % len(probs), '```'] + probs[:400] + ['```', '']

show = read('showcase.log')
if show is not None:
    sh = show.splitlines()
    done = any('LSSHOWCASE DONE' in l for l in sh)
    shots = sorted(os.listdir(os.path.join(OUT, 'screenshots'))) if os.path.isdir(os.path.join(OUT, 'screenshots')) else []
    lines += ['## Showcase: %s, %d screenshots' % ('done' if done else 'DID NOT FINISH', len(shots)), '']
    pats = ('Unable to load model', 'Missing textures', 'Exception', 'ERROR]', 'FATAL]', 'LSSHOWCASE')
    probs = [l for l in sh if any(p in l for p in pats)]
    lines += ['### Showcase log (%d)' % len(probs), '```'] + probs[:300] + ['```', '']

with open(os.path.join(OUT, 'summary.md'), 'w') as f:
    f.write('\n'.join(lines) + '\n')
trim_copy('compile.log', 'compile.log')
trim_copy('server/server.log', 'server.log')
trim_copy('client.log', 'client.log')
trim_copy('showcase.log', 'showcase.log')
trim_copy('jars.txt', 'jars.txt')
trim_copy('ci-gen.log', 'gen.log')
print('\n'.join(lines[:60]))
