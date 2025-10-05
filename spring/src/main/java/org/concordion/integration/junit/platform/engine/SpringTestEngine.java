package org.concordion.integration.junit.platform.engine;

import static java.util.stream.Stream.concat;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.ReflectionSupport.streamAllClassesInPackage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.granito.concordion.spring.SpringConcordionFixture;
import org.concordion.api.SpecificationLocator;
import org.concordion.internal.ClassNameBasedSpecificationLocator;
import org.concordion.internal.runner.SpringRunner;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

public abstract class SpringTestEngine extends
    HierarchicalTestEngine<ConcordionEngineExecutionContext> {
    private static final String ENDS_WITH_FIXTURE_OR_TEST_REGEX =
        ".*(Fixture|Test)$";

    private static final Predicate<Class<?>> IS_FIXTURE_CLASS = clazz ->
        findAnnotation(clazz, SpringConcordionFixture.class)
            .isPresent() &&
            clazz.getName().matches(ENDS_WITH_FIXTURE_OR_TEST_REGEX);

    public static Stream<Class<?>> fixtureStream(
        EngineDiscoveryRequest request)
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

    public static TestDescriptor createRoot(UniqueId id)
    {
        return new ConcordionEngineDescriptor(id,
            "Concordion with Quarkus for JUnit Platform");
    }

    private final Map<Class<?>, SpecificationDescriptor> cache =
        new HashMap<>();

    public SpringTestEngine()
    {
        System.setProperty("concordion.runner.concordion",
            SpringRunner.class.getName());
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request,
        UniqueId id)
    {
        var root = createRoot(id);
        var locator = new ClassNameBasedSpecificationLocator();

        fixtureStream(request)
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
        var descriptor = appendSpec(parent, fixture, locator);

        try {
            for (var example: descriptor.getExampleNames())
                appendExample(descriptor, fixture, example);
        } catch (IOException ex) {
            throw new RuntimeException(
                "error loading specification examples (with [" +
                    fixture.getName() + "] fixture)", ex);
        }
    }

    protected synchronized SpecificationDescriptor appendSpec(
        TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        var descriptor = cache.get(fixture);

        if (descriptor == null) {
            descriptor = new SpringSpecificationDescriptor(
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
}
