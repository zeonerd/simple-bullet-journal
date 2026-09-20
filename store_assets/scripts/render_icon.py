from PIL import Image, ImageDraw

SCALE = 512 / 108
BG = "#2D3748"
FG = "#F8F9FA"

img = Image.new("RGB", (512, 512), BG)
draw = ImageDraw.Draw(img)

# Bullet dot: center (35,54) r=8.5 in 108-space
cx, cy, r = 35 * SCALE, 54 * SCALE, 8.5 * SCALE
draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=FG)

# Underbar: line from (54,60) to (78,60), stroke width 6.5, round cap
x1, y1, x2, y2 = 54 * SCALE, 60 * SCALE, 78 * SCALE, 60 * SCALE
w = 6.5 * SCALE
draw.line([x1, y1, x2, y2], fill=FG, width=int(round(w)))
cap_r = w / 2
draw.ellipse([x1 - cap_r, y1 - cap_r, x1 + cap_r, y1 + cap_r], fill=FG)
draw.ellipse([x2 - cap_r, y2 - cap_r, x2 + cap_r, y2 + cap_r], fill=FG)

img.save("../hi_res_icon_512.png")
print("saved", img.size)
