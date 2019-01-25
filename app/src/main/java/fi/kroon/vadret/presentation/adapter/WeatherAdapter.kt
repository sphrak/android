package fi.kroon.vadret.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fi.kroon.vadret.R
import fi.kroon.vadret.data.weather.model.TimeSerie
import fi.kroon.vadret.di.scope.VadretApplicationScope
import fi.kroon.vadret.utils.MPS_TO_KMPH_FACTOR
import fi.kroon.vadret.utils.WINDCHILL_FORMULA_MAXIMUM
import fi.kroon.vadret.utils.WINDCHILL_FORMULA_MINIMUM
import fi.kroon.vadret.utils.extensions.toInvisible
import fi.kroon.vadret.utils.extensions.toVisible
import fi.kroon.vadret.utils.extensions.toWindChill
import kotlinx.android.synthetic.main.weather_forecast_date_item.view.*
import kotlinx.android.synthetic.main.weather_forecast_item.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.threeten.bp.format.TextStyle
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.Delegates

@VadretApplicationScope
class WeatherAdapter @Inject constructor() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal var collection: List<Any> by Delegates.observable(listOf()) { _, _, _ ->
        notifyDataSetChanged()
    }

    companion object {
        const val TYPE_WEEKDAY = 0
        const val TYPE_FORECAST = 1
    }

    class WeekdayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(localDate: LocalDate) {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            itemView.date.text = localDate.format(formatter)
            itemView.weekDay.text = localDate
                .dayOfWeek
                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                .toUpperCase()
        }
    }

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(timeSerie: TimeSerie) {

            itemView.time.text = OffsetDateTime.parse(timeSerie.validTime).toLocalTime().toString()

            timeSerie.parameters.map { parameter ->
                if (parameter.name == "t") {
                    Timber.d("t: ${parameter.values.first()}")
                    itemView.temperature.text = parameter.values.first().toString()
                    if (parameter.values.first() < WINDCHILL_FORMULA_MAXIMUM) {
                        Timber.d("Temperature ${parameter.values.first()} is less than $WINDCHILL_FORMULA_MAXIMUM")
                        timeSerie.parameters.map {
                            when (it.name) {
                                "ws" -> {
                                    if (it.values.first() * MPS_TO_KMPH_FACTOR > WINDCHILL_FORMULA_MINIMUM) {
                                        Timber.d("Windspeed ${it.values.first() * MPS_TO_KMPH_FACTOR}  is greater than $WINDCHILL_FORMULA_MINIMUM so we render 'feels like'")
                                        Timber.d("Feels like: ${parameter.values.first().toWindChill(it.values.first())}")
                                        itemView.feelsLikeTemperature.text = parameter.values.first().toWindChill(it.values.first())
                                        itemView.feelsLikeTemperature.toVisible()
                                        itemView.feelsLike.toVisible()
                                        itemView.feelsLikeTempUnit.toVisible()
                                    } else {
                                        itemView.feelsLikeTemperature.toInvisible()
                                        itemView.feelsLike.toInvisible()
                                        itemView.feelsLikeTempUnit.toInvisible()
                                        Timber.d("Windspeed ${it.values.first() * MPS_TO_KMPH_FACTOR}  is less than $WINDCHILL_FORMULA_MINIMUM so we DONT render 'feels like'")
                                    }
                                }
                            }
                        }
                    }
                } else if (parameter.name == "Wsymb2") {
                    itemView.wsymb2.setText(handleWsymb2Description(parameter.values.first().toInt()))
                    itemView.wsymb2Icon.setImageResource(handleWsymb2Icon(parameter.values.first().toInt()))
                }
            }
        }

        private fun handleWsymb2Description(index: Int): Int {
            return when (index) {
                1 -> R.string.wsymb2_clear_sky
                2 -> R.string.wsymb2_nearly_clear_sky
                3 -> R.string.wsymb2_variable_cloudiness
                4 -> R.string.wsymb2_halfclear_sky
                5 -> R.string.wsymb2_cloudy_sky
                6 -> R.string.wsymb2_overcast
                7 -> R.string.wsymb2_fog
                8 -> R.string.wsymb2_light_rain_showers
                9 -> R.string.wsymb2_moderate_rain_showers
                10 -> R.string.wsymb2_heavy_rain_showers
                11 -> R.string.wsymb2_thunderstorm
                12 -> R.string.wsymb2_light_sleet_showers
                13 -> R.string.wsymb2_moderate_sleet_showers
                14 -> R.string.wsymb2_heavy_sleet_showers
                15 -> R.string.wsymb2_light_snow_showers
                16 -> R.string.wsymb2_moderate_snow_showers
                17 -> R.string.wsymb2_heavy_snow_showers
                18 -> R.string.wsymb2_light_rain
                19 -> R.string.wsymb2_moderate_rain
                20 -> R.string.wsymb2_heavy_rain
                21 -> R.string.wsymb2_thunder
                22 -> R.string.wsymb2_light_sleet
                23 -> R.string.wsymb2_moderate_sleet
                24 -> R.string.wsymb2_heavy_sleet
                25 -> R.string.wsymb2_light_snowfall
                26 -> R.string.wsymb2_moderate_snowfall
                27 -> R.string.wsymb2_heavy_snowfall
                else -> {
                    R.string.wsymb2_clear_sky
                }
            }
        }

        private fun handleWsymb2Icon(index: Int): Int =
            when (index) {
                1 -> R.drawable.wsymb2_clear_sky
                2 -> R.drawable.wsymb2_nearly_clear_sky
                3 -> R.drawable.wsymb2_variable_cloudiness
                4 -> R.drawable.wsymb2_halfclear_sky
                5 -> R.drawable.wsymb2_cloudy_sky
                6 -> R.drawable.wsymb2_overcast
                7 -> R.drawable.wsymb2_fog
                8 -> R.drawable.wsymb2_light_rain_showers
                9 -> R.drawable.wsymb2_moderate_rain_showers
                10 -> R.drawable.wsymb2_heavy_rain_showers
                11 -> R.drawable.wsymb2_thunderstorm
                12 -> R.drawable.wsymb2_light_sleet_showers
                13 -> R.drawable.wsymb2_moderate_sleet_showers
                14 -> R.drawable.wsymb2_heavy_sleet_showers
                15 -> R.drawable.wsymb2_light_snow_showers
                16 -> R.drawable.wsymb2_moderate_snow_showers
                17 -> R.drawable.wsymb2_heavy_snow_showers
                18 -> R.drawable.wsymb2_light_rain
                19 -> R.drawable.wsymb2_moderate_rain
                20 -> R.drawable.wsymb2_heavy_rain
                21 -> R.drawable.wsymb2_thunder
                22 -> R.drawable.wsymb2_light_sleet
                23 -> R.drawable.wsymb2_moderate_sleet
                24 -> R.drawable.wsymb2_heavy_sleet
                25 -> R.drawable.wsymb2_light_snowfall
                26 -> R.drawable.wsymb2_moderate_snowfall
                27 -> R.drawable.wsymb2_heavy_snowfall
                else -> {
                    R.drawable.wsymb2_clear_sky
                }
            }

        private fun handlePrSort(prSort: Int) {
            when (prSort) {
                0 -> R.string.prsort_no_precipitation
                1 -> R.string.prsort_snow
                2 -> R.string.prsort_snow_and_rain
                3 -> R.string.prsort_rain
                4 -> R.string.prsort_drizzle
                5 -> R.string.prsort_freezing_rain
                6 -> R.string.prsort_freezing_drizzle
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (collection[position]) {
            is LocalDate -> TYPE_WEEKDAY
            else -> TYPE_FORECAST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_WEEKDAY -> WeekdayViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.weather_forecast_date_item, parent, false))
            else -> ForecastViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.weather_forecast_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_FORECAST -> (holder as ForecastViewHolder).bind(collection[position] as TimeSerie)
            TYPE_WEEKDAY -> (holder as WeekdayViewHolder).bind(collection[position] as LocalDate)
        }
    }

    override fun getItemCount(): Int = collection.size
}