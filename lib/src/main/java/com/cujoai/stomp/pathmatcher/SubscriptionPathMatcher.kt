package com.cujoai.stomp.pathmatcher

import com.cujoai.stomp.StompClient
import com.cujoai.stomp.dto.StompHeader
import com.cujoai.stomp.dto.StompMessage

class SubscriptionPathMatcher(private val stompClient: StompClient) : PathMatcher {
    override fun matches(path: String, msg: StompMessage): Boolean {
        // Compare subscription
        val pathSubscription = stompClient.getTopicId(path) ?: return false
        val subscription = msg.findHeader(StompHeader.SUBSCRIPTION)
        return pathSubscription == subscription
    }
}
