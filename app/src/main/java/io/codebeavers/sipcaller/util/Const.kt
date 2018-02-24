package io.codebeavers.sipcaller.util

/**
 * Created by Evgeny Eliseyev on 04/02/2018.
 */

class Const {
    enum class SipRegistration {
        STARTED, FINISHED, ERROR
    }

    enum class SipCall {
        STARTED, ANSWERED, ENDED
    }

    companion object {
        const val KEY_NAME = "key_name"
        const val KEY_STATUS = "key_status"
        const val KEY_ERROR_CODE = "key_error_code"
        const val KEY_IS_INCOMING = "key_is_incoming"

        const val ACTION_INCOMING_CALL = "io.codebeavers.sipcaller.INCOMING_CALL"
        const val ACTION_DATA_TO_ACTIVITY_EXCHANGE = "io.codebeavers.sipcaller.DATA_TO_ACTIVITY_EXCHANGE"
        const val ACTION_DATA_TO_SERVICE_EXCHANGE = "io.codebeavers.sipcaller.DATA_TO_SERVICE_EXCHANGE"

        const val SIP_URL = "ekiga.net"
        const val SIP_LOGIN = "konair1"
        const val SIP_PASSWORD = "321qwerty123"

        const val CALL_TIMEOUT = 30
    }
}