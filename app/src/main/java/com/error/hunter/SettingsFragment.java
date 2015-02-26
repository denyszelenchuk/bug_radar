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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	SharedPreferences sharedPref;
	SharedPreferences.Editor editor;
	String eMail;
	Context context;
	int counter;
	
	private CheckBoxPreference mCheckBoxPreference;
    private EditTextPreference mEditTextPreference;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        mEditTextPreference = (EditTextPreference) findPreference("pref_report_email");

        
        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(SettingsFragment.this.getActivity());
        String eMail = defaultSharedPref.getString("pref_report_email", getResources().getString(R.string.pref_email_summary));
        if (!eMail.equals(getResources().getString(R.string.pref_email_summary))){
        	if(!eMail.equals("")){
        		mEditTextPreference.setSummary(eMail);
        	} 
        }
       
        mEditTextPreference.setOnPreferenceChangeListener( new OnPreferenceChangeListener() {
        	@Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (((String)newValue).equals("")) {
                	preference.setSummary(getResources().getString(R.string.pref_email_summary));
                } else {
                	preference.setSummary((String)newValue);
                }
				return true;
            }
        });
        
        mCheckBoxPreference = (CheckBoxPreference) findPreference("pref_system_packages");
        mCheckBoxPreference.setOnPreferenceChangeListener( new OnPreferenceChangeListener() {
        	@Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
        		
        		counter++;
        		
        		sharedPref = SettingsFragment.this.getActivity().getSharedPreferences("eCatcher", 0);
        		editor = sharedPref.edit();
        		editor.putInt("counter", counter);
        		editor.commit();
				return true;
            }
        });
    }
    
    @Override
	public void onResume() {
        super.onResume();
    }
    
    @Override
	public void onDestroy(){
    	super.onDestroy();
    	counter = 0;
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	}
}
