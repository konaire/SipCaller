# Звонки внутри приложения с помощью SIP
Добрый день, в этой статье я расскажу, как сделать звонки внутри андроид приложения. Эта фича будет полезна, если в вашем приложении уже есть обычный чат, но вы хотите добавить еще и голосовой.

Android предоставляет стандартные инструменты для звонков по протоколу SIP, начиная с Android 2.3. Поэтому практически все устройства (~99.5%) на данный момент поддерживают этот функционал, и вам не нужно использовать низкоуровневые библиотеки для реализации звонков.

В данной статье я буду использовать стандартное апи, но прежде чем мы приступим, я хотел бы сразу прояснить пару острых моментов. Во-первых, стандартное апи не поддерживает SRTP протокол (аналог https для sip), т. е. не обеспечивает достаточной безопасности соединения, и абоненты будут уязвимы для [MITM атак](https://ru.wikipedia.org/wiki/%D0%90%D1%82%D0%B0%D0%BA%D0%B0_%D0%BF%D0%BE%D1%81%D1%80%D0%B5%D0%B4%D0%BD%D0%B8%D0%BA%D0%B0). Во-вторых, в данном api присутствует [ошибка](https://stackoverflow.com/questions/20594095/android-sip-registration-failed-9-in-progress), которую к сожалению никак не обойти. Проявляется она при переустановке/обновлении приложения, хотя и очень редко, и связана с тем, что сип профиль, который регистрируется в телефоне "подвисает", не выполняет корректный логаут. Лечится только перезагрузкой устройства или в некоторых случаях сменой сети (wi-fi/мобильный интернет и наоборот). Поэтому, если вы не собираетесь организовывать секретные переговоры пользователей и часто выкладывать обновления приложения, то этот способ звонков для вас.

Весь код я выложил на [гитхаб](https://github.com/konaire/SipCaller). Для вашего удобства, код также был закоммичен частями, в удобном для его разбора порядке. Вот [здесь](https://github.com/konaire/SipCaller/commit/f913700391f547f2a59b97ed32a277b474e88525) можно посмотреть инициализацию проекта. Я буду писать это приложение на котлин, если вы еще не знакомы с этим языком, предлагаю ознакомиться с ним самостоятельно, [вот](https://android.jlelse.eu/learn-kotlin-while-developing-an-android-app-introduction-567e21ff9664) например хорошая серия статей. Изучение этого языка довольно полезно не столько потому, что он модный, сколько потому, что он позволяет избавиться от NullPointerException еще во время компиляции, плюс много синтаксического сахара, который сильно упрощает и ускоряет разработку, позволяя писать меньше кода. Не будьте так уверены, что jav'ы вам достаточно, не позволяйте "[золотому молотку](https://ru.wikipedia.org/wiki/%D0%97%D0%BE%D0%BB%D0%BE%D1%82%D0%BE%D0%B9_%D0%BC%D0%BE%D0%BB%D0%BE%D1%82%D0%BE%D0%BA)" прижать вас.

## UI для звонков 
Начнем с пользовательского интерфейса. Для звонков нам понадобится всего 2 экрана: экран ввода логина для звонка и сам экран вызова. На экране ввода логина нужно разместить поле для ввода sip логина и кнопку позвонить, по нажатию на которую будет начинаться звонок и открываться экран вызова. На экране вызова будет виден sip логин вашего собеседника, а также 2 или 1 кнопки: "Ответить" и "Сбросить" или "Закончить". Да, этот экран будет использоваться как для исходящего, так и для входящего вызова. В данном приложении можно отказаться от фрагментов и оба экрана сделать как активити, но в более сложном случае отдельным активити стоило бы сделать только экран вызова.

**activity_main.xml - xml c версткой для экрана ввода логина**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="@dimen/default_gap"
    android:orientation="horizontal">

    <EditText
        android:id="@+id/name"
        android:layout_weight="1"
        android:layout_width="0px"
        android:layout_height="wrap_content"
        android:maxLength="50"
        android:singleLine="true"
        android:imeOptions="actionDone"
        android:hint="@string/name_hint"
        android:textColor="@android:color/black"
        android:textSize="@dimen/default_text_size" />

    <Button
        android:id="@+id/callButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/call_button"
        android:textColor="@android:color/black"
        android:textSize="@dimen/default_text_size" />
</LinearLayout>
```

**activity_call.xml - xml c версткой для экрана вызова**
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:ignore="MergeRootFrame">

    <TextView
        android:id="@+id/name"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:layout_marginLeft="@dimen/default_gap"
        android:layout_marginRight="@dimen/default_gap"
        android:layout_marginTop="@dimen/call_name_marginTop"
        android:gravity="center_horizontal"
        android:textColor="@android:color/black"
        android:textSize="@dimen/default_text_size" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/call_buttons_height"
        android:layout_gravity="bottom"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/greenButton"
            android:layout_weight="1"
            android:layout_width="0px"
            android:layout_height="match_parent"
            android:background="@android:color/holo_red_dark"
            android:gravity="center"
            android:textColor="@android:color/white"
            android:textSize="@dimen/default_text_size" />

        <TextView
            android:id="@+id/redButton"
            android:layout_weight="1"
            android:layout_width="0px"
            android:layout_height="match_parent"
            android:background="@android:color/holo_green_dark"
            android:gravity="center"
            android:textColor="@android:color/white"
            android:textSize="@dimen/default_text_size" />
    </LinearLayout>
</FrameLayout>
```

Как видите оба экрана довольно просты. Здесь же давайте добавим все права для того, чтобы звонок сработал, а пользователи друг друга слышали. Следующие права обязательно должны быть в вашем файле манифеста.

```xml
<!-- Права для сети -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Права для sip -->
<uses-permission android:name="android.permission.USE_SIP" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CONFIGURE_SIP" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- Убедимся, что у устройства есть микрофон и оно может звонить по sip. -->
<uses-feature android:name="android.hardware.sip.voip" android:required="true" />
<uses-feature android:name="android.hardware.microphone" android:required="true" />
```

В активити для ввода логина я присваиваю действие открытия активити вызова на нажатие кнопки, ставлю фокус на поле ввода, а также явно проверяю дал ли пользователь права для `USE_SIP` и `RECORD_AUDIO`, и если хотя бы одного из этих прав нет, закрываю приложение.

**MainActivity.kt - активити, представляющее экран ввода логина**
```kotlin
// Пропустим import'ы, чтобы уменьшить объем кода и облегчить его читаемость.
class MainActivity: AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_BEFORE_LOGIN: Int = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Открыть активити для вызова, если введено имя.
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

    // Зарегистрировать пользователя, если все права были предоставлены.
    private fun tryToLoginUser(isPermissionGranted: Boolean) {
        if (isPermissionGranted) {
            // TODO: Зарегистрировать пользователя.
        } else {
            finish()
        }
    }
}
```

В активити для вызова я всего лишь отображаю переданное ему имя вызываемого абонента. Для этого класса код я приводить не буду, полностью весь данный этап представлен в [этом коммите](https://github.com/konaire/SipCaller/commit/febcc1352753d9c0316323103f4632b6796931a1). Давайте посмотрим, что у нас получилось?

<img alt="Экран ввода логина" src="https://raw.githubusercontent.com/konaire/SipCaller/master/img/first.png" height="480"/> <img alt="Экран вызова" src="https://raw.githubusercontent.com/konaire/SipCaller/master/img/second.png" height="480"/>

_Дизайн приложения_

## Регистрация клиента на SIP сервере
Для того чтобы звонить, вы должны завести аккаунт на каком-нибудь публичном сервере. Вы можете поднять свой сервер на [Asterisk](https://www.asterisk.org/), но это уже выходит за рамки этой статьи, поэтому для простоты давайте зарегистрируемся на сервере [Ekiga](https://ekiga.im/index.php?page=register). Ekiga больше известна как программа для sip телефонии в Linux, но они также предоставляют бесплатные sip адресы, которые мы и будем использовать. Регистрация простейшая, как на обычном сайте, после нее ваш адрес будет выглядеть как: `username@ekiga.net`.

Давайте вынесем из активити всю работу с регистрацией, так же в дальнейшем будет вынесен и функционал звонков. Для этого я предлагаю создать отдельный сервис, который будет работать с SIP, а для передачи данных обратно в активити будем использовать `BroadcastReceiver`. Для этого всего лишь нужно послать специальный `Intent` через `sendBroadcast(Intent intent)`, но прежде всего не забудьте объявить сервис в манифесте.

```xml
<service android:exported="false" android:name=".call.CallService" />
```

Для создания интента должен использоваться уникальный `action`.

**CallService.getIntentForStatus() - метод для передачи данных в активити через ресивер**
```kotlin
private fun getIntentForStatus(status: Const.SipRegistration, errorCode: Int = 0): Intent {
    val intent = Intent()

    intent.action = Const.ACTION_DATA_EXCHANGE
    intent.putExtra(Const.KEY_ERROR_CODE, errorCode)
    intent.putExtra(Const.KEY_STATUS, status)
    return intent
}
```

Для того, чтобы зарегистрироваться в SIP на андроид, нужно сначала взять инстанс от `SipManager`, здесь же следует сразу проверить поддерживает ли устройство апи для sip, а затем уже надо инициализировать sip профиль, но там тоже есть пара острых моментов.

Во-первых, нужно передать `PendingIntent` с уникальным `action` для будущего `BroadcastReceiver`, который будет обрабатывать входящие звонки, подробнее об этом в следующем параграфе. Во-вторых, регистрировать listener нужно не во время открытия профиля, а после этого, т. к. иначе он не будет вызываться из-за какого бага внутри sip апи андроида. В-третьих, регистрация иногда зависает или кидает ошибку, хотя профиль успешно зарегистрирован, поэтому мы будем слать данные в активити с небольшой задержкой.

**CallService.kt - основные методы для регистрации на SIP сервере**
```kotlin
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

    mManager?.open(mProfile, PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA), null)
    mManager?.setRegistrationListener(mProfile!!.uriString, this)
    Handler(Looper.getMainLooper()).postDelayed({
        if (mDataIntent == null) {
            mDataIntent = getIntentForStatus(Const.SipRegistration.ERROR, SipErrorCode.TIME_OUT)
        }

        sendBroadcast(mDataIntent)
    }, 5000)
}
```

После того как сервис завершит свою работу, он должен закрыть sip профиль.

**CallService.closeLocalProfile() - метод для выхода из sip профиля**
```kotlin
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
```

Это были основные методы для регистрации на SIP сервере, полный код сервиса можно посмотреть в [этом коммите](https://github.com/konaire/SipCaller/commit/9c236826b4c19d0d8add74d3d19ca41b44f549b6). Теперь давайте посмотрим на ресивер для передачи данных от сервиса к активити. Здесь должно быть все понятно.

**DataReceiver.kt - ресивер-связка между нашим sip сервисом и активити**
```kotlin
class DataReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        val hasStatus = intent.extras?.containsKey(Const.KEY_STATUS) ?: false
        val hasErrorCode = intent.extras?.containsKey(Const.KEY_ERROR_CODE) ?: false

        if (context is MainActivity && hasStatus && hasErrorCode) {
            val status = intent.getSerializableExtra(Const.KEY_STATUS) as Const.SipRegistration
            val errorCode = intent.getIntExtra(Const.KEY_ERROR_CODE, 0)
            context.receiveRegisterState(status, errorCode)
        }
    }
}
```

Запускать сервис будем из `MainActivity`, а точнее биндить его к этой активити, т. к. на сегодня андроид, начиная с Android O, больше не поддерживает сервисы в бекграунде. Для этого нужно создать объект, имплементирующий `ServiceConnection`. Он, например, позволяет сохранить инстанс сервиса в момент, когда он присоединяется к активити, а также обеспечивает возможность завершить работу сервиса вместе с закрытием активити. Вместе с присоединением сервиса будем также регистрировать ресивер, а после того как он передаст статус регистрации в новый метод `receiveRegisterState`, можно "разрегистрировать" его.

**MainActivity.kt - методы для подключения к sip сервису и отображения статуса регистрации**
```kotlin
class MainActivity: AppCompatActivity() {
    private var mService: CallService? = null
    private var mReceiver: DataReceiver? = null
    private val mConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = (service as CallService.CallBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mService != null) {
            unbindService(mConnection)
        }
    }

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
```

## Входящие/исходящие вызовы
Для приема входящих звонков нам понадобится подключить библиотеку [EventBus](https://github.com/greenrobot/EventBus), строго говоря лучше написать свой собственный аналог на [RxJava](https://habrahabr.ru/post/269417/), т. к. `RxJava` может вам понадобиться для работы с сетью, базой данных и много чем еще, пример как сделать это, можно посмотреть [здесь](https://lorentzos.com/rxjava-as-event-bus-the-right-way-10a36bdd49ba). Но в данном приложении тянуть `RxJava` для того, чтобы кинуть одно событие, нет никакой необходимости и вполне достаточно `EventBus`. Начнем с регистрации в манифесте `BroadcastReceiver` для входящих звонков, этот ресивер должен иметь уникальный `action`.

```xml
<receiver android:exported="false" android:name=".call.CallReceiver">
    <intent-filter>
        <action android:name="io.codebeavers.sipcaller.INCOMING_CALL" />
    </intent-filter>
</receiver>
```

Сам ресивер будет посылать событие через `EventBus`, подписаться на которое нужно в `MainActivity`. Я делаю именно так, потому что при входящем звонке надо открыть экран вызова и позвать метод `takeAudioCall()` в уже запущенном `CallService`, удобнее всего это сделать в `MainActivity`, хотя из ресивера и нельзя передать данные в запущенную активити напрямую, можно использовать [паттерн наблюдателя](https://ru.wikipedia.org/wiki/%D0%9D%D0%B0%D0%B1%D0%BB%D1%8E%D0%B4%D0%B0%D1%82%D0%B5%D0%BB%D1%8C_(%D1%88%D0%B0%D0%B1%D0%BB%D0%BE%D0%BD_%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F)), который как раз и реализует `EventBus`.

**CallReceiver.kt - ресивер, передающий событие входящего звонка в активити**
```kotlin
class CallReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        EventBus.getDefault().post(CallEvent(intent))
    }
}
```

`CallEvent` - это простейший класс данных, который содержит одно поле для `Intent`. Кроме этого в `DataReceiver` была добавлена передача статусов о звонке между сервисом к активити и наоборот.

**DataReceiver.onReceive() - добавлен код для передачи статуса звонка между сервисом и активити**
```kotlin
// Пропустим код, который был описан ранее

val isRegistration = hasStatus && hasErrorCode
val isCall = hasStatus && !hasErrorCode

if (context is MainActivity && isRegistration) {
    val status = intent.getSerializableExtra(Const.KEY_STATUS) as Const.SipRegistration
    val errorCode = intent.getIntExtra(Const.KEY_ERROR_CODE, 0)
    context.receiveRegisterState(status, errorCode)
} else if (isCall) {
    val status = intent.getSerializableExtra(Const.KEY_STATUS) as Const.SipCall

    if (context is CallActivity) {
        context.updateCallStatus(status)
    } else if (context is CallService) {
        context.doAction(status)
    }
}
```

Помимо этого был создан еще один класс для работы со звуками. Зачем? Чтобы обеспечить дополнительную обратную связь между пользователем и приложением. Например, при входящем звонке обычно нужно не только показывать экран вызова, но и воспроизводить мелодию звонка с вибрацией, как это делает обычная звонилка. Если пользователь берет трубку или сбрасывает вызов, нужно все это дело прекратить. При исходящем звонке хорошо бы воспроизводить гудки: длинные, если собеседник не берет трубку, короткие, если он сбросил вызов и т. д. Для всего этого теперь есть класс `SoundManager`. Его код я приводить не буду, т. к. он не сильно относится к теме статьи. Полностью все изменения для данного этапа смотрите в [этом коммите](https://github.com/konaire/SipCaller/commit/3392e93ce5c35ac357270889653c2bc5473a0b5f).

Вначале давайте добавим в sip сервис методы для осуществления исходящего и приема входящего звонка. Для обоих методов нужно передать свой listener, который будет правильно обрабатывать состояния звонка.

**CallService.kt - методы для осуществления исходящего и приема входящего звонка**
```kotlin
fun makeAudioCall(sipAddress: String): Boolean {
    val listener = object : SipAudioCall.Listener() {
        private val handler = Handler(Looper.getMainLooper())

        // Останавливаем гудки и запускаем аудио поток для звонка.
        override fun onCallEstablished(call: SipAudioCall) {
            call.startAudio()
            call.setSpeakerMode(false)

            if (call.isMuted) {
                call.toggleMute()
            }

            mSound?.stopTone()
        }

        // Генерируем "занято" и шлем статус об окончании звонка в активити.
        override fun onCallBusy(call: SipAudioCall) {
            mSound?.startTone(ToneGenerator.TONE_SUP_BUSY)
            handler.postDelayed({ mSound?.stopTone() }, 3000)

            sendBroadcast(getIntentForStatusOfCall(Const.SipCall.ENDED))
        }

        // Генерируем одиночный гудок и шлем статус об окончании звонка в активити.
        override fun onCallEnded(call: SipAudioCall) {
            mSound?.startTone(ToneGenerator.TONE_PROP_PROMPT)
            handler.postDelayed({ mSound?.stopTone() }, 100)

            sendBroadcast(getIntentForStatusOfCall(Const.SipCall.ENDED))
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
        private val handler = Handler(Looper.getMainLooper())

        // Заканчиваем проигрыш мелодии звонка.
        override fun onCallEstablished(call: SipAudioCall) {
            mSound?.stopRinging()
        }

        // Генерируем одиночный гудок или заканчиваем проигрыш мелодии звонка.
        // В зависимости от того, ответили на звонок или нет.
        // Также шлем статус об окончании звонка в активити.
        override fun onCallEnded(call: SipAudioCall) {
            if (call.isInCall) {
                mSound?.startTone(ToneGenerator.TONE_PROP_PROMPT)
                handler.postDelayed({ mSound?.stopTone() }, 100)
            } else {
                mSound?.stopRinging()
            }

            sendBroadcast(getIntentForStatusOfCall(Const.SipCall.ENDED))
        }
    }

    mCall = try {
        mManager?.takeAudioCall(intent, listener)
    } catch (e: Exception) {
        null
    }

    return mCall?.peerProfile?.userName
}
```

Описанные выше методы необходимо дополнить методом сброса звонка, мы же не хотим, чтобы пользователь застрял в звонке? :smiley: И методом для ответа на входящий звонок, иначе придется сразу при поступлении звонка запускать аудио поток, что как-то глупо выглядит :grin: Также надо добавить публичный метод, который будет зваться из ресивера, чтобы запустить один из описанных выше методов, которые должны быть приватными.

**CallService.kt - методы для завершения звонка и ответа на звонок**
```kotlin
fun doAction(status: Const.SipCall) {
    if (status == Const.SipCall.ANSWERED) {
        answerCall()
    } else if (status == Const.SipCall.ENDED) {
        endCall()
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
        // Останавливаем гудки набора номера для исходящего звонка.
        // Заканчиваем проигрыш мелодии для входящего звонка.
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
```

Также необходимо зарегистрировать ресивер и инициализировать `SoundManager` при запуске сервиса, а при остановке: "разрегистрировать" ресивер и останавливать звонок. Вот так теперь это выглядит:

```kotlin
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
```

`MainActivity` теперь должно начинать как исходящий, так и входящий вызов, для этого были добавлены специальные методы. Здесь же не забудьте подписаться на событие входящего звонка, этот код смотрите на [гитхабе](https://github.com/konaire/SipCaller/commit/3392e93ce5c35ac357270889653c2bc5473a0b5f), приводить его я не буду.

**MainActivity.kt - методы для осуществления исходящего и приема входящего звонка**
```kotlin
@Subscribe // перехватываем событие входящего звонка.
// Пытаемся принять вызов, а в случае успеха показываем экран звонка.
fun onIncomingCallEvent(event: CallEvent) {
    val nameText = mService?.takeAudioCall(event.intent)
    val isStarted = nameText?.isNotEmpty() ?: false

    if (isStarted) {
        CallActivity.create(this, nameText!!, true)
    } else {
        toast(getString(R.string.sip_call_error))
    }
}

// Пытаемся совершить звонок, а в случае успеха показываем экран вызова.
private fun makeCall() {
    val nameText = name.text.toString()
    val sipAddress = "sip:$nameText@${Const.SIP_URL}"
    val isStarted = mService?.makeAudioCall(sipAddress) ?: false

    if (isStarted) {
        CallActivity.create(this, nameText, false)
    } else {
        toast(getString(R.string.sip_call_error))
    }
}
```

Ну и, наконец, экран вызова. Для него я добавил возможность блокировать затухание экрана и переход в режим блокировки, делается это очень просто и не требует специальных прав в манифесте. Нужно добавить в разметку активити, в корневое View, следующий атрибут: `android:keepScreenOn="true"`.

Помимо этого при инициализации добавлен параметр `isIncoming`, который, как понятно из имени, отвечает за то, является ли данный звонок входящим или нет. В зависимости от этого инициализируется UI и либо запускается проигрыш мелодии звонка, если звонок входящий, либо гудки набора номера, если звонок исходящий. Также блокируется закрытие активити по нажатию на аппаратную кнопку назад.

**CallActivity.kt - активити, представляющее экран вызова**
```kotlin
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
```

На этом все. Для тестирования приложения, соберите его из исходников на [гитхабе](https://github.com/konaire/SipCaller), если вы зарегистрировали аккаунт на [Ekiga](https://ekiga.im/), то можете проверить его работоспособность, используя следующие номера: **500** - эхо, будет повторять все, что слышит от вас, **520** - тест входящих звонков, позвоните туда, дождитесь сброса, и вам перезвонит эхо (500).

Спасибо за внимание.