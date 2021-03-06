package fi.kroon.vadret.presentation.radar

import android.os.Parcelable
import androidx.annotation.StringRes
import fi.kroon.vadret.data.radar.model.File
import fi.kroon.vadret.utils.NIL_INT
import kotlinx.android.parcel.Parcelize

object RadarView {

    sealed class Event {
        class OnViewInitialised(val stateParcel: StateParcel?) : Event()
        object OnFailureHandled : Event()
        object OnSeekBarStopped : Event()
        object OnStateParcelUpdated : Event()
        object OnPlayButtonStarted : Event()
        object OnPlayButtonStopped : Event()
        object OnPlayButtonClicked : Event()
        object OnSeekBarReset : Event()
        object OnSeekBarRestored : Event()
        class OnRadarImageDisplayed(val position: Int) : Event()
        class OnPositionUpdated(val position: Int) : Event()
    }

    data class State(
        val isInitialised: Boolean = false,
        val isSeekBarRunning: Boolean = false,
        val seekBarMax: Int = NIL_INT,
        val seekStep: Int = 1,
        val currentSeekBarIndex: Int = NIL_INT,
        val renderEvent: RenderEvent = RenderEvent.None,
        val timeStamp: Long? = null,
        val wasRestoredFromStateParcel: Boolean = false
    )

    sealed class RenderEvent {
        object None : RenderEvent()
        object UpdateStateParcel : RenderEvent()
        object StartSeekBar : RenderEvent()
        object StopSeekBar : RenderEvent()
        object ResetSeekBar : RenderEvent()
        object RestoreSeekBarPosition : RenderEvent()
        object SetPlayButtonToStopped : RenderEvent()
        object SetPlayButtonToPlaying : RenderEvent()
        class DisplayRadarImage(val file: File) : RenderEvent()
        class DisplayError(@StringRes val errorCode: Int) : RenderEvent()
    }

    @Parcelize
    data class StateParcel(
        val isInitialised: Boolean,
        val isSeekBarRunning: Boolean = false,
        val seekBarMax: Int,
        val currentSeekBarIndex: Int,
        val timeStamp: Long? = null
    ) : Parcelable
}