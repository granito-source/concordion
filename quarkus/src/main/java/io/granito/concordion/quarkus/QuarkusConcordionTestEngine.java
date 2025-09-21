package io.granito.concordion.quarkus;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.common.PathTestHelper;
import org.concordion.integration.junit.platform.engine.QuarkusTestEngine;
import org.concordion.internal.runner.QuarkusRunner;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

public class QuarkusConcordionTestEngine implements TestEngine {
    public static final String ENGINE_ID = "concordion-quarkus";

    private static final int MIN_PORT = 49152;

    private static final int MAX_PORT = 65500;

    private static TestEngine testEngine;

    public QuarkusConcordionTestEngine()
    {
        var underQuarkus = getClass()
            .getClassLoader()
            .getName()
            .contains("Quarkus");

        testEngine = underQuarkus ? createTestEngine() : loadTestEngine();
    }

    @Override
    public String getId()
    {
        return ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId id)
    {
        return testEngine.discover(request, id);
    }

    @Override
    public void execute(ExecutionRequest request)
    {
        testEngine.execute(request);
    }

    protected TestEngine newTestEngine()
    {
        return new QuarkusTestEngine();
    }

    protected <T> ServiceLoader<T> loadService(Class<T> clazz,
        ClassLoader classLoader)
    {
        return ServiceLoader.load(clazz, classLoader);
    }

    private TestEngine createTestEngine()
    {
        System.setProperty("concordion.runner.concordion",
            QuarkusRunner.class.getName());

        return newTestEngine();
    }

    private TestEngine loadTestEngine()
    {
        try {
            var application = runApplication(getClass());
            var classLoader = application.getClassLoader();
            var engineClass = classLoader
                .loadClass(TestEngine.class.getName());

            for (var provider: loadService(engineClass, classLoader)) {
                var engine = (TestEngine)provider;

                if (ENGINE_ID.equals(engine.getId()))
                    return engine;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        throw new IllegalStateException("no test engine for " + ENGINE_ID);
    }

    private RunningQuarkusApplication runApplication(Class<?> fixture)
        throws Exception
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
        var action = bootstrap
            .bootstrap()
            .createAugmentor()
            .createInitialRuntimeApplication();
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
