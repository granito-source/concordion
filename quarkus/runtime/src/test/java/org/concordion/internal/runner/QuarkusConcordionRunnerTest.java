/*
 * Copyright 2025 Alexei Yashkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.concordion.internal.runner;

import static org.assertj.core.api.Assertions.assertThat;

import io.granito.concordion.quarkus.ConcordionFixture;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

class QuarkusConcordionRunnerTest {
    private final QuarkusConcordionRunner runner =
        new QuarkusConcordionRunner();

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

    @ConcordionFixture
    private static class AnnotatedFixture {
    }

    @RunWith(ConcordionRunner.class)
    private static class LegacyFixture {
    }
}
