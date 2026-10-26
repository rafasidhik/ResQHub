"""Race test: 20 simultaneous 5-person allocations into a 30-bed shelter.
Invariants: occupancy never exceeds capacity; successes <= 6; occupancy == 5*successes."""
import sys,re,os,subprocess,shlex,threading,time; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
def sql(q): return subprocess.run(shlex.split(os.environ.get('RESQHUB_MYSQL','mysql -N resqhub'))+['-e',q],capture_output=True,text=True).stdout.strip()
user=os.environ.get('RQ_ADMIN','admin_test'); pw=os.environ.get('RQ_ADMIN_PW','Sup3rSecret99')
a=B(); a.get('/login'); a.post('/login',{'username':user,'password':pw})
code='RS-'+str(time.time_ns())[-8:]
a.get('/app/shelters'); a.post('/app/shelters',{'action':'create','_modal':'createShelter','name':'Race Hall','code':code,'district':'D','city':'C','address':'A','maxCapacity':'30','occupancy':'0','status':'AVAILABLE','contact':'9333333333','manager':'M','disasterId':sql('select id from disasters limit 1')})
check('race shelter created',a.alert() and a.alert()[0]=='success',a.alert())
sid=sql(f"select id from shelters where code='{code}'")
res=[]; lock=threading.Lock(); sessions=[]
for i in range(20):
    s=B(); s.get('/login'); s.post('/login',{'username':user,'password':pw}); s.get('/app/shelters?id='+sid); sessions.append((i,s))
def go(i,s):
    s.post('/app/shelters?id='+sid,{'action':'allocate','id':sid,'_modal':'allocate','victimId':'','familyName':f'Family {i}','people':'5','notes':''})
    with lock: res.append(s.alert())
ts=[threading.Thread(target=go,args=x) for x in sessions]; [t.start() for t in ts]; [t.join() for t in ts]
ok_=sum(1 for r in res if r and r[0]=='success'); occ=int(sql(f"select current_occupancy from shelters where id={sid}"))
allocs=int(sql(f"select coalesce(sum(people_count),0) from shelter_allocations where shelter_id={sid} and status in ('ACTIVE','CHECKED_IN','PENDING')"))
print(f'   successes={ok_} rejected={len(res)-ok_} occupancy={occ}/30 allocation_rows_total={allocs}')
check('occupancy never exceeds capacity',occ<=30,occ)
check('exactly 6 allocations fit (30/5)',ok_==6,ok_)
check('occupancy == 5 x successes',occ==5*ok_,occ)
check('allocation records match occupancy',allocs==occ,allocs)
sys.exit(1 if summary() else 0)
