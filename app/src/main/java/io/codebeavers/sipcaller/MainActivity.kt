package io.codebeavers.sipcaller

import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

import io.codebeavers.sipcaller.util.showKeyboard
import io.codebeavers.sipcaller.util.toast

import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by Evgeny Eliseyev on 31/01/2018.
 */

class MainActivity: AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_BEFORE_LOGIN: Int = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start call if name of companion was inputted.
        callButton.setOnClickListener {
            if (name.text.isEmpty()) {
                toast(getString(R.string.no_name_error))
            } else {
                CallActivity.create(this, name.text.toString())
            }
        }

        checkCallPermissions()
    }

    override fun onResume() {
        super.onResume()
        name.showKeyboard()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_BEFORE_LOGIN) {
            var isPermissionGranted = true
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    isPermissionGranted = isPermissionGranted && (result == PackageManager.PERMISSION_GRANTED)
                }
            } else {
                isPermissionGranted = false
            }

            tryToLoginUser(isPermissionGranted)
        }
    }

    private fun checkCallPermissions() {
        val permissions = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_SIP) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.USE_SIP)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.size > 0) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), PERMISSIONS_BEFORE_LOGIN
            )
        } else {
            tryToLoginUser(true)
        }
    }

    // Register user if all permissions was granted.
    private fun tryToLoginUser(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            // TODO: Initialize sip login.
        } else {
            finish()
        }
    }
}