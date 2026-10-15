package app.vela.bridge

import android.net.Uri

/**
 * A navigation destination extracted from an incoming intent URI.
 */
data class Destination(
    val latitude: Double?,
    val longitude: Double?,
    val query: String?,
    /** 'd' driving, 'w' walking, 'b' bicycling — informational only. */
    val mode: Char?,
) {
    /** Standard geo: URI any map app (including Vela) can open. */
    fun toGeoUri(): Uri =
        if (latitude != null && longitude != null) {
            if (!query.isNullOrBlank()) Uri.parse("geo:$latitude,$longitude?q=${Uri.encode(query)}")
            else Uri.parse("geo:$latitude,$longitude")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(query.orEmpty())}")
        }

    override fun toString(): String =
        "Destination(lat=$latitude, lng=$longitude, query=$query, mode=$mode)"
}

/**
 * Parses the navigation URI shapes Uber/Google Maps/Waze use:
 *   google.navigation:q=<lat,lng|address>[&mode=d]
 *   geo:<lat>,<lng>[?q=<label>]   /   geo:0,0?q=<address>
 *   waze://?ll=<lat>,<lng>&navigate=yes   /   waze://?q=<address>
 *   https://www.google.com/maps/dir/?api=1&destination=...
 *   https://maps.google.com/maps?q=... / ?daddr=...
 */
object NavUriParser {

    private val COORDS =
        Regex("""^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*(?:\(.*\))?\s*$""")

    fun parse(uri: Uri?): Destination? {
        if (uri == null) return null
        return when (uri.scheme?.lowercase()) {
            "google.navigation" -> parseGoogleNavigation(uri)
            "geo" -> parseGeo(uri)
            "waze" -> parseWaze(uri)
            "http", "https" -> parseHttpsMapsUri(uri)
            else -> null
        }
    }

    /** google.navigation: is an opaque URI — parse the scheme-specific part manually. */
    private fun parseGoogleNavigation(uri: Uri): Destination? {
        val params = splitParams(uri.encodedSchemeSpecificPart ?: return null)
        val q = params["q"]?.trim().orEmpty()
        if (q.isEmpty()) return null
        val mode = params["mode"]?.trim()?.firstOrNull()?.lowercaseChar()
        return destFromQuery(q, mode)
    }

    private fun parseGeo(uri: Uri): Destination? {
        val ssp = uri.encodedSchemeSpecificPart ?: return null
        val head = ssp.substringBefore("?")
        val tail = ssp.substringAfter("?", "")
        val coords = head.substringBefore(";").split(",")
        val lat = coords.getOrNull(0)?.toDoubleOrNull()
        val lng = coords.getOrNull(1)?.toDoubleOrNull()
        val q = splitParams(tail)["q"]?.trim()
        return when {
            lat != null && lng != null && !(lat == 0.0 && lng == 0.0) ->
                Destination(lat, lng, q?.ifEmpty { null }, null)
            !q.isNullOrEmpty() -> Destination(null, null, q, null)
            else -> null
        }
    }

    /**
     * waze://?ll=<lat>,<lng>&navigate=yes  or  waze://?q=<address>&navigate=yes
     * (the documented Waze deep-link API — what apps fire at the Waze package).
     */
    private fun parseWaze(uri: Uri): Destination? {
        val query = uri.encodedQuery
            ?: uri.encodedSchemeSpecificPart?.substringAfter("?", "")
            ?: ""
        val params = splitParams(query)
        params["ll"]?.let { ll ->
            val parts = ll.split(",")
            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
            if (lat != null && lng != null) return Destination(lat, lng, null, null)
        }
        val q = params["q"]?.trim()
        if (!q.isNullOrEmpty()) return destFromQuery(q, null)
        return null
    }

    private fun parseHttpsMapsUri(uri: Uri): Destination? {
        val host = uri.host?.lowercase() ?: return null
        if (host != "www.google.com" && host != "maps.google.com" && host != "google.com") return null
        val dest = uri.getQueryParameter("destination")
            ?: uri.getQueryParameter("daddr")
            ?: uri.getQueryParameter("q")
            ?: return null
        if (dest.isBlank()) return null
        return destFromQuery(dest.trim(), null)
    }

    private fun destFromQuery(q: String, mode: Char?): Destination {
        val m = COORDS.matchEntire(q)
        return if (m != null) {
            Destination(m.groupValues[1].toDouble(), m.groupValues[2].toDouble(), null, mode)
        } else {
            Destination(null, null, q, mode)
        }
    }

    /** Split an encoded "a=b&c=d" string into a decoded map. */
    private fun splitParams(encoded: String): Map<String, String> =
        encoded.split("&").mapNotNull { part ->
            val i = part.indexOf("=")
            if (i < 0) null
            else Uri.decode(part.substring(0, i)) to Uri.decode(part.substring(i + 1))
        }.toMap()
}
