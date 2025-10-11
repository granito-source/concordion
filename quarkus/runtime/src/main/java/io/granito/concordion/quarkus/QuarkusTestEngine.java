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

public class QuarkusTestEngine extends BaseConcordionTestEngine {
    private static final int MIN_PORT = 49152;

    private static final int MAX_PORT = 65500;

    private final StartupAction startupAction;

    private RunningQuarkusApplication runningApplication;

    public QuarkusTestEngine(StartupAction startupAction)
    {
        this.startupAction = startupAction;
        System.setProperty("concordion.runner.concordion",
            QuarkusConcordionRunner.class.getName());
    }

    @Override
    public String getId()
    {
        throw new UnsupportedOperationException(
            "this engine cannot be used directly");
    }

    @Override
    protected Class<?> adjustClass(Class<?> clazz)
    {
        try {
            return getClass().getClassLoader().loadClass(clazz.getName());
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void append(TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        ensureRunning();
        super.append(parent, fixture, locator);
    }

    @Override
    protected boolean annotatedAsFixture(Class<?> clazz)
    {
        return findAnnotation(clazz, ConcordionFixture.class).isPresent();
    }

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
