package io.granito.concordion.quarkus;

import java.nio.file.Paths;
import java.util.ServiceLoader;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.common.PathTestHelper;
import org.concordion.integration.junit.platform.engine.QuarkusTestEngine;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

public class QuarkusConcordionTestEngine implements TestEngine {
    public static final String ENGINE_ID = "concordion-quarkus";

    private static StartupAction startupAction;

    private static TestEngine testEngine;

    @Override
    public String getId()
    {
        return ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request,
        UniqueId id)
    {
        ensureTestEngine(request);

        return testEngine != null ? testEngine.discover(request, id) :
            QuarkusTestEngine.createRoot(id);
    }

    @Override
    public void execute(ExecutionRequest request)
    {
        if (testEngine != null)
            testEngine.execute(request);
    }

    protected <T> ServiceLoader<T> serviceLoaderLoad(Class<T> service,
        ClassLoader classLoader)
    {
        return ServiceLoader.load(service, classLoader);
    }

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
