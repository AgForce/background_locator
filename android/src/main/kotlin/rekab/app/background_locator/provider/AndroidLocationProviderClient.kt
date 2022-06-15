package rekab.app.background_locator.provider

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.os.Bundle

class AndroidLocationProviderClient(context: Context, override var listener: LocationUpdateListener?) : BLLocationProvider, LocationListener {
    private val client: LocationManager? =
            ContextCompat.getSystemService(context, LocationManager::class.java)

    private var overrideLocation: Boolean = false
    private var timeOfLastLocation: Long = 0L
    private var timeBetweenLocation: Long = 0L

    @SuppressLint("MissingPermission")
    override fun removeLocationUpdates() {
        client?.removeUpdates(this)
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(request: LocationRequestOptions) {
        timeBetweenLocation = request.interval
        if (client?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            client.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    request.interval,
                    request.distanceFilter,
                    this)
        }
        client?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
            onLocationChanged(it);
        }
    }

    override fun onLocationChanged(location: Location) {
        overrideLocation = false
        //whenever the expected time period is reached invalidate the last known accuracy
        // so that we don't just receive better and better accuracy and eventually risk receiving
        // only minimal locations
        if (location.hasAccuracy()) {
            if (!location.accuracy.isNaN() &&
                    location.accuracy != 0.0f &&
                    !location.accuracy.isFinite() &&
                    !location.accuracy.isInfinite()) {
                overrideLocation = true
            }
        }
        //ensure that we don't get a lot of events
        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
        if (location.time - timeOfLastLocation >= timeBetweenLocation || overrideLocation) {
            //be sure to store the time of receiving this event !
            timeOfLastLocation = location.time
            //send message to parent containing the location object
            listener?.onLocationUpdated(LocationParserUtil.getLocationMapFromLocation(location))
        }
    }

    override fun onProviderDisabled(provider: String) {
        // nop
    }

    override fun onProviderEnabled(provider: String) {
        // nop
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

}