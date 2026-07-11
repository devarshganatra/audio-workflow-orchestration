#!/bin/bash
# ============================================================
# Shruti Load Test Script
# ============================================================
# Sends concurrent audio workflow requests to stress test the
# distributed pipeline (RabbitMQ, Redis, Postgres, MinIO).
#
# Prerequisites: The app must be running with --spring.profiles.active=loadtest
# Usage: ./loadtest.sh [CONCURRENT_USERS] [TOTAL_REQUESTS]
# ============================================================

BASE_URL="http://localhost:8080"
CONCURRENT=${1:-10}
TOTAL=${2:-50}
RESULTS_FILE="/tmp/shruti-loadtest-results.txt"

# Pre-flight check: is the app running?
echo "Checking if Shruti is running at $BASE_URL..."
HTTP_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL" 2>/dev/null || true)
if [ "$HTTP_CHECK" = "000" ]; then
    echo "ERROR: Cannot reach $BASE_URL. Is the app running?"
    echo ""
    echo "Start infrastructure:  docker compose up -d postgres rabbitmq redis minio"
    echo "Then start the app in IntelliJ with: --spring.profiles.active=loadtest"
    exit 1
fi
echo "OK - Server is reachable."
echo ""

# Create a small dummy audio file for uploads
DUMMY_FILE="/tmp/shruti-loadtest.wav"
if [ ! -f "$DUMMY_FILE" ]; then
    echo "Creating dummy audio file..."
    dd if=/dev/urandom of="$DUMMY_FILE" bs=1024 count=64 2>/dev/null
fi

echo "============================================"
echo "  Shruti Load Test"
echo "============================================"
echo "  Target:       $BASE_URL"
echo "  Concurrency:  $CONCURRENT"
echo "  Total:        $TOTAL requests"
echo "============================================"
echo ""

# Clear previous results
> "$RESULTS_FILE"

# ---- Phase 1: Measure API Response Time (POST /api/v1/workflows) ----
echo "[Phase 1] Sending $TOTAL concurrent upload requests..."
echo ""

START_TIME=$(python3 -c 'import time; print(int(time.time() * 1000))')

for i in $(seq 1 "$TOTAL"); do
    (
        RESPONSE=$(curl -s -w "\n%{http_code} %{time_total}" \
            -X POST \
            -F "file=@${DUMMY_FILE};filename=test-${i}.wav" \
            "${BASE_URL}/api/v1/workflows" 2>/dev/null)

        LAST_LINE=$(echo "$RESPONSE" | tail -1)
        HTTP_CODE=$(echo "$LAST_LINE" | awk '{print $1}')
        TIME_TOTAL=$(echo "$LAST_LINE" | awk '{print $2}')

        if [ "$HTTP_CODE" = "202" ]; then
            echo "OK $i ${TIME_TOTAL}s" >> "$RESULTS_FILE"
            echo "OK $i ${TIME_TOTAL}s"
        else
            echo "FAIL $i ${TIME_TOTAL}s $HTTP_CODE" >> "$RESULTS_FILE"
            echo "FAIL $i ${TIME_TOTAL}s $HTTP_CODE"
        fi
    ) &

    # Throttle concurrency
    if (( i % CONCURRENT == 0 )); then
        wait
    fi
done
wait

END_TIME=$(python3 -c 'import time; print(int(time.time() * 1000))')
ELAPSED_MS=$(( END_TIME - START_TIME ))

echo ""
echo "============================================"
echo "  Results Summary"
echo "============================================"

TOTAL_OK=$(grep -c "^OK" "$RESULTS_FILE" || true)
TOTAL_FAIL=$(grep -c "^FAIL" "$RESULTS_FILE" || true)

# Calculate response time stats
if [ "$TOTAL_OK" -gt 0 ] 2>/dev/null; then
    AVG_TIME=$(grep "^OK" "$RESULTS_FILE" | awk '{gsub(/s$/,"",$3); sum += $3} END {printf "%.3fs", sum/NR}')
    MIN_TIME=$(grep "^OK" "$RESULTS_FILE" | awk '{gsub(/s$/,"",$3); print $3}' | sort -n | head -1)s
    MAX_TIME=$(grep "^OK" "$RESULTS_FILE" | awk '{gsub(/s$/,"",$3); print $3}' | sort -n | tail -1)s
    P95_LINE=$(( (TOTAL_OK * 95 + 99) / 100 ))
    P95_TIME=$(grep "^OK" "$RESULTS_FILE" | awk '{gsub(/s$/,"",$3); print $3}' | sort -n | sed -n "${P95_LINE}p")s
else
    AVG_TIME="N/A"
    MIN_TIME="N/A"
    MAX_TIME="N/A"
    P95_TIME="N/A"
fi

if [ "$ELAPSED_MS" -gt 0 ] 2>/dev/null; then
    THROUGHPUT=$(python3 -c "print(f'{$TOTAL_OK * 1000 / $ELAPSED_MS:.2f}')")
else
    THROUGHPUT="N/A"
fi

echo "  Successful:    $TOTAL_OK / $TOTAL"
echo "  Failed:        $TOTAL_FAIL / $TOTAL"
echo "  Total Time:    ${ELAPSED_MS}ms"
echo "  Throughput:    ${THROUGHPUT} req/s"
echo ""
echo "  Response Times (API latency only):"
echo "    Min:         ${MIN_TIME}"
echo "    Avg:         ${AVG_TIME}"
echo "    P95:         ${P95_TIME}"
echo "    Max:         ${MAX_TIME}"
echo "============================================"

# ---- Phase 2: Wait for workflows to complete ----
WAIT_SECS=45
echo ""
echo "[Phase 2] Waiting ${WAIT_SECS}s for workflows to complete through the pipeline..."
sleep "$WAIT_SECS"

# ---- Phase 3: Collect Metrics ----
echo ""
echo "[Phase 3] Collecting Micrometer metrics..."
echo ""

ANALYTICS=$(curl -s "$BASE_URL/api/v1/analytics")

echo "============================================"
echo "  Pipeline Metrics (from Micrometer)"
echo "============================================"
echo "$ANALYTICS" | python3 -m json.tool 2>/dev/null || echo "$ANALYTICS"
echo "============================================"
echo ""
echo "Load test complete."
