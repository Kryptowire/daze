package com.kryptowire.daze.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;
import com.kryptowire.daze.provider.ITA_Contract;
import com.kryptowire.daze.service.ITA_IntentService;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ITA_DisplayResult extends Activity {

	static String action;
	static String packageName;
	static String componentName;
	static String newLine = System.getProperty("line.separator");
	static String componentType;
	static String baseComponentType;
	static final String TAG = ITA_DisplayResult.class.getSimpleName();
	static Bundle emptyBundle = new Bundle();
	static boolean isBroadcastAction = false;
	static Process logcatProcess;
	static BufferedReader br;
	static boolean continueParsingLog = true;
	public static boolean foundException = false;
	static Thread logReaderThread = null;
	static Random random = new Random();
	static boolean makeTextViewRed = false;

	static final String settingsTableGlobal = "content://settings/global";
    static final String settingsTableSecure = "content://settings/secure";
    static final String settingsTableSystem = "content://settings/system";
    static final String settingsWifiKey = "wifi_on";

    static final String screenshotRegex = "/storage/emulated/(\\d+)/Pictures/Screenshots/(.*).(png|jpg)";
    static final Pattern screenshotPattern = Pattern.compile(screenshotRegex);

    static final String screenshotRegex2 = "/storage/emulated/(\\d+)/Screenshots/(.*).(png|jpg)";
    static final Pattern screenshotPattern2 = Pattern.compile(screenshotRegex2);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_displayresult);

        makeTextViewRed = false;

		Intent i = this.getIntent();
		if (i == null)
			return;

		String receiverIntentAction = i.getAction();
		if (receiverIntentAction == null)
			return;
		
		String text = null;
		
		isBroadcastAction = i.getBooleanExtra(ITA_Constants.IS_BROADCAST_ACTION, false);
		
		if (isBroadcastAction) {
			action = i.getStringExtra(ITA_Constants.ACTION);
			packageName = i.getStringExtra(ITA_Constants.PACKAGE_NAME);
			componentName = i.getStringExtra(ITA_Constants.COMPONENT_NAME);
			isBroadcastAction = true;
		}

		else {
			packageName = i.getStringExtra(ITA_Constants.PACKAGE_NAME);
			componentName = i.getStringExtra(ITA_Constants.COMPONENT_NAME);
			isBroadcastAction = false;
		}

		Button b = (Button) this.findViewById(R.id.buttonActionString);
		if (isBroadcastAction)
			b.setText("Send Intent - " + action);
		else {
			b.setText("Send Intent - " + packageName + "/" + componentName);
		}
		
		if (receiverIntentAction.equals(ITA_Constants.PROCESS_CRASH_DISPLAY_RESULT)) {
			init();

			try {
				Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_LOG_START_DISPLAY_RESULT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (isBroadcastAction) {
				text = getTextForAction(action);
			}
			else {
				text = getTextForComponent(packageName, componentName);
			}
		}
		else if (receiverIntentAction.equals(ITA_Constants.POTENTIAL_PRIVILEGE_ESCALATION_DISPLAY_RESULT)) {
			text = getTextForPotentialPrivilegeEscalation(action, packageName, componentName);
		}		

		TextView tv = (TextView) this.findViewById(R.id.logResultsTextView);
		if (makeTextViewRed)
            tv.setTextColor(Color.RED);
		tv.setText(text);
		tv.setMovementMethod(new ScrollingMovementMethod());

	}

	void init() {

		Log.w(TAG, "init");

		continueParsingLog = true;

		boolean createdReader = false;		
		for (int a = 0; a < 3 && createdReader == false; a++) {
			try {
				logcatProcess = ITA_IntentService.createLogcatProcess(true);
				br = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));				
				createdReader = true;
			} catch (IOException e) {
				e.printStackTrace();
				Log.w(TAG, "parseLogcatForCrash - failure creating logcat process");
				return;
			}			
		}

		if (br != null && createdReader) {			
			Runnable r = new Runnable() {
				@Override
				public void run() {
					try {
						ITA_DisplayResult.this.parseLogForCrash();
					} catch (IOException e) {
						e.printStackTrace();
					}					
				}				
			};
			logReaderThread = new Thread(r);
			logReaderThread.start();	
			Log.w(TAG, "parseLogForCrash thread started");
		}
		else {
			Log.w(TAG, "failed creating parseLogForCrash thread");
		}
	}


	public void parseLogForCrash() throws IOException {
		Log.w(TAG, "parseLogForCrash");
		String line = null;	
		while ((line = br.readLine()) != null && continueParsingLog) {	
			Matcher fatalExceptionMatcher = ITA_IntentService.fatalExceptionPattern.matcher(line);
			if (fatalExceptionMatcher.matches()) {
				Log.w(TAG, "parseLogcatForCrash - found fatal exception - " + line);
				foundException = true;
				return;
			}
			Matcher nativeCrashMatcher = ITA_IntentService.nativeCrashPattern.matcher(line);
			if (nativeCrashMatcher.matches()) {
				Log.w(TAG, "parseLogcatForCrash - found fatal native exception - " + line);
				foundException = true;
				return;				
			}
		}
	}

	protected void onDestroy() {
		super.onDestroy();

		if (isBroadcastAction) {
			Log.w(TAG, "onDestroy - action = " + action);		    
		}
		else {
			Log.w(TAG, "onDestroy - component = " + packageName + "/" + componentName);
		}

		// allow the thread to exit
		continueParsingLog = false;

		// kill the logcat process
		if (logcatProcess != null)
			logcatProcess.destroy();

		if (logReaderThread != null)
			logReaderThread.interrupt();
		
		logcatProcess = null;

		logReaderThread = null;
	}    
	
	
	public String getTextForPotentialPrivilegeEscalation(String action, String packageName, String componentName) {
		StringBuilder sb = new StringBuilder();
		ContentResolver cr = this.getContentResolver();

		String where = null;
		String[] selectionArgs = null;
		
		if (isBroadcastAction) {
			where = ITA_Contract.ACTION + " = ?";
			selectionArgs = new String[1];
			selectionArgs[0] = action;
			sb.append("Action: " + action + newLine);
			sb.append(newLine);
		}
		else {
			where = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
			selectionArgs = new String[2];
			selectionArgs[0] = packageName;
			selectionArgs[1] = componentName;
			sb.append("Package name: " + packageName + newLine);
			sb.append("Component name: " + componentName + newLine);
			sb.append(newLine);
		}
		
		Cursor cursor = cr.query(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, null, where, selectionArgs, null);    	
		try {

			//while (cursor != null && cursor.moveToNext()) {    		
			boolean moveToFirst = cursor.moveToFirst();
			if (moveToFirst) {    		
				do {
					
					String fileName = cursor.getString(cursor.getColumnIndex(ITA_Contract.NEW_FILE_AFTER_INTENT));
					int fileSize = cursor.getInt(cursor.getColumnIndex(ITA_Contract.FILE_SIZE));					
					String settingsTable = cursor.getString(cursor.getColumnIndex(ITA_Contract.SETTINGS_TABLE));
					String beforeSettingsValue = cursor.getString(cursor.getColumnIndex(ITA_Contract.BEFORE_SETTINGS_VALUE));					
					String beforeSettingsKey = cursor.getString(cursor.getColumnIndex(ITA_Contract.BEFORE_SETTINGS_KEY));
					String afterSettingsValue = cursor.getString(cursor.getColumnIndex(ITA_Contract.AFTER_SETTINGS_VALUE));					
					String afterSettingsKey = cursor.getString(cursor.getColumnIndex(ITA_Contract.AFTER_SETTINGS_KEY));					
					String beforePropertyValue = cursor.getString(cursor.getColumnIndex(ITA_Contract.BEFORE_PROPERTIES_VALUE));					
					String beforePropertyKey = cursor.getString(cursor.getColumnIndex(ITA_Contract.BEFORE_PROPERTIES_KEY));
					String afterPropertyValue = cursor.getString(cursor.getColumnIndex(ITA_Contract.AFTER_PROPERTIES_VALUE));					
					String afterPropertyKey = cursor.getString(cursor.getColumnIndex(ITA_Contract.AFTER_PROPERTIES_KEY));
					
					String potentialPrivilegeEscalationType = cursor.getString(cursor.getColumnIndex(ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE));
					componentType = cursor.getString(cursor.getColumnIndex(ITA_Contract.COMPONENT_TYPE));
										
					if (potentialPrivilegeEscalationType != null && !potentialPrivilegeEscalationType.isEmpty())
						sb.append("Potential Privilege Escalation Type: " + potentialPrivilegeEscalationType + newLine);
						
					if (fileName != null && !fileName.isEmpty()) {
						sb.append("File Created: " + fileName + newLine);
						sb.append("File Size: " + fileSize + newLine);
						
						LinearLayout linearLayout = (LinearLayout) findViewById(R.id.display_result_layout);
						LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				        linearLayout.setOrientation(LinearLayout.VERTICAL);
				        
				        Button button = new Button(this);
				        button.setText("Open file:" + fileName);
				        button.setOnClickListener(new View.OnClickListener() {
				            public void onClick(View v) {
				            		if (v == null)
				            			return;
				            		Button clickedButton = (Button) v;
				            		String text = clickedButton.getText().toString();
				            		String[] parsed = text.split("\\:", 2);
				            		if (parsed != null && parsed[1] != null) {
				            			String parsedFileName = parsed[1];
				            			Intent i = new Intent(Intent.ACTION_VIEW);
				            			//Uri uri = Uri.parse(parsedFileName);
				            			
				            			Uri uri = Uri.fromFile(new File(parsedFileName));
				            			
				            	        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
				            	        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());				            			
				            			i.setDataAndType(uri, mimeType);    
				            			
				            			try {
				            				ITA_DisplayResult.this.startActivity(i);
				            			}
				            			catch (Exception e) {
				            				e.printStackTrace();
				            				i.setDataAndType(uri, "plain/text");        				
					            			try {
					            				ITA_DisplayResult.this.startActivity(i);
					            			}
					            			catch (Exception e2) {
					            				e.printStackTrace();
					            			}					            				
				            			}				            			
				            		}				            	
				            }
				          });
				        button.setId(random.nextInt());				        
				        linearLayout.addView(button);


				        if (screenshotPattern.matcher(fileName).matches() || screenshotPattern2.matcher(fileName).matches()) {
                            Toast.makeText(getApplicationContext(), "This appears to be a screenshot", Toast.LENGTH_LONG).show();
                            makeTextViewRed = true;
                            sb.append("Note: Appears to be a screenshot" + newLine);
                        }
                        // TODO: handle additional file types and specific directories here
					}

					if (settingsTable != null && !settingsTable.isEmpty())
						sb.append("Settings URI: " + settingsTable + newLine);
					
					if (beforeSettingsKey != null && !beforeSettingsKey.isEmpty())
						sb.append("[before] Settings Key: " + beforeSettingsKey + newLine);
					
					if (beforeSettingsValue != null && !beforeSettingsValue.isEmpty())
						sb.append("[before] Settings Value: " + beforeSettingsValue + newLine);
					
					if (afterSettingsKey != null && !afterSettingsKey.isEmpty())
						sb.append("[after] Settings Key: " + afterSettingsKey + newLine);
					
					if (afterSettingsValue != null && !afterSettingsValue.isEmpty())
						sb.append("[after] Settings Value: " + afterSettingsValue + newLine);
					
					
					if (beforePropertyKey != null && !beforePropertyKey.isEmpty())
						sb.append("[before] Property Key: " + beforePropertyKey + newLine);
					
					if (beforePropertyValue != null && !beforePropertyValue.isEmpty())
						sb.append("[before] Property Value: " + beforePropertyValue + newLine);
					
					if (afterPropertyKey != null && !afterPropertyKey.isEmpty())
						sb.append("[after] Property Key: " + afterPropertyKey + newLine);
					
					if (afterPropertyValue != null && !afterPropertyValue.isEmpty())
						sb.append("[after] Property Value: " + afterPropertyValue + newLine);

					if (settingsTable != null && afterSettingsKey != null && afterSettingsValue != null) {
					    if (settingsTable.equals(settingsTableGlobal) && afterSettingsKey.equals(settingsWifiKey)) {
					        if (afterSettingsValue.equals("1")) {

                                Toast.makeText(getApplicationContext(), "This appears to have enabled Wi-Fi", Toast.LENGTH_LONG).show();
                                makeTextViewRed = true;
                                sb.append("Note: Appears to have enabled Wi-Fi" + newLine);
                            }
                            else if (afterSettingsValue.equals("0")) {
                                Toast.makeText(getApplicationContext(), "This appears to have disabled Wi-Fi", Toast.LENGTH_LONG).show();
                                sb.append("Note: Appears to have disabled Wi-Fi" + newLine);
                                makeTextViewRed = true;
                            }
                        }
                    }
					
					sb.append(newLine);
			
				} while (cursor.moveToNext());
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}
		return sb.toString();
	}
	

	public String getTextForAction(String action) {
		StringBuilder sb = new StringBuilder();
		ContentResolver cr = this.getContentResolver();

		String where = ITA_Contract.ACTION + " = ?";
		String[] selectionArgs = {action};

		Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, where, selectionArgs, null);    	
		int count = 0;
		try {

			//while (cursor != null && cursor.moveToNext()) {    		
			boolean moveToFirst = cursor.moveToFirst();
			if (moveToFirst) {    		
				do {
					if (count > 0)
						sb.append(newLine);

					int instanceTypeColumn = cursor.getColumnIndex(ITA_Contract.INSTANCE_TYPE);
					int processNameColumn = cursor.getColumnIndex(ITA_Contract.PROCESS_NAME);
					int exceptionNameColumn = cursor.getColumnIndex(ITA_Contract.EXCEPTION_NAME);
					int exceptionReasonColumn = cursor.getColumnIndex(ITA_Contract.EXCEPTION_REASON);
					int logEventsColumn = cursor.getColumnIndex(ITA_Contract.LOG_EVENT_MESSAGES);
					int logPathColumn = cursor.getColumnIndex(ITA_Contract.LOG_PATH);
					int pidColumn = cursor.getColumnIndex(ITA_Contract.PID);
					int componentTypeColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_TYPE);
					int baseComponentTypeColumn = cursor.getColumnIndex(ITA_Contract.BASE_COMPONENT_TYPE);

					String instanceType = cursor.getString(instanceTypeColumn);
					String processName = cursor.getString(processNameColumn);
					String exceptionName = cursor.getString(exceptionNameColumn);
					String exceptionReason = cursor.getString(exceptionReasonColumn);
					String logEvents = cursor.getString(logEventsColumn);
					String logPath = cursor.getString(logPathColumn);
					int pid = cursor.getInt(pidColumn);
					componentType = cursor.getString(componentTypeColumn);
					baseComponentType = cursor.getString(baseComponentTypeColumn);							

					sb.append("Crash type: " + instanceType + newLine);
					sb.append("Component: " + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + newLine);
					sb.append("Process: " + processName + newLine);
					sb.append("Exception: " + exceptionName + newLine);
					sb.append("Action: " + action + newLine);
					
					if (baseComponentType != null && !baseComponentType.isEmpty()) {
						sb.append("Base component type: " + baseComponentType + newLine);
						sb.append("Package name: " + packageName + newLine);
						sb.append("Component name: " + componentName + newLine);
					}					
					sb.append("Reason: " + exceptionReason + newLine);
					sb.append("Stacktrace: " + logEvents + newLine);
					sb.append("Log file path: " + logPath + newLine);
					sb.append("PID: " + pid + newLine);

					count++;    				
				} while (cursor.moveToNext());
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}

		return sb.toString();
	}

	public String getTextForComponent(String packageName, String componetName) {

		StringBuilder sb = new StringBuilder();

		ContentResolver cr = this.getContentResolver();

		String where = ITA_Contract.PACKAGE_NAME + " = ? AND " + ITA_Contract.COMPONENT_NAME + " = ?";
		String[] selectionArgs = {packageName, componentName};

		Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, where, selectionArgs, null);    	
		int count = 0;
		try {
			//while (cursor != null && cursor.moveToNext()) {
			boolean moveToFirst = cursor.moveToFirst();
			if (moveToFirst) {   

				do {
					if (count > 0)
						sb.append(newLine);

					int instanceTypeColumn = cursor.getColumnIndex(ITA_Contract.INSTANCE_TYPE);
					int processNameColumn = cursor.getColumnIndex(ITA_Contract.PROCESS_NAME);
					int exceptionNameColumn = cursor.getColumnIndex(ITA_Contract.EXCEPTION_NAME);
					int exceptionReasonColumn = cursor.getColumnIndex(ITA_Contract.EXCEPTION_REASON);
					int logEventsColumn = cursor.getColumnIndex(ITA_Contract.LOG_EVENT_MESSAGES);
					int logPathColumn = cursor.getColumnIndex(ITA_Contract.LOG_PATH);
					int pidColumn = cursor.getColumnIndex(ITA_Contract.PID);
					int componentTypeColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_TYPE);

					sb.append("Package name: " + packageName + newLine);
					sb.append("Component name: " + componentName + newLine);

					String instanceType = cursor.getString(instanceTypeColumn);
					String processName = cursor.getString(processNameColumn);
					String exceptionName = cursor.getString(exceptionNameColumn);
					String exceptionReason = cursor.getString(exceptionReasonColumn);
					String logEvents = cursor.getString(logEventsColumn);
					String logPath = cursor.getString(logPathColumn);
					int pid = cursor.getInt(pidColumn);
					componentType = cursor.getString(componentTypeColumn);

					sb.append("Crash type: " + instanceType + newLine);
					sb.append("Component: " + componentType + newLine);
					sb.append("Process: " + processName + newLine);
					sb.append("Exception: " + exceptionName + newLine);
					sb.append("Reason: " + exceptionReason + newLine);
					sb.append("Stacktrace: " + logEvents + newLine);
					sb.append("Log file path: " + logPath + newLine);
					sb.append("PID: " + pid + newLine);

					count++;    				
				} while (cursor.moveToNext());
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}
		return sb.toString();
	}

	public void sendIntent(View v) {

		foundException = false;

		if (isBroadcastAction) {
			Toast.makeText(this.getApplicationContext(), "Sending intent: " + action, Toast.LENGTH_LONG).show();
			Log.w(TAG, "Sending intent: " + action);
		}			
		else {
			Toast.makeText(this.getApplicationContext(), "Sending intent: " + packageName + "/" + componentName, Toast.LENGTH_LONG).show();
			Log.w(TAG, "Sending intent: " + packageName + "/" + componentName);
		}

		try {			
			if (componentType.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {

				if (ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))	
					ITA_IntentService.createBaselineDynamicBroadcastReceiversForPackage(packageName, getApplicationContext());
				
				Intent i = new Intent();
				i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				i.setClassName(packageName, componentName);
				try {
					startActivity(i);
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
					i.setAction("");
					startActivity(i);
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
					i.putExtras(emptyBundle);
					startActivity(i);

					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}					
					i.setData(ITA_Constants.SCHEMLESS_URI);
					startActivity(i);	
					
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
					
					if (ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))	
						ITA_IntentService.restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (componentType.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
				
				if (ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))	
					ITA_IntentService.createBaselineDynamicBroadcastReceiversForPackage(packageName, getApplicationContext());
				
				Intent i = new Intent();
				i.setClassName(packageName, componentName);
				try {
					startService(i);
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}

					i.setAction("");
					startService(i);
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}

					i.putExtras(emptyBundle);
					startService(i);			
					
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
										
					i.setData(ITA_Constants.SCHEMLESS_URI);
					startService(i);
					
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
					
					if (ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))	
						ITA_IntentService.restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
					
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			else if (componentType.equals(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME)) {
				
				if (ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))	
					ITA_IntentService.createBaselineDynamicBroadcastReceiversForPackage(packageName, getApplicationContext());
				
				Intent i = new Intent();
				i.setClassName(packageName, componentName);
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
					this.sendBroadcast(i);
					
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
					
					
					if (ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING))	
						ITA_IntentService.restestDynamicallyRegisteredBroadcastReceivers(getApplicationContext(), packageName, componentName, componentType);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (componentType.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
				
				
				if (baseComponentType != null && !baseComponentType.isEmpty()) {
					Intent i = new Intent();
					i.setClassName(packageName, componentName);
					
					try {
						if (baseComponentType.equals(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME)) {
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
							startActivity(i);
						}
						else if (baseComponentType.equals(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME)) {
							startService(i);
						}					
						else if (baseComponentType.equals(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME)) {
							sendBroadcast(i);
						}						
					} catch (Exception e) {
						e.printStackTrace();
					}
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_AFTER_CREATE_COMPONENT);
				}
				
				
				Intent i = new Intent(action);
				try {
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
					
					Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);
					
					if (foundException == true) {
						Log.w(TAG, "foundException - return");
						foundException = false;
						return;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}								
			}
			else if (componentType.equals(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME)) {
				String[] authorities = ITA_IntentService.getAuthoritiesFromProvider(packageName, componentName, getPackageManager());
				if (authorities == null)
					return;
				ContentResolver cr = getContentResolver();
				for (int a=0 ; a < authorities.length; a++) {
					String authority = authorities[a];
					Uri baseUri = Uri.parse("content://" + authority);
					ITA_IntentService.accessProvider(baseUri,componentName, cr, authority, getApplicationContext(), packageName, componentName);
				}
			}			
			else {
				// component type will be unavailable so try everything
				// occurs when a system crash doesn't get written to the
				// logs but was detected by a system failure
				Intent i = new Intent();

				if (isBroadcastAction) {

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
						
					}
					catch (Exception e) {
						e.printStackTrace();
					}					
				}					
				else {
					i.setClassName(packageName, componentName);
					i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					try {
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
						startActivity(i);		
						
						Thread.sleep(ITA_Constants.MILLISECONDS_TO_SLEEP_INBETWEEN_SAME_INTENT_DISPLAY_RESULT);

						if (foundException == true) {
							Log.w(TAG, "foundException - return");
							foundException = false;
							return;
						}
						i.setData(ITA_Constants.SCHEMLESS_URI);
						this.startActivity(i);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
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
						this.sendBroadcast(i);
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						String[] authorities = ITA_IntentService.getAuthoritiesFromProvider(packageName, componentName, getPackageManager());
						if (authorities == null)
							return;
						ContentResolver cr = getContentResolver();
						
						for (int a=0 ; a < authorities.length; a++) {
							Uri baseUri = Uri.parse("content://" + authorities[a]);
							ITA_IntentService.accessProvider(baseUri, componentType, cr, authorities[a], getApplicationContext(), packageName, componentName);						
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
