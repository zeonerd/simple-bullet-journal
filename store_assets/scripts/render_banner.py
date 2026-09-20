from PIL import Image, ImageDraw, ImageFont

W, H = 1024, 500

# Colors from app's dark chalkboard theme
DARK_PAPER = (26, 29, 32)       # #1A1D20
RULED_LINE = (45, 55, 72)       # #2D3748
MARGIN_LINE = (229, 115, 115)   # #E57373 (Red Chalk)
NOTE_TEXT = (232, 234, 239)     # #E8EAEF
PRIORITY = (255, 213, 79)       # #FFD54F

img = Image.new("RGB", (W, H), DARK_PAPER)
draw = ImageDraw.Draw(img)

# Vertical red margin line (notebook style), positioned left third
margin_x = 180
draw.line([(margin_x, 0), (margin_x, H)], fill=MARGIN_LINE, width=4)

# Horizontal ruled lines
line_height = 84
y = line_height
while y < H:
    draw.line([(0, y), (W, y)], fill=RULED_LINE, width=2)
    y += line_height

# Chalk bullet + underbar icon (scaled up), placed left of margin line
cx, cy, r = 90, 250, 22
draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=NOTE_TEXT)
lx1, ly1, lx2, ly2 = 130, 290, 170, 290
lw = 16
draw.line([lx1, ly1, lx2, ly2], fill=NOTE_TEXT, width=lw)
cap_r = lw / 2
draw.ellipse([lx1 - cap_r, ly1 - cap_r, lx1 + cap_r, ly1 + cap_r], fill=NOTE_TEXT)
draw.ellipse([lx2 - cap_r, ly2 - cap_r, lx2 + cap_r, ly2 + cap_r], fill=NOTE_TEXT)

# Title text
def load_font(size, bold=False):
    candidates = [
        "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
        "/System/Library/Fonts/AppleSDGothicNeo.ttc",
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
    ]
    for c in candidates:
        try:
            return ImageFont.truetype(c, size)
        except Exception:
            continue
    return ImageFont.load_default()

title_font = load_font(64, bold=True)
subtitle_font = load_font(30)

draw.text((margin_x + 40, 150), "Simple Bullet", font=title_font, fill=NOTE_TEXT)
draw.text((margin_x + 40, 225), "Journal", font=title_font, fill=NOTE_TEXT)
draw.text((margin_x + 40, 310), "줄공책 감성 그대로, 오늘 할 일을 기록하세요", font=subtitle_font, fill=(160, 174, 192))

# A small star accent (priority marker) near title
star_cx, star_cy, star_r = W - 90, 90, 26
import math
points = []
for i in range(10):
    angle = math.pi / 2 + i * math.pi / 5
    rad = star_r if i % 2 == 0 else star_r * 0.45
    points.append((star_cx + rad * math.cos(angle), star_cy - rad * math.sin(angle)))
draw.polygon(points, fill=PRIORITY)

img.save("../feature_graphic_1024x500.png")
print("saved", img.size)
