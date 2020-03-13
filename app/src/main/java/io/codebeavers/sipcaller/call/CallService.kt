package io.codebeavers.sipcaller.call

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.ToneGenerator
import android.net.sip.*
import android.os.*

import io.codebeavers.sipcaller.util.Const
import io.codebeavers.sipcaller.util.SoundManager

/**
 * Created by Evgeny Eliseyev on 21/02/2018.
 */

class CallService: Service(), SipRegistrationListener {
    inner class CallBinder: Binder() {
        fun getService(): CallService = this@CallService
    }

    private var mManager: SipManager? = null
    private var mProfile: SipProfile? = null
    private var mCall: SipAudioCall? = null
    private var mDataIntent: Intent? = null
    private var mSound: SoundManager? = null
    private var mReceiver: DataReceiver? = null

    override fun onBind(intent: Intent?): IBinder {
        val filter = IntentFilter(Const.ACTION_DATA_TO_SERVICE_EXCHANGE)

        initializeManager()
        unregisterReceiver()
        mReceiver = DataReceiver()
        registerReceiver(mReceiver, filter)
        mSound = SoundManager.getInstance(this)

        return CallBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        endCall()
        closeLocalProfile()
        unregisterReceiver()
        mSound = null

        return super.onUnbind(intent)
    }

    override fun onRegistering(localProfileUri: String?) {
        sendBroadcast(getIntentForStatusOfRegistration(Const.SipRegistration.STARTED))
    }

    override fun onRegistrationDone(localProfileUri: String?, expiryTime: Long) {
        mDataIntent = getIntentForStatusOfRegistration(Const.SipRegistration.FINISHED)
    }

    // Send error only if there was no other error or successful registration.
    override fun onRegistrationFailed(localProfileUri: String?, errorCode: Int, errorMessage: String?) {
        val hasStatus = mDataIntent?.extras?.containsKey(Const.KEY_STATUS) ?: false
        if (hasStatus) {
            return
        }

        mDataIntent = getIntentForStatusOfRegistration(Const.SipRegistration.ERROR, errorCode)
    }

    fun makeAudioCall(sipAddress: String): Boolean {
        val listener = object : SipAudioCall.Listener() {
            private val handler = Handler()

            // Stops dial tones and starts audio stream.
            override fun onCallEstablished(call: SipAudioCall) {
                call.startAudio()
                call.setSpeakerMode(false)

                if (call.isMuted) {
                    call.toggleMute()
                }

                mSound?.stopTone()
            }

            // Generates busy dial tone and sends call end status to activity.
            override fun onCallBusy(call: SipAudioCall) {
                mSound?.startTone(ToneGenerator.TONE_SUP_BUSY)
                handler.postDelayed({ mSound?.stopTone() }, 3000)

                sendBroadcast(getIntentWithEndStatusOfCall())
            }

            // Generates one beep and sends call end status to activity.
            override fun onCallEnded(call: SipAudioCall) {
                mSound?.startTone(ToneGenerator.TONE_PROP_PROMPT)
                handler.postDelayed({ mSound?.stopTone() }, 100)

                sendBroadcast(getIntentWithEndStatusOfCall())
            }
        }

        mCall = try {
            mManager?.makeAudioCall(mProfile?.uriString, sipAddress, listener, Const.CALL_TIMEOUT)
        } catch (e: Exception) {
            if (mProfile != null) {
                closeLocalProfile()
            }

            null
        }

        return mCall != null
    }

    fun takeAudioCall(intent: Intent): String? {
        val listener = object: SipAudioCall.Listener() {
            private val handler = Handler()

            // Stops ringing when call is answered.
            override fun onCallEstablished(call: SipAudioCall) {
                mSound?.stopRinging()
            }

            // Generates one beep or stops ringing.
            // It depends on whether you answered the call or not.
            // Also the method sends call end status to activity.
            override fun onCallEnded(call: SipAudioCall) {
                if (call.isInCall) {
                    mSound?.startTone(ToneGenerator.TONE_PROP_PROMPT)
                    handler.postDelayed({ mSound?.stopTone() }, 100)
                } else {
                    mSound?.stopRinging()
                }

                sendBroadcast(getIntentWithEndStatusOfCall())
            }
        }

        mCall = try {
            mManager?.takeAudioCall(intent, listener)
        } catch (e: Exception) {
            null
        }

        return mCall?.peerProfile?.userName
    }

    fun doAction(status: Const.SipCall) {
        if (status == Const.SipCall.ANSWERED) {
            answerCall()
        } else if (status == Const.SipCall.ENDED) {
            endCall()
        }
    }

    private fun initializeManager() {
        if (!SipManager.isApiSupported(this)) {
            sendBroadcast(getIntentForStatusOfRegistration(Const.SipRegistration.ERROR, SipErrorCode.CLIENT_ERROR))
        } else if (mManager == null) {
            mManager = SipManager.newInstance(this)
        }

        initializeLocalProfile()
    }

    private fun initializeLocalProfile() {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sometimes registration hangs so we need to send data after a small timeout.
        Handler(Looper.getMainLooper()).postDelayed({
            if (mDataIntent == null) {
                mDataIntent = getIntentForStatusOfRegistration(Const.SipRegistration.ERROR, SipErrorCode.TIME_OUT)
            }

            sendBroadcast(mDataIntent)
        }, 5000)
    }

    private fun closeLocalProfile() {
        try {
            val manager = mManager
            val profile = mProfile

            if (manager != null && profile != null) {
                manager.unregister(profile, this)
                manager.close(profile.uriString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun answerCall() {
        val call = mCall ?: return

        try {
            call.answerCall(Const.CALL_TIMEOUT)
            call.setSpeakerMode(false)
            call.startAudio()

            if (call.isMuted) {
                call.toggleMute()
            }
        } catch (e: Exception) {
            call.close()
        }
    }

    private fun endCall() {
        val call = mCall ?: return

        try {
            // Stops dial tones for outgoing call.
            // Stops ringing for incoming call.
            if (!call.isInCall) {
                mSound?.stopTone()
                mSound?.stopRinging()
            }

            call.endCall()
        } catch (e: SipException) {
            e.printStackTrace()
        }

        call.close()
    }

    // Generates intent to send registration status to activity via receiver.
    private fun getIntentForStatusOfRegistration(status: Const.SipRegistration, errorCode: Int = 0): Intent {
        val intent = Intent()

        intent.action = Const.ACTION_DATA_TO_ACTIVITY_EXCHANGE
        intent.putExtra(Const.KEY_ERROR_CODE, errorCode)
        intent.putExtra(Const.KEY_STATUS, status)
        return intent
    }
    
    private fun getIntentWithEndStatusOfCall(): Intent {
        val intent = Intent()

        intent.action = Const.ACTION_DATA_TO_ACTIVITY_EXCHANGE
        intent.putExtra(Const.KEY_STATUS, Const.SipCall.ENDED)
        return intent
    }

    private fun unregisterReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
            mReceiver = null
        }
    }
}