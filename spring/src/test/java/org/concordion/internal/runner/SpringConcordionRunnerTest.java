package org.concordion.internal.runner;

import static org.assertj.core.api.Assertions.assertThat;

import io.granito.concordion.spring.ConcordionFixture;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

class SpringConcordionRunnerTest {
    private final SpringConcordionRunner runner =
        new SpringConcordionRunner();

    @Test
    void indicatesConcordionQuarkusEngineIdWhenNotAnnotatedFixture()
    {
        assertThat(runner.resolveEngineId(PlainFixture.class))
            .isEqualTo("concordion-spring");
    }

    @Test
    void indicatesConcordionQuarkusEngineIdWhenAnnotatedAsSuch()
    {
        assertThat(runner.resolveEngineId(AnnotatedFixture.class))
            .isEqualTo("concordion-spring");
    }

    @Test
    void indicatesJunitVintageEngineIdWhenAnnotatedWithRunWith()
    {
        assertThat(runner.resolveEngineId(LegacyFixture.class))
            .isEqualTo("junit-vintage");
    }

    private static class PlainFixture {
    }

    @ConcordionFixture
    private static class AnnotatedFixture {
    }

    @RunWith(ConcordionRunner.class)
    private static class LegacyFixture {
    }
}
