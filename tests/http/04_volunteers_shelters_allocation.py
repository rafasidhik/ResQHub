import sys,re; import os; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
from httpkit import *
a=B(); a.get('/login'); a.post('/login',{'username':'admin_test','password':'Sup3rSecret99'})
# ---- users module: create volunteer accounts
for u,n,e in (('vol_one','Vol One','v1@example.org'),('vol_two','Vol Two','v2@example.org')):
    a.get('/app/users'); a.post('/app/users',{'action':'create','_modal':'createUser','fullName':n,'username':u,'email':e,'phone':'','role':'VOLUNTEER','password':'Passw0rd123','confirmPassword':'Passw0rd123'})
    check('admin creates '+u,a.alert() and a.alert()[0]=='success' or 'already' in str(a.alert()),a.alert())
a.get('/app/users'); check('users page lists new accounts','vol_one' in a.last and 'vol_two' in a.last)
# ---- volunteer role flows
v1=B(); v1.get('/login'); v1.post('/login',{'username':'vol_one','password':'Passw0rd123'})
check('volunteer lands on dashboard',v1.r.url.endswith('/app/dashboard'))
check('volunteer sidebar has My Tasks, no Disasters','My Tasks' in v1.last and '/app/disasters' not in v1.last and '/app/users' not in v1.last)
v1.get('/app/disasters'); check('volunteer blocked from disasters (403)',v1.r.status_code==403)
v1.get('/app/users'); check('volunteer blocked from users (403)',v1.r.status_code==403)
v1.get('/app/volunteers'); check('volunteer blocked from staff volunteers page (403)',v1.r.status_code==403)
v1.get('/app/my-tasks'); check('unlinked volunteer sees guidance (no tasks, no crash)','not linked to a volunteer profile' in v1.last)
v2=B(); v2.get('/login'); v2.post('/login',{'username':'vol_two','password':'Passw0rd123'})
# staff registers volunteer records, then links them to the accounts
a.get('/app/volunteers')
a.post('/app/volunteers',{'action':'create','_modal':'createVolunteer','fullName':'Vol One','contactNumber':'9111111111','email':'v1@example.org','location':'Nilambur','emergencyRole':'RESCUE','maxWorkload':'3','availability':'AVAILABLE','skills':'boating'})
check('staff registers volunteer 1',a.alert() and a.alert()[0]=='success',a.alert())
a.post('/app/volunteers',{'action':'create','_modal':'createVolunteer','fullName':'Vol Two','contactNumber':'9222222222','email':'v2@example.org','location':'Manjeri','emergencyRole':'MEDICAL','maxWorkload':'3','availability':'AVAILABLE','skills':'first aid'})
check('staff registers volunteer 2',a.alert() and a.alert()[0]=='success',a.alert())
a.post('/app/volunteers',{'action':'create','_modal':'createVolunteer','fullName':'Dup Phone','contactNumber':'9111111111','email':'','location':'X','emergencyRole':'GENERAL','maxWorkload':'3'})
check('duplicate contact number rejected (original rule)',a.alert() and a.alert()[0]=='danger',a.alert())
def link(name,user):
    a.get('/app/volunteers'); vid=re.search(r'/app/volunteers\?id=(\d+)',[x for x in re.findall(r'<tr data-row>(.*?)</tr>',a.last,re.S) if name in x][0]).group(1)
    a.get('/app/volunteers?id='+vid); uid=re.search(r'<option value="(\d+)"[^>]*>'+user+' ',a.last).group(1)
    a.post('/app/volunteers?id='+vid,{'action':'link','id':vid,'userId':uid}); return vid
link('Vol One','vol_one'); check('link account to volunteer 1',a.alert() and a.alert()[0]=='success',a.alert())
link('Vol Two','vol_two'); check('link account to volunteer 2',a.alert() and a.alert()[0]=='success',a.alert())
a.get('/app/volunteers'); vid1=re.search(r'/app/volunteers\?id=(\d+)',[x for x in re.findall(r'<tr data-row>(.*?)</tr>',a.last,re.S) if 'Vol One' in x][0]).group(1)
a.post('/app/volunteers?id='+vid1,{'action':'link','id':vid1,'userId':re.search(r'<option value="(\d+)"[^>]*>vol_two ',(a.get('/app/volunteers?id='+vid1) and a.last)).group(1)})
check('same account cannot be linked to two volunteers',a.alert() and a.alert()[0]=='danger',a.alert())
# ---- staff assigns a task to vol one
a.get('/app/volunteers'); check('volunteers list shows both','Vol One' in a.last and 'Vol Two' in a.last)
vid=re.search(r'/app/volunteers\?id=(\d+)',[x for x in re.findall(r'<tr data-row>(.*?)</tr>',a.last,re.S) if 'Vol One' in x][0]).group(1)
a.get('/app/volunteers?id='+vid); check('volunteer detail',a.r.status_code==200 and 'Tasks (0)' in a.last)
a.post('/app/volunteers?id='+vid,{'action':'assignTask','id':vid,'_modal':'assignTask','taskName':'Boat patrol','description':'x','location':'Nilambur','priority':'4'})
check('assign task',a.alert() and a.alert()[0]=='success',a.alert())
a.post('/app/volunteers?id='+vid,{'action':'addSkill','id':vid,'skillName':'Swimming'}); check('add skill',a.alert() and a.alert()[0]=='success',a.alert())
a.post('/app/volunteers',{'action':'smartAssign','_modal':'smartAssign','taskName':'Medical camp','description':'','location':'Manjeri','taskSkills':'first aid','priority':'3'})
check('smart-assign picks a volunteer',a.alert() and a.alert()[0]=='success',a.alert()); print('   ->',a.alert()[1][:140])
# ---- volunteer sees own task, advances it
v1.get('/app/my-tasks'); check('volunteer sees own task','Boat patrol' in v1.last and 'Medical camp' not in v1.last or 'Boat patrol' in v1.last)
aid=re.search(r'name="assignmentId" value="(\d+)"',v1.last).group(1)
v1.post('/app/my-tasks',{'action':'taskStatus','assignmentId':aid,'status':'ACCEPTED'}); check('volunteer accepts own task',v1.alert() and v1.alert()[0]=='success',v1.alert())
v1.post('/app/my-tasks',{'action':'taskStatus','assignmentId':aid,'status':'ASSIGNED'}); check('backwards transition rejected',v1.alert() and v1.alert()[0]=='danger',v1.alert())
# ---- tamper: volunteer two tries to update vol one's task
v2.get('/app/my-tasks')
v2.post('/app/my-tasks',{'action':'taskStatus','assignmentId':aid,'status':'IN_PROGRESS'})
check('IDOR blocked: vol_two cannot update vol_one task',v2.alert() and v2.alert()[0]=='danger' and 'your own' in v2.alert()[1],v2.alert())
check('vol_two cannot see vol_one task','Boat patrol' not in v2.last)
st=mysql_status=None
# ---- shelters
a.get('/app/shelters'); check('shelters page',a.r.status_code==200 and 'Shelter Management' in a.last)
a.get('/app/disasters'); did=re.search(r'data-id="(\d+)" data-title="Kerala Flood Test"',a.last).group(1)
def shelter(code,name,cap,occ,wc='on'):
    a.get('/app/shelters'); d={'action':'create','_modal':'createShelter','name':name,'code':code,'district':'Malappuram','city':'Nilambur','address':'Main Rd','locationDescription':'','maxCapacity':str(cap),'occupancy':str(occ),'status':'AVAILABLE','contact':'9333333333','manager':'Mgr','disasterId':did,'wheelchair':wc,'elderly':'on'}
    a.post('/app/shelters',d); return a
shelter('SH-A','Nilambur School Camp',100,10); check('create shelter A',a.alert() and a.alert()[0]=='success',a.alert())
shelter('SH-B','Manjeri Hall',30,29,wc=''); check('create shelter B',a.alert() and a.alert()[0]=='success',a.alert())
a.post('/app/shelters',{'action':'create','_modal':'createShelter','name':'Bad','code':'SH-C','district':'d','city':'c','address':'a','maxCapacity':'10','occupancy':'50','status':'AVAILABLE','contact':'9333333333','manager':'m','disasterId':''})
check('occupancy > capacity rejected (validation)',a.alert() and a.alert()[0]=='danger',a.alert())
a.get('/app/shelters'); sid=re.search(r'/app/shelters\?id=(\d+)',[x for x in re.findall(r'<tr data-row>(.*?)</tr>',a.last,re.S) if 'SH-A' in x][0]).group(1)
a.get('/app/shelters?id='+sid); check('shelter detail with occupancy bar','rq-bar' in a.last and '10 / 100' in a.last)
a.post('/app/shelters?id='+sid,{'action':'addFacility','id':sid,'facilityName':'Drinking water','available':'on'}); check('add facility',a.alert() and a.alert()[0]=='success',a.alert())
a.post('/app/shelters?id='+sid,{'action':'occupancy','id':sid,'occupancy':'500'}); check('occupancy above capacity rejected',a.alert() and a.alert()[0]=='danger',a.alert())
a.post('/app/shelters?id='+sid,{'action':'occupancy','id':sid,'occupancy':'20'}); check('occupancy update ok',a.alert() and a.alert()[0]=='success' and '20 / 100' in a.last,a.alert())
# ---- smart allocation
a.get('/app/smart-allocation'); check('smart allocation page',a.r.status_code==200 and 'Allocate a family' in a.last)
base={'_modal':'none','victimId':'','familyName':'Nair family','people':'5','priority':'HIGH','location':'Nilambur','needWheelchair':'on'}
a.post('/app/smart-allocation',dict(base,action='preview')); check('preview ranks shelters',a.alert() and a.alert()[0]=='success' and 'Ranking preview' in a.last,a.alert())
t=re.sub(r'<[^>]+>|\s+',' ',a.last[a.last.find('tblPreview'):a.last.find('tblPreview')+3000]); print('   preview:',t[:230])
check('accessibility respected: wheelchair family -> SH-A (SH-B has no wheelchair access)','SH-A' in t and 'SH-B' not in t,t[:200])
a.post('/app/smart-allocation',dict(base,action='allocate'));check('allocate best shelter',a.alert() and a.alert()[0]=='success',a.alert()); print('   ->',a.alert()[1][:150])
a.post('/app/smart-allocation',dict(base,action='allocate',people='500')); check('over-capacity allocation prevented',a.alert() and a.alert()[0]=='danger',a.alert()); print('   ->',a.alert()[1][:150])
a.get('/app/shelters?id='+sid); check('shelter occupancy increased by allocation','25 / 100' in a.last,re.findall(r'\d+ / 100',a.last))
sys.exit(1 if summary() else 0)
