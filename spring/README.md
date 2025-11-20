# Spring Integration for Concordion

This project provides [Concordion](https://www.concordion.org/)
integration with the
[Spring Framework](https://spring.io/projects/spring-framework).
For applications written for the Spring Framework, this provides the
convenience of having the Spring TestContext Framework, including
dependency injection, REST clients, transaction management, and
other features, available in Concordion fixtures.

## Installation

The integration is available from
[Maven Central](https://central.sonatype.com/artifact/io.granito.concordion/concordion-spring).

## Usage

The integration provides a `TestEngine` implementation for
[JUnit Platform](https://junit.org/) to run Concordion specifications.
As such, the JUnit Platform must be configured to run the tests.

In order to use the integration, you need to replace the standard
`@org.concorgion.api.ConcordionFixture` annotation with
`@io.granito.concordion.spring.ConcordionFixture` annotation.

```java
import io.granito.concordion.spring.ConcordionFixture;
import org.springframework.beans.factory.annotation.Autowired;

@ConcordionFixture
public class MyFixture {
    @Autowired
    private MyService myService;

    // ...
}
```
