package com.cujoai.stomp

import com.cujoai.stomp.provider.OkHttpConnectionProvider
import okhttp3.OkHttpClient

/**
 * You can add own relay, just implement ConnectionProvider for you stomp transport,
 * such as web socket.
 *
 *
 * Created by Duna Marius 28.11.2024
 */
object Stomp {
    /**
     * Do not add an existing OkHttp client here, cannot be reused for web sockets
     * @param uri                URI to connect
     * @param connectHttpHeaders HTTP headers, will be passed with handshake query, may be null
     * @return StompClient for receiving and sending messages. Call #StompClient.connect
     */
    @JvmOverloads
    fun over(uri: String, connectHttpHeaders: Map<String, String>? = null, okHttpClient: OkHttpClient? = null): StompClient {
        return createStompClient(
            OkHttpConnectionProvider(
                uri,
                connectHttpHeaders,
                if ((okHttpClient == null)) createOkhttpClient() else okHttpClient
            )
        )
    }

    private fun createOkhttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AccessTokenInterceptor())
            .build()
    }

    private fun createStompClient(connectionProvider: com.cujoai.stomp.provider.ConnectionProvider): StompClient {
        return StompClient(connectionProvider)
    }
}
