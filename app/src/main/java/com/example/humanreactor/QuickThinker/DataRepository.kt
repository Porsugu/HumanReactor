package com.example.humanreactor.QuickThinker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.humanreactor.vercel_port.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataRepository (private val context: Context) {

    private val geminiClient by lazy {
        GeminiClient("https://vercel-genimi-api-port.vercel.app//")
    }

    /**
     * check if there has any wifi connection
     */
    private fun isWifiConnected(context: Context): Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)as ConnectivityManager

        return if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.Q){

            // for android 10 and above version
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        } else {
            // android 9 and below
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    /**
     * getting data from api generated words
     */
    private suspend fun fetchDataFromApi(prompt: String): String = withContext(Dispatchers.IO){
        try {
            if (isWifiConnected(context)) {
                // if there are wifi connect gemini client
                geminiClient.generateContent(prompt)
            } else {
                // no internet then go for local data
                readLocalBackupData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // when there are error use local data
            readLocalBackupData()
        }
    }

    private fun readLocalBackupData(): String {
        return try {
            context.assets.open("backup_data.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            "cannot download data, please check the network connectivity"
        }
    }

}