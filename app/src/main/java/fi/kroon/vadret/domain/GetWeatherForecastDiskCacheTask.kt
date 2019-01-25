package fi.kroon.vadret.domain

import fi.kroon.vadret.data.cache.WeatherForecastCacheRepository
import fi.kroon.vadret.data.exception.Failure
import fi.kroon.vadret.data.functional.Either
import fi.kroon.vadret.data.weather.model.Weather
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

class GetWeatherForecastDiskCacheTask @Inject constructor(
    private val cacheRepo: WeatherForecastCacheRepository
) {
    operator fun invoke(): Single<Either<Failure, Weather>> =
        cacheRepo
            .getDiskCache()
            .doOnError {
                Timber.e("DisplayError getting disk cache: $it")
            }
}