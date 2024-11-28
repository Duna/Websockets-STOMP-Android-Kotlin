package com.cujoai.stomp.pathmatcher

import com.cujoai.stomp.dto.StompMessage

interface PathMatcher {
    fun matches(path: String, msg: StompMessage): Boolean
}
