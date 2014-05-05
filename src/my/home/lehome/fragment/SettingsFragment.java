package my.home.lehome.fragment;

import my.home.lehome.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        EditTextPreference bindPortEditTextPreference = (EditTextPreference) findPreference("settings_bind_port");
        bindPortEditTextPreference.setSummary(sharedPreferences.getString("settings_bind_port", "8004"));
        EditTextPreference plugPortEditTextPreference = (EditTextPreference) findPreference("settings_plug_port");
        plugPortEditTextPreference.setSummary(sharedPreferences.getString("settings_plug_port", "27431"));
        EditTextPreference boardcastAddressEditTextPreference = (EditTextPreference) findPreference("settings_broadcast_address");
        boardcastAddressEditTextPreference.setSummary(sharedPreferences.getString("settings_broadcast_address", "192.168.1.255"));
	}
}
