package com.example.juanjojg.rightnowweather

import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.example.juanjojg.rightnowweather.common.Common
import com.example.juanjojg.rightnowweather.common.Helper
import com.example.juanjojg.rightnowweather.model.OpenWeatherMap
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    // Constants
    private val PERMISSION_REQUEST_CODE: Int = 101
    private val UPDATE_INTERVAL: Long = 60000 // 60 seconds
    private val FASTEST_INTERVAL: Long = 15000 // 15 seconds

    // Variables
    internal var openWeatherMap = OpenWeatherMap()
    var mGoogleApiClient: GoogleApiClient? = null
    var mLocationRequest: LocationRequest? = null
    var permissionGranted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create a Google API Client object
        buildGoogleApiClient()

        // Configure the LocationRequest object
        mLocationRequest = LocationRequest()
        mLocationRequest!!.setInterval(UPDATE_INTERVAL)
        mLocationRequest!!.setFastestInterval(FASTEST_INTERVAL)
        mLocationRequest!!.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
    }

    /**
     * Method to get the Google API Client in order to use the Location Services
     */
    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    /**
     * Method called after calling connect() in order to make requests
     */
    override fun onConnected(p0: Bundle?) {
        requestLocationUpdates()
    }

    /**
     * Method called when the API client is in a disconnected state and needs to restart
     */
    override fun onConnectionSuspended(p0: Int) {
        mGoogleApiClient!!.connect()
    }

    /**
     * Method called when there's been a connection error
     */
    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i("ERROR", "Connection failed: " + p0.errorCode)
    }

    /**
     * Method to change the app content when location has changed
     */
    override fun onLocationChanged(p0: Location) {
        GetWeather().execute(Common.apiRequest(p0!!.latitude.toString(), p0!!.longitude.toString()))
    }

    /**
     * Method called when the app has been started
     */
    override fun onStart() {
        super.onStart()
        mGoogleApiClient!!.connect()
    }

    /**
     * Method called when the app has been resumed
     */
    override fun onResume() {
        super.onResume()
        if (permissionGranted) {
            if (mGoogleApiClient!!.isConnected) {
                requestLocationUpdates()
            }
        }
    }

    /**
     * Method called when the app has been paused
     */
    override fun onPause() {
        super.onPause()
        if (permissionGranted) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
        }
    }

    /**
     * Method called when the app has been stopped
     */
    override fun onStop() {
        super.onStop()
        if (permissionGranted) {
            mGoogleApiClient!!.disconnect()
        }
    }

    /**
     * Method to get a location update
     */
    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
            }
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
    }

    /**
     * Method called when the user has given a result for the permission request
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    permissionGranted = true
                    Toast.makeText(this, "Permissions have been granted. Thank you!", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied
                    permissionGranted = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
                    }
                    Toast.makeText(this, "This app requires location permissions to be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Inner class for handling the API request and update of the app content
     */
    private inner class GetWeather : AsyncTask<String, Void, String>() {
        internal var pd = ProgressDialog(this@MainActivity)

        override fun onPreExecute() {
            super.onPreExecute()
            pd.setTitle("Please wait...")
            pd.show()
        }

        override fun doInBackground(vararg p0: String?): String {
            var stream: String? = null
            var urlString = p0[0]

            val http = Helper()
            stream = http.getHTTPData(urlString)
            return stream
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result!!.contains("Error: Not found city")) {
                pd.dismiss()
                return
            }
            val gson = Gson()
            val mType = object : TypeToken<OpenWeatherMap>() {}.type

            openWeatherMap = gson.fromJson<OpenWeatherMap>(result, mType)
            pd.dismiss()

            // Set information in the UI
            txtCity.text = "${openWeatherMap.name}, ${openWeatherMap.sys!!.country}"
            txtLastUpdate.text = "Last updated:\n${Common.dateNow}"
            var descriptionAdjusted = "${openWeatherMap.weather!![0].description}"
            descriptionAdjusted = descriptionAdjusted.substring(0, 1).toUpperCase() + descriptionAdjusted.substring(1).toLowerCase()
            txtDescription.text = "Current weather:\n${descriptionAdjusted}"
            txtTime.text = "Sunrise at ${Common.unixTimeStampToDateTime(openWeatherMap.sys!!.sunrise)}\nSunset at ${Common.unixTimeStampToDateTime(openWeatherMap.sys!!.sunset)}"
            txtHumidity.text = "Humidity: ${openWeatherMap.main!!.humidity}%"
            txtCelsius.text = "Temperature:\n${openWeatherMap.main!!.temp} ºC"
            Picasso.with(this@MainActivity)
                    .load(Common.getImage((openWeatherMap.weather!![0].icon!!)))
                    .into(imageView)
        }
    }
}
