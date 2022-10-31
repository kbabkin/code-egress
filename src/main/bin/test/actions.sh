#!/bin/bash

# ---------------------------------------------------------------------
function initDirs() {
work_dir=$1

rm -rf ${work_dir}/tmp
mkdir ${work_dir}/tmp
mkdir ${work_dir}/tmp/bitbucket
mkdir ${work_dir}/tmp/github
mkdir ${work_dir}/tmp/scan-project
cp -r ${work_dir}/../../../../scan-project/sample/config ${work_dir}/tmp/scan-project
}

# ---------------------------------------------------------------------
function initialCommit() {
name=$1
work_dir=$2

cd ${work_dir}/tmp/${name}
git init
echo "This is a mock ${name} repository"  > README.txt
git add README.txt
git commit -m "Initializing ${name} repository with 1st commit"
}

# ---------------------------------------------------------------------
function emulateDev() {
work_dir=$1
n=$2

cd ${work_dir}/tmp/bitbucket
echo "Email me at manager@bnpparibas.com"  > test${n}.txt
git add test${n}.txt
git commit -m "Internal dev ${n}"

printBannerAndWait "Development changes committed (${n})."
}

# ---------------------------------------------------------------------
function tagEgressLatest() {
work_dir=$1
egress_date=$2
cd ${work_dir}/tmp/bitbucket
git checkout btech/DEV_INT_EGRESS
git tag "egress-latest"
git tag "egress-${egress_date}"
}

# ---------------------------------------------------------------------
function createTemporaryFrom() {
  work_dir=$1
  from=$2
  egress_date=$3

  cd ${work_dir}/tmp/bitbucket
  git checkout ${from}
  git checkout -b btech/DEV_INT_EGRESS_${egress_date}
}

# ---------------------------------------------------------------------
function mergeTemporaryToStaging() {
  egress_date=$1
  git checkout btech/DEV_INT_EGRESS
  git merge btech/DEV_INT_EGRESS_${egress_date}
  printBannerAndWait "Merge to staging branch completed (should be done by PR)"
}

# ---------------------------------------------------------------------
function mask() {
main_script_path=$1
msg=$2
"${main_script_path}"/runtool.sh MASK_PREVIEW

printTree ${work_dir}/tmp/scan-project/target/preview
printBannerAndWait "${msg} : Masking preview completed, see changed file list above (inside 'preview')"

${main_script_path}/runtool.sh MASK

cd ${work_dir}/tmp/bitbucket
git status
git diff
printBannerAndWait "${msg} : Masking completed, see diff above"
}

# ---------------------------------------------------------------------
function commitMasking() {
msg=$1
git add -A *
git commit -m "Masking for ${msg}"

banner "Masked code committed for ${msg}"
}

# ---------------------------------------------------------------------
function transferToGithub() {
work_dir=$1
main_script_path=$2
egress_transfer_date=$3
msg=$4

banner "Starting to Copy Changes from bitbucket to github for ${msg}"
cd ${work_dir}/tmp/github
git checkout btech/EGRESS
git checkout -b btech/EGRESS_${egress_transfer_date}

cd ${work_dir}/tmp/bitbucket
git checkout btech/DEV_INT_EGRESS
git show-ref --tags
git status
printBannerAndWait "Will start copying now."

${main_script_path}/runtool.sh COPY_PRIVATE_CHANGES

banner "Copied Private Changes"

cd ${work_dir}/tmp/github

#Without adding, can't use diff on a branch without commits
git add -A *
git status
git diff
printBannerAndWait "Review ${msg} to github before commit"

git commit -m "Accepting ${msg}"
}




