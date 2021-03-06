package fi.kroon.vadret.presentation.aboutapp.setting

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import fi.kroon.vadret.R
import fi.kroon.vadret.presentation.aboutapp.di.AboutAppFeatureScope
import fi.kroon.vadret.utils.COUNTY_KEY
import fi.kroon.vadret.utils.DEFAULT_SETTINGS
import fi.kroon.vadret.utils.DEFAULT_VALUE
import fi.kroon.vadret.utils.LATITUDE_KEY
import fi.kroon.vadret.utils.LOCALITY_KEY
import fi.kroon.vadret.utils.LONGITUDE_KEY
import fi.kroon.vadret.utils.MUNICIPALITY_KEY
import timber.log.Timber

@AboutAppFeatureScope
class AboutAppSettingFragment : PreferenceFragmentCompat() {

    companion object {
        val stringPreferenceKeysList: List<String> = listOf(
            COUNTY_KEY,
            MUNICIPALITY_KEY,
            LOCALITY_KEY,
            LATITUDE_KEY,
            LONGITUDE_KEY
        )

        fun newInstance(): AboutAppSettingFragment = AboutAppSettingFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Timber.d("ON ATTACH")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager?.run {
            sharedPreferencesName = DEFAULT_SETTINGS
            sharedPreferencesMode = Context.MODE_PRIVATE
        }
        setPreferencesFromResource(R.xml.about_app_preferences, rootKey)
        stringPreferenceKeysList.map { prefKey: String ->
            setStringSummary(prefKey)
        }
    }

    private fun setStringSummary(key: String) {
        findPreference<Preference>(key)?.summary =
            preferenceManager
                .sharedPreferences
                .getString(
                    key,
                    DEFAULT_VALUE
                )
    }
}