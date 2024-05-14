#!/bin/bash

# Get the current directory
current_dir=$(pwd)

# Add the current directory to the PATH variable
export PATH=$PATH:$current_dir

action.sh "$@"
