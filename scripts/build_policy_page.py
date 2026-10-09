"""Regenerates docs/index.html (the GitHub Pages privacy policy) from Privacy_Policy.txt.
Run: python scripts/build_policy_page.py
"""
import html
import os
import re

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
lines = open(os.path.join(root, "Privacy_Policy.txt"), encoding="utf-8").read().split("\n")

title = lines[0].strip()
effective = lines[1].strip()
body = []
in_list = False


def close_list():
    global in_list
    if in_list:
        body.append("</ul>")
        in_list = False


def linkify(text):
    text = html.escape(text)
    return re.sub(r"([\w.+-]+@[\w-]+\.[\w.-]+)", r'<a href="mailto:\1">\1</a>', text)


for raw in lines[2:]:
    line = raw.rstrip()
    if not line.strip():
        close_list()
        continue
    if re.match(r"^\d+\) ", line):
        close_list()
        body.append(f"<h2>{html.escape(line)}</h2>")
    elif line.startswith("- "):
        if not in_list:
            body.append("<ul>")
            in_list = True
        body.append(f"<li>{linkify(line[2:])}</li>")
    else:
        close_list()
        body.append(f"<p>{linkify(line)}</p>")
close_list()

page = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<style>
  :root {{ --bg:#f7f4ed; --fg:#1b1c18; --muted:#5b5d54; --accent:#4f7656; --card:#fffdf8; }}
  @media (prefers-color-scheme: dark) {{
    :root {{ --bg:#14150f; --fg:#ecebe3; --muted:#a7a99e; --accent:#8fb797; --card:#1c1d17; }}
  }}
  body {{ margin:0; background:var(--bg); color:var(--fg);
         font:16px/1.65 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }}
  main {{ max-width:720px; margin:0 auto; padding:32px 20px 64px; }}
  article {{ background:var(--card); border-radius:14px; padding:8px 24px 24px; }}
  h1 {{ font-size:1.8rem; margin:0 0 4px; }}
  .effective {{ color:var(--muted); margin:0 0 8px; }}
  h2 {{ font-size:1.1rem; color:var(--accent); margin:28px 0 6px; }}
  p {{ margin:8px 0; }}
  ul {{ margin:8px 0; padding-left:22px; }}
  li {{ margin:4px 0; }}
  a {{ color:var(--accent); }}
</style>
</head>
<body>
<main>
<h1>{html.escape(title)}</h1>
<p class="effective">{html.escape(effective)}</p>
<article>
{chr(10).join(body)}
</article>
</main>
</body>
</html>
"""

os.makedirs(os.path.join(root, "docs"), exist_ok=True)
open(os.path.join(root, "docs", "index.html"), "w", encoding="utf-8").write(page)
print("wrote docs/index.html,", len(body), "blocks")
