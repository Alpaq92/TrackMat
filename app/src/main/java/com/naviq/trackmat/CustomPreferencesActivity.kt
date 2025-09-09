package com.naviq.trackmat

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.edit
import androidx.preference.PreferenceManager


class CustomPreferencesActivity : PreferenceActivity() {
    private val lockTimeout = 500L

    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, CustomPreferenceFragment()).commit()
    }

    class CustomPreferenceFragment : PreferenceFragment() {
        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference): Boolean {
            if (preference.key == resources.getString(R.string.restore_preference)) {
                val context = ContextThemeWrapper(context, R.style.Base_Theme_TrackMat)

                val builder = AlertDialog.Builder(context)
                builder.setMessage(resources.getString(R.string.restore_confirm))
                    .setPositiveButton(resources.getString(R.string.app_ok)) { dialog, id ->
                        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

                        preferences.edit(commit = true) {
                            clear()
                        }

                        setPreferenceScreen(null);
                        addPreferencesFromResource(R.xml.preferences);

                        dialog.dismiss()
                    }
                    .setNegativeButton(resources.getString(R.string.app_cancel)) { dialog, id ->
                        dialog.cancel()
                    }

                val dialog = builder.create()
                dialog.show()
            }

            return false
        }
    }
}