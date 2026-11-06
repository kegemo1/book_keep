# -*- coding: utf-8 -*-
"""检查 WXML 里用到但样式表里没定义的 class（重构后防漏）"""
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


# 收集所有 wxss 里定义的 class 名
defined = set()
wxss_files = []
for dirpath, _, files in os.walk(ROOT):
    if "node_modules" in dirpath or ".git" in dirpath:
        continue
    for fn in files:
        if fn.endswith(".wxss"):
            wxss_files.append(os.path.join(dirpath, fn))
for p in wxss_files:
    for m in re.finditer(r"\.([a-zA-Z][\w-]*)", read(p)):
        defined.add(m.group(1))

# 收集每个 wxml 使用的 class
problems = []
for dirpath, _, files in os.walk(ROOT):
    if "node_modules" in dirpath or ".git" in dirpath:
        continue
    for fn in files:
        if not fn.endswith(".wxml"):
            continue
        p = os.path.join(dirpath, fn)
        src = read(p)
        used = set()
        for m in re.finditer(r'(?<![\w-])class="([^"]*)"', src):
            raw = m.group(1)
            # 去掉 {{...}} 之外的静态 token
            static = re.sub(r"\{\{.*?\}\}", " ", raw)
            for t in static.split():
                used.add(t)
            # 表达式里的字符串字面量（三目里的类名）
            for expr in re.findall(r"\{\{(.*?)\}\}", raw):
                for lit in re.findall(r"'([a-zA-Z][\w-]*)'", expr):
                    used.add(lit)
        missing = sorted(c for c in used if c not in defined)
        if missing:
            problems.append((p, missing))

if not problems:
    print("OK：所有 class 都有对应样式")
else:
    for p, missing in problems:
        print("%s -> %s" % (p.replace(ROOT, ""), ", ".join(missing)))
