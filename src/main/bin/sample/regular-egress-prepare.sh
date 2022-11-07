#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/setenv.sh

#Big blocks are factored out to functions

# =================================================
function prepareEgressWhenConfigChanged() {
 cd ${BITBUCKET_PROJECT}
 git fetch

 git checkout ${BITBUCKET_MAIN}
 git pull
 git checkout ${BITBUCKET_EGRESS_STAGING}
 git pull
 git checkout -b ${BITBUCKET_EGRESS_TMP}

 REVIEWED=0
 while [ ${REVIEWED} -neq 1 ]
 do
 ${RUNTOOL} MASK_PREVIEW
 read -p "Please review masking result and files: replace-report.csv and csv-dictionary-candidate.csv, then proceed to in-place replacement. Input 1 for Proceed, 0 for Re-scan:" REVIEWED
 done

 ${RUNTOOL} MASK
 read -p "Press Enter to commit masking result"

 echo "Masked changes for egress ${EGRESS_PREPARE_DATE}" > tmp_commit_msg.txt
 git commit -F tmp_commit_msg.txt

 git merge -s ours origin/${BITBUCKET_EGRESS_STAGING}
 read -p "Please check that there are no conflicts after the above merge. In case of conflicts, use your IDE to resolve. Press Enter to proceed when resolved. "

#backup
 cp ${SCAN_PROJECT_CONFIG}/restore-instruction.csv ${SCAN_PROJECT_CONFIG}/restore-instruction.csv.bak.${TIMESTAMP}
#overwrite
 cp ${SCAN_PROJECT_TARGET}/restore-instruction-last.csv ${SCAN_PROJECT_CONFIG}/restore-instruction.csv

 }

# =================================================
 function prepareEgressWhenConfigNotChanged() {
 cd ${BITBUCKET_PROJECT}
 git fetch

 git checkout ${BITBUCKET_MAIN}
 git pull

 git checkout ${BITBUCKET_EGRESS_STAGING}
 git pull

 git checkout -b ${BITBUCKET_EGRESS_TMP}
 git merge -s theirs ${BITBUCKET_MAIN}
 read -p "Please check that there are no conflicts after the above merge. In case of conflicts, use your IDE to resolve. Press Enter to proceed when resolved. "

 REVIEWED=0
 while [ ${REVIEWED} != "Y" ] && [ ${REVIEWED} != "y" ]
 do
 ${RUNTOOL} MASK_PREVIEW
 read -p "Please review masking result, then proceed to in-place replacement. Input Y for Proceed, 0 for Re-scan:" REVIEWED
 done

 ${RUNTOOL} MASK
 read -p "Press Enter to commit masking result"

 echo "Masked changes for egress ${EGRESS_PREPARE_DATE}" > tmp_commit_msg.txt
 git commit -F tmp_commit_msg.txt

#backup
 cp ${SCAN_PROJECT_CONFIG}/restore-instruction.csv ${SCAN_PROJECT_CONFIG}/restore-instruction.csv.bak.${TIMESTAMP}
#overwrite
 cp ${SCAN_PROJECT_TARGET}/restore-instruction-cumulative.csv ${SCAN_PROJECT_CONFIG}/restore-instruction.csv
 }

# =================================================
# Main script body
# Regular Egress steps
# =================================================

 read -p "Please make sure that project : ${PROJECT} - branch ${BITBUCKET_MAIN} is successfully built in Jenkins, then press Enter"

 cd ${BITBUCKET_PROJECT}
 git fetch --all

 git checkout ${BITBUCKET_MAIN}
 git pull
 git tag ${BITBUCKET_EGRESS_START_TAG}
 git push origin ${BITBUCKET_EGRESS_START_TAG}

if [ ${MASKING_CONFIG_CHANGED} -eq 1 ]; then
 prepareEgressWhenConfigChanged
else
 prepareEgressWhenConfigNotChanged
fi

 cd ${BITBUCKET_PROJECT}
 git push

#TODO clarify with Slava
 cd ${SCAN_PROJECT}

 read -p "Please create Pull Request ${BITBUCKET_TMP}  -- > ${BITBUCKET_STAGING}, approve and merge "