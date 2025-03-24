# Hapi FHIR Spring Boot

This project is an application skeleton for a Spring Boot application using Hapi FHIR.
The project contains a Patient Provider with an endpoint that returns a Patient Resource.
The app just shows how to use Spring Boot and Hapi FHIR together.

## Getting Started

To get you started you can simply clone the `hapi-fhir` repository and install the dependencies:

### Prerequisites

You need [git][git] to clone the `hapi-fhir` repository.

You will need [OpenJDK 17][jdk-download] and [Maven][maven].

### Clone `hapi-fhir`

Clone the `hapi-fhir` repository using git:

```bash
git clone https://github.com/jordigravi/hapi-fhir.git
cd hapi-fhir
```

### Install Dependencies

In order to install the dependencies and generate the jar you must run:

```bash
mvn clean install
```

### Run

To launch the server, simply run with java -jar the generated jar file.

```bash
cd target
java -jar spring-boot-hapi-fhir.jar
```

## API

You will find the swagger UI at http://localhost:8080/fhir/swagger-ui/

## Docker

### Build docker image

You can create the image with the following command:

```bash
docker build -t springboot-hapifhir . 
```

### Run the container

```bash
docker compose up
```

The app will be available at http://localhost:8080/fhir/swagger-ui/

## Documentation

See [Documentation](doc/README.md) section for further details about other technical specifications.


[git]: https://git-scm.com/
[sboot]: https://projects.spring.io/spring-boot/
[maven]: https://maven.apache.org/download.cgi
[jdk-download]: https://adoptopenjdk.net/
[JEE]: http://www.oracle.com/technetwork/java/javaee/tech/index.html
[jwt]: https://jwt.io/
[cors]: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing
[swagger]: https://swagger.io/
[allure]: https://docs.qameta.io/allure/
[junit]: https://junit.org/junit5/
