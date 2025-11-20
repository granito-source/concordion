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

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.util.Map;
import java.util.Random;

import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import jakarta.enterprise.inject.spi.CDI;
import org.concordion.api.SpecificationLocator;
import org.concordion.integration.junit.platform.engine.BaseConcordionTestEngine;
import org.concordion.internal.runner.QuarkusConcordionRunner;
import org.junit.platform.engine.TestDescriptor;

/**
 * An implementation of
 * {@link org.junit.platform.engine.TestEngine TestEngine}
 * that supports running Concordion specifications with Quarkus
 * dependency injection. This engine is not intended to be used
 * directly, but rather from a proxy {@link QuarkusConcordionTestEngine}
 * instance.
 */
public class QuarkusTestEngine extends BaseConcordionTestEngine {
    private static final int MIN_PORT = 49152;

    private static final int MAX_PORT = 65500;

    private final StartupAction startupAction;

    private RunningQuarkusApplication runningApplication;

    /**
     * Creates a new instance of {@link QuarkusTestEngine}.
     *
     * @param startupAction the action to start the Quarkus application
     */
    public QuarkusTestEngine(StartupAction startupAction)
    {
        this.startupAction = startupAction;
        System.setProperty("concordion.runner.concordion",
            QuarkusConcordionRunner.class.getName());
    }

    /**
     * Prevents the use of this engine directly.
     *
     * @return never
     * @throws UnsupportedOperationException always
     */
    @Override
    public String getId()
    {
        throw new UnsupportedOperationException(
            "this engine cannot be used directly");
    }

    /**
     * Adjusts the given class to be loaded in the Quarkus application
     * class loader.
     *
     * @param clazz the class to adjust
     * @return the adjusted class
     */
    @Override
    protected Class<?> adjustClass(Class<?> clazz)
    {
        try {
            return getClass().getClassLoader().loadClass(clazz.getName());
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Appends a test descriptor for the given fixture class after
     * ensuring that the Quarkus application is running.
     *
     * @param parent the parent test descriptor
     * @param fixture the fixture class
     * @param locator the specification locator
     */
    @Override
    protected void append(TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        ensureRunning();
        super.append(parent, fixture, locator);
    }

    /**
     * Checks if the given class is annotated as a Concordion fixture
     * with Quarkus integration.
     *
     * @param clazz the class to check
     * @return {@code true} if annotated as a Concordion fixture,
     * {@code false} otherwise
     */
    @Override
    protected boolean annotatedAsFixture(Class<?> clazz)
    {
        return findAnnotation(clazz, ConcordionFixture.class).isPresent();
    }

    /**
     * Creates a fixture object using Quarkus dependency injection.
     *
     * @param clazz the fixture class
     * @return the created fixture object
     */
    @Override
    protected Object createFixtureObject(Class<?> clazz)
    {
        return CDI.current().select(clazz).get();
    }

    private synchronized void ensureRunning()
    {
        if (runningApplication == null) {
            if (startupAction == null)
                throw new IllegalStateException("no startup action");

            try {
                runningApplication = runApplication(startupAction);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private RunningQuarkusApplication runApplication(StartupAction action)
        throws Exception
    {
        var port = new Random().nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;

        action.overrideConfig(
            Map.of("quarkus.http.test-port", String.valueOf(port)));
        configureRestAssured(action.getClassLoader(), port);

        return action.run();
    }

    private void configureRestAssured(ClassLoader classLoader, int port)
    {
        try {
            classLoader
                .loadClass("io.restassured.RestAssured")
                .getField("port")
                .setInt(null, port);
        } catch (Exception ex) {
            // this is the best effort, if not done ignore
        }
    }
}
