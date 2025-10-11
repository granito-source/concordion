package io.granito.concordion.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class QuarkusConcordionProviderTest {
    private final QuarkusConcordionProvider provider =
        new QuarkusConcordionProvider();

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

    @ConcordionFixture
    private static class AnnotatedTest {
    }
}
