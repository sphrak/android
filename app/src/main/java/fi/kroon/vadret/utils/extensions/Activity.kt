package fi.kroon.vadret.utils.extensions

import android.app.Activity
import fi.kroon.vadret.BaseApplication
import fi.kroon.vadret.core.di.component.CoreApplicationComponent

fun Activity.appComponent(): CoreApplicationComponent =
    BaseApplication
        .appComponent(this)