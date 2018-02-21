package io.codebeavers.sipcaller.call

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.sip.*
import android.os.*

import io.codebeavers.sipcaller.util.Const

/**
 * Created by Evgeny Eliseyev on 21/02/2018.
 */

class CallService: Service(), SipRegistrationListener {
    inner class CallBinder: Binder() {
        fun getService(): CallService = this@CallService
    }

    private var mManager: SipManager? = null
    private var mProfile: SipProfile? = null
    private var dataIntent: Intent? = null

    override fun onBind(intent: Intent?): IBinder {
        initializeManager()
        return CallBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        closeLocalProfile()
        return super.onUnbind(intent)
    }

    override fun onRegistering(localProfileUri: String?) {
        sendBroadcast(getIntentForStatus(Const.SipRegistration.STARTED))
    }

    override fun onRegistrationDone(localProfileUri: String?, expiryTime: Long) {
        dataIntent = getIntentForStatus(Const.SipRegistration.FINISHED)
    }

    override fun onRegistrationFailed(localProfileUri: String?, errorCode: Int, errorMessage: String?) {
        dataIntent = getIntentForStatus(Const.SipRegistration.ERROR, errorCode)
    }

    private fun initializeManager() {
        if (!SipManager.isApiSupported(this)) {
            sendBroadcast(getIntentForStatus(Const.SipRegistration.ERROR, SipErrorCode.CLIENT_ERROR))
        } else if (mManager == null) {
            mManager = SipManager.newInstance(this)
        }

        initializeLocalProfile()
    }

    private fun initializeLocalProfile() {
        val intent = Intent()
        val domain = Const.SIP_URL
        val login = Const.SIP_LOGIN
        val password = Const.SIP_PASSWORD
        val builder = SipProfile.Builder(login, domain)
        intent.action = Const.ACTION_INCOMING_CALL
        builder.setPassword(password)
        mProfile = builder.build()

        // Android will call special BroadcastReceiver when an incoming call will be arrived.
        // We should set SipRegistrationListener after opening profile, otherwise it willn't be called because of bug.
        mManager?.open(mProfile, PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA), null)
        mManager?.setRegistrationListener(mProfile!!.uriString, this)

        // Sometimes registration hangs so we need to send data after a small timeout.
        Handler(Looper.getMainLooper()).postDelayed({
            if (dataIntent == null) {
                dataIntent = getIntentForStatus(Const.SipRegistration.ERROR, SipErrorCode.TIME_OUT)
            }

            sendBroadcast(dataIntent)
        }, 2000)
    }

    private fun closeLocalProfile() {
        try {
            val manager = mManager
            val profile = mProfile

            if (manager != null && profile != null) {
                manager.unregister(profile, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Generates intent to send registration status to activity via receiver.
    private fun getIntentForStatus(status: Const.SipRegistration, errorCode: Int = 0): Intent {
        val intent = Intent()

        intent.action = Const.ACTION_DATA_EXCHANGE
        intent.putExtra(Const.KEY_ERROR_CODE, errorCode)
        intent.putExtra(Const.KEY_STATUS, status)
        return intent
    }
}