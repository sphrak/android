package fi.kroon.vadret.domain.weatherforecast

import fi.kroon.vadret.data.exception.Failure
import fi.kroon.vadret.data.functional.Either
import fi.kroon.vadret.data.functional.flatMap
import fi.kroon.vadret.data.functional.flatMapSingle
import fi.kroon.vadret.data.functional.map
import fi.kroon.vadret.data.location.model.Location
import fi.kroon.vadret.data.nominatim.model.NominatimReverseOut
import fi.kroon.vadret.data.weatherforecast.model.Weather
import fi.kroon.vadret.data.weatherforecast.model.WeatherOut
import fi.kroon.vadret.domain.BaseService
import fi.kroon.vadret.presentation.weatherforecast.WeatherForecastMapper
import fi.kroon.vadret.presentation.weatherforecast.model.BaseWeatherForecastModel
import fi.kroon.vadret.utils.FIVE_MINUTES_IN_MILLIS
import fi.kroon.vadret.utils.extensions.asSingle
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import timber.log.Timber
import javax.inject.Inject

class GetWeatherForecastService @Inject constructor(
    private val getWeatherTask: GetWeatherTask,
    private val getLocationAutomaticTask: GetLocationAutomaticTask,
    private val getReverseLocalityNameTask: GetReverseLocalityNameTask,
    private val getLocationModeTask: GetLocationModeTask,
    private val getLocationManualTask: GetLocationManualTask,
    private val setWeatherForecastDiskCacheTask: SetWeatherForecastDiskCacheTask,
    private val setWeatherForecastMemoryCacheTask: SetWeatherForecastMemoryCacheTask,
    private val getWeatherForecastMemoryCacheTask: GetWeatherForecastMemoryCacheTask,
    private val getWeatherForecastDiskCacheTask: GetWeatherForecastDiskCacheTask
) : BaseService() {

    data class Data(
        val baseWeatherForecastModelList: List<BaseWeatherForecastModel> = listOf(),
        val localityName: String? = null,
        val forceNet: Boolean = false,
        val weather: Weather? = null,
        val weatherOut: WeatherOut? = null,
        val timeStamp: Long? = null,
        val locationMode: Boolean = false,
        val location: Location? = null
    )

    operator fun invoke(timeStamp: Long?, forceNet: Boolean): Single<Either<Failure, Data>> =
        Single.just(Data(timeStamp = timeStamp, forceNet = forceNet))
            .flatMap(::getLocationMode)
            .flatMap(::getGpsLocationOrStoredLocation)
            .flatMap(::getWeatherForecastList)
            .flatMap(::doReverseNominatimLookupOrReturn)
            .map(::mapWeatherToWeatherForecastList)

    /**
     *  Decides if location should be derived from GPS or local storage.
     */
    private fun getLocationMode(data: Data): Single<Either<Failure, Data>> =
        getLocationModeTask()
            .map { either: Either<Failure, Boolean> ->
                either.map { locationMode ->
                    data.copy(locationMode = locationMode)
                }
            }

    private fun getGpsLocationOrStoredLocation(either: Either<Failure, Data>): Single<Either<Failure, Data>> =
        either.flatMapSingle { data: Data ->
            Timber.d("GetWeatherForecastService invoked with location auto: $data")
            when (data.locationMode) {
                false -> getLocationManual(data)
                true -> getLocationAutomatic(data)
                    .map(::mapLocationEntityToWeatherOut)
            }
        }

    private fun getLocationAutomatic(data: Data): Single<Either<Failure, Data>> =
        getLocationAutomaticTask().map { either: Either<Failure, Location> ->
            either.map { location: Location ->
                data.copy(location = location)
            }
        }

    private fun getLocationManual(data: Data): Single<Either<Failure, Data>> =
        getLocationManualTask()
            .map { either: Either<Failure, WeatherOut> ->
                either.map { weatherOut: WeatherOut ->
                    data.copy(
                        weatherOut = weatherOut,
                        localityName = weatherOut.localityName!!
                    )
                }
            }

    /**
     *  Takes location and creates a weather forecast request object
     */
    private fun mapLocationEntityToWeatherOut(either: Either<Failure, Data>): Either<Failure, Data> =
        either.map { data: Data ->
            val weatherOut = WeatherOut(
                latitude = data.location!!.latitude,
                longitude = data.location.longitude
            )
            data.copy(weatherOut = weatherOut)
        }

    private fun getWeatherForecastList(either: Either<Failure, Data>): Single<Either<Failure, Data>> =
        either.flatMapSingle { data: Data ->
            getWeather(data)
                .map { either: Either<Failure, Data> ->
                    either.map { dataIn: Data ->
                        dataIn.copy(
                            timeStamp = currentTimeMillis
                        )
                    }
                }
        }

    private fun doReverseNominatimLookupOrReturn(either: Either<Failure, Data>): Single<Either<Failure, Data>> =
        either.flatMapSingle { data: Data ->
            when (data.locationMode) {
                true -> doReverseNominatimLookup(either)
                false -> {
                    either.asSingle()
                }
            }
        }

    private fun doReverseNominatimLookup(either: Either<Failure, Data>): Single<Either<Failure, Data>> =
        either.flatMapSingle { data: Data ->
            val nominatimReverseOut = NominatimReverseOut(
                latitude = data.weatherOut!!.latitude,
                longitude = data.weatherOut.longitude
            )
            getReverseLocalityNameTask(nominatimReverseOut).map { result ->
                result.map { localityName: String? ->
                    localityName?.let {
                        data.copy(localityName = localityName)
                    } ?: data
                }
            }
        }

    private fun getWeather(data: Data): Single<Either<Failure, Data>> =
        with(data) {
            when {
                forceNet || (currentTimeMillis > (timeStamp!! + FIVE_MINUTES_IN_MILLIS)) -> {
                    getWeatherTask(data.weatherOut!!)
                        .map { either: Either<Failure, Weather> ->
                            either.map { weather: Weather ->
                                data.copy(weather = weather)
                            }
                        }.flatMap { either: Either<Failure, Data> ->
                            updateCache(either)
                        }
                }
                else -> {
                    Single.merge(
                        getWeatherForecastMemoryCacheTask()
                            .map { either: Either<Failure, Weather> ->
                                either.map { weather ->
                                    data.copy(weather = weather)
                                }
                            },
                        getWeatherForecastDiskCacheTask()
                            .map { either: Either<Failure, Weather> ->
                                either.map { weather ->
                                    data.copy(weather = weather)
                                }
                            }
                    ).filter { result ->
                        result.either(
                            {
                                false
                            },
                            { data ->
                                Timber.d("Fetched from cache: ${data.weather}")
                                data
                                    .weather!!
                                    .timeSeries!!
                                    .isNotEmpty()
                            }
                        )
                    }.take(1)
                        .switchIfEmpty(
                            getWeatherTask(data.weatherOut!!)
                                .map { either: Either<Failure, Weather> ->
                                    Timber.d("Cache was empty, fetching weather from network.")
                                    either.map { weather: Weather ->
                                        data.copy(weather = weather)
                                    }
                                }.flatMap { data ->
                                    updateCache(data)
                                }.toFlowable()
                        ).singleOrError()
                }
            }
        }

    private fun updateCache(either: Either<Failure, Data>): Single<Either<Failure, Data>> =
        either.flatMapSingle { data: Data ->
            setWeatherForecastMemoryCacheTask(data.weather!!)
                .zipWith(setWeatherForecastDiskCacheTask(data.weather))
                .map { pair: Pair<Either<Failure, Weather>,
                    Either<Failure, Weather>> ->
                    Timber.i("Updating cache")
                    val (firstEither: Either<Failure, Weather>,
                        secondEither: Either<Failure, Weather>) = pair
                    firstEither.flatMap { _: Weather ->
                        secondEither.map { _: Weather ->
                            data
                        }
                    }
                }
        }

    private fun mapWeatherToWeatherForecastList(either: Either<Failure, Data>): Either<Failure, Data> =
        either.map { data: Data ->
            val locationEntity = Location(data.weatherOut!!.latitude, data.weatherOut.longitude)
            val baseWeatherForecastModelList: List<BaseWeatherForecastModel> = WeatherForecastMapper
                .toWeatherForecastModel(
                    data.weather!!.timeSeries!!,
                    locationEntity
                )
            data.copy(baseWeatherForecastModelList = baseWeatherForecastModelList)
        }
}