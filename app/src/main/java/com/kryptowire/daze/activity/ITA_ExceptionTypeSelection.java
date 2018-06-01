package com.kryptowire.daze.activity;

import java.util.ArrayList;
import java.util.Collections;

import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;
import com.kryptowire.daze.provider.ITA_Contract;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class ITA_ExceptionTypeSelection extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exceptiontypeselection);
	}

	public void allCrashes(View v) {

		ArrayList<String> allCrashes = this.getAllCrashes();

		if (allCrashes.size() == 0) {			
			Toast.makeText(this.getApplicationContext(), "There are no results for process crashes", Toast.LENGTH_LONG).show();
			return;
		}

		Intent i = new Intent(this.getApplicationContext(), ITA_CrashSelection.class);		
		Bundle b = new Bundle();
		b.putStringArrayList(ITA_Constants.CRASHES_LIST, allCrashes);	
		i.setAction(ITA_Constants.DISPLAY_CRASHES);
		i.putExtras(b);
		this.startActivity(i);
	}

	public void systemCrashes(View v) {

		ArrayList<String> systemCrashes = getSystemCrashes(this.getContentResolver());

		if (systemCrashes.size() == 0) {			
			Toast.makeText(getApplicationContext(), "There are no results for system crashes", Toast.LENGTH_LONG).show();
			return;
		}

		Intent i = new Intent(getApplicationContext(), ITA_CrashSelection.class);		
		Bundle b = new Bundle();
		b.putStringArrayList(ITA_Constants.CRASHES_LIST, systemCrashes);	
		i.setAction(ITA_Constants.DISPLAY_CRASHES);
		i.putExtras(b);
		this.startActivity(i);


	}
	
	public void examinePotentialPrivilegeEscalation(View v) {
		
		ArrayList<String> ppe = getPotentialPrivilegeEscalation();
		
		if (ppe.size() == 0) {			
			Toast.makeText(this.getApplicationContext(), "There are no results for potential privilege escalation", Toast.LENGTH_LONG).show();
			return;
		}
		
		Intent i = new Intent(this.getApplicationContext(), ITA_SystemMonitoring.class);
		Bundle b = new Bundle();
		b.putStringArrayList(ITA_Constants.POTENTIAL_PRIVILEGE_ESCALATION_LIST, ppe);	
		i.setAction(ITA_Constants.DISPLAY_POTENTIAL_PRIVILEGE_ESCALATION);
		i.putExtras(b);
		this.startActivity(i);
		
		
	}
	

	public void processCrashes(View v) {

		ArrayList<String> processCrashes = getProcessCrashes(getContentResolver());

		if (processCrashes.size() == 0) {			
			Toast.makeText(this.getApplicationContext(), "There are no results for process crashes", Toast.LENGTH_LONG).show();
			return;
		}

		Intent i = new Intent(this.getApplicationContext(), ITA_CrashSelection.class);		
		Bundle b = new Bundle();
		b.putStringArrayList(ITA_Constants.CRASHES_LIST, processCrashes);	
		i.setAction(ITA_Constants.DISPLAY_CRASHES);
		i.putExtras(b);
		this.startActivity(i);
	}
	
	
	private ArrayList<String> getPotentialPrivilegeEscalation() {
		ArrayList<String> results = new ArrayList<String>();
		ContentResolver cr = this.getContentResolver();
		Cursor cursor = cr.query(ITA_Contract.PotentialPrivilegeEscalationTable.CONTENT_URI, null, null, null, null);    	
		try {
			while (cursor != null && cursor.moveToNext()) {

				int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);
				int packageNameColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
				int componentNameColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);

				String action = cursor.getString(actionColumn);
				String packageName = cursor.getString(packageNameColumn);
				String componentName = cursor.getString(componentNameColumn);

				if (action != null && !action.equals("")) {
					if (!results.contains(action))    				
						results.add(action);
				}
				else {
					String component = packageName + "/" + componentName;
					if (!results.contains(component))
						results.add(component);
				}
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		} 
		return results;
	}

	private ArrayList<String> getAllCrashes() {

		ArrayList<String> results = new ArrayList<String>();
		ContentResolver cr = this.getContentResolver();
		Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, null, null, null);    	

		try {
			
			boolean moveToFirst = cursor.moveToFirst();
			if (moveToFirst) {    		
				do {
					int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);					
					int packageNameColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
					int componentNameColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);

					String action = cursor.getString(actionColumn);
					String packageName = cursor.getString(packageNameColumn);
					String componentName = cursor.getString(componentNameColumn);

					if (action != null && !action.equals("")) {
						String builtString = action;
						if (packageName != null && !packageName.isEmpty()) {
							builtString = builtString + " (" + packageName + "/" + componentName + ")";
						}						
						if (!results.contains(builtString))    				
							results.add(builtString);
					}
					else {
						String component = packageName + "/" + componentName;
						if (!results.contains(component))
							results.add(component);
					}					
				} while (cursor != null && cursor.moveToNext());
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}    	
		Collections.sort(results);    	
		return results;
	}

	public static ArrayList<String> getProcessCrashes(ContentResolver cr) {

		ArrayList<String> results = new ArrayList<String>();


		String where = ITA_Contract.INSTANCE_TYPE + " = ?";
		String[] selectionArgs = {ITA_Constants.PROCESS_CRASH};

		Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, where, selectionArgs, null);    	

		try {
			
			boolean moveToFirst = cursor.moveToFirst();
			if (moveToFirst) {    		
				do {
					int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);
					int packageNameColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
					int componentNameColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);

					String action = cursor.getString(actionColumn);
					String packageName = cursor.getString(packageNameColumn);
					String componentName = cursor.getString(componentNameColumn);

					if (action != null && !action.equals("")) {
						String builtString = action;
						if (packageName != null && !packageName.isEmpty()) {
							builtString = builtString + " (" + packageName + "/" + componentName + ")";
						}						
						if (!results.contains(builtString))    				
							results.add(builtString);
					}
					else {
						String component = packageName + "/" + componentName;
						if (!results.contains(component))
							results.add(component);
					}					
				} while (cursor != null && cursor.moveToNext());
			}	
		} finally {
			if (cursor != null)			
				cursor.close();
		}

		Collections.sort(results);    
		return results;
	}


	public static ArrayList<String> getSystemCrashes(ContentResolver cr) {

		ArrayList<String> results = new ArrayList<String>();

		String where = ITA_Contract.INSTANCE_TYPE + " = ?";
		String[] selectionArgs = {ITA_Constants.SYSTEM_CRASH};

		Cursor cursor = cr.query(ITA_Contract.ResultsTable.CONTENT_URI, null, where, selectionArgs, null);    	

		try {
			
			boolean moveToFirst = cursor.moveToFirst();
			if (moveToFirst) {    		
				do {
					int actionColumn = cursor.getColumnIndex(ITA_Contract.ACTION);
					int packageNameColumn = cursor.getColumnIndex(ITA_Contract.PACKAGE_NAME);
					int componentNameColumn = cursor.getColumnIndex(ITA_Contract.COMPONENT_NAME);

					String action = cursor.getString(actionColumn);
					String packageName = cursor.getString(packageNameColumn);
					String componentName = cursor.getString(componentNameColumn);

					if (action != null && !action.equals("")) {
						String builtString = action;
						if (packageName != null && !packageName.isEmpty()) {
							builtString = builtString + " (" + packageName + "/" + componentName + ")";
						}						
						if (!results.contains(builtString))    				
							results.add(builtString);
					}
					else {
						String component = packageName + "/" + componentName;
						if (!results.contains(component))
							results.add(component);
					}
	
				} while (cursor != null && cursor.moveToNext());
			}
		} finally {
			if (cursor != null)			
				cursor.close();
		}
		Collections.sort(results);    
		return results;
	}




}

