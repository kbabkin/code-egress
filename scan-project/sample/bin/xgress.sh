#!/bin/bash

# Documentation / usage
function usage() {
  echo ""
  echo "Script facilitating Egress/Ingress process."
  echo "  Supports development processes including changes in both internal and external repositories"
  echo "  integrated with Replace/Restore (Mask/Unmask) functionality."
  echo "Configure: before start check/update setenv.sh, copied from setenv.sh.template"
  echo "Usage: xgress.sh [OPTIONS..] FLOW"
  echo "  FLOW:"
  echo "    egress_start - mask and create branch for pull request"
  echo "    egress_finish - transfer merged egress pull request to external repository"
  echo "    ingress - transfer from external repository, unmask, create branch for pull request"
  echo "  OPTIONS:"
  echo "    -s - silent mode, without user confirmations"
  echo "    -c - only show commands, without executing them"
  echo "Examples:"
  echo "  ./xgress.sh egress_start"
  echo "Features:"
  echo "  Show each command before execution"
  echo "  Return back to previous command"
  echo "  Error handling - ask user decision to abort, retry, etc."
  echo "  Log everything"
  exit 1
}

# Utility functions
function configure_log() {
  local log_dir="$scan_project/target/logs"
  mkdir -p "$log_dir"
  log_file="$log_dir/xgress-$(date +%Y%m%d-%H%M%S).log"
}

function log_info() {
  echo "[$(date +%Y-%m-%d\ %H:%M:%S)] $*" | tee -a "$log_file"
}

function log_debug() {
  echo "[$(date +%Y-%m-%d\ %H:%M:%S)] $*" >>"$log_file"
}

function show_log() {
  if [[ $opt_silent -eq 1 ]]; then
    echo "Detailed log: $log_file"
  else
    read -n 1 -r -p "View detailed log: (y)es, (n)o, (c)onsole? " view_logs
    echo ""
    if [[ $view_logs == [yY] ]]; then
      less "$log_file"
    elif [[ $view_logs == [cC] ]]; then
      cat "$log_file"
    fi
  fi
}

function check_var() {
  local var_name=$1
  local var_value="${!var_name}"
  if [ -z "$var_value" ]; then
    log_info "Failed! Missing value for $var_name"
    usage
  fi
}

function check_exit_code() {
  exit_code=$?
  if [ $exit_code -ne 0 ]; then
    log_info "Failed! Log: $log_file Run: $*"
    exit ${exit_code}
  fi
}

# Step functions
function add_step() {
  step_commands+=("$1")
  step_descriptions+=("$2")
}

function exec_step() {
  local step_num=$1
  local description=${step_descriptions[$step_num]}
  local command=${step_commands[$step_num]}
  log_info "[$step_num]: $description"
  log_info "    $command"

  if [[ $opt_silent -eq 1 ]]; then
    step_action="e"
  else
    read -n 1 -r -p "Enter action: (e)xecute, (p)revious, (n)ext, (q)uit/(a)bort, (l)og): " step_action
    echo ""
  fi
  log_debug "Action: $step_action"

  if [[ $step_action == [eE] ]]; then
    log_info "[$step_num]: Starting in $(pwd)"

    #replace args
    set -- $command
    "$@" >>"$log_file" 2>&1

    local step_result=$?
    if [ $step_result -eq 0 ]; then
      log_info "[$step_num]: Success"
      return 0
    else
      log_info "[$step_num]: FAILED! Exit code: $step_result, Command: $command"
      if [[ $opt_silent -eq 1 ]]; then
        log_info "Exit silent mode due to error"
        exit 1
      fi
      return 3
    fi
  elif [[ $step_action == [aAqQ] ]]; then
    log_info "Aborting! Log: $log_file"
    exit 1
  elif [[ $step_action == [pP] ]]; then
    return 1
  elif [[ $step_action == [nN] ]]; then
    return 2
  elif [[ $step_action == [lL] ]]; then
    less +G "$log_file"
    return 3
  else
    log_info "Unrecognized: $step_action"
    return 3
  fi
}

function exec_sequence() {
  local current_step=0
  while [[ $current_step -lt ${#step_commands[@]} ]]; do
    exec_step $current_step
    local step_result=$?
    if [[ $step_result -eq 0 || $step_result -eq 2 ]]; then
      current_step=$((current_step + 1))
    elif [[ $step_result -eq 1 ]]; then
      current_step=$((current_step - 1))
      if [[ $current_step -lt 0 ]]; then
        current_step=0
      fi
    fi
  done
}

# Domain logic functions
function java_xgress() {
  #replace args
  set -- java -Dscan.project=$scan_project "$@" -jar ${xgress_jar}
  echo -n "Full command: "
  echo "$@"
  "$@"
}

function open_repo() {
  local repo=$1
  if [[ -d $repo ]] && [[ -d $repo/.git ]]; then
    echo "Opening repo folder: cd $repo"
    cd $repo || return 1
  else
    echo "Invalid repo folder $repo"
    return 1
  fi
}

function egress_start() {
  add_step "open_repo $internal_repo" "Open internal repo folder"
  add_step "git checkout $internal_egress_durable_branch" "Update local internal egress durable branch"
  add_step "git pull" "Pull last internal egress durable branch"
  add_step "git checkout $internal_develop_branch" "Checkout latest internal develop branch"
  add_step "git pull" "Pull latest internal develop branch"
  add_step "git tag $internal_egress_start_tag" "Create egress start tag for future ingress"
  add_step "git checkout -b $internal_egress_branch" "Create internal egress branch"

  add_step "java_xgress -Dscan.direction=replace -Dwrite.inplace=false" "Scan and REVIEW report"
  add_step "java_xgress -Dscan.direction=replace -Dwrite.inplace=true" "Replace - AFTER SCAN RESULTS ARE REVIEWED, (p) to rescan"

  add_step "git diff" "Git - REVIEW in log changes to be committed"
  add_step "git add ." "Git - stage changes from replace"
  add_step "git commit -m Masked.changes.for.egress.$internal_egress_branch" "Git - commit changes from replace"
  add_step "git merge -s ours $internal_egress_durable_branch" "Git - merge previous egress for nice Pull Request"
  add_step "git push origin $internal_egress_start_tag" "Git - push egress start tag"
  add_step "git push --set-upstream origin $internal_egress_branch" "Git - push internal egress branch"
}

function egress_finish() {
  add_step "open_repo $internal_repo" "Open internal repo folder"
  add_step "git checkout $internal_egress_durable_branch" "Checkout last internal egress durable branch"
  add_step "git pull" "Pull last internal egress durable branch with merged egress pull request"

  add_step "open_repo $external_repo" "Open external repo folder"
  add_step "git checkout $external_egress_durable_branch" "Update external egress durable branch"
  add_step "git pull" "Pull external egress durable branch"
  add_step "git checkout -b $external_egress_branch" "Create external egress branch"

  #todo implement in java xgress.action=copy-all
  add_step "java_xgress -Dxgress.action=copy-all -Dscan.direction=replace" "Replace all external files with internal files excluding .git etc."

  add_step "git diff" "Git - review changes to be committed"
  add_step "git add ." "Git - stage transferred egress changes"
  #todo git remove missing
  add_step "git commit -m Transferred.egress.changes.to.$external_egress_branch" "Git - commit transferred changes"
  add_step "git push --set-upstream origin $external_egress_branch" "Git - push external egress branch"
}

function ingress() {
  add_step "open_repo $external_repo" "Open external repo folder"
  add_step "git fetch" "Git - fetch latest external changes"
  add_step "git checkout $external_ingress_tag" "Checkout external ingress tag"

  add_step "open_repo $internal_repo" "Open internal repo folder"
  add_step "git fetch" "Git - fetch latest internal changes"
  add_step "git checkout $internal_egress_start_tag" "Checkout internal egress start tag"
  add_step "git checkout -b $internal_ingress_branch" "Create internal ingress branch"

  #todo implement in java xgress.action=get-changed-files
  add_step "java_xgress -Dxgress.action=get-changed-files -Dxgress.fileChanges=config/file-changes -Dxgress.fromRevision=$external_ingress_start" \
    "Get list of files changed externally since last egress"
  #todo implement in java xgress.action=copy-patch
  add_step "java_xgress -Dxgress.action=copy-patch -Dxgress.fileChanges=config/file-changes" \
    "Transfer changed files only"
  add_step "java_xgress -Dscan.direction=restore -Dwrite.inplace=true  -Dxgress.fileChanges=config/file-changes" "Restore changed files"

  add_step "git diff" "Git - review changes to be committed due to ingress"
  add_step "git add ." "Git - stage transferred unmasked ingress changes"
  #todo git remove missing
  add_step "git commit -m Transferred.unmasked.ingress.changes.from.$external_ingress_tag.to.$internal_ingress_branch" \
    "Git - commit transferred unmasked ingress changes"
  add_step "git push --set-upstream origin $internal_ingress_branch" "Git - push internal ingress branch"
}

# Application init functions
function init_env() {
  local setEnvFile="$(dirname $0)/setenv.sh"
  if [[ ! -f $setEnvFile ]]; then
    echo "No environment setup file $setEnvFile"
    usage
  fi
  source "$setEnvFile"
  cd "$(dirname $0)/.."
  scan_project="$(pwd)"
  configure_log
}

function init_args() {
  OPTIND=1 # Reset in case getopts has been used previously in the shell.

  # Initialize our own variables:
  opt_silent=0
  opt_command=0

  while getopts "s?c?" opt; do
    case "$opt" in
    s)
      opt_silent=1
      ;;
    c)
      opt_command=1
      ;;
    *)
      echo "Unknown option: $OPTARG"
      usage
      ;;
    esac
  done

  shift $((OPTIND - 1))

  [ "${1:-}" = "--" ] && shift

  flow="$1"
  check_var flow
  log_info "Config: flow: $flow, opt_silent: $opt_silent, opt_command: $opt_command"
}

function exec_flow() {
  if [[ $flow != "egress_start" && $flow != "egress_finish" && $flow != "ingress" ]]; then
    log_info "FAILED! Unsupported flow: $flow"
    usage
  fi

  # Array variable. Multiple variables are used, e.g. step_descriptions[i] to simulate steps[i].description
  declare -a step_descriptions
  declare -a step_commands
  $flow
  exec_sequence
}

# Setup and run
init_env
init_args "$@"

trap show_log EXIT
exec_flow
log_info "Success! Log: $log_file"
