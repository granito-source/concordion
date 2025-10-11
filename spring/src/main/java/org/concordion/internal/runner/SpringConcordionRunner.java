package org.concordion.internal.runner;

import io.granito.concordion.spring.SpringConcordionTestEngine;

public class SpringConcordionRunner extends DefaultConcordionRunner {
    @Override
    String resolveEngineId(Class<?> fixtureClass)
    {
        return hasRunWithConcordionRunner(fixtureClass) ?
            "junit-vintage" : SpringConcordionTestEngine.ENGINE_ID;
    }
}
