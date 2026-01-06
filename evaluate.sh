#!/bin/bash
set -e

BASE_COMMIT=$1
RUN_NAME=$2

if [ -z "$BASE_COMMIT" ] || [ -z "$RUN_NAME" ]; then
  echo "Usage: ./evaluate.sh <base_commit> <run_name>"
  echo "Example: ./evaluate.sh abc123 human"
  exit 1
fi

OUT_DIR="evaluation/$RUN_NAME"
mkdir -p "$OUT_DIR"

echo "Using base commit: $BASE_COMMIT"
echo "Run name: $RUN_NAME"
echo

# Ensure clean state
git reset --hard
git clean -fd

echo "Checking out base commit for BEFORE logs..."
git checkout "$BASE_COMMIT"

echo "Running tests (BEFORE)..."
mvn clean test > "$OUT_DIR/test_before.log" 2>&1 || true

echo "Running SpotBugs (BEFORE)..."
mvn spotbugs:check > "$OUT_DIR/spotbugs_before.log" 2>&1 || true

echo "Returning to current working tree..."
git checkout -

echo "Running tests (AFTER)..."
mvn clean test > "$OUT_DIR/test_after.log" 2>&1 || true

echo "Running SpotBugs (AFTER)..."
mvn spotbugs