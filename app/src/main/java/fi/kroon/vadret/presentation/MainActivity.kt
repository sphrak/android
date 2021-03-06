package fi.kroon.vadret.presentation

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import fi.kroon.vadret.R
import fi.kroon.vadret.data.nominatim.model.Locality
import fi.kroon.vadret.utils.DEFAULT_SETTINGS
import fi.kroon.vadret.utils.extensions.appComponent
import fi.kroon.vadret.utils.extensions.setupWithNavController
import fi.kroon.vadret.utils.extensions.toGone
import fi.kroon.vadret.utils.extensions.toVisible
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var navController: LiveData<NavController>? = null

    /**
     *  Currently the [MainActivity] is a bit cluttered
     *  because Android Architecture Navigation component
     *  lacks native support for multistack navigation.
     *  Google is currently working on a solution and
     *  the issue is being tracked here: https://issuetracker.google.com/issues/80029773#comment25
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("ON CREATE")
        appComponent()
            .inject(this)

        setContentView(R.layout.activity_main)
        setupSupportActionBar()

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }

        PreferenceManager.setDefaultValues(
            this,
            DEFAULT_SETTINGS,
            MODE_PRIVATE,
            R.xml.about_app_preferences,
            false
        )
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        Timber.d("ON RESTORE INSTANCE STATE")
        setupBottomNavigationBar()
    }

    private fun setupBottomNavigationBar() {
        Timber.d("setupBottomNavigationBar")

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navGraphIds: List<Int> = listOf(
            R.navigation.weather,
            R.navigation.alert,
            R.navigation.radar,
            R.navigation.settings
        )
        // Setup the bottom navigation view with a list of navigation graphs
        val controller: LiveData<NavController> = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(
            this,
            Observer { navController ->
                setupActionBarWithNavController(navController)
            }
        )
        navController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.value?.navigateUp() ?: false
    }

    /**
     * Overriding popBackStack is necessary in this case if the app is started from the deep link.
     */
    override fun onBackPressed() {
        if (navController?.value?.popBackStack() != true) {
            super.onBackPressed()
        }
    }

    private fun setupSupportActionBar() {
        setSupportActionBar(toolBar)
    }

    fun hideLocalityActionBar() = currentLocationName.toGone()

    fun setLocalityActionBar(locality: Locality) {
        locality.name?.let {
            currentLocationName.text = locality.name
        } ?: currentLocationName.setText(R.string.unknown_area)
        currentLocationName.toVisible()
    }

    inline fun <reified T : Fragment> getFragmentByClassName(className: String): T {
        val navHostFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.nav_host_container)
        return navHostFragment?.childFragmentManager?.fragments?.filterNotNull()?.find {
            it.javaClass.name == className
        }!! as T
    }
}