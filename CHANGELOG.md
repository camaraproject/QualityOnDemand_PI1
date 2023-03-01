# ChangeLog QoD-API

## Release 0.8.0-dtag.1 (17.02.2023)
### Added <br><br>
### Changed
#### /sessions
    - ueAddr (ipv4) changed to ueId (ipv4, ipv6, msisdn, externalid)
    - asAddr (ipv4) changed to asId (ipv4, ipv6)
    - PortsSpec is a structred object now, not a plain string
    - Protocol (in & out) removed
    - QoS profiles renamed
    - Basepath changed

### Removed
    - /check-qos-availability endpoint

### Bugfixes <br><br>

## Release 0.1.0 (25.07.2022)

This is the initial, non-productive release for the Camara project.

### Southbound interface

- 3GPP TS 29.122 V17

### Added

##### /sessions

    - Create a new qod session
    - Get information for an existing session
    - Delete a session
    - Customer notification via callback URL
    - Using 3GPP AsSessionWithQoS

#### /check-qos-availability

    - Check for qos (quality of service) availability. Currently it returns a mock-value.

### Bugfixes
