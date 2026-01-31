package com.vaadin.hilla;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an endpoint exposed to the frontend. An optional value can
 * be provided to customize the endpoint name used in the generated TypeScript
 * client.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Endpoint {
    /**
     * The custom name for the endpoint. If empty, the class name is used.
     *
     * @return the endpoint name
     */
    String value() default "";
}
