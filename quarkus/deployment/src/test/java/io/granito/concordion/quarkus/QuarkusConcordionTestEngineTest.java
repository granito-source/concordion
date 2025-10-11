package io.granito.concordion.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;
import spec.quarkus.DemoFixture;
import spec.quarkus.PartialMatchesFixture;
import spec.quarkus.SpikeFixture;

class QuarkusConcordionTestEngineTest {
    private final EngineTestKit.Builder engine = EngineTestKit
        .engine("concordion-quarkus");

    @Test
    void loads()
    {
        var descriptor = engine.discover().getEngineDescriptor();

        assertThat(descriptor.getDisplayName())
            .isEqualTo("Concordion with Quarkus for JUnit Platform");
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
            .containsExactly("spec.quarkus.Demo");
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
                "spec.quarkus.Demo",
                "spec.quarkus.PartialMatches",
                "spec.quarkus.Spike"
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
