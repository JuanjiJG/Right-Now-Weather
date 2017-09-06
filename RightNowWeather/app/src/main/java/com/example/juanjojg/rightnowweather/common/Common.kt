package com.example.juanjojg.rightnowweather.common

import java.text.SimpleDateFormat
import java.util.*

object Common {
    val API_KEY = "fd3602f20bbaa233d0e0c31fe39f0c93"
    val API_LINK = "http://api.openweathermap.org/data/2.5/"

    /**
     * Method to return a string of a OpenWeatherMap API call
     */
    fun apiRequest(lat: String, lon: String): String {
        var sb = StringBuilder(API_LINK)
        sb.append("weather?lat=${lat}&lon=${lon}&appid=${API_KEY}&units=metric")
        return sb.toString()
    }

    /**
     * Method to convert from Unix timestamp to Date time
     */
    fun unixTimeStampToDateTime(unixTimeStamp: Double): String {
        val dateFormat = SimpleDateFormat("HH:mm")
        val date = Date()
        date.time = unixTimeStamp.toLong()*1000

        return dateFormat.format(date)
    }

    /**
     * Method to get image of weather from data
     */
    fun getImage (icon: String): String {
        return "http://openweathermap.org/img/w/${icon}.png"
    }

    val dateNow: String
        get() {
            val dateFormat = SimpleDateFormat("dd MM yyyy HH:mm")
            val date = Date()
            return dateFormat.format(date)
        }
}