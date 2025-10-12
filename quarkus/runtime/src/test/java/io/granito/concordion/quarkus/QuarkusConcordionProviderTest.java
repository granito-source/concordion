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
