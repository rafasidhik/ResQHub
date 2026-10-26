#!/usr/bin/env python3
"""Runs the HTTP test scripts in order against a RUNNING ResQHub on a FRESH, EMPTY database.

    RESQHUB_URL=http://localhost:8080/resqhub  python3 tests/http/run_all.py

The scripts create their own users and records, so use a throw-away database
(never your real one). 07_security_and_concurrency.py reads/changes a few rows
through the mysql command line; set RESQHUB_MYSQL="mysql -N -u USER -pPASS resqhub"
if root-socket access is not available. Requires:  pip install requests
"""
import os, subprocess, sys
here = os.path.dirname(os.path.abspath(__file__))
order = ["01_auth.py", "02_disasters_victims.py", "03_rescue_teams_requests.py",
         "04_volunteers_shelters_allocation.py", "05_resources_food_donations_reports.py",
         "06_report_privacy.py", "07_security_and_concurrency.py",
         "09_concurrent_stock.py", "10_concurrent_shelter.py", "11_concurrent_team_assignment.py"]
failed = 0
for name in order:
    r = subprocess.run([sys.executable, os.path.join(here, name)], capture_output=True, text=True)
    passed = r.stdout.count("  PASS"); bad = r.stdout.count("  FAIL")
    print(f"{name:45s} {passed:3d} passed, {bad} failed" + ("  (script error!)" if r.returncode not in (0, 1) else ""))
    if bad or r.returncode not in (0, 1):
        failed += 1; print(r.stdout[-1500:]); print(r.stderr[-800:])
sys.exit(1 if failed else 0)
