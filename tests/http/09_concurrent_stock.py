"""Race test: 30 simultaneous 'distribute 10 units' requests compete for 100 units.
Invariants: stock never negative; successes <= 10; final stock == 100 - 10*successes;
distribution log total == 10*successes. Run after 01_auth.py (needs the admin account)."""
import sys,re,os,subprocess,shlex,threading; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
def sql(q): return subprocess.run(shlex.split(os.environ.get('RESQHUB_MYSQL','mysql -N resqhub'))+['-e',q],capture_output=True,text=True).stdout.strip()
user=os.environ.get('RQ_ADMIN','admin_test'); pw=os.environ.get('RQ_ADMIN_PW','Sup3rSecret99')
a=B(); a.get('/login'); a.post('/login',{'username':user,'password':pw})
a.get('/app/resources'); a.post('/app/resources',{'action':'create','_modal':'createResource','name':'RaceWater','code':'RACE-'+str(__import__('time').time_ns())[-9:],'category':'WATER','unit':'btl','available':'100','minLevel':'1'})
rid=sql("select id from resources where name='RaceWater' order by id desc limit 1"); check('race resource created with 100 units',sql(f"select available_quantity from resources where id={rid}")=='100')
res=[]; lock=threading.Lock()
sessions=[]
for i in range(30):
    s=B(); s.get('/login'); s.post('/login',{'username':user,'password':pw}); s.get('/app/resources'); sessions.append(s)
def go(s):
    s.post('/app/resources',{'action':'distribute','id':rid,'_modal':'distribute','quantity':'10','destination':'OTHER','distributedTo':'race','disasterId':'','shelterId':'','victimId':'','reason':'race'})
    with lock: res.append(s.alert())
ts=[threading.Thread(target=go,args=(s,)) for s in sessions]; [t.start() for t in ts]; [t.join() for t in ts]
ok_=sum(1 for r in res if r and r[0]=='success'); bad=len(res)-ok_
final=int(sql(f"select available_quantity from resources where id={rid}")); logged=int(sql(f"select coalesce(sum(quantity),0) from resource_distributions where resource_id={rid}"))
print(f'   successes={ok_} rejected={bad} final_stock={final} logged_distribution={logged}')
check('stock never negative',final>=0,final)
check('exactly 10 requests could succeed (100/10)',ok_==10,ok_)
check('final stock == 100 - 10*successes',final==100-10*ok_,final)
check('distribution log total == units removed',logged==10*ok_,logged)
check('rejected requests got a clear business error',all(r and r[0]=='danger' for r in res if not (r and r[0]=='success')))
sys.exit(1 if summary() else 0)
