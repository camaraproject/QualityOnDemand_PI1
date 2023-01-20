# ChangeLog QoD-API

## Next Release x.x.x (IN PROGRESS, NOT RELEASED SO FAR)

### Features

##### /sessions

    - Parameters protocolIn and protocolOut are optional, default value is ANY
    - When creating new session the response can have an array of messages which contain information / warnings
    - Change duration of an existing qod session

### Bugfixes
    
    - session validation for protocol overlap

## Release 0.1.0 (25.07.2022)

This is the initial, non-productive release for the Camara project.

### Southbound interface

- 3GPP TS 29.122 V17

### Features

##### /sessions

    - Create a new qod session
    - Get information for an existing session
    - Delete a session
    - Customer notification via callback URL
    - Using 3GPP AsSessionWithQoS

#### /check-qos-availability

    - Check for qos (quality of service) availability. Currently it returns a mock-value.

### Bugfixes
