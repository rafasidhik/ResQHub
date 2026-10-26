import sys,re,hashlib,subprocess,threading,os,shlex; import os; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
import requests
def sql(q): return subprocess.run(shlex.split(os.environ.get('RESQHUB_MYSQL','mysql -N resqhub'))+['-e',q],capture_output=True,text=True).stdout.strip()
a=B(); a.get('/login'); a.post('/login',{'username':'admin_test','password':'Sup3rSecret99'})
def mk(u,role):
    a.get('/app/users'); a.post('/app/users',{'action':'create','_modal':'createUser','fullName':u.replace('_',' ').title(),'username':u,'email':u+'@example.org','phone':'','role':role,'password':'Passw0rd123','confirmPassword':'Passw0rd123'})
    return a.alert()
for u,r in (('off_one','RESCUE_OFFICER'),('camp_one','CAMP_MANAGER')): print('  create',u,mk(u,r))
# ---------- role x module matrix (expected values mirror Module.java / original nav rules)
M={'/app/help':'ADMIN OFFICER CAMP VOL CIT','/app/dashboard':'ADMIN OFFICER CAMP VOL CIT','/app/profile':'ADMIN OFFICER CAMP VOL CIT','/app/notifications':'ADMIN OFFICER CAMP VOL CIT','/app/reports':'ADMIN OFFICER CAMP VOL CIT',
'/app/disasters':'ADMIN OFFICER','/app/teams':'ADMIN OFFICER','/app/volunteers':'ADMIN OFFICER','/app/donations':'ADMIN OFFICER','/app/users':'ADMIN',
'/app/victims':'ADMIN OFFICER CAMP','/app/requests':'ADMIN OFFICER CAMP','/app/shelters':'ADMIN OFFICER CAMP','/app/smart-allocation':'ADMIN OFFICER CAMP','/app/resources':'ADMIN OFFICER CAMP','/app/food':'ADMIN OFFICER CAMP',
'/app/my-tasks':'VOL','/app/report-emergency':'CIT'}
users={'ADMIN':'admin_test','OFFICER':'off_one','CAMP':'camp_one','VOL':'vol_one','CIT':'cit_zen'}
pw={'admin_test':'Sup3rSecret99'}
bad=[]
for role,u in users.items():
    s=B(); s.get('/login'); s.post('/login',{'username':u,'password':pw.get(u,'Passw0rd123')})
    assert s.r.url.endswith('/app/dashboard'),(u,s.r.url)
    for path,allowed in M.items():
        s.get(path); exp=200 if role in allowed.split() else 403
        if s.r.status_code!=exp: bad.append((role,path,s.r.status_code,exp))
    nav=set(re.findall(r'href="/resqhub(/app/[a-z-]+)"',s.last))
    nav_expected={p for p,al in M.items() if role in al.split()}
    if nav!=nav_expected: bad.append((role,'sidebar',sorted(nav^nav_expected)))
check(f'role x module matrix: {len(users)} roles x {len(M)} modules + sidebar contents all as expected',not bad,bad)
# ---------- citizen can submit an emergency
c=B(); c.get('/login'); c.post('/login',{'username':'cit_zen','password':'Passw0rd123'}); c.get('/app/report-emergency')
did=re.search(r'<option value="(\d+)"',c.last[c.last.find('name="disasterId"'):]).group(1)
c.post('/app/report-emergency',{'action':'create','_modal':'none','requesterName':'Cit Zen','contactNumber':'9555555555','location':'Riverside','people':'3','children':'1','elderly':'0','medicalEmergency':'on','disasterId':did,'assistance':'water'})
check('citizen submits emergency request',c.alert() and c.alert()[0]=='success',c.alert())
c.get('/app/requests'); check('citizen cannot read request list (403)',c.r.status_code==403)
# ---------- legacy SHA-256 password -> silent upgrade
sha=hashlib.sha256(b'LegacyPass1').hexdigest()
sql("insert into users (username,password_hash,full_name,email,role_id,account_status) select 'legacy_u','%s','Legacy User','l@example.org',id,'ACTIVE' from roles where role_name='CITIZEN'"%sha)
check('legacy account present with 64-char SHA-256 hash',len(sql("select password_hash from users where username='legacy_u'"))==64)
l=B(); l.get('/login'); l.post('/login',{'username':'legacy_u','password':'LegacyPass1'})
check('legacy account can still log in',l.r.url.endswith('/app/dashboard'),l.alert())
h=sql("select password_hash from users where username='legacy_u'"); print('   stored hash now:',h[:22]+'...')
check('hash silently upgraded to salted PBKDF2',h.startswith('pbkdf2$210000$'))
l2=B(); l2.get('/login'); l2.post('/login',{'username':'legacy_u','password':'LegacyPass1'}); check('login works again after upgrade',l2.r.url.endswith('/app/dashboard'))
l3=B(); l3.get('/login'); l3.post('/login',{'username':'legacy_u','password':'wrongpass1'}); check('wrong password still rejected after upgrade',l3.alert() and l3.alert()[0]=='danger')
h1=sql("select password_hash from users where username='cit_zen'"); h2=sql("select password_hash from users where username='vol_one'")
check('same password -> different hashes (per-user salt)',h1!=h2 and h1.startswith('pbkdf2$') and h2.startswith('pbkdf2$'))
check('no plaintext passwords stored',sql("select count(*) from users where password_hash in ('Passw0rd123','Sup3rSecret99')")=='0')
# ---------- cookies, headers, session lifecycle
r=requests.get(BASE+'/login'); sc=r.headers.get('Set-Cookie','')
check('session cookie HttpOnly + SameSite=Lax',('HttpOnly' in sc) and ('SameSite=Lax' in sc) and 'RESQHUBSESSION' in sc,sc)
s=B(); s.get('/login'); s.post('/login',{'username':'off_one','password':'Passw0rd123'}); s.get('/app/dashboard'); jar=dict(s.s.cookies)
tok=s.token()
r=s.s.post(BASE+'/logout',data={},allow_redirects=True); check('logout without CSRF token is refused (user stays logged in)',r.url.endswith('/app/dashboard'),r.url)
s.get('/app/dashboard'); check('session survives the refused logout',s.r.url.endswith('/app/dashboard'))
s.post('/logout'); 
old=requests.get(BASE+'/app/dashboard',cookies=jar,allow_redirects=True); check('old session cookie is dead after logout',('Welcome back' in old.text) and '/login' in old.url)
# session fixation: id changes at login
f=B(); f.get('/login'); before=f.s.cookies.get('RESQHUBSESSION'); f.post('/login',{'username':'off_one','password':'Passw0rd123'}); after=f.s.cookies.get('RESQHUBSESSION')
check('session id rotated on login (fixation)',before and after and before!=after,(before,after))
# ---------- XSS
a.get('/app/victims'); a.get('/app/disasters'); xd=re.search(r'data-id="(\d+)" data-title="Kerala Flood Test"',a.last).group(1); a.get('/app/victims')
a.post('/app/victims',{'action':'create','_modal':'createVictim','fullName':'Xss Tester','age':'30','gender':'MALE','phone':'','emergencyStatus':'SAFE','currentLocation':'<script>alert(2)</script>','disasterId':xd,'medicalCondition':'<img src=x onerror=alert(1)>','familyInfo':'<b>bold</b>'})
check('XSS test record accepted',a.alert() and a.alert()[0]=='success',a.alert())
a.get('/app/victims'); check('XSS payloads rendered escaped in victim table + edit-button data attributes',('<img src=x' not in a.last) and ('<script>alert(2)' not in a.last) and '&lt;script&gt;alert(2)' in a.last,'')
a.get('/app/smart-allocation'); check('XSS escaped in smart-allocation victim dropdown','<img src=x' not in a.last)
# ---------- open redirect / exposure
a.get('/app/disasters'); tk=a.token()
r=a.s.post(BASE+'/app/disasters',data={'action':'close','id':'999999','_csrf':tk,'_return':'https://evil.example/x'},allow_redirects=False)
check('open redirect via _return ignored',not r.headers.get('Location','').startswith('http'),r.headers.get('Location'))
r=a.s.post(BASE+'/app/disasters',data={'action':'close','id':'999999','_csrf':tk,'_return':'/resqhub/app/../login'},allow_redirects=False)
for p in ('/WEB-INF/web.xml','/WEB-INF/views/layout.jsp','/WEB-INF/classes/config/db.properties','/config/db.properties','/META-INF/context.xml','/assets/../WEB-INF/web.xml'):
    r=requests.get(BASE+p); check('not publicly served: '+p,r.status_code in (400,404) and 'db.password' not in r.text and '<web-app' not in r.text,r.status_code)
a.get('/app/nope'); check('unknown /app path (logged in) -> friendly 404, no stack trace',a.r.status_code==404 and 'Exception' not in a.last and 'at com.' not in a.last,a.r.status_code)
r=requests.get(BASE+'/app/nope',allow_redirects=False); check('unknown /app path (anonymous) -> redirected to login',r.status_code==302 and 'login' in r.headers.get('Location',''))
r=requests.get(BASE+'/app/disasters?id=abc',allow_redirects=True); check('bad id parameter does not leak stack trace','at com.resqhub' not in r.text)
a.get('/app/requests?id=notanumber'); check('non-numeric id on admin page handled gracefully (400, no trace)',a.r.status_code==400 and 'at com.resqhub' not in a.last,a.r.status_code)
a.get('/app/requests?id=99999999'); check('unknown record id -> 404 page',a.r.status_code==404)
# ---------- deactivation takes effect on the very next request
d=B(); d.get('/login'); d.post('/login',{'username':'camp_one','password':'Passw0rd123'}); d.get('/app/shelters'); assert d.r.status_code==200
a.get('/app/users'); uid=re.search(r'data-id="(\d+)" data-full-name="Camp One"',a.last).group(1)
a.post('/app/users',{'action':'status','id':uid,'_modal':'statusUser','status':'INACTIVE'}); check('admin deactivates camp_one',a.alert() and a.alert()[0]=='success',a.alert())
d.get('/app/shelters'); check('deactivated user is kicked on next request',('Welcome back' in d.last) and '/login' in d.r.url)
# ---------- lockout
for i in range(4): x=B(); x.get('/login'); x.post('/login',{'username':'off_one','password':'bad-password-'+str(i)})
check('account locks after repeated failures (original rule)', 'lock' in x.text().lower() or sql("select account_status from users where username='off_one'")=='LOCKED',x.alert())
a.get('/app/users'); uid=re.search(r'data-id="(\d+)" data-full-name="Off One"',a.last).group(1); a.post('/app/users',{'action':'unlock','id':uid}); check('admin can unlock',a.alert() and a.alert()[0]=='success',a.alert())
# ---------- CONCURRENCY: identities must never bleed between simultaneous users
names={'admin_test':'Test Admin','vol_one':'Vol One','cit_zen':'Cit Zen','off_one':'Off One'}
sess={}
for u in names:
    b=B(); b.get('/login'); b.post('/login',{'username':u,'password':pw.get(u,'Passw0rd123')}); sess[u]=b
errors=[]; lock=threading.Lock()
def worker(u):
    b=sess[u]; own=names[u]
    for i in range(60):
        try:
            r=b.s.get(BASE+'/app/dashboard'); html=r.text
            m=re.search(r'<span class="d-block small fw-semibold">([^<]*)</span>',html)
            who=m.group(1) if m else None
            r2=b.s.get(BASE+'/app/users'); expect=200 if u=='admin_test' else 403
            if who!=own or r2.status_code!=expect:
                with lock: errors.append((u,i,who,r2.status_code))
        except Exception as e:
            with lock: errors.append((u,i,str(e)))
ts=[threading.Thread(target=worker,args=(u,)) for u in names for _ in range(3)]
[t.start() for t in ts]; [t.join() for t in ts]
check(f'concurrency: {len(ts)} threads x 120 requests, 4 users - no identity/role bleed',not errors,errors[:5])
sys.exit(1 if summary() else 0)
