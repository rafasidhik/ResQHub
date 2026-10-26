"""Tiny browser-like HTTP client (cookies + CSRF token scraping) and check helpers for the ResQHub test scripts."""
import re, requests, html
import os
BASE=os.environ.get('RESQHUB_URL','http://localhost:8080').rstrip('/')
class B:
    def __init__(s): s.s=requests.Session(); s.last=''; s.r=None
    def get(s,p,**k):
        s.r=s.s.get(BASE+p,allow_redirects=True,**k); s.last=s.r.text; return s.r
    def token(s):
        m=re.search(r'name="_csrf" value="([^"]+)"',s.last); return html.unescape(m.group(1)) if m else None
    def post(s,p,data=None,token=True,**k):
        d=dict(data or {})
        if token and s.token(): d['_csrf']=s.token()
        m=re.search(r'name="_return" value="([^"]*)"',s.last)
        if token and m and '_return' not in d: d['_return']=html.unescape(m.group(1))
        s.r=s.s.post(BASE+p,data=d,allow_redirects=True,**k); s.last=s.r.text; return s.r
    def alert(s):
        m=re.search(r'<div class="alert alert-(\w+)[^>]*>(.*?)</div>',s.last,re.S)
        if not m: return None
        return m.group(1), re.sub(r'<[^>]+>|\s+',' ',html.unescape(m.group(2))).strip()
    def text(s): return re.sub(r'\s+',' ',re.sub(r'<[^>]+>',' ',html.unescape(s.last)))
ok=fail=0
def check(name,cond,extra=''):
    global ok,fail
    if cond: ok+=1; print('  PASS',name)
    else: fail+=1; print('  FAIL',name,extra)
def summary(): print(f'== {ok} passed, {fail} failed'); return fail
