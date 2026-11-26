package khaled.benzo.core.net

/**
 * Which network paths the chosen proxy covers. The Settings row shows this whenever a proxy is selected, so a
 * global-looking setting never hides a path that connects directly. Update this list with any new network path.
 */
object ProxyCoverage {
    enum class Route { LINK_DOWNLOADS, EDITOR_SIZE_CHECK, TORRENTS, BROWSER, PER_SITE_PROXY, PER_ITEM_PROXY }
    enum class State { PROXIED, DIRECT, NOT_BUILT }

    val routes: Map<Route, State> = mapOf(
        Route.LINK_DOWNLOADS to State.PROXIED,
        Route.EDITOR_SIZE_CHECK to State.PROXIED,
        Route.TORRENTS to State.DIRECT,
        Route.BROWSER to State.DIRECT,
        Route.PER_SITE_PROXY to State.NOT_BUILT,
        Route.PER_ITEM_PROXY to State.NOT_BUILT
    )

    fun direct(): List<Route> = routes.filterValues { it == State.DIRECT }.keys.toList()
}
