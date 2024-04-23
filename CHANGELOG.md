# Changelog QualityOnDemand

## Table of Contents

- **[v0.10.1](#v0101)**
- [v0.10.0](#v0100)
- [v0.9.0](#v090)
- [v0.8.1](#v081)
- [v0.8.0](#v080)

### v0.10.1

- adaption for extending a session
- check creation of session based on requested QoS profile
- dependency upgrades

### v0.10.0

- Align event notification with CloudEvents spec
- Added a new operation /sessions/{sessionId}/extend which allows extending the duration of an active session
- Added "DELETE_REQUESTED" as statusInfo information via CloudEvent, when a session got deleted by the user
- If the network calls back with SESSION_TERMINATION or FAILED_RESOURCES_ALLOCATION, then after 360s the session will be deleted.
- Single IP addresses in Device-model specified with standard formats instead of patterns
- Add IPv4 validation for device.ipv4Address.publicAddress
- Improve security for REST-Endpoints
- Notification event structure updated to comply with CloudEvents specification.
- In the Device model, single IP addresses are now specified with standard formats instead of patterns.
- Notifications will now be sent for all changes of QosStatus, even if initiated by the client.
- remove automatic attachment of "/notifications" from notificationUrl for CloudEvents

### v0.9.0

- Introduced qosStatus and corresponding notification event
- new /qos-profiles - endpoints
- minor changes in Exception-Handling
- Updated method for identifying devices by IPv4 address.
- Updated notification event-related fields to adhere to CAMARA design guidelines.
- Changed request parameter class from UeId to Device.
- Renamed request parameter from MSISDN to phoneNumber.
- Renamed request parameter from externalId to networkAccessIdentifier.
- Renamed request parameter from ipv4addr to ipv4Address.
- Renamed request parameter from ipv6Addr to ipv6Address.
- Renamed request parameter from asId to applicationServer.
- Renamed request parameter from uePorts to devicePorts.
- Renamed request parameter from asPorts to applicationServerPorts.
- Renamed request parameter from qos to qosProfile.

### v0.8.1

- Update from notificationsUri to notificationsUrl.
- Added Generic error 500 to remaining procedures.

### v0.8.0

- First implementation release of QoD
