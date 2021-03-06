package fi.kroon.vadret.domain.weatherforecast

import fi.kroon.vadret.data.exception.Failure
import fi.kroon.vadret.data.functional.Either
import fi.kroon.vadret.data.weatherforecast.local.WeatherForecastLocalKeyValueDataSource
import fi.kroon.vadret.utils.AUTOMATIC_LOCATION_MODE_KEY
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

class SetLocationModeTask @Inject constructor(
    private val repo: WeatherForecastLocalKeyValueDataSource
) {

    operator fun invoke(value: Boolean): Single<Either<Failure, Unit>> {
        Timber.d("Updating settings: Setting $AUTOMATIC_LOCATION_MODE_KEY to $value")
        return repo.putBoolean(AUTOMATIC_LOCATION_MODE_KEY, value)
    }
}