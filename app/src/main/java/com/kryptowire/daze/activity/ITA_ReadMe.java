package com.kryptowire.daze.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;

/**
 * Created by ohnonotyou on 4/12/18.
 */

public class ITA_ReadMe extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readme);


        TextView disclaimerHeaderTV = (TextView) findViewById(R.id.disclaimerHeaderTextView);
        String disclaimerHeadertext = "Daze Disclaimer\n";
        disclaimerHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        disclaimerHeaderTV.setText(disclaimerHeadertext);

        TextView disclaimerTV = (TextView) findViewById(R.id.disclaimerTextView);
        String disclaimerText = "Use Daze at your own risk. During our testing, Daze accessed some exported components that programmatically factory reset different devices. There was even an instance of making a device not boot properly. If you"
                + " are testing on a phone that has important data, it is advisable to back it up first prior to testing. A factory reset will wipe all user data and can result in damaging data loss. Daze may also send Intents that may make changes"
                + " changes to settings such as disabling mobile data, forgetting Wi-Fi networks, disabling Wi-Fi, dialing the emergency number 112 which may get forwared to 911, etc. Please use with caution!";

        disclaimerTV.setText(disclaimerText);

        TextView permHeaderTV = (TextView) findViewById(R.id.permHeaderTextView);
        String permHeadertext = "\nGranting required permissions via ADB to the App for Full Functionality\n";
        permHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        permHeaderTV.setText(permHeadertext);

        TextView permTextView = (TextView) findViewById(R.id.permTextView);
        String permText = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            permText = "This application needs to be granted three development permissions to function properly. Please install Android Debugging Bridge (ADB) on your computer. "
                    + "Then Grant the app the DUMP permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.DUMP
                    + "\n\nAlso grant the app the READ_LOGS permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.READ_LOGS +
                    "\n\nAlso grant the app the PACKAGE_USAGE_STATS permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.PACKAGE_USAGE_STATS +
                    "\n\nAfter you grant these three permissions, restart the application and then you will be able to use the buttons on the previous screen. The " + getResources().getString(R.string.intentBarrage) + " and the " + getResources().getString(R.string.oomAttack) + " buttons do not require any permissions to use.";
        } else{
            permText = "This application needs to be granted two development permissions to function properly. Please install Android Debugging Bridge (ADB) on your computer. "
                    + "Then Grant the app the DUMP permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.DUMP
                    + "\n\nAlso grant the app the READ_LOGS permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.READ_LOGS +
                    "\n\nAfter you grant these two permissions, restart the application and then you will be able to use the buttons on the previous screen. The " + getResources().getString(R.string.intentBarrage) + " and the " + getResources().getString(R.string.oomAttack) + " buttons do not require any permissions to use.";
        }

        permText = permText + "\n\nOnce you have enabled Developer options, you should also click the \"Stay awake\" option to enable it in Developer options so that the screen stays on during testing.";

        permTextView.setText(permText);

        TextView scriptHeaderTV = (TextView) findViewById(R.id.scriptHeaderTextView);
        String scriptHeadertext = "\nRunning the Shell Script for Automation\n";
        scriptHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        scriptHeaderTV.setText(scriptHeadertext);

        TextView scriptTV = (TextView) findViewById(R.id.scriptTextView);
        String scriptText = "Daze requires a bash script to run just prior to analyzing a single app or all apps on a device. This script will be copied to the path " + ITA_Constants.outputDir + "/daze.sh on the SD card each"
        +" time the app is run. This script will close any crash dialogs and restart the app if it killed by OS or is running on a device that does not send out the " + Intent.ACTION_BOOT_COMPLETED + " broadcast intent."
        +" It is possible not to use this script, but you will need to manually close crash dialogs quickly as they occur. In addition, you will need to monitor log cat to ensure that the app is restarted after a system"
        +" crash. This can be done by clicking on Daze in the launcher. A small amount of devices will not send out the " + Intent.ACTION_BOOT_COMPLETED + " after a system crash. In the general case, Daze will be restarted"
        +" by this intent. If testing multiple Android devices concurrently, the -s <device ID> option should be used. For whole device testing, the -w option should be used and this will run the Daze script until it is "
        +" manually killed by the user. Daze can also serially test a directory containing APKs with the -i <input dir> option. A single APK can be tested with the -q <apk path> option. This script should work fine on Mac"
        +" and Linux, but you may need some additional software to run the bash script. The script requires that the Android command line tools from the SDK be installed. Specifically, only adb and aapt need to be present."
        +" These two binaries do not need to be on the path if they are passed in using the appropriate script options (e.g., -a and -t).";
        scriptTV.setText(scriptText);

        TextView logcatHeaderTV = (TextView) findViewById(R.id.logcatHeaderTextView);
        String logcatHeadertext = "\nMonitoring Daze via Logcat\n";
        logcatHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        logcatHeaderTV.setText(logcatHeadertext);

        TextView logcatTV = (TextView) findViewById(R.id.logcatTextView);
        String logcatText = "Daze writes extensive information to the logcat log. Using logcat, you can view the progress of each intent being tested and any interesting results. All of the log tags in Daze start with the"
                +" text of \"ITA_\". To monitor the progress of Daze, you can type the following command \"adb logcat | grep \"ITA_\" and examine the log messages or redirect the output to a file for later examination.";
        logcatTV.setText(logcatText);

        TextView notificationHeaderTV = (TextView) findViewById(R.id.notificationHeaderTextView);
        String notificationHeadertext = "\nMonitoring Daze via Notifications\n";
        notificationHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        notificationHeaderTV.setText(notificationHeadertext);

        TextView notificationTV = (TextView) findViewById(R.id.notificationTextView);
        String notificationText = "Daze creates notifications that show the current step being performed in the analysis. The three main phases of daze are gathering the available intents to send on the device, sending the intents,"
        + ", and then processing the logs from intent sending and resending any intent that caused a crash to see if sending the intent again will cause a crash. System crashes will be tested twice. These are tested twice in a row "
        + " during the intent sending phase. A user can examine the current notification to see the current phase daze is executing. When daze finishes an analysis, it will open an activity showing the results of the analysis.";
        notificationTV.setText(notificationText);

        TextView dazeOptionsHeaderTV = (TextView) findViewById(R.id.dazeOptionsHeaderTextView);
        String dazeOptionsHeadertext = "\nDaze Options\n";
        dazeOptionsHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        dazeOptionsHeaderTV.setText(dazeOptionsHeadertext);

        TextView dazeOptionsTV = (TextView) findViewById(R.id.dazeOptionsTextView);
        String dazeOptionsText = "The main activity of Daze presents the user with a number of options. Here is an explanation of each option.\n\n " + getResources().getString(R.string.startAnalysis) + " - Daze will test all"
        +" apps on the device using the options set in the Settings options\n\n" + getResources().getString(R.string.testIndividualApp) + " - Daze will test a single app. The user can select the"
        +" package name from all apps installed on the device.\n\n" + getResources().getString(R.string.stopAnalysis) + " - This allows the user to stop Daze prior to it finishing processing a single app or all apps on the device.\n\n"
        + getResources().getString(R.string.examineResults) + " - This allows the user to examine the results of Daze from the last processing job. It will show the crashes that occurred, if any, for both regular and system"
        + " processes. In addition, it will show changes that occurred, if the option is selected, in system settings, system properties, and new files appearing on the SD card.\n\n" + getResources().getString(R.string.settings)
        + " - Provides the user the option when prior to performing an analysis with Daze. The options allow the user whether to test all types of components or a subset there of. There is an option to test only the android"
        + " package (system_server). Testing only this process will be the quickest way to discover any system crashes on the device if they exist. Once this option is selected, it will be tested with the "
        + getResources().getString(R.string.startAnalysis) + " option. Alternatively, the android package can be directly selected with the " + getResources().getString(R.string.testIndividualApp) + " option. There is an option"
        + " for accelerated testing to make the analysis go faster, but it cannot be used with system monitoring. There are options to limit it so that only one stacktrace per crash in an application component is shown."
        + " In addition some devices may have a process that will repeatedly crash until the device is rebooted. There is an option to filter these out. There is also an option to test the broadcast receivers that a component"
        + " dynamically registers in addition to testing the component itself.\n\n" + getResources().getString(R.string.intentBarrage) + " - Will obtain all the intents possible to send on the device and send them. This has the"
        + " possibility to crash the device.\n\n" + getResources().getString(R.string.oomAttack) + " - This will exhaust the heap of system_server and cause the device to eventually crash." ;

        dazeOptionsTV.setText(dazeOptionsText);

        TextView resultsHeaderTV = (TextView) findViewById(R.id.resultsHeaderTextView);
        String resultsHeadertext = "\nExamining Results\n";
        resultsHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        resultsHeaderTV.setText(resultsHeadertext);

        TextView resultsTV = (TextView) findViewById(R.id.resultsTextView);
        String resultsText = "Daze records the results after each analysis run (either for a single app or the whole device). The " + getResources().getString(R.string.examineResults) + " option allows the user to manually examine the"
        + " the results.  In addition, an SQLite database for the run will be copied to the sd card in the specific directory for the analysis run. The primary directory for daze is " + ITA_Constants.outputDir + ". This contains all"
        + " files daze creates on external storage. Daze creates a directory in this directory named after the device name (value of the ro.product.name system property) which contains a directory for each time daze is run. The directory"
        + " directory name for each run where the directory is <device name>_<unix timestamp>. This directory will contain the database containing the results. It also contains the logs for when each intent was sent although only the log"
        + " messages with exception are recorded (will contains exceptions and not general log messages). If the analysis was for a single file the database name contain the package name, version name, version code, and an MD5 of the apk."
        + " If the analysis was for all apps on the device, the database name will be the value of the build fingerprint system property (ie, ro.build.fingerprint).";
        resultsTV.setText(resultsText);

        TextView dazeHeaderTV = (TextView) findViewById(R.id.dazeHeaderTextView);
        String dazeHeadertext = "\nDaze Functionality\n";
        dazeHeaderTV.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        dazeHeaderTV.setText(dazeHeadertext);

        TextView dazeTV = (TextView) findViewById(R.id.dazeTextView);
        String dazeText = "Daze will test either an single app or all apps on the device. Daze will enumerate the application component interfaces that are exported and do not require a permission to access. Daze will send a number of intents."
        + " to all application components it finds. All app components (except for Content Resolvers) are accessed via intents. The Content Resolvers will be tested using a Content Resolver. Daze starts by sending a minimal intent with just"
        + " enough information to address the component. Developers may forget to perform null-checking at runtime and not include adequate exception handling. This can lead to an uncaught exception and process termination. This can also happen"
        + " in an Android OS process named system_server and cause a system crash. Additional fields to the intent are filled-in and sent to the target application component to see if the process containing the component will crash. Although"
        + " NullPointerExceptions are encountered the most a number of other exceptions can occur such as SuperNotCalledException, ClassNotFoundException, IllegalArgumentException, RuntimeException, SecurityException, UnsatisfiedLinkError, and more."
        + " Daze has the ability to monitor system settings, system properties, and the state of files on the SD card. Daze will associate these changes with sending an intent, so it can uncover certain behavior such as the taking of screenshots,"
        + " enabling/disabling wireless communication (eg, WiFi, Bluetooth, etc), and the writing of arbitrary files to the SD card.";
        dazeTV.setText(dazeText);



    }

}
