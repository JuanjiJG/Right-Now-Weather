package com.example.juanjojg.rightnowweather

import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.example.juanjojg.rightnowweather.common.Common
import com.example.juanjojg.rightnowweather.common.Helper
import com.example.juanjojg.rightnowweather.model.OpenWeatherMap
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
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
    val PERMISSION_REQUEST_CODE = 1001
    val PLAY_SERVICES_RESOLUTION_REQUEST = 1000
    val UPDATE_INTERVAL = 15000
    val FASTEST_INTERVAL = 5000

    // Permissions Arrays
    var permissionsToRequest: MutableList<String>? = null
    var permissionsRejected: MutableList<String>? = null
    var permissions: MutableList<String>? = null

    // Variables
    var mGoogleApiClient: GoogleApiClient? = null
    var mLocationRequest: LocationRequest? = null
    var mLocation: Location? = null
    internal var openWeatherMap = OpenWeatherMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissions!!.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions!!.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)

        requestPermission()

        if (checkPlayService()) {
            buildGoogleApiClient()
        }

        // Show location button click listener
        btnRefresh.setOnClickListener {
            createLocationRequest()
        }
    }

    /**
     * Method to request the location permission
     */
    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    /**
     * Method called when the user has given a result for the permission request
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If the permission has been granted, then we can continue getting the location
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayService()) {
                        buildGoogleApiClient();
                    }
                }
            }
        }
    }

    /**
     * Method to get the Google API Client in order to use the Location Services
     */
    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build()
    }

    /**
     * Method to check if the device has Google Play Services available
     */
    private fun checkPlayService(): Boolean {
        var resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show()
            } else {
                Toast.makeText(applicationContext, "This device is not compatible", Toast.LENGTH_SHORT).show()
                finish()
            }
            return false
        }
        return true
    }

    /**
     * Method called after calling connect() in order to make requests
     */
    override fun onConnected(p0: Bundle?) {
        createLocationRequest()
    }

    /**
     * Method to get a location update
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
        Toast.makeText(applicationContext, "Weather updated!", Toast.LENGTH_SHORT).show()
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
     * Method to change the app text when location has changed
     */
    override fun onLocationChanged(p0: Location) {
        GetWeather().execute(Common.apiRequest(p0!!.latitude.toString(), p0!!.longitude.toString()))
    }

    /**
     * Method called when the app starts
     */
    override fun onStart() {
        super.onStart()
        // If there isn't an API Client connected, make the connection
        if (mGoogleApiClient != null)
            mGoogleApiClient!!.connect()
    }

    /**
     * Method called when the app is finished
     */
    override fun onDestroy() {
        // Terminate the API Client
        mGoogleApiClient!!.disconnect()
        super.onDestroy()
    }

    /**
     * Method called when the app has been resumed
     */
    override fun onResume() {
        super.onResume()
        checkPlayService()
    }

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
            txtLastUpdate.text = "Last updated: ${Common.dateNow}"
            var descriptionAdjusted = "${openWeatherMap.weather!![0].description}"
            descriptionAdjusted = descriptionAdjusted.substring(0, 1).toUpperCase() + descriptionAdjusted.substring(1).toLowerCase()
            txtDescription.text = "Current weather:\n${descriptionAdjusted}"
            txtTime.text = "Sunrise at ${Common.unixTimeStampToDateTime(openWeatherMap.sys!!.sunrise)}\nSunset at ${Common.unixTimeStampToDateTime(openWeatherMap.sys!!.sunset)}"
            txtHumidity.text = "Humidity: ${openWeatherMap.main!!.humidity}%"
            txtCelsius.text = "Temperature: ${openWeatherMap.main!!.temp} ºC"
            Picasso.with(this@MainActivity)
                    .load(Common.getImage((openWeatherMap.weather!![0].icon!!)))
                    .into(imageView)
        }
    }
}
