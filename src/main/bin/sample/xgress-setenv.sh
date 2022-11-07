#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

# Make 'git' verbose and interactive:
function git() {
  echo "In directory: `pwd`"
  read -p "EXEC: git $1 $2 $3 $4 $5 $6 $7 $8 $9? Execute [<ENTER>], skip [s] or abort [a]:" EXECUTE_ANSWER
  EXECUTE_ANSWER=${EXECUTE_ANSWER:-e}
  if [ $EXECUTE_ANSWER == "S" ] || [ $EXECUTE_ANSWER == "s" ]; then
    echo Skipped.
    return
  fi

  if [ $EXECUTE_ANSWER == 'A' ] || [ $EXECUTE_ANSWER == 'a' ]; then
      echo Script aborted with exit code 1.
      exit 1
  fi

  command git $1 $2 $3 $4 $5 $6 $7 $8 $9
  retVal=$?
  if [ $retVal -ne 0 ]; then
    echo
    read -p "git failed! git exit code: $retVal. Ignore error [i] or abort script [a]:" IGNORE_ANSWER
    if [ $IGNORE_ANSWER == 'A' ] || [ $IGNORE_ANSWER == 'a' ]; then
      echo Script aborted with exit code 2.
      exit 2
    fi
  fi
}

# Make 'cp' verbose and interactive:
function cp() {
  pwd
  read -p "EXEC: cp $1 $2 $3 $4 $5 $6 $7 $8 $9 ? Execute [<ENTER>] or skip [s]:" EXECUTE_ANSWER
  EXECUTE_ANSWER=${EXECUTE_ANSWER:-e}
  if [ $EXECUTE_ANSWER == 'S' ] || [ $ANSWER == 's' ]; then
    echo Skipped.
    return
  fi

  command cp $1 $2 $3 $4 $5 $6 $7 $8 $9
}

echo Parameters dump started.
set -x

# =================================================
# Technical parameters
# =================================================

#Proxy for repositories access for git
#export https_proxy="http://user:password@proxy:8080"

# =================================================
# Regular Egress/Ingress parameters
# =================================================

# Parameters to be set up for each egress/ingress:

# PROJECT - name of project: e.g. database, ess-engine, ess-engine-common, ...
export PROJECT=myproject

# Main branch name and its respective representation as a prefix (e.g. DEVELOP is prefix for develop)
export BITBUCKET_MAIN=DEV_INT
export BITBUCKET_PREFIX=DEV_INT

export GITHUB_MAIN=main
#github does not use any prefixes

# Parameters to be set up for each egress:
# MASKING_CONFIG_CHANGED - flag (0 or 1) to indicate whether Scan Project config has changed
# EGRESS_PREPARE_DATE - day when egress was initiated
# EGRESS_TRANSFER_DATE - day when EGRESS Pull Request already approved/merged and we want to transfer changes to GH

# Masking parameters
export MASKING_CONFIG_CHANGED=1
#export MASKING_CONFIG_CHANGED=0

# Dates
export EGRESS_PREPARE_DATE=2022-10-25
export EGRESS_TRANSFER_DATE=2022-10-27
export INGRESS_DATE=2022-10-31

# Scan project ID
export SCAN_PROJECT_ID=2022_10_25

# Stable configuration parameters:

export TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S_%3N")

#export GLOBAL_ROOT=/d
export GLOBAL_ROOT=/c/Work
export ORG=btech
export DOMAIN=ESS
export PROJECT_ROOT=${GLOBAL_ROOT}/ingress/${DOMAIN}
export BITBUCKET_PROJECT=${PROJECT_ROOT}/${PROJECT}_bitbucket
export GITHUB_PROJECT=${PROJECT_ROOT}/${PROJECT}_github

export SCAN_PROJECT=${GLOBAL_ROOT}/scan-project/${DOMAIN}/${PROJECT}/${SCAN_PROJECT_ID}
export SCAN_PROJECT_TARGET=${SCAN_PROJECT}/target
export SCAN_PROJECT_CONFIG=${SCAN_PROJECT}/config

export RUNTOOL=${script_dir}/../runtool.sh

#Egress/ingress branches
#Tag to put on main branch when egress is started
export BITBUCKET_EGRESS_START_TAG=${BITBUCKET_PREFIX}_EGRESS_START_${EGRESS_PREPARE_DATE}
#Tag to put on staging branch when egress changes transfer is completed
export BITBUCKET_LATEST_EGRESS=LATEST-EGRESS
#Staging branch
export BITBUCKET_EGRESS_STAGING=${ORG}/${BITBUCKET_PREFIX}_EGRESS
#Branch created as source for Pull Requests to Staging
export BITBUCKET_EGRESS_TMP=${ORG}/${BITBUCKET_PREFIX}_EGRESS_${EGRESS_PREPARE_DATE}

export GITHUB_EGRESS_STAGING=${ORG}/EGRESS
export GITHUB_EGRESS_TMP=${ORG}/${EGRESS_TRANSFER_DATE}

export GITHUB_INGRESS_TAG=INGRESS_${INGRESS_DATE}
export GITHUB_INGRESS_TMP=${ORG}/INGRESS_${INGRESS_DATE}

export BITBUCKET_INGRESS_TMP=${ORG}/${BITBUCKET_PREFIX}_INGRESS_${INGRESS_DATE}

set +x

echo Parameters dump completed.