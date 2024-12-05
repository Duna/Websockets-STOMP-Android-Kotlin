# STOMP protocol via WebSocket for Android

## Overview

![Visitor Count](https://profile-counter.glitch.me/Duna/count.svg)

This library provide support for STOMP protocol https://stomp.github.io/
At now library works only as client for backend with support STOMP, such as
NodeJS (stompjs or other) or Spring Boot (SockJS).

**Basic usage**
``` kotlin

 val mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://10.10.2.1:8080/websocket")
 mStompClient.connect()
  
 mStompClient.topic("/topic/greetings").subscribe(topicMessage -> {
     Log.d(TAG, topicMessage.getPayload())
 });
  
 mStompClient.send("/topic/hello-msg-mapping", "My first STOMP message!").subscribe()
 
 mStompClient.disconnect()

```

Method `Stomp.over` consume class for create connection as first parameter.
You must provide dependency for lib and pass class.
At now supported connection provider:
- `okhttp3.WebSocket.class` ('com.squareup.okhttp3:okhttp:4.12.0')

You can add own connection provider. Just implement interface `ConnectionProvider`.
If you implement new provider, please create pull request :)

**Subscribe lifecycle connection**
``` kotlin
mStompClient.lifecycle().subscribe(lifecycleEvent -> {
    switch (lifecycleEvent.type) {
    
        case OPENED:
            Log.d(TAG, "Stomp connection opened");
            break;
            
        case ERROR:
            Log.e(TAG, "Error", lifecycleEvent.getException());
            break;
            
        case CLOSED:
             Log.d(TAG, "Stomp connection closed");
             break;
    }
});
```

Library support just send & receive messages. ACK messages, transactions not implemented yet.
