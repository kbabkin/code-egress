#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/setenv.sh

#Big blocks are factored out to functions

# =================================================
function initialEgress {
 cd ${BITBUCKET_PROJECT}
 git fetch

 git checkout ${BITBUCKET_MAIN}
 git pull
 git checkout -b ${BITBUCKET_EGRESS_STAGING}
 git push --set-upstream origin ${BITBUCKET_EGRESS_STAGING}

 git checkout -b ${BITBUCKET_EGRESS_TMP}
 git push --set-upstream origin ${BITBUCKET_EGRESS_TMP}

 REVIEWED=0
 while [ ${REVIEWED} != "Y" ] && [ ${REVIEWED} != "y" ]
 do
 ${RUNTOOL} MASK_PREVIEW
 read -p "Please review masking result and files: replace-report.csv ('Allow' column) and csv-dictionary-candidate.csv, then proceed to in-place replacement. Input Y for Proceed, N for Re-scan:" REVIEWED
 done

 ${RUNTOOL} MASK
 read -p "Press Enter to commit masking result"

 git commit -m "Masked changes for initial egress ${EGRESS_PREPARE_DATE}"

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

#TODO clarify with Slava
 cd ${SCAN_PROJECT}

#Option 1
# git add -A *
# git commit -m "Scan project config for Egress as of ${TIMESTAMP} for ${EGRESS_PREPARE_DATE}"
# git push

#Option 2
# mkdir ${SCAN_PROJECT}/../${EGRESS_PREPARE_DATE}
# cp -r ${SCAN_PROJECT} ${SCAN_PROJECT}/../${EGRESS_PREPARE_DATE}

 read -p "Please create Pull Request ${BITBUCKET_TMP}  -- > ${BITBUCKET_STAGING}, approve and merge "