package com.kryptowire.daze;

import com.kryptowire.daze.activity.ITA_AppSelection;
import com.kryptowire.daze.activity.ITA_ExceptionTypeSelection;
import com.kryptowire.daze.activity.ITA_PermInfo;
import com.kryptowire.daze.activity.ITA_ReadMe;
import com.kryptowire.daze.activity.ITA_Settings;
import com.kryptowire.daze.provider.ITA_Contract;
import com.kryptowire.daze.service.ITA_IntentService;
import com.kryptowire.daze.service.ITA_OOMService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class ITA_MainAct extends Activity {

	final static String TAG = ITA_MainAct.class.getSimpleName();

	// example: RTC_WAKEUP #0: Alarm{67c0730 type 0 when 1523999998476 com.kryptowire.daze}
    static final String alarmLineRegex = "\\s*RTC_WAKEUP #(\\d+): Alarm\\{(.*) type (.*) when (\\d+) com.kryptowire.daze\\}";
    static final Pattern alarmLinePattern = Pattern.compile(alarmLineRegex);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();

		if (intent == null)
			return;
		String action = intent.getAction();
		if (action == null)
			return;

		init();

		processReceivedIntent(intent);		
	}

	private void processReceivedIntent(Intent intent) {
		final String action = intent.getAction();
		if (action == null)
		    return;

        Log.w(TAG, "Action Received = " + action);

		if (action.equals(ITA_Constants.DISPLAY_TOAST)) {
			String toastMessage = intent.getStringExtra(ITA_Constants.TOAST_MESSAGE);
			if (toastMessage != null && !toastMessage.isEmpty())
				Toast.makeText(this.getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
			//finish();
		}
		else if (action.equals(ITA_Constants.FINISHED_ANALYZING_LOGS)) {
			Toast.makeText(this.getApplicationContext(), "Examine Results", Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, ITA_ExceptionTypeSelection.class);
			startActivity(i);
			finish();
		}
		else if (action.equals(ITA_Constants.CONTINUE_TESTING_ACTION)) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    checkIfSoftReboot(action);
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }
        else if (action.equals(ITA_Constants.START_ANALYSIS)) {
		    String packageName = intent.getStringExtra(ITA_Constants.PACKAGE_NAME);
		    if (packageName != null && !packageName.isEmpty()) {
                Intent intent1 = new Intent(getApplicationContext(), ITA_IntentService.class);
                intent1.setAction(ITA_Constants.START_ANALYSIS);
                intent1.putExtra(ITA_Constants.PACKAGE_NAME, packageName);
                startService(intent1);
            }
        }
		else if (action.equals(ITA_Constants.FINISH_ACTIVITY)) {
			finish();
		}		
	}

	public void checkIfSoftReboot(String action) {
        boolean isSendingIntents = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PERFORMING_ANALYSIS);
        boolean isAnalyzingLogs = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ANALYZING_LOGS);

        Log.w(TAG, "isSendingIntents = " + isSendingIntents + ", isAnalyzingLogs = " + isAnalyzingLogs);

        if (isSendingIntents) {
            if (!alarmExists()) {
                action = ITA_Constants.DEVICE_REBOOTED_RESUME_ANALYSIS;
            }
            Intent i = new Intent(getApplicationContext(), ITA_IntentService.class);
            i.setAction(action);
            startService(i);
        }
        else if (isAnalyzingLogs) {
            Intent i = new Intent(getApplicationContext(), ITA_IntentService.class);
            i.setAction(ITA_Constants.ANALYZE_LOGS);
            startService(i);
        }
    }

    public static boolean alarmExists() {
	    try {
            String[] command = {"dumpsys", "alarm"};
            ArrayList<String> alarmData = ITA_IntentService.execCommandGetOutput(command);
            if (alarmData == null)
                return false;

            for (int a = 0; a < alarmData.size(); a++) {
                if (alarmLinePattern.matcher(alarmData.get(a)).matches()) {
                    Log.w(TAG, "found placeholder alarm - " + alarmData.get(a));
                    return true;
                }
            }
        } catch (Exception e) {
	        e.printStackTrace();
        }
        Log.w(TAG, "placeholder alarm not found");
	    return false;
    }




	public void init() {
		boolean isNotFirstRun = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.NOT_FIRST_RUN);
		if (!isNotFirstRun) {
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.NOT_FIRST_RUN, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_ACTIVITY, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_SERVICE, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_RECEIVER, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_BROADCASTACTION, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_PROVIDER, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING, false);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH, true);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.CHECK_FOR_PRIVILEGE_ESCALTION, false);
            ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING, true);
			Toast.makeText(getApplicationContext(), "Please read the readme prior to use if you are not familiar with Daze", Toast.LENGTH_LONG).show();
        }

        copyDazeScriptToSdCard();
	}

	public void copyDazeScriptToSdCard() {

		File outputPath = new File(ITA_Constants.outputDir);
		if (!outputPath.exists()) {
			outputPath.mkdir();
		}

		AssetManager assetManager = getAssets();
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = assetManager.open("daze.sh");
			fos = new FileOutputStream(new File(outputPath, "daze.sh"));
			byte[] readBytes = new byte[2048];
			while ((is.read(readBytes)) > 0) {
				fos.write(readBytes);
			}
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	public void targetIndividualApp(View view) {

		if (checkPermissions() == false) {
			Intent i = new Intent(this, ITA_PermInfo.class);
			this.startActivity(i);
			return;
		}

		Toast.makeText(this.getApplicationContext(), "Target Individual App", Toast.LENGTH_LONG).show();

		ArrayList<String> packages = new ArrayList<String>();
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(0);
		for (int i = 0; i < apps.size(); i++) {
			ApplicationInfo ai = apps.get(i);
			String packageName = ai.packageName;
			packages.add(packageName);
		}
		Collections.sort(packages);

		Bundle b = new Bundle();
		b.putStringArrayList(ITA_Constants.PACKAGE_NAME_LIST, packages);
		Intent intent = new Intent(this, ITA_AppSelection.class);	
		intent.putExtras(b);
		this.startActivity(intent);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if (intent == null)
			return;
		String action = intent.getAction();
		if (action == null)
			return;		
		processReceivedIntent(intent);

	}
	
	// this method will just return to the home screen by
	// sending an intent
	public void returnToHomeScreen() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}


	private void intentServiceStart(String action) {
		Intent i = new Intent(this.getApplicationContext(), ITA_IntentService.class);
		i.setAction(action);
		startService(i);
	}


	public void startTesting(View v) {

		if (checkPermissions() == false) {
			Intent i = new Intent(this, ITA_PermInfo.class);
			this.startActivity(i);
			return;
		}

		Toast.makeText(this.getApplicationContext(), "Starting Analysis", Toast.LENGTH_LONG).show();
        intentServiceStart(ITA_Constants.START_ANALYSIS);
		
		//waitAndFinishActivity();
	}

	public void stopTesting(View v) {		
		if (checkPermissions() == false) {
			Intent i = new Intent(this, ITA_PermInfo.class);
			this.startActivity(i);
			return;
		}	
		
		Toast.makeText(this.getApplicationContext(), "Stopping Analysis", Toast.LENGTH_LONG).show();
        intentServiceStart(ITA_Constants.STOP_ANALYSIS);
	}

	public void analyzeLogs(View v) {

		if (checkPermissions() == false) {
			Intent i = new Intent(this, ITA_PermInfo.class);
			this.startActivity(i);
			return;
		}

		Toast.makeText(this.getApplicationContext(), "Analyzing Logs", Toast.LENGTH_LONG).show();
        intentServiceStart(ITA_Constants.ANALYZE_LOGS);
	}	

	private boolean resultsExist() {

		ContentResolver cr = this.getContentResolver();
		Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, null, null, null);
		try {
			while (cursor != null && cursor.moveToNext()) {
				return true;
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}
		
		cursor = cr.query(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, null, null, null, null);
		try {
			while (cursor != null && cursor.moveToNext()) {
				return true;
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}
		
		
		return false;
	}
	
	
	private void waitAndFinishActivity() {
		
		try {
			Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_BEFORE_CLOSING_ACTIVITY);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finish();
		
	}

	private void launchKillSelfThread(final long millis) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				ITA_MainAct.this.killSelf(millis);
			}

		};
		Thread t = new Thread(r);
		t.start();
	}

	private void killSelf(long millis) {	
		
		Log.w(TAG, "killSelf");
		
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		android.os.Process.killProcess(android.os.Process.myPid());
	}


	private boolean checkPermissions() {
		int pid = android.os.Process.myPid();
		int uid = android.os.Process.myUid();

		int dumpPermission = this.checkPermission(Manifest.permission.READ_LOGS, pid, uid);    			
		int readLogsPermission = this.checkPermission(Manifest.permission.DUMP, pid, uid);

		if (dumpPermission == PackageManager.PERMISSION_DENIED) {
			Toast.makeText(this.getApplicationContext(), "Grant the app the DUMP permission using the following ADB command: adb shell pm grant " + this.getPackageName() + " " + Manifest.permission.DUMP, Toast.LENGTH_LONG).show();
		}	
		if (readLogsPermission == PackageManager.PERMISSION_DENIED) {
			Toast.makeText(this.getApplicationContext(), "Grant the app the READ_LOGS permission using the following ADB command: adb shell pm grant " + this.getPackageName() + " " + Manifest.permission.READ_LOGS, Toast.LENGTH_LONG).show();
		}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int packageUsageStatsPermission = this.checkPermission(Manifest.permission.PACKAGE_USAGE_STATS, pid, uid);
            if (dumpPermission == -1 || readLogsPermission == -1 || packageUsageStatsPermission == -1) {
                return false;
            }
        }
        else {
            if (dumpPermission == -1 || readLogsPermission == -1) {
                return false;
            }
        }

		boolean killedOnce = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.KILLED_AFTER_GRANT);
		if (killedOnce == false) {
			Toast.makeText(this.getApplicationContext(), "Killing app to ensure granted permissions are active. Please restart app", Toast.LENGTH_LONG).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.KILLED_AFTER_GRANT, true);
			this.launchKillSelfThread(2000);
			return false;
		}
		return true;
	}

	public void settings(View v) {
		Toast.makeText(this.getApplicationContext(), "Modify Settings", Toast.LENGTH_LONG).show();
		Intent i = new Intent(this, ITA_Settings.class);
		this.startActivity(i);
	}


	public void intentBarrage(View v) {
        intentServiceStart(ITA_Constants.INTENT_BARRAGE);
	}

	public void oomSoftReboot(View v) {
        startService(new Intent(getApplicationContext(), ITA_OOMService.class));
    }

    public void readme(View view) {
		startActivity(new Intent(getApplicationContext(), ITA_ReadMe.class));
	}

	public void examineResults(View v) {
		if (checkPermissions() == false) {
			Intent i = new Intent(this, ITA_PermInfo.class);
			this.startActivity(i);
			return;
		}
		boolean resultsExist = this.resultsExist();
		if (resultsExist == false) {
			Toast.makeText(this.getApplicationContext(), "There are no results to show. Try analyzing the logs first.", Toast.LENGTH_LONG).show();
			return;
		}
		Toast.makeText(this.getApplicationContext(), "Examine Results", Toast.LENGTH_LONG).show();
		Intent i = new Intent(this, ITA_ExceptionTypeSelection.class);
		this.startActivity(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
