package fi.kroon.vadret.data.aboutinfo.local

import fi.kroon.vadret.R
import fi.kroon.vadret.core.di.scope.CoreApplicationScope
import fi.kroon.vadret.data.aboutinfo.model.AboutInfo
import fi.kroon.vadret.data.exception.Failure
import fi.kroon.vadret.data.functional.Either
import fi.kroon.vadret.utils.extensions.asRight
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

@CoreApplicationScope
class AboutInfoLocalDataSource @Inject constructor() {

    operator fun invoke(): Single<Either<Failure, List<AboutInfo>>> = Single.fromCallable {
        listOf(
            AboutInfo(
                iconResourceId = R.drawable.ic_info_outline,
                titleResourceId = R.string.version_row_title,
                hintResourceId = R.string.app_version,
                urlResourceId = R.string.app_release_page
            ),
            AboutInfo(
                iconResourceId = R.drawable.ic_copyright,
                titleResourceId = R.string.license_row_title,
                hintResourceId = R.string.license_row_hint,
                urlResourceId = R.string.app_license_url
            ),
            AboutInfo(
                iconResourceId = R.drawable.ic_history,
                titleResourceId = R.string.changelog_row_title,
                urlResourceId = R.string.app_github_changelog_url
            ),
            AboutInfo(
                iconResourceId = R.drawable.ic_code,
                titleResourceId = R.string.source_code_row_title,
                urlResourceId = R.string.app_github_source_url
            )
        ).asRight() as Either<Failure, List<AboutInfo>>
    }.doOnError {
        Timber.e("$it")
    }
}