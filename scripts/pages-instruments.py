#!/usr/bin/env python3
"""Génère une page « Transposer pour … » par instrument transpositeur (docs/transposer-*.html).

Les intervalles sont ceux de core/music (Instruments.kt) et de docs/sources-instruments.md.
Usage : python3 scripts/pages-instruments.py [--check]
"""
import html, json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
SITE = "https://pilmil-studio.github.io/fredonne/"
e = lambda s: html.escape(s, quote=False)

# (fichier, nom, article « pour la/le/l' », degrés diatoniques, demi-tons, intervalle en toutes lettres, note sonore d'un do écrit, octave à préciser)
INSTR = [
    ("clarinette-si-bemol", "Clarinette en si♭", "la clarinette en si♭", 1, 2, "une seconde majeure (un ton)", "si♭", ""),
    ("trompette-si-bemol", "Trompette en si♭", "la trompette en si♭", 1, 2, "une seconde majeure (un ton)", "si♭", ""),
    ("saxophone-soprano", "Saxophone soprano", "le saxophone soprano", 1, 2, "une seconde majeure (un ton)", "si♭", ""),
    ("saxophone-alto", "Saxophone alto", "le saxophone alto", 5, 9, "une sixte majeure", "mi♭", ""),
    ("saxophone-tenor", "Saxophone ténor", "le saxophone ténor", 8, 14, "une neuvième majeure (une octave et un ton)", "si♭", " une octave plus bas"),
    ("saxophone-baryton", "Saxophone baryton", "le saxophone baryton", 12, 21, "une octave et une sixte majeure", "mi♭", " une octave plus bas"),
    ("cor-en-fa", "Cor en fa", "le cor en fa", 4, 7, "une quinte juste", "fa", ""),
    ("guitare", "Guitare, basse et contrebasse", "la guitare, la basse et la contrebasse", 7, 12, "une octave", "do", " une octave plus bas"),
]
STEPS = ["do", "ré", "mi", "fa", "sol", "la", "si"]
NAT = [0, 2, 4, 5, 7, 9, 11]
ACC = {-2: "𝄫", -1: "♭", 0: "", 1: "♯", 2: "𝄪"}
CHROM = [(0, 0), (0, 1), (1, 0), (2, -1), (2, 0), (3, 0), (3, 1), (4, 0), (5, -1), (5, 0), (6, -1), (6, 0)]
MAJ = ['do♭', 'sol♭', 'ré♭', 'la♭', 'mi♭', 'si♭', 'fa', 'do', 'sol', 'ré', 'la', 'mi', 'si', 'fa♯', 'do♯']
MIN = ['la♭', 'mi♭', 'si♭', 'fa', 'do', 'sol', 'ré', 'la', 'mi', 'si', 'fa♯', 'do♯', 'sol♯', 'ré♯', 'la♯']
FIFTHS = {0: 0, 2: 2, 9: 3, 7: 1}

def name(step, alt):
    return STEPS[step] + ACC[alt]

def up(step, alt, d, s):
    ns = (step + d) % 7
    semi = (NAT[step] + alt + s - NAT[ns]) % 12
    alt2 = semi if semi <= 6 else semi - 12
    return ns, alt2

def armure(k):
    return "aucune altération" if k == 0 else f"{abs(k)} {'dièse' if k > 0 else 'bémol'}{'s' if abs(k) > 1 else ''}"

def page(i, tpl):
    slug, nom, pour, d, s, inter, sonne, octv = i
    n = FIFTHS[s % 12]
    head = tpl[:tpl.index("</head>")]
    title = f"Transposer pour {pour}"
    desc = f"{nom} : un do écrit sonne {sonne}{octv}. Écrire chaque note {inter.split(' (')[0]} plus haut, tableau des tonalités et des notes, pièges à éviter."
    head = re.sub(r"<title>.*?</title>", f"<title>{e(title)} : Fredonne</title>", head)
    for k, v in [('name="description"', desc), ('property="og:title"', title), ('property="og:description"', desc)]:
        head = re.sub(rf'<meta {k} content="[^"]*">', f'<meta {k} content="{html.escape(v)}">', head)
    url = f"{SITE}transposer-{slug}.html"
    head = re.sub(r'<meta property="og:url" content="[^"]*">', f'<meta property="og:url" content="{url}">', head)
    head = re.sub(r'<link rel="canonical"[^>]*>', f'<link rel="canonical" href="{url}">', head)
    head = re.sub(r'<script type="application/ld\+json".*?</script>\n?', "", head, flags=re.S)
    crumbs = [("Guides", SITE + "guides.html"), ("Transposer", SITE + "transposer.html"), (nom, url)]
    ld = json.dumps({"@context": "https://schema.org", "@type": "BreadcrumbList", "itemListElement": [
        {"@type": "ListItem", "position": k + 1, "name": a, "item": b} for k, (a, b) in enumerate(crumbs)]}, ensure_ascii=False)
    head += f'<script type="application/ld+json">{ld}</script>\n'
    header = tpl[tpl.index("</head>"):tpl.index("<main")].replace(' aria-current="page"', "")
    foot = tpl[tpl.index('<footer class="wrap foot">'):]
    foot = re.sub(r"<script>.*?</script>\s*", "", foot, flags=re.S)
    keys = "".join(f'  <div class="i">{MAJ[k + 7]} majeur · {MIN[k + 7]} mineur <small>({armure(k)})</small></div><div class="n">{MAJ[w + 7]} majeur · {MIN[w + 7]} mineur <small>({armure(w)})</small></div>\n'
                   for k in range(-6, 7) for w in [k + n - 12 if k + n > 6 else k + n])
    notes = "".join(f'  <div class="i">{name(st, al)}</div><div class="n">{name(*up(st, al, d, s))}</div>\n' for st, al in CHROM)
    others = " · ".join(f'<a href="transposer-{o[0]}.html">{e(o[1])}</a>' for o in INSTR if o[0] != slug)
    octave_note = (f"<p>L'intervalle comprend une octave : on écrit la partie une octave plus haut que l'intervalle simple, pour qu'elle tombe dans la tessiture écrite de l'instrument.</p>" if d >= 7 else "")
    body = f'''<main id="contenu" class="wrap doc">
<p class="kicker"><a href="guides.html">Guides</a> › <a href="transposer.html">Transposer</a></p>
<h1>Transposer pour {e(pour)}</h1>
<p class="lead">{e(nom)} : quand on lit un do, on entend un {sonne}{octv}. Pour jouer une partition écrite en sons réels (en ut), écrivez chaque note <strong>{e(inter)}</strong> plus haut.</p>

<h2>L'armure en un coup d'œil</h2>
<p>{("Ajoutez " + armure(n) + " à l'armure, ou retirez " + ("un bémol" if n == 1 else f"{n} bémols") + ".") if n else "L'armure ne change pas : seule l'octave change."} Si l'on dépasse six altérations, passez à la tonalité équivalente.</p>
{octave_note}

<h2>Tonalité du morceau → tonalité à écrire</h2>
<div class="grid two" role="table" aria-label="Tonalités">
  <div class="h">Le morceau sonne en</div><div class="h">J'écris en</div>
{keys}</div>

<h2>Note par note</h2>
<p>Pour une note isolée ou une altération accidentelle, sans tenir compte de l'octave{(" (ajoutez-la)" if d >= 7 else "")}.</p>
<div class="grid two" role="table" aria-label="Notes">
  <div class="h">Note entendue</div><div class="h">Note écrite</div>
{notes}</div>

<h2>Dans l'autre sens</h2>
<p>Pour jouer au piano ou à la flûte une partie écrite pour {e(pour)}, faites l'inverse : écrivez chaque note {e(inter.split(" (")[0])} plus bas.</p>

<aside class="callout">
  <h2>Transposer d'un geste</h2>
  <p>Fredonne transpose une mélodie pour {e(pour)}, la fait écouter et signale les notes hors tessiture. Gratuite, sans compte et sans réseau.</p>
  <div class="cta" style="margin-top:0">
    <a class="btn" href="https://play.google.com/store/apps/details?id=fr.fredonne.app&amp;referrer=utm_source%3Dsite%26utm_content%3Dtransposer-{slug}">Google Play</a>
    <a class="btn ghost" href="transposer.html">L'aide-mémoire complet</a>
  </div>
</aside>

<h2>Les autres instruments</h2>
<p>{others}</p>
<p>Intervalles vérifiés : <a href="sources.html">sources des transpositions</a>.</p>
</main>

'''
    css = "<style>.grid.two{grid-template-columns:1fr 1fr!important}.grid.two>.n{display:block!important}.grid.two>.h{display:block!important}.grid small{color:var(--muted);font-weight:400}.kicker a{color:inherit}</style>\n"
    return f"transposer-{slug}.html", head + css + header + body + foot

def main():
    tpl = (DOCS / "transposer.html").read_text(encoding="utf-8")
    pages = dict(page(i, tpl) for i in INSTR)
    links = '<p class="instr-links">' + " · ".join(f'<a href="transposer-{i[0]}.html">{e(i[1])}</a>' for i in INSTR) + "</p>"
    tr_new = re.sub(r"(<!-- INSTRUMENTS -->).*?(<!-- FIN INSTRUMENTS -->)", lambda m: m.group(1) + links + m.group(2), tpl, flags=re.S)
    if "--check" in sys.argv:
        stale = [n for n, c in pages.items() if not (DOCS / n).exists() or (DOCS / n).read_text(encoding="utf-8") != c]
        if tr_new != tpl:
            stale.append("transposer.html")
        sys.exit(f"{len(stale)} pages à régénérer" if stale else 0)
    (DOCS / "transposer.html").write_text(tr_new, encoding="utf-8")
    for n, c in pages.items():
        (DOCS / n).write_text(c, encoding="utf-8")
    print(f"{len(pages)} pages écrites")

if __name__ == "__main__":
    main()
