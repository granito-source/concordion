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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;
import spec.spring.DemoFixture;
import spec.spring.PartialMatchesFixture;
import spec.spring.SpikeFixture;

class SpringConcordionTestEngineTest {
    private final EngineTestKit.Builder engine = EngineTestKit
        .engine("concordion-spring");

    @Test
    void loads()
    {
        var descriptor = engine.discover().getEngineDescriptor();

        assertThat(descriptor.getDisplayName())
            .isEqualTo("Concordion for JUnit Platform");
    }

    @Test
    void discoversFixturesByClasses()
    {
        var discoveryResults = engine
            .selectors(DiscoverySelectors.selectClass(DemoFixture.class))
            .discover();

        assertThat(discoveryResults.getDiscoveryIssues()).isEmpty();

        var descriptor = discoveryResults.getEngineDescriptor();

        assertThat(descriptor.isContainer()).isTrue();
        assertThat(descriptor.getChildren())
            .extracting(TestDescriptor::getDisplayName)
            .containsExactly("spec.spring.Demo");
    }

    @Test
    void discoversFixturesByPackages()
    {
        var discoveryResults = engine
            .selectors(DiscoverySelectors
                .selectPackage(DemoFixture.class.getPackageName()))
            .discover();

        assertThat(discoveryResults.getDiscoveryIssues()).isEmpty();

        var descriptor = discoveryResults.getEngineDescriptor();

        assertThat(descriptor.isContainer()).isTrue();
        assertThat(descriptor.getChildren())
            .extracting(TestDescriptor::getDisplayName)
            .containsExactlyInAnyOrder(
                "spec.spring.Demo",
                "spec.spring.PartialMatches",
                "spec.spring.Spike"
            );
    }

    @Test
    void runsConcordionSpecs()
    {
        var executionResults = engine
            .selectors(
                DiscoverySelectors.selectClass(DemoFixture.class),
                DiscoverySelectors.selectClass(PartialMatchesFixture.class),
                DiscoverySelectors.selectClass(SpikeFixture.class))
            .execute();

        executionResults
            .containerEvents()
            .assertStatistics(stats -> stats.started(1 + 3));
        executionResults
            .testEvents()
            .assertStatistics(stats -> stats.started(3).succeeded(3));
    }
}
