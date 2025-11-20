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

package io.granito.concordion.spring;

import org.concordion.integration.TestFrameworkProvider;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * An implementation of {@link TestFrameworkProvider} with Spring
 * dependency injection. It recognizes Concordion fixtures by
 * the presence of {@link ConcordionFixture} annotation.
 */
public class SpringConcordionProvider implements TestFrameworkProvider {
    /**
     * Check if the class is an instance of Concordion fixture with
     * Spring dependency injection.
     *
     * @param clazz the class to check
     * @return {@code true} if Concordion fixture, {@code false} otherwise
     */
    @Override
    public boolean isConcordionFixture(Class<?> clazz)
    {
        return AnnotationSupport
            .findAnnotation(clazz, ConcordionFixture.class)
            .isPresent();
    }
}
