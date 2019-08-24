package name.mikanoshi.customiuizer.subs;

import android.os.Bundle;
import android.preference.Preference;

import java.util.Objects;

import name.mikanoshi.customiuizer.SubFragment;
import name.mikanoshi.customiuizer.utils.Helpers;

public class Controls extends SubFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle args = getArguments();
		String sub = args.getString("sub");
		if (sub == null) sub = "";

		selectSub("pref_key_controls", sub);
		switch (sub) {
			case "pref_key_controls_cat_navbar":
				findPreference("pref_key_controls_backlong").setOnPreferenceClickListener(openNavbarActions);
				findPreference("pref_key_controls_homelong").setOnPreferenceClickListener(openNavbarActions);
				findPreference("pref_key_controls_menulong").setOnPreferenceClickListener(openNavbarActions);

				findPreference("pref_key_controls_navbarleft").setOnPreferenceClickListener(openNavbarActions);
				findPreference("pref_key_controls_navbarleftlong").setOnPreferenceClickListener(openNavbarActions);
				findPreference("pref_key_controls_navbarright").setOnPreferenceClickListener(openNavbarActions);
				findPreference("pref_key_controls_navbarrightlong").setOnPreferenceClickListener(openNavbarActions);
				break;
			case "pref_key_controls_cat_fingerprint":
				findPreference("pref_key_controls_fingerprint1").setOnPreferenceClickListener(openControlsActions);
				findPreference("pref_key_controls_fingerprint2").setOnPreferenceClickListener(openControlsActions);
				findPreference("pref_key_controls_fingerprintlong").setOnPreferenceClickListener(openControlsActions);

				findPreference("pref_key_controls_fingerprintsuccess_ignore").setEnabled(!Objects.equals(Helpers.prefs.getString("pref_key_controls_fingerprintsuccess", "1"), "1"));
				findPreference("pref_key_controls_fingerprintsuccess").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						findPreference("pref_key_controls_fingerprintsuccess_ignore").setEnabled(!newValue.equals("1"));
						return true;
					}
				});
				break;
		}
	}

}