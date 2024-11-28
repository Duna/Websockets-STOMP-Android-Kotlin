package ua.naiksoftware.stomp

import com.cujoai.stomp.dto.StompCommand
import com.cujoai.stomp.dto.StompHeader
import com.cujoai.stomp.dto.StompMessage
import com.cujoai.stomp.pathmatcher.RabbitPathMatcher

class PathMatcherTests extends Configuration {

    def "rmq-style matcher must return expected value"() {
        given:
        def matcher = new RabbitPathMatcher()

        expect:
        matcher.matches(path, message(dest)) == value

        where:
        path              |          dest          | value
        'lorem/*/ipsum'   | 'lorem/any/ipsum'      | true
        'lorem/*/ipsum'   | 'lorem/ipsum'          | false
        'lorem/*/prefix*' | 'lorem/ipsum/prefix123'| true
        'lorem/*/pref*3'  | 'lorem/ipsum/prefix123'| true
    }

    def message(String dest) {
        return new StompMessage(StompCommand.MESSAGE, [new StompHeader(StompHeader.DESTINATION, dest)], null)
    }
}
