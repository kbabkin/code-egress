#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/xgress-setenv.sh

#Big blocks are factored out to functions

# =================================================
function initialEgress {
 cd ${BITBUCKET_PROJECT}
 git fetch

 git checkout ${BITBUCKET_MAIN}
 git pull
 git checkout -b ${BITBUCKET_EGRESS_STAGING}
 git push --set-upstream origin ${BITBUCKET_EGRESS_STAGING}
 #Just emulate 'pre-initial' egress tag, so that diff algorithm to work in Staging branch:
 git tag ${BITBUCKET_EGRESS_LATEST}
 git push origin ${BITBUCKET_EGRESS_LATEST}

 git checkout -b ${BITBUCKET_EGRESS_TMP}
 git push --set-upstream origin ${BITBUCKET_EGRESS_TMP}

 REVIEWED=0
 while [ ${REVIEWED} != "Y" ] && [ ${REVIEWED} != "y" ]
 do
 ${RUNTOOL} MASK_PREVIEW
 read -p "Please review masking result and files: replace-report.csv ('Allow' column) and csv-dictionary-candidate.csv, then proceed to in-place replacement. Input Y for Proceed, N for Re-scan:" REVIEWED
 done

 ${RUNTOOL} MASK

 git add --all -- ':!tmp_commit_msg.txt'
 git status
 read -p "Press Enter to commit masking result"

 echo "Masked changes for initial egress ${EGRESS_PREPARE_DATE}" > tmp_commit_msg.txt
 git commit -F tmp_commit_msg.txt
 rm tmp_commit_msg.txt

#backup
 cp ${SCAN_PROJECT_CONFIG}/restore-instruction.csv ${SCAN_PROJECT_CONFIG}/restore-instruction.csv.bak.${TIMESTAMP}
#overwrite
 cp ${SCAN_PROJECT_TARGET}/restore-instruction-last.csv ${SCAN_PROJECT_CONFIG}/restore-instruction.csv

 }

# =================================================
# Main script body
# First Egress Prepare steps
# =================================================

 read -p "Please make sure that project : ${PROJECT} - branch ${BITBUCKET_MAIN} is successfully built in Jenkins, then press Enter"

 cd ${BITBUCKET_PROJECT}
 git fetch --all

 git checkout ${BITBUCKET_MAIN}
 git pull
 git tag ${BITBUCKET_EGRESS_START_TAG}
 git push origin ${BITBUCKET_EGRESS_START_TAG}

 initialEgress

 cd ${BITBUCKET_PROJECT}
 git push

 cd ${SCAN_PROJECT}

# TODO how do we preserve Scan Project changes so that we can rollback afterwards?
 read -p "Please preserve Scan Project:  ${SCAN_PROJECT} and press Enter."

 read -p "Please create Pull Request ${BITBUCKET_EGRESS_TMP}  -- > ${BITBUCKET_EGRESS_STAGING}, approve and merge "