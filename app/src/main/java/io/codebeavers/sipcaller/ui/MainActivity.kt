package io.codebeavers.sipcaller.ui

import android.content.pm.PackageManager
import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

import io.codebeavers.sipcaller.R
import io.codebeavers.sipcaller.call.*
import io.codebeavers.sipcaller.util.*

import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by Evgeny Eliseyev on 31/01/2018.
 */

class MainActivity: AppCompatActivity() {
    private var mService: CallService? = null
    private var mReceiver: DataReceiver? = null
    private val mConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = (service as CallService.CallBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    companion object {
        private const val PERMISSIONS_BEFORE_REGISTER: Int = 1
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

    override fun onDestroy() {
        super.onDestroy()
        if (mService != null) {
            unbindService(mConnection)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_BEFORE_REGISTER) {
            var isPermissionGranted = true
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    isPermissionGranted = isPermissionGranted && (result == PackageManager.PERMISSION_GRANTED)
                }
            } else {
                isPermissionGranted = false
            }

            tryToRegister(isPermissionGranted)
        }
    }

    // This method will get registration status from the service.
    // Also you need to unregister the receiver here because it isn't needed anymore.
    fun receiveRegisterState(status: Const.SipRegistration, errorCode: Int) {
        when (status) {
            Const.SipRegistration.ERROR -> toast(getString(R.string.sip_registration_error, errorCode))
            Const.SipRegistration.STARTED -> toast(getString(R.string.sip_registration_started))
            Const.SipRegistration.FINISHED -> {
                callButton.isEnabled = true
                toast(getString(R.string.sip_registration_finished))
            }
        }

        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
            mReceiver = null
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
                this, permissions.toTypedArray(), PERMISSIONS_BEFORE_REGISTER
            )
        } else {
            tryToRegister(true)
        }
    }

    // Register user if all permissions was granted.
    // We'll do it via service so let's bind it and register the receiver.
    private fun tryToRegister(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            val filter = IntentFilter(Const.ACTION_DATA_EXCHANGE)

            if (mReceiver != null) {
                unregisterReceiver(mReceiver)
            }

            mReceiver = DataReceiver()
            registerReceiver(mReceiver, filter)
            bindService(Intent(this, CallService::class.java), mConnection, Context.BIND_AUTO_CREATE)
        } else {
            finish()
        }
    }
}