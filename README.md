# QualityOnDemand

This Quality on Demand (QoD) Service Enabling Function (SeNF) simplifies mobile network resources
allocation.

## QoD SeNF API description

This document outlines the implementation details for the Quality-on-Demand (QoD) API, which allows developers to request stable latency or
throughput for their applications in 4G/5G networks.

This service uses API first approach. It uses OpenAPI Generator to generate server stubs automatically.

### Implemented Version

r1.2

### API Functionality

The QoD API offers functionalities for managing QoS sessions and profiles:

- **Create QoS Sessions:** Developers can create new sessions by specifying the device, application server, desired QoS profile, and
  optional duration and notification details.
- **Manage QoS Sessions:**
    - **Get Session Information:** Retrieve information about an active session using its ID.
    - **Extend Session Duration:** Extend the duration of an active session.
    - **Delete Session:** Terminate an active session.
- **Access QoS Profiles:**
    - **Get All QoS Profiles:** Retrieve a list of all available QoS profiles, optionally filtered by name or status (ACTIVE, INACTIVE,
      DEPRECATED).
    - **Get Specific QoS Profile:** Retrieve details about a specific QoS profile by its name.

**_Note: This implementation does not provide a qos-provisioning solution._**
## How to run service locally

### Prerequisites

Please ensure that the prerequisites are fulfilled.
To run a QoD API locally, you have to follow these steps:

* Make sure Maven 3.x and JDK 21 or greater are installed in the system
* Build
* Create Mongo Database
* Setup Kafka
* Possible NEF - connection / mocks in the background
* Setup kafka and define its host and port information inside the application.yaml

### Build

Build the project by running ```mvn clean package```

### Create Database

#### Mongo

You can use a local instance of MongoDB or start Mongo from a Docker container on port 27017.

### Setup Kafka

To send and listen CloudEvent-Notifications from QOD-API and Webhook-dispatcher respectively, Kafka server needs to be set up locally. After
setup, its host and port information need to be added as follows inside `application.yaml`

```
kafka:
    producer:
        bootstrap-servers: localhost:29092
```

### Start QoD

#### Running from terminal

* Start the qod-senf service locally by
  running
  ```java --add-opens=java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.time.zone=ALL-UNNAMED -jar core\target\senf-core-<current version>.jar -Dspring.profiles.active=local```

#### Running from IDE

Application can also be started directly from IDE like IntelliJ, Eclipse, etc. bypassing VM
options
```--add-opens=java.base/java.net=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.time.zone=ALL-UNNAMED -Dspring.profiles.active=local```
in the "Run
Configuration"

#### Running locally on docker

    mvn clean package
    docker build -t qod-api .
    docker run -dp 9091:9091 -p 9092:9092 qod-api

#### Swagger UI documentation

After the application was successfully started, the swagger-ui is reachable on: http://localhost:9091/swagger.

The swagger displays the api-specification:

- [Quality On Demand API](/api/src/main/resources/static/quality-on-demand.yaml)
- [QoS Profiles API](/api/src/main/resources/static/qos-profiles.yaml)

## Service Design

### API First Approach

QoD SenF uses API first approach.
OpenAPI 3 format describes both northbound and southbound APIs.
The QoD SenF southbound API is defined and provided by 3GPP.
It is currently T8 reference point for Northbound API Release 17.

Note that the official specification T8 reference point for the Northbound API OpenAPI document is
adjusted - basic authentication security schema is added.

The corresponding client (southbound API) and server (northbound API) implementations are
automatically generated.

## Implementation Related

### Session-Workflow

```plantuml
left to right direction
skinparam componentStyle rectangle

    node "Sink" {
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

- **Swagger Specification:** The API adheres to the provided Swagger specification (https://swagger.io/specification/v2/) to ensure
  consistency and facilitate client development.
- **Authentication:** The API uses OAuth 2.0 for authentication. Currently, the below flow is supported:
    - **Client Credentials Grant:** Used for server-to-server communication between trusted partners/clients.
- **Security:** Implement appropriate security measures to protect user data and resources.
- **Data Model:** Utilize the provided data model definitions in the Swagger specification for request and response structures.
- **Error Handling:** Implement robust error handling mechanisms to provide informative messages for various error scenarios.
- **Callback Notifications:** The API supports optional notification callbacks (via CloudEvents) using webhooks to inform developers about
  session status changes (e.g., DURATION_EXPIRED, NETWORK_TERMINATED, DELETE_REQUESTED).

## Code-Style

To follow the rules of the style guide, the [.editorconfig](.editorconfig) - file is used to have a default
code-style setting integrated based on Google code style.

## License

To manage the license of this project and its dependencies, the plugin license-maven-plugin by
org.codehaus.mojo is used.

The overview of the Third Party dependencies is updated by
running ```mvn license:aggregate-add-third-party``` in the root project.
Please run the mentioned command if you add new dependencies to the project or update
existing dependencies.

The license header is added to the files by running ```mvn license:update-file-header``` in the root
project.
Please run the mentioned command if you add new files to the project.