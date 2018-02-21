package io.codebeavers.sipcaller.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import io.codebeavers.sipcaller.R
import io.codebeavers.sipcaller.util.Const

import kotlinx.android.synthetic.main.activity_call.*

/**
 * Created by Evgeny Eliseyev on 04/02/2018.
 */

class CallActivity: AppCompatActivity() {
    companion object {
        fun create(context: Context, name: String) {
            val intent = Intent(context, CallActivity::class.java)
            intent.putExtra(Const.KEY_NAME, name)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        name.text = intent.getStringExtra(Const.KEY_NAME)
    }
}