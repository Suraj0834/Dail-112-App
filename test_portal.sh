#!/bin/bash
# =============================================================================
# Portal API Test Suite - Tests all portal endpoints
# =============================================================================

BASE="http://localhost:5001/api"
TOKEN=$(cat /tmp/dial112_token.txt)
PASS=0
FAIL=0
TOTAL=0

test_endpoint() {
    local method=$1
    local url=$2
    local data=$3
    local expect=$4
    local desc=$5
    TOTAL=$((TOTAL+1))

    if [ "$method" = "GET" ]; then
        RESP=$(curl -s -w "\n%{http_code}" "$BASE$url" -H "Authorization: Bearer $TOKEN")
    elif [ "$method" = "POST" ]; then
        RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE$url" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data")
    elif [ "$method" = "PUT" ]; then
        RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE$url" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data")
    elif [ "$method" = "PATCH" ]; then
        RESP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE$url" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data")
    elif [ "$method" = "DELETE" ]; then
        RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE$url" -H "Authorization: Bearer $TOKEN")
    fi

    HTTP_CODE=$(echo "$RESP" | tail -n1)
    BODY=$(echo "$RESP" | sed '$d')

    if echo "$HTTP_CODE" | grep -qE "^${expect}"; then
        echo "  ✅ $desc (HTTP $HTTP_CODE)"
        PASS=$((PASS+1))
    else
        echo "  ❌ $desc (HTTP $HTTP_CODE, expected $expect)"
        echo "     Response: $(echo $BODY | head -c 200)"
        FAIL=$((FAIL+1))
    fi
}

echo "═══════════════════════════════════════════════════════════"
echo "  DIAL-112 PORTAL API TEST SUITE"
echo "═══════════════════════════════════════════════════════════"
echo ""

# ── 1. Dashboard ──────────────────────────────────────────────
echo "📊 Dashboard Endpoints"
test_endpoint GET "/portal/dashboard/stats" "" "2" "Dashboard stats"
test_endpoint GET "/portal/dashboard/trends" "" "2" "Monthly trends"
test_endpoint GET "/portal/dashboard/crime-distribution" "" "2" "Crime distribution"
echo ""

# ── 2. SOS ────────────────────────────────────────────────────
echo "🚨 SOS Endpoints"
test_endpoint GET "/portal/sos" "" "2" "SOS list"
test_endpoint GET "/portal/sos?status=ACTIVE" "" "2" "SOS list filtered"
echo ""

# ── 3. Cases ──────────────────────────────────────────────────
echo "📁 Cases Endpoints"
test_endpoint GET "/portal/cases" "" "2" "Cases list"
test_endpoint GET "/portal/cases?status=PENDING" "" "2" "Cases filtered by status"
test_endpoint GET "/portal/cases?category=THEFT" "" "2" "Cases filtered by category"
test_endpoint GET "/portal/cases?search=test" "" "2" "Cases search"
echo ""

# ── 4. Police ─────────────────────────────────────────────────
echo "👮 Police Endpoints"
test_endpoint GET "/portal/police" "" "2" "Police list"
# Create a test officer
test_endpoint POST "/portal/police" '{"name":"Test Officer","email":"testofficer@dial112.in","password":"test1234","phone":"8888888888","badgeId":"TEST001","station":"Test Station","rank":"Constable"}' "2" "Create police officer"
echo ""

# ── 5. Criminals ──────────────────────────────────────────────
echo "🔍 Criminals Endpoints"
test_endpoint GET "/portal/criminals" "" "2" "Criminals list"
test_endpoint POST "/portal/criminals" '' "4" "Criminal create (no data → 400)"
echo ""

# ── 6. Vehicles ──────────────────────────────────────────────
echo "🚗 Vehicles Endpoints"
test_endpoint GET "/portal/vehicles" "" "2" "Vehicles list"
test_endpoint GET "/portal/vehicles?isStolen=true" "" "2" "Vehicles - stolen filter"
test_endpoint GET "/portal/vehicles/plate/XX00XX0000" "" "4" "Vehicle plate lookup (404)"
test_endpoint POST "/portal/vehicles" '{"plateNumber":"DL01AB1234","ownerName":"Test Owner","ownerPhone":"7777777777","vehicleType":"FOUR_WHEELER","model":"Test Car","color":"White"}' "2" "Create vehicle"
echo ""

# ── 7. Roles ──────────────────────────────────────────────────
echo "🔐 Roles Endpoints"
test_endpoint GET "/portal/roles" "" "2" "Roles list"
test_endpoint POST "/portal/roles" '{"name":"test_role","displayName":"Test Role","description":"A test role","permissions":["dashboard.view","cases.view"]}' "2" "Create role"
test_endpoint GET "/portal/roles/permissions/all" "" "2" "All permissions list"
echo ""

# ── 8. Auth ───────────────────────────────────────────────────
echo "🔑 Auth Endpoints"
test_endpoint GET "/auth/profile" "" "2" "Get profile"
test_endpoint POST "/auth/login" '{"email":"admin@dial112.in","password":"admin123"}' "2" "Login"
echo ""

# ── 9. Unauthorized Access ────────────────────────────────────
echo "🔒 Security Tests"
TOTAL=$((TOTAL+1))
RESP=$(curl -s -w "\n%{http_code}" "$BASE/portal/dashboard/stats")
HTTP_CODE=$(echo "$RESP" | tail -n1)
if [ "$HTTP_CODE" = "401" ]; then
    echo "  ✅ Unauthenticated → 401 (HTTP $HTTP_CODE)"
    PASS=$((PASS+1))
else
    echo "  ❌ Unauthenticated should be 401 (HTTP $HTTP_CODE)"
    FAIL=$((FAIL+1))
fi
echo ""

# ── Summary ───────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════"
echo "  RESULTS: $PASS/$TOTAL passed, $FAIL failed"
echo "═══════════════════════════════════════════════════════════"
