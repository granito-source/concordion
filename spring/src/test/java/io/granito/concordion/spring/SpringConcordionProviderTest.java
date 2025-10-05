package io.granito.concordion.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SpringConcordionProviderTest {
    private final SpringConcordionProvider provider =
        new SpringConcordionProvider();

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

    @SpringConcordionFixture
    private static class AnnotatedTest {
    }
}
