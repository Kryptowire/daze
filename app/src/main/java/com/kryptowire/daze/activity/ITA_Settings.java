package com.kryptowire.daze.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;
import com.kryptowire.daze.service.ITA_IntentService;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

public class ITA_Settings extends Activity {

	CheckBox activityCB = null;
	CheckBox serviceCB = null;
	CheckBox receiverCB = null;
	CheckBox broadcastActionCB = null;
	CheckBox providerCB = null;
	CheckBox systemServerCB = null;
	CheckBox acceleratedCB = null;
	CheckBox oneStacktraceCB = null;
	CheckBox oneStacktracePerCrashCB = null;
	CheckBox privilegeEscalationCB = null;
	CheckBox dynamicBroadcastTestingCB = null;
	final static String ACCELERATED_PRIVILEGE_ESCALATION_AND_DYN_BROADS_WARNING = "Accelerated testing cannot be used with either checking for possible privilege escalation or testing for dynamically regsitered broadcast receivers";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		activityCB = (CheckBox) this.findViewById(R.id.analyzeActivities);
		serviceCB = (CheckBox) this.findViewById(R.id.analyzeServices);
		receiverCB = (CheckBox) this.findViewById(R.id.analyzeBroadcastReceivers);
		broadcastActionCB = (CheckBox) this.findViewById(R.id.analyzeBroadcastActions);
		providerCB = (CheckBox) this.findViewById(R.id.analyzeProviders);
		systemServerCB = (CheckBox) this.findViewById(R.id.systemServer);
		acceleratedCB = (CheckBox) this.findViewById(R.id.accelerated);
		oneStacktraceCB = (CheckBox) this.findViewById(R.id.limitStacktrace);
		oneStacktracePerCrashCB = (CheckBox) this.findViewById(R.id.limitStacktracePerCrash);
		privilegeEscalationCB = (CheckBox) this.findViewById(R.id.privilegeEscalationCheck);
		dynamicBroadcastTestingCB = (CheckBox) this.findViewById(R.id.aggressiveDynamicBroadcastTesting);

		boolean activity = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_ACTIVITY);
		boolean service = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_SERVICE);
		boolean receiver = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_RECEIVER);
		boolean broadcastAction = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_BROADCASTACTION);
		boolean provider = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_PROVIDER);
		boolean onlySystemServer = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER);
		boolean acceleratedTesting = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING);
		boolean oneStacktrace = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PREVENT_RECURRING_FATAL_ERRORS);
		boolean oneStacktracePerCrash = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH);
		boolean checkForPrivilegeEscalation = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.CHECK_FOR_PRIVILEGE_ESCALTION);
		boolean aggressiveBroadcastReceiverTesting = ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING);
		
		activityCB.setChecked(activity);
		serviceCB.setChecked(service);
		receiverCB.setChecked(receiver);
		broadcastActionCB.setChecked(broadcastAction);
		systemServerCB.setChecked(onlySystemServer);
		acceleratedCB.setChecked(acceleratedTesting);
		oneStacktraceCB.setChecked(oneStacktrace);
		oneStacktracePerCrashCB.setChecked(oneStacktracePerCrash);
		providerCB.setChecked(provider);
		privilegeEscalationCB.setChecked(checkForPrivilegeEscalation);
		dynamicBroadcastTestingCB.setChecked(aggressiveBroadcastReceiverTesting);
	}	

	public void targetIndividualApp(View view) {
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

	public void onCheckboxClicked(View view) {
		CheckBox cb = (CheckBox) view;		
		boolean checked = cb.isChecked();	    
		String cbtext = cb.getText().toString();
		switch(view.getId()) {
		case R.id.analyzeActivities:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_ACTIVITY, checked);	             
			break;
		case R.id.analyzeServices:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_SERVICE, checked);	             
			break;
		case R.id.analyzeBroadcastReceivers:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_RECEIVER, checked);	             
			break;
		case R.id.analyzeBroadcastActions:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_BROADCASTACTION, checked);	             
			break;
		case R.id.analyzeProviders:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.TEST_PROVIDER, checked);	             
			break;	
		case R.id.systemServer:	        	
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONLY_SYSTEM_SERVER, checked);	             
			break;
		case R.id.accelerated:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING, checked);		
			// this check is only possible if accelerated testing is disabled
			boolean showWarningToast = false;
			if (checked == true && ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.CHECK_FOR_PRIVILEGE_ESCALTION)) {				
				privilegeEscalationCB.setChecked(false);
				ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.CHECK_FOR_PRIVILEGE_ESCALTION, false);
				showWarningToast = true;
			}			
			if (checked == true && ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING)) {				
				dynamicBroadcastTestingCB.setChecked(false);
				ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING, false);
				showWarningToast = true;
			}			
			if (showWarningToast == true)
				Toast.makeText(this.getApplicationContext(), ACCELERATED_PRIVILEGE_ESCALATION_AND_DYN_BROADS_WARNING, Toast.LENGTH_SHORT).show();			
			break;
		case R.id.limitStacktrace:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.PREVENT_RECURRING_FATAL_ERRORS, checked);			
			break;			
		case R.id.limitStacktracePerCrash:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ONE_STACKTRACE_PER_CRASH, checked);			
			break;
		case R.id.privilegeEscalationCheck:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.CHECK_FOR_PRIVILEGE_ESCALTION, checked);
			// this check is only possible if accelerated testing is disabled				
			if (checked == true && ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING)) {
				acceleratedCB.setChecked(false);
				ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING, false);
				Toast.makeText(this.getApplicationContext(), ACCELERATED_PRIVILEGE_ESCALATION_AND_DYN_BROADS_WARNING, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.aggressiveDynamicBroadcastTesting:
			Toast.makeText(this.getApplicationContext(), cbtext, Toast.LENGTH_SHORT).show();
			ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.AGGRESSIVE_BROADCAST_RECEIVER_TESTING, checked);
			// this check is only possible if accelerated testing is disabled				
			if (checked == true && ITA_IntentService.readBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING)) {
				acceleratedCB.setChecked(false);
				ITA_IntentService.writeBooleanSharedPreferences(getApplicationContext(), ITA_Constants.ACCELERATED_TESTING, false);
				Toast.makeText(this.getApplicationContext(), ACCELERATED_PRIVILEGE_ESCALATION_AND_DYN_BROADS_WARNING, Toast.LENGTH_SHORT).show();
			}

			break;
		default:
			break;			
		}
	}
}
