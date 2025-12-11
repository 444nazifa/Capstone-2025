#!/usr/bin/env zsh
# Script: commit-medication-startdate.sh
# Purpose: Create a feature branch and split changes into up to three commits:
#   1) UI changes (commonMain)
#   2) Platform defensive changes (androidMain + iosMain)
#   3) Other changes (scripts, build files)
# The script stages only files that have modifications and will skip commits if nothing to commit for that group.

set -euo pipefail

REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$REPO_ROOT"

BRANCH_NAME="feature/medication-start-date"

# Commit messages
UI_MSG_TITLE="ui: default medication start_date to today in AddMedicationBottomSheet"
UI_MSG_BODY="Ensure the created CreateMedicationRequest includes a start_date (yyyy-MM-dd) so the home calendar won't display past dots."

PLATFORM_MSG_TITLE="platform: default start_date to today before sending CreateMedicationRequest (Android / iOS)"
PLATFORM_MSG_BODY="Defensive copy to set start_date=yyyy-MM-dd when missing. Android uses Calendar+Locale.US to avoid java.time API level issues."

OTHER_MSG_TITLE="chore: misc changes related to medication start_date"
OTHER_MSG_BODY="Auxiliary changes (scripts, tooling) for splitting commits and improved workflow."

# Ensure we're in a git repo
if ! git rev-parse --git-dir >/dev/null 2>&1; then
  echo "Not a git repository. Run this script from the 'composeApp' directory tree." >&2
  exit 1
fi

# Create and switch to branch (create if doesn't exist)
if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
  echo "Branch $BRANCH_NAME exists. Checking it out..."
  git checkout "$BRANCH_NAME"
else
  echo "Creating branch $BRANCH_NAME..."
  git checkout -b "$BRANCH_NAME"
fi

# Collect modified tracked files under src/ and the script itself
# Use porcelain status; include staged and unstaged changes
mapfile -t MODIFIED_FILES < <(git status --porcelain --untracked-files=no | awk '{print $2}')

# Filter to files we care about
UI_FILES=()
PLATFORM_FILES=()
OTHER_FILES=()

for f in "${MODIFIED_FILES[@]}"; do
  if [[ "$f" == src/commonMain/* ]]; then
    UI_FILES+=("$f")
  elif [[ "$f" == src/androidMain/* || "$f" == src/iosMain/* ]]; then
    PLATFORM_FILES+=("$f")
  elif [[ "$f" == composeApp/scripts/* || "$f" == scripts/* || "$f" == build.gradle* || "$f" == settings.gradle* ]]; then
    OTHER_FILES+=("$f")
  fi
done

# Always include this script if it's changed
if git status --porcelain -- "scripts/commit-medication-startdate.sh" | grep -q .; then
  OTHER_FILES+=("scripts/commit-medication-startdate.sh")
fi

commit_group_files() {
  local -n files_ref=$1
  local title=$2
  local body=$3

  if [ ${#files_ref[@]} -eq 0 ]; then
    echo "No files to commit for: $title"
    return
  fi

  # Stage only existing files (safety)
  local to_stage=()
  for f in "${files_ref[@]}"; do
    if [ -f "$f" ]; then
      to_stage+=("$f")
    fi
  done

  if [ ${#to_stage[@]} -eq 0 ]; then
    echo "No existing files found to stage for: $title"
    return
  fi

  git add "${to_stage[@]}"
  git commit -m "$title" -m "$body"
  echo "Committed: $title"
}

# Run commits in order: UI, PLATFORM, OTHER
commit_group_files UI_FILES "$UI_MSG_TITLE" "$UI_MSG_BODY"
commit_group_files PLATFORM_FILES "$PLATFORM_MSG_TITLE" "$PLATFORM_MSG_BODY"
commit_group_files OTHER_FILES "$OTHER_MSG_TITLE" "$OTHER_MSG_BODY"

# Show recent commits on branch
echo "\nRecent commits on $(git rev-parse --abbrev-ref HEAD):"
git --no-pager log --oneline -n 20

echo "\nDone. To push the branch run:\n  git push -u origin $BRANCH_NAME"
