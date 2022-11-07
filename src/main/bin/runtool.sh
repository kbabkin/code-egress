#!/bin/bash

script_path="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. ${script_path}/tool-setenv.sh


if [[ -z "$SCAN_TOOL_JAR" ]]; then
    echo "SCAN_TOOL_JAR environment variable not set, exiting" 1>&2
    exit 1
fi

echo script_path=$script_path
echo SCAN_TOOL_JAR=$SCAN_TOOL_JAR
echo ===========================================
#In case we will need any JVM args:
export JAVA_ARGS=''

if [[ -z "$1" ]]; then
# Run interactively
   PS3='Select egress/ingress scan tool run mode: '
   options=("COPY_PRIVATE_CHANGES" "COPY_PUBLIC_CHANGES" "MASK_PREVIEW" "MASK" "UNMASK_PREVIEW" "UNMASK" "Quit")
   select opt in "${options[@]}"
   do
      case $opt in
        "COPY_PRIVATE_CHANGES")
            echo "Will copy git changes to github target folder file-by-file"
            ;;
        "COPY_PUBLIC_CHANGES")
            echo "Will copy git changes to bitbucket target folder file-by-file"
            ;;
        "COMPLETE_PRIVATE_CHANGES")
            echo "Will tag the egress staging branch after changes have been copied to github"
            ;;
        "MASK_PREVIEW")
            echo "Will copy masked files to preview folder"
            ;;
        "MASK")
            echo "Will mask files and overwrite them in place"
            ;;
        "UNMASK_PREVIEW")
            echo "Will copy unmasked files to preview folder"
            ;;
        "UNMASK")
            echo "Will unmask files and overwrite them in place"
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
       esac
       echo "Press enter to start, Ctrl-C to abort"
       read

       java $JAVA_OPTS -jar "${SCAN_TOOL_JAR}" --mode=$opt
    done
else
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     $JAVA_OPTS -jar "${SCAN_TOOL_JAR}" --mode=$1
fi


