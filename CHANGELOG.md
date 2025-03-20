# Changelog quality-on-demand
<!-- TOC -->
* [Changelog quality-on-demand](#changelog-quality-on-demand)
  * [r1.3](#r13)
    * [quality-on-demand v0.11.1](#quality-on-demand-v0111)
      * [Added](#added)
      * [Changed](#changed)
    * [qos-profiles v0.11.1](#qos-profiles-v0111)
      * [Added](#added-1)
      * [Changed](#changed-1)
    * [Dependency updates](#dependency-updates)
  * [r1.2](#r12)
    * [quality-on-demand v0.11.0](#quality-on-demand-v0110)
      * [Added](#added-2)
      * [Changed](#changed-2)
    * [qos-profiles v0.11.0](#qos-profiles-v0110)
      * [Changed](#changed-3)
    * [Additional Changes](#additional-changes)
  * [v0.10.1-patch](#v0101-patch)
  * [v0.10.1](#v0101)
  * [v0.10.0](#v0100)
  * [v0.9.0](#v090)
  * [v0.8.1](#v081)
  * [v0.8.0](#v080)
<!-- TOC -->


## r1.3

### quality-on-demand v0.11.1

#### Added

- n/a

#### Changed

- Changed the response-status for `/extend`, when the extended duration is not able to be extended, as it already reached the limit.

### qos-profiles v0.11.1

#### Added

- n/a

#### Changed

- n/a

### Dependency updates

- Spring-Boot v3.4.2 

## r1.2

### quality-on-demand v0.11.0

#### Added
- device can also be provided inside the access-token. The provider will extract the corresponding claims to use them for the following flows.
- If a device is given via accessToken and request-body, then an additional match-validation is triggered. This will verify that, e.g., the phoneNumber  of the access-token matches the one of the request-body If there is a mismatch, the provider will answer with HTTP-403 INVALID_TOKEN_CONTEXT
- If there is no device provided by access-token or request-body then the provider will return with HTTP-422 UNIDENTIFIABLE_DEVICE
- If the provided device-identifier is not supported by the provider implementation, the provider will return with HTTP-422 UNSUPPORTED_DEVICE_IDENTIFIERS
- New endpoint for retrieving all sessions for a specific device retrieveSessions

#### Changed
- Sessions can only be extended when they are in state `AVAILABLE`
- Sessions start when they get `AVAILABLE` otherwise they stay in the status `REQUESTED` with a duration of one day (86400 seconds).
- updated subscription-model by using `sink` and `sinkCredentials`
- `sinkCredentials` can only support `ACCESSTOKEN`-type, others will be declined (e.g. PLAIN, REFRESHTOKEN)
- optional `device`-parameter 
- `startedAt` and `expiresAt` are now in Date-Time format
- devíce can be delivered via access-token
- added `statusInfo` as parameter in `SessionInfo`
- mandatory `+` for `phoneNumber`


### qos-profiles v0.11.0

#### Changed

- changed the retrieval operation to allow the get the QoS Profiles available for a given device  
- changed query from GET /qos-profiles to POST /retrieve-qos-profiles

### Additional Changes
- Upgraded to Spring Boot 3.4.1 and other dependencies
- Switched to MongoDB - database

## v0.10.1-patch

- remove `/notifications` from token-validation

## v0.10.1

- adaption for extending a session
- check creation of session based on requested QoS profile
- dependency upgrades

## v0.10.0

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

## v0.9.0

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

## v0.8.1

- Update from notificationsUri to notificationsUrl.
- Added Generic error 500 to remaining procedures.

## v0.8.0

- First implementation release of QoD
