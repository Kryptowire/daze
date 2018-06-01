package com.kryptowire.daze.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.kryptowire.daze.ITA_Constants;
import com.kryptowire.daze.R;
import com.kryptowire.daze.service.ITA_IntentService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ITA_AppSelection extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_appselection);

		final ListView listview = (ListView) findViewById(R.id.applistview);

		Bundle b = this.getIntent().getExtras();
		if (b != null) {
			final ArrayList<String> list = b.getStringArrayList(ITA_Constants.PACKAGE_NAME_LIST);
			final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, list); 
			listview.setAdapter(adapter);
			listview.setFastScrollEnabled(true);
			final Context con = this.getApplicationContext();

			listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {



				@Override
				public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
					String packageName = listview.getItemAtPosition(position).toString();
					Intent intent = new Intent(con, ITA_IntentService.class);
					intent.setAction(ITA_Constants.START_ANALYSIS);
					Bundle b = new Bundle();
					b.putString(ITA_Constants.PACKAGE_NAME, packageName);
					intent.putExtras(b);
					startService(intent);
					Toast.makeText(parent.getContext(), "Selected: " + packageName + " - Starting Analysis", Toast.LENGTH_LONG).show();

					// makes it so that it will not come up this window again and kill the application multiple times
					ITA_AppSelection.this.finish();
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