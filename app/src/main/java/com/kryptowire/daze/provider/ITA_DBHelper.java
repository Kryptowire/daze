package com.kryptowire.daze.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ITA_DBHelper extends SQLiteOpenHelper{
		
	public static final String DBNAME = "intents.db";
	public static final int VERSION = 1;
	
	private static final String SQL_CREATE_ACTIVITY_TABLE = "CREATE TABLE " +
			ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME +
		    "(" +
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.DYNAMIC_BROADCAST_RECEIVERS_REGISTERED + " TEXT, " +
		    ITA_Contract.PERMISSION + " TEXT, " +
		    ITA_Contract.SKIPPED + " INTEGER, " +
		    ITA_Contract.UID + " INTEGER, " +
		    ITA_Contract.SKIPPED_DYNAMIC_BROADCAST_ACTIONS + " TEXT, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +			    
		    ITA_Contract.VERSION_CODE + " TEXT " +
		    ");";

	private static final String SQL_CREATE_SERVICE_TABLE = "CREATE TABLE " +
			ITA_Contract.ServiceTable.SERVICE_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.DYNAMIC_BROADCAST_RECEIVERS_REGISTERED + " TEXT, " +
		    ITA_Contract.PERMISSION + " TEXT, " +
		    ITA_Contract.SKIPPED + " INTEGER, " +
		    ITA_Contract.UID + " INTEGER, " +
		    ITA_Contract.SKIPPED_DYNAMIC_BROADCAST_ACTIONS + " TEXT, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT " +
		    ");";

	private static final String SQL_CREATE_RECEIVER_TABLE = "CREATE TABLE " +
			ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.DYNAMIC_BROADCAST_RECEIVERS_REGISTERED + " TEXT, " +
		    ITA_Contract.PERMISSION + " TEXT, " +
		    ITA_Contract.SKIPPED + " INTEGER, " +
		    ITA_Contract.UID + " INTEGER, " +
		    ITA_Contract.SKIPPED_DYNAMIC_BROADCAST_ACTIONS + " TEXT, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT " +
		    ");";	
	
	private static final String SQL_CREATE_BROADCAST_ACTIONS_TABLE = "CREATE TABLE " +
			ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.ACTION + " TEXT, " +
		    ITA_Contract.PERMISSION + " TEXT, " +
		    ITA_Contract.SKIPPED + " INTEGER, " +
		    ITA_Contract.UID + " INTEGER, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT " +
		    ");";
	
	private static final String SQL_CREATE_PROVIDER_TABLE = "CREATE TABLE " +
			ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.PROVIDER_AUTHORITY + " TEXT, " +
		    ITA_Contract.SENT + " INTEGER, " +
		    ITA_Contract.SKIPPED + " INTEGER, " +
		    ITA_Contract.TIMEOUT + " INTEGER, " +
		    ITA_Contract.UID + " INTEGER, " +
		    ITA_Contract.CAN_READ + " INTEGER, " +
		    ITA_Contract.CAN_WRITE + " INTEGER, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT " +
		    ");";
	
	private static final String SQL_CREATE_RESULTS_TABLE = "CREATE TABLE " +
			ITA_Contract.ResultsTable.RESULTS_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.ACTION + " TEXT, " +
		    ITA_Contract.LOG_PATH + " TEXT, " +
		    ITA_Contract.PID + " INTEGER, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT, " +
		    ITA_Contract.INSTANCE_TYPE + " TEXT, " +
		    ITA_Contract.BASE_COMPONENT_TYPE + " TEXT, " +
		    ITA_Contract.DYNAMIC_RECEIVER_CRASH + " INTEGER, " +
		    ITA_Contract.LOG_EVENT_MESSAGES + " TEXT, " +
		    ITA_Contract.PROCESS_NAME + " TEXT, " +
		    ITA_Contract.EXCEPTION_NAME + " TEXT, " +
		    ITA_Contract.EXCEPTION_REASON + " TEXT, " +
		    ITA_Contract.APK_MD5 + " TEXT, " +
		    ITA_Contract.COMPONENT_TYPE + " TEXT " +		    
		    ");";
	
	private static final String SQL_CREATE_APPS_TABLE = "CREATE TABLE " +
			ITA_Contract.AppsTable.APPS_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT, " +
		    ITA_Contract.APP_TYPE + " TEXT, " +
		    ITA_Contract.APK_MD5 + " TEXT, " +
		    ITA_Contract.PERMISSIONS + " TEXT, " +
		    ITA_Contract.PROCESS_NAME + " TEXT, " +
		    ITA_Contract.UID + " TEXT " +
		    ");";
	
	private static final String SQL_CREATE_TIME_TABLE = "CREATE TABLE " +
			ITA_Contract.TimeTable.TIME_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.START_TIME + " TEXT, " +
		    ITA_Contract.STOP_TIME + " TEXT, " +
		    ITA_Contract.TOTAL_TIME + " TEXT " +
		    ");";

	private static final String SQL_CREATE_POTENTIAL_PRIVILEGE_ESCALATION_TABLE = "CREATE TABLE " +
			ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.ACTION + " TEXT, " +
		    ITA_Contract.NEW_FILE_AFTER_INTENT + " TEXT, " +
		    ITA_Contract.FILE_SIZE + " INTEGER, " +
		    ITA_Contract.COMPONENT_TYPE + " TEXT, " +
		    ITA_Contract.POTENTIAL_PRIVILEGE_ESCALATION_TYPE + " TEXT, " +
		    ITA_Contract.SETTINGS_TABLE + " TEXT, " +
		    ITA_Contract.BEFORE_SETTINGS_KEY + " TEXT, " +
		    ITA_Contract.BEFORE_SETTINGS_VALUE + " TEXT, " +
		    ITA_Contract.AFTER_SETTINGS_KEY + " TEXT, " +
		    ITA_Contract.AFTER_SETTINGS_VALUE + " TEXT, " +
		    ITA_Contract.BEFORE_PROPERTIES_KEY + " TEXT, " +
		    ITA_Contract.BEFORE_PROPERTIES_VALUE + " TEXT, " +
		    ITA_Contract.AFTER_PROPERTIES_KEY + " TEXT, " +
		    ITA_Contract.AFTER_PROPERTIES_VALUE + " TEXT " +		    
		    ");";
	
	private static final String SQL_CREATE_ALL_COMPONENTS_TABLE = "CREATE TABLE " +
			ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME +
		    "(" +                           
		    ITA_Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		    ITA_Contract.PACKAGE_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_NAME + " TEXT, " +
		    ITA_Contract.COMPONENT_TYPE + " TEXT, " +
		    ITA_Contract.PROVIDER_AUTHORITY + " TEXT, " +
		    ITA_Contract.EXPORTED + " INTEGER," +
		    ITA_Contract.VERSION_NAME + " TEXT, " +
		    ITA_Contract.VERSION_CODE + " TEXT, " +
		    ITA_Contract.PERMISSION + " TEXT, " +
		    ITA_Contract.READ_PERMISSION + " TEXT, " +
		    ITA_Contract.WRITE_PERMISSION + " TEXT, " +
		    ITA_Contract.UID + " INTEGER " +
		    ");";
	
	public ITA_DBHelper(Context context) {
		super(context, DBNAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_ACTIVITY_TABLE);
		db.execSQL(SQL_CREATE_SERVICE_TABLE);
		db.execSQL(SQL_CREATE_RECEIVER_TABLE);
		db.execSQL(SQL_CREATE_BROADCAST_ACTIONS_TABLE);
		db.execSQL(SQL_CREATE_PROVIDER_TABLE);
		db.execSQL(SQL_CREATE_RESULTS_TABLE);
		db.execSQL(SQL_CREATE_APPS_TABLE);
		db.execSQL(SQL_CREATE_TIME_TABLE);
		db.execSQL(SQL_CREATE_POTENTIAL_PRIVILEGE_ESCALATION_TABLE);
		db.execSQL(SQL_CREATE_ALL_COMPONENTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.ReceiverTable.RECEIVER_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.ActivityTable.ACTIVITY_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.ServiceTable.SERVICE_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.BroadcastActionTable.BROADCAST_ACTION_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.ProviderTable.PROVIDER_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.ResultsTable.RESULTS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.AppsTable.APPS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.TimeTable.TIME_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.PotentialPrivilegeEscalationTable.POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ITA_Contract.AllComponentsTable.ALL_COMPONENTS_TABLE_NAME);
        onCreate(db);
	}
}
