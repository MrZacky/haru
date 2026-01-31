package com.vaadin.hilla;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a superclass or interface whose public methods should be included in
 * the generated TypeScript client when the class is part of an endpoint's type
 * hierarchy. This annotation must not be combined with access control
 * annotations ({@code @PermitAll}, {@code @RolesAllowed}, {@code @DenyAll},
 * {@code @AnonymousAllowed}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EndpointExposed {
}
