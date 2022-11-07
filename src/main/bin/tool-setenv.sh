#!/bin/bash

#JAR location
script_path="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

# Make 'java' verbose and interactive:
function java() {
  echo "In directory: `pwd`"
  read -p "EXEC: java $1 $2 $3 $4 $5 $6 $7 $8 $9? Execute [<ENTER>], skip [s] or abort [a]:" EXECUTE_ANSWER
  EXECUTE_ANSWER=${EXECUTE_ANSWER:-e}
  if [ $EXECUTE_ANSWER == "S" ] || [ $EXECUTE_ANSWER == "s" ]; then
    echo Skipped.
    return
  fi

  if [ $EXECUTE_ANSWER == 'A' ] || [ $EXECUTE_ANSWER == 'a' ]; then
      echo Script aborted with exit code 1.
      exit 1
  fi

  command java $1 $2 $3 $4 $5 $6 $7 $8 $9
  retVal=$?
  if [ $retVal -ne 0 ]; then
    echo
    read -p "java failed! java exit code: $retVal. Ignore error [y] or abort script [n]:" IGNORE_ANSWER
    if [ $IGNORE_ANSWER == 'N' ] || [ $IGNORE_ANSWER == 'n' ]; then
      echo Script aborted with exit code 2.
      exit 2
    fi
  fi
}

SCAN_TOOL_JAR=${script_path}/../../../target/code-egress-0.2.0-SNAPSHOT.jar

#Masking setup
export scan_project=${SCAN_PROJECT}
export read_folder=${BITBUCKET_PROJECT}

#Copy setup
export copy_private_dir=${BITBUCKET_PROJECT}
export copy_public_dir=${GITHUB_PROJECT}
export copy_mode=FILES
export copy_private_prefix=${BITBUCKET_PREFIX}

#Tag/branch setup
export copy_privateSource_egress_main=${BITBUCKET_MAIN}

export copy_privateSource_egress_date=${EGRESS_PREPARE_DATE}
export copy_publicSource_egress_date=${EGRESS_TRANSFER_DATE}
export copy_publicSource_ingress_date=${INGRESS_DATE}
export copy_privateSource_ingress_date=${INGRESS_DATE}

