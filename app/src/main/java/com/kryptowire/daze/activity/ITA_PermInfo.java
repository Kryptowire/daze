package com.kryptowire.daze.activity;

import com.kryptowire.daze.R;
import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

public class ITA_PermInfo extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_perminfo);	
		TextView tv = (TextView) this.findViewById(R.id.permwaningTV);
		String text = "";
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            text = "This application needs to be granted three development permissions to function properly. Please install Android Debugging Bridge (ADB) on your computer. "
                    + "Then Grant the app the DUMP permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.DUMP
                    + "\n\nAlso grant the app the READ_LOGS permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.READ_LOGS +
                    "\n\nAlso grant the app the PACKAGE_USAGE_STATS permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.PACKAGE_USAGE_STATS +
                    "\n\nAfter you grant these three permissions, restart the application and then you will be able to use the buttons on the previous screen. The " + getResources().getString(R.string.intentBarrage) + " and the " + getResources().getString(R.string.oomAttack) + " buttons do not require any permissions to use.";
        } else{
            text = "This application needs to be granted two development permissions to function properly. Please install Android Debugging Bridge (ADB) on your computer. "
                    + "Then Grant the app the DUMP permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.DUMP
                    + "\n\nAlso grant the app the READ_LOGS permission using the following ADB command:\n\nadb shell pm grant " + this.getPackageName() + " " + Manifest.permission.READ_LOGS +
                    "\n\nAfter you grant these two permissions, restart the application and then you will be able to use the buttons on the previous screen. The " + getResources().getString(R.string.intentBarrage) + " and the " + getResources().getString(R.string.oomAttack) + " buttons do not require any permissions to use.";
        }
		tv.setText(text);
	}

}
