package com.kryptowire.daze.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;

public class ITA_SystemMonitoring extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_potentialprivilegeescalationselection);

		final ListView listview = (ListView) findViewById(R.id.listviewppe);

		Intent i = this.getIntent();
		if (i == null)
			return;		
		String action = i.getAction();
		if (action == null)
			return;

		if (action.equals(ITA_Constants.DISPLAY_POTENTIAL_PRIVILEGE_ESCALATION)) {

			Bundle b = this.getIntent().getExtras();
			if (b == null)
				return;			
			final ArrayList<String> list = b.getStringArrayList(ITA_Constants.POTENTIAL_PRIVILEGE_ESCALATION_LIST);
			final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, list); 
			listview.setAdapter(adapter);
			listview.setFastScrollEnabled(true);
			final Context con = this.getApplicationContext();
			listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
					String selection = listview.getItemAtPosition(position).toString();
					Intent intent = new Intent(con, ITA_DisplayResult.class);
					intent.setAction(ITA_Constants.POTENTIAL_PRIVILEGE_ESCALATION_DISPLAY_RESULT);
					if (selection.contains("/")) {		    			
						String[] part = selection.split("/");		    			
						intent.putExtra(ITA_Constants.PACKAGE_NAME, part[0]);
						intent.putExtra(ITA_Constants.COMPONENT_NAME, part[1]);
						intent.putExtra(ITA_Constants.IS_BROADCAST_ACTION, false);
					}
					else {
						intent.putExtra(ITA_Constants.ACTION, selection);
						intent.putExtra(ITA_Constants.IS_BROADCAST_ACTION, true);						
					}
					startActivity(intent);
					Toast.makeText(parent.getContext(), "Selected: " + selection, Toast.LENGTH_LONG).show();
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
