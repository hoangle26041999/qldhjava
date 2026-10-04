#!/usr/bin/env python3
"""Dedup m2 repo jars by latest version, write cp.txt (no newlines)."""
import os, re, sys
from pathlib import Path

m2 = Path(r"C:\Users\PC\.m2\repository")
exclude = ('sources.jar', 'javadoc.jar', 'junit', 'mockito', 'hamcrest', 'maven-', 'plexus-', 'aether-')

jars_by_artifact = {}
for jar in m2.rglob("*.jar"):
    name = jar.name
    if any(x in name for x in exclude):
        continue
    parts = str(jar).split(os.sep)
    # path = ...\group\artifact\version\file.jar
    artifact_id = parts[-2]  # artifact name
    group_id = parts[-3]     # group
    version = parts[-1]      # version-xxx.jar
    # extract version
    m = re.match(r'(.+?)-(\d+(?:\.\d+){1,3}(?:[\.\-][^.]+)*)\.jar$', name)
    if not m:
        continue
    artifact_name = m.group(1)
    version_str = m.group(2)
    key = f"{group_id}/{artifact_name}"
    ver_tuple = tuple(int(x) if x.isdigit() else x for x in re.findall(r'\d+|[A-Za-z]+', version_str))
    if key not in jars_by_artifact or ver_tuple > jars_by_artifact[key][1]:
        jars_by_artifact[key] = (jar, ver_tuple)

# Output single line with semicolons
out_path = Path(r"D:\LUMI-HOME\qldhjava\build-cp.txt")
with open(out_path, 'w', encoding='ascii') as f:
    parts = [str(p[0]) for p in jars_by_artifact.values()]
    parts.append('target\\classes')
    f.write(';'.join(parts))
print(f"Wrote {len(parts)} entries to {out_path}")