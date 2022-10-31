#!/bin/bash


export work_dir="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export main_script_path=${work_dir}/..

export initial_egress_date="2022-10-22"
export initial_egress_tag_ts=$(date +"%Y-%m-%d_%H-%M-%S_%3N")

export initial_egress_transfer_date="2022-10-23"

. ${work_dir}/utils.sh
. ${work_dir}/actions.sh

printBannerAndWait "RUNNING INITIAL EGRESS"
echo Running script in ${work_dir}

cp test-setenv.sh ../setenv.sh


#"${main_script_path}"/runtool.sh SHOW_SCRIPT egress-prepare initial
#"${main_script_path}"/runtool.sh SHOW_SCRIPT egress-prepare config-changed
#"${main_script_path}"/runtool.sh SHOW_SCRIPT egress-prepare [config-unchanged]
#"${main_script_path}"/runtool.sh SHOW_SCRIPT egress-transfer

#"${main_script_path}"/runtool.sh SHOW_SCRIPT ingress


# ========================================
# Prepare mock repositories
# ========================================

#1. Re-create mock directories and a single mock scan project in temporary space: ${work_dir}/tmp
initDirs ${work_dir}

#2. Mock bitbucket repo contents by emulating initial commit and development commit
initialCommit bitbucket ${work_dir}
git checkout -b DEV_INT
emulateDev ${work_dir} "01"

#3. Create bitbucket staging branch
cd ${work_dir}/tmp/bitbucket
git checkout -b btech/DEV_INT_EGRESS

#4. Mock github repo contents
initialCommit github ${work_dir}
git checkout -b main

#5. Create github staging branch
git checkout -b btech/EGRESS

# ========================================
# Perform initial egress - changes preparation
# ========================================

#3. Put 2 tags: latest egress without date and latest egress with date
# (emulate COMPLETE_CHANGES tool mode)
echo tagEgressLatest ${work_dir} ${initial_egress_tag_ts}
tagEgressLatest ${work_dir} ${initial_egress_tag_ts}

#4. Create egress temporary branch -- done as in 'config hasn't changed'
createTemporaryFrom ${work_dir} btech/DEV_INT_EGRESS ${initial_egress_date}

#5. Obfuscate
mask ${main_script_path} "initial egress"
commitMasking "initial egress"

#6. Pull Request to staging branch (emulating via merge)
mergeTemporaryToStaging ${initial_egress_date}

# ========================================
# Perform initial egress - changes transfer
# ========================================

#1. Transfer changes to temporary github branch
transferToGithub ${work_dir} ${main_script_path} ${initial_egress_transfer_date} "initial egress"

#2. Merge to main in github (by PR? Done by Bubble team)
cd ${work_dir}/tmp/github
git checkout main
git merge btech/EGRESS_${initial_egress_transfer_date}

#3. All OK - put a finalizing tag
${main_script_path}/runtool.sh COMPLETE_PRIVATE_CHANGES

banner "RUNNING INITIAL EGRESS COMPLETED"

#TODO move to separate file
# ========================================
# Perform regular egress - changes preparation (config didn't change)
# ========================================

#createTemporaryFrom ${work_dir} btech/DEV_INT_EGRESS

# ========================================
# Perform regular egress - changes transfer
# ========================================

# ========================================
# Perform regular egress - changes preparation (config changed)
# ========================================

# ========================================
# Perform regular egress - changes transfer
# ========================================

# ========================================
# Perform ingress
# ========================================

# ========================================
# Perform regular egress - changes preparation (config changed)
# ========================================

# ========================================
# Perform regular egress - changes transfer
# ========================================
