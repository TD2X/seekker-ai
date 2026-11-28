#!/usr/bin/env bash
# One-command check of the whole backend (api-service + document-worker).
#   bash scripts/test-all.sh
#
# What it does:
#   1. loads the variables it needs from .env.local (repo root): AWS_*, S3_*, DB_*
#   2. stops early, saying which variable is missing
#   3. runs ALL tests of both services, including the *IT tests that talk to the real database and S3
#   4. prints one summary line per test class, and fails if anything failed or was skipped
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/.env.local}"

# --- 1. load only the variables we need (do NOT `source` the file: Neon URLs contain & characters)
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line; do
    key="${line%%=*}"
    val="${line#*=}"
    val="${val#\"}"; val="${val%\"}"; val="${val#\'}"; val="${val%\'}"
    export "$key=$val"
  done < <(grep -E '^(AWS_[A-Z0-9_]+|S3_[A-Z_]+|DB_[A-Z_]+)=' "$ENV_FILE")
else
  echo "note: $ENV_FILE not found, using the variables already set in this terminal"
fi

# --- 2. check the required variables (DB_PASSWORD may be empty, but it must be set)
missing=0
for v in DB_HOST DB_USERNAME DB_PASSWORD AWS_ENDPOINT_URL_S3 AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY; do
  if [ -z "${!v+x}" ]; then echo "MISSING variable: $v"; missing=1; fi
done
if [ "$missing" -ne 0 ]; then
  echo "Put the missing values in $ENV_FILE (see .env.example) and run again."
  exit 2
fi

# --- 3. run both services
status=0
for module in apps/api-service apps/workers/document-worker; do
  echo
  echo "=== $module"
  ( cd "$ROOT/$module" && ./mvnw -q test -Dtest='*IT,*Tests' -Dsurefire.failIfNoSpecifiedTests=false ) || status=1
done

# --- 4. summary
echo
echo "=== SUMMARY"
for module in apps/api-service apps/workers/document-worker; do
  for report in "$ROOT/$module"/target/surefire-reports/*.txt; do
    [ -f "$report" ] || continue
    line="$(grep -m1 'Tests run' "$report")"
    echo "$(basename "$report" .txt | sed 's/.*\.//'): ${line%% -- in*}"
    case "$line" in
      *"Skipped: 0"*) ;;
      *) echo "  ^ some tests were SKIPPED: a variable is missing or empty"; status=1 ;;
    esac
  done
done

if [ "$status" -eq 0 ]; then echo "ALL GOOD"; else echo "SOMETHING FAILED (see above)"; fi
exit "$status"
