package com.example.kanbun.ui.settings

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.kanbun.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("profile")?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
            true
        }
    }
}