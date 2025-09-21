package io.granito.concordion.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import io.granito.concordion.api.QuarkusConcordionFixture;
import org.junit.jupiter.api.Test;

public class JUnit5QuarkusConcordionProviderTest {
    private final JUnit5QuarkusConcordionProvider provider =
        new JUnit5QuarkusConcordionProvider();

    @Test
    void doesNotIndicateConcordionFixtureWhenNotAnnotated()
    {
        assertThat(provider.isConcordionFixture(PlainClass.class))
            .isFalse();
    }

    @Test
    void indicatesConcordionFixtureWhenAnnotated()
    {
        assertThat(provider.isConcordionFixture(AnnotatedTest.class))
            .isTrue();
    }

    private static class PlainClass {
    }

    @QuarkusConcordionFixture
    private static class AnnotatedTest {
    }
}
