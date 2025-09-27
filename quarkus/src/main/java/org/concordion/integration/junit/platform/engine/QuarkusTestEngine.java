package org.concordion.integration.junit.platform.engine;

import static java.util.stream.Stream.concat;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.ReflectionSupport.streamAllClassesInPackage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.granito.concordion.api.QuarkusConcordionFixture;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import org.concordion.api.SpecificationLocator;
import org.concordion.internal.ClassNameBasedSpecificationLocator;
import org.concordion.internal.runner.QuarkusRunner;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

public class QuarkusTestEngine extends
    HierarchicalTestEngine<ConcordionEngineExecutionContext> {
    private static final int MIN_PORT = 49152;

    private static final int MAX_PORT = 65500;

    private static final String ENDS_WITH_FIXTURE_OR_TEST_REGEX =
        ".*(Fixture|Test)$";

    private static final Predicate<Class<?>> IS_FIXTURE_CLASS = clazz ->
        findAnnotation(clazz, QuarkusConcordionFixture.class)
            .isPresent() &&
            clazz.getName().matches(ENDS_WITH_FIXTURE_OR_TEST_REGEX);

    public static Stream<Class<?>> fixtureStream(EngineDiscoveryRequest request)
    {
        var byClass = request.getSelectorsByType(ClassSelector.class)
            .stream()
            .map(ClassSelector::getJavaClass);
        var byPackage = request.getSelectorsByType(PackageSelector.class)
            .stream()
            .flatMap(selector -> streamAllClassesInPackage(
                selector.getPackageName(), clazz -> true,
                className -> true));

        return concat(byClass, byPackage);
    }

    private final Map<Class<?>, SpecificationDescriptor> cache =
        new HashMap<>();

    private final StartupAction startupAction;

    private RunningQuarkusApplication runningApplication;

    public QuarkusTestEngine(StartupAction startupAction)
    {
        this.startupAction = startupAction;
        System.setProperty("concordion.runner.concordion",
            QuarkusRunner.class.getName());
    }

    @Override
    public String getId()
    {
        throw new UnsupportedOperationException(
            "this engine cannot be used directly");
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId id)
    {
        var root = new ConcordionEngineDescriptor(id,
            "Concordion with Quarkus for JUnit Platform");
        var locator = new ClassNameBasedSpecificationLocator();

        fixtureStream(request)
            .map(this::ensureClassLoader)
            .filter(IS_FIXTURE_CLASS)
            .forEach(fixture -> append(root, fixture, locator));

        return root;
    }

    @Override
    protected ConcordionEngineExecutionContext createExecutionContext(
        ExecutionRequest request)
    {
        return new ConcordionEngineExecutionContext(request);
    }

    protected void append(TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        ensureRunning();

        var descriptor = appendSpec(parent, fixture, locator);

        try {
            for (var example: descriptor.getExampleNames())
                appendExample(descriptor, fixture, example);
        } catch (IOException ex) {
            throw new RuntimeException(
                "Error occurred while loading specification examples (with [" +
                    fixture.getName() + "] fixture)", ex);
        }
    }

    protected synchronized SpecificationDescriptor appendSpec(
        TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        var descriptor = cache.get(fixture);

        if (descriptor == null) {
            descriptor = new QuarkusSpecificationDescriptor(
                parent.getUniqueId(), fixture, locator);
            cache.put(fixture, descriptor);
        }

        parent.addChild(descriptor);

        return descriptor;
    }

    protected void appendExample(SpecificationDescriptor parent,
        Class<?> fixture, String example)
    {
        parent.addChild(new ExampleDescriptor(
            parent.getUniqueId()
                .append(ExampleDescriptor.SEGMENT_TYPE, example),
            fixture, example));
    }

    private Class<?> ensureClassLoader(Class<?> clazz)
    {
        var classLoader = getClass().getClassLoader();

        if (clazz.getClassLoader() == classLoader)
            return clazz;

        try {
            return classLoader.loadClass(clazz.getName());
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
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
