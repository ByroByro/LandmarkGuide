package zw.co.byrosolutions.landmarkguide;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import androidx.annotation.Nullable;

import zw.co.byrosolutions.landmarkguide.logic.methods;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        getPreferenceManager().findPreference("metric")
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        methods.Companion.saveSettings("metric", getActivity());
                        return true;
                    }
                });

        getPreferenceManager().findPreference("landmark")
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        methods.Companion.saveSettings("landmark", getActivity());
                        return true;
                    }
                });

    }
}
