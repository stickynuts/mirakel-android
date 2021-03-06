package sheetrock.panda.changelog;

/**
 * Copyright (C) 2011, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 * modifyed by AZ and Weiznich 2013
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.webkit.WebView;
import de.azapps.mirakel.changelog.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.tools.Log;

public class ChangeLog {

	private final Context context;
	private String lastVersion, thisVersion;

	private final static int API_LEVEL = Build.VERSION.SDK_INT;

	// this is the key for storing the version name in SharedPreferences
	private static final String VERSION_KEY = "PREFS_VERSION_KEY";

	private static final String NO_VERSION = "";

	/**
	 * Constructor
	 * 
	 * Retrieves the version names and stores the new version name in
	 * SharedPreferences
	 * 
	 * @param context
	 * @param sp
	 *            the shared preferences to store the last version name into
	 */
	public ChangeLog(Context context) {
		this.context = context;

		// get version numbers
		this.lastVersion = MirakelCommonPreferences.getVersionKey();
		Log.d(TAG, "lastVersion: " + this.lastVersion);
		try {
			this.thisVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			this.thisVersion = NO_VERSION;
			Log.e(TAG, "could not get version name from manifest!");
			e.printStackTrace();
		}
		Log.d(TAG, "appVersion: " + this.thisVersion);
	}

	/**
	 * @return The version name of the last installation of this app (as
	 *         described in the former manifest). This will be the same as
	 *         returned by <code>getThisVersion()</code> the second time this
	 *         version of the app is launched (more precisely: the second time
	 *         ChangeLog is instantiated).
	 * @see AndroidManifest.xml#android:versionName
	 */
	public String getLastVersion() {
		return this.lastVersion;
	}

	/**
	 * @return The version name of this app as described in the manifest.
	 * @see AndroidManifest.xml#android:versionName
	 */
	public String getThisVersion() {
		return this.thisVersion;
	}

	/**
	 * @return <code>true</code> if this version of your app is started the
	 *         first time
	 */
	public boolean firstRun() {
		return !this.lastVersion.equals(this.thisVersion);
	}

	/**
	 * @return <code>true</code> if your app including ChangeLog is started the
	 *         first time ever. Also <code>true</code> if your app was
	 *         deinstalled and installed again.
	 */
	public boolean firstRunEver() {
		return NO_VERSION.equals(this.lastVersion);
	}

	/**
	 * @return An AlertDialog displaying the changes since the previous
	 *         installed version of your app (what's new). But when this is the
	 *         first run of your app including ChangeLog then the full log
	 *         dialog is show.
	 */
	public AlertDialog getLogDialog() {
		return this.getDialog(this.firstRunEver());
	}

	/**
	 * @return an AlertDialog with a full change log displayed
	 */
	public AlertDialog getFullLogDialog() {
		return this.getDialog(true);
	}

	private AlertDialog getDialog(boolean full) {
		WebView wv = new WebView(new ContextThemeWrapper(this.context,
				 R.style.Dialog));
		if (API_LEVEL >= Build.VERSION_CODES.HONEYCOMB) {
			Compatibility.setViewLayerTypeSoftware(wv);
			wv.setBackgroundColor(0); // transparent
		} else {
			wv.setBackgroundColor(Color.WHITE);
		}
		String log = this.getLog(full);
		if (MirakelCommonPreferences.isDark())
			log = "<font color='"
					+ String.format("#%06X", 0xFFFFFF & this.context.getResources()
							.getColor(R.color.holo_blue_light)) + "'>" + log
					+ "</font>";
		wv.loadDataWithBaseURL(null, log, "text/html", "UTF-8", null);

		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setTitle(
				this.context.getResources().getString(
						full ? R.string.changelog_full_title
								: R.string.changelog_title))
				.setView(wv)
				.setCancelable(true)
				// OK button
				.setPositiveButton(
						this.context.getResources().getString(
								R.string.changelog_ok_button),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								updateVersionInPreferences();
							}
						});

		if (!full) {
			// "more ..." button
			builder.setNegativeButton(R.string.changelog_show_full,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							getFullLogDialog().show();
						}
					});
		}

		return builder.create();
	}

	@SuppressLint("NewApi")
	private void updateVersionInPreferences() {
		SharedPreferences.Editor editor = MirakelCommonPreferences.getEditor();
		editor.putString(VERSION_KEY, this.thisVersion);
		// // on SDK-Versions > 9 you should use this:
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			editor.commit();
		} else {
			editor.apply();
		}
	}

	/**
	 * @return HTML displaying the changes since the previous installed version
	 *         of your app (what's new)
	 */
	public String getLog() {
		return this.getLog(false);
	}

	/**
	 * @return HTML which displays full change log
	 */
	public String getFullLog() {
		return this.getLog(true);
	}

	/** modes for HTML-Lists (bullet, numbered) */
	private enum Listmode {
		NONE, ORDERED, UNORDERED,
	}

	private Listmode listMode = Listmode.NONE;
	private StringBuffer sb = null;
	private static final String EOCL = "END_OF_CHANGE_LOG";

	private String getLog(boolean full) {
		// read changelog.txt file
		this.sb = new StringBuffer();
		try {
			InputStream ins = this.context.getResources().openRawResource(
					R.raw.changelog);
			BufferedReader br = new BufferedReader(new InputStreamReader(ins));

			String line = null;
			boolean advanceToEOVS = false; // if true: ignore further version
											// sections
			while ((line = br.readLine()) != null) {
				line = line.trim();
				char marker = line.length() > 0 ? line.charAt(0) : 0;
				if (marker == '$') {
					// begin of a version section
					this.closeList();
					String version = line.substring(1).trim();
					// stop output?
					if (!full) {
						if (this.lastVersion.equals(version)) {
							advanceToEOVS = true;
						} else if (version.equals(EOCL)) {
							advanceToEOVS = false;
						}
					}
				} else if (!advanceToEOVS) {
					switch (marker) {
					case '%':
						// line contains version title
						this.closeList();
						this.sb.append("<div class='title'>"
								+ line.substring(1).trim() + "</div>\n");
						break;
					case '_':
						// line contains version title
						this.closeList();
						this.sb.append("<div class='subtitle'>"
								+ line.substring(1).trim() + "</div>\n");
						break;
					case '!':
						// line contains free text
						this.closeList();
						this.sb.append("<div class='freetext'>"
								+ line.substring(1).trim() + "</div>\n");
						break;
					case '#':
						// line contains numbered list item
						this.openList(Listmode.ORDERED);
						this.sb.append("<li>" + line.substring(1).trim() + "</li>\n");
						break;
					case '*':
						// line contains bullet list item
						this.openList(Listmode.UNORDERED);
						this.sb.append("<li>" + line.substring(1).trim() + "</li>\n");
						break;
					default:
						// no special character: just use line as is
						this.closeList();
						this.sb.append(line + "\n");
					}
				}
			}
			this.closeList();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return this.sb.toString();
	}

	private void openList(Listmode listMode) {
		if (this.listMode != listMode) {
			closeList();
			if (listMode == Listmode.ORDERED) {
				this.sb.append("<div class='list'><ol>\n");
			} else if (listMode == Listmode.UNORDERED) {
				this.sb.append("<div class='list'><ul>\n");
			}
			this.listMode = listMode;
		}
	}

	private void closeList() {
		if (this.listMode == Listmode.ORDERED) {
			this.sb.append("</ol></div>\n");
		} else if (this.listMode == Listmode.UNORDERED) {
			this.sb.append("</ul></div>\n");
		}
		this.listMode = Listmode.NONE;
	}

	private static final String TAG = "ChangeLog";

	/**
	 * manually set the last version name - for testing purposes only
	 * 
	 * @param lastVersion
	 */
	void setLastVersion(String lastVersion) {
		this.lastVersion = lastVersion;
	}

	/**
	 * Methods introduced in API-Levels > 3
	 */
	@SuppressLint("NewApi")
	static class Compatibility {
		/**
		 * due to an error in android itself, do this to prevent the background
		 * to be transparant and therefore white
		 * 
		 * API Level 11
		 * 
		 * @see http://code.google.com/p/android-change-log/issues/detail?id=17
		 */
		static void setViewLayerTypeSoftware(View v) {
			v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
	}
}
