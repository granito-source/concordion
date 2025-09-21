package io.granito.concordion.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Stereotype;
import org.junit.platform.commons.annotation.Testable;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Testable
@Stereotype
@Dependent
public @interface QuarkusConcordionFixture {
}
