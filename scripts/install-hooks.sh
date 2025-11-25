#!/bin/sh
# ./scripts/install-hooks.sh

mkdir -p .git/hooks

cp .github/hooks/pre-commit .git/hooks/pre-commit
cp .github/hooks/post-commit .git/hooks/post-commit

chmod +x .git/hooks/pre-commit
chmod +x .git/hooks/post-commit

echo "Hooks instalados"
