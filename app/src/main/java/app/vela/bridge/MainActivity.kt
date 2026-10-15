package app.vela.bridge

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * Launcher entry: shows install status and offers self-test buttons that fire
 * the same shapes of intent Uber Driver fires, so the bridge can be verified
 * without an actual trip.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val velaOk = isInstalled(BridgeActivity.VELA_PACKAGE)
        val uberOk = isInstalled(UBER_DRIVER)
        findViewById<TextView>(R.id.status).text =
            "Vela (app.vela): ${if (velaOk) "installed" else "NOT installed"}\n" +
            "Uber Driver: ${if (uberOk) "installed" else "not found"}\n" +
            "Bridge package: $packageName"

        // Deterministic: straight at our own bridge, exactly like an app
        // firing setPackage(...) would.
        findViewById<Button>(R.id.btn_test_direct).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=37.4219983,-122.084&mode=d")
                ).setPackage(packageName)
            )
        }
        findViewById<Button>(R.id.btn_test_address).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=1600+Amphitheatre+Parkway,+Mountain+View,+CA")
                ).setPackage(packageName)
            )
        }
        // As the system sees it: fully implicit. Expect a chooser
        // (Maps vs Vela Bridge) if real Maps is installed.
        findViewById<Button>(R.id.btn_test_implicit).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=37.4219983,-122.084")
                )
            )
        }
        findViewById<Button>(R.id.btn_open_uber).setOnClickListener {
            packageManager.getLaunchIntentForPackage(UBER_DRIVER)?.let { startActivity(it) }
        }
    }

    private fun isInstalled(pkg: String): Boolean =
        try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    companion object {
        const val UBER_DRIVER = "com.ubercab.driver"
    }
}
