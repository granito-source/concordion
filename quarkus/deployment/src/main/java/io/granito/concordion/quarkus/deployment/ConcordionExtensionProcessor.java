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
