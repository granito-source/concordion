package io.granito.concordion.quarkus.deployment;

import io.granito.concordion.quarkus.ConcordionFixture;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.DotName;

public class ConcordionExtensionProcessor {
    private static final DotName FIXTURE_ANNOTATION =
        DotName.createSimple(ConcordionFixture.class);

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem("concordion-extension");
    }

    @BuildStep
    AutoAddScopeBuildItem autoAddScope()
    {
        return AutoAddScopeBuildItem.builder()
            .containsAnnotations(FIXTURE_ANNOTATION)
            .defaultScope(BuiltinScope.DEPENDENT)
            .build();
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBean()
    {
        return UnremovableBeanBuildItem
            .targetWithAnnotation(FIXTURE_ANNOTATION);
    }
}
