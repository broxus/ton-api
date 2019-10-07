package utils

import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSFilter
import play.filters.headers.SecurityHeadersFilter

class Filters @Inject()(corsFilter: CORSFilter, securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
    override def filters: Seq[EssentialFilter] = Seq(corsFilter, securityHeadersFilter)
}
