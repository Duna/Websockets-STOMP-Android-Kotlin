package com.cujoai.stomp.dto

/**
 * Created by Duna Marius 28.11.2024
 */
object StompCommand {
    const val CONNECT: String = "CONNECT"
    const val CONNECTED: String = "CONNECTED"
    const val SEND: String = "SEND"
    const val MESSAGE: String = "MESSAGE"
    const val SUBSCRIBE: String = "SUBSCRIBE"
    const val UNSUBSCRIBE: String = "UNSUBSCRIBE"

    const val UNKNOWN: String = "UNKNOWN"
}
