package com.example.blescan.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.blescan.R


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame, ScanFragment.newInstance())
                .commitNow()
        }
    }



    companion object {
        const val TAG = "TAG";
        const val ENVIRONMENTAL_SERVICE_UUID = "50b55126-d4b1-11eb-b8bc-0242ac130003"
        const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
        const val LOCATION_PERMISSION_REQUEST_CODE = 2
    }

}