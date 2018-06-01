package com.kryptowire.daze.receiver;

import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.service.ITA_IntentService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ITA_BootReceiver extends BroadcastReceiver {

	final static String TAG = ITA_BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.w(TAG, "onReceive");

		if (intent == null) 
			return;
		String action = intent.getAction(); 
		if (action == null)
			return;

		if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
			Log.w(TAG, "Action Received = " + action);

			boolean isSendingIntents = ITA_IntentService.readBooleanSharedPreferences(context, ITA_Constants.PERFORMING_ANALYSIS);
			boolean isAnalyzingLogs = ITA_IntentService.readBooleanSharedPreferences(context, ITA_Constants.ANALYZING_LOGS);
			
			Log.w(TAG, "isSendingIntents = " + isSendingIntents + ", isAnalyzingLogs = " + isAnalyzingLogs);

			if (isSendingIntents) {
				Intent i = new Intent(context, ITA_IntentService.class);
				i.setAction(action);
				context.startService(i);
			}
			else if (isAnalyzingLogs) {
				Intent i = new Intent(context, ITA_IntentService.class);
				i.setAction(ITA_Constants.ANALYZE_LOGS);				
				context.startService(i);
			}
		}
	}
}
