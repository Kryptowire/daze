package com.kryptowire.daze.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ITA_CrashSelection extends Activity {

	static final String extendedActionRegex = "(.*) \\((.*)/(.*)\\)";
	static final Pattern extendedActionPattern = Pattern.compile(extendedActionRegex);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crashlisting);
		
		final ListView listview = (ListView) findViewById(R.id.listview);
		
		Intent i = this.getIntent();
		if (i == null)
			return;		
		String action = i.getAction();
		if (action == null)
			return;
		
		if (action.equals(ITA_Constants.DISPLAY_CRASHES)) {
			
			Bundle b = this.getIntent().getExtras();
			if (b == null)
				return;			
			final ArrayList<String> list = b.getStringArrayList(ITA_Constants.CRASHES_LIST);
		    final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, list); 
		    listview.setAdapter(adapter);
		    listview.setFastScrollEnabled(true);
		    final Context con = this.getApplicationContext();
		    listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		    	@Override
		    	public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
		    		String selection = listview.getItemAtPosition(position).toString();
		    		Intent intent = new Intent(con, ITA_DisplayResult.class);
		    		intent.setAction(ITA_Constants.PROCESS_CRASH_DISPLAY_RESULT);
		    		Toast.makeText(parent.getContext(), "Selected: " + selection, Toast.LENGTH_LONG).show();		    		
		    		
		    		Matcher actionMatcher = extendedActionPattern.matcher(selection);
		    		if (actionMatcher.matches()) {
		    			
		    			String action = actionMatcher.group(1);
		    			String packageName = actionMatcher.group(2);
		    			String componentName = actionMatcher.group(3);
		    			
		    			intent.putExtra(ITA_Constants.ACTION, action);
		    			intent.putExtra(ITA_Constants.PACKAGE_NAME, packageName);
		    			intent.putExtra(ITA_Constants.COMPONENT_NAME, componentName);
		    			intent.putExtra(ITA_Constants.IS_BROADCAST_ACTION, true);
		    			startActivity(intent);	
		    			return;
		    		}
		    		
		    		String[] part = selection.split("/");
		    		if (part.length > 1) {
		    			intent.putExtra(ITA_Constants.PACKAGE_NAME, part[0]);
		    			intent.putExtra(ITA_Constants.COMPONENT_NAME, part[1]);
		    			intent.putExtra(ITA_Constants.IS_BROADCAST_ACTION, false);
		    		}
		    		else {
		    			intent.putExtra(ITA_Constants.ACTION, selection);
		    			intent.putExtra(ITA_Constants.IS_BROADCAST_ACTION, true);		    			
		    		}
		    		startActivity(intent);	
		    		return;
		    	}			
		    });
		}
	}

	class StableArrayAdapter extends ArrayAdapter<String> {

		HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

		public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
		    for (int i = 0; i < objects.size(); ++i) {
		    	mIdMap.put(objects.get(i), i);
		    }
		}

		@Override
		public long getItemId(int position) {
			String item = getItem(position);
		    return mIdMap.get(item);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
    }
	
}
