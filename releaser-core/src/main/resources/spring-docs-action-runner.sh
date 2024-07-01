#!/bin/bash
set -e
set -f

# Get the current directory
current_dir=$(pwd)

# Add the current directory to the PATH variable
export PATH=$PATH:$current_dir

__action_usage() {
  echo "usage: action.sh [OPTION]...

   --docs-username=USERNAME       the username used to connect to ssh
   --docs-host=HOST               the host to connect to ssh
   --docs-ssh-key-path=KEY        the path to the private key used to connect to ssh
   --docs-ssh-host-key=HOST_KEY   the host key used to connect to ssh
   --dry-run                      signals that rsync should be in dry run mode
   --site-path=PATH               the local directory path to sync to the server. Default build/site
   --github-repository=GH_REPO    the github repository (e.g. spring-projects/spring-security)
   --httpdocs-path=PATH           the optional base httpdocs path (e.g. https://docs.spring.io/spring-security/reference would be /spring-security/reference)
                                  If this is set, then the project must own the directory by ensuring to have a .github-repository file with the
                                  <OWNER>/<REPOSITORY_NAME> as the content. The default is to use /\${REPOSITORY_NAME}/reference
                                  from --github-repository
"
}

__action_usage_error() {
  echo "Error: $1" >&2
  __action_usage
  exit 1
}

__action() {
  local docs_username docs_host docs_ssh_key docs_ssh_host_key site_path github_repository httpdocs_path valid_args
  local rsync_flag_options=''
  if [ ! -z "$BUILD_REFNAME" ]; then
    rsync_flag_options="${rsync_flag_options} --build-ref-name $BUILD_REFNAME"
  fi
  valid_args=$(getopt --options '' --long docs-username:,docs-host:,docs-ssh-key-path:,docs-ssh-host-key:,dry-run,site-path:,github-repository:,httpdocs-path: -- "$@")
  if [[ $? -ne 0 ]]; then
    __action_usage
    exit 1;
  fi

  eval set -- "$valid_args"

  while [ : ]; do
    case "$1" in
      --docs-username)
          docs_username="$2"
          shift 2
          ;;
      --docs-host)
          docs_host="$2"
          shift 2
          ;;
      --docs-ssh-key-path)
          docs_ssh_key=$(cat "$2")
          shift 2
          ;;
      --docs-ssh-host-key)
          docs_ssh_host_key="$2"
          shift 2
          ;;
      --dry-run)
          rsync_flag_options="${rsync_flag_options} --dry-run"
          shift
          ;;
      --site-path)
          site_path="$2"
          shift 2
          ;;
      --github-repository)
          github_repository="$2"
          shift 2
          ;;
      --httpdocs-path)
          httpdocs_path="$2"
          shift 2
          ;;
       --) shift;
          break
          ;;
      *)
        __action_usage_error "Invalid argument $1 $2"
        ;;
    esac
  done

  if [ -z "$docs_username" ]; then
    __action_usage_error "Missing option '--docs-username'"
  fi
  if [ -z "$docs_host" ]; then
    __action_usage_error "Missing option '--docs-host'"
  fi
  if [ -z "$docs_ssh_key" ]; then
    __action_usage_error "Missing option '--docs-ssh-key'"
  fi
  if [ -z "$docs_ssh_host_key" ]; then
    __action_usage_error "Missing option '--docs-ssh-host-key'"
  fi
  if [ -z "$site_path" ]; then
    site_path="build/site"
  fi
  if [ -z "$github_repository" ]; then
    __action_usage_error "Missing option '--github-repository'"
  fi

  (
    set -e
    set -f

     action.sh --docs-username "$docs_username" --docs-host "$docs_host" --docs-ssh-key "$docs_ssh_key" --docs-ssh-host-key "$docs_ssh_host_key" --site-path "$site_path" --github-repository "$github_repository" $rsync_flag_options
  )
  exit_code=$?

  echo "Exiting with code $exit_code"

  exit $exit_code
}

__action "$@"
