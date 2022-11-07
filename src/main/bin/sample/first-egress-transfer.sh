#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/xgress-setenv.sh


# =================================================
# Main script body
# First Egress Transfer steps
# =================================================

read -p "Press Enter to start First Egress Transfer"

cd ${GITHUB_PROJECT}
git fetch --all

git checkout ${GITHUB_MAIN}
git pull
git checkout -b ${GITHUB_EGRESS_STAGING}
git push --set-upstream origin ${GITHUB_EGRESS_STAGING}

git checkout -b ${GITHUB_EGRESS_TMP}
git push --set-upstream origin ${GITHUB_EGRESS_TMP}

cd ${BITBUCKET_PROJECT}
git checkout ${BITBUCKET_EGRESS_STAGING}
git pull
git status

read -p "Ready to start copy of initial egress changes. Press Enter to continue."

${RUNTOOL} COPY_PRIVATE_CHANGES

cd ${GITHUB_PROJECT}
echo "Transfer of masked changes to github for egress ${EGRESS_PREPARE_DATE} as of transfer date ${EGRESS_TRANSFER_DATE}" > tmp_commit_msg.txt
git commit -F tmp_commit_msg.txt
rm tmp_commit_msg.txt

read -p "Please create Pull Request ${GITHUB_EGRESS_TMP}  -- > ${GITHUB_EGRESS_STAGING}, approve and merge "