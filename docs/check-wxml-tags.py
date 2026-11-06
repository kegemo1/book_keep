# -*- coding: utf-8 -*-
"""粗略检查 WXML 标签闭合（改版手写完模板后自查用）"""
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
VOID = {"input", "image", "switch", "slider"}

bad = []
for dirpath, _, files in os.walk(ROOT):
    if "node_modules" in dirpath or ".git" in dirpath:
        continue
    for fn in files:
        if not fn.endswith(".wxml"):
            continue
        p = os.path.join(dirpath, fn)
        src = open(p, encoding="utf-8").read()
        src = re.sub(r"<!--.*?-->", "", src, flags=re.S)
        stack = []
        for m in re.finditer(r"<(/?)([a-zA-Z][\w-]*)([^>]*?)(/?)>", src):
            closing, tag, attrs, self_close = m.groups()
            if tag in ("wxs",):
                continue
            if closing:
                if not stack:
                    bad.append("%s: 多余的 </%s>" % (p, tag))
                elif stack[-1][0] != tag:
                    bad.append("%s: </%s> 与 <%s> 不匹配" % (p, tag, stack[-1][0]))
                    stack.pop()
                else:
                    stack.pop()
            else:
                if self_close or tag in VOID:
                    continue
                stack.append((tag, m.start()))
        for tag, pos in stack:
            bad.append("%s: <%s> 未闭合" % (p, tag))

if bad:
    print("\n".join(bad))
else:
    print("OK：标签闭合正常")
