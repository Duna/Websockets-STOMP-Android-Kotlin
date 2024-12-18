package com.cujoai.stomp

import android.annotation.SuppressLint
import android.util.Log
import com.cujoai.stomp.HeartBeatTask.FailedListener
import com.cujoai.stomp.HeartBeatTask.SendCallback
import com.cujoai.stomp.dto.LifecycleEvent
import com.cujoai.stomp.dto.StompCommand
import com.cujoai.stomp.dto.StompHeader
import com.cujoai.stomp.dto.StompMessage
import com.cujoai.stomp.pathmatcher.PathMatcher
import com.cujoai.stomp.pathmatcher.SimplePathMatcher
import com.cujoai.stomp.provider.ConnectionProvider
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Duna Marius 28.11.2024
 */
class StompClient(private val connectionProvider: ConnectionProvider) {
    private var topics: ConcurrentHashMap<String, String>? = null
    private var legacyWhitespace = false

    private var messageStream: PublishSubject<StompMessage>? = null
    private var connectionStream: BehaviorSubject<Boolean>? = null
    private val streamMap = ConcurrentHashMap<String, Flowable<StompMessage>>()
    private var pathMatcher: PathMatcher
    private var lifecycleDisposable: Disposable? = null
    private var messagesDisposable: Disposable? = null
    private val lifecyclePublishSubject = PublishSubject.create<LifecycleEvent>()
    private var headers: List<StompHeader>? = null
    private val heartBeatTask: HeartBeatTask

    init {
        pathMatcher = SimplePathMatcher()
        heartBeatTask = HeartBeatTask(object : SendCallback {
            override fun sendClientHeartBeat(pingMessage: String?) {
                sendHeartBeat(pingMessage!!)
            }
        }, object : FailedListener {
            override fun onServerHeartBeatFailed() {
                lifecyclePublishSubject.onNext(LifecycleEvent(LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT))
            }
        }
        )
    }

    /**
     * Sets the heartbeat interval to request from the server.
     *
     *
     * Not very useful yet, because we don't have any heartbeat logic on our side.
     *
     * @param ms heartbeat time in milliseconds
     */
    fun withServerHeartbeat(ms: Int): StompClient {
        heartBeatTask.setServerHeartbeat(ms.toLong())
        return this
    }

    /**
     * Sets the heartbeat interval that client propose to send.
     *
     *
     * Not very useful yet, because we don't have any heartbeat logic on our side.
     *
     * @param ms heartbeat time in milliseconds
     */
    fun withClientHeartbeat(ms: Int): StompClient {
        heartBeatTask.setClientHeartbeat(ms.toLong())
        return this
    }

    /**
     * Connect to websocket. If already connected, this will silently fail.
     *
     * @param _headers HTTP headers to send in the INITIAL REQUEST, i.e. during the protocol upgrade
     */
    /**
     * Connect without reconnect if connected
     */
    @SuppressLint("CheckResult")
    @JvmOverloads
    fun connect(_headers: List<StompHeader>? = null) {
        Log.d(TAG, "Connect")

        this.headers = _headers

        if (isConnected) {
            Log.d(TAG, "Already connected, ignore")
            return
        }
        lifecycleDisposable = connectionProvider.lifecycle()
            .subscribe { lifecycleEvent: LifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        val headers: MutableList<StompHeader> = ArrayList()
                        headers.add(StompHeader(StompHeader.VERSION, SUPPORTED_VERSIONS))
                        headers.add(
                            StompHeader(
                                StompHeader.HEART_BEAT,
                                heartBeatTask.getClientHeartbeat().toString() + "," + heartBeatTask.getServerHeartbeat()
                            )
                        )

                        if (_headers != null) headers.addAll(_headers)

                        val mess = StompMessage(StompCommand.CONNECT, headers, null).compile(legacyWhitespace)
                        connectionProvider.send(mess).subscribe(
                            {
                                Log.d(TAG, "Publish open")
                                lifecyclePublishSubject.onNext(lifecycleEvent)
                            },
                            { error ->
                                Log.e(TAG, "Error occurred: ${error.message}", error)
                            }
                        )
                    }

                    LifecycleEvent.Type.CLOSED -> {
                        Log.d(TAG, "Socket closed")
                        disconnect()
                    }

                    LifecycleEvent.Type.ERROR -> {
                        Log.d(TAG, "Socket closed with error")
                        lifecyclePublishSubject.onNext(lifecycleEvent)
                    }

                    LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {}//TODO()
                }
            }

        messagesDisposable = connectionProvider.messages()
            .map { StompMessage.from(it) }
            .filter { message: StompMessage? -> heartBeatTask.consumeHeartBeat(message!!) }
            .doOnNext { t: StompMessage -> getMessageStream().onNext(t) }
            .filter { msg: StompMessage -> msg.stompCommand == StompCommand.CONNECTED }
            .subscribe({
                getConnectionStream().onNext(true)
            },
                { onError: Throwable? ->
                    Log.e(TAG, "Error parsing message", onError)
                })
    }

    @Synchronized
    private fun getConnectionStream(): BehaviorSubject<Boolean> {
        if (connectionStream == null || connectionStream!!.hasComplete()) {
            connectionStream = BehaviorSubject.createDefault(false)
        }
        return connectionStream!!
    }

    @Synchronized
    private fun getMessageStream(): PublishSubject<StompMessage> {
        if (messageStream == null || messageStream!!.hasComplete()) {
            messageStream = PublishSubject.create()
        }
        return messageStream as PublishSubject<StompMessage>
    }

    @JvmOverloads
    fun send(destination: String, data: String? = null): Completable {
        return send(
            StompMessage(
                StompCommand.SEND,
                listOf(StompHeader(StompHeader.DESTINATION, destination)),
                data
            )
        )
    }

    fun send(stompMessage: StompMessage): Completable {
        val completable = connectionProvider.send(stompMessage.compile(legacyWhitespace))
        val connectionComplete: CompletableSource = getConnectionStream()
            .filter { isConnected: Boolean? -> isConnected!! }
            .firstElement().ignoreElement()
        return completable
            .startWith(connectionComplete)
    }

    @SuppressLint("CheckResult")
    private fun sendHeartBeat(pingMessage: String) {
        val completable = connectionProvider.send(pingMessage)
        val connectionComplete: CompletableSource = getConnectionStream()
            .filter { isConnected: Boolean? -> isConnected!! }
            .firstElement().ignoreElement()
        completable.startWith(connectionComplete)
            .onErrorComplete()
            .subscribe()
    }

    fun lifecycle(): Flowable<LifecycleEvent> {
        return lifecyclePublishSubject.toFlowable(BackpressureStrategy.BUFFER)
    }

    /**
     * Disconnect from server, and then reconnect with the last-used headers
     */
    @SuppressLint("CheckResult")
    fun reconnect() {
        disconnectCompletable()
            .subscribe(
                { connect(headers) },
                { e: Throwable? -> Log.e(TAG, "Disconnect error", e) })
    }

    @SuppressLint("CheckResult")
    fun disconnect() {
        disconnectCompletable().subscribe({},
            { e: Throwable? -> Log.e(TAG, "Disconnect error", e) })
    }

    fun disconnectCompletable(): Completable {
        heartBeatTask.shutdown()

        if (lifecycleDisposable != null) {
            lifecycleDisposable!!.dispose()
        }
        if (messagesDisposable != null) {
            messagesDisposable!!.dispose()
        }

        return connectionProvider.disconnect()
            .doFinally {
                Log.d(TAG, "Stomp disconnected")
                getConnectionStream().onComplete()
                getMessageStream().onComplete()
                lifecyclePublishSubject.onNext(LifecycleEvent(LifecycleEvent.Type.CLOSED))
            }
    }

    fun topic(destinationPath: String): Flowable<StompMessage>? {
        return topic(destinationPath, null)
    }

    fun topic(destPath: String, headerList: List<StompHeader>?): Flowable<StompMessage>? {
        if (!streamMap.containsKey(destPath)) streamMap[destPath] = Completable.defer { subscribePath(destPath, headerList) }.andThen(
            getMessageStream()
                .filter { msg: StompMessage? -> pathMatcher.matches(destPath, msg!!) }
                .toFlowable(BackpressureStrategy.BUFFER)
                .doFinally { unsubscribePath(destPath).subscribe() }
                .share())
        return streamMap[destPath]
    }

    private fun subscribePath(destinationPath: String, headerList: List<StompHeader>?): Completable {
        val topicId = UUID.randomUUID().toString()

        if (topics == null) topics = ConcurrentHashMap()

        // Only continue if we don't already have a subscription to the topic
        if (topics!!.containsKey(destinationPath)) {
            Log.d(TAG, "Attempted to subscribe to already-subscribed path!")
            return Completable.complete()
        }

        topics!![destinationPath] = topicId
        val headers: MutableList<StompHeader> = ArrayList()
        headers.add(StompHeader(StompHeader.ID, topicId))
        headers.add(StompHeader(StompHeader.DESTINATION, destinationPath))
        headers.add(StompHeader(StompHeader.ACK, DEFAULT_ACK))
        if (headerList != null) headers.addAll(headerList)
        return send(
            StompMessage(
                StompCommand.SUBSCRIBE,
                headers, null
            )
        )
            .doOnError { throwable: Throwable? -> unsubscribePath(destinationPath).subscribe() }
    }


    private fun unsubscribePath(dest: String): Completable {
        streamMap.remove(dest)

        val topicId = topics!![dest] ?: return Completable.complete()

        topics!!.remove(dest)

        Log.d(TAG, "Unsubscribe path: $dest id: $topicId")

        return send(
            StompMessage(
                StompCommand.UNSUBSCRIBE,
                listOf(StompHeader(StompHeader.ID, topicId)), null
            )
        ).onErrorComplete()
    }

    /**
     * Set the wildcard or other matcher for Topic subscription.
     *
     *
     * Right now, the only options are simple, rmq supported.
     * But you can write you own matcher by implementing [PathMatcher]
     *
     *
     * When set to [RabbitPathMatcher], topic subscription allows for RMQ-style wildcards.
     *
     *
     *
     * @param pathMatcher Set to [SimplePathMatcher] by default
     */
    fun setPathMatcher(pathMatcher: PathMatcher) {
        this.pathMatcher = pathMatcher
    }

    val isConnected: Boolean
        get() = getConnectionStream().value!!

    /**
     * Reverts to the old frame formatting, which included two newlines between the message body
     * and the end-of-frame marker.
     *
     *
     * Legacy: Body\n\n^@
     *
     *
     * Default: Body^@
     *
     * @param legacyWhitespace whether to append an extra two newlines
     * @see [The STOMP spec](http://stomp.github.io/stomp-specification-1.2.html.STOMP_Frames)
     */
    fun setLegacyWhitespace(legacyWhitespace: Boolean) {
        this.legacyWhitespace = legacyWhitespace
    }

    /** returns the to topic (subscription id) corresponding to a given destination
     * @param dest the destination
     * @return the topic (subscription id) or null if no topic corresponds to the destination
     */
    fun getTopicId(dest: String): String? {
        return topics!![dest]
    }

    companion object {
        private val TAG: String = StompClient::class.java.simpleName

        const val SUPPORTED_VERSIONS: String = "1.1,1.2"
        const val DEFAULT_ACK: String = "auto"
    }
}
