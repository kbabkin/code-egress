#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/setenv.sh

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

 dropBranch ${BITBUCKET_EGRESS_TMP}
 dropBranch ${BITBUCKET_EGRESS_STAGING}
 dropTag ${BITBUCKET_EGRESS_START_TAG}

#TODO clarify with Slava
 cd ${SCAN_PROJECT}

# ??? rmdir ${SCAN_PROJECT}/../${EGRESS_PREPARE_DATE} ???
#Option 1
# git add -A *
# git commit -m "Scan project config for Egress as of ${TIMESTAMP} for ${EGRESS_PREPARE_DATE}"
# git push

#Option 2
# mkdir ${SCAN_PROJECT}/../${EGRESS_PREPARE_DATE}
# cp -r ${SCAN_PROJECT} ${SCAN_PROJECT}/../${EGRESS_PREPARE_DATE}

echo Rollback completed.
