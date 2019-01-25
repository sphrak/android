package fi.kroon.vadret.presentation.weatherforecast

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.view.clicks
import fi.kroon.vadret.R
import fi.kroon.vadret.data.autocomplete.model.AutoCompleteItem
import fi.kroon.vadret.data.nominatim.model.Locality
import fi.kroon.vadret.presentation.BaseFragmentV2
import fi.kroon.vadret.presentation.MainActivity
import fi.kroon.vadret.presentation.weatherforecast.autocomplete.AutoCompleteAdapter
import fi.kroon.vadret.presentation.weatherforecast.di.WeatherForecastComponent
import fi.kroon.vadret.presentation.weatherforecast.di.WeatherForecastScope
import fi.kroon.vadret.utils.Schedulers
import fi.kroon.vadret.utils.extensions.appComponent
import fi.kroon.vadret.utils.extensions.snack
import fi.kroon.vadret.utils.extensions.toGone
import fi.kroon.vadret.utils.extensions.toInvisible
import fi.kroon.vadret.utils.extensions.toObservable
import fi.kroon.vadret.utils.extensions.toVisible
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.weather_forecast_fragment.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber
import javax.inject.Inject

@RuntimePermissions
@WeatherForecastScope
class WeatherForecastFragment : BaseFragmentV2() {

    companion object {
        const val STATE_PARCEL_KEY: String = "STATE_PARCEL_KEY"
        const val RESTORABLE_SCROLL_POSITION_KEY: String = "RESTORABLE_SCROLL_POSITION_KEY"
    }

    override fun layoutId(): Int = R.layout.weather_forecast_fragment

    @Inject
    lateinit var viewModel: WeatherForecastViewModel

    @Inject
    lateinit var onViewInitialisedEventSubject: PublishSubject<WeatherForecastView.Event.OnViewInitialised>

    @Inject
    lateinit var onLocationPermissionDeniedSubject: PublishSubject<WeatherForecastView.Event.OnLocationPermissionDenied>

    @Inject
    lateinit var onLocationPermissionDeniedNeverAskAgainSubject: PublishSubject<WeatherForecastView.Event.OnLocationPermissionDeniedNeverAskAgain>

    @Inject
    lateinit var onLocationPermissionGrantedSubject: PublishSubject<WeatherForecastView.Event.OnLocationPermissionGranted>

    @Inject
    lateinit var onStartShimmerEffectSubject: PublishSubject<WeatherForecastView.Event.OnShimmerEffectStarted>

    @Inject
    lateinit var onShimmerEffectStoppedSubject: PublishSubject<WeatherForecastView.Event.OnShimmerEffectStopped>

    @Inject
    lateinit var onProgressBarEffectStartedSubject: PublishSubject<WeatherForecastView.Event.OnProgressBarEffectStarted>

    @Inject
    lateinit var onProgressBarEffectStoppedSubject: PublishSubject<WeatherForecastView.Event.OnProgressBarEffectStopped>

    @Inject
    lateinit var onAutoCompleteItemClickedSubject: PublishSubject<AutoCompleteItem>

    @Inject
    lateinit var onSearchViewDismissedSubject: PublishSubject<WeatherForecastView.Event.OnSearchViewDismissed>

    @Inject
    lateinit var onFailureHandledSubject: PublishSubject<WeatherForecastView.Event.OnFailureHandled>

    @Inject
    lateinit var onWeatherListVisibleSubject: PublishSubject<WeatherForecastView.Event.OnWeatherListDisplayed>

    @Inject
    lateinit var onScrollPositionRestoredSubject: PublishSubject<WeatherForecastView.Event.OnScrollPositionRestored>

    @Inject
    lateinit var onStateParcelUpdatedSubject: PublishSubject<WeatherForecastView.Event.OnStateParcelUpdated>

    @Inject
    lateinit var weatherForecastAdapter: WeatherForecastAdapter

    @Inject
    lateinit var autoCompleteAdapter: AutoCompleteAdapter

    @Inject
    lateinit var schedulers: Schedulers

    private var isConfigChangeOrProcessDeath = false
    private var stateParcel: WeatherForecastView.StateParcel? = null
    private var bundle: Bundle? = null

    private val cmp: WeatherForecastComponent by lazy {
        appComponent()
            .forecastComponentBuilder()
            .build()
    }

    override fun onAttach(context: Context) {
        Timber.d("-----BEGIN-----")
        Timber.d("ON ATTACH")
        cmp.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("ON CREATE")
        savedInstanceState?.let { restoredBundle ->
            if (bundle == null) {
                Timber.d("savedInstanceState restored: $restoredBundle")
                bundle = restoredBundle
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setup()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Timber.d("ON SAVEINSTANCESTATE")
        outState.apply {
            putParcelable(RESTORABLE_SCROLL_POSITION_KEY,
                (weatherForecastRecyclerView.layoutManager as LinearLayoutManager)
                    .onSaveInstanceState()
            )
            Timber.d("Saving instance: $stateParcel")
            Timber.d("-----END-----")
            putParcelable(STATE_PARCEL_KEY, stateParcel)
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("ON STOP")
        isConfigChangeOrProcessDeath = true
    }

    override fun onResume() {
        super.onResume()
        Timber.d("ON RESUME")
        if (isConfigChangeOrProcessDeath) {
            setupEvents()
            isConfigChangeOrProcessDeath = false
        }
    }

    private fun setup() {
        setupEvents()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        weatherForecastRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = weatherForecastAdapter
        }
    }

    private fun setupEvents() {

        if (subscriptions.size() == 0) {
            weatherForecastSearchView
                .setOnCloseListener {
                    onSearchViewDismissedSubject.onNext(
                        WeatherForecastView
                            .Event
                            .OnSearchViewDismissed
                    )
                    true
                }
            Observable.mergeArray(
                onViewInitialisedEventSubject
                    .toObservable(),
                onLocationPermissionGrantedSubject
                    .toObservable(),
                onLocationPermissionDeniedSubject
                    .toObservable(),
                onLocationPermissionDeniedNeverAskAgainSubject
                    .toObservable(),
                onSearchViewDismissedSubject
                    .toObservable(),
                onWeatherListVisibleSubject
                    .toObservable(),
                onScrollPositionRestoredSubject
                    .toObservable(),
                onFailureHandledSubject
                    .toObservable(),
                onStartShimmerEffectSubject
                    .toObservable(),
                onShimmerEffectStoppedSubject
                    .toObservable(),
                onProgressBarEffectStartedSubject
                    .toObservable(),
                onProgressBarEffectStoppedSubject
                    .toObservable(),
                onStateParcelUpdatedSubject
                    .toObservable(),
                onAutoCompleteItemClickedSubject
                    .toObservable()
                    .map { item: AutoCompleteItem ->
                        WeatherForecastView
                            .Event
                            .OnAutoCompleteItemClicked(item)
                    },
                weatherForecastLocationSearchButton
                    .clicks()
                    .map {
                        WeatherForecastView
                            .Event
                            .OnSearchButtonToggled
                    },
                weatherForecastRefresh
                    .refreshes()
                    .map {
                        WeatherForecastView
                            .Event
                            .OnSwipedToRefresh
                    },
                weatherForecastSearchView
                    .queryTextChangeEvents()
                    .skipInitialValue()
                    .map { searchEvent ->
                        when {
                            searchEvent.isSubmitted -> WeatherForecastView
                                .Event
                                .OnSearchButtonSubmitted(searchEvent.queryText.toString())
                            else -> WeatherForecastView
                                .Event
                                .OnSearchTextChanged(
                                    searchEvent.queryText.toString()
                                )
                        }
                    }
            ).observeOn(
                schedulers.io()
            ).compose(
                viewModel()
            ).observeOn(
                schedulers.ui()
            ).subscribe(
                ::render
            ).addTo(
                subscriptions
            )

            onViewInitialisedEventSubject
                .onNext(
                    WeatherForecastView
                        .Event
                        .OnViewInitialised(
                            stateParcel = bundle?.getParcelable(
                                STATE_PARCEL_KEY
                            )
                        )
                )
        }
    }

    private fun render(viewState: WeatherForecastView.State) =
        when (viewState.renderEvent) {
            WeatherForecastView.RenderEvent.None -> Unit
            WeatherForecastView.RenderEvent.RequestLocationPermission -> onRequestLocationPermission()
            WeatherForecastView.RenderEvent.StartShimmerEffect -> startShimmerEffect()
            WeatherForecastView.RenderEvent.StartProgressBarEffect -> startProgressBarEffect()
            WeatherForecastView.RenderEvent.StopShimmerEffect -> stopShimmerEffect()
            WeatherForecastView.RenderEvent.StopProgressBarEffect -> stopProgressBarEffect()
            WeatherForecastView.RenderEvent.OmitLocationPermissionGrantedCheck -> omitLocationPermissionGrantedCheck()
            WeatherForecastView.RenderEvent.OmitLocationPermissionDeniedCheck -> omitLocationPermissionDeniedCheck()
            WeatherForecastView.RenderEvent.EnableSearchView -> enableSearchView()
            WeatherForecastView.RenderEvent.RestoreScrollPosition -> restoreScrollPosition()
            is WeatherForecastView.RenderEvent.DisableSearchView -> disableSearchView(viewState.renderEvent)
            is WeatherForecastView.RenderEvent.DisplayAutoComplete -> displayAutoCompleteList(viewState, viewState.renderEvent)
            is WeatherForecastView.RenderEvent.DisplayWeatherForecast -> {
                displayWeatherForecast(viewState.renderEvent)
            }
            is WeatherForecastView.RenderEvent.DisplayError -> renderError(viewState.renderEvent.errorCode)
            WeatherForecastView.RenderEvent.UpdateStateParcel -> updateStateParcel(viewState)
        }

    private fun startShimmerEffect() {
        Timber.d("startShimmerEffect")
        shimmerEffect.startShimmer()
        shimmerEffect.toVisible()
        onStartShimmerEffectSubject.onNext(
            WeatherForecastView
                .Event
                .OnShimmerEffectStarted
        )
    }

    private fun startProgressBarEffect() {
        Timber.d("startProgressBarEffect")
        weatherForecastLoadingProgressBar.toVisible()
        onProgressBarEffectStartedSubject.onNext(
            WeatherForecastView
                .Event
                .OnProgressBarEffectStarted
        )
    }

    private fun stopShimmerEffect() {
        Timber.d("stopShimmerEffect")
        shimmerEffect.stopShimmer()
        shimmerEffect.toGone()
        onShimmerEffectStoppedSubject.onNext(
            WeatherForecastView
                .Event
                .OnShimmerEffectStopped
        )
    }

    private fun stopProgressBarEffect() {
        Timber.d("stopProgressBarEffect")
        weatherForecastLoadingProgressBar.toGone()
        weatherForecastRefresh.isRefreshing = false
        onProgressBarEffectStoppedSubject.onNext(
            WeatherForecastView
                .Event
                .OnProgressBarEffectStopped
        )
    }

    private fun displayAutoCompleteList(viewState: WeatherForecastView.State, renderEvent: WeatherForecastView.RenderEvent.DisplayAutoComplete) {
        autoCompleteAdapter.updateList(renderEvent.newFilteredList)
        updateStateParcel(viewState)
        autoCompleteRecyclerView.adapter?.run {
            renderEvent.diffResult?.dispatchUpdatesTo(this)
        }
    }

    private fun restoreScrollPosition() {
        Timber.d("RESTORE SCROLL POSITION")
        bundle?.run {
            (weatherForecastRecyclerView.layoutManager as LinearLayoutManager)
                .onRestoreInstanceState(
                    getParcelable(RESTORABLE_SCROLL_POSITION_KEY)
                )
        }

        onScrollPositionRestoredSubject.onNext(
            WeatherForecastView
                .Event
                .OnScrollPositionRestored
        )
    }

    private fun updateStateParcel(state: WeatherForecastView.State) {
        stateParcel = WeatherForecastView.StateParcel(
            searchText = state.searchText,
            isSearchToggled = state.isSearchToggled,
            forceNet = state.forceNet,
            startLoading = state.startLoading,
            startRefreshing = state.startRefreshing,
            stopLoading = state.stopLoading,
            stopRefreshing = state.stopRefreshing,
            timeStamp = state.timeStamp,
            hasRunTimeLocationPermission = state.hasRunTimeLocationPermission
        )

        onStateParcelUpdatedSubject.onNext(
            WeatherForecastView
                .Event
                .OnStateParcelUpdated
        )
    }

    private fun disableSearchView(renderEvent: WeatherForecastView.RenderEvent.DisableSearchView) {
        Timber.d("Disabling search view, should be gone now!")
        weatherForecastLocationSearchButton.toVisible()
        weatherForecastSearchView.toInvisible()
        weatherForecastSearchView.setQuery(renderEvent.text, false)
        autoCompleteRecyclerView.adapter = null
        autoCompleteRecyclerView.toInvisible()

        onSearchViewDismissedSubject.onNext(
            WeatherForecastView
                .Event
                .OnSearchViewDismissed
        )
    }

    private fun enableSearchView() {
        autoCompleteAdapter.clearList()
        weatherForecastSearchView.toVisible()
        weatherForecastSearchView.isFocusable = true
        weatherForecastSearchView.isIconified = false
        weatherForecastSearchView.requestFocusFromTouch()

        if (!autoCompleteRecyclerView.isVisible) {
            autoCompleteRecyclerView.toVisible()
            autoCompleteRecyclerView.adapter = autoCompleteAdapter
            autoCompleteRecyclerView.layoutManager = LinearLayoutManager(this.context, RecyclerView.VERTICAL, false)
            autoCompleteRecyclerView.addItemDecoration(DividerItemDecoration(this.context, RecyclerView.VERTICAL))
            autoCompleteRecyclerView.hasFixedSize()
        }
        weatherForecastLocationSearchButton.toInvisible()
    }

    private fun hideActionBarLocalityName() =
        (requireActivity() as MainActivity).disableLocalityActionBar()

    private fun displayActionBarLocalityName(locality: Locality) =
        (requireActivity() as MainActivity).displayLocalityActionBar(locality)

    private fun displayWeatherForecast(renderEvent: WeatherForecastView.RenderEvent.DisplayWeatherForecast) {
        Timber.d("Rendering weather forecast data")
        weatherForecastAdapter.updateList(renderEvent.list)
        displayActionBarLocalityName(renderEvent.locality)

        onWeatherListVisibleSubject.onNext(
            WeatherForecastView
                .Event
                .OnWeatherListDisplayed
        )
    }

    private fun omitLocationPermissionGrantedCheck() =
        onLocationPermissionGrantedSubject
            .onNext(
                WeatherForecastView
                    .Event
                    .OnLocationPermissionGranted
            )

    private fun omitLocationPermissionDeniedCheck() =
        onLocationPermissionDeniedSubject
            .onNext(
                WeatherForecastView
                    .Event
                    .OnLocationPermissionDenied
            )

    private fun onRequestLocationPermission(): Unit =
        onLocationPermissionGrantedWithPermissionCheck()

    @NeedsPermission(value = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun onLocationPermissionGranted() =
        onLocationPermissionGrantedSubject
            .onNext(
                WeatherForecastView
                    .Event
                    .OnLocationPermissionGranted
            )

    @OnPermissionDenied(value = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun onLocationPermissionDenied() =
        onLocationPermissionDeniedSubject
            .onNext(
                WeatherForecastView
                    .Event
                    .OnLocationPermissionDenied
            )

    @OnNeverAskAgain(value = [Manifest.permission.ACCESS_FINE_LOCATION])
    fun onLocationPermissionNeverAskAgain() =
        onLocationPermissionDeniedNeverAskAgainSubject
            .onNext(
                WeatherForecastView
                    .Event
                    .OnLocationPermissionDeniedNeverAskAgain
            )

    override fun renderError(errorCode: Int) {
        snack(errorCode)
        Timber.d("Rendering error code: $errorCode")
        onFailureHandledSubject
            .onNext(
                WeatherForecastView
                    .Event
                    .OnFailureHandled
            )
    }
}