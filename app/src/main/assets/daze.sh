#!/bin/bash

trap cleanup SIGINT SIGQUIT SIGHUP

usage(){
  echo "usage: $0 -i <input dir> -o <output dir> [-d] [-s <device ID>] -w -q <apk path> [-a <adb full path>] [-t <full path to aapt>]"
  echo
  echo "where:"
  echo "  -w                       Whole device or single app testing (daemon mode)"
  echo "  -q <apk path>            Process single APK"
  echo "  -i <input dir>      	   Input directory containing APKs (process all APKs in a directory)"
  echo "  -o <output dir>      	   Output directory to store DB files"
  echo "  -d                       Activate debug mode"
  echo "  -s <device ID>           ADB Device ID from adb devices"
  echo "  -a                       Full path to adb binary (if not in PATH)"
  echo "  -t                       Full path to aapt binary (if not in PATH)"
  echo
  echo "Either the -w or -i <input dir> or -q <apk path> switch must be used"
}


cleanup() {
 
  if [ ! -z $crashClearPID ]; then
    kill -9 $crashClearPID
  fi

  if [ ! -z $appRestartPID ]; then
    kill -9 $appRestartPID
  fi

  if [ ! -z $forceStopAppMonitorPID ]; then
    kill -9 $forceStopAppMonitorPID
  fi

   if [ -f forceStopLogcatPID ]; then
    kill -9 $(<forceStopLogcatPID)
    rm forceStopLogcatPID
  fi
  
  if [ ! -z $DEBUG ]; then
    echo crashClearPID is $crashClearPID
    echo appRestartPID is $appRestartPID
    echo forceStopLogcatPID is $forceStopLogcatPID
    echo forceStopAppMonitorPID is $forceStopAppMonitorPID
  fi

  exit 0

}

forceStopApp(){

  #echo "forceStopApp"

  ## sleep for a second
  sleep 1

  logLine=$1

  packageName=$(echo "$logLine" | awk -F"|" '{print $2}')

  echo "force stop $packageName"

  $ADB shell am force-stop "$packageName"
}

forceStopAppMonitor(){
  ($ADB logcat -v brief -s ITA_Finished_Package:W & echo $! > forceStopLogcatPID) | grep --line-buffered 'ITA_Forcestop_Package' | while read line ; do forceStopApp "$line" ; done
}

longRunningClearCrashDialog(){
  while true
  do
    sleep 1.5

    currentWindow=$($ADB shell dumpsys window windows | grep mCurrentFocus)

    if [[ $currentWindow == *"Application Error"* || $currentWindow == *"has stopped."* ]]; then
      $ADB shell input keyevent 66 22 66
      echo "crash dialog cleared"
    fi
  done
}

restartOwnApp(){
while true
  do

    sleep 30

    psOutput=$($ADB shell ps | grep com.kryptowire.daze)

    if [[ $psOutput != *"com.kryptowire.daze"* ]]; then
      $ADB shell am start -n com.kryptowire.daze/.ITA_MainAct -a com.kryptowire.daze.continue
      echo "our app not running - restart it"
    fi
  done
}

processSingleApp() {

  # the path of the apk to install
  apkFile="$1"
 
  ## the temp directory 
  tempDir="$2"

  ## get the package name of the app
  #packageName=$($AAPT dump badging ${apkFile} | awk -F="*'" '/package: name=/{print $2}'  
  packageName=$($AAPT dump badging ${apkFile} | awk -F" " '/package: name/ {print $2}'| awk -F"'" '/name=/ {print $2}')

  if [ ! -z $DEBUG ]; then
    echo "package name is $packageName and tempDir is $tempDir"
  fi

  if [ -z $packageName ]; then
    echo "skipping...failed to get package name of $apkFile" 
    return
  fi

  ## install the app (-g grants all runtime perms)
  installResult=$($ADB install -g -r "$apkFile" 2>&1) &
  installPID=$!

  wait $installPID

  pmComm=$($ADB shell pm list package $packageName)

  if [ ! -z $pmComm ]; then
    echo "successful install"
  else
    echo "install failed"
    return
  fi

  ## sleep a few seconds
  sleep 4
  echo "sleeping 4 seconds"

  ## clear the log
  $ADB logcat -c
  
  $ADB shell am start -n com.kryptowire.daze/com.kryptowire.daze.ITA_MainAct -a start_analysis --es packageName ${packageName}

  ## more time
  sleep 4  

  DONE=

  while [ -z "$DONE" ]; do

    ## give it some time
    sleep 10

    ## try to pull the file that tells that it is done
    pullResult=$($ADB pull /sdcard/daze/${packageName}.txt "${tempDir}/${packageName}.txt")

    ## too extra
    if [ ! -z $DEBUG ]; then
      echo "pullResult is $pullResult"
    fi

    ## check to see if the file exists that indicates that the app has finished processing
    if [ -f "${tempDir}/${packageName}.txt" ]; then
     
       if [ ! -z $DEBUG ]; then
	 echo "${tempDir}/${packageName}.txt exists"
       fi
       
      ## read the file to see if the 
      dbPath=`cat "${tempDir}/${packageName}.txt"`

      if [ ! -z $DEBUG ]; then
	echo "dbPath is $dbPath"
      fi

      ##
      dbPullResult=$($ADB pull "$dbPath" "$OUTPUT_DIR") 

      if [ ! -z $DEBUG ]; then
        echo "pull db results is $dbPullResult"
      fi
      
      ## delete the file that indicated it was done
      $ADB shell rm /sdcard/daze/${packageName}.txt

      ## also delete the local file
      rm "${tempDir}/${packageName}.txt"

      ## break out of the infinite loop
      DONE="done"
    else       

      ## we are still waiting for it to complete
      if [ ! -z $DEBUG ]; then
        echo "still processing"
      fi
    fi

  done

  echo "uninstalling app $packageName"

  $ADB uninstall "$packageName"

  sleep 1.5

}

processAppsList(){
 
  ## file containing full path of apks to process
  appsFilePath=$1

  ## if restarting, this will contain the apk to process
  currentAppFile=$2

  ## the temp file to delete when done
  tempDir=$3

  ## the current APK to process
  currentApp=

  if [ -f "$currentAppFile" ]; then
    currentApp=$(cat "$currentAppFile")
    echo "restoring apk to process next - $currentApp"
  else
    echo "file does not exist"
  fi

  ## a counter for the file being processed
  counter=1  

  ## total lines to process
  total=$(wc -l $appsFilePath | cut -d ' ' -f1)

  ## iterate through all files and try to pull them from the device to the files directory 
  for filePath in $(cat $appsFilePath) ; do
    
    if [ ! -z "$currentApp" ]; then
      if [ "$filePath" != "$currentApp" ]; then 
        echo "already processed $filePath"
    
        ## increment the counter
        counter=$((counter+1)) 
        
        continue
      else
        echo "found app to process $filePath"
        currentApp=
      fi
    fi

    ## let them know what's up
    echo "[${counter}/${total}] Processing $filePath"

    ## record the file that we are processing
    echo "$filePath" > "$currentAppFile"

    processSingleApp "$filePath" "$tempDir"

    echo "finished processing $filePath"    

    if ((counter % 100 == 0)); then
      echo "reboot at $counter...sleeping 60 seconds"
      $ADB reboot
      sleep 60
    fi

    ## increment the counter
    counter=$((counter+1))

    ## delete the base app directory so it does not fill up
    $ADB shell rm -r /sdcard/daze

  done

  # once this works properly then uncomment the line below
  rm -r "$tempDir"

}

daemonMode() {

  echo "daemon mode active"

  ## run the clear crash dialog in the background
  longRunningClearCrashDialog &

  ## get the PID
  crashClearPID=$!

  ## restart own app if it crashes or gets killed
  restartOwnApp &

  ## get the PID
  appRestartPID=$!

  ## start the log monitoring process
  forceStopAppMonitor &
  
  ## get the PID
  forceStopAppMonitorPID=$!
  
  ## these processes will run until killed or crashed
  wait $crashClearPID $appRestartPID $forceStopAppMonitorPID

  exit 0

}

## process a single app
processOneApp() {

  echo "processOneApp"
 
  if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR=${WORKING_DIR}/output
  fi

  ## get the full path for input directory
  SINGLE_APK_PATH=$(readlink -f "$SINGLE_APK_PATH")

  ## get the full path for the output directory
  OUTPUT_DIR=$(readlink -f "$OUTPUT_DIR")

  ## create the output directory if needed
  if [ ! -d "$OUTPUT_DIR" ]; then
    mkdir "$OUTPUT_DIR"
  fi

    ## make a temp directory to keep some state
  if [ ! -d ${OUTPUT_DIR}/${TEMP_DIR} ]; then
    mkdir ${OUTPUT_DIR}/${TEMP_DIR}
  fi

  ## run the clear crash dialog in the background
  longRunningClearCrashDialog &

  ## get the PID 
  crashClearPID=$!

  ## restart own app if it crashes or gets killed
  restartOwnApp &

  ## get the PID
  appRestartPID=$!

  processSingleApp "$SINGLE_APK_PATH" "${OUTPUT_DIR}/${TEMP_DIR}"

  echo "done!"

  # once this works properly then uncomment the line below
  rm -r "$tempDir"

  ## kill the started processes
  cleanup
}

processAppDirectory() {

  ## make sure there is an input directory
  if [ -z "$INPUT_DIR" ]; then
    echo "No Input file!"
    usage
    exit 1
  fi

  if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR=${WORKING_DIR}/output
  fi

  ## get the full path for input directory
  INPUT_DIR=$(readlink -f "$INPUT_DIR")

  ## get the full path for the output directory
  OUTPUT_DIR=$(readlink -f "$OUTPUT_DIR")

  ## create the output directory if needed
  if [ ! -d "$OUTPUT_DIR" ]; then
    mkdir "$OUTPUT_DIR"
  fi

  if [ ! -z $DEBUG ]; then
    echo INPUT_DIR is "$INPUT_DIR"
    echo OUTPUT_DIR is "$OUTPUT_DIR"
  fi

  ## make a temp directory to keep some state
  if [ ! -d ${OUTPUT_DIR}/${TEMP_DIR} ]; then
    mkdir ${OUTPUT_DIR}/${TEMP_DIR}
    echo "$INPUT_DIR" > ${OUTPUT_DIR}/${TEMP_DIR}/${INPUT_DIR_NAME}
    find "$INPUT_DIR" -type f -name '*.apk' > ${OUTPUT_DIR}/${TEMP_DIR}/${APPS_TO_PROCESS_FILE}
  else
    if [ -f ${OUTPUT_DIR}/${TEMP_DIR}/${INPUT_DIR_NAME} ]; then
      PREVIOUS_INPUT_DIR_NAME=$(<${OUTPUT_DIR}/${TEMP_DIR}/${INPUT_DIR_NAME})
      if [ $PREVIOUS_INPUT_DIR_NAME != $INPUT_DIR ]; then
        echo "starting new directory [$INPUT_DIR] analysis before previous one [$PREVIOUS_INPUT_DIR_NAME] was finished"
        echo "$INPUT_DIR" > ${OUTPUT_DIR}/${TEMP_DIR}/${INPUT_DIR_NAME}
        rm ${OUTPUT_DIR}/${TEMP_DIR}/${CURRENT_APP_FILE}
        find "$INPUT_DIR" -type f -name '*.apk' > ${OUTPUT_DIR}/${TEMP_DIR}/${APPS_TO_PROCESS_FILE}
      else
        ## check if the current apks to process file exists
        if [ ! -f ${OUTPUT_DIR}/${TEMP_DIR}/${APPS_TO_PROCESS_FILE} ]; then
          find "$INPUT_DIR" -type f -name '*.apk' > ${OUTPUT_DIR}/${TEMP_DIR}/${APPS_TO_PROCESS_FILE}
        fi
      fi
    else
      echo "$INPUT_DIR" > ${OUTPUT_DIR}/${TEMP_DIR}/${INPUT_DIR_NAME}
      find "$INPUT_DIR" -type f -name '*.apk' > ${OUTPUT_DIR}/${TEMP_DIR}/${APPS_TO_PROCESS_FILE}
    fi
  fi

  ## run the clear crash dialog in the background
  longRunningClearCrashDialog &

  ## get the PID 
  crashClearPID=$!

  ## restart own app if it crashes or gets killed
  restartOwnApp &

  ## get the PID
  appRestartPID=$!

  ## start processing the apps
  processAppsList "${OUTPUT_DIR}/${TEMP_DIR}/${APPS_TO_PROCESS_FILE}" "${OUTPUT_DIR}/${TEMP_DIR}/${CURRENT_APP_FILE}" "${OUTPUT_DIR}/${TEMP_DIR}"

  echo "done!"

  ## kill the started processes
  cleanup
}

## input directory containing the APKs
INPUT_DIR=

## the name of the input directory that is being processed
INPUT_DIR_NAME=inputDirName.txt

## output directory to put the DB files
OUTPUT_DIR=

## temp dir name
TEMP_DIR=temp

## debug mode
DEBUG=

## get the working directory
WORKING_DIR=$(pwd)

## file that contains list of APKs to process
APPS_TO_PROCESS_FILE=appsToProcess.txt

## the file that contains the app next to be processed
CURRENT_APP_FILE=currentApp.txt

## keep processing until killed
DAEMON=

## log monitoring to stop processed apps
forceStopLogcatPID=

# Android device ID (if using)
DEVICE_ID=

## full path to adb
ADB_FULL_PATH=

## adb command
ADB=adb

## full path to aapt
AAPT_FULL_PATH=

## aapt command
AAPT=aapt

## apk path when processing a single app
SINGLE_APK_PATH=

while getopts i:o:ds:wa:t:q: opt
do
  case "${opt}" in
    i) INPUT_DIR="${OPTARG}";;
    o) OUTPUT_DIR="${OPTARG}";;
    d) DEBUG="debug";;
    s) DEVICE_ID="${OPTARG}";;
    w) DAEMON="true";;
    a) ADB_FULL_PATH="${OPTARG}";;
    t) AAPT_FULL_PATH="${OPTARG}";;
    q) SINGLE_APK_PATH="${OPTARG}";;
    \?) usage
        exit 1;;
  esac
done

if [ ! -z $DEBUG ]; then
  echo "debug mode active"
fi 

if [ ! -z $DAEMON ] && [ ! -z "$INPUT_DIR" ] && [ ! -z "$SINGLE_APK_PATH" ]; then
  echo "you must use either -w (daemon mode) or -i <input dir> (process all apks in a dir) or -q <apk path> (process a single apk)"
  usage
  exit
fi

if [ -z "$ADB_FULL_PATH" ]; then
  RESOLVED_ADB_PATH=$(which adb)
  if [ -z "$RESOLVED_ADB_PATH" ]; then
    echo "No adb in PATH and none supplied as an argument"
    exit 1
  else
    ADB="$RESOLVED_ADB_PATH"
  fi
else
  ADB="$ADB_FULL_PATH"
fi

if [ -z "$AAPT_FULL_PATH" ]; then
  RESOLVED_AAPT_PATH=$(which aapt)
  if [ -z "$RESOLVED_AAPT_PATH" ]; then
    echo "No aapt in PATH and none supplied as an argument"
    exit 1
  else
    AAPT="$RESOLVED_AAPT_PATH"
  fi
else
  AAPT="$AAPT_FULL_PATH"
fi


if [ ! -z "$DEVICE_ID" ]; then
  ADB="$ADB -s $DEVICE_ID"
  if [ ! -z $DEBUG ]; then
    echo adb command is "$ADB"
  fi
fi

if [ ! -z $DAEMON ]; then
  daemonMode
elif [ ! -z "$SINGLE_APK_PATH" ]; then
  processOneApp
else
  processAppDirectory
fi

