#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/xgress-setenv.sh

#Big blocks are factored out to functions

# =================================================
function dropBranch {
 BRANCH=$1

 git push -d origin ${BRANCH}
 git branch -D ${BRANCH}
 }

# =================================================
function dropTag {
 TAG=$1

 git tag -d $TAG
 git push --delete origin $TAG
}

# =================================================
# Main script body
# First Egress Prepare Rollback steps
# =================================================

 read -p "We are going to rollback First Egress - Prepare - for ${EGRESS_PREPARE_DATE}"

 cd ${BITBUCKET_PROJECT}
 git fetch

 git checkout ${BITBUCKET_MAIN}
 git pull
 #Get rid of any local changes:
 git reset --hard HEAD

 dropBranch ${BITBUCKET_EGRESS_TMP}
 dropBranch ${BITBUCKET_EGRESS_STAGING}
 dropTag ${BITBUCKET_EGRESS_START_TAG}

 cd ${SCAN_PROJECT}

# TODO how do we rollback Scan Project changes if any?
 read -p "Please rollback Scan Project:  ${SCAN_PROJECT} and press Enter."

echo Rollback completed.
