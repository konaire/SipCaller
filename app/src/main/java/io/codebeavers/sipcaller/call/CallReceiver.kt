package io.codebeavers.sipcaller.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import io.codebeavers.sipcaller.events.CallEvent

import org.greenrobot.eventbus.EventBus

/**
 * Created by Evgeny Eliseyev on 23/02/2018.
 * Sends event about an incoming call to the running activity.
 */

class CallReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        EventBus.getDefault().post(CallEvent(intent))
    }
}