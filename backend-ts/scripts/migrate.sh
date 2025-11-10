#!/bin/bash

set -e

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

if [ -z "$DATABASE_URL" ]; then
  echo "❌ DATABASE_URL not set"
  exit 1
fi

MIGRATIONS_DIR="./migrations"
MIGRATION_TABLE="migration_history"

echo "🚀 Starting migrations..."
echo ""

psql "$DATABASE_URL" -c "
CREATE TABLE IF NOT EXISTS $MIGRATION_TABLE (
  id SERIAL PRIMARY KEY,
  migration_name VARCHAR(255) NOT NULL UNIQUE,
  executed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_migration_history_name ON $MIGRATION_TABLE(migration_name);
" > /dev/null 2>&1

echo "✅ Migration tracking table ready"
echo ""

executed_migrations=$(psql "$DATABASE_URL" -t -c "SELECT migration_name FROM $MIGRATION_TABLE ORDER BY migration_name;" 2>/dev/null || echo "")

pending_count=0

for file in "$MIGRATIONS_DIR"/*.sql; do
  [ -e "$file" ] || continue

  filename=$(basename "$file")

  [ "$filename" = "000_migration_tracker.sql" ] && continue

  if echo "$executed_migrations" | grep -q "$filename"; then
    continue
  fi

  ((pending_count++))

  echo "📝 Executing: $filename"

  psql "$DATABASE_URL" -f "$file" > /dev/null

  psql "$DATABASE_URL" -c "INSERT INTO $MIGRATION_TABLE (migration_name) VALUES ('$filename');" > /dev/null

  echo "✅ Completed: $filename"
  echo ""
done

if [ $pending_count -eq 0 ]; then
  echo "✨ No pending migrations"
else
  echo "✨ All migrations completed successfully!"
fi
