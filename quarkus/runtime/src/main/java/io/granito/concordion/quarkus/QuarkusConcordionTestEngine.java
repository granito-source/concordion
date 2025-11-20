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

import java.nio.file.Paths;
import java.util.ServiceLoader;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.common.PathTestHelper;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

/**
 * An implementation of {@link TestEngine} that supports running
 * Concordion specifications with Quarkus dependency injection. This
 * implementation is a proxy that delegates to the actual test engine
 * loaded in the Quarkus application class loader.
 */
public class QuarkusConcordionTestEngine implements TestEngine {
    /** The test engine's ID */
    public static final String ENGINE_ID = "concordion-quarkus";

    private static StartupAction startupAction;

    private static TestEngine testEngine;

    /**
     * Return the ID of this test engine.
     *
     * @return the test engine ID, see {@link #ENGINE_ID}
     */
    @Override
    public String getId()
    {
        return ENGINE_ID;
    }

    /**
     * Discover tests based on the given discovery request. When running
     * outside of Quarkus, a minimal root test descriptor is returned.
     * When running under Quarkus, the actual test engine is loaded and
     * used to perform test discovery.
     *
     * @param request the engine discovery request
     * @param id the unique ID for the root test descriptor
     * @return the root test descriptor
     */
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request,
        UniqueId id)
    {
        ensureTestEngine(request);

        return testEngine != null ? testEngine.discover(request, id) :
            QuarkusTestEngine.createRoot(id);
    }

    /**
     * Execute tests based on the given execution request. If the
     * actual test engine is not available, no tests are executed.
     *
     * @param request the execution request
     */
    @Override
    public void execute(ExecutionRequest request)
    {
        if (testEngine != null)
            testEngine.execute(request);
    }

    /**
     * Load service providers using the given class loader. This method
     * can be overridden to facilitate unit testing.
     *
     * @param <T> the service type
     * @param service the service class
     * @param classLoader the class loader
     * @return the service loader
     */
    protected <T> ServiceLoader<T> serviceLoaderLoad(Class<T> service,
        ClassLoader classLoader)
    {
        return ServiceLoader.load(service, classLoader);
    }

    /**
     * Create a new instance of {@link QuarkusTestEngine} using the
     * given startup action. This method can be overridden to
     * facilitate unit testing.
     *
     * @param startupAction the Quarkus startup action
     * @return a new instance of {@code QuarkusTestEngine}
     */
    protected TestEngine newQuarkusTestEngine(StartupAction startupAction)
    {
        return new QuarkusTestEngine(startupAction);
    }

    private TestEngine reloadTestEngine()
    {
        var classLoader = startupAction.getClassLoader();

        try {
            var engineClass = classLoader
                .loadClass(TestEngine.class.getName());

            for (var provider: serviceLoaderLoad(engineClass, classLoader)) {
                var engine = (TestEngine)provider;

                if (ENGINE_ID.equals(engine.getId())) {
                    var field = engine
                        .getClass()
                        .getDeclaredField("startupAction");

                    field.setAccessible(true);
                    field.set(null, startupAction);
                    field.setAccessible(false);

                    return engine;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                "unable to load TestEngine class", ex);
        }

        throw new IllegalStateException("no TestEngine with ID " +
            ENGINE_ID);
    }

    private void ensureTestEngine(EngineDiscoveryRequest request)
    {
        synchronized (QuarkusConcordionTestEngine.class) {
            if (testEngine != null)
                return;

            var runningUnderQuarkus = getClass()
                .getClassLoader()
                .getClass()
                .getName()
                .contains("Quarkus");

            if (runningUnderQuarkus) {
                testEngine = newQuarkusTestEngine(startupAction);

                return;
            }

            QuarkusTestEngine.fixtureStream(request)
                .findAny()
                .map(this::bootstrap)
                .ifPresent(action -> {
                    startupAction = action;
                    testEngine = reloadTestEngine();
                });
        }
    }

    private StartupAction bootstrap(Class<?> fixture)
    {
        var testLocation = PathTestHelper.getTestClassesLocation(fixture);
        var appLocation = PathTestHelper
            .getAppClassLocationForTestLocation(testLocation);
        var applicationRoot = PathsCollection.builder()
            .add(appLocation)
            .add(testLocation)
            .build();
        var bootstrap = QuarkusBootstrap.builder()
            .setFlatClassPath(true)
            .addParentFirstArtifact(ArtifactKey.ga("org.junit.platform",
                "junit-platform-engine"))
            .addParentFirstArtifact(ArtifactKey.ga("org.junit.platform",
                "junit-platform-commons"))
            .addParentFirstArtifact(ArtifactKey.ga("org.opentest4j",
                "opentest4j"))
            .addParentFirstArtifact(ArtifactKey.ga("org.apiguardian",
                "apiguardian-api"))
            .addParentFirstArtifact(ArtifactKey.ga("org.jspecify",
                "jspecify"))
            .setMode(QuarkusBootstrap.Mode.TEST)
            .setProjectRoot(Paths.get("").normalize().toAbsolutePath())
            .setApplicationRoot(applicationRoot)
            .build();

        try {
            return bootstrap
                .bootstrap()
                .createAugmentor()
                .createInitialRuntimeApplication();
        } catch (BootstrapException ex) {
            throw new RuntimeException(ex);
        }
    }
}
