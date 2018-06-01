package com.kryptowire.daze.provider;

import android.content.ContentUris;
import android.net.Uri;

public class ITA_Contract {
	
	static String AUTHORITY = "com.kryptowire.daze.provider";
	static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);	
	public final static String PACKAGE_NAME = "packageName";
	public final static String COMPONENT_NAME = "componentName";
	public final static String ACTION = "action";
	public final static String SENT = "sent";
	public final static String LOG_PATH = "logPath";
	public final static String VERSION_NAME = "versionName";
	public final static String VERSION_CODE = "versionCode";
	public final static String READ_PERMISSION = "readPermission";
	public final static String WRITE_PERMISSION = "writePermission";
	public final static String PERMISSION = "permission";
	public final static String _ID = "_id";
	public final static String UID = "uid";
	public final static String INSTANCE_TYPE = "instanceType";
	public final static String PID = "pid";
	public final static String APK_MD5 = "apkmd5";
	public final static String LOG_EVENT_MESSAGES = "logEventMessages";
	public final static String PROCESS_NAME = "processName";
	public final static String EXCEPTION_NAME = "exceptionName";
	public final static String EXCEPTION_REASON = "exceptionReason";
	public final static String COMPONENT_TYPE = "componentType";
	public final static String APP_TYPE = "appType";
	public final static String START_TIME = "startTime";
	public final static String STOP_TIME = "stopTime";
	public final static String TOTAL_TIME = "totalTime";
	public final static String CAN_WRITE = "canWrite";
	public final static String CAN_READ = "canRead";
	public final static String EXPORTED = "exported";
	public final static String FILE_SIZE = "fileSize";
	public final static String NEW_FILE_AFTER_INTENT = "newFileAfterIntent";
	public final static String SETTINGS_TABLE = "settingsTable";
	public final static String BEFORE_SETTINGS_KEY = "beforeSettingsKey";
	public final static String BEFORE_SETTINGS_VALUE = "beforeSettingsValue";
	public final static String AFTER_SETTINGS_KEY = "afterSettingsKey";
	public final static String AFTER_SETTINGS_VALUE = "afterSettingsValue";
	public final static String BEFORE_PROPERTIES_KEY = "beforePropertiesKey";
	public final static String BEFORE_PROPERTIES_VALUE = "beforePropertiesValue";
	public final static String AFTER_PROPERTIES_KEY = "afterPropertiesKey";
	public final static String AFTER_PROPERTIES_VALUE = "afterPropertiesValue";
	public final static String DYNAMIC_BROADCAST_RECEIVERS_REGISTERED = "dynamicbroadcastreceiversregistered";
	public final static String DYNAMIC_RECEIVER_CRASH = "dynamicReceiverCrash";
	public final static String BASE_COMPONENT_TYPE = "baseComponentType";
	public final static String SKIPPED = "skipped";
	public final static String TIMEOUT = "timeout";
	public final static String PERMISSIONS = "permissions";
	public final static String SKIPPED_DYNAMIC_BROADCAST_ACTIONS = "skippeddynamicactions";	
	public final static String PROVIDER_AUTHORITY = "providerAuthority";
	public final static int CAN_ACCESS_PROV = 1;
	public final static int CANT_ACCESS_PROV = 0;
	public final static String POTENTIAL_PRIVILEGE_ESCALATION_TYPE = "potentialPrivilegeEscalationType";
	
	public static final class ActivityTable {
		public static String ACTIVITY_TABLE_NAME = "activity";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(ACTIVITY_TABLE_NAME).build();		
		
		public static Uri buildActivityTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}
	
	public static final class ReceiverTable {
		public static String RECEIVER_TABLE_NAME = "broadcastreceiver";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(RECEIVER_TABLE_NAME).build();
		
		public static Uri buildReceiverTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}
	
	public static final class BroadcastActionTable {
		public static String BROADCAST_ACTION_TABLE_NAME = "broadcastaction";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(BROADCAST_ACTION_TABLE_NAME).build();
		
		public static Uri buildBroadcastActionTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}
	
	public static final class ServiceTable {
		public static String SERVICE_TABLE_NAME = "service";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(SERVICE_TABLE_NAME).build();
		
		public static Uri buildServiceTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}
	
	public static final class ResultsTable {
		public static String RESULTS_TABLE_NAME = "results";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(RESULTS_TABLE_NAME).build();
		
		public static Uri buildResultsTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}
	}
	
	public static final class AppsTable {
		public static String APPS_TABLE_NAME = "apps";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(APPS_TABLE_NAME).build();
		
		public static Uri buildAppsTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}		
	}
	
	public static final class ProviderTable {
		public static String PROVIDER_TABLE_NAME = "provider";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PROVIDER_TABLE_NAME).build();
		
		public static Uri buildProviderTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}		
	}
	
	public static final class TimeTable {
		public static String TIME_TABLE_NAME = "analysistime";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(TIME_TABLE_NAME).build();
		
		public static Uri buildTimeTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}		
	}
	
	public static final class AllComponentsTable {
		public static String ALL_COMPONENTS_TABLE_NAME = "allcomponents";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(ALL_COMPONENTS_TABLE_NAME).build();
		
		public static Uri buildTimeTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}		
	}
	
	public static final class PotentialPrivilegeEscalationTable {
		public static String POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME = "potentialprivilegeescalation";
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(POTENTIAL_PRIVILEGE_ESCALATOIN_TABLE_NAME).build();
		
		public static Uri buildPotentialPrivilegeEscalationTableUri(long id){
    			return ContentUris.withAppendedId(CONTENT_URI, id);
		}		
	}
	

}
