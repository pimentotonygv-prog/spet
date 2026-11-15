from pathlib import Path
import re, math
source=Path('app/src/main/java/br/com/passo/MainActivity.java').read_text(encoding='utf-8')
def colors(name):
    return [int(v,16)&0xffffff for v in re.search(name+r'=\{([^}]+)\}',source)[1].split(',')]
def luminance(color):
    rgb=[((color>>s)&255)/255 for s in (16,8,0)]
    linear=[v/12.92 if v<=0.04045 else ((v+0.055)/1.055)**2.4 for v in rgb]
    return sum(a*b for a,b in zip(linear,[0.2126,0.7152,0.0722]))
def contrast(a,b):
    x,y=sorted([luminance(a),luminance(b)])
    return (y+0.05)/(x+0.05)
ratios=[]
for name,foreground in [('LIGHT',0xffffff),('DARK',0x152019)]:
    for color in colors(name):
        ratio=contrast(color,foreground)
        assert ratio>=4.5,(name,hex(color),ratio)
        ratios.append(ratio)
for a,b in [(0x1b2923,0xf4f8f2),(0x4f6157,0xf4f8f2),(0xedf4ef,0x121a17),(0xb8c5be,0x121a17)]:
    assert contrast(a,b)>=4.5
# A imagem completa cabe dentro do círculo seguro do ícone adaptativo.
assert math.sqrt(2)*180 < 432*66/108
print('8 perfis/temas: contraste mínimo dos botões %.2f:1; imagem inteira dentro da área segura.'%min(ratios))
