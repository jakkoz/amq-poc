#!/bin/bash

set -euo pipefail

WARMUP_REQUESTS="${WARMUP_REQUESTS:-100}"
BENCH_REQUESTS="${BENCH_REQUESTS:-1000}"

for i in $(seq 1 "$WARMUP_REQUESTS"); do
  curl -s -o /dev/null "http://localhost:8081/api/send?message=warmup" || exit 1
done

TIME_OUTPUT="$(
  /usr/bin/time -p sh -c "
for i in \$(seq 1 $BENCH_REQUESTS); do
  curl -s -o /dev/null \"http://localhost:8081/api/send?message=bench\" || exit 1
done
" 2>&1
)"

echo "$TIME_OUTPUT"

REAL_SECONDS="$(printf '%s\n' "$TIME_OUTPUT" | awk '/^real / { print $2 }')"
THROUGHPUT="$(awk -v n="$BENCH_REQUESTS" -v t="$REAL_SECONDS" 'BEGIN { if (t > 0) printf "%.2f", n / t; else print "inf" }')"

echo "throughput ${THROUGHPUT} req/s (${BENCH_REQUESTS} requests)"
