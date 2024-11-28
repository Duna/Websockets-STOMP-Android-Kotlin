package com.cujoai.stomp.pathmatcher

import com.cujoai.stomp.dto.StompHeader
import com.cujoai.stomp.dto.StompMessage

class SimplePathMatcher : PathMatcher {
    override fun matches(path: String, msg: StompMessage): Boolean {
        val dest = msg.findHeader(StompHeader.DESTINATION)
        return if (dest == null) false
        else path == dest
    }
}
