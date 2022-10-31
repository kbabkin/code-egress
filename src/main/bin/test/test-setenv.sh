#!/bin/bash

#JAR location
script_path="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
MAIN_JAR=${script_path}/../../../target/code-egress-0.2.0-SNAPSHOT.jar

#Masking setup
export scan_project=${script_path}/test/tmp/scan-project
export read_folder=${script_path}/test/tmp/bitbucket

#Copy setup
export copy_private_dir=${script_path}/test/tmp/bitbucket
export copy_public_dir=${script_path}/test/tmp/github

#Tag/branch setup
export copy_private_egress_date=2022-22-10
export copy_public_egress_date=2022-23-10
export copy_public_ingress_date=2022-27-10
export copy_private_ingress_date=2022-28-10
export copy_mode=FILES
