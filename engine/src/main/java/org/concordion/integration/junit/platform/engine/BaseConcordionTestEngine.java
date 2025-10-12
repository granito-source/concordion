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

public abstract class BaseConcordionTestEngine extends
    HierarchicalTestEngine<ConcordionEngineExecutionContext> {
    private static final String FIXTURE_PATTERN = ".*(Fixture|Test)$";

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

    public static TestDescriptor createRoot(UniqueId id)
    {
        return new ConcordionEngineDescriptor(id,
            "Concordion for JUnit Platform");
    }

    private final Map<Class<?>, SpecificationDescriptor> cache =
        new HashMap<>();

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

    @Override
    protected ConcordionEngineExecutionContext createExecutionContext(
        ExecutionRequest request)
    {
        return new ConcordionEngineExecutionContext(request);
    }

    protected Class<?> adjustClass(Class<?> clazz)
    {
        return clazz;
    }

    protected void append(TestDescriptor parent, Class<?> fixture,
        SpecificationLocator locator)
    {
        var spec = appendSpec(parent, fixture, locator);

        try {
            for (var example: spec.getExampleNames())
                spec.addChild(exampleDescriptor(spec.getUniqueId(), fixture, example));
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
        var spec = cache.get(fixture);

        if (spec == null) {
            spec = specificationDescriptor(parent.getUniqueId(), fixture, locator);
            cache.put(fixture, spec);
        }

        parent.addChild(spec);

        return spec;
    }

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

    protected ExampleDescriptor exampleDescriptor(UniqueId parentId,
        Class<?> fixture, String example)
    {
        var id = parentId.append(ExampleDescriptor.SEGMENT_TYPE, example);

        return new ExampleDescriptor(id, fixture, example);
    }

    protected abstract boolean annotatedAsFixture(Class<?> clazz);

    protected abstract Object createFixtureObject(Class<?> clazz);
}
