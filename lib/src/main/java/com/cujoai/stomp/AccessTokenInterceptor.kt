package com.cujoai.stomp

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AccessTokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req: Request = chain.request()
        return chain.proceed(
            newRequestWithAccessToken(
                req,
           ".JWT token.",
                "...whatever...."
            )
        )

    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String, xApiKey: String): Request {
        return request.newBuilder().header("Authorization", "Bearer $accessToken").header("x-api-key", xApiKey).build()
    }
}
