package com.cujoai.stomp.pathmatcher

import com.cujoai.stomp.dto.StompHeader
import com.cujoai.stomp.dto.StompMessage

class RabbitPathMatcher : PathMatcher {
    /**
     * RMQ-style wildcards.
     */
    override fun matches(path: String, msg: StompMessage): Boolean {
        val dest = msg.findHeader(StompHeader.DESTINATION) ?: return false

        // for example string "lorem.ipsum.*.sit":

        // split it up into ["lorem", "ipsum", "*", "sit"]
        val split = path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val transformed = ArrayList<String>()
        // check for wildcards and replace with corresponding regex
        for (s in split) {
            when (s) {
                "*" -> transformed.add("[^.]+")
                "#" ->                     // TODO: make this work with zero-word
                    // e.g. "lorem.#.dolor" should ideally match "lorem.dolor"
                    transformed.add(".*")

                else -> transformed.add(s.replace("\\*".toRegex(), ".*"))
            }
        }
        // at this point, 'transformed' looks like ["lorem", "ipsum", "[^.]+", "sit"]
        val sb = StringBuilder()
        for (s in transformed) {
            if (sb.isNotEmpty()) sb.append("\\.")
            sb.append(s)
        }
        val join = sb.toString()

        // join = "lorem\.ipsum\.[^.]+\.sit"
        return dest.matches(join.toRegex())
    }
}
