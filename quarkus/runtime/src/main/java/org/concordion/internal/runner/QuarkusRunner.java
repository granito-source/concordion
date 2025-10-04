package org.concordion.internal.runner;

import io.granito.concordion.quarkus.QuarkusConcordionTestEngine;

public class QuarkusRunner extends DefaultConcordionRunner {
    @Override
    String resolveEngineId(Class<?> fixtureClass)
    {
        return hasRunWithConcordionRunner(fixtureClass) ?
            "junit-vintage" : QuarkusConcordionTestEngine.ENGINE_ID;
    }
}
