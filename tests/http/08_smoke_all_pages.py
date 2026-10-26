import sys,re; import os; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
user = sys.argv[1] if len(sys.argv) > 1 else os.environ.get('RESQHUB_TEST_USER', 'admin')
pw = sys.argv[2] if len(sys.argv) > 2 else os.environ.get('RESQHUB_TEST_PASSWORD', '')
if not pw:
    print('ERROR: Missing test credentials. Pass via CLI args or set RESQHUB_TEST_USER and RESQHUB_TEST_PASSWORD.')
    sys.exit(1)
b=B(); b.get('/login'); b.post('/login',{'username':user,'password':pw})
assert b.r.url.endswith('/app/dashboard'), 'Login failed for test user'
pages=['/app/dashboard','/app/profile','/app/disasters','/app/victims','/app/requests','/app/teams','/app/volunteers','/app/shelters','/app/smart-allocation','/app/resources','/app/food','/app/donations','/app/notifications','/app/reports','/app/users']
reports=['OVERVIEW','DISASTERS','VICTIMS','RESCUE_REQUESTS','RESCUE_PERFORMANCE','VOLUNTEERS','DONATIONS','SHELTER_OCCUPANCY','ALLOCATION_OVERVIEW','RESOURCE_INVENTORY','FOOD_DISTRIBUTION']
detail={'/app/requests':'/app/requests?id=','/app/teams':'/app/teams?id=','/app/volunteers':'/app/volunteers?id=','/app/shelters':'/app/shelters?id=','/app/food':'/app/food?id=','/app/donations':'/app/donations?id='}
for p in pages+['/app/reports?type='+r for r in reports]:
    b.get(p); bad=b.r.status_code!=200 or 'alert alert-danger' in b.last or 'Something went wrong' in b.last
    check(f'{p} -> {b.r.status_code}, rows={len(re.findall("<tr data-row>",b.last))}',not bad,b.alert())
    if p in detail:
        ids=sorted(set(re.findall(re.escape(detail[p])+r'(\d+)',b.last)))[:2]
        for i in ids:
            b.get(detail[p]+i); check(f'  detail {detail[p]}{i} -> {b.r.status_code}',b.r.status_code==200 and 'Something went wrong' not in b.last)
sys.exit(1 if summary() else 0)
