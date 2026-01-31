package com.vaadin.flow.server.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint or endpoint method as accessible without authentication.
 * This is an access control annotation that can be applied at the class level
 * (applies to all methods) or at the method level.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AnonymousAllowed {
}
