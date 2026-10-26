import sys,re; import os; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
c=B(); c.get('/signup'); c.post('/signup',{'fullName':'Cit Zen','username':'cit_zen','email':'cz@example.org','phone':'','password':'Passw0rd123','confirmPassword':'Passw0rd123'})
check('citizen signup',(c.alert() and c.alert()[0]=='success') or 'already' in str(c.alert()),c.alert())
c.post('/login',{'username':'cit_zen','password':'Passw0rd123'})
check('citizen sees Report Emergency, not Victims','Report Emergency' in c.last and '/app/victims' not in c.last)
PII=['Anil Kumar','Meera','Low One','9000000001','9876543210','Green Corp','9444444444','g@corp.org','Vol One','9111111111','Vol Two','9222222222','Sita','9876500000','Ravi','9876501234','v1@example','admin@example']
c.get('/app/reports'); types=re.findall(r'<option value="([A-Z_]+)"',c.last[c.last.find('id="reportType"'):c.last.find('id="f_disaster"')])
leaks={}
for t in types:
    c.get('/app/reports?type='+t); body=c.text()
    hit=[p for p in PII if p in body]
    if hit: leaks[t]=hit
    print(f'   {t:20s} status={c.r.status_code} PII={hit}')
check('NO report exposes personal data to a citizen', not leaks, leaks)
# direct URL tampering for restricted report types
for t in ('VOLUNTEERS','DONATIONS'):
    c.get('/app/reports?type='+t); check('tampered URL type='+t+' -> 403 and no data',c.r.status_code==403 and 'Vol One' not in c.text() and 'Green Corp' not in c.text(),c.r.status_code)
    r=c.s.get(BASE+'/app/reports?type='+t+'&export=csv'); check('tampered CSV export type='+t+' blocked',r.status_code==403 and 'Green Corp' not in r.text and 'Vol One' not in r.text,r.status_code)
check('dropdown hides restricted reports for citizen','VOLUNTEERS' not in c.last[c.last.find('id="reportType"'):c.last.find('id="f_disaster"')])
a=B(); a.get('/login'); a.post('/login',{'username':'admin_test','password':'Sup3rSecret99'})
a.get('/app/reports?type=DONATIONS'); check('admin still sees donations report','Green Corp' in a.text() or '25000' in a.text())
sys.exit(1 if summary() else 0)
