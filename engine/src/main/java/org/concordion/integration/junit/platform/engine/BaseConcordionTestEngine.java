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

package org.concordion.integration.junit.platform.engine;

import static java.util.stream.Stream.concat;
import static org.junit.platform.commons.support.ReflectionSupport.streamAllClassesInPackage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.concordion.api.SpecificationLocator;
import org.concordion.internal.ClassNameBasedSpecificationLocator;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

/**
 * A common foundation for Concordion test engines. The implementation
 * must define how to determine if a class is a Concordion fixture and
 * how to create a fixture object from such class.
 */
public abstract class BaseConcordionTestEngine extends
    HierarchicalTestEngine<ConcordionEngineExecutionContext> {
    private static final String FIXTURE_PATTERN = ".*(Fixture|Test)$";

    /**
     * Return a stream of fixture candidates classes from an
     * engine discovery request based on their simple names only.
     *
     * @param request the discovery request
     * @return a stream of fixture candidate classes
     */
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

        return concat(byClass, byPackage)
            .filter(clazz -> clazz.getName().matches(FIXTURE_PATTERN));
    }

    /**
     * Create a root test descriptor for the provided uniqueID.
     *
     * @param id the unique ID
     * @return a new test descriptor, an instance of
     * {@link ConcordionEngineDescriptor}
     */
    public static TestDescriptor createRoot(UniqueId id)
    {
        return new ConcordionEngineDescriptor(id,
            "Concordion for JUnit Platform");
    }

    private final Map<Class<?>, SpecificationDescriptor> cache =
        new HashMap<>();

    /**
     * Discover tests according to the supplied
     * {@linkplain EngineDiscoveryRequest discovery request}.
     * This implementation supports
     * {@linkplain ClassSelector class selectors} and
     * {@linkplain PackageSelector package selectors}.
     *
     * @param request the discovery request
     * @param id the unique ID to be used for this test engine's
     * {@code TestDescriptor}
     * @return the root {@code TestDescriptor} of this engine, an
     * instance of {@link ConcordionEngineDescriptor}
     */
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request,
        UniqueId id)
    {
        var root = createRoot(id);
        var locator = new ClassNameBasedSpecificationLocator();

        fixtureStream(request)
            .map(this::adjustClass)
            .filter(this::annotatedAsFixture)
            .forEach(fixture -> append(root, fixture, locator));

        return root;
    }

    /**
     * Create the initial execution context for executing the supplied
     * {@linkplain ExecutionRequest request}.
     *
     * @param request the request about to be executed
     * @return the initial context that will be passed to nodes in
     * the hierarchy
     */
    @Override
    protected ConcordionEngineExecutionContext createExecutionContext(
        ExecutionRequest request)
    {
        return new ConcordionEngineExecutionContext(request);
    }

    /**
     * Potentially adjust the class of the fixture, for example,
     * it can use a different class loader if needed. The default
     * implementation does nothing to the class. Override to change
     * this behavior.
     *
     * @param clazz the fixture class
     * @return the adjusted class
     */
    protected Class<?> adjustClass(Class<?> clazz)
    {
        return clazz;
    }

    /**
     * Append the
     * {@linkplain SpecificationDescriptor specification descriptor} with
     * all contained {@linkplain ExampleDescriptor example descriptors}
     * to the parent {@linkplain TestDescriptor test descriptor}.
     *
     * @param parent the parent test descriptor
     * @param fixture the fixture class
     * @param locator the specification locator
     */
    protected void append(TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        var spec = appendSpec(parent, fixture, locator);

        try {
            for (var example: spec.getExampleNames())
                spec.addChild(exampleDescriptor(spec.getUniqueId(),
                    fixture, example));
        } catch (IOException ex) {
            throw new RuntimeException(
                "error loading specification examples (with [" +
                    fixture.getName() + "] fixture)", ex);
        }
    }

    /**
     * Append a {@linkplain SpecificationDescriptor specification descriptor}
     * to the parent {@linkplain TestDescriptor test descriptor}.
     * The implementation first tries to locate the specification
     * description in the cache, and if not found, creates a new one.
     *
     * @param parent the parent test descriptor
     * @param fixture the fixture class
     * @param locator the locator of the specification
     * @return the appended specification descriptor
     */
    protected synchronized SpecificationDescriptor appendSpec(
        TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        var spec = cache.get(fixture);

        if (spec == null) {
            spec = specificationDescriptor(parent.getUniqueId(), fixture,
                locator);
            cache.put(fixture, spec);
        }

        parent.addChild(spec);

        return spec;
    }

    /**
     * Create a new
     * {@linkplain SpecificationDescriptor specification descriptor}.
     *
     * @param parentId the parent ID
     * @param fixture the fixture class
     * @param locator the locator of the specification
     * @return a new specification descriptor
     */
    protected SpecificationDescriptor specificationDescriptor(
        UniqueId parentId, Class<?> fixture, SpecificationLocator locator)
    {
        return new SpecificationDescriptor(parentId, fixture, locator) {
            @Override
            protected Object createFixtureObject()
            {
                return BaseConcordionTestEngine.this
                    .createFixtureObject(getFixtureClass());
            }
        };
    }

    /**
     * Create a new {@linkplain ExampleDescriptor example descriptor}.
     *
     * @param parentId the parent ID
     * @param fixture the fixture class
     * @param example the name of the example
     * @return a new example descriptor
     */
    protected ExampleDescriptor exampleDescriptor(UniqueId parentId,
        Class<?> fixture, String example)
    {
        var id = parentId.append(ExampleDescriptor.SEGMENT_TYPE, example);

        return new ExampleDescriptor(id, fixture, example);
    }

    /**
     * Check if the class is annotated as a Concordion fixture.
     *
     * @param clazz the class to check
     * @return {@code true} when the given class is a Concordion fixture,
     * {@code false} otherwise
     */
    protected abstract boolean annotatedAsFixture(Class<?> clazz);

    /**
     * Create a fixture object instance given the required fixture class.
     *
     * @param clazz the class of the fixture
     * @return a fixture instance
     */
    protected abstract Object createFixtureObject(Class<?> clazz);
}
