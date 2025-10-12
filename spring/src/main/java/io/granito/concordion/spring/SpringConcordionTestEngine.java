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

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import org.concordion.integration.junit.platform.engine.BaseConcordionTestEngine;
import org.concordion.internal.runner.SpringConcordionRunner;
import org.junit.platform.commons.support.ReflectionSupport;
import org.springframework.test.context.TestContextManager;

public class SpringConcordionTestEngine extends BaseConcordionTestEngine {
    public static final String ENGINE_ID = "concordion-spring";
    public SpringConcordionTestEngine()
    {
        System.setProperty("concordion.runner.concordion",
            SpringConcordionRunner.class.getName());
    }

    @Override
    public String getId()
    {
        return ENGINE_ID;
    }

    @Override
    protected boolean annotatedAsFixture(Class<?> clazz)
    {
        return findAnnotation(clazz, ConcordionFixture.class)
            .isPresent();
    }

    @Override
    protected Object createFixtureObject(Class<?> clazz)
    {
        var testManager = new TestContextManager(clazz);
        var object = ReflectionSupport.newInstance(clazz);

        try {
            testManager.prepareTestInstance(object);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return object;
    }
}
