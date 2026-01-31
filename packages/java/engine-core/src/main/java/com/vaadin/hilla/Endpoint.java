package com.vaadin.hilla;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Marks a class as an endpoint whose public methods are exposed to the
 * frontend. Each annotated class automatically becomes a Spring
 * {@link Component} bean.
 * <p>
 * This is an alias for {@link BrowserCallable}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Endpoint {
    /**
     * The name of an endpoint to use. If nothing is specified, the name of the
     * annotated class is taken.
     * <p>
     * Note: custom names are not allowed to be blank, be equal to any of the
     * ECMAScript reserved words or have whitespaces in them.
     *
     * @return the name of the endpoint to use in post requests
     */
    String value() default "";
}
