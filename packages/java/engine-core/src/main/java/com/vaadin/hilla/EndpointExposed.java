package com.vaadin.hilla;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marks the class itself should not be treated as an
 * {@link Endpoint}, but inheritable methods will be added to the descendant
 * Endpoints.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EndpointExposed {
}
