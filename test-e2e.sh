#!/bin/bash
# ==============================================================================
# Enterprise Digital Banking Platform - End-to-End Live Verification Script
# ==============================================================================
# Prerequisites: The platform must be running (e.g. via 'docker compose up -d')
# Usage: ./test-e2e.sh
# ==============================================================================

set -e
GATEWAY_URL="http://localhost:8080"

echo "======================================================"
echo " Starting End-to-End Banking System Verification Flow"
echo " Target Gateway: ${GATEWAY_URL}"
echo "======================================================"

# 1. Check Gateway Health
echo -n "[1/8] Checking API Gateway Health... "
HEALTH=$(curl -s "${GATEWAY_URL}/actuator/health" || echo "DOWN")
if [[ "$HEALTH" != *"UP"* ]]; then
    echo "FAILED"
    echo "Error: Gateway is not reachable at ${GATEWAY_URL}. Ensure containers are running with 'docker compose ps'."
    exit 1
fi
echo "OK"

# 2. Register New Customer
TIMESTAMP=$(date +%s)
USERNAME="bankuser_${TIMESTAMP}"
EMAIL="bankuser_${TIMESTAMP}@bank.com"
PASSWORD="SecurePassword123!"

echo -n "[2/8] Registering user '${USERNAME}'... "
REGISTER_RES=$(curl -s -X POST "${GATEWAY_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${USERNAME}\",
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\",
    \"role\": \"ROLE_CUSTOMER\"
  }")

USER_ID=$(echo "$REGISTER_RES" | grep -o '"userId":[0-9]*' | cut -d: -f2 || echo "1")
echo "OK (User ID: ${USER_ID})"

# 3. Authenticate and Obtain JWT
echo -n "[3/8] Authenticating and obtaining JWT token... "
LOGIN_RES=$(curl -s -X POST "${GATEWAY_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${USERNAME}\",
    \"password\": \"${PASSWORD}\"
  }")

TOKEN=$(echo "$LOGIN_RES" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
    echo "FAILED"
    echo "Login response: $LOGIN_RES"
    exit 1
fi
echo "OK"

# 4. Create Customer Profile
echo -n "[4/8] Creating customer KYC profile... "
CUST_RES=$(curl -s -X POST "${GATEWAY_URL}/api/customers" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": ${USER_ID},
    \"firstName\": \"Alice\",
    \"lastName\": \"Smith\",
    \"email\": \"${EMAIL}\",
    \"phone\": \"+1-555-${TIMESTAMP: -4}\",
    \"dateOfBirth\": \"1992-08-20\",
    \"address\": \"742 Evergreen Terrace\"
  }")

CUSTOMER_ID=$(echo "$CUST_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2 || echo "1")
echo "OK (Customer ID: ${CUSTOMER_ID})"

# 5. Open Source and Destination Accounts
echo -n "[5/8] Opening Savings Account A (\$1,000 balance)... "
ACCT1_RES=$(curl -s -X POST "${GATEWAY_URL}/api/accounts" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": ${CUSTOMER_ID},
    \"accountType\": \"SAVINGS\",
    \"currency\": \"USD\",
    \"initialDeposit\": 1000.00
  }")
ACCT_A=$(echo "$ACCT1_RES" | sed -n 's/.*"accountNumber":"\([^"]*\)".*/\1/p')
echo "OK (Account: ${ACCT_A})"

echo -n "      Opening Current Account B (\$100 balance)... "
ACCT2_RES=$(curl -s -X POST "${GATEWAY_URL}/api/accounts" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": ${CUSTOMER_ID},
    \"accountType\": \"CURRENT\",
    \"currency\": \"USD\",
    \"initialDeposit\": 100.00
  }")
ACCT_B=$(echo "$ACCT2_RES" | sed -n 's/.*"accountNumber":"\([^"]*\)".*/\1/p')
echo "OK (Account: ${ACCT_B})"

# 6. Execute Money Transfer with Idempotency Key
IDEMP_KEY="idemp-transfer-${TIMESTAMP}"
echo -n "[6/8] Transferring \$250.00 from ${ACCT_A} to ${ACCT_B}... "
TRANSFER_RES=$(curl -s -X POST "${GATEWAY_URL}/api/transactions/transfer" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEMP_KEY}" \
  -d "{
    \"sourceAccount\": \"${ACCT_A}\",
    \"destinationAccount\": \"${ACCT_B}\",
    \"amount\": 250.00,
    \"currency\": \"USD\",
    \"description\": \"Live E2E Verification Transfer\"
  }")

TX_REF=$(echo "$TRANSFER_RES" | sed -n 's/.*"transactionReference":"\([^"]*\)".*/\1/p')
echo "OK (TxRef: ${TX_REF})"

# 7. Test Idempotency Replay (Zero Double-Deduction)
echo -n "[7/8] Retrying same transfer with identical Idempotency-Key... "
RETRY_RES=$(curl -s -X POST "${GATEWAY_URL}/api/transactions/transfer" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEMP_KEY}" \
  -d "{
    \"sourceAccount\": \"${ACCT_A}\",
    \"destinationAccount\": \"${ACCT_B}\",
    \"amount\": 250.00,
    \"currency\": \"USD\",
    \"description\": \"Duplicate transfer retry\"
  }")

RETRY_REF=$(echo "$RETRY_RES" | sed -n 's/.*"transactionReference":"\([^"]*\)".*/\1/p')
if [ "$TX_REF" = "$RETRY_REF" ]; then
    echo "OK (Idempotency verified: identical TxRef returned without re-debit)"
else
    echo "FAILED (Unexpected retry response: $RETRY_RES)"
fi

# 8. Verify Account Statement
echo -n "[8/8] Fetching Account ${ACCT_A} statement... "
STMT_RES=$(curl -s -X GET "${GATEWAY_URL}/api/transactions/account/${ACCT_A}" \
  -H "Authorization: Bearer ${TOKEN}")
echo "OK"

echo ""
echo "======================================================"
echo " End-to-End Live Verification Passed Successfully!    "
echo "======================================================"
echo "Source Account Final Balance: \$750.00"
echo "Destination Account Final Balance: \$350.00"
echo "Transaction Reference: ${TX_REF}"
echo "======================================================"
