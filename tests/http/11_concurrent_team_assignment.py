"""Race test: the SAME rescue team is assigned to 2 different requests by 2 users at the same instant.
Invariant: a team can only be on one active assignment -> exactly one succeeds."""
import sys,re,os,subprocess,shlex,threading,time; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
def sql(q): return subprocess.run(shlex.split(os.environ.get('RESQHUB_MYSQL','mysql -N resqhub'))+['-e',q],capture_output=True,text=True).stdout.strip()
user=os.environ.get('RQ_ADMIN','admin_test'); pw=os.environ.get('RQ_ADMIN_PW','Sup3rSecret99')
a=B(); a.get('/login'); a.post('/login',{'username':user,'password':pw})
tag=str(time.time_ns())[-6:]
ltag=''.join(chr(97+int(c)) for c in tag)   # letters only: names may not contain digits
a.get('/app/teams'); a.post('/app/teams',{'action':'create','_modal':'createTeam','teamName':'Race Team '+tag,'teamType':'FIRE_RESCUE','leaderName':'Leader Race','contactNumber':'9876501234','memberCount':'5','baseLocation':'X','skills':'rescue','equipment':'rope'})
tid=sql(f"select id from rescue_teams where team_name='Race Team {tag}'"); did=sql('select id from disasters limit 1')
rids=[]
for n in range(2):
    a.get('/app/requests'); a.post('/app/requests',{'action':'create','_modal':'createRequest','requesterName':f'Racer {ltag}'+'xy'[n],'contactNumber':'9000000009','disasterId':did,'location':f'Location {tag} {n}','people':'2','children':'0','elderly':'0','assistance':'x'})
    rid=sql(f"select max(id) from rescue_requests"); rids.append(rid); a.post('/app/requests?id='+rid,{'action':'startReview','id':rid})
check('two requests under review',all(sql(f"select status from rescue_requests where id={r}")=='UNDER_REVIEW' for r in rids))
res=[]; lock=threading.Lock(); ss=[]
for r in rids:
    s=B(); s.get('/login'); s.post('/login',{'username':user,'password':pw}); s.get('/app/requests?id='+r); ss.append((r,s))
def go(r,s):
    s.post('/app/requests?id='+r,{'action':'assign','id':r,'teamId':tid})
    with lock: res.append(s.alert())
ts=[threading.Thread(target=go,args=x) for x in ss]; [t.start() for t in ts]; [t.join() for t in ts]
ok_=sum(1 for r in res if r and r[0]=='success'); active=int(sql(f"select count(*) from rescue_assignments where rescue_team_id={tid} and assignment_status not in ('COMPLETED','ABORTED')"))
print('   results:',[ (r[0], r[1][:60]) if r else None for r in res],'active assignments for team:',active)
check('exactly one of the two simultaneous assignments succeeds',ok_==1,ok_)
check('team has exactly one active assignment',active==1,active)
sys.exit(1 if summary() else 0)
