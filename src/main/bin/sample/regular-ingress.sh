#!/bin/bash

export script_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_dir}/setenv.sh


# =================================================
# Main script body
# Regular Ingress steps
# =================================================

read -p "Press Enter to start Ingress"

cd ${GITHUB_PROJECT}
git fetch --all

git checkout tags/${GITHUB_INGRESS_TAG} -b ${GITHUB_INGRESS_TMP}

cd ${BITBUCKET_PROJECT}
git fetch --all

git checkout ${BITBUCKET_MAIN}
git pull

git checkout tags/${BITBUCKET_EGRESS_START_TAG} -b ${BITBUCKET_INGRESS_TMP}

${RUNTOOL} COPY_PUBLIC_CHANGES

${RUNTOOL} UNMASK --scan-project=${SCAN_PROJECT}

git commit -m "Ingress ${INGRESS_DATE} for respective egress ${EGRESS_PREPARE_DATE}"

#TODO clarify with Slava
cd ${SCAN_PROJECT}

#Option 1
#git commit -m "Scan project config for Ingress as of ${TIMESTAMP}"
#git push

#Option 2
#mkdir ${SCAN_PROJECT}-ingress-${TIMESTAMP}
#cp -r ${SCAN_PROJECT} ${SCAN_PROJECT}-ingress-${TIMESTAMP}

git merge ${BITBUCKET_MAIN}
read -p "Please check that there are no conflicts after the above merge. In case of conflicts, use your IDE to resolve & commit. Press Enter to proceed when resolved. "

read -p "Please create Pull Request ${BITBUCKET_INGRESS_TMP}  -- > ${BITBUCKET_MAIN}, approve and merge "