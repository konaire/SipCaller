package io.codebeavers.sipcaller.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.ToneGenerator
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import io.codebeavers.sipcaller.R
import io.codebeavers.sipcaller.call.DataReceiver
import io.codebeavers.sipcaller.util.SoundManager
import io.codebeavers.sipcaller.util.Const

import kotlinx.android.synthetic.main.activity_call.*

/**
 * Created by Evgeny Eliseyev on 04/02/2018.
 */

class CallActivity: AppCompatActivity() {
    private var mReceiver: DataReceiver? = null

    companion object {
        fun create(context: Context, name: String, isIncoming: Boolean) {
            val intent = Intent(context, CallActivity::class.java)
            intent.putExtra(Const.KEY_IS_INCOMING, isIncoming)
            intent.putExtra(Const.KEY_NAME, name)
            context.startActivity(intent)
        }
    }

    private fun isIncoming(): Boolean = intent.getBooleanExtra(Const.KEY_IS_INCOMING, false)

    // Registers receiver.
    // Also starts ringing for incoming call.
    // And starts dial tones for outgoing call.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        val soundManager = SoundManager.getInstance(this)
        val filter = IntentFilter(Const.ACTION_DATA_TO_ACTIVITY_EXCHANGE)

        unregisterReceiver()
        mReceiver = DataReceiver()
        registerReceiver(mReceiver, filter)

        if (isIncoming()) {
            soundManager.startRinging()
        } else {
            soundManager.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        }

        updateCallStatus(Const.SipCall.STARTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    // Prevents going back with hardware button.
    override fun onBackPressed() { }

    fun updateCallStatus(status: Const.SipCall) {
        when (status) {
            Const.SipCall.STARTED -> {
                if (isIncoming()) {
                    greenButton.text = getString(R.string.call_answer)
                    redButton.text = getString(R.string.call_decline)

                    greenButton.setOnClickListener {
                        greenButton.visibility = View.GONE
                        redButton.text = getString(R.string.call_end)
                        sendBroadcast(getIntentForStatusOfCall(Const.SipCall.ANSWERED))
                    }
                } else {
                    greenButton.visibility = View.GONE
                    redButton.text = getString(R.string.call_end)
                }

                name.text = intent.getStringExtra(Const.KEY_NAME)

                redButton.setOnClickListener {
                    sendBroadcast(getIntentForStatusOfCall(Const.SipCall.ENDED))
                    finish()
                }
            }
            Const.SipCall.ENDED -> finish()
        }
    }

    private fun getIntentForStatusOfCall(status: Const.SipCall): Intent {
        val intent = Intent()

        intent.action = Const.ACTION_DATA_TO_SERVICE_EXCHANGE
        intent.putExtra(Const.KEY_STATUS, status)
        return intent
    }

    private fun unregisterReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
            mReceiver = null
        }
    }
}