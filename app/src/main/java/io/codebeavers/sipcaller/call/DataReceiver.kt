package io.codebeavers.sipcaller.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import io.codebeavers.sipcaller.ui.MainActivity
import io.codebeavers.sipcaller.util.Const

/**
 * Created by Evgeny Eliseyev on 21/02/2018.
 */

class DataReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        val hasData = intent.extras?.containsKey(Const.KEY_STATUS) ?: false
        val hasErrorCode = intent.extras?.containsKey(Const.KEY_ERROR_CODE) ?: false

        if (context is MainActivity && hasData && hasErrorCode) {
            val status = intent.getSerializableExtra(Const.KEY_STATUS) as Const.SipRegistration
            val errorCode = intent.getIntExtra(Const.KEY_ERROR_CODE, 0)
            context.receiveRegisterState(status, errorCode)
        }
    }
}