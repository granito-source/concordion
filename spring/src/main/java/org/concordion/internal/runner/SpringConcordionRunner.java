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

import io.granito.concordion.spring.SpringConcordionTestEngine;

/**
 * A {@linkplain org.concordion.api.Runner run-command runner}
 * implementation that uses JUnit Platform engines
 * (e.g. {@code "concordion-spring"}, "{@code "junit-vintage"}) to run
 * the fixture class' specification/examples.
 */
public class SpringConcordionRunner extends DefaultConcordionRunner {
    /**
     * Determine the test engine ID to use to run the specification.
     *
     * @param fixtureClass the class of the fixture
     * @return {@code "junit-vintage"} when the fixture is annotated with
     * {@link org.junit.runner.RunWith @RunWith} annotation or
     * {@code "concordion-spring"} otherwise.
     */
    @Override
    String resolveEngineId(Class<?> fixtureClass)
    {
        return hasRunWithConcordionRunner(fixtureClass) ?
            "junit-vintage" : SpringConcordionTestEngine.ENGINE_ID;
    }
}
