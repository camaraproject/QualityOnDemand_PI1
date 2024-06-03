# Development of Quality on Demand APIs

## Get started

### Prerequisites

Make sure Maven 3 or greater and JDK 17 are installed on your system.

### How to run locally

Please ensure, that the prerequisites are fulfilled.
To run a QoD API locally, you have to follow these steps:

* Build
* Create Database
* Setup Kafka
* Start QoD API

#### Build

Build the project by running ```mvn clean package```

#### Create Redis-Database (optional)

The creation of a Redis-Database is only needed if you use another profile than **'local'**.
Otherwise, QoD API will use an in-memory H2-database.

If a profile is used different to **'local'**, the QoD API needs a Redis database to persist the session data.
Therefore, you can use a local instance of Redis DB or start Redis from Docker container:

* start Redis from Docker container by running ```docker run --name qos-redis -p 6379:6379 -d redis```
* local instance on Windows: https://github.com/zkteco-home/redis-windows
* local instance on Linux: https://redis.io/docs/getting-started/installation/install-redis-on-linux/

#### Setup Kafka

To send and listen CloudEvent-Notifications from QOD-API and Webhook-dispatcher respectively, Kafka server needs to be set up locally. After setup, its host and port information need to be added as follows inside [application.yaml](https://gitlab.devops.telekom.de/hnce/development/avp-dev/avp/-/blob/develop/camara/core/src/main/resources/application.yml?ref_type=heads)
```
kafka:
    producer:
        bootstrap-servers: localhost:29092
```
#### Start QoD API

For information about how to configure the QoD API, please refer to the next section "Configuration".

Start the qod-api service from terminal or from an IDE or with docker

* from
  terminal:```java --add-opens=java.base/java.net=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar core\target\senf-core-<current version>.jar --spring.profiles.active=local```
* from an IDE: Application can also be started directly from an IDE like IntelliJ, Eclipse, etc. by passing VM
  options in the "Run Configuration":
    * --add-opens=java.base/java.net=ALL-UNNAMED
    * --spring.profiles.active=local
* with docker:
    * ```docker build -t qod-api . ```
    * ```docker run -dp 9091:9091 -p 9092:9092 qod-api```

With the 'local'-profile the application will automatically connect to an in-memory H2-database.

## Configuration

The configuration can be adapted here: core/src/main/resources/application.yml

The below table lists the environment variables that are used to configure the application properly. This reference
implementation for the QoD API makes use of the AsSessionWithQoS API (https://www.3gpp.org/ftp/Specs/latest/Rel-17)
which is usually made available as a part of a NEF/SCEF system. All variables in the below table starting with "NETWORK"
can be understood as NEF/SCEF.

| ENV                              | Description                                                                                                              |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| SERVER_PORT                      | TCP/IP port listened on by application                                                                                   |
| MANAGEMENT_SERVER_PORT           | TCP/IP port listened on by application management facilities                                                             |
| NETWORK_SERVER_APIROOT           | SCEF endpoint API root URL                                                                                               |
| NETWORK_SERVER_SCSASID           | ID of SCS or AS providing SCEF endpoint                                                                                  |
| NETWORK_SERVER_SUPPORTEDFEATURES | A list of supported features used as described in subclause 5.8. (3GPP)                                                  |
| NETWORK_AUTH_OAUTH_TOKEN         | OAuth authentication token for calling SCEF endpoint                                                                     |
| NETWORK_NOTIFICATIONS_URL        | URL for delivering (POST) notifications from SCEF, it should include the path /3gpp-as-session-with-qos/v1/notifications |
| QOD_QOS_REFERENCES               | QoS reference for predefined specific QoS parameter set                                                                  |
| SPRING_REDIS_HOST                | Redis server host                                                                                                        |
| SPRING_REDIS_PORT                | Port where the Redis server is listening                                                                                 |
| EXPIRATION_TIME-BEFORE-HANDLING  | How many seconds before expiration, should an ExpiredSessionTask be created                                              |
| EXPIRATION_TRIGGER-INTERVAL      | How often should the ExpiredSessionMonitor check for (almost) expired sessions                                           |
| EXPIRATION_LOCK-TIME             | How many seconds the session should be locked for the processing of the deletion task                                    |
| MASK-SENSIBLE-DATA               | If set to true, sensible data is masked in response body                                                                 |
| ALLOW-MULTIPLE-UEADDR            | If set to true, network segments are allowed for ueAddr                                                                  |


## Implementation Related
### Introduction
This document outlines the implementation details for the Quality-on-Demand (QoD) API, which allows developers to request stable latency or throughput for their applications in 4G/5G networks.

### Implemented Version
v0.10.0

### API Functionality

The QoD API offers functionalities for managing QoS sessions and profiles:

- **Create QoS Sessions:** Developers can create new sessions by specifying the device, application server, desired QoS profile, and optional duration and notification details.
- **Manage QoS Sessions:**
    - **Get Session Information:** Retrieve information about an active session using its ID.
    - **Extend Session Duration:** Extend the duration of an active session.
    - **Delete Session:** Terminate an active session.
- **Access QoS Profiles:**
    - **Get All QoS Profiles:** Retrieve a list of all available QoS profiles, optionally filtered by name or status (ACTIVE, INACTIVE, DEPRECATED).
    - **Get Specific QoS Profile:** Retrieve details about a specific QoS profile by its name.

```plantuml
left to right direction
skinparam componentStyle rectangle

    node "Notification-url" {
      [Client]
    }
    node "NEF Server" {
      [NEF]
    }
    component [User] #LightGrey
    component [Client] #Pink
    component [Qod API] #Pink
    component [Webhook Dispatcher] #Pink
    component [Kafka] #LightBlue
    component [NEF] #Yellow
    [User] --> [Client]: Rest Get Event
    [User] --> [Qod API]: Create Session
    [Qod API] ..> [Kafka]: Pub Event
    [Webhook Dispatcher] ..> [Client]: Rest Callback
    [Webhook Dispatcher] ..> [Kafka]: Sub Event
    [NEF] ..> [Qod API]: Rest Callback SESSION_TERMINATION
    [Qod API] --> [NEF]

```

### Implementation Requirements

- **Swagger Specification:** The API adheres to the provided Swagger specification (https://swagger.io/specification/v2/) to ensure consistency and facilitate client development.
- **Authentication:** The API utilizes OAuth 2.0 for authentication. Two flows are supported:
    - **Client Credentials Grant:** Used for server-to-server communication between trusted partners/clients.
- **Security:** Implement appropriate security measures to protect user data and resources.
- **Data Model:** Utilize the provided data model definitions in the Swagger specification for request and response structures.
- **Error Handling:** Implement robust error handling mechanisms to provide informative messages for various error scenarios.
- **Callback Notifications:** The API supports optional notification callbacks (via CloudEvents) using webhooks to inform developers about session status changes (e.g., DURATION_EXPIRED, NETWORK_TERMINATED, DELETE_REQUESTED).

## Validations

The QoD reference implementation has been qualified against the following network service vendors:

| System Name | Vendor   | Camara Testing Member | Qualified |
|-------------|----------|-----------------------|-----------|
| 5G-NEF      | Ericsson | DT                    | Yes       |

We actively ask other Camara members to validate this implementation against their available backend network services and
share the results with the community.

## Contribution

Contribution and feedback is always welcome. For information how to contribute, please refer to our
[Contribution Guideline](https://github.com/camaraproject/Governance/blob/main/CONTRIBUTING.md)

## License

Copyright (c) 2024 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC

All contributors / copyright owners license this file to you under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
