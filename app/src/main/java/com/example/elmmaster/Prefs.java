/*
T�RKAY B�L�YOR turkaybiliyor@hotmail.com
 */
package com.example.elmmaster;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        addPreferencesFromResource(R.xml.preference);
    }	 
}