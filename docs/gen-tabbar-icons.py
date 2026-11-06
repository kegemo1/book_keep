# -*- coding: utf-8 -*-
"""
生成小程序 TabBar 线性图标（81x81 PNG，4 倍超采样抗锯齿）

设计约束：
- 线宽统一 3px（81 画布），圆角端点，与页面里 2rpx 分隔线、999rpx 胶囊的"细笔画"语言一致
- 常态 #A3A69F（暖灰），选中 #24352E（深松绿），与 app.json tabBar.color/selectedColor 对应
- 输出到 bookkeepingWhiz-mp/assets/tabbar/

用法：python gen-tabbar-icons.py <小程序根目录>
"""

import os
import sys

from PIL import Image, ImageDraw

S = 4                    # 超采样倍率
SIZE = 81                # 最终尺寸
W = SIZE * S
STROKE = 3 * S
NORMAL = (163, 166, 159, 255)
ACTIVE = (36, 53, 46, 255)


def canvas():
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def line(d, p1, p2, color):
    """带圆头的直线"""
    d.line([p1, p2], fill=color, width=STROKE)
    for p in (p1, p2):
        r = STROKE / 2
        d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=color)


def polyline(d, points, color):
    for i in range(len(points) - 1):
        line(d, points[i], points[i + 1], color)


def round_rect(d, box, radius, color):
    d.rounded_rectangle(box, radius=radius, outline=color, width=STROKE)


def circle(d, box, color):
    d.ellipse(box, outline=color, width=STROKE)


def icon_home(color):
    """工作台：2x2 圆角方块"""
    img, d = canvas()
    side, gap = 96, 32
    x0 = (W - (side * 2 + gap)) // 2
    y0 = x0
    for i in range(2):
        for j in range(2):
            x = x0 + j * (side + gap)
            y = y0 + i * (side + gap)
            round_rect(d, [x, y, x + side, y + side], 26, color)
    return img


def icon_clock(color):
    """工时：表盘 + 指针"""
    img, d = canvas()
    r = 104
    c = W // 2
    circle(d, [c - r, c - r, c + r, c + r], color)
    line(d, (c, c), (c, c - 56), color)
    line(d, (c, c), (c + 46, c + 26), color)
    return img


def icon_wallet(color):
    """记账：钱包 + 扣件"""
    img, d = canvas()
    round_rect(d, [46, 74, W - 46, W - 76], 34, color)
    # 上盖折线，暗示"钱夹开口"
    line(d, (46 + STROKE, 148), (W - 46 - STROKE, 148), color)
    # 扣件
    d.ellipse([W - 46 - 68, W // 2 + 2, W - 46 - 24, W // 2 + 46], fill=color)
    return img


def icon_folder(color):
    """项目：文件夹"""
    img, d = canvas()
    pts = [
        (52, W - 82),
        (52, 108),
        (138, 108),
        (172, 146),
        (W - 52, 146),
        (W - 52, W - 82),
        (52, W - 82),
    ]
    polyline(d, pts, color)
    d.rounded_rectangle([52 - STROKE / 2, W - 82 - STROKE / 2,
                         W - 52 + STROKE / 2, W - 82 + STROKE / 2],
                        radius=STROKE, fill=color)
    return img


def icon_user(color):
    """我的：头 + 肩"""
    img, d = canvas()
    r = 46
    c = W // 2
    head_cy = 116
    circle(d, [c - r, head_cy - r, c + r, head_cy + r], color)
    d.arc([78, 196, W - 78, W - 24], start=190, end=350, fill=color, width=STROKE)
    return img


ICONS = {
    "home": icon_home,
    "time": icon_clock,
    "wallet": icon_wallet,
    "folder": icon_folder,
    "user": icon_user,
}


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    out_dir = os.path.join(root, "assets", "tabbar")
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in ICONS.items():
        for suffix, color in (("", NORMAL), ("-on", ACTIVE)):
            img = fn(color).resize((SIZE, SIZE), Image.LANCZOS)
            path = os.path.join(out_dir, "%s%s.png" % (name, suffix))
            img.save(path)
            print("生成 %s" % path)


if __name__ == "__main__":
    main()
