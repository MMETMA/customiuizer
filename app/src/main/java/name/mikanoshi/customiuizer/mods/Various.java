package name.mikanoshi.customiuizer.mods;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import name.mikanoshi.customiuizer.MainModule;
import name.mikanoshi.customiuizer.R;

import name.mikanoshi.customiuizer.utils.Helpers;

public class Various {

	public static void AppInfoHook(LoadPackageParam lpparam) {
		Helpers.hookAllMethods("com.miui.appmanager.AMAppInfomationActivity", lpparam.classLoader, "onLoadFinished", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				final PreferenceActivity act = (PreferenceActivity)param.thisObject;
				final PackageInfo mPackageInfo = (PackageInfo)XposedHelpers.getObjectField(act, "mPackageInfo");
				final Resources modRes = Helpers.getModuleRes(act);
				Method[] addPref = XposedHelpers.findMethodsByExactParameters(act.getClass(), void.class, String.class, String.class, String.class);
				if (addPref.length == 0) {
					Helpers.log("AppInfoHook", "Unable to find class/method in SecurityCenter to hook");
					return;
				} else {
					addPref[0].setAccessible(true);
				}
				addPref[0].invoke(act, "apk_filename", modRes.getString(R.string.appdetails_apk_file), mPackageInfo.applicationInfo.sourceDir);
				addPref[0].invoke(act, "data_path", modRes.getString(R.string.appdetails_data_path), mPackageInfo.applicationInfo.dataDir);
				addPref[0].invoke(act, "app_uid", modRes.getString(R.string.appdetails_app_uid), String.valueOf(mPackageInfo.applicationInfo.uid));
				addPref[0].invoke(act, "target_sdk", modRes.getString(R.string.appdetails_sdk), String.valueOf(mPackageInfo.applicationInfo.targetSdkVersion));
				addPref[0].invoke(act, "launch_app", modRes.getString(R.string.appdetails_launch), "");

				@SuppressWarnings("deprecation")
				Preference pref = act.findPreference("launch_app");
				pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent launchIntent = act.getPackageManager().getLaunchIntentForPackage(mPackageInfo.packageName);
						if (launchIntent == null) {
							Toast.makeText(act, modRes.getString(R.string.appdetails_nolaunch), Toast.LENGTH_SHORT).show();
						} else {
							launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
							act.startActivity(launchIntent);
						}
						return true;
					}
				});
			}
		});
	}

	public static void AppsDefaultSortHook(LoadPackageParam lpparam) {
		Helpers.findAndHookMethod("com.miui.appmanager.AppManagerMainActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
				Bundle bundle = (Bundle)param.args[0];
				if (bundle == null) bundle = new Bundle();
				int order = Integer.parseInt(Helpers.getSharedStringPref((Context)param.thisObject, "pref_key_various_appsort", "0"));
				bundle.putInt("current_sory_type", order); // Xiaomi noob typos :)
				bundle.putInt("current_sort_type", order); // Future proof, they may fix it someday :D
				param.args[0] = bundle;
			}
		});

//		Helpers.findAndHookMethod("com.miui.appmanager.AppManagerMainActivity", lpparam.classLoader, "onSaveInstanceState", Bundle.class, new XC_MethodHook() {
//			@Override
//			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
//				Bundle bundle = (Bundle)param.args[0];
//				if (bundle == null) bundle = new Bundle();
//				bundle.putInt("current_sory_type", 1); // Xiaomi noob typos :)
//				bundle.putInt("current_sort_type", 1); // Future proof, they may fix it someday :D
//				Helpers.log("onSaveInstanceState: " + String.valueOf(bundle));
//			}
//		});
	}

	private static void setAppState(final Activity act, String pkgName, MenuItem item, boolean enable) {
		try {
			PackageManager pm = act.getPackageManager();
			pm.setApplicationEnabledSetting(pkgName, enable ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
			int state = pm.getApplicationEnabledSetting(pkgName);
			boolean isEnabledOrDefault = (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
			if ((enable && isEnabledOrDefault) || (!enable && !isEnabledOrDefault)) {
				item.setTitle(act.getResources().getIdentifier(enable ? "app_manager_disable_text" : "app_manager_enable_text", "string", "com.miui.securitycenter"));
				Toast.makeText(act, act.getResources().getIdentifier(enable ? "app_manager_enabled" : "app_manager_disabled", "string", "com.miui.securitycenter"), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(act, Helpers.getModuleRes(act).getString(R.string.disable_app_fail), Toast.LENGTH_LONG).show();
			}
			new Handler().postDelayed(act::invalidateOptionsMenu, 500);
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}

	public static void AppsDisableHook(LoadPackageParam lpparam) {
		Helpers.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				Activity act = (Activity)param.thisObject;
				Menu menu = (Menu)param.args[0];
				MenuItem dis = menu.add(0, 666, 1, act.getResources().getIdentifier("app_manager_disable_text", "string", lpparam.packageName));
				dis.setIcon(act.getResources().getIdentifier("action_button_stop", "drawable", lpparam.packageName));
				dis.setEnabled(true);
				dis.setShowAsAction(1);
				//XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDisableButton", dis);

				PackageManager pm = act.getPackageManager();
				String mPackageName = (String)XposedHelpers.getObjectField(act, "mPackageName");
				ApplicationInfo appInfo = pm.getApplicationInfo(mPackageName, PackageManager.GET_META_DATA);
				boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
				boolean isUpdatedSystem = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

				dis.setTitle(act.getResources().getIdentifier(appInfo.enabled ? "app_manager_disable_text" : "app_manager_enable_text", "string", lpparam.packageName));

				if (!appInfo.enabled || (isSystem && !isUpdatedSystem)) {
					MenuItem item = menu.findItem(2);
					if (item != null) item.setVisible(false);
				}
			}
		});

		Helpers.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity", lpparam.classLoader, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				MenuItem item = (MenuItem)param.args[0];
				if (item == null || item.getItemId() != 666) return;

				Activity act = (Activity)param.thisObject;
				Resources modRes = Helpers.getModuleRes(act);
				String mPackageName = (String)XposedHelpers.getObjectField(act, "mPackageName");
				if ("com.android.settings".equals(mPackageName)) {
					Toast.makeText(act, modRes.getString(R.string.disable_app_settings), Toast.LENGTH_SHORT).show();
					return;
				}

				boolean mIsSystem = XposedHelpers.getBooleanField(act, "mIsSystem");
				PackageManager pm = act.getPackageManager();
				int state = pm.getApplicationEnabledSetting(mPackageName);
				boolean isEnabledOrDefault = (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
				if (isEnabledOrDefault) {
					if (mIsSystem) {

						String title = modRes.getString(R.string.disable_app_title);
						String text = modRes.getString(R.string.disable_app_text);
						new AlertDialog.Builder(act).setTitle(title).setMessage(text).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								setAppState(act, mPackageName, item, false);
							}
						}).setNegativeButton(android.R.string.cancel, null).show();
					} else setAppState(act, mPackageName, item, false);
				} else setAppState(act, mPackageName, item, true);
				param.setResult(true);
			}
		});
	}

	public static void AlarmCompatHook() {
		Helpers.findAndHookMethod("android.provider.Settings$System", null, "getStringForUser", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
				try {
					ContentResolver resolver = (ContentResolver)param.args[0];
					String pkgName = (String)XposedHelpers.callMethod(resolver, "getPackageName");
					String key = (String)param.args[1];
					if ("next_alarm_formatted".equals(key) && MainModule.mPrefs.getStringSet("various_alarmcompat_apps").contains(pkgName))
					param.args[1] = "next_alarm_clock_formatted";
				} catch (Throwable t) {
					XposedBridge.log(t);
				}
			}
		});
	}

	public static void AlarmCompatServiceHook(LoadPackageParam lpparam) {
		Helpers.findAndHookMethod("com.android.server.AlarmManagerService", lpparam.classLoader, "onBootPhase", int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				if ((int)param.args[0] != 500 /*PHASE_SYSTEM_SERVICES_READY*/) return;

				Context mContext = (Context)XposedHelpers.callMethod(param.thisObject, "getContext");
				if (mContext == null) {
					Helpers.log("AlarmCompatServiceHook", "Context is NULL");
					return;
				}
				ContentResolver resolver = mContext.getContentResolver();
				ContentObserver alarmObserver = new ContentObserver(new Handler()) {
					@Override
					public void onChange(boolean selfChange) {
						if (selfChange) return;
						String nextAlarm = Settings.System.getString(resolver, "next_alarm_clock_formatted");
						long nextTime = 0;
						if (!TextUtils.isEmpty(nextAlarm)) try {
							Calendar cal = Calendar.getInstance();
							cal.set(Calendar.HOUR_OF_DAY, 0);
							cal.clear(Calendar.MINUTE);
							cal.clear(Calendar.SECOND);
							cal.clear(Calendar.MILLISECOND);

							SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(mContext) ? "EHm" : "Ehma"), Locale.getDefault());
							dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
							nextTime = cal.getTimeInMillis() + dateFormat.parse(nextAlarm).getTime();
						} catch (Throwable t) {
							XposedBridge.log(t);
						}
						XposedHelpers.setAdditionalInstanceField(param.thisObject, "mNextAlarmTime", nextTime);
					}
				};
				alarmObserver.onChange(false);
				resolver.registerContentObserver(Settings.System.getUriFor("next_alarm_clock_formatted"), false, alarmObserver);
			}
		});

		Helpers.findAndHookMethod("com.android.server.AlarmManagerService", lpparam.classLoader, "getNextAlarmClockImpl", int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
				Context mContext = (Context)XposedHelpers.callMethod(param.thisObject, "getContext");
				String pkgName = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
				Object mNextAlarmTime = XposedHelpers.getAdditionalInstanceField(param.thisObject, "mNextAlarmTime");
				if (mNextAlarmTime != null && MainModule.mPrefs.getStringSet("various_alarmcompat_apps").contains(pkgName))
				param.setResult((long)mNextAlarmTime == 0 ? null : new AlarmManager.AlarmClockInfo((long)mNextAlarmTime, null));
			}
		});
	}

//	public static void LargeCallerPhotoHook(LoadPackageParam lpparam) {
//		Helpers.findAndHookMethod("com.android.incallui.CallCardFragment", lpparam.classLoader, "setCallCardImage", Drawable.class, boolean.class, new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
//				param.args[1] = true;
//			}
//		});
//
//		Helpers.findAndHookMethod("com.android.incallui.CallCardFragment", lpparam.classLoader, "showBigAvatar", boolean.class, Drawable.class, new XC_MethodHook() {
//			@Override
//			protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
//				//Helpers.log("showBigAvatar: " + String.valueOf(param.args[0]) + " | " + String.valueOf(param.args[1]));
//				if (param.args[1] == null)
//					param.setResult(null);
//				else
//					param.args[0] = true;
//			}
//		});
//	}

}