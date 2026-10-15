package app.vela.bridge

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * Invisible bridge activity (stn_bridge pattern).
 *
 * Receives navigation intents meant for Google Maps (google.navigation:,
 * geo:, Google Maps https links), translates the destination into a plain
 * geo: URI, and forwards it to Vela (app.vela). Never shows UI.
 */
class BridgeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handle(intent)
        finish()
    }

    private fun handle(intent: Intent?) {
        val data = intent?.data
        Log.i(TAG, "bridge got action=${intent?.action} data=$data via pkg=${intent?.getPackage()}")
        val dest = NavUriParser.parse(data)
        if (dest == null) {
            // Unrecognized shape — hand the raw URI to Vela untouched; it may know it.
            if (data != null && tryVela(data)) return
            toast("Vela Bridge: couldn't understand navigation request: $data")
            return
        }
        // 1) Parsed geo: URI, explicitly to Vela.
        if (tryVela(dest.toGeoUri())) return
        // 2) Original URI explicitly to Vela (in case it handles google.navigation: itself).
        if (data != null && tryVela(data)) return
        // 3) Implicit geo: — system picker; the user can set Vela as "always".
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, dest.toGeoUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            toast("Vela Bridge: nothing can open $dest — is Vela installed?")
        }
    }

    /** Explicit intent at Vela. Returns false if Vela can't handle this URI. */
    private fun tryVela(uri: Uri): Boolean {
        return try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .setPackage(VELA_PACKAGE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Log.i(TAG, "forwarded to $VELA_PACKAGE: $uri")
            true
        } catch (e: ActivityNotFoundException) {
            Log.i(TAG, "$VELA_PACKAGE cannot handle $uri")
            false
        }
    }

    private fun toast(msg: String) {
        // Toasts survive finish(), so this still shows.
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "VelaBridge"
        const val VELA_PACKAGE = "app.vela"
    }
}
