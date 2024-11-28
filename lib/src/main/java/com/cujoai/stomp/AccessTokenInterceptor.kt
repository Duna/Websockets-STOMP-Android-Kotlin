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
               // "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6IjQyYWIzMDY1LTc5N2UtNDlkZC05NWI5LWNjNWViZmVjNWI2ZCJ9.eyJ2ZXIiOiJ2MSIsInN1YiI6ImZlNDhkMjczLTAzN2YtNGI2OS1iY2EyLWE1NjlhN2UzNmMwZCIsImNsaWVudF9pZCI6ImN1am8tcGFydG5lci1hcHAtYW5kcm9pZC1jbGllbnQtdjMtdGVtcCIsInNjb3BlIjoib3RtOmFwaSBkdXM6aW5nZXN0Oio6b3RtOnRlbGVtZXRyeS0qIHNlbnRyeTpyZXB1dGF0aW9uOio6cmVhZCIsImF1ZCI6W10sInBhcnRuZXJJZCI6IjExODI2IiwiYWdlbnRJZCI6MTA0MDkwMywiYWdlbnRUeXBlIjoiQ1VKTyIsImFnZW50U2VyaWFsIjoiOTQ4M2M0MDE3Mzk2IiwiYXBwSW5zdGFuY2VJZCI6IkE1OTBBMkYxODgyQzQyQUZCODE4NTBEOTI1QTFERTZFIiwiaWF0IjoxNzMyNjkzMDEzLCJleHAiOjE3MzI4MjI2MTMsImlzcyI6ImV3MS1jdWpvLWNvcmUtZ2RlL2N1am8taWQiLCJqdGkiOiJKVElWMVBPU2x1SlZmQ3UifQ.eOAaB1oaxXAbcBjupxYVO2Lf2tlxsPxdyXQqkaP-XCC4rKq0iKxNcxiURGSv6n8_KhfdCXXCPk0_PwtRGaqZxg",
               "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6IjQyYWIzMDY1LTc5N2UtNDlkZC05NWI5LWNjNWViZmVjNWI2ZCJ9.eyJ2ZXIiOiJ2MSIsInN1YiI6ImZlNDhkMjczLTAzN2YtNGI2OS1iY2EyLWE1NjlhN2UzNmMwZCIsImNsaWVudF9pZCI6ImN1am8tcGFydG5lci1hcHAtYW5kcm9pZC1jbGllbnQtdjMtdGVtcCIsInNjb3BlIjoib3RtOmFwaSBkdXM6aW5nZXN0Oio6b3RtOnRlbGVtZXRyeS0qIHNlbnRyeTpyZXB1dGF0aW9uOio6cmVhZCIsImF1ZCI6W10sInBhcnRuZXJJZCI6IjExODI2IiwiYWdlbnRJZCI6MTA0MDkwMywiYWdlbnRUeXBlIjoiQ1VKTyIsImFnZW50U2VyaWFsIjoiOTQ4M2M0MDE3Mzk2IiwiYXBwSW5zdGFuY2VJZCI6IkE1OTBBMkYxODgyQzQyQUZCODE4NTBEOTI1QTFERTZFIiwiaWF0IjoxNzMyNjk1NjU5LCJleHAiOjE3MzI4MjUyNTksImlzcyI6ImV3MS1jdWpvLWNvcmUtZ2RlL2N1am8taWQiLCJqdGkiOiJKVElMSGpDZ2t0aFFnN0MifQ.pkA5wKNKBlD_G3HJrVcOdtsd27gdSz_eaAzgfJo3RdC2w76C8GsCzqk_e6Nm9zjmG_xR5whzwluRopbvr80LhQ",
                "67437b8c-f626-40e4-9c6c-1a7a5ed18efd"
            )
        )

    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String, xApiKey: String): Request {
        return request.newBuilder().header("Authorization", "Bearer $accessToken").header("x-api-key", xApiKey).build()
    }
}
