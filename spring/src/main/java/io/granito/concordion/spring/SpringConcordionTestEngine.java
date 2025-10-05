package io.granito.concordion.spring;

import org.concordion.integration.junit.platform.engine.SpringTestEngine;

public class SpringConcordionTestEngine extends SpringTestEngine {
    public static final String ENGINE_ID = "concordion-spring";

    @Override
    public String getId()
    {
        return ENGINE_ID;
    }
}
