package fi.kroon.vadret.di.modules

import android.content.Context
import android.location.LocationManager
import dagger.Module
import dagger.Provides
import fi.kroon.vadret.data.location.local.LocationLocalDataSource
import fi.kroon.vadret.di.scope.VadretApplicationScope

@Module
object LocationServiceModule {

    @Provides
    @JvmStatic
    @VadretApplicationScope
    fun provideLocationManager(context: Context): LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides
    @JvmStatic
    @VadretApplicationScope
    fun locationProvider(locationManager: LocationManager): LocationLocalDataSource = LocationLocalDataSource(locationManager)
}