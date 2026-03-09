#!/bin/bash
# =============================================================================
# test_all_endpoints.sh - Comprehensive API Testing Script
# =============================================================================

BASE_URL="http://localhost:5001"
AI_URL="http://localhost:8000"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
PASS=0
FAIL=0
WARN=0

log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; ((PASS++)); }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; ((FAIL++)); }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; ((WARN++)); }

echo "============================================"
echo " DIAL-112 FULL API TEST SUITE"
echo "============================================"
echo ""

# =============================================
# 1. HEALTH CHECK
# =============================================
echo "--- 1. HEALTH CHECKS ---"

# Backend health
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/health")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "Backend health check ($HTTP_CODE)"
else
    log_fail "Backend health check ($HTTP_CODE): $BODY"
fi

# AI health
RESP=$(curl -s -w "\n%{http_code}" "$AI_URL/health")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "AI service health check ($HTTP_CODE)"
else
    log_fail "AI service health check ($HTTP_CODE)"
fi

# =============================================
# 2. AUTH - REGISTER
# =============================================
echo ""
echo "--- 2. AUTH ENDPOINTS ---"

# Register a citizen
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"name":"Test Citizen","email":"citizen@test.com","password":"test123456","phone":"9876543210","role":"citizen"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "409" ]; then
    log_pass "Register citizen ($HTTP_CODE)"
else
    log_fail "Register citizen ($HTTP_CODE): $BODY"
fi

# Register a police officer
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"name":"Test Officer","email":"officer@test.com","password":"test123456","phone":"9876543211","role":"police","badgeId":"BADGE001","station":"Central Station"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "409" ]; then
    log_pass "Register police officer ($HTTP_CODE)"
else
    log_fail "Register police officer ($HTTP_CODE): $BODY"
fi

# Register - missing fields
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{"name":"No Email"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "400" ]; then
    log_pass "Register validation (missing fields returns 400)"
else
    log_fail "Register validation ($HTTP_CODE)"
fi

# =============================================
# 3. AUTH - LOGIN
# =============================================

# Login citizen
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"citizen@test.com","password":"test123456"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    CITIZEN_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
    if [ -n "$CITIZEN_TOKEN" ]; then
        log_pass "Login citizen ($HTTP_CODE) - Token received"
    else
        log_fail "Login citizen ($HTTP_CODE) - No token in response"
    fi
else
    log_fail "Login citizen ($HTTP_CODE): $BODY"
fi

# Login police
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"officer@test.com","password":"test123456"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    POLICE_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
    if [ -n "$POLICE_TOKEN" ]; then
        log_pass "Login police ($HTTP_CODE) - Token received"
    else
        log_fail "Login police ($HTTP_CODE) - No token"
    fi
else
    log_fail "Login police ($HTTP_CODE): $BODY"
fi

# Login with wrong password
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"citizen@test.com","password":"wrongpassword"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "401" ]; then
    log_pass "Login wrong password (returns 401)"
else
    log_fail "Login wrong password ($HTTP_CODE)"
fi

# Login with missing fields
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{}')
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "400" ]; then
    log_pass "Login missing fields (returns 400)"
else
    log_fail "Login missing fields ($HTTP_CODE)"
fi

# =============================================
# 4. AUTH - PROFILE (Protected)
# =============================================

# Get profile with valid token
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/auth/profile" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "Get profile with valid token ($HTTP_CODE)"
else
    log_fail "Get profile ($HTTP_CODE)"
fi

# Get profile without token
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/auth/profile")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "401" ]; then
    log_pass "Get profile without token (returns 401)"
else
    log_fail "Get profile without token ($HTTP_CODE)"
fi

# Get profile with invalid token
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/auth/profile" \
    -H "Authorization: Bearer invalidtoken123")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "401" ]; then
    log_pass "Get profile with invalid token (returns 401)"
else
    log_fail "Get profile with invalid token ($HTTP_CODE)"
fi

# =============================================
# 5. SOS ENDPOINTS
# =============================================
echo ""
echo "--- 3. SOS ENDPOINTS ---"

# Trigger SOS
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/sos" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CITIZEN_TOKEN" \
    -d '{"latitude":28.6139,"longitude":77.2090,"address":"India Gate, Delhi","type":"SOS"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "201" ]; then
    SOS_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['sosId'])" 2>/dev/null)
    log_pass "Trigger SOS ($HTTP_CODE) - sosId: $SOS_ID"
else
    log_fail "Trigger SOS ($HTTP_CODE): $BODY"
fi

# SOS without auth
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/sos" \
    -H "Content-Type: application/json" \
    -d '{"latitude":28.6139,"longitude":77.2090}')
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "401" ]; then
    log_pass "SOS without auth (returns 401)"
else
    log_fail "SOS without auth ($HTTP_CODE)"
fi

# SOS without coordinates
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/sos" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CITIZEN_TOKEN" \
    -d '{"address":"somewhere"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "400" ]; then
    log_pass "SOS without coords (returns 400)"
else
    log_fail "SOS without coords ($HTTP_CODE)"
fi

# SOS history
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/sos/history" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "SOS history ($HTTP_CODE)"
else
    log_fail "SOS history ($HTTP_CODE)"
fi

# =============================================
# 6. CASES ENDPOINTS
# =============================================
echo ""
echo "--- 4. CASES ENDPOINTS ---"

# File new FIR (citizen)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/cases" \
    -H "Authorization: Bearer $CITIZEN_TOKEN" \
    -F "title=Stolen Phone" \
    -F "description=My phone was stolen at India Gate bus stop around 3pm today. Samsung Galaxy S24 Ultra." \
    -F "category=THEFT" \
    -F "latitude=28.6139" \
    -F "longitude=77.2090" \
    -F "address=India Gate Bus Stop, Delhi")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "201" ]; then
    CASE_ID=$(echo "$BODY" | python3 -c "import sys,json; r=json.load(sys.stdin); print(r.get('case',{}).get('_id',''))" 2>/dev/null)
    log_pass "File FIR ($HTTP_CODE) - caseId: $CASE_ID"
else
    log_fail "File FIR ($HTTP_CODE): $BODY"
fi

# File FIR missing fields
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/cases" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CITIZEN_TOKEN" \
    -d '{"title":"Incomplete"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "400" ]; then
    log_pass "File FIR validation (missing fields returns 400)"
else
    log_fail "File FIR validation ($HTTP_CODE)"
fi

# Get user's cases
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/cases" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    CASE_COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('total',0))" 2>/dev/null)
    log_pass "Get user cases ($HTTP_CODE) - total: $CASE_COUNT"
else
    log_fail "Get user cases ($HTTP_CODE): $BODY"
fi

# Get single case by ID
if [ -n "$CASE_ID" ]; then
    RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/cases/$CASE_ID" \
        -H "Authorization: Bearer $CITIZEN_TOKEN")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "Get case by ID ($HTTP_CODE)"
    else
        log_fail "Get case by ID ($HTTP_CODE)"
    fi

    # Update case status (police only)
    RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/cases/$CASE_ID" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $POLICE_TOKEN" \
        -d '{"status":"INVESTIGATING","notes":"Officer assigned, investigation started"}')
    HTTP_CODE=$(echo "$RESP" | tail -1)
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "Update case status - police ($HTTP_CODE)"
    else
        log_fail "Update case status - police ($HTTP_CODE)"
    fi

    # Citizen tries to update (should fail)
    RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/cases/$CASE_ID" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $CITIZEN_TOKEN" \
        -d '{"status":"RESOLVED"}')
    HTTP_CODE=$(echo "$RESP" | tail -1)
    if [ "$HTTP_CODE" = "403" ]; then
        log_pass "Citizen cannot update case (returns 403)"
    else
        log_fail "Citizen update case RBAC ($HTTP_CODE)"
    fi
fi

# =============================================
# 7. VEHICLE LOOKUP
# =============================================
echo ""
echo "--- 5. VEHICLE LOOKUP ---"

# Vehicle not found
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/vehicles/DL01AB1234" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "404" ]; then
    log_pass "Vehicle not found (returns 404)"
else
    log_fail "Vehicle lookup ($HTTP_CODE)"
fi

# Vehicle invalid plate
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/vehicles/AB" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "400" ]; then
    log_pass "Vehicle invalid plate (returns 400)"
else
    log_fail "Vehicle invalid plate ($HTTP_CODE)"
fi

# Vehicle without auth
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/vehicles/DL01AB1234")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "401" ]; then
    log_pass "Vehicle without auth (returns 401)"
else
    log_fail "Vehicle without auth ($HTTP_CODE)"
fi

# =============================================
# 8. AI ENDPOINTS (direct)
# =============================================
echo ""
echo "--- 6. AI ENDPOINTS (Direct) ---"

# AI Chat
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AI_URL/ai/chat" \
    -H "Content-Type: application/json" \
    -d '{"message":"I need help, there is an emergency!","sessionId":"test-session"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "AI Chat direct ($HTTP_CODE)"
else
    log_fail "AI Chat direct ($HTTP_CODE): $BODY"
fi

# AI Classify complaint
RESP=$(curl -s -w "\n%{http_code}" -X POST "$AI_URL/ai/classify-complaint" \
    -H "Content-Type: application/json" \
    -d '{"text":"Someone stole my wallet at the market while I was shopping yesterday afternoon"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    CATEGORY=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('category',''))" 2>/dev/null)
    log_pass "AI Classify complaint ($HTTP_CODE) - Category: $CATEGORY"
else
    log_fail "AI Classify complaint ($HTTP_CODE): $BODY"
fi

# AI Hotspots
RESP=$(curl -s -w "\n%{http_code}" "$AI_URL/ai/hotspots")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "AI Hotspots ($HTTP_CODE)"
else
    log_fail "AI Hotspots ($HTTP_CODE): $BODY"
fi

# =============================================
# 9. AI ENDPOINTS (via backend proxy)
# =============================================
echo ""
echo "--- 7. AI ENDPOINTS (via Backend Proxy) ---"

# Chat via backend
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/chat" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CITIZEN_TOKEN" \
    -d '{"message":"How do I file an FIR?","sessionId":"backend-test"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "AI Chat via backend ($HTTP_CODE)"
else
    log_fail "AI Chat via backend ($HTTP_CODE): $BODY"
fi

# Classify via backend
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/classify-complaint" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $CITIZEN_TOKEN" \
    -d '{"text":"A group of people attacked me near the park with knives and robbed my belongings"}')
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
if [ "$HTTP_CODE" = "200" ]; then
    CATEGORY=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('category',''))" 2>/dev/null)
    log_pass "AI Classify via backend ($HTTP_CODE) - Category: $CATEGORY"
else
    log_fail "AI Classify via backend ($HTTP_CODE): $BODY"
fi

# Hotspots via backend
RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/ai/hotspots" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
    log_pass "AI Hotspots via backend ($HTTP_CODE)"
else
    log_fail "AI Hotspots via backend ($HTTP_CODE)"
fi

# Face recognition without image (police only)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/face-recognition" \
    -H "Authorization: Bearer $POLICE_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "400" ]; then
    log_pass "Face recognition missing image (returns 400)"
else
    log_fail "Face recognition missing image ($HTTP_CODE)"
fi

# Face recognition citizen denied (RBAC)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/face-recognition" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "403" ]; then
    log_pass "Face recognition RBAC citizen denied (returns 403)"
else
    log_fail "Face recognition RBAC ($HTTP_CODE)"
fi

# Weapon detection citizen denied (RBAC)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/detect-weapon" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "403" ]; then
    log_pass "Weapon detection RBAC citizen denied (returns 403)"
else
    log_fail "Weapon detection RBAC ($HTTP_CODE)"
fi

# ANPR citizen denied (RBAC)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/anpr" \
    -H "Authorization: Bearer $CITIZEN_TOKEN")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "403" ]; then
    log_pass "ANPR RBAC citizen denied (returns 403)"
else
    log_fail "ANPR RBAC ($HTTP_CODE)"
fi

# =============================================
# 10. 404 HANDLING
# =============================================
echo ""
echo "--- 8. ERROR HANDLING ---"

RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/nonexistent")
HTTP_CODE=$(echo "$RESP" | tail -1)
if [ "$HTTP_CODE" = "404" ]; then
    log_pass "404 for unknown route ($HTTP_CODE)"
else
    log_fail "404 handling ($HTTP_CODE)"
fi

# =============================================
# IMAGE-BASED AI TESTS (using a generated test image)
# =============================================
echo ""
echo "--- 9. IMAGE AI TESTS ---"

# Create a small test image (1x1 black pixel JPEG)
python3 -c "
from PIL import Image
import io
img = Image.new('RGB', (100, 100), color='red')
img.save('/tmp/test_image.jpg', 'JPEG')
print('Test image created')
" 2>/dev/null

if [ -f /tmp/test_image.jpg ]; then
    # Face recognition with image (via AI direct)
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$AI_URL/ai/face-recognition" \
        -F "file=@/tmp/test_image.jpg")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "AI Face recognition with image ($HTTP_CODE)"
    else
        log_warn "AI Face recognition ($HTTP_CODE): $BODY"
    fi

    # ANPR with image (via AI direct)
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$AI_URL/ai/anpr" \
        -F "file=@/tmp/test_image.jpg")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "AI ANPR with image ($HTTP_CODE)"
    else
        log_warn "AI ANPR ($HTTP_CODE): $BODY"
    fi

    # Weapon detection with image (via AI direct)
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$AI_URL/ai/detect-weapon" \
        -F "file=@/tmp/test_image.jpg")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "AI Weapon detection with image ($HTTP_CODE)"
    else
        log_warn "AI Weapon detection ($HTTP_CODE): $BODY"
    fi

    # Weapon detection with image via backend (police token)
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/detect-weapon" \
        -H "Authorization: Bearer $POLICE_TOKEN" \
        -F "image=@/tmp/test_image.jpg")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "AI Weapon detect via backend ($HTTP_CODE)"
    else
        log_warn "AI Weapon detect via backend ($HTTP_CODE): $BODY"
    fi

    # ANPR via backend (police token)
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/anpr" \
        -H "Authorization: Bearer $POLICE_TOKEN" \
        -F "image=@/tmp/test_image.jpg")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "AI ANPR via backend (police) ($HTTP_CODE)"
    else
        log_warn "AI ANPR via backend ($HTTP_CODE): $BODY"
    fi

    # Face recognition via backend (police token)
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/ai/face-recognition" \
        -H "Authorization: Bearer $POLICE_TOKEN" \
        -F "image=@/tmp/test_image.jpg")
    HTTP_CODE=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | sed '$d')
    if [ "$HTTP_CODE" = "200" ]; then
        log_pass "AI Face recognition via backend ($HTTP_CODE)"
    else
        log_warn "AI Face recognition via backend ($HTTP_CODE): $BODY"
    fi

    rm -f /tmp/test_image.jpg
else
    log_warn "Could not create test image (Pillow not available)"
fi

# =============================================
# SUMMARY
# =============================================
echo ""
echo "============================================"
echo " TEST RESULTS"
echo "============================================"
echo -e "${GREEN}PASSED: $PASS${NC}"
echo -e "${RED}FAILED: $FAIL${NC}"
echo -e "${YELLOW}WARNINGS: $WARN${NC}"
echo "TOTAL: $((PASS + FAIL + WARN))"
echo "============================================"
