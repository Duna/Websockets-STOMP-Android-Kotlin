package com.duna.stomp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cujoai.stomp.Stomp
import com.cujoai.stomp.StompClient
import com.cujoai.stomp.dto.LifecycleEvent
import com.cujoai.stomp.dto.StompHeader
import com.cujoai.stomp.dto.StompMessage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stompclientexample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val STOMP_WS_HOST: String = "wss://....fill_me..../notifications"

    private lateinit var mStompClient: StompClient
    private val mRestPingDisposable: Disposable? = null
    private var compositeDisposable: CompositeDisposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        mStompClient = Stomp.over(STOMP_WS_HOST)

        resetSubscriptions()
        registerClicks()
    }

    private fun registerClicks() {
        binding.buttonConnect.setOnClickListener {
            connectStomp()
        }

        binding.buttonDisconnect.setOnClickListener {
            disconnectStomp()
        }
    }

    private fun disconnectStomp() {
        mStompClient.disconnect()
    }

    private fun connectStomp() {
        val headers: MutableList<StompHeader> = ArrayList()
        headers.add(StompHeader("accept-version", "1.2,1.1,1.0"))
        mStompClient.withClientHeartbeat(1000).withServerHeartbeat(1000)

        resetSubscriptions()

        val dispLifecycle = mStompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { lifecycleEvent: LifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        toast("Stomp connection opened")
                        connectTopicConfiguration()
                    }

                    LifecycleEvent.Type.ERROR -> {
                        val error = lifecycleEvent.exception.toString()
                        Log.e(TAG, "Stomp connection error: $error")
                        toast("Error$error")
                    }

                    LifecycleEvent.Type.CLOSED -> {
                        toast("Stomp connection closed")
                        resetSubscriptions()
                    }

                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> toast("Stomp failed server heartbeat")
                    LifecycleEvent.Type.OPENED -> {}//TODO()
                    LifecycleEvent.Type.CLOSED -> {}//TODO()
                    LifecycleEvent.Type.ERROR -> {}//TODO()
                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {}//TODO()
                }
            }

        compositeDisposable!!.add(dispLifecycle)
        mStompClient.connect(headers)
    }

    private fun connectTopicConfiguration() {
        Handler(Looper.getMainLooper()).postDelayed({
            val dispTopic = mStompClient.topic("/configuration")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ topicMessage: StompMessage ->
                    toast("Received " + topicMessage.payload)
                    Log.d(TAG, "Received " + topicMessage.payload) //{"refreshConfig":true}
                },
                    { throwable: Throwable? ->
                        Log.e(TAG, "Error on subscribe topic", throwable)
                    })
            compositeDisposable!!.add(dispTopic)
        }, 3000)
    }

    private fun toast(text: String) {
        Log.i(TAG, text)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable!!.dispose()
        }
        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        mStompClient.disconnect()

        mRestPingDisposable?.dispose()
        if (compositeDisposable != null) compositeDisposable!!.dispose()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
