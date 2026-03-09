#!/bin/bash
# Vehicle API Test Script
set -e

BASE="http://localhost:5001"

echo "🔐 Logging in..."
RESP=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@dial112.in","password":"admin123"}')
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "✅ Token acquired"
echo ""

AUTH="Authorization: Bearer $TOKEN"

# ── TEST 1: All vehicles paginated ────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 1: All vehicles  (page 1, 20 per page)"
echo "══════════════════════════════════════════════════════"
curl -s "$BASE/api/portal/vehicles?page=1&limit=20" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'  Total: {d[\"total\"]}   Pages: {d[\"pages\"]}   Returned this page: {len(d[\"vehicles\"])}')
print()
print(f'  {\"PLATE\":<14} {\"OWNER\":<25} {\"TYPE\":<15} {\"MODEL\":<25} {\"COLOR\":<12} YEAR')
print('  ' + '-'*100)
for v in d['vehicles'][:10]:
    stolen = ' 🚨STOLEN' if v.get('isStolen') else (' 🔶SUSPECT' if v.get('isSuspected') else '')
    print(f'  {v[\"plateNumber\"]:<14} {v[\"ownerName\"]:<25} {v[\"vehicleType\"]:<15} {v[\"model\"]:<25} {v[\"color\"]:<12} {v.get(\"registrationYear\",\"N/A\")}{stolen}')
print(f'  ... ({d[\"total\"] - 10} more records in DB)')
"
echo ""

# ── TEST 2: All vehicles page 2 ───────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 2: Page 2"
echo "══════════════════════════════════════════════════════"
curl -s "$BASE/api/portal/vehicles?page=2&limit=20" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'  Page {d[\"page\"]} of {d[\"pages\"]}  |  Showing 5 of {len(d[\"vehicles\"])} records on this page:')
for v in d['vehicles'][:5]:
    print(f'  {v[\"plateNumber\"]:<14} {v[\"ownerName\"]:<25} {v[\"model\"]}')
"
echo ""

# ── TEST 3: Stolen vehicles ───────────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 3: Stolen vehicles only"
echo "══════════════════════════════════════════════════════"
curl -s "$BASE/api/portal/vehicles?isStolen=true&limit=50" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'  🚨 Total stolen: {d[\"total\"]}')
print()
for v in d['vehicles']:
    print(f'  🚨 {v[\"plateNumber\"]:<14} {v[\"ownerName\"]:<25} {v[\"vehicleType\"]:<15} {v[\"model\"]}')
"
echo ""

# ── TEST 4: Suspected vehicles ────────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 4: Suspected vehicles only"
echo "══════════════════════════════════════════════════════"
curl -s "$BASE/api/portal/vehicles?isSuspected=true&limit=50" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'  🔶 Total suspected: {d[\"total\"]}')
for v in d['vehicles']:
    print(f'  🔶 {v[\"plateNumber\"]:<14} {v[\"ownerName\"]:<25} {v[\"vehicleType\"]:<15} {v[\"model\"]}')
"
echo ""

# ── TEST 5: Filter by type ────────────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 5: Filter by vehicle type"
echo "══════════════════════════════════════════════════════"
for TYPE in FOUR_WHEELER TWO_WHEELER TRUCK BUS OTHER; do
  COUNT=$(curl -s "$BASE/api/portal/vehicles?vehicleType=$TYPE&limit=1" -H "$AUTH" | python3 -c "import sys,json; print(json.load(sys.stdin)['total'])")
  echo "  $TYPE : $COUNT records"
done
echo ""

# ── TEST 6: Search by name ────────────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 6: Search by owner name 'Kumar'"
echo "══════════════════════════════════════════════════════"
curl -s "$BASE/api/portal/vehicles?search=Kumar&limit=5" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'  Found: {d[\"total\"]} vehicles with Kumar in name/plate/model')
for v in d['vehicles'][:5]:
    print(f'  {v[\"plateNumber\"]:<14} {v[\"ownerName\"]:<25} {v[\"model\"]}')
"
echo ""

# ── TEST 7: Lookup specific plate ─────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 7: Lookup a specific plate number"
echo "══════════════════════════════════════════════════════"
# Get first stolen plate
PLATE=$(curl -s "$BASE/api/portal/vehicles?isStolen=true&limit=1" -H "$AUTH" | python3 -c "import sys,json; print(json.load(sys.stdin)['vehicles'][0]['plateNumber'])")
echo "  Looking up stolen plate: $PLATE"
curl -s "$BASE/api/portal/vehicles/plate/$PLATE" -H "$AUTH" | python3 -c "
import sys, json
d = json.load(sys.stdin)
v = d['vehicle']
print(f'  Plate   : {v[\"plateNumber\"]}')
print(f'  Owner   : {v[\"ownerName\"]}')
print(f'  Phone   : {v[\"ownerPhone\"]}')
print(f'  Type    : {v[\"vehicleType\"]}')
print(f'  Model   : {v[\"model\"]}')
print(f'  Color   : {v[\"color\"]}')
print(f'  Year    : {v.get(\"registrationYear\",\"N/A\")}')
print(f'  Stolen  : {v[\"isStolen\"]}')
print(f'  Suspect : {v[\"isSuspected\"]}')
"
echo ""

# ── TEST 8: 404 for unknown plate ─────────────────────────────────────────────
echo "══════════════════════════════════════════════════════"
echo " TEST 8: Lookup non-existent plate (expect 404)"
echo "══════════════════════════════════════════════════════"
curl -s "$BASE/api/portal/vehicles/plate/XX99ZZ0000" -H "$AUTH"
echo ""
echo ""

echo "✅ All tests complete."
