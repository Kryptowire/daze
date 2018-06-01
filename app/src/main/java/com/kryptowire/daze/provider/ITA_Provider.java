package com.kryptowire.daze.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class ITA_Provider extends ContentProvider {

	private ITA_DBHelper dbHelper;
	private SQLiteDatabase db;
	static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	final static int ACTIVITY_TABLE_ALL = 1;
	final static int ACTIVITY_TABLE_WITH_ID = 2;
	final static int SERVICE_TABLE_ALL = 3;
	final static int SERVICE_TABLE_WITH_ID = 4;
	final static int RECEIVER_TABLE_ALL = 5;
	final static int RECEIVER_TABLE_WITH_ID = 6;
	final static int BROADCAST_ACTION_TABLE_ALL = 7;
	final static int BROADCAST_ACTION_TABLE_WITH_ID = 8;
	final static int PROVIDER_TABLE_ALL = 9;
	final static int PROVIDER_TABLE_WITH_ID = 10;	
	final static int RESULTS_TABLE_ALL = 11;
	final static int RESULTS_TABLE_WITH_ID = 12;
	final static int APPS_TABLE_ALL = 13;
	final static int APPS_TABLE_WITH_ID = 14;
	final static int TIME_TABLE_ALL = 15;
	final static int TIME_TABLE_WITH_ID = 16;
	final static int POTENTIAL_PRIVILEGE_ESCALATION_TABLE_ALL = 17;
	final static int POTENTIAL_PRIVILEGE_ESCALATION_TABLE_WITH_ID = 18;
	final static int ALL_COMPONENTS_TABLE_ALL = 19;
	final static int ALL_COMPONENTS_TABLE_WITH_ID = 20;

	static {
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, ACTIVITY_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME + "/#", ACTIVITY_TABLE_WITH_ID);        
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, SERVICE_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ServiceTable.SERVICE_TABLE_NAME + "/#", SERVICE_TABLE_WITH_ID);        
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, RECEIVER_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + "/#", RECEIVER_TABLE_WITH_ID);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, BROADCAST_ACTION_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + "/#", BROADCAST_ACTION_TABLE_WITH_ID);		
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, PROVIDER_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME + "/#", PROVIDER_TABLE_WITH_ID);		
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, RESULTS_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.ResultsTable.RESULTS_TABLE_NAME + "/#", RESULTS_TABLE_WITH_ID);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.AppsTable.APPS_TABLE_NAME, APPS_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.AppsTable.APPS_TABLE_NAME + "/#", APPS_TABLE_WITH_ID);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.TimeTable.TIME_TABLE_NAME, TIME_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.TimeTable.TIME_TABLE_NAME + "/#", TIME_TABLE_WITH_ID);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, POTENTIAL_PRIVILEGE_ESCALATION_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME + "/#", POTENTIAL_PRIVILEGE_ESCALATION_TABLE_WITH_ID);		
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, ALL_COMPONENTS_TABLE_ALL);
		sURIMatcher.addURI(ITA_Contract.AUTHORITY, ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME + "/#", ALL_COMPONENTS_TABLE_WITH_ID);
	}	

	@Override
	public boolean onCreate() {		
		dbHelper = new ITA_DBHelper(getContext());
		db = dbHelper.getWritableDatabase();
		return (db == null) ? false:true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor retCursor;
		switch(sURIMatcher.match(uri)) {
		case ACTIVITY_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case ACTIVITY_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}			
		case SERVICE_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case SERVICE_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}			
		case RECEIVER_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case RECEIVER_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case BROADCAST_ACTION_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case BROADCAST_ACTION_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case PROVIDER_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case PROVIDER_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case RESULTS_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case RESULTS_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case APPS_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.AppsTable.APPS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case APPS_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.AppsTable.APPS_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case TIME_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.TimeTable.TIME_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case TIME_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.TimeTable.TIME_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		case ALL_COMPONENTS_TABLE_ALL:{
			retCursor = db.query(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
			return retCursor;
		}
		case ALL_COMPONENTS_TABLE_WITH_ID: {
			retCursor = db.query(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, projection, "_id = ?", new String[] {String.valueOf(ContentUris.parseId(uri))}, null, null, sortOrder);
			return retCursor;				
		}
		default: {
			break;
		}
		}
		return null;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri returnUri;
		switch(sURIMatcher.match(uri)) {
		case ACTIVITY_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.ActivityTable.buildActivityTableUri(_id);
				return returnUri;
			}
			break;
		}
		case SERVICE_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.ServiceTable.buildServiceTableUri(_id);
				return returnUri;
			}
			break;
		}
		case RECEIVER_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.ReceiverTable.buildReceiverTableUri(_id);						
				return returnUri;
			}
			break;
		}
		case BROADCAST_ACTION_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.BroadcastActionTable.buildBroadcastActionTableUri(_id);
				return returnUri;
			}
			break;
		}		
		case PROVIDER_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.ProviderTable.buildProviderTableUri(_id);
				return returnUri;
			}
			break;
		}
		case RESULTS_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.ResultsTable.buildResultsTableUri(_id);
				return returnUri;
			}	
			break;
		}
		case APPS_TABLE_ALL : {
			long _id = db.insert(ITA_Contract.AppsTable.APPS_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.AppsTable.buildAppsTableUri(_id);
				return returnUri;
			}	
			break;
		}
		case TIME_TABLE_ALL : {
			long _id = db.insert(ITA_Contract.TimeTable.TIME_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.TimeTable.buildTimeTableUri(_id);
				return returnUri;
			}	
			break;
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.PotentialPrivilegeEscalationTable.buildPotentialPrivilegeEscalationTableUri(_id);
				return returnUri;
			}	
			break;			
		}		
		case ALL_COMPONENTS_TABLE_ALL: {
			long _id = db.insert(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, null, values);			
			if (_id > 0) {
				returnUri = ITA_Contract.PotentialPrivilegeEscalationTable.buildPotentialPrivilegeEscalationTableUri(_id);
				return returnUri;
			}	
			break;			
		}
		default: {break;}
		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int numDeleted = 0;		
		switch(sURIMatcher.match(uri)){
		case ACTIVITY_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME + "';");			
			break;
		}
		case ACTIVITY_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME,ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case SERVICE_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.ServiceTable.SERVICE_TABLE_NAME + "';");
			break;
		}
		case SERVICE_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case RECEIVER_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME + "';");
			break;
		}
		case RECEIVER_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case BROADCAST_ACTION_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME + "';");
			break;
		}
		case BROADCAST_ACTION_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case PROVIDER_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME + "';");
			break;
		}
		case PROVIDER_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case RESULTS_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.ResultsTable.RESULTS_TABLE_NAME + "';");
			break;
		}
		case RESULTS_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case APPS_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.AppsTable.APPS_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.AppsTable.APPS_TABLE_NAME + "';");
			break;
		}
		case APPS_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.AppsTable.APPS_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case TIME_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.TimeTable.TIME_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.TimeTable.TIME_TABLE_NAME + "';");
			break;
		}
		case TIME_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.TimeTable.TIME_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME + "';");
			break;
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}		
		case ALL_COMPONENTS_TABLE_ALL:{
			numDeleted = db.delete(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, selection, selectionArgs);
			db.execSQL("delete from sqlite_sequence where name='" + ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME + "';");
			break;
		}
		case ALL_COMPONENTS_TABLE_WITH_ID: {
			numDeleted = db.delete(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}	
		default:{}
		}
		return numDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int numUpdated = 0;
		if (values == null)
			return numUpdated;
		switch(sURIMatcher.match(uri)){
		case ACTIVITY_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case ACTIVITY_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case SERVICE_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case SERVICE_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.ServiceTable.SERVICE_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case RECEIVER_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case RECEIVER_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case BROADCAST_ACTION_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case BROADCAST_ACTION_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}		
		case PROVIDER_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case PROVIDER_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case RESULTS_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case RESULTS_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.ResultsTable.RESULTS_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case APPS_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.AppsTable.APPS_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case APPS_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.AppsTable.APPS_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case TIME_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.TimeTable.TIME_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case TIME_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.TimeTable.TIME_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case POTENTIAL_PRIVILEGE_ESCALATION_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		case ALL_COMPONENTS_TABLE_ALL:{
			numUpdated = db.update(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		case ALL_COMPONENTS_TABLE_WITH_ID: {
			numUpdated = db.update(ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME, values, ITA_Contract._ID + " = ?", new String[] {String.valueOf(ContentUris.parseId(uri))});
			break;
		}
		default:{
			return numUpdated;
		}
		}
		return numUpdated;
	}
}
