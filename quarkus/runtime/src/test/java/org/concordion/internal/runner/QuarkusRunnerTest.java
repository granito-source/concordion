package org.concordion.internal.runner;

import static org.assertj.core.api.Assertions.assertThat;

import io.granito.concordion.quarkus.QuarkusConcordionFixture;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

class QuarkusRunnerTest {
    private final QuarkusRunner runner = new QuarkusRunner();

    @Test
    void indicatesConcordionQuarkusEngineIdWhenNotAnnotatedFixture()
    {
        assertThat(runner.resolveEngineId(PlainFixture.class))
            .isEqualTo("concordion-quarkus");
    }

    @Test
    void indicatesConcordionQuarkusEngineIdWhenAnnotatedAsSuch()
    {
        assertThat(runner.resolveEngineId(AnnotatedFixture.class))
            .isEqualTo("concordion-quarkus");
    }

    @Test
    void indicatesJunitVintageEngineIdWhenAnnotatedWithRunWith()
    {
        assertThat(runner.resolveEngineId(LegacyFixture.class))
            .isEqualTo("junit-vintage");
    }

    private static class PlainFixture {
    }

    @QuarkusConcordionFixture
    private static class AnnotatedFixture {
    }

    @RunWith(ConcordionRunner.class)
    private static class LegacyFixture {
    }
}
