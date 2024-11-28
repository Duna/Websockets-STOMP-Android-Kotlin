package com.cujoai.stomp.provider

import io.reactivex.Completable
import io.reactivex.Observable
import com.cujoai.stomp.dto.LifecycleEvent

/**
 * Created by Duna Marius 28.11.2024
 */
interface ConnectionProvider {
    /**
     * Subscribe this for receive stomp messages
     */
    fun messages(): Observable<String>

    /**
     * Sending stomp messages via you ConnectionProvider.
     * onError if not connected or error detected will be called, or onCompleted id sending started
     * TODO: send messages with ACK
     */
    fun send(stompMessage: String): Completable

    /**
     * Subscribe this for receive #LifecycleEvent events
     */
    fun lifecycle(): Observable<LifecycleEvent>

    /**
     * Disconnects from server. This is basically a Callable.
     * Automatically emits Lifecycle.CLOSE
     */
    fun disconnect(): Completable
}
