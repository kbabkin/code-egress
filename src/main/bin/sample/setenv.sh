#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

# Make 'git' verbose and interactive:
function git() {
  pwd
  echo "EXEC: git $1 $2 $3 $4 $5 $6 $7 $8 ? Enter - continue"
  read
  command git $1 $2 $3 $4 $5 $6 $7 $8
  retVal=$?
  if [ $retVal -ne 0 ]; then
    echo
    read -p "git failed! exit code: $retVal. Continue anyway [y/n] ?" ANSWER
    if [ $ANSWER == 'N' ] || [ $ANSWER == 'n' ];    then
      echo Aborted.
      exit 1
    fi
  fi
}

echo Parameters dump started.
set -x

# =================================================
# Technical parameters
# =================================================

#Proxy for repositories
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

# Stable configuration parameters:

export TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S_%3N")

#export GLOBAL_ROOT=/d
export GLOBAL_ROOT=/c/Work
export PROJECT_ROOT=${GLOBAL_ROOT}/ingress/ESS
export BITBUCKET_PROJECT=${PROJECT_ROOT}/${PROJECT}_bitbucket
export GITHUB_PROJECT=${PROJECT_ROOT}/${PROJECT}_github

#TODO clarify with Slava:
#TOD0 is thare a separate date-based scan project folder created for each egress?
#We use 'current' as a placeholder
export SCAN_PROJECT=${GLOBAL_ROOT}/scan-project/ESS/${PROJECT}/current
export SCAN_PROJECT_TARGET=${SCAN_PROJECT}/target
export SCAN_PROJECT_CONFIG=${SCAN_PROJECT}/config

export RUNTOOL=${script_dir}/../runtool.sh

#Egress/ingress branches
export BITBUCKET_EGRESS_START_TAG=${BITBUCKET_PREFIX}_EGRESS_START_${EGRESS_PREPARE_DATE}
export BITBUCKET_EGRESS_STAGING=btech/${BITBUCKET_PREFIX}_EGRESS
export BITBUCKET_EGRESS_TMP=btech/${BITBUCKET_PREFIX}_EGRESS_${EGRESS_PREPARE_DATE}

export GITHUB_EGRESS_STAGING=btech/EGRESS
export GITHUB_EGRESS_TMP=btech/${EGRESS_TRANSFER_DATE}

export GITHUB_INGRESS_TAG=INGRESS_${INGRESS_DATE}
export GITHUB_INGRESS_TMP=btech/INGRESS_${INGRESS_DATE}

export BITBUCKET_INGRESS_TMP=btech/${BITBUCKET_PREFIX}_INGRESS_${INGRESS_DATE}

set +x

echo Parameters dump completed.