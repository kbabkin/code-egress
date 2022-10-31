#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/setenv.sh


# =================================================
# Main script body
# Regular Egress Transfer steps
# =================================================

read -p "Press Enter to start Regular Egress Transfer"

cd ${GITHUB_PROJECT}
git fetch --all

git checkout ${GITHUB_EGRESS_STAGING}
git pull

git checkout -b ${GITHUB_EGRESS_TMP}
git push --set-upstream origin ${GITHUB_EGRESS_TMP}

cd ${BITBUCKET_PROJECT}
git checkout ${BITBUCKET_EGRESS_STAGING}
git pull

${RUNTOOL} COPY_PRIVATE_CHANGES
cd ${GITHUB_PROJECT}
git commit -m "Transfer of masked changes to github for egress ${EGRESS_PREPARE_DATE} as of date ${EGRESS_TRANSFER_DATE}"

read -p "Please create Pull Request ${GITHUB_EGRESS_TMP}  -- > ${GITHUB_EGRESS_STAGING}, approve and merge "