package com.kryptowire.daze.service;

import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.ITA_MainAct;
import com.kryptowire.daze.R;
import com.kryptowire.daze.activity.ITA_ExceptionTypeSelection;
import com.kryptowire.daze.provider.ITA_Contract;
import com.kryptowire.daze.provider.ITA_DBHelper;
import com.kryptowire.daze.receiver.ITA_BootReceiver;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.Manifest;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

// grant the two development permission for < Android 8.0
//adb shell pm grant com.kryptowire.daze android.permission.READ_LOGS
//adb shell pm grant com.kryptowire.daze android.permission.DUMP

// grant the third development permission for  >= Android 8.0
//adb shell pm grant com.kryptowire.daze android.permission.PACKAGE_USAGE_STATS

// if trying to automatically have the app re-enable adb, then grant this perm
//adb shell pm grant intent.test.app android.permission.WRITE_SECURE_SETTINGS

// get foreground activity
// adb shell dumpsys window windows | grep mCurrentFocus
//   mCurrentFocus=Window{18b84a6 u0 SystemUIDialog}

public class ITA_IntentService extends Service {

    final static boolean DEBUG = true;
    final static String TAG = ITA_IntentService.class.getSimpleName();
    static HashSet<String> troublesomeBroadcastActions = new HashSet<String>(); // broadcast that are known to have negative effects or cannot be sent due to privileges
    static HashSet<String> troublesomeApplicationComponents = new HashSet<String>(); // broadcast that are known to have negative effects on the phone
    static String currentDir; // current run directory
    static String currentLogDir; // current log directory
    static final String SP = ITA_IntentService.class.getPackage().getName(); // shared preferences file name
    static String newLine = System.getProperty("line.separator"); // platform new-line
    static final int notificationID = 327198;
    static final boolean performLoggingDuringIntentSending = true; // if true, a logcat capture will be produced for each broadcast intent
    static boolean keepGoing = true; // while true, continue the analysis
    static Thread analysisThread = null; // the thread doing the analysis
    static boolean keepGoingLogging = true; // while true, continue the analysis
    static Thread logThread = null; // the thread reading the log
    static Thread analyzeLogsThread = null; // the thread reading the log
    static Bundle emptyBundle = new Bundle();
    static boolean foundCrash = false;
    static volatile boolean foundCrashEternalThread = false;
    static volatile boolean foundSystemCrashEternalThread = false;
    static volatile boolean isSendingAcceleratedIntents = false;
    static volatile int currentID = 1;
    static boolean continueParsingLog = false;
    static boolean crashDueToCrashInDynamicBroadcastReceiver = false;
    static boolean foundException;
    static BufferedReader br;
    static boolean getAppMD5 = true;
    static final File EXTERNAL_STORAGE_DIR = Environment.getExternalStorageDirectory();
    static HashSet<String> EXTERNAL_STORAGE_FILES_HASHSET_BEFORE = new HashSet<String>(); // hash set for containing files on external storage before an intent
    static HashSet<String> EXTERNAL_STORAGE_FILES_HASHSET_AFTER = new HashSet<String>(); // hash set for containing files on external storage after an intent
    static HashSet<String> SETTINGS_BEFORE = new HashSet<String>(); // hash set for containing settings before an intent
    static HashSet<String> SETTINGS_AFTER = new HashSet<String>(); // hash set for containing settings after an intent
    static HashSet<String> SYSTEM_PROPERTIES_BEFORE = new HashSet<String>(); // hash set for containing system properties before an intent
    static HashSet<String> SYSTEM_PROPERTIES_AFTER = new HashSet<String>(); // hash set for containing system properties after an intent
    static String currentPackageNameDynamicBroadcastReceiver; // the package name for which the dynamically registered broadcast receivers will be stored
    static HashSet<String> baselinePackageDynamicBroadcastReceivers = new HashSet<String>(); // the cached broadcast receiver actions used as a baseline
    static HashSet<String> testedDynamicBroadcastReceiversForPackage = new HashSet<String>(); // first time an action get registered for a broadcast receiver if it gets reloaded, it will look like another app did it
    static HashSet<String> CRASHED_COMPONENTS = new HashSet<String>(); // hash set for containing components and actions that have a crash
    static String currentPackageName; // the current app being analyzed
    static boolean contentProviderTestFinishes = false; // a check to see if the content provider test finished properly or hangs
    static boolean isFirstComponentOfAnAppDynBR = false; // a boolean to note if the baseline dynamic broadcast receivers should be recoreded for the app

    // * ReceiverList{1dba60c1 19163 com.google.android.gms.persistent/10011/u0 remote:2749fda8}
    // * ReceiverList{79324fa 16623 system/1000/u-1 local:com.samsung.android.hqm.BigDataModule$1@de9731c,120bb25}
    static final String receiverListRegex = "\\s*\\* ReceiverList\\{(.*) (\\d+) (.*)/(\\d+)/(u|u-)(\\d+) (.*):(.*)\\}";
    static final Pattern receiverListPattern = Pattern.compile(receiverListRegex);

    // Action: "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE"
    static final String actionRegex = "\\s*Action: \\\"(.*)\\\"\\s*";
    static final Pattern actionPattern = Pattern.compile(actionRegex);

    // requiredPermission=android.permission.BACKUP
    static final String permissionRegex = "\\s*requiredPermission=(.*)\\s*";
    static final Pattern permissionPattern = Pattern.compile(permissionRegex);

    //     Filter #0: BroadcastFilter{c8a76c9}
    static final String broadcastFilterRegex = "\\s*Filter\\s*#\\d+:\\s*BroadcastFilter\\{\\S+\\}\\s*";
    static final Pattern broadcastFilterPattern = Pattern.compile(broadcastFilterRegex);

    //    E: protected-broadcast (line=251)
    static final String protectedBroadcastRegex = "\\s*E: protected-broadcast \\(line=(\\d+)\\)\\s*";
    static final Pattern protectedBroadcastPattern = Pattern.compile(protectedBroadcastRegex);

    // A: android:name(0x01010003)="android.net.wifi.p2p.CONNECTION_STATE_CHANGE" (Raw: "android.net.wifi.p2p.CONNECTION_STATE_CHANGE")
    static final String protectedActionRegex = "\\s*A: android:name\\(0x(\\d+)\\)=\\\"(.*)\\\" \\(Raw: \\\"(.*)\\\"\\)";
    static final Pattern protectedActionPattern = Pattern.compile(protectedActionRegex);

    // regex line for a failure in system_server
    // example: 02-26 16:10:29.568  3501  3501 E AndroidRuntime: *** FATAL EXCEPTION IN SYSTEM PROCESS: main
    //static final String fatalExceptionSystemRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*): (.*FATAL EXCEPTION IN SYSTEM PROCESS.*)";
    static final String fatalExceptionSystemRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*): (.*FATAL EXCEPTION IN SYSTEM PROCESS:) (.*)";
    static final Pattern fatalExceptionSystemPattern = Pattern.compile(fatalExceptionSystemRegex);
    // 06-25 11:07:57.140   829   850 E AndroidRuntime: *** FATAL EXCEPTION IN SYSTEM PROCESS: android.bg

    // regex line for a fatal exception in a process
    // example: com.sec.rcs.mediatransfer.vsh.action.SHARE_ACCEPT.txt:02-26 16:09:27.658 18067 18067 E AndroidRuntime: FATAL EXCEPTION: main
    public static final String fatalExceptionRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?): (.*FATAL EXCEPTION.*)";
    public static final Pattern fatalExceptionPattern = Pattern.compile(fatalExceptionRegex);

    // standard log line
    // example: 02-26 16:09:13.768  3069  3737 D Netd    : getNetworkForDns: using netid 502 for uid 1000
    // example: 02-26 16:08:35.918  5934  5934 E AndroidRuntime: Process: com.sec.android.inputmethod, PID: 5934
    static final String logLineRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?): (.*)";
    static final Pattern logLinePattern = Pattern.compile(logLineRegex);

    // log line showing exception cause
    // example: 02-26 16:10:29.568  3501  3501 E AndroidRuntime: Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'android.net.NetworkInfo$DetailedState android.net.NetworkInfo.getDetailedState()' on a null object reference
    static final String exceptionCauseRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*Caused by:\\s*(.*?):\\s*(.*)";
    static final Pattern exceptionCausePattern = Pattern.compile(exceptionCauseRegex);

    // example: 09-03 22:48:20.429 21618 21618 E AndroidRuntime: java.lang.RuntimeException: Unable to resume activity {com.android.providers.downloads.ui/com.android.providers.downloads.ui.OmaDownloadActivity}: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.equals(java.lang.Object)' on a null object reference
    // example: 06-24 21:28:06.298 13729 13729 E AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String java.lang.String.replace(java.lang.CharSequence, java.lang.CharSequence)' on a null object reference
    //static final String exceptionCause2Regex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*Attempt to\\s*(.*):\\s*(.*)";
    static final String exceptionCause2Regex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*(.*):\\s*Attempt to\\s*(.*)";
    static final Pattern exceptionCause2Pattern = Pattern.compile(exceptionCause2Regex);

    // example: 09-03 23:16:23.682 30706 30706 E AndroidRuntime: android.util.SuperNotCalledException: Activity {com.google.android.gms/com.google.android.gms.wallet.common.ui.UpdateAddressActivity} did not call through to super.onCreate()
    // example: 09-03 23:06:45.969 27876 27876 E AndroidRuntime: java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
    static final String exceptionCause3Regex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*(.*?):\\s*(.*)";
    static final Pattern exceptionCause3Pattern = Pattern.compile(exceptionCause3Regex);

    // example: 10-04 02:21:20.637 20193 20193 E AndroidRuntime: java.lang.NullPointerException
    static final String exceptionCause4Regex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*(.*Exception|.*Error)";
    static final Pattern exceptionCause4Pattern = Pattern.compile(exceptionCause4Regex);

    // log line showing the crashed process
    // example: 02-26 16:09:27.658 18067 18067 E AndroidRuntime: Process: com.sec.imsservice, PID: 18067
    static final String crashedProcessRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*Process:\\s*(.*), PID:\\s*(.*)";
    static final Pattern crashedProcessPattern = Pattern.compile(crashedProcessRegex);

    //example: 07-02 00:52:36.935 11411 11429 W ITA_IntentService: [499/855] Starting activity - packageName=com.google.android.gms, componentName=com.google.android.gms.wallet.ow.ChooseAccountShimActivity
    //example: 07-02 01:29:32.150 11411 11429 W ITA_IntentService: [5/283] Starting service - packageName=com.google.android.googlequicksearchbox, componentName=com.google.android.apps.gsa.projection.CarAssistantService
    //example: 07-02 02:04:16.339 11411 11429 W ITA_IntentService: [73/349] Starting receiver - packageName=com.google.android.apps.genie.geniewidget, componentName=com.google.android.apps.genie.geniewidget.miniwidget.MiniDarkWidgetProvider
    static final String startApplicationComponentSentIntentRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*\\[\\d+/\\d+\\] Starting (" + ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME + "|" + ITA_Contract.ServiceTable.SERVICE_TABLE_NAME + "|" + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + "|" + ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME + ") - packageName=(.*), componentName=(.*)\\s*";
    static final Pattern startApplicationComponentSentIntentPattern = Pattern.compile(startApplicationComponentSentIntentRegex);

    //example: 07-01 23:53:09.073 19259 19277 W ITA_IntentService: [40/119] Sending broadcastaction - action=android.intent.action.MEDIA_SHARED
    static final String sendBroadcastActionIntentRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*\\[\\d+/\\d+\\] Sending " + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + " - action=(.*)\\s*";
    static final Pattern sendBroadcastActionIntentPattern = Pattern.compile(sendBroadcastActionIntentRegex);

    // example: 11-05 17:17:59.311  4424  4497 E ITA_IntentService: [dynamic broadcast receiver] packageName=com.google.android.youtube, componentName=com.google.android.apps.youtube.app.application.Shell$HomeActivity, componentType=activity, action=android.net.conn.CONNECTIVITY_CHANGE
    static final String sendDynamicBroadcastActionIntentRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*\\[dynamic broadcast receiver\\] packageName=(.*), componentName=(.*), componentType=(.*), action=(.*)\\s*";
    static final Pattern sendDynamicBroadcastActionIntentPattern = Pattern.compile(sendDynamicBroadcastActionIntentRegex);

    // example: 03-06 22:13:54.694   198   198 F DEBUG   : pid: 9623, tid: 9623, name: e.nativecodeapp  >>> com.kryptowire.nativecodeapp <<<
    static final String finishedSendingInitalRoundOfIntentsRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*" + ITA_Constants.FINISHED_INITIAL_SENDING_OF_INTENTS;
    static final Pattern finishedSendingInitalRoundOfIntentsPattern = Pattern.compile(finishedSendingInitalRoundOfIntentsRegex);

    // example: 03-06 22:13:54.694   198   198 F DEBUG   : pid: 9623, tid: 9623, name: e.nativecodeapp  >>> com.kryptowire.nativecodeapp <<<
    public static final String nativeCrashRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*(pid:\\s(.*), tid: (.*), name: (.*)\\s*>>>\\s*(.*)\\s*<<<)\\s*";
    public static final Pattern nativeCrashPattern = Pattern.compile(nativeCrashRegex);

    // example: 03-06 22:30:31.248   198   198 F DEBUG   : signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
    static final String nativeCrashReasonRegex = "\\s*(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(\\d+)\\s*(\\d+)\\s*(\\D)\\s*(.*?):\\s*((.*),\\s*(.*),\\s*(.*))";
    static final Pattern nativeCrashReasonPattern = Pattern.compile(nativeCrashReasonRegex);

    // example: package:/system/priv-app/Settings/Settings.apk=com.android.settings
    static final String pmOutputRegex = "\\s*package:(.*?).apk=(.*)\\s*";
    static final Pattern pmOutputPattern = Pattern.compile(pmOutputRegex);

    static class ContentObserverImpl extends ContentObserver {

        public ContentObserverImpl(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            return;
        }
    }

    class SecureTableContentObserver extends ContentObserver {

        public SecureTableContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {

            if (uri == null)
                return;

            Log.w(TAG, "Uri changed = " + uri.toString() + ", selfChange=" + selfChange);

            // self change so ignore
            if (selfChange == true)
                return;

            // get the last part of the Uri
            String last = uri.getLastPathSegment();
            if (last == null || last.isEmpty())
                return;

            try {
                if (last.equals(Settings.Secure.ADB_ENABLED)) {

                    Settings.Secure.putInt(getContentResolver(), "", 0);

                    int adb_enabled;
                    try {
                        adb_enabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ADB_ENABLED);
                    } catch (SettingNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                    Log.w(TAG, "current setting: adb_enabled=" + adb_enabled);
                    if (adb_enabled == 0) {
                        Log.w(TAG, "enabling adb_enabled");
                        Settings.Secure.putInt(getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
                    }
                    return;
                }

                if (last.equals(Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED)) {
                    int development_settings_enabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0);
                    Log.w(TAG, "current setting: development_settings_enabled=" + development_settings_enabled);
                    if (development_settings_enabled == 0) {
                        Log.w(TAG, "enabling development_settings_enabled");
                        Settings.Secure.putInt(getContentResolver(), Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 1);
                    }
                    return;
                }
            } catch (Exception e) {}
        }
    }

    @SuppressLint("NewApi")
    class GlobalTableContentObserver extends ContentObserver {

        public GlobalTableContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {

            if (uri == null)
                return;

            Log.w(TAG, "Uri changed = " + uri.toString() + ", selfChange=" + selfChange);

            // self change so ignore
            if (selfChange == true)
                return;

            // get the last part of the Uri
            String last = uri.getLastPathSegment();
            if (last == null || last.isEmpty())
                return;

            try {
                if (last.equals(Settings.Global.ADB_ENABLED)) {
                    int adb_enabled = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0);
                    Log.w(TAG, "current setting: adb_enabled=" + adb_enabled);
                    if (adb_enabled == 0) {
                        Log.w(TAG, "enabling adb_enabled");
                        Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                    }
                    return;
                }

                if (last.equals(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)) {
                    int development_settings_enabled = Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
                    Log.w(TAG, "current setting: development_settings_enabled=" + development_settings_enabled);
                    if (development_settings_enabled == 0) {
                        Log.w(TAG, "enabling development_settings_enabled");
                        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                    }
                    return;
                }

                if (last.equals(Settings.Global.STAY_ON_WHILE_PLUGGED_IN)) {
                    int development_settings_enabled = Settings.Global.getInt(getContentResolver(), Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0);
                    Log.w(TAG, "current setting: stay_on_while_plugged_in=" + development_settings_enabled);
                    if (development_settings_enabled == 0) {
                        Log.w(TAG, "enabling stay_on_while_plugged_in");
                        Settings.Global.putInt(getContentResolver(), Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 1);
                    }
                    return;
                }
            } catch (Exception e) {}
        }
    }


    static ContentObserverImpl coi = new ContentObserverImpl(new Handler());

    // this method will register content observers to listen to each of the three important tables in the settings.db.
    // this allows the service to be alerted when a value changes, so that it can be changed back according to the value
    // that the policy dictates
    @SuppressLint("NewApi")
    private void registerSettingsListeners() {
        int wssPermission = this.checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS, android.os.Process.myPid(), android.os.Process.myUid());
        if (wssPermission == PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                GlobalTableContentObserver globalSettingsContentObserver = new GlobalTableContentObserver(new Handler());
                getContentResolver().registerContentObserver(android.provider.Settings.Global.CONTENT_URI, true, globalSettingsContentObserver);
            } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.JELLY_BEAN) {
                SecureTableContentObserver secureSettingsContentObserver = new SecureTableContentObserver(new Handler());
                getContentResolver().registerContentObserver(android.provider.Settings.Secure.CONTENT_URI, true, secureSettingsContentObserver);
            }
        }
    }

    @Override
    public void onCreate() {
        if (DEBUG)
            Log.w(TAG, "onCreate");

        // populate broadcast actions that can interfere with the analysis
        populateKnowntroublesomeBroadcastActions();

        // populate application components that can interfere with the analysis
        populateKnownToublesomeApplicationComponents();

        currentDir = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR);

        currentLogDir = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_DIR);

        registerSettingsListeners();

        createPlaceHolderAlarm();
    }

    public void createPlaceHolderAlarm() {
        Intent i = new Intent(ITA_Constants.PLACEHOLDER_INTENT_ACTION);
        i.setClass(getApplicationContext(), ITA_BootReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), ITA_Constants.PLACE_HOLDER_REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 999999999, 999999999, sender);
        Log.w(TAG, "placeholder alarm set");
    }

    // stat method to create the log output
    public static Process createLogcatProcess(boolean clearLog) throws IOException {

        if (clearLog) {
            String[] commandClear = {"logcat", "-c"};
            execCommandNoOutput(commandClear);
            try {
                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CLEAR_LOG);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // command to start logcat
        String[] cmd = {"logcat", "-v", "threadtime", "-s", "AndroidRuntime:E", "ITA_IntentService:E", "DEBUG:E"};

        // execute the logcat command and get the buffered reader
        Process logcat = Runtime.getRuntime().exec(cmd);

        return logcat;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (DEBUG)
            Log.w(TAG, "onStartCommand");
        if (intent == null) {
            Log.w(TAG, "onStartCommand - intent is null");
            boolean isSendingIntents = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS);
            if (isSendingIntents) {
                launchThreadToResumeSendingIntents(null);
                return START_STICKY;
            }
            boolean isAnalyzingLogs = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ANALYZING_LOGS);
            if (isAnalyzingLogs) {
                launchAnalyzeLogsThread();
                return START_STICKY;
            }
            return START_STICKY;
        }
        String action = intent.getAction();
        Log.w(TAG, "action=" + action);
        if (action == null) {
            Log.w(TAG, "onStartCommand - action is null");
            boolean isSendingIntents = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS);
            if (isSendingIntents) {
                launchThreadToResumeSendingIntents(null);
                return START_STICKY;
            }
            boolean isAnalyzingLogs = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ANALYZING_LOGS);
            if (isAnalyzingLogs) {
                launchAnalyzeLogsThread();
                return START_STICKY;
            }
            return START_STICKY;
        }
        if (action.equals(ITA_Constants.START_ANALYSIS)) {
            if (intent.hasExtra(ITA_Constants.PACKAGE_NAME)) {
                String target = intent.getStringExtra(ITA_Constants.PACKAGE_NAME);
                if (target != null && target.length() != 0) {
                    Log.w(TAG, "Analysis on package name - " + target);
                    writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER, false);
                    writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP, target);

                    File f = new File(ITA_Constants.outputDir + "/" + target + ".txt");
                    if (f.exists())
                        f.delete();
                } else
                    writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP, null);
            } else
                writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP, null);

            Log.w(TAG, "onlySystemServer=" + readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER));

            launchStartAnalysisThread();
            return START_STICKY;
        } else if (action.equals(ITA_Constants.STOP_ANALYSIS)) {
            stopAnalysis();
            return START_STICKY;
        } else if (action.equals(ITA_Constants.ANALYZE_LOGS)) {
            Log.w(TAG, "start analyzing logs");
            this.launchAnalyzeLogsThread();
            return START_STICKY;
        } else if (action.equals(ITA_Constants.INTENT_BARRAGE)) {
            Log.w(TAG, "intentBarrage");
            this.launchIntentBarrage();
            return START_STICKY;
        } else if (action.equals(ITA_Constants.CONTINUE_TESTING_ACTION) || action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS)) {
            boolean isSendingIntents = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS);
            Log.w(TAG, "isSendingIntents=" + isSendingIntents);
            if (isSendingIntents)
                launchThreadToResumeSendingIntents(action);
            return Service.START_STICKY;
        }
        return START_STICKY;
    }

    private void launchThreadToResumeSendingIntents(final String action) {

        Log.w(TAG, "launchThreadToResumeSendingIntents()");

        if (analysisThread == null || !analysisThread.isAlive()) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ITA_IntentService.this.resumeSendingIntents(action);
                }
            };
            analysisThread = new Thread(r);
            analysisThread.start();

        } else {
            Log.w(TAG, "analysisThread is not null - not launching");
        }
    }

    private void resumeSendingIntents(String action) {

        clearLog();

        Log.w(TAG, "resumeSendingIntents");

        createNotification("Performing Analysis");

        currentID = readIntSharedPreferences(getApplicationContext(), ITA_Constants.GLOBAL_SEQUENTIAL_ID);

        keepGoingLogging = true;

        startLogcatExceptionMonitorThread();

        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_LOG_START);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String currentTable = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_TABLE);
        int lastSentRow = readIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID);

        Log.w(TAG, "currentTable=" + currentTable + ", lastSentRow=" + lastSentRow);

        if (lastSentRow == -1) {
            Log.w(TAG, "Last Intent Sent Before Reboot contained a soft reboot during accelerated testing!");
            lastSentRow = 1;
        }

        boolean justSoftRebootedOrAppKilled = true;

        if (currentTable.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME)) {
            String authority = readStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY);
            Log.w(TAG, "Last Intent Sent Before Reboot: lastAuthorityTested=" + authority + ", currentTable=" + currentTable + ", lastSentRow=" + lastSentRow);
        } else {
            String lastIntent = readStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT);
            Log.w(TAG, "Last Intent Sent Before Reboot: lastIntentSent=" + lastIntent + ", currentTable=" + currentTable + ", lastSentRow=" + lastSentRow);
        }

        if (currentTable.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME) && keepGoing) {
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME);
            resumeContentProvider(ITA_Contract.ProviderTable.CONTENT_URI, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, lastSentRow, justSoftRebootedOrAppKilled, action);
            currentTable = ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME;
            lastSentRow = 1; // reset the row
            justSoftRebootedOrAppKilled = false;
            action = null;
        }

        if (currentTable.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME) && keepGoing) {
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME);
            resumeApplicationComponent(ITA_Contract.ActivityTable.CONTENT_URI, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, false, lastSentRow, justSoftRebootedOrAppKilled, action);
            currentTable = ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME;
            lastSentRow = 1; // reset the row
            justSoftRebootedOrAppKilled = false;
            action = null;
        }

        if (currentTable.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME) && keepGoing) {
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME);
            resumeApplicationComponent(ITA_Contract.BroadcastActionTable.CONTENT_URI, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, true, lastSentRow, justSoftRebootedOrAppKilled, action);
            currentTable = ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME;
            lastSentRow = 1; // reset the row
            justSoftRebootedOrAppKilled = false;
            action = null;
        }

        if (currentTable.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME) && keepGoing) {
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME);
            resumeApplicationComponent(ITA_Contract.ReceiverTable.CONTENT_URI, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, false, lastSentRow, justSoftRebootedOrAppKilled, action);
            currentTable = ITA_Contract.ServiceTable.SERVICE_TABLE_NAME;
            lastSentRow = 1; // reset the row
            justSoftRebootedOrAppKilled = false;
            action = null;
        }

        if (currentTable.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME) && keepGoing) {
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME);
            resumeApplicationComponent(ITA_Contract.ServiceTable.CONTENT_URI, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, false, lastSentRow, justSoftRebootedOrAppKilled, action);
            lastSentRow = 1; // reset the row
            justSoftRebootedOrAppKilled = false;
            action = null;
        }

        logFinishInitialSendingOfIntents();

        // indicate that we are done
        keepGoing = false;

        analysisThread = null;

        // denote that the analysis has stopped
        writeBooleanSharedPreferences(this.getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS, false);

        recordTimeFile(ITA_Constants.FINISH_INTENT_SENDING_TIMESTAMP);

        createNotification("Finished Sending Intents");

        Log.w(TAG, "Finished Sending Intents");

        // reset the last intent sent data
        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, -1);

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT, "");

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY, "");

        writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, "");

        launchAnalyzeLogsThread();
    }


    synchronized private void launchAnalyzeLogsThread() {

        Log.w(TAG, "launchAnalyzeLogsThread()");

        if (analyzeLogsThread == null || !analyzeLogsThread.isAlive()) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ITA_IntentService.this.analyzeLogs();
                    analyzeLogsThread = null;
                }
            };
            analyzeLogsThread = new Thread(r);
            analyzeLogsThread.start();
        }
    }

    private void stopAnalysis() {

        // let log readers know
        Log.w(TAG, "stopAnalysis()");

        // copy the intents.db to the sd card
        copyDatabaseToSdcard();

        // make the analysis thread exit
        keepGoing = false;

        // null out the analysis thread so it can start again
        analysisThread = null;

        // stop the logging thread
        keepGoingLogging = false;

        // write a message with Log.e so that the thread will wake from waiting to read from log
        Log.e(TAG, "die logging thread");

        logThread = null;

        // denote that the analysis has stopped
        writeBooleanSharedPreferences(this.getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS, false);

        // denote that the log analysis is over
        writeBooleanSharedPreferences(this.getApplicationContext(), ITA_Constants.ANALYZING_LOGS, false);

        // reset the last intent sent data
        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, -1);

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT, "");

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY, "");

        writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, "");

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP, null);

        // show the user that the analysis has stopped
        createNotification("Stopped Analysis");

        // record the ending time
        recordTimeFile(ITA_Constants.STOP_TIMESTAMP);

    }

    // will delete all rows in the tables in the db
    private void deleteAllRowsFromTables() {
        ContentResolver cr = this.getContentResolver();


        try {
            cr.delete(ITA_Contract.ActivityTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            cr.delete(ITA_Contract.ServiceTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.ReceiverTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.BroadcastActionTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.ProviderTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.ResultsTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.AppsTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.TimeTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.AllComponentsTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // will delete all rows in the tables in the db
    private int deleteAllRowsFromSingleTable(Uri target) {
        ContentResolver cr = this.getContentResolver();
        return cr.delete(target, null, null);
    }

    synchronized Thread startLogcatExceptionMonitorThread() {
        Log.w(TAG, "startLogcatExceptionMonitorThread");
        if (logThread == null || !logThread.isAlive()) {
            Log.w(TAG, "startLogcatExceptionMonitorThread starting");
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    while (keepGoingLogging) {
                        int result = ITA_IntentService.this.parseLogcatForCrashEternal();
                        Log.w(TAG, "parseLogcatForCrashEternal() returnValue=" + result);
                        if (result == ITA_Constants.LOG_READER_NORMAL_EXIT) {
                            break;
                        }
                    }
                }
            };
            logThread = new Thread(r);
            logThread.setPriority(Thread.MAX_PRIORITY);
            logThread.start();
        } else {
            Log.w(TAG, "startLogcatExceptionMonitorThread already running");
        }
        return logThread;
    }

    static void clearLog() {
        String[] commandClear = {"logcat", "-c"};
        execCommandNoOutput(commandClear);
        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CLEAR_LOG);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static BufferedReader createReader() {
        Process process = null;
        BufferedReader br = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String[] commandClear = {"logcat", "-G", "1m"};
            execCommandNoOutput(commandClear);

            try {
                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_INCREASE_LOG_BUFFER);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        boolean createdReader = false;
        for (int a = 0; a < 3 && createdReader == false; a++) {
            try {
                process = createLogcatProcess(true);
                br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                createdReader = true;
            } catch (IOException e) {
                e.printStackTrace();
                Log.w(TAG, "parseLogcatForCrashEternal - failure creating logcat process");
            }
        }
        return br;
    }

    int parseLogcatForCrashEternal() {
        foundCrashEternalThread = false;
        foundSystemCrashEternalThread = false;
        Log.w(TAG, "parseLogcatForCrashEternal - enter and foundCrash=" + foundCrashEternalThread + ", threadID=" + Thread.currentThread().getId());
        BufferedReader br = null;
        String line = null;

        br = createReader();
        if (br == null) {
            Log.w(TAG, "create logcat reader failed - launching startLogcatExceptionMonitorThread()");
            return ITA_Constants.CREATE_LOG_READER_ERROR;
        }

        BufferedWriter bw = null;

        StringBuilder sb = new StringBuilder();

        String action = null;
        String packageName = null;
        String componentName = null;

        while (keepGoingLogging) {
            try {
                if (br == null) {
                    br = createReader();
                    if (br == null) {
                        Log.w(TAG, "create logcat reader failed - launching startLogcatExceptionMonitorThread()");
                        return ITA_Constants.CREATE_LOG_READER_ERROR;
                    }
                }

                line = br.readLine();
                if (line == null)
                    return ITA_Constants.LOG_READER_END_OF_STREAM;

                Matcher startApplicationComponentSentIntentMatcher = startApplicationComponentSentIntentPattern.matcher(line);
                if (startApplicationComponentSentIntentMatcher.matches()) {

                    if (bw != null) {
                        try {
                            bw.write(sb.toString());
                            bw.flush();
                            bw.close();
                            sb.setLength(0);
                        } catch (IOException e) {
                        }
                    }

                    String component = startApplicationComponentSentIntentMatcher.group(7);
                    packageName = startApplicationComponentSentIntentMatcher.group(8);
                    componentName = startApplicationComponentSentIntentMatcher.group(9);
                    String formattedID = String.format("%06d", currentID++);
                    bw = new BufferedWriter(new FileWriter(currentLogDir + "/" + component + "/" + formattedID + "|" + packageName + "|" + componentName + ".txt"));
                    sb.append(line + ITA_IntentService.newLine);
                    Log.w(TAG, "Logging " + packageName + "/" + componentName);
                    writeIntSharedPreferences(getApplicationContext(), ITA_Constants.GLOBAL_SEQUENTIAL_ID, currentID);
                    continue;
                }

                Matcher sendDynamicBroadcastActionIntentMatcher = sendDynamicBroadcastActionIntentPattern.matcher(line);
                if (sendDynamicBroadcastActionIntentMatcher.matches()) {
                    if (bw != null) {
                        try {
                            bw.write(sb.toString());
                            bw.flush();
                            bw.close();
                            sb.setLength(0);
                        } catch (IOException e) {
                        }
                    }

                    String formattedID = String.format("%06d", currentID++);
                    packageName = sendDynamicBroadcastActionIntentMatcher.group(7);
                    componentName = sendDynamicBroadcastActionIntentMatcher.group(8);
                    String componentType = sendDynamicBroadcastActionIntentMatcher.group(9);
                    action = sendDynamicBroadcastActionIntentMatcher.group(10);
                    bw = new BufferedWriter(new FileWriter(currentLogDir + "/" + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + "/" + formattedID + "|" + componentType + "|" + packageName + "|" + componentName + "|" + action + ".txt"));
                    sb.append(line + ITA_IntentService.newLine);
                    Log.w(TAG, "Logging " + action + "-" + packageName + "/" + componentName);
                    writeIntSharedPreferences(getApplicationContext(), ITA_Constants.GLOBAL_SEQUENTIAL_ID, currentID);
                    continue;
                }
                
                Matcher sendBroadcastActionIntentPatternMatcher = sendBroadcastActionIntentPattern.matcher(line);
                if (sendBroadcastActionIntentPatternMatcher.matches()) {
                    if (bw != null) {
                        try {
                            bw.write(sb.toString());
                            bw.flush();
                            bw.close();
                            sb.setLength(0);
                        } catch (IOException e) {
                        }
                    }

                    String formattedID = String.format("%06d", currentID++);
                    action = sendBroadcastActionIntentPatternMatcher.group(7);
                    bw = new BufferedWriter(new FileWriter(currentLogDir + "/" + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + "/" + formattedID + "|" + action + ".txt"));
                    sb.append(line + ITA_IntentService.newLine);
                    Log.w(TAG, "Logging " + action);
                    writeIntSharedPreferences(getApplicationContext(), ITA_Constants.GLOBAL_SEQUENTIAL_ID, currentID);
                    continue;
                }

                Matcher fatalExceptionSystemMatcher = fatalExceptionSystemPattern.matcher(line);
                if (fatalExceptionSystemMatcher.matches()) {
                    foundCrashEternalThread = true;
                    foundSystemCrashEternalThread = true;

                    Log.w(TAG, "parseLogcatForCrashEternal - found fatal system exception - " + line + ", threadID=" + Thread.currentThread().getId());
                    if (bw != null)
                        sb.append(line + ITA_IntentService.newLine);
                    continue;
                }

                Matcher fatalExceptionMatcher = fatalExceptionPattern.matcher(line);
                if (fatalExceptionMatcher.matches()) {
                    foundCrashEternalThread = true;
                    Log.w(TAG, "parseLogcatForCrashEternal - found fatal exception - " + line + ", threadID=" + Thread.currentThread().getId());
                    if (bw != null)
                        sb.append(line + ITA_IntentService.newLine);
                    continue;
                }

                Matcher nativeCrashMatcher = nativeCrashPattern.matcher(line);
                if (nativeCrashMatcher.matches()) {
                    foundCrashEternalThread = true;
                    Log.w(TAG, "parseLogcatForCrashEternal - found fatal native exception - " + line + ", threadID=" + Thread.currentThread().getId());
                    if (bw != null)
                        sb.append(line + ITA_IntentService.newLine);
                    continue;
                }

                Matcher finishedSendingInitialIntentsMatcher = finishedSendingInitalRoundOfIntentsPattern.matcher(line);
                if (finishedSendingInitialIntentsMatcher.matches()) {
                    if (bw != null) {
                        try {
                            bw.write(sb.toString());
                            bw.flush();
                            bw.close();
                            sb.setLength(0);
                        } catch (IOException e) {
                        }
                    }
                    continue;
                }
                if (bw != null)
                    sb.append(line + ITA_IntentService.newLine);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (bw != null) {
            try {
                bw.flush();
                bw.close();
            } catch (IOException e) {
            }
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
            }
        }
        Log.w(TAG, "parseLogcatForCrashEternal - exit and foundCrash=" + foundCrash + ", threadID=" + Thread.currentThread().getId());
        return ITA_Constants.LOG_READER_NORMAL_EXIT;
    }


    private void writeInstalledAppsToDB() {

        Log.w(TAG, "writeInstalledAppsToDB()");

        if (ITA_IntentService.getAppMD5 == false) {
            Log.w(TAG, "skipping app md5s");
            return;
        }

        final PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        ContentResolver cr = getContentResolver();
        PackageManager pm = getPackageManager();
        PackageInfo pi = null;
        String apkPath = "";
        String apkMD5 = "";
        String permissions = "";

        String targetAPK = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        boolean onlySystemServer = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
        if (onlySystemServer == true)
            targetAPK = "android";

        for (ApplicationInfo appInfo : installedApplications) {

            if (keepGoing == false) {
                Log.w(TAG, "break out of getting md5 of apks");
                break;
            }


            int uid = appInfo.uid;
            String processName = appInfo.processName;
            String appType = ITA_Constants.THIRD_PARTY_APP;
            String packageName = appInfo.packageName;

            // ignore our own app
            if (packageName.equals(getPackageName())) {
                continue;
            }

            // if there is a target and the current app is not it, skip
            if (targetAPK != null && !targetAPK.equals("")) {
                if (!targetAPK.equals(packageName)) {
                    continue;
                }
            }

            apkPath = appInfo.sourceDir;
            apkMD5 = calculateMD5(new File(apkPath));
            Log.w(TAG, "apkPath=" + apkPath + ", apkMD5=" + apkMD5);

            String versionName = "";
            int versionCode = -1;

            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                appType = ITA_Constants.SYSTEM_APP;
            } else if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                appType = ITA_Constants.UPDATED_SYSTEM_APP;
            }

            try {
                pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                versionName = pi.versionName;
                versionCode = pi.versionCode;
                String[] reqPerms = pi.requestedPermissions;
                if (reqPerms != null && reqPerms.length != 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int h = 0; h < reqPerms.length; h++) {
                        if (h != (reqPerms.length - 1)) {
                            sb.append(reqPerms[h] + "|");
                        } else {
                            sb.append(reqPerms[h]);
                        }
                    }
                    permissions = sb.toString();
                }
            } catch (Exception e) {}

            if (apkMD5 == null)
                continue;

            ContentValues cv = new ContentValues();
            cv.put(ITA_Contract.PACKAGE_NAME, packageName);
            cv.put(ITA_Contract.PROCESS_NAME, processName);
            cv.put(ITA_Contract.UID, uid);
            cv.put(ITA_Contract.VERSION_NAME, versionName);
            cv.put(ITA_Contract.VERSION_CODE, versionCode);
            cv.put(ITA_Contract.APP_TYPE, appType);
            cv.put(ITA_Contract.PERMISSIONS, permissions);
            cv.put(ITA_Contract.APK_MD5, apkMD5);
            Uri dest = ITA_Contract.AppsTable.CONTENT_URI;
            cr.insert(dest, cv);
        }
    }

    private void recordStartTimeinDB() {
        ContentResolver cr = getContentResolver();
        ContentValues cv = new ContentValues();
        long millisTimestamp = System.currentTimeMillis();
        cv.put(ITA_Contract.START_TIME, String.valueOf(millisTimestamp));
        Uri dest = ITA_Contract.TimeTable.CONTENT_URI;
        cr.insert(dest, cv);
    }

    private void recordStopTimeinDB() {
        ContentResolver cr = getContentResolver();
        Uri dest = ITA_Contract.TimeTable.CONTENT_URI;
        Cursor cursor = cr.query(dest, null, null, null, null);
        String id = "1";
        String startTime = null;
        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst == false)
                Log.w(TAG, "[" + ITA_Contract.TimeTable.TIME_TABLE_NAME + "] no entries for this table");
            else {
                id = cursor.getString(cursor.getColumnIndex(ITA_Contract._ID));
                startTime = cursor.getString(cursor.getColumnIndex(ITA_Contract.START_TIME));
            }
            cursor.close();
        }
        ContentValues cv = new ContentValues();
        long millisTimestamp = System.currentTimeMillis();
        String timestamp = String.valueOf(millisTimestamp);
        if (startTime != null) {
            long totalTime = millisTimestamp - (long) Long.valueOf(startTime);
            cv.put(ITA_Contract.TOTAL_TIME, totalTime);
        }
        cv.put(ITA_Contract.STOP_TIME, timestamp);
        cr.update(dest, cv, ITA_Contract._ID + " = ?", new String[]{id});
    }

    private void startAnalysis() {

        clearLog();

        // flag for the analysis thread
        keepGoing = true;

        // let this be populated
        currentPackageName = null;

        isFirstComponentOfAnAppDynBR = false;

        // record that the analysis is starting
        writeBooleanSharedPreferences(this.getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS, true);

        // ensure there are reset
        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, -1);

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT, "");

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY, "");

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_TABLE, "");

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TROUBLESOME_COMPONENT_NAME, null);

        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.NUM_TIMES_APP_KILLED_ON_TROUBLESOME_COMPONENT, 0);

        writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_COMPONENT_NAME, null);

        writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_ACTION, null);

        writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP, null);

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_PACKAGE_NAME_DYNAMIC_RECEIVER_TEST, null);

        // write a 1 for the current ID
        currentID = 1;
        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.GLOBAL_SEQUENTIAL_ID, currentID);

        // show that the analysis is active with a notification
        createNotification("Performing Analysis");

        // clear out the previous analysis if it exists
        deleteAllRowsFromTables();

        // record the start time
        recordStartTimeinDB();

        // record the installed apps
        writeInstalledAppsToDB();

        Log.w(TAG, "startingAnalysis and keepGoing=" + keepGoing);

        // set up the directories for analysis
        init();

        currentPackageNameDynamicBroadcastReceiver = null;
        baselinePackageDynamicBroadcastReceivers = new HashSet<String>();
        testedDynamicBroadcastReceiversForPackage = new HashSet<String>();

        keepGoingLogging = true;

        startLogcatExceptionMonitorThread();

        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_LOG_START);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // record the start time
        recordTimeFile(ITA_Constants.START_INTENT_SENDING_TIMESTAMP);

        // obtain the exposed provider interfaces and write them to the database
        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_PROVIDER)) {
            Log.w(TAG, ITA_Constants.TEST_PROVIDER + "=true");
            writeContentProvidersToDB();
        }

        // obtain the exposed activity interfaces and write them to the database
        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_ACTIVITY)) {
            Log.w(TAG, ITA_Constants.TEST_ACTIVITY + "=true");
            writeActivitiesToDB();
        }

        // obtain the exposed service interfaces and write them to the database
        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_SERVICE)) {
            Log.w(TAG, ITA_Constants.TEST_SERVICE + "=true");
            writeServicesToDB();
        }

        // obtain the exposed receiver interfaces and write them to the database
        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_RECEIVER)) {
            Log.w(TAG, ITA_Constants.TEST_RECEIVER + "=true");
            writeReceiversToDB();
        }

        // obtain the broadcast actions for receiver interfaces and write them to the database
        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_BROADCASTACTION)) {
            Log.w(TAG, ITA_Constants.TEST_BROADCASTACTION + "=true");
            obtainActiveBroadcastReceiverActions();
        }

        returnToHomeScreen();

        finishMainActivity();

        // send the intents
        sendAllIntents();

        // return if the user hit stop analysis
        if (keepGoing == false && keepGoingLogging == false) {
            Log.d(TAG, "user has hit stop analysis. do not analyze the logs");
            return;
        }

        logFinishInitialSendingOfIntents();

        // show that the analysis is active with a notification
        createNotification("Retesting Crashes");

        // indicate that the analysis is finished
        writeBooleanSharedPreferences(this.getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS, false);

        // record the end time
        recordTimeFile(ITA_Constants.FINISH_INTENT_SENDING_TIMESTAMP);

        // make the analysis thread exit
        keepGoing = false;

        // reset the last intent sent data
        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, -1);

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT, "");

        launchAnalyzeLogsThread();

    }
    
    void analyzeContentProviders() {
        writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME);
        Log.w(TAG, "analyzing the content providers");
        startProviderApplicationComponent(ITA_Contract.ProviderTable.CONTENT_URI, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME);
        Log.w(TAG, "finished analyzing the content providers");
    }
    
    void launchStartAnalysisThread() {

        Log.w(TAG, "launchStartAnalysisThread()");

        boolean isAnalyzingLogs = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ANALYZING_LOGS);
        if (isAnalyzingLogs) {
            Log.w(TAG, "Analyzing logs currently. Don't launch new thread for Intent sending!");
            sendToastToMainActivity("Analyzing Logs was interrupted or crashed. Press the \"Analyze Logs\" to conintue analyzing the logs or \"Stop Analysis\" and then start a new analysis");
            return;
        }

        if (analysisThread == null || !analysisThread.isAlive()) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ITA_IntentService.this.startAnalysis();
                    analysisThread = null;
                }
            };
            analysisThread = new Thread(r);
            analysisThread.start();
        } else {
            Log.w(TAG, "analysis thread already active. not lauching a new one");
        }
    }

    // will send all intents and record the log for each
    void sendAllIntents() {

        Log.w(TAG, "sending all intents");

        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_PROVIDER) && keepGoing) {
            // record the table we are currently sending
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME);
            Log.w(TAG, "sending provider requests");
            analyzeContentProviders();
            Log.w(TAG, "finished sending provider requests");
        }

        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_ACTIVITY) && keepGoing) {
            // record the table we are currently sending
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME);
            Log.w(TAG, "sending activity intents");
            // send all the intents
            startApplicationComponent(ITA_Contract.ActivityTable.CONTENT_URI, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, false);
            Log.w(TAG, "finished sending activity intents");
        }

        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_BROADCASTACTION) && keepGoing) {
            // record the table we are currently sending
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME);
            Log.w(TAG, "sending broadcast action intents");
            // send all the intents
            startApplicationComponent(ITA_Contract.BroadcastActionTable.CONTENT_URI, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, true);
            Log.w(TAG, "finished sending broadcast action intents");
        }

        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_RECEIVER) && keepGoing) {
            // record the table we are currently sending
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME);
            Log.w(TAG, "sending receiver intents");
            // send all the intents
            startApplicationComponent(ITA_Contract.ReceiverTable.CONTENT_URI, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, false);
            Log.w(TAG, "finished sending receiver intents");
        }

        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_SERVICE) && keepGoing) {
            // record the table we are currently sending
            writeStringSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_TABLE, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME);
            Log.w(TAG, "sending service intents");
            // send all the intents
            startApplicationComponent(ITA_Contract.ServiceTable.CONTENT_URI, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, false);
            Log.w(TAG, "finished sending service intents");
        }

        Log.w(TAG, "finished sending all intents");
    }

    // needed so the last file gets written out
    public void logFinishInitialSendingOfIntents() {
        Log.e(TAG, ITA_Constants.FINISHED_INITIAL_SENDING_OF_INTENTS);
    }

    public void analyzeLogs() {

        // start the log reading thread in case it has not been started in case or reboot during log analysis
        keepGoingLogging = true;

        startLogcatExceptionMonitorThread();

        Log.w(TAG, "start analyzing logs");

        recordTimeFile(ITA_Constants.START_ANALYZING_LOGS_TIMESTAMP);

        currentPackageNameDynamicBroadcastReceiver = null;
        baselinePackageDynamicBroadcastReceivers = new HashSet<String>();
        testedDynamicBroadcastReceiversForPackage = new HashSet<String>();
        currentPackageName = null;

        writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP, null);
        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_PACKAGE_NAME_DYNAMIC_RECEIVER_TEST, null);

        HashSet<String> killerComponents = readHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_COMPONENT_NAME);
        HashSet<String> killerActions = readHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_ACTION);

        boolean isResumiungAfterReboot = false;
        boolean resumePlaceFound = false;

        String currentLogFile = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_FILE);

        if (currentLogFile == null || currentLogFile.equals("")) {

            Log.w(TAG, "Starting fresh with analyzing logs (not a resumption)");

            // delete the results from previous run(s)
            deleteAllRowsFromSingleTable(ITA_Contract.ResultsTable.CONTENT_URI);
        } else {
            Log.w(TAG, "Resuming Log Analysis and file to be examined is - " + currentLogFile);
            isResumiungAfterReboot = true;
        }

        createNotification("Started Analzying Logs");

        // determine if the service is sending broadcasts
        boolean isSendingIntents = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS);

        // stop sending intents if it is actively doing so
        if (isSendingIntents) {

            // will stop the analysis thread
            keepGoing = false;
            writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS, false);
        }

        String logDirStr = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_DIR);

        // make sure we have a legit directory
        if (logDirStr == null || logDirStr.isEmpty()) {
            return;
        }

        // make sure we have a legit directory
        File logDir = new File(logDirStr);
        if (!logDir.exists()) {
            return;
        }

        // indicate that we are analyzing the logs
        writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ANALYZING_LOGS, true);

        // clear this hash set
        CRASHED_COMPONENTS.clear();

        String curDirStr = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR);

        File resultsDir = new File(curDirStr + "/" + ITA_Constants.LOG_RESULTS);

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_RESULTS_DIR, resultsDir.getAbsolutePath());

        if (!resultsDir.exists())
            resultsDir.mkdir();

        ArrayList<String> systemCrashes = checkSystemFailureFile();
        HashMap<String, String> stackTraces = new HashMap<String, String>();

        // the directories in the log directory
        File[] logDirs = logDir.listFiles();

        // sort the log directories alphabetically
        Arrays.sort(logDirs);

        // iterate through the directories where each contains log files
        for (int a = 0; a < logDirs.length; a++) {
            File aLogDir = logDirs[a]; // specific log directory
            String shortDirName = aLogDir.getName();
            File[] logFiles = aLogDir.listFiles(); // list of all log files
            Arrays.sort(logFiles);
            Log.e(TAG, "Processing log directory - " + aLogDir.getAbsolutePath());
            for (int b = 0; b < logFiles.length && keepGoingLogging == true; b++) {
                File aLogFile = logFiles[b];
                String fileName = aLogFile.getAbsolutePath();
                if (isResumiungAfterReboot) {
                    if (resumePlaceFound == false) {
                        if (currentLogFile.equals(fileName)) {
                            resumePlaceFound = true;
                            Log.w(TAG, "Found the current log file to analyze - " + fileName);
                        } else {
                            Log.w(TAG, "Already analyzed [skipping] - " + fileName);
                            continue;
                        }
                    }
                }

                writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_FILE, fileName);

                analyzeLogFile(aLogFile, resultsDir, shortDirName, systemCrashes, stackTraces, killerComponents, killerActions);
            }
        }

        // write any system crashes where the actual log was unavailable
        writeRemainingSystemCrashesToDatabase(systemCrashes);

        removeProcessCrashesFromIntentsThatCausedSystemCrash();

        createNotification("Finished Analyzing Logs");

        sendToastToMainActivity("Finished Analyzing Logs");

        Log.e(TAG, "finished analyzing logs");

        recordStopTimeinDB();
        String dbpath = copyDatabaseToSdcard();
        String jsonPath = copyJSONresultsToSdcard();

        String targettedApp = ITA_IntentService.readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);
        if (targettedApp != null && !targettedApp.isEmpty()) {
            writeArbitraryStringToSDCard(ITA_Constants.outputDir + "/" + targettedApp + ".txt", dbpath);
            Log.w(ITA_Constants.FINISH_ANALYZING_SINGLE_APP_WITH_PATH_TAG, "|" + targettedApp + "|" + dbpath + "|");
        } else {
            Log.w(ITA_Constants.FINISH_ANALYZING_SINGLE_APP_WITH_PATH_TAG, "|allapps|" + dbpath + "|");
        }

        // zero out the targeted app if it exists
        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP, null);

        // indicate that analyzing logs has finished
        writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ANALYZING_LOGS, false);

        recordTimeFile(ITA_Constants.FINISH_ANALYZING_LOGS_TIMESTAMP);

        // stop the logging thread
        keepGoingLogging = false;

        // zero out the variables for reading the logs if a soft reboot happens during log analysis
        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_FILE, "");

        Intent i = new Intent(this.getApplicationContext(), ITA_MainAct.class);
        i.setAction(ITA_Constants.FINISHED_ANALYZING_LOGS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // if the system crashes, other processes will crash, we will remove them since they
    // will show up in the process crashes and they are not that relevant since they are
    // indirectly caused by the system crash
    public void removeProcessCrashesFromIntentsThatCausedSystemCrash() {
        ArrayList<String> systemCrashes = ITA_ExceptionTypeSelection.getSystemCrashes(this.getContentResolver());
        if (systemCrashes == null)
            return;
        ContentResolver cr = getContentResolver();
        for (String systemCrash : systemCrashes) {
            if (systemCrash.contains("/")) {
                String[] parts = systemCrash.split("/");
                String packageName = parts[0];
                String componentName = parts[1];
                String where = ITA_Contract.INSTANCE_TYPE + " = ? AND " + ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
                String[] selectionArgs = {ITA_Constants.PROCESS_CRASH, packageName, componentName};
                cr.delete(ITA_Contract.ResultsTable.CONTENT_URI, where, selectionArgs);
            } else {
                String where = ITA_Contract.INSTANCE_TYPE + " = ? AND " + ITA_Contract.ACTION + " = ?";
                String[] selectionArgs = {ITA_Constants.PROCESS_CRASH, systemCrash};
                cr.delete(ITA_Contract.ResultsTable.CONTENT_URI, where, selectionArgs);
            }
        }
    }

    public void writeRemainingSystemCrashesToDatabase(ArrayList<String> lines) {
        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length == 2) {
                this.writeSystemCrashToDatabase(parts[1], "", "", parts[0]);
            } else {
                this.writeSystemCrashToDatabase("", parts[1], parts[2], parts[0]);
            }
        }
    }

    // this method will examine the Intents that cause a system failure and mark
    // them as such
    public ArrayList<String> checkSystemFailureFile() {
        return readFileIntoArrayList(new File(currentDir + "/" + ITA_Constants.SYSTEM_FAILURE_INTENTS_FILE));
    }

    void writeSystemCrashToDatabase(String action, String packageName, String componentName, String componentType) {
        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
        cv.put(ITA_Contract.ACTION, action);
        cv.put(ITA_Contract.LOG_PATH, "none");
        cv.put(ITA_Contract.PID, -1);
        cv.put(ITA_Contract.INSTANCE_TYPE, ITA_Constants.SYSTEM_CRASH);
        cv.put(ITA_Contract.LOG_EVENT_MESSAGES, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.PROCESS_NAME, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.EXCEPTION_NAME, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.EXCEPTION_REASON, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
        cv.put(ITA_Contract.VERSION_NAME, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.VERSION_CODE, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.APK_MD5, getMD5fromPackageName(packageName));
        Uri dest = ITA_Contract.ResultsTable.CONTENT_URI;
        getContentResolver().insert(dest, cv);
    }

    void enterSkippedProviderIntoResults(String packageName, String componentName, String dirName, File logFile) {
        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
        cv.put(ITA_Contract.LOG_PATH, logFile.getAbsolutePath());
        cv.put(ITA_Contract.PID, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.INSTANCE_TYPE, ITA_Constants.PROCESS_CRASH);
        cv.put(ITA_Contract.LOG_EVENT_MESSAGES, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.PROCESS_NAME, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.EXCEPTION_NAME, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.EXCEPTION_REASON, ITA_Constants.UNAVAILABLE);
        cv.put(ITA_Contract.COMPONENT_TYPE, dirName);
        cv.put(ITA_Contract.DYNAMIC_RECEIVER_CRASH, ITA_Constants.IS_NOT_DYNAMIC_RECEIVER_CRASH);
        cv.put(ITA_Contract.BASE_COMPONENT_TYPE, ITA_Constants.UNAVAILABLE);
        String[] data = getAppVersionDatafromPackageName(packageName);
        if (data == null) {
            cv.put(ITA_Contract.VERSION_NAME, "");
            cv.put(ITA_Contract.VERSION_CODE, "");
            cv.put(ITA_Contract.APK_MD5, "");
        } else {
            cv.put(ITA_Contract.VERSION_NAME, data[2]);
            cv.put(ITA_Contract.VERSION_CODE, data[1]);
            cv.put(ITA_Contract.APK_MD5, data[0]);
        }
        Uri dest = ITA_Contract.ResultsTable.CONTENT_URI;
        getContentResolver().insert(dest, cv);

        CRASHED_COMPONENTS.add(packageName + "/" + componentName);
    }

    // method to use regex to determine the effects of a broadcast intent and write the results
    // to a file in the resultsDir
    void analyzeLogFile(File logFile, File resultsDir, String dirName, ArrayList<String> systemCrashes, HashMap<String, String> stackTraces, HashSet<String> killerComponents, HashSet<String> killerActions) {

        ArrayList<String> fileLines = readFileIntoArrayList(logFile);

        String shortFileName = logFile.getName();

        Log.e(TAG, "Parsing log file - " + logFile.toString());

        String[] parts = shortFileName.split("\\|");
        String packageName = "";
        String componentName = "";
        String action = "";
        String baseComponentType = "";
        boolean isMultiStage = false;
        boolean skippedProvider = false;

        if (dirName.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
            if (parts.length == 2) {
                action = parts[1].substring(0, parts[1].length() - 4);
            } else if (parts.length == 5) {
                baseComponentType = parts[1];
                packageName = parts[2];
                componentName = parts[3];
                action = parts[4].substring(0, parts[4].length() - 4);
                isMultiStage = true;
            }
            if (killerActions.contains(action)) {
                Log.w(TAG, "[" + action + "] skipping broadcast action since it killed our app " + ITA_Constants.NUM_TIMES_KILLED_SKIP_THRESHOLD + " times!");
                return;
            }
        } else {
            packageName = parts[1];
            componentName = parts[2].substring(0, parts[2].length() - 4);
            if (killerComponents.contains(packageName + "/" + componentName)) {
                if (dirName.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME)) {
                    skippedProvider = true;
                } else {
                    Log.w(TAG, "[" + packageName + "/" + componentName + "] skipping component since it killed our app " + ITA_Constants.NUM_TIMES_KILLED_SKIP_THRESHOLD + " times!");
                    return;
                }
            }
        }

        // check to ensure that a second file was not created for a particular component if we only want one
        // crash for a component
        if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH)) {
            if (action.isEmpty()) {
                if (CRASHED_COMPONENTS.contains(packageName + "/" + componentName)) {
                    Log.w(TAG, "skipping file that already has a crash - " + packageName + "/" + componentName + ", filenane=" + shortFileName);
                    return;
                }
            } else {
                if (isMultiStage) {
                    if (CRASHED_COMPONENTS.contains(packageName + "/" + componentName + "/" + action)) {
                        Log.w(TAG, "skipping file that already has a crash - " + packageName + "/" + componentName + ", action=" + action + ", filenane=" + shortFileName);
                        return;
                    }
                } else {
                    if (CRASHED_COMPONENTS.contains(action)) {
                        Log.w(TAG, "skipping file that already has a crash - " + action + ", filenane=" + shortFileName);
                        return;
                    }
                }
            }
        }

        if (skippedProvider == true)
            enterSkippedProviderIntoResults(packageName, componentName, dirName, logFile);

        ArrayList<JSONObject> logInstances = new ArrayList<JSONObject>();

        boolean properLogIntentSentMessageFound = false;

        ListIterator<String> iter = fileLines.listIterator();
        while (iter.hasNext()) {
            String line = iter.next();

            if (action.equals("")) {
                Matcher startActivitySentIntentMatcher = startApplicationComponentSentIntentPattern.matcher(line);

                if (startActivitySentIntentMatcher.matches()) {

                    String logPackageName = startActivitySentIntentMatcher.group(8);
                    String logComponentName = startActivitySentIntentMatcher.group(9);

                    if (packageName.equals(logPackageName) && componentName.equals(logComponentName)) {
                        properLogIntentSentMessageFound = true;
                        continue;
                    } else if ((!packageName.equals(logPackageName) || !componentName.equals(logComponentName)) && properLogIntentSentMessageFound) {
                        Log.w(TAG, "encountered the sending of the next intent in the log file - " + logFile.getAbsolutePath());
                        break;
                    }
                }
            } else {

                Matcher sendBroadcastActionIntentMatcher = sendBroadcastActionIntentPattern.matcher(line);
                if (sendBroadcastActionIntentMatcher.matches()) {
                    String logAction = sendBroadcastActionIntentMatcher.group(7);

                    if (action.equals(logAction)) {
                        properLogIntentSentMessageFound = true;
                        continue;
                    } else if (!action.equals(logAction) && properLogIntentSentMessageFound) {
                        Log.w(TAG, "encountered the sending of the next intent in the log file - " + logFile.getAbsolutePath());
                        break;
                    }
                }
            }

            Matcher fatalExceptionSystemMatcher = fatalExceptionSystemPattern.matcher(line);
            if (fatalExceptionSystemMatcher.matches()) {
                JSONObject instance = parseOutLogInstance(fatalExceptionSystemMatcher, iter, line, action, logFile, ITA_Constants.SYSTEM_CRASH, packageName, componentName, dirName, systemCrashes, stackTraces, baseComponentType, skippedProvider);
                if (instance != null) {
                    logInstances.add(instance);
                    if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH)) {
                        if (isMultiStage) {
                            CRASHED_COMPONENTS.add(packageName + "/" + componentName + "/" + action);
                        } else if (action.isEmpty())
                            CRASHED_COMPONENTS.add(packageName + "/" + componentName);
                        else
                            CRASHED_COMPONENTS.add(action);
                        break;
                    }
                }
                break;
            }

            Matcher fatalExceptionMatcher = fatalExceptionPattern.matcher(line);
            if (fatalExceptionMatcher.matches()) {
                JSONObject instance = parseOutLogInstance(fatalExceptionMatcher, iter, line, action, logFile, ITA_Constants.PROCESS_CRASH, packageName, componentName, dirName, systemCrashes, stackTraces, baseComponentType, skippedProvider);
                if (instance != null) {
                    logInstances.add(instance);
                    if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH)) {
                        if (isMultiStage) {
                            CRASHED_COMPONENTS.add(packageName + "/" + componentName + "/" + action);
                        } else if (action.isEmpty())
                            CRASHED_COMPONENTS.add(packageName + "/" + componentName);
                        else
                            CRASHED_COMPONENTS.add(action);
                        break;
                    }
                }
                break;
            }

            Matcher nativeCrashMatcher = nativeCrashPattern.matcher(line);
            if (nativeCrashMatcher.matches()) {
                JSONObject instance = parseOutLogInstance(nativeCrashMatcher, iter, line, action, logFile, ITA_Constants.NATIVE_CRASH, packageName, componentName, dirName, systemCrashes, stackTraces, baseComponentType, skippedProvider);
                if (instance != null) {
                    logInstances.add(instance);
                    if (readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH)) {
                        if (isMultiStage) {
                            CRASHED_COMPONENTS.add(packageName + "/" + componentName + "/" + action);
                        } else if (action.isEmpty())
                            CRASHED_COMPONENTS.add(packageName + "/" + componentName);
                        else
                            CRASHED_COMPONENTS.add(action);
                        break;
                    }
                }
                break;
            }
        }

        if (logInstances.size() != 0) {
            String filePath = resultsDir.getAbsolutePath() + "/" + shortFileName;
            writeJSONObjectArrayListToFile(filePath, logInstances);
        }
    }

    JSONObject parseOutLogInstance(Matcher matcher, ListIterator<String> iter, String line, String action, File logFile, String type, String packageName, String componentName, String componentType, ArrayList<String> systemCrashes, HashMap<String, String> stackTraces, String baseComponentType, boolean isSkippedProvider) {
        String dateAndMonth = matcher.group(1);
        String time = matcher.group(2);
        String pid = matcher.group(3);
        String basePID = pid;
        String ppid = matcher.group(4);
        String logLevel = matcher.group(5);
        String baseLogTag = matcher.group(6);
        String logTag = "";
        String logMessage = matcher.group(7);
        String exceptionName = "";
        String exceptionReason = "";
        String processName = "";

        if (type.equals(ITA_Constants.NATIVE_CRASH)) {
            processName = matcher.group(11);
        } else {
            logMessage = matcher.group(7);
        }

        JSONArray logLines = new JSONArray();
        logLines.put(baseLogTag + ": " + logMessage);

        Matcher fatalExceptionSystemMatcher = fatalExceptionSystemPattern.matcher(line);

        // get the process name for system crashes
        if (fatalExceptionSystemMatcher.matches())
            processName = fatalExceptionSystemMatcher.group(8);

        boolean keepIterating = true;
        while (iter.hasNext() && keepIterating) {

            String nextLine = iter.next(); // 02-26 16:10:29.568  3501  3501 E AndroidRuntime: java.lang.RuntimeException: Error receiving broadcast Intent { act=com.samsung.android.net.wifi.NETWORK_OXYGEN_STATE_CHANGE flg=0x10 bqHint=4 } in com.android.server.wifi.WifiTrafficPoller$1@f99309e
            // 09-03 23:16:23.682 30706 30706 E AndroidRuntime: android.util.SuperNotCalledException: Activity {com.google.android.gms/com.google.android.gms.wallet.common.ui.UpdateAddressActivity} did not call through to super.onCreate()
            Matcher logLineMatcher = logLinePattern.matcher(nextLine);

            if (logLineMatcher.matches()) {
                pid = logLineMatcher.group(3);
                ppid = logLineMatcher.group(4);
                logLevel = logLineMatcher.group(5);
                logTag = logLineMatcher.group(6);
                logMessage = logLineMatcher.group(7);

                if (pid.equals(basePID) && baseLogTag.equals(logTag)) {
                    logLines.put(logTag + ": " + logMessage); // 06-24 21:28:06.298 13729 13729 E AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String java.lang.String.replace(java.lang.CharSequence, java.lang.CharSequence)' on a null object reference
                    Matcher exceptionCauseMatcher = exceptionCausePattern.matcher(nextLine);    // 06-24 21:28:06.298 13729 13729 E AndroidRuntime: Process: com.google.android.youtube, PID: 13729
                    if (exceptionCauseMatcher.matches()) {
                        exceptionName = exceptionCauseMatcher.group(7);
                        exceptionReason = exceptionCauseMatcher.group(8);
                        continue;
                    }

                    Matcher exceptionCauseMatcher2 = exceptionCause2Pattern.matcher(nextLine); // 06-24 21:40:08.824 16691 16691 E AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String java.lang.String.replace(java.lang.CharSequence, java.lang.CharSequence)' on a null object reference
                    if (exceptionCauseMatcher2.matches()) {
                        exceptionName = exceptionCauseMatcher2.group(7);

                        // sometimes the actual exception name is at the end
                        int semicolon = exceptionName.lastIndexOf(':');
                        if (semicolon != -1) {
                            try {
                                exceptionName = exceptionName.substring(semicolon + 2);
                            } catch (IndexOutOfBoundsException e) {
                            }
                        }
                        exceptionReason = "Attempt to " + exceptionCauseMatcher2.group(8);
                        continue;
                    }

                    Matcher crashedProcessMatcher = crashedProcessPattern.matcher(nextLine);
                    if (crashedProcessMatcher.matches()) {
                        processName = crashedProcessMatcher.group(7);
                        pid = crashedProcessMatcher.group(8);
                        continue;
                    }
                    // E AndroidRuntime: java.lang.RuntimeException: Unable to instantiate activity ComponentInfo{com.android.cts.priv.ctsshim/com.android.cts.priv.ctsshim.InstallPriority}: java.lang.ClassNotFoundException: Didn't find class "com.android.cts.priv.ctsshim.InstallPriority" on path: DexPathList[[],nativeLibraryDirectories=[/system/priv-app/CtsShimPrivPrebuilt/lib/arm, /system/lib, /vendor/lib, /system/vendor/lib, /system/lib, /vendor/lib, /system/vendor/lib]]
                    Matcher exceptionCauseMatcher3 = exceptionCause3Pattern.matcher(nextLine); // 06-24 21:40:08.824 16691 16691 E AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String java.lang.String.replace(java.lang.CharSequence, java.lang.CharSequence)' on a null object reference
                    if (exceptionCauseMatcher3.matches()) {
                        String potentialExceptionName = exceptionCauseMatcher3.group(7);
                        if (potentialExceptionName.startsWith("at ") || potentialExceptionName.startsWith("Caused by")) {
                            continue;
                        }
                        if (potentialExceptionName.matches(".*\\..*")) {
                            exceptionName = potentialExceptionName;
                            exceptionReason = exceptionCauseMatcher3.group(8);
                        }
                        continue;
                    }

                    Matcher nativeCrashReasonMatcher = nativeCrashReasonPattern.matcher(nextLine);
                    if (nativeCrashReasonMatcher.matches()) {
                        exceptionReason = nativeCrashReasonMatcher.group(7);
                        exceptionName = nativeCrashReasonMatcher.group(8);
                        continue;
                    }

                    // 10-04 02:21:20.637 20193 20193 E AndroidRuntime: java.lang.NullPointerException
                    Matcher exceptionCauseMatcher4 = exceptionCause4Pattern.matcher(nextLine);
                    if (exceptionCauseMatcher4.matches() && exceptionName.equals("")) {
                        String potentialExceptionName = exceptionCauseMatcher4.group(7);
                        if (potentialExceptionName.startsWith("at ") || potentialExceptionName.startsWith("Caused by")) {
                            continue;
                        }
                        exceptionName = potentialExceptionName;
                        continue;
                    }
                } else {
                    iter.previous();
                    keepIterating = false;
                }
            }
        }

        // make sure the exception is in the target package
        if (!packageName.equals("")) {
            if (!processName.equals(packageName) && !processName.startsWith(packageName + ":")) {
                Log.w(TAG, "Dropping found exception in other process: current package=" + packageName + ", crashed process=" + processName);
                return null;
            }
        }

        PackageInfo pi = null;
        PackageManager pm = this.getPackageManager();
        String versionName = "";
        int versionCode = 0;

        try {
            if (!action.equals("")) {
                if (processName.contains(":")) {
                    int semiColonPlace = processName.indexOf(':');
                    if (semiColonPlace != -1) {
                        pi = pm.getPackageInfo(processName.substring(0, semiColonPlace), PackageManager.GET_META_DATA);
                    } else {
                        pi = pm.getPackageInfo(processName, PackageManager.GET_META_DATA);
                    }
                } else {
                    pi = pm.getPackageInfo(processName, PackageManager.GET_META_DATA);
                }
                versionName = pi.versionName;
                versionCode = pi.versionCode;
            } else if (!packageName.equals("")) {
                pi = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
                versionName = pi.versionName;
                versionCode = pi.versionCode;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        // if there are system crashes and we have the log for it then remove
        // it from the ArrayList
        if (systemCrashes != null && systemCrashes.size() != 0 && ITA_Constants.SYSTEM_CRASH.equals(type)) {
            if (componentType.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
                if (systemCrashes.contains(componentType + "|" + action))
                    systemCrashes.remove(componentType + "|" + action);
                else // it was not in the verified system failure reboot file which is tested twice
                    return null;
            } else {
                if (systemCrashes.contains(componentType + "|" + packageName + "|" + componentName))
                    systemCrashes.remove(componentType + "|" + packageName + "|" + componentName);
                else // it was not in the verified system failure reboot file which is tested twice
                    return null;
            }
        }

        StringBuilder sbNoPID = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        for (int z = 0; z < logLines.length(); z++) {
            String s;
            try {
                s = logLines.getString(z);
                if (z == logLines.length() - 1) {
                    sb.append(s);
                    sbNoPID.append(s);
                } else {

                    if (z == 1) {
                        int lastSemi = s.lastIndexOf(':');
                        if (lastSemi != -1) {
                            String s2 = s.substring(0, lastSemi);
                            sbNoPID.append(s2 + ITA_IntentService.newLine);
                        } else {
                            sbNoPID.append(s + ITA_IntentService.newLine);
                        }
                    } else {
                        // normalize the line so it does not matter whether is has a Bundle or not
                        // AndroidRuntime: java.lang.RuntimeException: Error receiving broadcast Intent { act=android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED flg=0x10 } in com.android.settingslib.bluetooth.BluetoothEventManager$1@89fa9
                        // AndroidRuntime: java.lang.RuntimeException: Error receiving broadcast Intent { act=android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED flg=0x10 (has extras) } in com.android.settingslib.bluetooth.BluetoothEventManager$1@89fa9
                        String newStr = s.replace(" (has extras)", "");

                        // remove object toString at the end cause it varies - example below
                        // AndroidRuntime: java.lang.RuntimeException: Error receiving broadcast Intent { act=android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED flg=0x10 } in com.android.settingslib.bluetooth.BluetoothEventManager$1@89fa9
                        if (newStr.matches(".*@.*")) {
                            int lastAt = newStr.lastIndexOf('@');
                            if (lastAt != -1) {
                                newStr = newStr.substring(0, lastAt);
                                sbNoPID.append(newStr + ITA_IntentService.newLine);
                            } else {
                                sbNoPID.append(newStr + ITA_IntentService.newLine);
                            }
                        } else {
                            sbNoPID.append(newStr + ITA_IntentService.newLine);
                        }
                    }
                    sb.append(s + ITA_IntentService.newLine);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        boolean preventRecurringFatalErrors = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PREVENT_RECURRING_FATAL_ERRORS);
        if (preventRecurringFatalErrors) {
            String stacktraceNoPID = sbNoPID.toString();
            boolean containsTrace = stackTraces.containsKey(stacktraceNoPID);
            if (containsTrace == true) {

                String value = stackTraces.get(stacktraceNoPID);

                // check to see if it is a broadcast intent
                if (!action.equals("")) {
                    Log.e(TAG, "dropping equivalent stacktrace where original Intent is " + value + " and dropped one is " + action + " and stacktrace is " + stacktraceNoPID);
                    return null;
                } else { // it is a component
                    // only allow a duplicate of a stacktrace for the same package name
                    // otherwise, return null for a different package so it does not get logged
                    String parts[] = value.split("\\|");
                    if (parts.length == 2) {
                        if (!parts[0].equals(packageName)) {
                            Log.e(TAG, "dropping equivalent stacktrace where original package name is " + parts[0] + "/" + parts[1] + " and dropped one is " + packageName + "/" + componentName + " and stacktrace is " + stacktraceNoPID);
                            return null;
                        } else { // they have the same package name
                            Log.e(TAG, "dropping equivalent stacktrace where original package name is " + parts[0] + "/" + parts[1] + " and dropped one is " + packageName + "/" + componentName + " and stacktrace is " + stacktraceNoPID);
                            return null;
                        }
                    } else {
                        Log.e(TAG, "dropping equivalent stacktrace where original Intent is " + value + " and dropped one is " + action + " and stacktrace is " + stacktraceNoPID);
                        return null;
                    }
                }
            } else { // it is not contained in the hashmap so add it
                // option to retest any crash that is not a system crash
                if (ITA_Constants.RETEST_ALL_INTENTS_WITH_A_CRASH && !ITA_Constants.SYSTEM_CRASH.equals(type)) {
                    boolean hasAction = false;
                    if (!action.equals("")) {
                        hasAction = true;
                    }
                    if (componentName.equals("com.android.internal.app.ChooserActivity")) {
                        int a = 0;
                        a++;
                    }
                    boolean result = retestAndVerifyIntent(packageName, componentName, action, hasAction, componentType, baseComponentType);
                    if (result == false) {

                        if (hasAction == true)
                            Log.e(TAG, "Retested action and DID NOT encounter crash = " + action);
                        else
                            Log.e(TAG, "Retested component and DID NOT encounter crash = " + packageName + "/" + componentName);
                        return null;
                    } else {
                        if (hasAction == true)
                            Log.e(TAG, "Retested action and DID encounter crash = " + action);
                        else
                            Log.e(TAG, "Retested component and DID encounter crash = " + packageName + "/" + componentName);
                    }
                    try {
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_RETEST_INTENT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (!action.equals("")) {
                    stackTraces.put(stacktraceNoPID, action);
                } else {
                    stackTraces.put(stacktraceNoPID, packageName + "|" + componentName);
                }
            }
        } else {
            // option to retest any crash that is not a system crash
            if (ITA_Constants.RETEST_ALL_INTENTS_WITH_A_CRASH && !ITA_Constants.SYSTEM_CRASH.equals(type)) {
                boolean hasAction = false;
                if (!action.equals(""))
                    hasAction = true;
                boolean result = retestAndVerifyIntent(packageName, componentName, action, hasAction, componentType, baseComponentType);
                if (result == false) {
                    if (hasAction == true) {
                        Log.e(TAG, "Retested action and DID NOT encounter crash = " + action);
                    } else {
                        Log.e(TAG, "Retested component and DID NOT encounter crash = " + packageName + "/" + componentName);
                    }
                    return null;
                } else {
                    if (hasAction == true) {
                        Log.e(TAG, "Retested action and DID encounter crash = " + action);
                    } else {
                        Log.e(TAG, "Retested component and DID encounter crash = " + packageName + "/" + componentName);
                    }
                }
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_RETEST_INTENT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        int dynamicBroadcastReceiverCrash = ITA_Constants.IS_NOT_DYNAMIC_RECEIVER_CRASH;
        if (crashDueToCrashInDynamicBroadcastReceiver) {
            crashDueToCrashInDynamicBroadcastReceiver = false;
            Log.w(TAG, "crash due to dynamic broadcast receiver");
            dynamicBroadcastReceiverCrash = ITA_Constants.IS_DYNAMIC_RECEIVER_CRASH;
        }

        JSONObject instance = new JSONObject();
        try {
            instance.put(ITA_Contract.ACTION, action);
            instance.put(ITA_Contract.LOG_PATH, logFile.getAbsolutePath());
            instance.put(ITA_Contract.INSTANCE_TYPE, type);
            instance.put(ITA_Contract.PID, basePID);
            instance.put(ITA_Contract.LOG_EVENT_MESSAGES, logLines);
            instance.put(ITA_Contract.PROCESS_NAME, processName);
            instance.put(ITA_Contract.EXCEPTION_NAME, exceptionName);
            instance.put(ITA_Contract.EXCEPTION_REASON, exceptionReason);
            instance.put(ITA_Contract.PACKAGE_NAME, packageName);
            instance.put(ITA_Contract.COMPONENT_NAME, componentName);
            instance.put(ITA_Contract.DYNAMIC_RECEIVER_CRASH, dynamicBroadcastReceiverCrash);
            instance.put(ITA_Contract.COMPONENT_TYPE, componentType);
            instance.put(ITA_Contract.VERSION_NAME, versionName);
            instance.put(ITA_Contract.VERSION_CODE, versionCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
        cv.put(ITA_Contract.ACTION, action);
        cv.put(ITA_Contract.LOG_PATH, logFile.getAbsolutePath());
        cv.put(ITA_Contract.PID, pid);
        cv.put(ITA_Contract.INSTANCE_TYPE, type);
        cv.put(ITA_Contract.LOG_EVENT_MESSAGES, sbNoPID.toString());
        cv.put(ITA_Contract.PROCESS_NAME, processName);
        cv.put(ITA_Contract.EXCEPTION_NAME, exceptionName);
        cv.put(ITA_Contract.EXCEPTION_REASON, exceptionReason);
        cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
        cv.put(ITA_Contract.DYNAMIC_RECEIVER_CRASH, dynamicBroadcastReceiverCrash);
        cv.put(ITA_Contract.BASE_COMPONENT_TYPE, baseComponentType);
        cv.put(ITA_Contract.VERSION_NAME, versionName);
        cv.put(ITA_Contract.VERSION_CODE, versionCode);
        cv.put(ITA_Contract.APK_MD5, getMD5fromPackageName(packageName));
        Uri dest = ITA_Contract.ResultsTable.CONTENT_URI;
        getContentResolver().insert(dest, cv);
        return instance;
    }
    
    private boolean isAppSystemApp(String packageName) {
        if (packageName == null)
            return false;

        ContentResolver cr = getContentResolver();
        Uri dest = ITA_Contract.AppsTable.CONTENT_URI;

        String[] projection = {ITA_Contract.APP_TYPE};
        String selection = ITA_Contract.PACKAGE_NAME + " = ?";
        String[] selectionArgs = {packageName};

        Cursor cursor = cr.query(dest, projection, selection, selectionArgs, null);
        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst == false)
                Log.w(TAG, "[" + ITA_Contract.AppsTable.APPS_TABLE_NAME + "] no entries for this table");
            else {
                String appType = cursor.getString(cursor.getColumnIndex(ITA_Contract.APP_TYPE));
                if (appType != null) {
                    if (appType.equals(ITA_Constants.SYSTEM_APP) || appType.equals(ITA_Constants.UPDATED_SYSTEM_APP)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            cursor.close();
        }
        return false;
    }

    String getMD5fromPackageName(String packageName) {
        if (packageName == null)
            return null;

        String md5 = "";
        ContentResolver cr = getContentResolver();
        Uri dest = ITA_Contract.AppsTable.CONTENT_URI;

        String[] projection = {ITA_Contract.APK_MD5};
        String selection = ITA_Contract.PACKAGE_NAME + " = ?";
        String[] selectionArgs = {packageName};

        Cursor cursor = cr.query(dest, projection, selection, selectionArgs, null);
        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst == false)
                Log.w(TAG, "[" + ITA_Contract.AppsTable.APPS_TABLE_NAME + "] no entries for this table");
            else {
                md5 = cursor.getString(cursor.getColumnIndex(ITA_Contract.APK_MD5));
            }
            cursor.close();
        }
        return md5;
    }

    String[] getAppVersionDatafromPackageName(String packageName) {
        if (packageName == null)
            return null;
        String[] appData = new String[3];
        ContentResolver cr = getContentResolver();
        Uri dest = ITA_Contract.AppsTable.CONTENT_URI;
        String[] projection = {ITA_Contract.APK_MD5, ITA_Contract.VERSION_CODE, ITA_Contract.VERSION_NAME};
        String selection = ITA_Contract.PACKAGE_NAME + " = ?";
        String[] selectionArgs = {packageName};
        Cursor cursor = cr.query(dest, projection, selection, selectionArgs, null);
        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst == false)
                Log.w(TAG, "[" + ITA_Contract.AppsTable.APPS_TABLE_NAME + "] no entries for this table");
            else {
                appData[0] = cursor.getString(cursor.getColumnIndex(ITA_Contract.APK_MD5));
                appData[1] = cursor.getString(cursor.getColumnIndex(ITA_Contract.VERSION_CODE));
                appData[2] = cursor.getString(cursor.getColumnIndex(ITA_Contract.VERSION_NAME));
            }
            cursor.close();
        }
        return appData;
    }
    
    synchronized boolean launchIntentsAccelerated(ArrayList<String> intents, boolean justAction, int id, int totalCount, String table) {

        foundCrashEternalThread = false;

        isSendingAcceleratedIntents = true;

        Log.w(TAG, "launchIntentsAccelerated and intent.size()=" + intents.size() + ", starting ID=" + id);

        if (justAction == true) {

            for (int a = 0; a < intents.size() && keepGoing; a++) {
                String action = intents.get(a);

                if (troublesomeBroadcastActions.contains(action)) {
                    Log.w(TAG, "[" + id + "/" + totalCount + "] Skipping troublesome broadcast action - " + action);
                    id++;
                    continue;
                }

                Log.e(TAG, "[" + id + "/" + totalCount + "] Sending " + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + " - action=" + action);

                try {
                    Intent i = new Intent(action);
                    this.sendBroadcast(i);
                    try {
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (foundCrashEternalThread == false) {
                        i = new Intent(action);
                        Bundle bundle = new Bundle();
                        i.putExtras(bundle);
                        this.sendBroadcast(i);
                        try {
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (foundCrashEternalThread == false) {
                            i.setData(ITA_Constants.SCHEMLESS_URI);
                            this.sendBroadcast(i);
                            try {
                                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                if (foundCrashEternalThread == true) {
                    Log.w(TAG, "Found crash - stopping this batch of aggressive intent sending");
                    break;
                }
                id++;
            }
        } else {
            for (int a = 0; a < intents.size() && keepGoing; a++) {
                String component = intents.get(a);
                if (troublesomeApplicationComponents.contains(component)) {
                    Log.w(TAG, "[" + id + "/" + totalCount + "] Skipping troublesome application component - " + component);
                    id++;
                    continue;
                }

                String[] parts = component.split("/");
                String packageName = parts[0];
                String componentName = parts[1];

                Intent i = new Intent();
                i.setClassName(packageName, componentName);

                if (table.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {

                    try {
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                        startActivity(i);
                        try {
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                        } catch (InterruptedException e) {
                            //e.printStackTrace();
                        }
                        if (foundCrashEternalThread == false) {
                            i.setAction("");
                            startActivity(i);
                            try {
                                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                            } catch (InterruptedException e) {}
                            if (foundCrashEternalThread == false) {
                                i.putExtras(emptyBundle);
                                startActivity(i);

                                try {
                                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                                } catch (InterruptedException e) {}

                                if (foundCrashEternalThread == false) {
                                    i.setData(ITA_Constants.SCHEMLESS_URI);
                                    startActivity(i);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                } else if (table.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
                    try {
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ServiceTable.SERVICE_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);

                        stopService(i);
                        try {
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                        } catch (InterruptedException e) {
                        }

                        startService(i);
                        try {
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                        } catch (InterruptedException e) {
                        }
                        if (foundCrashEternalThread == false) {
                            i.setAction("");
                            startService(i);
                            try {
                                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                            } catch (InterruptedException e) {
                            }
                            if (foundCrashEternalThread == false) {
                                i.putExtras(emptyBundle);
                                startService(i);
                                try {
                                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                                } catch (InterruptedException e) {
                                }

                                if (foundCrashEternalThread == false) {
                                    i.setData(ITA_Constants.SCHEMLESS_URI);
                                    startService(i);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                } else if (table.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME)) {

                    try {
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                        sendBroadcast(i);
                        try {
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                        } catch (InterruptedException e) {
                        }
                        if (foundCrashEternalThread == false) {
                            i.setAction("");
                            sendBroadcast(i);
                            try {
                                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                            } catch (InterruptedException e) {
                            }
                            if (foundCrashEternalThread == false) {
                                i.putExtras(emptyBundle);
                                sendBroadcast(i);
                                try {
                                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_ACCELERATED);
                                } catch (InterruptedException e) {
                                }

                                if (foundCrashEternalThread == false) {
                                    i.setData(ITA_Constants.SCHEMLESS_URI);
                                    sendBroadcast(i);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                } else
                    Log.e(TAG, "Table [" + table + "] not found and row=" + id);

                if (foundCrashEternalThread == true) {
                    Log.w(TAG, "Found crash - stopping this batch of aggressive intent sending");
                    break;
                }
                id++;

                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_INTENTS_AGGRESSIVE_SENDING);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        isSendingAcceleratedIntents = false;

        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FINAL_ACCELERATED_INTENT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return foundCrashEternalThread;
    }

    static class Thanatos implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            Log.w(TAG, "binderDied");
        }
    }

    public synchronized static void accessProvider(Uri uri, String tableName, ContentResolver cr, String authority, Context context, String packageName, String componentName) {

        Log.w(TAG, "accessProvider - authority=" + authority + ", packageName=" + packageName + ", componentName=" + componentName);

        contentProviderTestFinishes = false;

        String nullStr = null;

        ContentValues cv = new ContentValues();
        cv.put(nullStr, nullStr);
        cv.putNull("_id");
        cv.putNull("id");

        try {
            cr.query(uri, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.query(uri, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.query(uri, new String[]{}, null, new String[]{}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.query(uri, new String[]{"one", "two", "three"}, null, new String[]{null, null, null}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.query(uri, new String[]{"_id"}, null, new String[]{null}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notify();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.acquireUnstableContentProviderClient(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ArrayList al = new ArrayList();
            al.add(null);
            cr.applyBatch(authority, al);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.bulkInsert(uri, new ContentValues[]{null, null});
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.bulkInsert(uri, new ContentValues[]{cv});
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.cancelSync(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.getStreamTypes(uri, "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.unregisterContentObserver(coi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notifyChange(uri, coi); // this one
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notifyChange(uri, coi, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.registerContentObserver(uri, true, coi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.unregisterContentObserver(coi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ContentObserver nullcoi = null;

        try {
            cr.unregisterContentObserver(nullcoi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notifyChange(uri, nullcoi); // this one
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notifyChange(uri, nullcoi, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notifyChange(uri, nullcoi, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.registerContentObserver(uri, true, nullcoi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.registerContentObserver(uri, false, nullcoi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.notifyChange(uri, nullcoi); // this one
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.openAssetFileDescriptor(uri, "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                cr.openAssetFileDescriptor(uri, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                cr.releasePersistableUriPermission(uri, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                cr.takePersistableUriPermission(uri, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ContentProviderClient cpc = null;
            try {
                cpc = cr.acquireContentProviderClient(authority);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (cpc != null) {
                try {
                    cpc.uncanonicalize(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    cpc.canonicalize(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            cr.openOutputStream(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.openOutputStream(uri, "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.openTypedAssetFileDescriptor(uri, "r", emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.openTypedAssetFileDescriptor(uri, "text/plain", emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.startSync(uri, emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.update(uri, cv, "", new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.delete(uri, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.call(uri, "onCreate", "", emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.call(uri, "", "", emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.call(uri, "query", "", emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.call(uri, "query", null, emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.call(uri, "", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.getType(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.insert(uri, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }

        cv.put("_id", 2008);

        try {
            cr.insert(uri, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            cr.insert(uri, new ContentValues());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            InputStream is = cr.openInputStream(uri);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ContentResolver.requestSync(new Account("not", "empty"), authority, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ContentResolver.requestSync(new Account("not", "empty"), authority, emptyBundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle b = new Bundle();
        b.putString("something", nullStr);
        try {
            ContentResolver.requestSync(new Account("not", "empty"), authority, b);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROVIDER_AUTHORITY_TEST);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        contentProviderTestFinishes = true;
    }

    void startProviderApplicationComponent(Uri uri, final String tableName) {
        
        final ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);

        int totalCount = cursor.getCount();

        try {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {
                do {

                    String authority = cursor.getString(cursor.getColumnIndex(ITA_Contract.PROVIDER_AUTHORITY));

                    if (authority == null)
                        continue;

                    String[] parts = authority.split(";");

                    if (parts == null)
                        continue;

                    int id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                    int readAccess = cursor.getInt(cursor.getColumnIndex(ITA_Contract.CAN_READ));
                    int writeAccess = cursor.getInt(cursor.getColumnIndex(ITA_Contract.CAN_WRITE));
                    final String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                    final String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));

                    writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);

                    Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);


                    // give a small amount of time so that the log process reads the previous log message
                    try {
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_PROVIDER);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (int a = 0; a < parts.length; a++) {

                        final String currentAuthority = parts[a];

                        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY, authority);
                        final Uri baseUri = Uri.parse("content://" + authority);

                        Runnable queryContentProvider = new Runnable() {

                            @Override
                            public void run() {
                                accessProvider(baseUri, tableName, cr, currentAuthority, getApplicationContext(), packageName, componentName);
                            }
                        };
                        Thread t = new Thread(queryContentProvider);
                        t.start();

                        for (int z = 0; z < 3; z++) {
                            Log.w(TAG, "[Iteration " + z + "] sleep for provider " + packageName + "/" + componentName);

                            try {
                                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_DURING_PROVIDER_AUTHORITY_TEST);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (contentProviderTestFinishes) {
                                Log.w(TAG, "Breaking out on iteration " + z + " for provider " + packageName + "/" + componentName);
                                break;
                            }
                        }

                        if (contentProviderTestFinishes == false) {
                            Log.w(TAG, "provider hung and timed out authority=" + authority + ", packageName=" + packageName + ", componentName=" + componentName);
                            markComponentAsTimeOut(packageName, componentName, null, false, uri, cr);
                            break;
                        }
                    }
                } while (cursor.moveToNext() && keepGoing);
            } else
                Log.w(TAG, "[" + tableName + "] no entries for this table");
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
    
    static void markComponentAsTimeOut(String packageName, String componentName, String action, boolean isBroadcastAction, Uri uri, ContentResolver cr) {
        String selection = null;
        String[] selectionArgs = null;
        if (!isBroadcastAction) {
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else {
            selection = ITA_Contract.ACTION + " = ?";
            selectionArgs = new String[1];
            selectionArgs[0] = action;
        }
        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.TIMEOUT, Integer.valueOf(1));
        try {
            cr.update(uri, cv, selection, selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void markComponentAsSkipped(String packageName, String componentName, String action, boolean isBroadcastAction, Uri uri, ContentResolver cr) {
        String selection = null;
        String[] selectionArgs = null;
        if (!isBroadcastAction) {
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else {
            selection = ITA_Contract.ACTION + " = ?";
            selectionArgs = new String[1];
            selectionArgs[0] = action;
        }
        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.SKIPPED, Integer.valueOf(1));
        try {
            cr.update(uri, cv, selection, selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // will send all Intents to an application component. The uri belongs to the table to send
    // and the table name is just the table name :-)
    void startApplicationComponent(Uri uri, String tableName, boolean justAction) {
        ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);

        currentPackageName = null;

        int totalCount = cursor.getCount();

        boolean acceleratedTesting = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING);

        // allow acclerated testing for all components exception broadcast receiver actions
        if (acceleratedTesting && !tableName.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
            try {
                boolean moveToFirst = cursor.moveToFirst();
                if (moveToFirst) {
                    do {
                        if (justAction) {

                            ArrayList<String> actions = new ArrayList<String>();
                            boolean keepGathering = true;

                            String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                            int id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                            writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);

                            actions.add(action);

                            Log.w(TAG, "[" + tableName + "] aggressive sending of intents from id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));

                            int loopGuard = ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1;

                            for (int a = 0; a < loopGuard && keepGathering == true; a++) {

                                if (cursor.moveToNext()) {
                                    action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                    actions.add(action);
                                } else {
                                    keepGathering = false;
                                }
                            }

                            boolean foundError = launchIntentsAccelerated(actions, justAction, id, totalCount, tableName);
                            if (foundError == true) {
                                if (foundSystemCrashEternalThread == true) {
                                    Log.w(TAG, "System Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                    try {
                                        Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    foundSystemCrashEternalThread = false;
                                } else {
                                    Log.w(TAG, "Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED + " milliseconds");
                                    try {
                                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.w(TAG, "Found error during last Intent batch - resending Intents starting at id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));
                                for (int z = 0; z < actions.size(); z++) {
                                    String actionToSend = actions.get(z);
                                    writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                                    sendIntent(null, null, actionToSend, true, tableName, id, totalCount, true, getApplicationContext());
                                    id++;
                                }
                            } else {
                                continue;
                            }
                        } else {

                            ArrayList<String> components = new ArrayList<String>();
                            boolean keepGathering = true;

                            String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                            String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                            int id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                            writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);

                            components.add(packageName + "/" + componentName);

                            int loopGuard = ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1;

                            Log.w(TAG, "[" + tableName + "] aggressive sending of intents from id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));

                            for (int a = 0; a < loopGuard && keepGathering; a++) {
                                if (cursor.moveToNext()) {
                                    packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                    componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                                    components.add(packageName + "/" + componentName);
                                } else {
                                    keepGathering = false;
                                }
                            }

                            boolean foundError = this.launchIntentsAccelerated(components, justAction, id, totalCount, tableName);
                            if (foundError == true) {
                                if (foundSystemCrashEternalThread == true) {
                                    Log.w(TAG, "System Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                    try {
                                        Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    foundSystemCrashEternalThread = false;
                                } else {
                                    Log.w(TAG, "Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED + " milliseconds");
                                    try {
                                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                Log.w(TAG, "Found error during last Intent batch - resending Intents starting at id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));

                                for (int z = 0; z < components.size(); z++) {
                                    String component = components.get(z);
                                    writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                                    String[] parts = component.split("/");

                                    if (currentPackageName != null) {
                                        if (!parts[0].equals(currentPackageName)) {
                                            Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + currentPackageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                        }
                                    }

                                    boolean isSystemApp = this.isAppSystemApp(parts[0]);
                                    sendIntent(parts[0], parts[1], null, false, tableName, id, totalCount, isSystemApp, getApplicationContext());
                                    id++;
                                    currentPackageName = parts[0];
                                }
                            } else {

                                for (int z = 0; z < components.size(); z++) {

                                    String whole = components.get(z);
                                    String[] parts = whole.split("/");

                                    if (currentPackageName != null) {
                                        if (!currentPackageName.equals(parts[0])) {
                                            Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + currentPackageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                        }
                                    }
                                    currentPackageName = parts[0];
                                }
                                continue;
                            }
                        }
                    } while (cursor.moveToNext() && keepGoing);
                } else
                    Log.w(TAG, "[" + tableName + "] no entries for this table");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        } else {
            try {
                boolean moveToFirst = cursor.moveToFirst();
                if (moveToFirst) {
                    do {
                        if (justAction) {

                            int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);
                            int idColumn = cursor.getColumnIndex(ITA_Contract._ID);
                            String action = cursor.getString(actionColumn);
                            int id = cursor.getInt(idColumn);
                            writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                            sendIntent(null, null, action, true, tableName, id, totalCount, true, getApplicationContext());
                            if (foundSystemCrashEternalThread == true) {
                                String targettedApp = ITA_IntentService.readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);
                                if (targettedApp != null && !targettedApp.isEmpty() && !currentPackageName.equals("android")) {
                                    PackageManager pm = getPackageManager();
                                    Intent intent = pm.getLaunchIntentForPackage(targettedApp);
                                    if (intent != null) {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        Log.w(TAG, "restart " + targettedApp + " after crash from action broadcast to potentially reregister dynamically-registered broadcast receivers");
                                        try {
                                            startActivity(intent);
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_FAILED_INTENT);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else if (currentPackageName != null && !currentPackageName.equals("android")) {
                                    PackageManager pm = getPackageManager();
                                    Intent intent = pm.getLaunchIntentForPackage(currentPackageName);
                                    if (intent != null) {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        Log.w(TAG, "restart " + currentPackageName + " after crash from action broadcast to potentially reregister dynamically-registered broadcast receivers");
                                        try {
                                            startActivity(intent);
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_FAILED_INTENT);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else {
                            int packageColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
                            int componentColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);
                            int idColumn = cursor.getColumnIndex(ITA_Contract._ID);
                            String packageName = cursor.getString(packageColumn);
                            String componentName = cursor.getString(componentColumn);
                            int id = cursor.getInt(idColumn);
                            boolean isSystemApp = isAppSystemApp(packageName);
                            if (currentPackageName != null) {
                                if (!currentPackageName.equals(packageName)) {
                                    Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + currentPackageName + ITA_Constants.DELIMITER_CHAR_LOG);

                                    // stop the app to get better picture of which components dynamically register the broadcast receivers
                                    if (ITA_Constants.KILL_APP_BEFORE_FIRST_TEST_FOR_BETTER_DYN_BROAD_REG_IDENTIFICATION && isSystemApp) {
                                        Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + packageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                        Log.w(TAG, "Sleeping " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP + " milliseconds for app to be stopped");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {
                                // stop the app to get better picture of which components dynamically register the broadcast receivers
                                if (ITA_Constants.KILL_APP_BEFORE_FIRST_TEST_FOR_BETTER_DYN_BROAD_REG_IDENTIFICATION && isSystemApp) {
                                    Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + packageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                    Log.w(TAG, "Sleeping " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP + " milliseconds for app to be stopped");
                                    try {
                                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            // record the row ID
                            writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                            sendIntent(packageName, componentName, null, false, tableName, id, totalCount, isSystemApp, getApplicationContext());
                            currentPackageName = packageName;
                        }
                    } while (cursor.moveToNext() && keepGoing);
                } else
                    Log.w(TAG, "[" + tableName + "] no entries for this table");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    boolean retestActionOrComponentForSoftReboot(String action, String packageName, String componentName, String tableName, int id, int totalCount, String authority) {
        Log.e(TAG, "retestActionOrComponentForSoftReboot");
        // number of times to retry if there is a android.os.DeadSystemException which can happen
        // if the device comes up to quickly. Not sure why.
        int counter = 0;

        do {
            boolean hitExceptionDuringSending = false;
            Intent i = new Intent();
            try {
                if (tableName.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
                    Log.e(TAG, "[" + id + "/" + totalCount + "] retesting - action=" + action + ", table=" + tableName);
                    i.setAction(action);
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                    i = new Intent(action);
                    Bundle bundle = new Bundle();
                    i.putExtras(bundle);
                    sendBroadcast(i);
                } else {
                    Log.e(TAG, "[" + id + "/" + totalCount + "] retesting - packageName=" + packageName + ", componentName=" + componentName + ", table=" + tableName);
                    i.setClassName(packageName, componentName);
                    if (tableName.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                        startActivity(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        i.setAction("");
                        startActivity(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        i.putExtras(emptyBundle);
                        startActivity(i);
                    } else if (tableName.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ServiceTable.SERVICE_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                        stopService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        startService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        i.setAction("");
                        startService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        i.putExtras(emptyBundle);
                        startService(i);

                    } else if (tableName.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME)) {
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                        sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        i.setAction("");
                        sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        i.putExtras(emptyBundle);
                        sendBroadcast(i);
                    } else if (tableName.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME)) {
                        Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                        accessProvider(Uri.parse("content://" + authority), tableName, getContentResolver(), authority, getApplicationContext(), packageName, componentName);
                    }
                }

                Log.w(TAG, "sleeping for " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_RETEST_INTENT_SOFT_REBOOT + " milliseconds");
                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_RETEST_INTENT_SOFT_REBOOT);
                return foundSystemCrashEternalThread;
            } catch (Exception e) {

                hitExceptionDuringSending = true;
                String message = e.getMessage(); // android.os.DeadSystemException
                Log.e(TAG, "Failure sending " + action + " - " + message + ", counter = " + counter);
                // system is dying at this point
                if (message.contains("Failure from system")) {
                    try {
                        Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } else { // not a fatal system excpetion - sleep for system to boot
                    try {
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_RETEST_INTENT_SOFT_REBOOT);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } while (counter++ < 3);

        return false;
    }

    void resumeContentProvider(Uri uri, String tableName, int lastRowID, boolean justSoftRebooted, String actionReceived) {
        ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);
        boolean rowIDFound = false;
        int totalCount = cursor.getCount();
        boolean testNormally = false;
        int authorityIndex = -1;
        Log.w(TAG, "resumeContentProvider [" + tableName + "] and cursor count=" + totalCount + ", id=" + lastRowID + ", actionReceived=" + actionReceived);
        boolean moveToFirst = cursor.moveToFirst();
        if (moveToFirst) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                if (lastRowID == 1 && justSoftRebooted == false)
                    rowIDFound = true;
                else if (rowIDFound == false) {
                    if (id == lastRowID) {
                        rowIDFound = true;
                        Log.w(TAG, "[" + tableName + "] LastRow=" + id + ", nextRow=" + (id + 1));
                        if (actionReceived != null && (actionReceived.equals(Intent.ACTION_BOOT_COMPLETED) || actionReceived.equals(ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS))) {
                            String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                            String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                            String retestComponent = readStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH);
                            if (retestComponent == null || retestComponent.isEmpty()) {
                                writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, packageName + "/" + componentName);
                                String[] authorities = ITA_IntentService.getAuthoritiesFromProvider(packageName, componentName, getPackageManager());
                                if (authorities == null) {
                                    Log.w(TAG, "authority is either null or empty");
                                    continue;
                                }
                                String authority = readStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY);
                                if (authority == null || authority.isEmpty()) {
                                    Log.w(TAG, "authority is either null or empty");
                                    continue;
                                }
                                boolean found = false;
                                for (int a = 0; a < authorities.length; a++) {
                                    if (authority.equals(authorities[a])) {
                                        found = true;
                                        authorityIndex = a;
                                        Log.w(TAG, "authority to test found - " + authority);
                                        break;
                                    }
                                }
                                if (found == true) {
                                    boolean softRebooted = retestActionOrComponentForSoftReboot(null, packageName, componentName, tableName, id, totalCount, authority);
                                    if (softRebooted == true) {
                                        Log.e(TAG, "Soft Reboot - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                        } catch (InterruptedException ex) {
                                            ex.printStackTrace();
                                        }
                                    } else {
                                        testNormally = true;
                                        Log.e(TAG, "Did not soft reboot after testing packageName=" + packageName + ", componentName=" + componentName);
                                    }
                                } else {
                                    testNormally = true;
                                }
                            } else {
                                Log.e(TAG, "Just soft rebooted retestedComponent=" + retestComponent + ", currentComponent=" + packageName + "/" + componentName);
                                writeSystemFailureIntent(tableName + "|" + packageName + "|" + componentName);
                            }
                            Log.e(TAG, "Setting " + ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH + " to null");
                            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, null);
                        } else {
                            Log.w(TAG, "was not a real soft reboot");
                            testNormally = true;
                            String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                            String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                            String currentComponent = packageName + "/" + componentName;
                            boolean shouldSkip = checkKillCountComponentOrAction(currentComponent, false, uri);
                            if (shouldSkip == true)
                                continue;
                        }
                    }
                    if (testNormally == false)
                        continue;
                }

                String authority = cursor.getString(cursor.getColumnIndex(ITA_Contract.PROVIDER_AUTHORITY));
                if (authority == null)
                    continue;
                String[] parts = authority.split(";");
                if (parts == null)
                    continue;
                int readAccess = cursor.getInt(cursor.getColumnIndex(ITA_Contract.CAN_READ));
                int writeAccess = cursor.getInt(cursor.getColumnIndex(ITA_Contract.CAN_WRITE));
                String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                for (int a = 0; a < parts.length; a++) {
                    authority = parts[a];
                    if (authorityIndex != -1) {
                        if (authorityIndex != a) {
                            continue;
                        } else {
                            authorityIndex = -1;
                        }
                    }
                    writeStringSharedPreferences(getApplicationContext(), ITA_Constants.AUTHORITY, authority);
                    Uri baseUri = Uri.parse("content://" + authority);
                    accessProvider(baseUri, tableName, cr, authority, getApplicationContext(), packageName, componentName);
                }

            } while (cursor.moveToNext() && keepGoing);
        } else
            Log.w(TAG, "[" + tableName + "] no entries for this table");
    }

    // will send all Intents to an application component. The uri belongs to the table to send
    // and the table name is just the table name :-)
    void resumeApplicationComponent(Uri uri, String tableName, boolean justAction, int lastRowID, boolean justSoftRebootedOrAppKilled, String actionReceived) {
        ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);
        boolean rowIDFound = false;
        currentPackageName = null;
        boolean testNormally = false;
        int totalCount = cursor.getCount();
        Log.w(TAG, "resumeApplicationComponent [" + tableName + "] and cursor count=" + totalCount + ", id=" + lastRowID + ", actionReceived=" + actionReceived);
        try {
            boolean acceleratedTesting = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING);
            if (acceleratedTesting && !tableName.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
                int idStartAccelerated = lastRowID + ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED;
                Log.w(TAG, "[" + tableName + "] start accelerated intent sending at id=" + lastRowID);
                boolean moveToFirst = cursor.moveToFirst();
                if (moveToFirst) {
                    do {
                        if (justAction) {
                            int id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                            if (lastRowID == 1 && justSoftRebootedOrAppKilled == false)
                                rowIDFound = true;
                            else if (rowIDFound == false) {
                                if (id == lastRowID) {
                                    rowIDFound = true;
                                    Log.w(TAG, "[" + tableName + "] LastRow=" + id + ", nextRow=" + (id + 1));
                                    if (actionReceived != null && (actionReceived.equals(Intent.ACTION_BOOT_COMPLETED) || actionReceived.equals(ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS))) {
                                        String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));

                                        String retestAction = readStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH);
                                        if (retestAction == null || retestAction.isEmpty()) {

                                            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, action);

                                            boolean softRebooted = retestActionOrComponentForSoftReboot(action, null, null, tableName, id, totalCount, null);
                                            if (softRebooted == true) {
                                                Log.e(TAG, "Soft Reboot - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                                try {
                                                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                                } catch (InterruptedException ex) {
                                                    ex.printStackTrace();
                                                }
                                            } else {
                                                testNormally = true;
                                                Log.e(TAG, "Did not soft reboot after retesting action=" + action);
                                            }
                                        } else {
                                            Log.e(TAG, "Just soft rebooted retestedAction=" + retestAction + ", currentAction=" + action);
                                            writeSystemFailureIntent(tableName + "|" + action);
                                        }
                                        Log.e(TAG, "Setting " + ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH + " to null");
                                        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, null);

                                    } else {
                                        Log.w(TAG, "was not a real soft reboot");
                                        testNormally = true;

                                        String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                        boolean shouldSkip = checkKillCountComponentOrAction(action, true, uri);
                                        if (shouldSkip == true)
                                            continue;
                                    }
                                }
                                if (testNormally == false)
                                    continue;
                            }

                            if (id < idStartAccelerated) {
                                String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                                sendIntent(null, null, action, true, tableName, id, totalCount, true, getApplicationContext());
                            } else {

                                int loopGuard = ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1;

                                Log.w(TAG, "[" + tableName + "] aggressive sending of intents from id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));

                                ArrayList<String> actions = new ArrayList<String>();
                                boolean keepGathering = true;

                                String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                                writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);

                                actions.add(action);

                                for (int a = 0; a < loopGuard && keepGathering == true; a++) {

                                    if (cursor.moveToNext()) {
                                        action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                        actions.add(action);
                                    } else {
                                        keepGathering = false;
                                    }
                                }

                                boolean foundError = this.launchIntentsAccelerated(actions, justAction, id, totalCount, tableName);
                                if (foundError == true) {

                                    if (foundSystemCrashEternalThread == true) {
                                        Log.w(TAG, "System Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        foundSystemCrashEternalThread = false;
                                    } else {
                                        Log.w(TAG, "Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED + " milliseconds");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    Log.w(TAG, "Found error during last Intent batch - resending Intents starting at id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));
                                    for (int z = 0; z < actions.size(); z++) {
                                        String actionToSend = actions.get(z);
                                        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                                        sendIntent(null, null, actionToSend, true, tableName, id++, totalCount, true, getApplicationContext());
                                    }
                                } else {
                                    continue;
                                }
                            }
                        } else {

                            int idColumn = cursor.getColumnIndex(ITA_Contract._ID);
                            int id = cursor.getInt(idColumn);

                            if (lastRowID == 1 && justSoftRebootedOrAppKilled == false)
                                rowIDFound = true;
                            else if (rowIDFound == false) {
                                if (id == lastRowID) {
                                    rowIDFound = true;
                                    Log.w(TAG, "[" + tableName + "] LastRow=" + id + ", nextRow=" + (id + 1));
                                    if (actionReceived != null && (actionReceived.equals(Intent.ACTION_BOOT_COMPLETED) || actionReceived.equals(ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS))) {
                                        String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                        String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));

                                        String retestComponent = readStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH);
                                        if (retestComponent == null || retestComponent.isEmpty()) {

                                            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, packageName + "/" + componentName);

                                            boolean softRebooted = retestActionOrComponentForSoftReboot(null, packageName, componentName, tableName, id, totalCount, null);
                                            if (softRebooted == true) {
                                                Log.e(TAG, "Soft Reboot - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                                try {
                                                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                                } catch (InterruptedException ex) {
                                                    ex.printStackTrace();
                                                }

                                            } else {
                                                testNormally = true;
                                                Log.e(TAG, "Did not soft reboot after testing packageName=" + packageName + ", componentName=" + componentName);
                                            }
                                        } else {
                                            Log.e(TAG, "Just soft rebooted retestedComponent=" + retestComponent + ", currentComponent=" + packageName + "/" + componentName);
                                            writeSystemFailureIntent(tableName + "|" + packageName + "|" + componentName);
                                        }
                                        Log.e(TAG, "Setting " + ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH + " to null");
                                        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, null);

                                    } else {
                                        Log.w(TAG, "was not a real soft reboot");
                                        testNormally = true;

                                        String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                        String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                                        String currentComponent = packageName + "/" + componentName;
                                        boolean shouldSkip = checkKillCountComponentOrAction(currentComponent, false, uri);
                                        if (shouldSkip == true)
                                            continue;
                                    }

                                }
                                if (testNormally == false)
                                    continue;
                            }

                            if (id < idStartAccelerated) {
                                String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                                writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                                boolean isSystemApp = isAppSystemApp(packageName);
                                sendIntent(packageName, componentName, null, false, tableName, id, totalCount, isSystemApp, getApplicationContext());
                            } else {

                                int loopGuard = ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1;

                                Log.w(TAG, "[" + tableName + "] aggressive sending of intents from id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));

                                ArrayList<String> components = new ArrayList<String>();
                                boolean keepGathering = true;

                                id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));
                                String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));

                                writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);

                                components.add(packageName + "/" + componentName);

                                for (int a = 0; a < loopGuard && keepGathering == true; a++) {

                                    if (cursor.moveToNext()) {
                                        packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                        componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                                        components.add(packageName + "/" + componentName);
                                    } else {
                                        keepGathering = false;
                                    }
                                }

                                boolean foundError = this.launchIntentsAccelerated(components, justAction, id, totalCount, tableName);
                                if (foundError == true) {
                                    if (foundSystemCrashEternalThread == true) {
                                        Log.w(TAG, "System Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        foundSystemCrashEternalThread = false;
                                    } else {
                                        Log.w(TAG, "Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED + " milliseconds");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_ACCELERATED);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    Log.w(TAG, "Found error during last Intent batch - resending Intents starting at id=" + id + " to id=" + (id + (ITA_Constants.NUMBER_INTENTS_TO_SEND_ACCELERATED - 1)));
                                    for (int z = 0; z < components.size(); z++) {
                                        String[] parts = components.get(z).split("/");
                                        writeIntSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                                        boolean isSystemApp = isAppSystemApp(parts[0]);
                                        sendIntent(parts[0], parts[1], null, false, tableName, id++, totalCount, isSystemApp, getApplicationContext());
                                        if (currentPackageName != null) {
                                            if (!currentPackageName.equals(parts[0])) {
                                                Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + currentPackageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                            }
                                        }
                                        currentPackageName = parts[0];
                                    }
                                } else {
                                    for (int z = 0; z < components.size(); z++) {
                                        String whole = components.get(z);
                                        String[] parts = whole.split("/");
                                        if (currentPackageName != null) {
                                            if (!currentPackageName.equals(parts[0])) {
                                                Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + currentPackageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                            }
                                        }
                                        currentPackageName = parts[0];
                                    }
                                    continue;
                                }
                            }
                        }
                    } while (cursor.moveToNext() && keepGoing);
                } else
                    Log.w(TAG, "[" + tableName + "] no entries for this table");
            } else {
                boolean moveToFirst = cursor.moveToFirst();
                if (moveToFirst) {
                    do {
                        if (justAction) {
                            int idColumn = cursor.getColumnIndex(ITA_Contract._ID);
                            int id = cursor.getInt(idColumn);
                            if (lastRowID == 1 && justSoftRebootedOrAppKilled == false)
                                rowIDFound = true;
                            else if (rowIDFound == false) {
                                if (id == lastRowID) {
                                    rowIDFound = true;
                                    Log.w(TAG, "[" + tableName + "] LastRow=" + id + ", nextRow=" + (id + 1));
                                    if (actionReceived != null && (actionReceived.equals(Intent.ACTION_BOOT_COMPLETED) || actionReceived.equals(ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS))) {
                                        String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                        String retestAction = readStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH);
                                        if (retestAction == null || retestAction.isEmpty()) {
                                            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, action);
                                            boolean softRebooted = retestActionOrComponentForSoftReboot(action, null, null, tableName, id, totalCount, null);
                                            if (softRebooted == true) {
                                                Log.e(TAG, "Soft Reboot - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                                try {
                                                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                                } catch (InterruptedException ex) {
                                                    ex.printStackTrace();
                                                }
                                            } else {
                                                testNormally = true;
                                                Log.e(TAG, "Did not soft reboot after retesting action=" + action);
                                            }
                                        } else {
                                            Log.e(TAG, "Just soft rebooted retestedAction=" + retestAction + ", currentAction=" + action);
                                            writeSystemFailureIntent(tableName + "|" + action);
                                        }
                                        Log.e(TAG, "Setting " + ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH + " to null");
                                        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, null);
                                    } else {
                                        Log.w(TAG, "was not a real soft reboot");
                                        testNormally = true;
                                        String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                                        boolean shouldSkip = checkKillCountComponentOrAction(action, true, uri);
                                        if (shouldSkip == true)
                                            continue;
                                    }
                                }
                                if (testNormally == false)
                                    continue;
                            }
                            int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);
                            String action = cursor.getString(actionColumn);
                            writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                            sendIntent(null, null, action, true, tableName, id, totalCount, true, getApplicationContext());
                            if (foundSystemCrashEternalThread == true) {
                                String targettedApp = ITA_IntentService.readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);
                                if (targettedApp != null && !targettedApp.isEmpty() && !currentPackageName.equals("android")) {
                                    PackageManager pm = getPackageManager();
                                    Intent intent = pm.getLaunchIntentForPackage(targettedApp);
                                    if (intent != null) {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        Log.w(TAG, "restart " + targettedApp + " after crash from action broadcast to potentially reregister dynamically-registered broadcast receivers");
                                        try {
                                            startActivity(intent);
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_FAILED_INTENT);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else if (currentPackageName != null && !currentPackageName.equals("android")) {
                                    PackageManager pm = getPackageManager();
                                    Intent intent = pm.getLaunchIntentForPackage(currentPackageName);
                                    if (intent != null) {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        Log.w(TAG, "restart " + currentPackageName + " after crash from action broadcast to potentially reregister dynamically-registered broadcast receivers");
                                        try {
                                            startActivity(intent);
                                            Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_FAILED_INTENT);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } else {
                            int idColumn = cursor.getColumnIndex(ITA_Contract._ID);
                            int id = cursor.getInt(idColumn);
                            if (lastRowID == 1 && justSoftRebootedOrAppKilled == false)
                                rowIDFound = true;
                            else if (rowIDFound == false) {
                                if (id == lastRowID) {
                                    rowIDFound = true;
                                    Log.w(TAG, "[" + tableName + "] LastRow=" + id + ", nextRow=" + (id + 1));
                                    if (actionReceived != null && (actionReceived.equals(Intent.ACTION_BOOT_COMPLETED) || actionReceived.equals(ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS))) {
                                        String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                        String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                                        String retestComponent = readStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH);
                                        if (retestComponent == null || retestComponent.isEmpty()) {
                                            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, packageName + "/" + componentName);
                                            boolean softRebooted = retestActionOrComponentForSoftReboot(null, packageName, componentName, tableName, id, totalCount, null);
                                            if (softRebooted == true) {
                                                Log.e(TAG, "Soft Reboot - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                                                try {
                                                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                                                } catch (InterruptedException ex) {
                                                    ex.printStackTrace();
                                                }
                                            } else {
                                                testNormally = true;
                                                Log.e(TAG, "Did not soft reboot after testing packageName=" + packageName + ", componentName=" + componentName);
                                            }
                                        } else {
                                            Log.e(TAG, "Just soft rebooted retestedComponent=" + retestComponent + ", currentComponent=" + packageName + "/" + componentName);
                                            writeSystemFailureIntent(tableName + "|" + packageName + "|" + componentName);

                                        }
                                        Log.e(TAG, "Setting " + ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH + " to null");
                                        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.RETESTING_INTENT_SYSTEM_CRASH, null);
                                    } else {
                                        // our app was killed
                                        Log.w(TAG, "was not a real soft reboot");
                                        testNormally = true;
                                        String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                                        String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                                        String currentComponent = packageName + "/" + componentName;
                                        boolean shouldSkip = checkKillCountComponentOrAction(currentComponent, false, uri);
                                        if (shouldSkip == true)
                                            continue;
                                    }
                                }
                                if (testNormally == false)
                                    continue;
                            }

                            String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                            String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                            boolean isSystemApp = isAppSystemApp(packageName);
                            if (currentPackageName != null) {
                                if (!packageName.equals(currentPackageName)) {
                                    Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + currentPackageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                    // stop the app to get better picture of which components dynamically register the broadcast receivers
                                    if (ITA_Constants.KILL_APP_BEFORE_FIRST_TEST_FOR_BETTER_DYN_BROAD_REG_IDENTIFICATION && isSystemApp) {
                                        Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + packageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                        Log.w(TAG, "Sleeping " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP + " milliseconds for app to be stopped");
                                        try {
                                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {
                                // stop the app to get better picture of which components dynamically register the broadcast receivers
                                if (ITA_Constants.KILL_APP_BEFORE_FIRST_TEST_FOR_BETTER_DYN_BROAD_REG_IDENTIFICATION && isSystemApp) {
                                    Log.w(ITA_Constants.FORCESTOP_APP_TAG, ITA_Constants.DELIMITER_CHAR_LOG + packageName + ITA_Constants.DELIMITER_CHAR_LOG);
                                    Log.w(TAG, "Sleeping " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP + " milliseconds for app to be stopped");
                                    try {
                                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_FORCE_STOP_APP);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            // record the row ID
                            writeIntSharedPreferences(this.getApplicationContext(), ITA_Constants.CURRENT_ROW_ID, id);
                            sendIntent(packageName, componentName, null, false, tableName, id, totalCount, isSystemApp, getApplicationContext());
                            currentPackageName = packageName;
                        }
                    } while (cursor.moveToNext() && keepGoing);
                } else
                    Log.w(TAG, "[" + tableName + "] no entries for this table");
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    boolean checkKillCountComponentOrAction(String currentComponentOrAction, boolean isBroadcastAction, Uri uri) {
        String troublesomeComponentSP = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TROUBLESOME_COMPONENT_NAME);
        if (troublesomeComponentSP == null || troublesomeComponentSP.isEmpty()) {
            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TROUBLESOME_COMPONENT_NAME, currentComponentOrAction);
            writeIntSharedPreferences(getApplicationContext(), ITA_Constants.NUM_TIMES_APP_KILLED_ON_TROUBLESOME_COMPONENT, 1);
            //Log.w(TAG, "component or action that killed our app [" + currentComponentOrAction + "] killCount = 1");
            return false;
        } else {
            if (troublesomeComponentSP.equals(currentComponentOrAction)) {
                int timesKilled = readIntSharedPreferences(getApplicationContext(), ITA_Constants.NUM_TIMES_APP_KILLED_ON_TROUBLESOME_COMPONENT);
                if (timesKilled >= ITA_Constants.NUM_TIMES_KILLED_SKIP_THRESHOLD) {

                    writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TROUBLESOME_COMPONENT_NAME, null);
                    writeIntSharedPreferences(getApplicationContext(), ITA_Constants.NUM_TIMES_APP_KILLED_ON_TROUBLESOME_COMPONENT, 0);

                    // record that it was aborted
                    String selection = null;
                    String[] selectionArgs = null;

                    String packageName = "";
                    String componentName = "";

                    if (!isBroadcastAction) {
                        selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
                        String[] parts = currentComponentOrAction.split("/");
                        selectionArgs = new String[2];
                        packageName = parts[0];
                        selectionArgs[0] = parts[0];
                        componentName = parts[1];
                        selectionArgs[1] = parts[1];
                    } else {
                        selection = ITA_Contract.ACTION + " = ?";
                        selectionArgs = new String[1];
                        selectionArgs[0] = currentComponentOrAction;
                    }

                    ContentResolver cr = getContentResolver();
                    ContentValues cv = new ContentValues();
                    cv.put(ITA_Contract.SKIPPED, Integer.valueOf(1));
                    cr.update(uri, cv, selection, selectionArgs);

                    String dynamicBroadcastAction = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_ACTIION_DYNAMIC_RECEIVER_TEST);
                    if (dynamicBroadcastAction != null && !dynamicBroadcastAction.isEmpty()) {
                        HashSet<String> killerActions = readHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_ACTION);
                        killerActions.add(dynamicBroadcastAction);
                        Log.w(TAG, "Added dynamic broadcast action to killer actions - " + dynamicBroadcastAction);

                        HashSet<String> testedBroadcastActions = readHashSetSharedPreferences(getApplicationContext(), ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP);
                        if (testedBroadcastActions != null) {
                            testedBroadcastActions.add(dynamicBroadcastAction);
                            Log.w(TAG, "Added dynamic broadcast action to tested dynamic broadcast actions - " + dynamicBroadcastAction);
                            writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP, testedBroadcastActions);
                        }
                        updateSkippedDynamicBroadcastReceiverActions(packageName, componentName, uri, dynamicBroadcastAction);
                    }

                    if (isBroadcastAction) {
                        HashSet<String> killerActions = readHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_ACTION);
                        killerActions.add(currentComponentOrAction);
                        writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_ACTION, killerActions);
                        Log.w(TAG, "wrote action=" + currentComponentOrAction + " to " + ITA_Constants.HASHSET_APP_KILLER_ACTION + " and size is " + killerActions.size());
                    } else {
                        HashSet<String> killerComponents = readHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_COMPONENT_NAME);
                        killerComponents.add(currentComponentOrAction);
                        writeHashSetSharedPreferences(getApplicationContext(), ITA_Constants.HASHSET_APP_KILLER_COMPONENT_NAME, killerComponents);
                        Log.w(TAG, "wrote component=" + currentComponentOrAction + " to " + ITA_Constants.HASHSET_APP_KILLER_COMPONENT_NAME + " and size is " + killerComponents.size());
                    }
                    Log.w(TAG, "skipping component or action that killed our app " + ITA_Constants.TROUBLESOME_COMPONENT_NAME + " times - " + currentComponentOrAction);
                    return true;
                } else {
                    int newKillCount = timesKilled + 1;
                    writeIntSharedPreferences(getApplicationContext(), ITA_Constants.NUM_TIMES_APP_KILLED_ON_TROUBLESOME_COMPONENT, newKillCount);
                    Log.w(TAG, "component or action that killed our app [" + currentComponentOrAction + "] killCount = " + newKillCount);
                    return false;
                }
            } else {
                writeStringSharedPreferences(getApplicationContext(), ITA_Constants.TROUBLESOME_COMPONENT_NAME, currentComponentOrAction);
                writeIntSharedPreferences(getApplicationContext(), ITA_Constants.NUM_TIMES_APP_KILLED_ON_TROUBLESOME_COMPONENT, 1);
                return false;
            }
        }

    }

    JSONArray convertCrashResultsToJSON() {
        JSONArray toRet = new JSONArray();
        final ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, null, null, null);
        try {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {
                do {
                    String instanceType = cursor.getString(cursor.getColumnIndex(ITA_Contract.INSTANCE_TYPE));
                    String packageName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME));
                    String componentName = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME));
                    String componentType = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_TYPE));
                    int dynamicReceiverCrash = cursor.getInt(cursor.getColumnIndex(ITA_Contract.DYNAMIC_RECEIVER_CRASH));
                    String baseComponentType = cursor.getString(cursor.getColumnIndex(ITA_Contract.BASE_COMPONENT_TYPE));
                    String action = cursor.getString(cursor.getColumnIndex(ITA_Contract.ACTION));
                    String processName = cursor.getString(cursor.getColumnIndex(ITA_Contract.PROCESS_NAME));
                    String exceptionName = cursor.getString(cursor.getColumnIndex(ITA_Contract.EXCEPTION_NAME));
                    String exceptionReason = cursor.getString(cursor.getColumnIndex(ITA_Contract.EXCEPTION_REASON));
                    String logEvents = cursor.getString(cursor.getColumnIndex(ITA_Contract.LOG_EVENT_MESSAGES));
                    int pid = cursor.getInt(cursor.getColumnIndex(ITA_Contract.PID));
                    int id = cursor.getInt(cursor.getColumnIndex(ITA_Contract._ID));

                    JSONObject crashInstance = new JSONObject();
                    try {
                        crashInstance.put("id", id);
                        crashInstance.put("instanceType", instanceType);
                        crashInstance.put("packageName", packageName);
                        crashInstance.put("componentName", componentName);
                        crashInstance.put("componentType", componentType);
                        crashInstance.put("dynamicReceiverCrash", dynamicReceiverCrash);
                        crashInstance.put("baseComponentType", baseComponentType);
                        crashInstance.put("action", action);
                        crashInstance.put("processName", processName);
                        crashInstance.put("exceptionName", exceptionName);
                        crashInstance.put("exceptionReason", exceptionReason);
                        crashInstance.put("logEvents", logEvents);
                        crashInstance.put("pid", pid);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    toRet.put(crashInstance);
                } while (cursor.moveToNext());
            } else
                Log.w(TAG, "[" + ITA_Contract.ResultsTable.RESULTS_TABLE_NAME + "] no entries for this table");
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return toRet;
    }
    
    String copyJSONresultsToSdcard() {

        String filename = null;

        String possibleTargetApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        if (possibleTargetApp != null && !possibleTargetApp.isEmpty()) {

            String[] appData = getAppVersionDatafromPackageName(possibleTargetApp);
            if (appData == null) {
                filename = possibleTargetApp + ".json";
            } else {
                filename = possibleTargetApp + "_" + appData[2] + "_" + appData[1] + "_" + appData[0] + ".json";
            }

            String currentDir = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR);
            File jsonOutputPath = new File(currentDir, filename);

            JSONArray jarray = this.convertCrashResultsToJSON();

            for (int a = 0; a < 3; a++) {

                writeArbitraryStringToSDCard(jsonOutputPath.getAbsolutePath(), jarray.toString());

                if (jsonOutputPath.exists())
                    break;
            }

            return jsonOutputPath.getAbsolutePath();

        }
        return filename;
    }
    
    String copyDatabaseToSdcard() {
        String filename = "";
        String[] command = {"getprop", "ro.build.fingerprint"};

        String possibleTargetApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        if (possibleTargetApp != null && !possibleTargetApp.isEmpty()) {

            String[] appData = getAppVersionDatafromPackageName(possibleTargetApp);
            if (appData == null) {
                filename = possibleTargetApp + ".db";
            } else {
                filename = possibleTargetApp + "_" + appData[2] + "_" + appData[1] + "_" + appData[0] + ".db";
            }
        } else {
            ArrayList<String> fingerprintData = execCommandGetOutput(command);
            if (fingerprintData.size() != 0) {
                String fingerprint = fingerprintData.get(0);
                if (fingerprint != null)
                    filename = fingerprint.replace("/", " ") + ".db";
                else
                    filename = "somedevice.db";
            } else
                filename = "somedevice.db";
        }

        String currentDir = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR);

        for (int a = 0; a < 3; a++) {
            String[] cmd = {"cp", this.getDatabasePath(ITA_DBHelper.DBNAME).getAbsolutePath(), currentDir + "/" + filename};
            ArrayList<String> what = execCommandGetOutput(cmd);
            // will be size zero if successful
            if (what.size() == 0)
                break;
        }
        return currentDir + "/" + filename;
    }

    void recordTimeFile(String fileName) {
        String dir = readStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR);
        File file = new File(dir, fileName);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            long millisTimestamp = System.currentTimeMillis();
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            fw.write(millisTimestamp + "|" + timeStamp + newLine);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void writeArbitraryStringToSDCard(String filePath, String fileContent) {
        File file = new File(filePath);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(fileContent);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void restestDynamicallyRegisteredBroadcastReceivers(Context context, String packageName, String componentName, String componentType) {

        ArrayList<String> broadsToTest = new ArrayList<String>();
        parseOutBroadcastReceiversForApp(packageName, broadsToTest);
        if (broadsToTest != null && broadsToTest.size() != 0) {
            sendDynamicallyRegisteredBroadcastActions(broadsToTest, packageName, componentName, context, componentType);
            try {
                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (ITA_IntentService.foundCrashEternalThread == true)
            crashDueToCrashInDynamicBroadcastReceiver = true;
    }
    
    public void sendIntentRetest(String packageName, String componentName, String action, String componentType, boolean actionOnly, String baseComponentType) {
        if (actionOnly) {
            Log.w(TAG, "[retest] Sending intent [" + componentType + "]: " + action);
        } else {
            Log.w(TAG, "[retest] Sending intent [" + componentType + "]: " + packageName + "/" + componentName);
        }

        try {
            if (componentType.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {
                Intent i = new Intent();
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                i.setClassName(packageName, componentName);
                try {

                    startActivity(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.setAction("");
                    startActivity(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.putExtras(emptyBundle);
                    startActivity(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.setData(ITA_Constants.SCHEMLESS_URI);
                    startActivity(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == false && readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))
                        restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (componentType.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
                Intent i = new Intent();
                i.setClassName(packageName, componentName);
                try {

                    this.stopService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    this.startService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.setAction("");
                    this.startService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.putExtras(emptyBundle);
                    this.startService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.setData(ITA_Constants.SCHEMLESS_URI);
                    startService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == false && readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))
                        restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (componentType.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME)) {
                Intent i = new Intent();
                i.setClassName(packageName, componentName);
                try {
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.setAction("");
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.putExtras(emptyBundle);
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        return;
                    }

                    i.setData(ITA_Constants.SCHEMLESS_URI);
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == false && readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))
                        restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (componentType.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {

                if (!baseComponentType.isEmpty()) {
                    startComponent(packageName, componentName, baseComponentType, getApplicationContext());
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CREATE_COMPONENT);
                }
                
                Intent i = new Intent(action);
                try {
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        crashDueToCrashInDynamicBroadcastReceiver = true;
                        return;
                    }

                    i.putExtras(emptyBundle);
                    this.sendBroadcast(i);

                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        crashDueToCrashInDynamicBroadcastReceiver = true;
                        return;
                    }

                    i.setData(ITA_Constants.SCHEMLESS_URI);
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                    if (ITA_IntentService.foundCrashEternalThread == true) {
                        Log.w(TAG, "foundException - return");
                        crashDueToCrashInDynamicBroadcastReceiver = true;
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (componentType.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME)) {

                PackageManager pm = getPackageManager();
                ContentResolver cr = getContentResolver();

                PackageInfo providers = pm.getPackageInfo(packageName, PackageManager.GET_PROVIDERS);
                if (providers != null && providers.providers != null) {
                    for (ProviderInfo pi : providers.providers) {

                        // skip other providers except the target
                        if (!componentName.equals(pi.name)) {
                            continue;
                        }

                        String readPermission = pi.readPermission;
                        if (readPermission == null)
                            readPermission = "";
                        String writePermission = pi.writePermission;
                        if (writePermission == null)
                            writePermission = "";
                        boolean exported = pi.exported;

                        if (exported == false)
                            continue;

                        if (!readPermission.equals("") && !writePermission.equals("")) {
                            continue;
                        }

                        String authority = pi.authority;

                        String[] parts = authority.split(";");

                        if (parts == null)
                            continue;

                        for (int a = 0; a < parts.length; a++) {
                            authority = parts[a];
                            Uri baseUri = Uri.parse("content://" + authority);
                            accessProvider(baseUri, componentType, cr, authority, getApplicationContext(), packageName, componentName);
                        }
                    }
                }
            } else {
                // component type will be unavailable so try everything
                // occurs when a system crash doesn't get written to the
                // logs but was detected by a system failure
                Intent i = new Intent();

                if (actionOnly) {

                    try {
                        i.setAction(action);
                        this.sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.putExtras(emptyBundle);
                        this.sendBroadcast(i);

                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setData(ITA_Constants.SCHEMLESS_URI);
                        this.sendBroadcast(i);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    i.setClassName(packageName, componentName);
                    try {
                        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        this.startActivity(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setAction("");
                        this.startActivity(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.putExtras(emptyBundle);
                        this.startActivity(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setData(ITA_Constants.SCHEMLESS_URI);
                        this.startActivity(i);

                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        this.stopService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        this.startService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setAction("");
                        this.startService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.putExtras(emptyBundle);
                        this.startService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setData(ITA_Constants.SCHEMLESS_URI);
                        this.startService(i);

                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        this.sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setAction("");
                        this.sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.putExtras(emptyBundle);
                        this.sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

                        if (foundException == true) {
                            Log.w(TAG, "foundException - return");
                            foundException = false;
                            return;
                        }

                        i.setData(ITA_Constants.SCHEMLESS_URI);
                        sendBroadcast(i);

                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (ITA_IntentService.foundCrashEternalThread == false && readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))
                        restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] getAuthoritiesFromProvider(String packageName, String componentName, PackageManager pm) {

        PackageInfo providers = null;
        try {
            providers = pm.getPackageInfo(packageName, PackageManager.GET_PROVIDERS);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        if (providers != null && providers.providers != null) {
            for (ProviderInfo pi : providers.providers) {

                // skip other providers except the target
                if (!componentName.equals(pi.name)) {
                    continue;
                }

                String readPermission = pi.readPermission;
                if (readPermission == null)
                    readPermission = "";
                String writePermission = pi.writePermission;
                if (writePermission == null)
                    writePermission = "";
                boolean exported = pi.exported;

                if (exported == false)
                    continue;

                if (!readPermission.equals("") && !writePermission.equals("")) {
                    continue;
                }

                String authority = pi.authority;
                String[] parts = authority.split(";");
                return parts;
            }
        }
        return null;
    }
    
    static Uri getUriFromTableName(String tableName) {

        if (tableName == null)
            return null;

        if (tableName.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME))
            return ITA_Contract.ActivityTable.CONTENT_URI;
        else if (tableName.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME))
            return ITA_Contract.ServiceTable.CONTENT_URI;
        else if (tableName.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME))
            return ITA_Contract.ReceiverTable.CONTENT_URI;
        else if (tableName.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME))
            return ITA_Contract.BroadcastActionTable.CONTENT_URI;
        else if (tableName.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME))
            return ITA_Contract.ProviderTable.CONTENT_URI;
        else if (tableName.equals(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME))
            return ITA_Contract.AllComponentsTable.CONTENT_URI;
        else if (tableName.equals(ITA_Contract.AppsTable.APPS_TABLE_NAME))
            return ITA_Contract.AppsTable.CONTENT_URI;
        else
            return null;
    }
    
    // method to retest an intent to see if it will crash a process
    boolean retestAndVerifyIntent(String packageName, String componentName, String action, boolean actionOnly, String componentType, String baseComponentType) {

        ITA_IntentService.foundCrashEternalThread = false;
        crashDueToCrashInDynamicBroadcastReceiver = false;

        String currentComponentOrAction;
        if (actionOnly) {
            currentComponentOrAction = action;
        } else {
            currentComponentOrAction = packageName + "/" + componentName;
        }
        boolean shouldSkip = checkKillCountComponentOrAction(currentComponentOrAction, false, getUriFromTableName(componentType));
        if (shouldSkip == true)
            return false;
        
        if (action != null && action.isEmpty())
            createBaselineDynamicBroadcastReceiversForPackage(packageName, getApplicationContext());

        sendIntentRetest(packageName, componentName, action, componentType, actionOnly, baseComponentType);

        for (int a = 0; a < 2; a++) {
            try {
                Thread.sleep(ITA_Constants.MILLISECONDS_TO_LOG_RETEST_INTENT / (long) 2.0);
                if (ITA_IntentService.foundCrashEternalThread) {
                    Log.e(TAG, "breakout after sleeping for - " + ITA_Constants.MILLISECONDS_TO_LOG_RETEST_INTENT / (long) 2.0);
                    break;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ITA_IntentService.foundCrashEternalThread;
    }

    // recursively obtain all files in a directory
    static void getAllFilesOfDir(File directory, HashSet<String> filesHashSet) {
        final File[] files = directory.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            if (file == null)
                continue;
            if (file.isDirectory())
                getAllFilesOfDir(file, filesHashSet);
            else {
                String absolutePath = file.getAbsolutePath();
                if (absolutePath == null)
                    continue;
                if (!absolutePath.startsWith(ITA_Constants.outputDir))
                    filesHashSet.add(file.getAbsolutePath());
            }
        }
    }

    static void getSystemProperties(HashSet<String> systemProperties) {

        String[] command = {"getprop"};
        ArrayList<String> getPropOutput = execCommandGetOutput(command);
        if (getPropOutput == null || getPropOutput.isEmpty())
            return;

        for (String prop : getPropOutput) {
            boolean foundPropertyToIgnore = false;
            for (String s : ITA_Constants.SYSTEM_PROPERTIES_TO_IGNORE) {
                if (prop.startsWith(s)) {
                    foundPropertyToIgnore = true;
                    break;
                }
            }
            if (!foundPropertyToIgnore)
                systemProperties.add(prop);
        }
    }

    @SuppressLint("NewApi")
    static void getSettings(ContentResolver cr, HashSet<String> settingsHashSet) {

        Uri[] urisToQuery = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            urisToQuery = new Uri[3];
            urisToQuery[0] = Settings.Secure.CONTENT_URI;
            urisToQuery[1] = Settings.System.CONTENT_URI;
            urisToQuery[2] = Settings.Global.CONTENT_URI;
        } else {
            urisToQuery = new Uri[2];
            urisToQuery[0] = Settings.Secure.CONTENT_URI;
            urisToQuery[1] = Settings.System.CONTENT_URI;
        }

        Cursor cursor = null;
        String table = null;
        String name = null;
        String value = null;

        for (int a = 0; a < urisToQuery.length; a++) {
            try {
                table = urisToQuery[a].toString();
                cursor = cr.query(urisToQuery[a], null, null, null, null);
                boolean moveToFirst = cursor.moveToFirst();
                if (moveToFirst) {
                    do {
                        name = cursor.getString(cursor.getColumnIndex(Settings.NameValueTable.NAME));
                        value = cursor.getString(cursor.getColumnIndex(Settings.NameValueTable.VALUE));
                        settingsHashSet.add(table + "|" + name + "|" + value);
                    } while (cursor != null && cursor.moveToNext());
                } else
                    Log.w(TAG, "[" + table + "] no entries for this table");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void createBaselineDynamicBroadcastReceiversForPackage(String packageName, Context context) {

        if (currentPackageNameDynamicBroadcastReceiver == null) {
            String lastPackageTest = readStringSharedPreferences(context, ITA_Constants.CURRENT_PACKAGE_NAME_DYNAMIC_RECEIVER_TEST);
            if (lastPackageTest == null || lastPackageTest.isEmpty())
                writeStringSharedPreferences(context, ITA_Constants.CURRENT_PACKAGE_NAME_DYNAMIC_RECEIVER_TEST, packageName);

            currentPackageNameDynamicBroadcastReceiver = packageName;
            baselinePackageDynamicBroadcastReceivers.clear();

            if (packageName.equals(lastPackageTest)) {
                testedDynamicBroadcastReceiversForPackage = readHashSetSharedPreferences(context, ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP);
                Log.w(TAG, "Restored tested broadcast actions for app - " + packageName + " - size of " + testedDynamicBroadcastReceiversForPackage.size());
            } else {
                isFirstComponentOfAnAppDynBR = true;
                testedDynamicBroadcastReceiversForPackage.clear();
                writeHashSetSharedPreferences(context, ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP, null);
            }
            parseOutBroadcastReceiversForApp(packageName, baselinePackageDynamicBroadcastReceivers);
            Log.w(TAG, "Created baseline for dynamically-registered broadcast receivers for " + packageName + " with a size of " + baselinePackageDynamicBroadcastReceivers.size());
        } else {
            if (!currentPackageNameDynamicBroadcastReceiver.equals(packageName)) {
                isFirstComponentOfAnAppDynBR = true;
                currentPackageNameDynamicBroadcastReceiver = packageName;
                baselinePackageDynamicBroadcastReceivers.clear();
                testedDynamicBroadcastReceiversForPackage.clear();
                writeStringSharedPreferences(context, ITA_Constants.CURRENT_PACKAGE_NAME_DYNAMIC_RECEIVER_TEST, packageName);
                writeHashSetSharedPreferences(context, ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP, null);
                parseOutBroadcastReceiversForApp(packageName, baselinePackageDynamicBroadcastReceivers);
                Log.w(TAG, "Created baseline for dynamically-registered broadcast receivers for " + packageName + " with a size of " + baselinePackageDynamicBroadcastReceivers.size());
            } else {
                isFirstComponentOfAnAppDynBR = false;
            }
        }
    }

    void sendIntent(String packageName, String componentName, String action, boolean isBroadcastActionIntent, String table, int id, int totalCount, boolean checkSettingsAndProperties, Context context) {
        // if the use has hit stop analysis, then exit
        if (keepGoing == false)
            return;

        // reset this variable to false
        ITA_IntentService.foundCrashEternalThread = false;

        // if it is a broadcast Intent action that should not be sent or has already been sent, skip it
        if (isBroadcastActionIntent == true && troublesomeBroadcastActions.contains(action)) {
            Log.w(TAG, "Skipping troublesome broadcast action - " + action);
            return;
        } else { // check if it is a troublesome application component
            if (troublesomeApplicationComponents.contains(packageName + "/" + componentName)) {
                Log.w(TAG, "Skipping troublesome application component - " + packageName + "/" + componentName);
                return;
            }
        }

        // variable to tell if an exception was encountered when sending the intent
        boolean failed = false;

        boolean checkPossiblePrivilegeEscalation = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.CHECK_FOR_PRIVILEGE_ESCALTION);
        if (checkPossiblePrivilegeEscalation) {

            if (EXTERNAL_STORAGE_FILES_HASHSET_AFTER.isEmpty()) {
                if (!EXTERNAL_STORAGE_FILES_HASHSET_BEFORE.isEmpty())
                    EXTERNAL_STORAGE_FILES_HASHSET_BEFORE.clear();
                getAllFilesOfDir(EXTERNAL_STORAGE_DIR, EXTERNAL_STORAGE_FILES_HASHSET_BEFORE);
            } else {
                EXTERNAL_STORAGE_FILES_HASHSET_BEFORE = EXTERNAL_STORAGE_FILES_HASHSET_AFTER;
                EXTERNAL_STORAGE_FILES_HASHSET_AFTER = new HashSet<String>();
            }

            if (checkSettingsAndProperties) {
                if (SETTINGS_AFTER.isEmpty()) {
                    if (!SETTINGS_BEFORE.isEmpty())
                        SETTINGS_BEFORE.clear();
                    getSettings(getContentResolver(), SETTINGS_BEFORE);
                } else {
                    SETTINGS_BEFORE = SETTINGS_AFTER;
                    SETTINGS_AFTER = new HashSet<String>();
                }

                if (SYSTEM_PROPERTIES_AFTER.isEmpty()) {
                    if (SYSTEM_PROPERTIES_BEFORE.isEmpty()) {
                        SYSTEM_PROPERTIES_BEFORE.clear();
                    }
                    getSystemProperties(SYSTEM_PROPERTIES_BEFORE);
                } else {
                    SYSTEM_PROPERTIES_BEFORE = SYSTEM_PROPERTIES_AFTER;
                    SYSTEM_PROPERTIES_AFTER = new HashSet<String>();
                }
            }
        }

        // get a baseline for a package's dynamically-registered broadcast receivers
        if (!isBroadcastActionIntent && readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING)) {
            createBaselineDynamicBroadcastReceiversForPackage(packageName, getApplicationContext());
        }

        try {
            if (isBroadcastActionIntent) {
                // send an broadcast intent only using the action

                writeStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT, action);

                Log.e(TAG, "[" + id + "/" + totalCount + "] Sending " + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + " - action=" + action);

                Intent i = new Intent(action);
                sendBroadcast(i);
                Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                if (ITA_IntentService.foundCrashEternalThread == false) {
                    i = new Intent(action);
                    Bundle bundle = new Bundle();
                    i.putExtras(bundle);
                    sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                    if (ITA_IntentService.foundCrashEternalThread == false) {
                        i.setData(ITA_Constants.SCHEMLESS_URI);
                        sendBroadcast(i);
                    }
                } else
                    Log.w(TAG, "Found crash - stopping testing of action=" + action);
            } else {
                // is to an application component directly
                writeStringSharedPreferences(getApplicationContext(), ITA_Constants.LAST_INTENT_SENT, packageName + "/" + componentName);

                Intent i = new Intent();
                i.setClassName(packageName, componentName);

                if (table.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                    this.startActivity(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                    if (ITA_IntentService.foundCrashEternalThread == false) {
                        i.setAction("");
                        this.startActivity(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        if (ITA_IntentService.foundCrashEternalThread == false) {
                            i.putExtras(emptyBundle);
                            this.startActivity(i);
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                            if (ITA_IntentService.foundCrashEternalThread == false) {
                                i.setData(ITA_Constants.SCHEMLESS_URI);
                                this.startActivity(i);
                            }
                        } else
                            Log.w(TAG, "Found crash - stopping testing of packageName=" + packageName + ", componentName=" + componentName);
                    } else
                        Log.w(TAG, "Found crash - stopping testing of packageName=" + packageName + ", componentName=" + componentName);
                } else if (table.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
                    Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ServiceTable.SERVICE_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                    this.stopService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);

                    this.startService(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);

                    if (ITA_IntentService.foundCrashEternalThread == false) {
                        i.setAction("");
                        this.startService(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        if (ITA_IntentService.foundCrashEternalThread == false) {
                            i.putExtras(emptyBundle);
                            this.startService(i);
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                            if (ITA_IntentService.foundCrashEternalThread == false) {
                                i.setData(ITA_Constants.SCHEMLESS_URI);
                                this.startService(i);
                            }
                        } else
                            Log.w(TAG, "Found crash - stopping testing of packageName=" + packageName + ", componentName=" + componentName);
                    } else
                        Log.w(TAG, "Found crash - stopping testing of packageName=" + packageName + ", componentName=" + componentName);
                } else if (table.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME)) {
                    Log.e(TAG, "[" + id + "/" + totalCount + "] Starting " + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + " - packageName=" + packageName + ", componentName=" + componentName);
                    this.sendBroadcast(i);
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                    if (ITA_IntentService.foundCrashEternalThread == false) {
                        i.setAction("");
                        this.sendBroadcast(i);
                        Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                        if (ITA_IntentService.foundCrashEternalThread == false) {
                            i.putExtras(emptyBundle);
                            this.sendBroadcast(i);
                            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT);
                            if (ITA_IntentService.foundCrashEternalThread == false) {
                                i.setData(ITA_Constants.SCHEMLESS_URI);
                                this.sendBroadcast(i);
                            }
                        } else
                            Log.w(TAG, "Found crash - stopping testing of packageName=" + packageName + ", componentName=" + componentName);
                    } else
                        Log.w(TAG, "Found crash - stopping testing of packageName=" + packageName + ", componentName=" + componentName);
                } else {
                    Log.e(TAG, "Table [" + table + "] not found and row=" + id);
                }
            }

            // a crash occurred, so provide some time to
            if (ITA_IntentService.foundSystemCrashEternalThread) {
                Log.w(TAG, "System Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (ITA_IntentService.foundCrashEternalThread) {
                Log.w(TAG, "Crash detected - sleeping for " + ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_NORMAL + " milliseconds");
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_NORMAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            failed = true;
            String message = e.getMessage();

            if (isBroadcastActionIntent) {
                Log.e(TAG, "Failure sending " + action + " - " + message);
                troublesomeBroadcastActions.add(action);
            } else
                Log.e(TAG, "Failure sending " + packageName + "/" + componentName + " - " + message);

            // system is dying at this point
            if (message.contains("Failure from system")) {

                Log.w(TAG, "Failure from system - sleep and wait for soft reboot");

                // record that the current
                if (isBroadcastActionIntent)
                    writePotentialSystemFailureIntent(table + "|" + action);
                else
                    writePotentialSystemFailureIntent(table + "|" + packageName + "|" + componentName);

                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } else {
                // if we are just sending the action and it fails then record the action
                // in a file
                if (ITA_Constants.WRITE_FAILED_BROADCAST_INTENTS_TO_FILE && isBroadcastActionIntent)
                    writeDisallowedBroadcast(action);
            }

            try {
                Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_FAILED_INTENT);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        // will only be entered if we did not encounter some sort of exception
        // when sending the Intent
        if (failed == false) {

            long startMillis = System.currentTimeMillis();

            boolean keepLogging = true;

            // keep logging until the threshold is reached
            while (keepLogging) {
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_WAIT_LOOP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                long currentMillis = System.currentTimeMillis();
                long diff = currentMillis - startMillis;
                if (diff >= ITA_Constants.MILLISECONDS_TO_LOG) {
                    keepLogging = false;
                    break;
                }
            }
        }

        if (foundSystemCrashEternalThread == true) {
            Log.e(TAG, "Encountered System Crash - sleeping for " + ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT + " milliseconds");
            try {
                Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_SOFT_REBOOT);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            foundSystemCrashEternalThread = false;
        }

        if (!isBroadcastActionIntent && failed == false && ITA_IntentService.foundCrashEternalThread == false && readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING)) {
            ArrayList<String> broadsToTest = new ArrayList<String>();
            parseOutBroadcastReceiversForApp(packageName, broadsToTest);
            if (broadsToTest != null && broadsToTest.size() != 0) {
                if (isFirstComponentOfAnAppDynBR) {
                    Log.w(TAG, "writing all broadcast actions for [" + packageName + "/" + componentName + "]");

                    StringBuilder sb = new StringBuilder();
                    for (int a = 0; a < broadsToTest.size(); a++) {
                        sb.append(broadsToTest.get(a) + "|");
                    }

                    String actionsString = sb.toString();

                    if (!actionsString.isEmpty()) {
                        if (actionsString.endsWith("|"))
                            actionsString = actionsString.substring(0, actionsString.length() - 1);
                        updateComponentsDynamicBroadcastReceiverActions(packageName, componentName, table, action, actionsString);
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int a = 0; a < broadsToTest.size(); a++) {
                        String broad = broadsToTest.get(a);
                        if (!baselinePackageDynamicBroadcastReceivers.contains(broad)) {
                            baselinePackageDynamicBroadcastReceivers.add(broad);
                            sb.append(broadsToTest.get(a) + "|");
                        }
                    }

                    String actionsString = sb.toString();
                    if (!actionsString.isEmpty()) {
                        if (actionsString.endsWith("|"))
                            actionsString = actionsString.substring(0, actionsString.length() - 1);
                        updateComponentsDynamicBroadcastReceiverActions(packageName, componentName, table, action, actionsString);
                    }
                }

                sendDynamicallyRegisteredBroadcastActions(broadsToTest, packageName, componentName, this.getApplicationContext(), table);
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (checkPossiblePrivilegeEscalation) {
            getAllFilesOfDir(EXTERNAL_STORAGE_DIR, EXTERNAL_STORAGE_FILES_HASHSET_AFTER);
            if (checkSettingsAndProperties) {
                getSettings(getContentResolver(), SETTINGS_AFTER);
                getSystemProperties(SYSTEM_PROPERTIES_AFTER);
            }
            compareHashsetsForChanges(action, packageName, componentName, table, getApplicationContext(), checkSettingsAndProperties);
        }
    }
    
    void updateSkippedDynamicBroadcastReceiverActions(String packageName, String componentName, Uri table, String action) {

        Log.w(TAG, "updateSkippedDynamicBroadcastReceiverActions - action=" + action);

        Uri target = null;
        String selection = null;
        String[] selectionArgs = null;
        ContentResolver cr = getContentResolver();
        String currentSkippedBroadcastActions = null;

        if (table.equals(ITA_Contract.ActivityTable.CONTENT_URI)) {
            target = ITA_Contract.ActivityTable.CONTENT_URI;
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else if (table.equals(ITA_Contract.ServiceTable.CONTENT_URI)) {
            target = ITA_Contract.ServiceTable.CONTENT_URI;
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else if (table.equals(ITA_Contract.ReceiverTable.CONTENT_URI)) {
            target = ITA_Contract.ReceiverTable.CONTENT_URI;
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else if (table.equals(ITA_Contract.BroadcastActionTable.CONTENT_URI)) {
            target = ITA_Contract.BroadcastActionTable.CONTENT_URI;
            selection = ITA_Contract.ACTION + " = ?";
            selectionArgs = new String[1];
            selectionArgs[0] = action;
        } else {
            return;
        }

        Cursor cursor = cr.query(target, null, null, null, null);
        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst == false)
                Log.w(TAG, "[" + ITA_Contract.TimeTable.TIME_TABLE_NAME + "] no entries for this table");
            else {
                currentSkippedBroadcastActions = cursor.getString(cursor.getColumnIndex(ITA_Contract.SKIPPED_DYNAMIC_BROADCAST_ACTIONS));
            }
            cursor.close();
        }

        if (currentSkippedBroadcastActions != null) {
            currentSkippedBroadcastActions = currentSkippedBroadcastActions + "|" + action;
        }

        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.SKIPPED_DYNAMIC_BROADCAST_ACTIONS, action);
        cr.update(target, cv, selection, selectionArgs);
    }

    void updateComponentsDynamicBroadcastReceiverActions(String packageName, String componentName, String table, String action, String actions) {
        Uri target = null;
        String selection = null;
        String[] selectionArgs = null;

        Log.w(TAG, "update dynnamic receiver actions - [" + packageName + "/" + componentName + "] actions=" + actions);

        if (table.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {
            target = ITA_Contract.ActivityTable.CONTENT_URI;
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else if (table.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
            target = ITA_Contract.ServiceTable.CONTENT_URI;
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else if (table.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME)) {
            target = ITA_Contract.ReceiverTable.CONTENT_URI;
            selection = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
            selectionArgs = new String[2];
            selectionArgs[0] = packageName;
            selectionArgs[1] = componentName;
        } else if (table.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
            target = ITA_Contract.BroadcastActionTable.CONTENT_URI;
            selection = ITA_Contract.ACTION + " = ?";
            selectionArgs = new String[1];
            selectionArgs[0] = action;
        } else {
            return;
        }

        ContentResolver cr = getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(ITA_Contract.DYNAMIC_BROADCAST_RECEIVERS_REGISTERED, actions);
        cr.update(target, cv, selection, selectionArgs);
    }

    static void sendDynamicallyRegisteredBroadcastActions(ArrayList<String> broads, String packageName, String componentName, Context context, String componentType) {
        if (broads == null)
            return;

        for (String action : broads) {

            // check to see if it is in the baseline
            if (packageName.equals("android") && baselinePackageDynamicBroadcastReceivers.contains(action)) {
                continue;
            }

            // this prevents a component in the same package from testing the same dynamic receivers
            // as were tested in a previous component in the same package. it adds overhead that
            // is probably unnecessary
            if (testedDynamicBroadcastReceiversForPackage.contains(action))
                continue;

            // skip broadcasts that we should not send or do not have the privileges for
            if (troublesomeBroadcastActions.contains(action))
                continue;
            Log.e(TAG, "[dynamic broadcast receiver] packageName=" + packageName + ", componentName=" + componentName + ", componentType=" + componentType + ", action=" + action);

            writeStringSharedPreferences(context, ITA_Constants.CURRENT_ACTIION_DYNAMIC_RECEIVER_TEST, action);

            boolean exceptionHappened = false;

            try {
                Intent i = new Intent(action);
                context.sendBroadcast(i);
                if (ITA_IntentService.foundCrashEternalThread == false) {
                    i = new Intent(action);
                    Bundle bundle = new Bundle();
                    i.putExtras(bundle);
                    context.sendBroadcast(i);
                    if (ITA_IntentService.foundCrashEternalThread == false) {
                        i.setData(ITA_Constants.SCHEMLESS_URI);
                        context.sendBroadcast(i);
                    }
                }
            } catch (Exception e) {

                // sending the intent failed, so add it to the list
                troublesomeBroadcastActions.add(action);
                Log.e(TAG, "[added to troublesome actions] Failure sending " + action + " - " + e.getMessage());
                exceptionHappened = true;

                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_AFTER_FAILED_INTENT);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            if (exceptionHappened == false) {
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_SENDING_DYNAMICALLY_REGISTERD_BROADCAST_RECEIVERS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            writeStringSharedPreferences(context, ITA_Constants.CURRENT_ACTIION_DYNAMIC_RECEIVER_TEST, null);

            // record that it was tested already
            testedDynamicBroadcastReceiversForPackage.add(action);

            // record the tested ones
            writeHashSetSharedPreferences(context, ITA_Constants.TESTED_DYNAMIC_BROADCAST_ACTIONS_FOR_APP, testedDynamicBroadcastReceiversForPackage);


            if (ITA_IntentService.foundCrashEternalThread == true) {
                ITA_IntentService.foundCrashEternalThread = false;
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_PROCESS_CRASH_NORMAL);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                startComponent(packageName, componentName, componentType, context);
                try {
                    Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CREATE_COMPONENT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.w(TAG, "[dynamic broadcast receiver] restarted component [" + packageName + "/" + componentName + "] after crash of type=" + componentType);
            }

            if (keepGoing == false)
                return;
        }
    }

    public static boolean startComponent(String packageName, String componentName, String componentType, Context context) {
        Intent i = new Intent();
        i.setClassName(packageName, componentName);
        try {
            if (componentType.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(i);
            } else if (componentType.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
                context.startService(i);
            } else if (componentType.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
                context.sendBroadcast(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static String findChangedSettingsBefore(String beforeSetting) {
        String target = beforeSetting.substring(0, beforeSetting.lastIndexOf('|') + 1);
        for (String afterSetting : SETTINGS_AFTER) {
            if (afterSetting.startsWith(target)) {
                return afterSetting;
            }
        }
        return null;
    }

    static String findChangedSettingsAfter(String afterSetting) {
        String target = afterSetting.substring(0, afterSetting.lastIndexOf('|') + 1);
        for (String beforeSetting : SETTINGS_BEFORE) {
            if (beforeSetting.startsWith(target)) {
                return beforeSetting;
            }
        }
        return null;
    }

    static String findChangedPropertiesBefore(String beforeProperty) {
        String target = beforeProperty.substring(0, beforeProperty.indexOf(':'));
        target = target.trim();
        for (String afterSetting : SYSTEM_PROPERTIES_AFTER) {
            if (afterSetting.startsWith(target)) {
                return afterSetting;
            }
        }
        return null;
    }

    static String findChangedPropertiesAfter(String afterProperty) {
        String target = afterProperty.substring(0, afterProperty.indexOf(':'));
        target = target.trim();
        for (String beforeSetting : SYSTEM_PROPERTIES_BEFORE) {
            if (beforeSetting.startsWith(target)) {
                return beforeSetting;
            }
        }
        return null;
    }

    static void compareHashsetsForChanges(String action, String packageName, String componentName, String componentType, Context context, boolean checkSettingsAndProperties) {
        ContentResolver cr = context.getContentResolver();
        if (EXTERNAL_STORAGE_FILES_HASHSET_BEFORE.size() != EXTERNAL_STORAGE_FILES_HASHSET_AFTER.size()) {
            Log.w(TAG, "[external storage files] Sizes are different - before=" + EXTERNAL_STORAGE_FILES_HASHSET_BEFORE.size() + ", after=" + EXTERNAL_STORAGE_FILES_HASHSET_AFTER.size());
        }

        for (String afterFile : EXTERNAL_STORAGE_FILES_HASHSET_AFTER) {
            if (!EXTERNAL_STORAGE_FILES_HASHSET_BEFORE.contains(afterFile)) {
                Log.w(TAG, "File present after Intent but not before - " + afterFile);
                File file = new File(afterFile);
                ContentValues cv = new ContentValues();
                cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                cv.put(ITA_Contract.ACTION, action);
                cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
                cv.put(ITA_Contract.NEW_FILE_AFTER_INTENT, afterFile);
                cv.put(ITA_Contract.FILE_SIZE, file.length());
                cr.insert(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, cv);
            }
        }

        if (checkSettingsAndProperties) {
            ArrayList<String> changedSettings = new ArrayList<String>();

            for (String beforeSetting : SETTINGS_BEFORE) {
                if (!SETTINGS_AFTER.contains(beforeSetting)) {
                    String[] beforeParts = beforeSetting.split("\\|");
                    if (beforeParts == null || beforeParts.length == 0)
                        continue;
                    String afterSetting = findChangedSettingsBefore(beforeSetting);
                    ContentValues cv = new ContentValues();
                    cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                    cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                    cv.put(ITA_Contract.ACTION, action);
                    cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
                    cv.put(ITA_Contract.SETTINGS_TABLE, beforeParts[0]);
                    cv.put(ITA_Contract.BEFORE_SETTINGS_KEY, beforeParts[1]);
                    if (beforeParts.length > 2)
                        cv.put(ITA_Contract.BEFORE_SETTINGS_VALUE, beforeParts[2]);
                    else
                        cv.put(ITA_Contract.BEFORE_SETTINGS_VALUE, "null");
                    if (afterSetting != null) {
                        Log.w(TAG, "Settings change after Intent [before]=" + beforeSetting + ", [after]=" + afterSetting);
                        changedSettings.add(afterSetting);
                        cv.put(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE, ITA_Constants.SETTINGS_CHANGE);
                        String[] afterParts = afterSetting.split("\\|");
                        cv.put(ITA_Contract.AFTER_SETTINGS_KEY, afterParts[1]);
                        if (afterParts.length > 2)
                            cv.put(ITA_Contract.AFTER_SETTINGS_VALUE, afterParts[2]);
                        else
                            cv.put(ITA_Contract.AFTER_SETTINGS_VALUE, "null");
                    } else {
                        Log.w(TAG, "Settings removed after Intent [before]=" + beforeSetting + ", [after]=" + afterSetting);
                        cv.put(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE, ITA_Constants.SETTINGS_REMOVED);
                        cv.put(ITA_Contract.AFTER_SETTINGS_KEY, ITA_Constants.DOES_NOT_EXIST);
                        cv.put(ITA_Contract.AFTER_SETTINGS_VALUE, ITA_Constants.DOES_NOT_EXIST);
                    }
                    cr.insert(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, cv);
                }
            }

            for (String afterSetting : SETTINGS_AFTER) {
                if (!SETTINGS_BEFORE.contains(afterSetting)) {
                    String[] afterParts = afterSetting.split("\\|");
                    if (afterParts == null || afterParts.length == 0)
                        continue;
                    if (changedSettings.contains(afterSetting)) {
                        Log.w(TAG, "already recorded changed setting - " + afterSetting);
                        continue;
                    }
                    String beforeSetting = findChangedSettingsAfter(afterSetting);
                    if (beforeSetting == null) {
                        Log.w(TAG, "Settings added after Intent [before]=" + beforeSetting + ", [after]=" + afterSetting);
                        ContentValues cv = new ContentValues();
                        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                        cv.put(ITA_Contract.ACTION, action);
                        cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
                        cv.put(ITA_Contract.SETTINGS_TABLE, afterParts[0]);
                        cv.put(ITA_Contract.AFTER_SETTINGS_KEY, afterParts[1]);
                        if (afterParts.length > 2)
                            cv.put(ITA_Contract.AFTER_SETTINGS_VALUE, afterParts[2]);
                        else
                            cv.put(ITA_Contract.AFTER_SETTINGS_VALUE, "null");
                        cv.put(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE, ITA_Constants.SETTINGS_ADDED);
                        cv.put(ITA_Contract.BEFORE_SETTINGS_KEY, ITA_Constants.DID_NOT_EXIST);
                        cv.put(ITA_Contract.BEFORE_SETTINGS_VALUE, ITA_Constants.DID_NOT_EXIST);
                        cr.insert(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, cv);
                    }
                }
            }

            ArrayList<String> changedProperties = new ArrayList<String>();
            for (String beforeProperty : SYSTEM_PROPERTIES_BEFORE) {
                if (!SYSTEM_PROPERTIES_AFTER.contains(beforeProperty)) {
                    String[] beforeParts = beforeProperty.split("\\: ");
                    if (beforeParts == null || beforeParts.length == 1) {
                        beforeParts = beforeProperty.split("\\:");
                        if (beforeParts == null || beforeParts.length == 1) {
                            continue;
                        }
                    }
                    String afterProperty = findChangedPropertiesBefore(beforeProperty);
                    ContentValues cv = new ContentValues();
                    cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                    cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                    cv.put(ITA_Contract.ACTION, action);
                    cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
                    cv.put(ITA_Contract.BEFORE_PROPERTIES_KEY, beforeParts[0]);
                    if (beforeParts.length > 1)
                        cv.put(ITA_Contract.BEFORE_PROPERTIES_VALUE, beforeParts[1]);
                    else
                        cv.put(ITA_Contract.BEFORE_PROPERTIES_VALUE, "null");
                    if (afterProperty != null) {
                        Log.w(TAG, "Property change after Intent [before]=" + beforeProperty + ", [after]=" + afterProperty);
                        changedProperties.add(afterProperty);
                        cv.put(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE, ITA_Constants.PROPERTIES_CHANGE);
                        String[] afterParts = afterProperty.split("\\: ");
                        if (afterParts == null || afterParts.length == 1) {
                            afterParts = afterProperty.split("\\:");
                        }
                        cv.put(ITA_Contract.AFTER_PROPERTIES_KEY, afterParts[0]);
                        if (afterParts.length > 1)
                            cv.put(ITA_Contract.AFTER_PROPERTIES_VALUE, afterParts[1]);
                        else
                            cv.put(ITA_Contract.AFTER_PROPERTIES_VALUE, "null");
                    } else {
                        Log.w(TAG, "Properties removed after Intent [before]=" + beforeProperty + ", [after]=" + afterProperty);
                        cv.put(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE, ITA_Constants.PROPERTIES_REMOVED);
                        cv.put(ITA_Contract.AFTER_PROPERTIES_KEY, ITA_Constants.DOES_NOT_EXIST);
                        cv.put(ITA_Contract.AFTER_PROPERTIES_VALUE, ITA_Constants.DOES_NOT_EXIST);
                    }
                    cr.insert(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, cv);
                }
            }

            for (String afterProperty : SYSTEM_PROPERTIES_AFTER) {
                if (!SYSTEM_PROPERTIES_BEFORE.contains(afterProperty)) {
                    String[] afterParts = afterProperty.split("\\: ");
                    if (afterParts == null || afterParts.length == 1) {
                        afterParts = afterProperty.split("\\:");
                        if (afterParts == null || afterParts.length == 1)
                            continue;
                    }
                    if (changedProperties.contains(afterProperty)) {
                        Log.w(TAG, "already recorded changed propery - " + afterProperty);
                        continue;
                    }
                    String beforeProperty = findChangedPropertiesAfter(afterProperty);
                    if (beforeProperty == null) {
                        Log.w(TAG, "Property added after Intent [before]=" + beforeProperty + ", [after]=" + afterProperty);
                        ContentValues cv = new ContentValues();
                        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                        cv.put(ITA_Contract.ACTION, action);
                        cv.put(ITA_Contract.COMPONENT_TYPE, componentType);
                        cv.put(ITA_Contract.AFTER_PROPERTIES_KEY, afterParts[0]);
                        if (afterParts.length > 1)
                            cv.put(ITA_Contract.AFTER_PROPERTIES_VALUE, afterParts[1]);
                        else
                            cv.put(ITA_Contract.AFTER_PROPERTIES_VALUE, "null");
                        cv.put(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE, ITA_Constants.PROPERTIES_ADDED);
                        cv.put(ITA_Contract.BEFORE_PROPERTIES_KEY, ITA_Constants.DID_NOT_EXIST);
                        cv.put(ITA_Contract.BEFORE_PROPERTIES_VALUE, ITA_Constants.DID_NOT_EXIST);
                        cr.insert(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, cv);
                    }
                }
            }
        }
    }

    // this will write an the action for a broadcast intent to a special file when a
    // failure from the system is encountered when sending a broadcast intent using
    // just the action
    void writeSystemFailureIntent(String actionOrComponent) {
        PrintWriter out = null;
        try {
            FileWriter fw = new FileWriter(currentDir + "/" + ITA_Constants.SYSTEM_FAILURE_INTENTS_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            out.println(actionOrComponent);
            out.flush();
        } catch (IOException e) {
            out = null;
        } finally {
            if (out != null)
                out.close();
        }
    }

    // this will write an the action for a broadcast intent to a special file when a
    // failure from the system is encountered when sending a broadcast intent using
    // just the action
    void writePotentialSystemFailureIntent(String actionOrComponent) {
        PrintWriter out = null;
        try {
            FileWriter fw = new FileWriter(currentDir + "/" + ITA_Constants.POTENTIAL_SYSTEM_FAILURE_INTENTS_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            out.println(actionOrComponent);
        } catch (IOException e) {
            out = null;
        } finally {
            if (out != null)
                out.close();
        }
    }

    // this will write an the package name and component name from intent to a
    // special file when a failure from the system is encountered when sending
    // a broadcast intent using just the action
    void writeDisallowedBroadcast(String action) {
        PrintWriter out = null;
        try {
            FileWriter fw = new FileWriter(currentDir + "/" + ITA_Constants.DISALLOWED_BROADCASTS_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);
            out.println(action);
        } catch (IOException e) {
            out = null;
        } finally {
            if (out != null)
                out.close();
        }
    }

    // the init method will create directories on the sd card to store the
    // files and create a specific dire
    private void init() {
        File outputPath = new File(ITA_Constants.outputDir);
        if (!outputPath.exists()) {
            outputPath.mkdir();
        }
        createSpecificRunDirectory();
    }

    // this method will create a directory on the sd card for the
    // specific analysis run
    public void createSpecificRunDirectory() {
        String[] command = {"getprop", "ro.product.name"};
        ArrayList<String> productData = execCommandGetOutput(command);
        long timestamp = System.currentTimeMillis();
        if (productData.size() != 0) {
            String device = productData.get(0).replace(" ", "_");
            File dir = new File(ITA_Constants.outputDir + "/" + device);
            if (!dir.exists())
                dir.mkdir();
            currentDir = ITA_Constants.outputDir + "/" + device + "/" + device + "_" + timestamp;
            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR, currentDir);
        } else {
            File dir = new File(ITA_Constants.outputDir + "/" + ITA_Constants.UNKNOWN_DEVICE);
            if (!dir.exists())
                dir.mkdir();
            currentDir = ITA_Constants.outputDir + "/" + ITA_Constants.UNKNOWN_DEVICE + "/" + ITA_Constants.UNKNOWN_DEVICE + "_" + timestamp;
            writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_DIR, currentDir);
        }

        File runDir = new File(currentDir);
        if (!runDir.exists())
            runDir.mkdir();

        currentLogDir = currentDir + "/" + ITA_Constants.LOGS;

        writeStringSharedPreferences(getApplicationContext(), ITA_Constants.CURRENT_LOG_DIR, currentLogDir);

        File logDir = new File(currentLogDir);
        if (!logDir.exists())
            logDir.mkdir();

        File providerDir = new File(currentLogDir, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME);
        if (!providerDir.exists())
            providerDir.mkdir();

        File activityDir = new File(currentLogDir, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME);
        if (!activityDir.exists())
            activityDir.mkdir();

        File serviceDir = new File(currentLogDir, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME);
        if (!serviceDir.exists())
            serviceDir.mkdir();

        File receiverDir = new File(currentLogDir, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME);
        if (!receiverDir.exists())
            receiverDir.mkdir();

        File broadcastActionDir = new File(currentLogDir, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME);
        if (!broadcastActionDir.exists())
            broadcastActionDir.mkdir();
    }

    // method to launch a thread that will send all the intents it can find
    // from apps and the system on the device
    public void launchIntentBarrage() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ITA_IntentService.this.intentBarrage();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    // this method will just return to the home screen by
    // sending an intent
    public void returnToHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // method that will send all the intents it can find
    // where the sending of each type of intent will be
    // launched in its own thread
    public void intentBarrage() {

        // delete the rows from the tables
        ContentResolver cr = this.getContentResolver();
        cr.delete(ITA_Contract.ActivityTable.CONTENT_URI, null, null);
        cr.delete(ITA_Contract.ServiceTable.CONTENT_URI, null, null);
        cr.delete(ITA_Contract.ReceiverTable.CONTENT_URI, null, null);
        cr.delete(ITA_Contract.BroadcastActionTable.CONTENT_URI, null, null);

        Log.w(TAG, "intentBarrage");
        sendToastToMainActivity("Gathering Intents. Please wait...");

        returnToHomeScreen();
        writeActivitiesToDB();
        writeServicesToDB();
        writeReceiversToDB();
        obtainActiveBroadcastReceiverActions();
        Log.w(TAG, "start sending all intents found!");
        sendToastToMainActivity("Sending Intents...");
        launchSendAllActivitiesBarrage();
        launchSendAllServicesBarrage();
        launchSendAllReceiversBarrage();
        launchSendAllBroadcastActionsBarrage();
    }

    public void launchSendAllBroadcastActionsBarrage() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ITA_IntentService.this.sendAllBroadcastActionsBarrage();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    public void launchSendAllServicesBarrage() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ITA_IntentService.this.sendAllServicesBarrage();
            }

        };
        Thread t = new Thread(r);
        t.start();
    }

    public void launchSendAllReceiversBarrage() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ITA_IntentService.this.sendAllReceiversBarrage();
            }

        };
        Thread t = new Thread(r);
        t.start();
    }

    public void launchSendAllActivitiesBarrage() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ITA_IntentService.this.sendAllActivitiesBarrage();
            }

        };
        Thread t = new Thread(r);
        t.start();
    }

    public void sendAllBroadcastActionsBarrage() {
        ContentResolver cr = this.getContentResolver();
        Uri activity = ITA_Contract.BroadcastActionTable.CONTENT_URI;
        Cursor cursor = cr.query(activity, null, null, null, null);
        try {
            while (cursor != null && cursor.moveToNext()) {
                int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);
                String action = cursor.getString(actionColumn);
                try {
                    Intent i = new Intent(action);
                    this.sendBroadcast(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void sendAllActivitiesBarrage() {
        ContentResolver cr = this.getContentResolver();
        Uri activity = ITA_Contract.ActivityTable.CONTENT_URI;
        Cursor cursor = cr.query(activity, null, null, null, null);
        try {
            while (cursor != null && cursor.moveToNext()) {
                int packageColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
                int componentColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);
                String packageName = cursor.getString(packageColumn);
                String componentName = cursor.getString(componentColumn);
                try {
                    Intent i = new Intent();
                    i.setClassName(packageName, componentName);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void sendAllServicesBarrage() {
        ContentResolver cr = this.getContentResolver();
        Uri service = ITA_Contract.ServiceTable.CONTENT_URI;
        Cursor cursor = cr.query(service, null, null, null, null);
        try {
            while (cursor != null && cursor.moveToNext()) {
                int packageColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
                int componentColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);
                String packageName = cursor.getString(packageColumn);
                String componentName = cursor.getString(componentColumn);
                try {
                    Intent i = new Intent();
                    i.setClassName(packageName, componentName);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startService(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void sendAllReceiversBarrage() {
        ContentResolver cr = this.getContentResolver();
        Uri receiver = ITA_Contract.ServiceTable.CONTENT_URI;
        Cursor cursor = cr.query(receiver, null, null, null, null);
        try {
            while (cursor != null && cursor.moveToNext()) {
                int packageColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
                int componentColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);
                String packageName = cursor.getString(packageColumn);
                String componentName = cursor.getString(componentColumn);
                try {
                    Intent i = new Intent();
                    i.setClassName(packageName, componentName);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.sendBroadcast(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public Process startLogForBroadcastActionIntent(String dir, int id, String action) {
        String[] commandClear = {"logcat", "-c"};
        execCommandNoOutput(commandClear);
        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CLEAR_LOG);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] commandLog = {"logcat", "-v", "threadtime", "-f", currentLogDir + "/" + dir + "/" + action + ".txt", "-s", "*:E"};
        Process logIt = execCommandNoOutput(commandLog);
        return logIt;
    }

    public Process startLogForApplicationComponent(String dir, int id, String packageName, String componentName) {
        String[] commandClear = {"logcat", "-c"};
        execCommandNoOutput(commandClear);
        try {
            Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CLEAR_LOG);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] commandLog = {"logcat", "-v", "threadtime", "-f", currentLogDir + "/" + dir + "/" + packageName + "|" + componentName + ".txt", "-s", "*:E"};
        Process logIt = execCommandNoOutput(commandLog);
        return logIt;
    }

    static Process execCommandNoOutput(String[] command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return p;
    }

    public void populateKnownToublesomeApplicationComponents() {
        troublesomeApplicationComponents.add("com.mediatek.schpwronoff/com.mediatek.schpwronoff.ShutdownActivity"); // will turn off the device for some devices with a MediaTek chipset
        //java.lang.RuntimeException: Unable to instantiate service com.android.services.telephony.sip.SipCallServiceProvider: java.lang.ClassNotFoundException: Didn't find class "com.android.services.telephony.sip.SipCallServiceProvider" on path: DexPathList[[zip file "/system/priv-app/TeleService/TeleService.apk"],nativeLibraryDirectories=[/system/priv-app/TeleService/lib/arm, /vendor/lib, /system/lib]]
        troublesomeApplicationComponents.add("com.android.phone/com.android.services.telephony.sip.SipCallServiceProvider"); // will continually crash AOSP 6.0.1 on Nexus 5
        //java.lang.RuntimeException: Unable to instantiate service com.mediatek.bluetooth.map.BluetoothMapServerService: java.lang.ClassNotFoundException: Didn't find class "com.mediatek.bluetooth.map.BluetoothMapServerService" on path: DexPathList[[zip file "/system/app/MtkBt/MtkBt.apk"],nativeLibraryDirectories=[/vendor/lib, /system/lib]]
        troublesomeApplicationComponents.add("com.mediatek.bluetooth/com.mediatek.bluetooth.map.BluetoothMapServerService"); // will continually crash Juning Z8 on Android 5.1
        //java.lang.RuntimeException: Unable to instantiate service com.android.services.telephony.TelephonyCallServiceProvider: java.lang.ClassNotFoundException: Didn't find class "com.android.services.telephony.TelephonyCallServiceProvider" on path: DexPathList[[zip file "/system/priv-app/TeleService/TeleService.apk"],nativeLibraryDirectories=[/vendor/lib, /system/lib]]
        troublesomeApplicationComponents.add("com.android.phone/com.android.services.telephony.TelephonyCallServiceProvider"); // will continually crash Figo Atrium 5.5 on Android 5.1
        // SVC-Creating service: CreateServiceData{token=android.os.BinderProxy@2b7e1ed8 className=com.mediatek.bluetooth.ftp.BluetoothFtpService packageName=com.mediatek.bluetooth intent=null}
        troublesomeApplicationComponents.add("com.mediatek.bluetooth/com.mediatek.bluetooth.ftp.BluetoothFtpService"); // will continually crash Figo Atrium 5.5 on Android 5.1
        // E/AndroidRuntime( 3006): java.lang.UnsatisfiedLinkError: dalvik.system.PathClassLoader[DexPathList[[zip file "/system/app/MtkBt/MtkBt.apk"],nativeLibraryDirectories=[/vendor/lib, /system/lib]]] couldn't find "libextfmoverbt_jni.so"
        troublesomeApplicationComponents.add("com.mediatek.bluetooth/com.mediatek.bluetooth.fmOverBt.FmOverBtService"); // will continually crash Figo Atrium 5.5 on Android 5.1
        troublesomeApplicationComponents.add("com.mediatek.thermalmanager/com.mediatek.thermalmanager.ShutDownAlarm"); // will shut down Ulefone Power X2 (7.0) - broadcast receiver
        troublesomeApplicationComponents.add("com.android.settings/com.android.settings.FactoryReceiver"); // will factory reset the Plum Compass (6.0) - broadcast receiver
        troublesomeApplicationComponents.add("com.android.settings/com.sprd.settings.SprdUsbSettings"); // breaks ADB connection on Plum (6.0) - broadcast receiver
        troublesomeApplicationComponents.add("com.autoreboot.android/com.autoreboot.android.MainActivity"); // will consistently reboot if called although can hit a button and stop it on RCA Voyager Tablet 2 (5.0)
        troublesomeApplicationComponents.add("android/com.android.server.SystemRestoreReceiver"); // bricks MXQ box
        troublesomeApplicationComponents.add("com.sony.dtv.timers/com.sony.dtv.preferences.sleeptimer.LedLit"); // crashes continuosly on Sony Android TV - Sony/SVP4KDTV15_UC/SVP-DTV15:6.0.1/MMB29V.S35/3.885:user/release-keys
        troublesomeApplicationComponents.add("com.ts.android.hiddenmenu/com.ts.android.hiddenmenu.rtn.RTNResetActivity"); // factory resets the Essential phone - essential/mata/mata:7.1.1/NMJ88C/464:user/release-keys
    }

    public void populateKnowntroublesomeBroadcastActions() {
        troublesomeBroadcastActions.add("com.mediatek.ppl.NOTIFY_LOCK"); // will stop ADB form working - Figo Atrium
        troublesomeBroadcastActions.add("com.mediatek.ppl.NOTIFY_UNLOCK"); // may stop ADB form working - Figo Atrium
        troublesomeBroadcastActions.add("action_screen_pinning_request"); // pins the screen - BLU LIFE ONE X2
        troublesomeBroadcastActions.add("com.android.systemui.power.action.ACTION_REQUEST_SHUTDOWN"); // will power off Samsung Tab A on Android 6.0.1
        troublesomeBroadcastActions.add("android.intent.action.BMW_SHUTDOWN_START"); // will power off Samsung Tab A on Android 6.0.1
        troublesomeBroadcastActions.add("android.intent.action.FORCE_UPDATE_ALARM"); // will try to flash a firmware update and go into recovery mode for Samsung S6 Edge
        troublesomeBroadcastActions.add("android.intent.action.POLICY_WINDOWED_ALARM"); // will try to flash a firmware update and go into recovery mode for Samsung S6 Edge
        troublesomeBroadcastActions.add("edm.intent.action.disable.mtp"); // will disable ADB for Samsung S6 Edge until the USB is unplugged and plugged back in
        troublesomeBroadcastActions.add("com.amazon.firelauncher.appsgrid.intent.action.USE_FAKE_APPS"); // will place fake apps on the screen for Amazon Fire TV Stick2 on Android 5.1
        troublesomeBroadcastActions.add("android.intent.action.normal.shutdown"); // can turn the screen blank - Figo Atrium
        troublesomeBroadcastActions.add("com.lge.intent.action.ACTION_THERMALDAEMON_SHUTDOWN"); // will shutdown the device - LG Phoenix 2
        troublesomeBroadcastActions.add("com.mediatek.dm.LAWMO_LOCK"); // locks interface on Kata C2
        troublesomeBroadcastActions.add("com.mediatek.dm.LAWMO_UNLOCK"); // may lock interface on Kata C2
        troublesomeBroadcastActions.add("android.net.dhcp.DhcpClient.wlan0.KICK"); // may lock interface on Kata C2
        troublesomeBroadcastActions.add("android.intent.action.ACTION_SHUTDOWN_IPO"); // will turn off screen on RCA Q1
        troublesomeBroadcastActions.add("android.intent.action.ACTION_BOOT_IPO"); // will lock screen on Yuntab
        troublesomeBroadcastActions.add("usb_connected_choose_mtp"); // will turn off USB Debugging on Huawei
        troublesomeBroadcastActions.add("usb_connected_choose_ptp"); // will turn off USB Debugging on Huawei - either this one of the one above
        troublesomeBroadcastActions.add("usb_connected_choose_storage"); // will turn off USB Debugging on Huawei - either this one of the one above
        troublesomeBroadcastActions.add("com.android.server.WifiManager.action.DEVICE_IDLE"); // will turn off USB Debugging on Huawei - either this one of the one above
        troublesomeBroadcastActions.add("usb_connected_deactive_mtp"); // will turn off USB Debugging on Huawei - either this one of the one above
        troublesomeBroadcastActions.add("PNW.batterySettings"); // will bring up battery activity and kills our process on Juning Media Box on Android 5.1.1
        troublesomeBroadcastActions.add("android.intent.action.normal.boot.done"); // will bring up battery activity and kills our process on Juning Media Box on Android 5.1.1
        troublesomeBroadcastActions.add("android.intent.action.normal.boot"); // // will bring up the boot screen forever on RCA Voyager Tablet II on Android 5.0, BLU Advance 5.0 on Android 5.1, BLU R1 HD on Android 6.0 (BLU/R1_HD/R1_HD:6.0/MRA58K/1471954662:user/release-keys), Yuntab on Android 4.4.2, RCA Q1, Juning Z8 on Android 5.1
        troublesomeBroadcastActions.add("com.innovatech.magickey.unlocked"); // breaks ADB connection on Plum (6.0)
        troublesomeBroadcastActions.add("android.intent.action.phonetrack_masterclear"); // will factory reset Leagoo Z5C (6.0)
        troublesomeBroadcastActions.add("android.intent.action.MASTER_CLEAR"); // will factory reset MXQ TV Box (4.4.2)
        troublesomeBroadcastActions.add("android.intent.action.SYSTEM_RESTORE"); // will brick MXQ TV Box (4.4.2)
    }

    public void obtainActiveBroadcastReceiverActions() {

        String[] command = {"dumpsys", "activity", "broadcasts"};
        ArrayList<String> rawBroadcastData = execCommandGetOutput(command);

        if (rawBroadcastData.size() == 0)
            Log.w(TAG, "No output from running the command adb shell dumpsys activity broadcasts");

        // parse out the actions from the raw data
        ArrayList<String> broadcastActions = processRawBroadcastReceiverOutput(rawBroadcastData);

        // write the actions to the database
        writeBroadcastActionsToDB(broadcastActions);
    }

    public ArrayList<String> processRawBroadcastReceiverOutput(ArrayList<String> data) {

        ArrayList<String> broadcastActions = new ArrayList<String>();

        int counter = 0;

        boolean onlySystemServer = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
        String targetedApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        ListIterator<String> iter = data.listIterator();
        while (iter.hasNext()) {

            String str = iter.next();

            Matcher receiverMatcher = receiverListPattern.matcher(str);
            if (!receiverMatcher.matches()) continue;

            String packageName = receiverMatcher.group(3);
            String userID = receiverMatcher.group(4);

            if (targetedApp != null && targetedApp.length() != 0) {
                if (targetedApp.equals("android") && packageName.equals("system")) {
                    parseOutBroadcastReceverData(packageName, userID, iter, ++counter, broadcastActions);
                }
            } else { // operating on all apps
                if (packageName.equals("system")) {
                    parseOutBroadcastReceverData(packageName, userID, iter, ++counter, broadcastActions);
                }
            }
        }
        return broadcastActions;
    }

    public static Collection<String> parseOutBroadcastReceiversForApp(String packageName, Collection<String> broadcastActions) {
        if (packageName == null)
            return null;

        String[] command = {"dumpsys", "activity", "broadcasts", packageName};
        ArrayList<String> broadInfo = execCommandGetOutput(command);
        if (broadInfo == null)
            return null;

        ArrayList<String> tempActions = new ArrayList<String>();
        boolean requiresPermission = false;

        for (String line : broadInfo) {

            Matcher actionMatcher = actionPattern.matcher(line);
            if (actionMatcher.matches()) {
                String action = actionMatcher.group(1);
                if (!tempActions.contains(action))
                    tempActions.add(action);
                continue;
            }
            Matcher permissionMatcher = permissionPattern.matcher(line);
            if (permissionMatcher.matches()) {
                requiresPermission = true;
                continue;
            }

            Matcher broadcastFilterMatcher = broadcastFilterPattern.matcher(line);
            if (broadcastFilterMatcher.matches()) {
                if (requiresPermission == false) {
                    for (String action : tempActions) {
                        if (!broadcastActions.contains(action))
                            broadcastActions.add(action);
                    }
                } else
                    requiresPermission = false;
                tempActions.clear();
                continue;
            }
        }

        if (!tempActions.isEmpty()) {
            if (requiresPermission == false) {
                for (String action : tempActions) {
                    if (!broadcastActions.contains(action))
                        broadcastActions.add(action);
                }
            }
        }
        return broadcastActions;
    }

    public void parseOutBroadcastReceverData(String packageName, String userID, ListIterator<String> iter, int counter, ArrayList<String> broadcastActions) {
        ArrayList<String> tempActions = new ArrayList<String>();
        boolean requiresPermission = false;
        boolean keepParsing = true;
        while (keepParsing == true && iter.hasNext()) {
            String next = iter.next();

            Matcher actionMatcher = actionPattern.matcher(next);
            if (actionMatcher.matches()) {
                String action = actionMatcher.group(1);
                if (!tempActions.contains(action))
                    tempActions.add(action);
                continue;
            }
            Matcher permissionMatcher = permissionPattern.matcher(next);
            if (permissionMatcher.matches()) {
                requiresPermission = true;
                continue;
            }
            Matcher receiverMatcher = receiverListPattern.matcher(next);
            if (receiverMatcher.matches()) {
                iter.previous();
                keepParsing = false;
            }
        }
        if (requiresPermission == false) {
            for (String action : tempActions) {
                if (!broadcastActions.contains(action))
                    broadcastActions.add(action);
            }
        }
    }

    public static String calculateMD5(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

    // will execute a command and return the output as an ArrayList of strings
    public static ArrayList<String> execCommandGetOutput(String[] command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process p;
        ArrayList<String> output = new ArrayList<String>();
        try {
            p = pb.start();
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                output.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    void sendToastToMainActivity(String message) {
        Intent i = new Intent(this.getApplicationContext(), ITA_MainAct.class);
        i.setAction(ITA_Constants.DISPLAY_TOAST);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(ITA_Constants.TOAST_MESSAGE, message);

        try {
            startActivity(i);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    void finishMainActivity() {
        Intent i = new Intent(this.getApplicationContext(), ITA_MainAct.class);
        i.setAction(ITA_Constants.FINISH_ACTIVITY);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(i);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void writeBroadcastActionsToDB(ArrayList<String> broadcastActions) {
        Log.w(TAG, "obtaining broadcast actions and writing them to database");
        if (broadcastActions == null)
            return;
        Uri dest = ITA_Contract.BroadcastActionTable.CONTENT_URI;
        ContentResolver cr = this.getContentResolver();
        for (String action : broadcastActions) {
            ContentValues cv = new ContentValues();
            cv.put(ITA_Contract.SKIPPED, 0);
            cv.put(ITA_Contract.ACTION, action);
            Uri ret = cr.insert(dest, cv);
            int a = 0;
            a++;
        }
        Log.w(TAG, "finished broadcast actions receivers to database");
    }

    private void writeReceiversToDB() {
        Log.w(TAG, "obtaining receivers and writing them to database");
        String myPackage = this.getApplicationContext().getPackageName();
        ContentResolver cr = this.getContentResolver();
        PackageManager pm = getPackageManager();

        boolean onlySystemServer = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
        String targetedApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        List<PackageInfo> packs = pm.getInstalledPackages(PackageManager.GET_RECEIVERS);
        if (packs != null) {
            for (final PackageInfo p : packs) {

                if (p == null)
                    continue;

                // ignore out own app
                if (p.packageName.equals(myPackage)) {
                    continue;
                }

                // if onlySystemServer == true, then only components in the android package are considered
                if (onlySystemServer && !p.packageName.equals("android")) {
                    continue;
                }

                if (targetedApp != null && targetedApp.length() != 0 && !p.packageName.equals(targetedApp)) {
                    continue;
                }

                if (Arrays.asList(ITA_Constants.PACKAGE_NAMES_TO_SKIP).contains(p.packageName)) {
                    Log.w(TAG, "skipping whitelisted package - " + p.packageName);
                    continue;
                }

                HashSet<String> uniqueReceivers = new HashSet<String>();
                ActivityInfo[] receivers = p.receivers;
                if (receivers != null) {
                    for (ActivityInfo ai : receivers) {
                        String componentName = ai.name;
                        if (uniqueReceivers.contains(componentName)) {
                            Log.w(TAG, "not recording duplicate component - " + p.packageName + "/" + componentName);
                            continue;
                        } else {
                            uniqueReceivers.add(componentName);
                        }

                        String permission = ai.permission;
                        String packageName = p.packageName;
                        if (permission == null)
                            permission = "";
                        boolean exported = ai.exported;
                        String versionName = p.versionName;
                        int versionCode = p.versionCode;
                        int uid = -1;
                        try {
                            uid = pm.getApplicationInfo(p.packageName, 0).uid;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (ITA_Constants.WRITE_ALL_COMPONENTS_TO_SEPARATE_TABLE) {
                            ContentValues cv = new ContentValues();
                            cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                            cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                            cv.put(ITA_Contract.COMPONENT_TYPE, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME);
                            cv.put(ITA_Contract.VERSION_NAME, versionName);
                            cv.put(ITA_Contract.PERMISSION, permission);
                            cv.put(ITA_Contract.VERSION_CODE, versionCode);
                            if (exported == true)
                                cv.put(ITA_Contract.EXPORTED, 1);
                            else
                                cv.put(ITA_Contract.EXPORTED, 0);
                            cv.put(ITA_Contract.UID, uid);
                            Uri dest = ITA_Contract.AllComponentsTable.CONTENT_URI;
                            cr.insert(dest, cv);
                        }

                        if (exported == false || !permission.equals(""))
                            continue;

                        ContentValues cv = new ContentValues();
                        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                        cv.put(ITA_Contract.UID, uid);
                        cv.put(ITA_Contract.SKIPPED, 0);
                        cv.put(ITA_Contract.VERSION_NAME, p.versionName);
                        cv.put(ITA_Contract.VERSION_CODE, p.versionCode);
                        cv.put(ITA_Contract.PERMISSION, permission);
                        Uri dest = ITA_Contract.ReceiverTable.CONTENT_URI;
                        cr.insert(dest, cv);
                    }
                }
            }
        }
        Log.w(TAG, "finished writing receivers to database");
    }

    private void writeContentProvidersToDB() {
        Log.w(TAG, "obtaining content providers and writing them to database");
        String myPackage = this.getApplicationContext().getPackageName();
        ContentResolver cr = this.getContentResolver();
        PackageManager pm = getPackageManager();
        @SuppressLint("WrongConstant") List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_PROVIDERS);
        boolean onlySystemServer = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
        String targetedApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        for (ApplicationInfo packageInfo : packages) {

            if (packageInfo == null)
                continue;

            String packageName = packageInfo.packageName;

            // ignore our own app
            if (packageInfo.packageName.equals(myPackage)) {
                continue;
            }

            if (onlySystemServer && !packageInfo.packageName.equals("android")) {
                continue;
            }

            if (targetedApp != null && targetedApp.length() != 0 && !packageInfo.packageName.equals(targetedApp)) {
                continue;
            }

            if (Arrays.asList(ITA_Constants.PACKAGE_NAMES_TO_SKIP).contains(packageName)) {
                Log.w(TAG, "skipping whitelisted package - " + packageName);
                continue;
            }

            try {
                HashSet<String> uniqueProviders = new HashSet<String>();
                PackageInfo providers = pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_PROVIDERS);
                if (providers != null && providers.providers != null) {
                    for (ProviderInfo pi : providers.providers) {
                        String readPermission = pi.readPermission;
                        if (readPermission == null)
                            readPermission = "";
                        String writePermission = pi.writePermission;
                        if (writePermission == null)
                            writePermission = "";
                        boolean exported = pi.exported;
                        String authority = pi.authority;
                        String componentName = pi.name;
                        String versionName = providers.versionName;
                        int versionCode = providers.versionCode;
                        int uid = packageInfo.uid;

                        if (uniqueProviders.contains(componentName + "/" + authority)) {
                            Log.w(TAG, "not recording duplicate component - " + packageName + "/" + componentName);
                            continue;
                        } else {
                            uniqueProviders.add(componentName + "/" + authority);
                        }

                        if (ITA_Constants.WRITE_ALL_COMPONENTS_TO_SEPARATE_TABLE) {
                            ContentValues cv = new ContentValues();
                            cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                            cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                            cv.put(ITA_Contract.PROVIDER_AUTHORITY, authority);
                            cv.put(ITA_Contract.COMPONENT_TYPE, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME);
                            cv.put(ITA_Contract.VERSION_NAME, versionName);
                            cv.put(ITA_Contract.VERSION_CODE, versionCode);
                            if (exported == true)
                                cv.put(ITA_Contract.EXPORTED, 1);
                            else
                                cv.put(ITA_Contract.EXPORTED, 0);
                            cv.put(ITA_Contract.READ_PERMISSION, readPermission);
                            cv.put(ITA_Contract.WRITE_PERMISSION, writePermission);
                            cv.put(ITA_Contract.UID, uid);
                            Uri dest = ITA_Contract.AllComponentsTable.CONTENT_URI;
                            cr.insert(dest, cv);
                        }

                        if (exported == false)
                            continue;

                        // skip if we cannot read or write
                        if (!readPermission.equals("") && !writePermission.equals("")) {
                            continue;
                        }

                        ContentValues cv = new ContentValues();
                        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                        cv.put(ITA_Contract.UID, uid);
                        cv.put(ITA_Contract.PROVIDER_AUTHORITY, authority);
                        cv.put(ITA_Contract.VERSION_NAME, versionName);
                        cv.put(ITA_Contract.VERSION_CODE, versionCode);
                        if (writePermission.equals(""))
                            cv.put(ITA_Contract.CAN_WRITE, ITA_Contract.CAN_ACCESS_PROV);
                        else
                            cv.put(ITA_Contract.CAN_WRITE, ITA_Contract.CANT_ACCESS_PROV);
                        if (readPermission.equals(""))
                            cv.put(ITA_Contract.CAN_READ, ITA_Contract.CAN_ACCESS_PROV);
                        else
                            cv.put(ITA_Contract.CAN_READ, ITA_Contract.CANT_ACCESS_PROV);
                        Uri dest = ITA_Contract.ProviderTable.CONTENT_URI;
                        cr.insert(dest, cv);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.w(TAG, "finished writing content providers to database");
    }

    private void writeActivitiesToDB() {
        Log.w(TAG, "obtaining activities and writing them to database");
        String myPackage = this.getApplicationContext().getPackageName();
        ContentResolver cr = this.getContentResolver();
        PackageManager pm = getPackageManager();
        @SuppressLint("WrongConstant") List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_INTENT_FILTERS);
        boolean onlySystemServer = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
        String targetedApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        for (ApplicationInfo packageInfo : packages) {

            if (packageInfo == null)
                continue;

            String packageName = packageInfo.packageName;

            // ignore our own app
            if (packageInfo.packageName.equals(myPackage)) {
                continue;
            }

            // user has decided to only target system_server
            if (onlySystemServer && !packageInfo.packageName.equals("android")) {
                continue;
            }

            // user has select to target only a single app
            if (targetedApp != null && targetedApp.length() != 0 && !packageInfo.packageName.equals(targetedApp)) {
                continue;
            }

            // apps to ignore testing for
            if (Arrays.asList(ITA_Constants.PACKAGE_NAMES_TO_SKIP).contains(packageName)) {
                Log.w(TAG, "skipping whitelisted package - " + packageName);
                continue;
            }

            try {
                PackageInfo activities = pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_ACTIVITIES);
                HashSet<String> uniqueActivities = new HashSet<String>();
                if (activities != null && activities.activities != null) {
                    for (ActivityInfo ai : activities.activities) {
                        String permission = ai.permission;
                        if (permission == null)
                            permission = "";
                        boolean exported = ai.exported;
                        String versionName = activities.versionName;
                        int versionCode = activities.versionCode;
                        int uid = packageInfo.uid;
                        String componentName = ai.name;

                        if (uniqueActivities.contains(componentName)) {
                            Log.w(TAG, "not recording duplicate component - " + packageName + "/" + componentName);
                            continue;
                        } else {
                            uniqueActivities.add(componentName);
                        }

                        if (ITA_Constants.WRITE_ALL_COMPONENTS_TO_SEPARATE_TABLE) {
                            ContentValues cv = new ContentValues();
                            cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                            cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                            cv.put(ITA_Contract.COMPONENT_TYPE, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME);
                            cv.put(ITA_Contract.VERSION_NAME, versionName);
                            cv.put(ITA_Contract.PERMISSION, permission);
                            cv.put(ITA_Contract.VERSION_CODE, versionCode);
                            if (exported == true)
                                cv.put(ITA_Contract.EXPORTED, 1);
                            else
                                cv.put(ITA_Contract.EXPORTED, 0);
                            cv.put(ITA_Contract.UID, uid);
                            Uri dest = ITA_Contract.AllComponentsTable.CONTENT_URI;
                            cr.insert(dest, cv);
                        }

                        if (exported == false || !permission.equals(""))
                            continue;

                        ContentValues cv = new ContentValues();
                        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                        cv.put(ITA_Contract.UID, uid);
                        cv.put(ITA_Contract.VERSION_NAME, versionName);
                        cv.put(ITA_Contract.SKIPPED, 0);
                        cv.put(ITA_Contract.VERSION_CODE, versionCode);
                        cv.put(ITA_Contract.PERMISSION, permission);
                        Uri dest = ITA_Contract.ActivityTable.CONTENT_URI;
                        cr.insert(dest, cv);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.w(TAG, "finished writing activities to database");
    }

    private void writeServicesToDB() {

        Log.w(TAG, "obtaining services and writing them to database");

        String myPackage = this.getApplicationContext().getPackageName();

        boolean onlySystemServer = readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
        String targetedApp = readStringSharedPreferences(getApplicationContext(), ITA_Constants.TARGETED_APP);

        ContentResolver cr = this.getContentResolver();
        PackageManager pm = getPackageManager();
        @SuppressLint("WrongConstant") List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_INTENT_FILTERS);
        //List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_SERVICES);

        for (ApplicationInfo packageInfo : packages) {

            if (packageInfo == null)
                continue;

            String packageName = packageInfo.packageName;

            // ignore our own app
            if (packageInfo.packageName.equals(myPackage)) {
                continue;
            }

            if (onlySystemServer && !packageName.equals("android")) {
                continue;
            }

            if (targetedApp != null && targetedApp.length() != 0 && !packageName.equals(targetedApp)) {
                continue;
            }

            if (Arrays.asList(ITA_Constants.PACKAGE_NAMES_TO_SKIP).contains(packageName)) {
                Log.w(TAG, "skipping whitelisted package - " + packageName);
                continue;
            }

            try {
                HashSet<String> uniqueServices = new HashSet<String>();
                PackageInfo services = pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_SERVICES);
                if (services != null && services.services != null) {
                    for (ServiceInfo si : services.services) {
                        String permission = si.permission;
                        if (permission == null)
                            permission = "";
                        boolean exported = si.exported;
                        String versionName = services.versionName;
                        int versionCode = services.versionCode;
                        int uid = packageInfo.uid;
                        String componentName = si.name;

                        if (uniqueServices.contains(componentName)) {
                            Log.w(TAG, "not recording duplicate component - " + packageName + "/" + componentName);
                            continue;
                        } else {
                            uniqueServices.add(componentName);
                        }

                        if (ITA_Constants.WRITE_ALL_COMPONENTS_TO_SEPARATE_TABLE) {
                            ContentValues cv = new ContentValues();
                            cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                            cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                            cv.put(ITA_Contract.COMPONENT_TYPE, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME);
                            cv.put(ITA_Contract.VERSION_NAME, versionName);
                            cv.put(ITA_Contract.PERMISSION, permission);
                            cv.put(ITA_Contract.VERSION_CODE, versionCode);
                            if (exported == true)
                                cv.put(ITA_Contract.EXPORTED, 1);
                            else
                                cv.put(ITA_Contract.EXPORTED, 0);
                            cv.put(ITA_Contract.UID, uid);
                            Uri dest = ITA_Contract.AllComponentsTable.CONTENT_URI;
                            cr.insert(dest, cv);
                        }

                        if (exported == false || !permission.equals(""))
                            continue;

                        ContentValues cv = new ContentValues();
                        cv.put(ITA_Contract.PACKAGE_NAME, packageName);
                        cv.put(ITA_Contract.COMPONENT_NAME, componentName);
                        cv.put(ITA_Contract.UID, uid);
                        cv.put(ITA_Contract.SKIPPED, 0);
                        cv.put(ITA_Contract.VERSION_NAME, versionName);
                        cv.put(ITA_Contract.VERSION_CODE, versionCode);
                        cv.put(ITA_Contract.PERMISSION, permission);
                        Uri dest = ITA_Contract.ServiceTable.CONTENT_URI;
                        cr.insert(dest, cv);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (DEBUG)
            Log.w(TAG, "finished writing services to database");
    }

    void createNotification(String str) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setOngoing(false)
                        .setContentText(str)
                        .setContentTitle(str);

        Intent i = new Intent(this.getApplicationContext(), ITA_MainAct.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 300, i, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            Notification noti = mBuilder.build();
            nm.notify(ITA_IntentService.notificationID, noti);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ArrayList<String> readFileIntoArrayList(File file) {
        ArrayList<String> fileLines = new ArrayList<String>();
        if (!file.exists())
            return fileLines;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            try {
                while ((line = br.readLine()) != null) {
                    fileLines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return fileLines;
    }

    // will take an ArrayList<String> and write each string to the file separated by newlines
    public void writeJSONObjectArrayListToFile(String path, ArrayList<JSONObject> data) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            for (JSONObject s : data) {
                out.write(s.toString() + newLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static boolean readBooleanSharedPreferences(Context con, String key) {
        SharedPreferences sp = con.getSharedPreferences(SP, MODE_PRIVATE);
        boolean got = sp.getBoolean(key, false);
        return got;
    }

    public static String readStringSharedPreferences(Context con, String key) {
        SharedPreferences sp = con.getSharedPreferences(SP, MODE_PRIVATE);
        String got = sp.getString(key, "");
        return got;
    }

    public static int readIntSharedPreferences(Context con, String key) {
        SharedPreferences sp = con.getSharedPreferences(SP, MODE_PRIVATE);
        int got = sp.getInt(key, 1);
        return got;
    }

    public static long readLongSharedPreferences(Context con, String key) {
        SharedPreferences sp = con.getSharedPreferences(SP, MODE_PRIVATE);
        long got = sp.getLong(key, -1);
        return got;
    }

    public static void writeBooleanSharedPreferences(Context con, String key, boolean value) {
        SharedPreferences.Editor editor = con.getSharedPreferences(SP, MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void writeStringSharedPreferences(Context con, String key, String value) {
        SharedPreferences.Editor editor = con.getSharedPreferences(SP, MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void writeIntSharedPreferences(Context con, String key, int value) {
        SharedPreferences.Editor editor = con.getSharedPreferences(SP, MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void writeLongSharedPreferences(Context con, String key, long value) {
        SharedPreferences.Editor editor = con.getSharedPreferences(SP, MODE_PRIVATE).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static HashSet<String> readHashSetSharedPreferences(Context con, String key) {
        SharedPreferences sp = con.getSharedPreferences(SP, MODE_PRIVATE);
        HashSet<String> set = (HashSet<String>) sp.getStringSet(key, new HashSet<String>());
        return set;
    }

    public static void writeHashSetSharedPreferences(Context con, String key, HashSet<String> set) {
        SharedPreferences.Editor editor = con.getSharedPreferences(SP, MODE_PRIVATE).edit();
        editor.putStringSet(key, set);
        editor.commit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
