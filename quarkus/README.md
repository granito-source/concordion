# Quarkus Integration for Concordion

This project provides [Concordion](https://www.concordion.org/)
integration with the
[Quarkus Platform](https://quarkus.io/).
For applications written for the Quarkus Platform, this provides the
convenience of having the Quarkus dependency injection available in
Concordion fixtures.

## Installation

The integration is available from
[Maven Central](https://central.sonatype.com/artifact/io.granito.concordion/concordion-quarkus).

## Usage

The integration provides a `TestEngine` implementation for
[JUnit Platform](https://junit.org/) to run Concordion specifications.
As such, the JUnit Platform must be configured to run the tests.

In order to use the integration, you need to replace the standard
`@org.concorgion.api.ConcordionFixture` annotation with
`@io.granito.concordion.quarkus.ConcordionFixture` annotation.

```java
import io.granito.concordion.quarkus.ConcordionFixture;
import jakarta.inject.Inject;

@ConcordionFixture
public class MyFixture {
    @Inject
    MyService myService;

    // ...
}
```
