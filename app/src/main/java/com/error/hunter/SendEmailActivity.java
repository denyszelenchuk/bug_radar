/** Copyright © 2015 Denys Zelenchuk.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.**/

package com.error.hunter;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SendEmailActivity extends Activity{

	int notificationID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(SendEmailActivity.this);
    	String eMail = sharedPref.getString("pref_report_email", "");
		
		Intent intentExtras = getIntent();
		String errorReport = intentExtras.getStringExtra("REPORT");
		notificationID = intentExtras.getIntExtra("NOTIFICATION_ID", 0);
		String badProcess = intentExtras.getStringExtra("BAD_PROCESS");
		removeNotification();
		
		Intent intent = new Intent(Intent.ACTION_SENDTO);
    	intent.setType("message/rfc822");
    	intent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.exception_in) + " " + badProcess);
    	intent.putExtra(Intent.EXTRA_TEXT, errorReport);
    	intent.setData(Uri.parse("mailto:" + eMail)); 
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
    	startActivity(Intent.createChooser(intent, this.getString(R.string.send_report_dialog)));
    	
    	SendEmailActivity.this.finish();
	}
	
	private void removeNotification() {
	    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
	    manager.cancel(notificationID);  
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
}
