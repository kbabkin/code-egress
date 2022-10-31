#!/bin/bash

#JAR location
script_path="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
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

