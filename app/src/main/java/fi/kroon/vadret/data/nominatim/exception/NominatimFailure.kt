package fi.kroon.vadret.data.nominatim.exception

import fi.kroon.vadret.data.exception.Failure

class NominatimFailure {
    object NominatimNotAvailable : Failure.FeatureFailure()
}