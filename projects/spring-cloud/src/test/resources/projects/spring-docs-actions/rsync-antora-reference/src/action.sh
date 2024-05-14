#!/bin/bash
set -e
set -f

setup_ssh.sh

echo "Synced docs"
echo 'Synced docs' > /tmp/executed_docs

exit_code=$?

exit $exit_code
