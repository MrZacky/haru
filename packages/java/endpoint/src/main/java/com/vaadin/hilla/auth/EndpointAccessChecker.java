/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.hilla.auth;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.function.Function;

import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Component used for checking role-based ACL in Vaadin Endpoints.
 * <p>
 * For each request that is trying to access the method in the corresponding
 * Vaadin Endpoint, the permission check is carried on.
 * <p>
 * It looks for {@link AnonymousAllowed} {@link PermitAll}, {@link DenyAll} and
 * {@link RolesAllowed} annotations in endpoint methods and classes containing
 * these methods (no super classes' annotations are taken into account).
 * <p>
 * Method-level annotation override Class-level ones.
 */
public class EndpointAccessChecker {

    public static final String ACCESS_DENIED_MSG = "Access denied";

    public static final String ACCESS_DENIED_MSG_DEV_MODE = "Access denied to endpoint; "
            + "to enable endpoint access use one of the following annotations: @AnonymousAllowed, @PermitAll, @RolesAllowed";

    private final boolean devMode;

    /**
     * Creates a new instance.
     *
     * @param devMode
     *            whether the application is running in development mode
     */
    public EndpointAccessChecker(boolean devMode) {
        this.devMode = devMode;
    }

    /**
     * Check that the endpoint is accessible for the current user.
     *
     * @param method
     *            the endpoint method to check ACL
     * @param request
     *            the request that triggers the method invocation
     * @return an error String with an issue description, if any validation
     *         issues occur, {@code null} otherwise
     */
    public String check(Method method, HttpServletRequest request) {
        return check(method, request.getUserPrincipal(), request::isUserInRole);
    }

    /**
     * Check that the endpoint is accessible for the current user.
     *
     * @param clazz
     *            the endpoint class to check ACL
     * @param request
     *            the request that triggers the method invocation
     * @return an error String with an issue description, if any validation
     *         issues occur, {@code null} otherwise
     */
    public String check(Class<?> clazz, HttpServletRequest request) {
        return check(clazz, request.getUserPrincipal(), request::isUserInRole);
    }

    /**
     * Check that the endpoint method is accessible for the current user.
     *
     * @param method
     *            the endpoint method to check ACL
     * @param principal
     *            the user principal object
     * @param rolesChecker
     *            a function for checking if a user is in a given role
     * @return an error String with an issue description, if any validation
     *         issues occur, {@code null} otherwise
     */
    public String check(Method method, Principal principal,
            Function<String, Boolean> rolesChecker) {
        if (hasAccess(method, principal, rolesChecker)) {
            return null;
        }
        return devMode ? ACCESS_DENIED_MSG_DEV_MODE : ACCESS_DENIED_MSG;
    }

    /**
     * Check that the endpoint class is accessible for the current user.
     *
     * @param clazz
     *            the endpoint class to check ACL
     * @param principal
     *            the user principal object
     * @param rolesChecker
     *            a function for checking if a user is in a given role
     * @return an error String with an issue description, if any validation
     *         issues occur, {@code null} otherwise
     */
    public String check(Class<?> clazz, Principal principal,
            Function<String, Boolean> rolesChecker) {
        if (hasAccess(clazz, principal, rolesChecker)) {
            return null;
        }
        return devMode ? ACCESS_DENIED_MSG_DEV_MODE : ACCESS_DENIED_MSG;
    }

    /**
     * Checks if the given method is accessible for the given principal.
     * Method-level annotations override class-level ones.
     *
     * @param method
     *            the method to check
     * @param principal
     *            the user principal, or null for anonymous
     * @param rolesChecker
     *            a function for checking if a user is in a given role
     * @return true if access is allowed
     */
    public boolean hasAccess(Method method, Principal principal,
            Function<String, Boolean> rolesChecker) {
        // Method-level annotations take precedence
        if (hasSecurityAnnotation(method)) {
            return checkAccess(method, principal, rolesChecker);
        }
        // Fall back to class-level annotations
        return hasAccess(method.getDeclaringClass(), principal, rolesChecker);
    }

    /**
     * Checks if the given class is accessible for the given principal.
     *
     * @param clazz
     *            the class to check
     * @param principal
     *            the user principal, or null for anonymous
     * @param rolesChecker
     *            a function for checking if a user is in a given role
     * @return true if access is allowed
     */
    public boolean hasAccess(Class<?> clazz, Principal principal,
            Function<String, Boolean> rolesChecker) {
        if (hasSecurityAnnotation(clazz)) {
            return checkAccess(clazz, principal, rolesChecker);
        }
        // No annotation means deny by default for endpoints
        return false;
    }

    private boolean checkAccess(AnnotatedElement element, Principal principal,
            Function<String, Boolean> rolesChecker) {
        if (element.isAnnotationPresent(DenyAll.class)) {
            return false;
        }
        if (element.isAnnotationPresent(AnonymousAllowed.class)) {
            return true;
        }
        if (principal == null) {
            return false;
        }
        if (element.isAnnotationPresent(PermitAll.class)) {
            return true;
        }
        RolesAllowed rolesAllowed = element
                .getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            for (String role : rolesAllowed.value()) {
                if (rolesChecker.apply(role)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean hasSecurityAnnotation(AnnotatedElement element) {
        return element.isAnnotationPresent(AnonymousAllowed.class)
                || element.isAnnotationPresent(PermitAll.class)
                || element.isAnnotationPresent(DenyAll.class)
                || element.isAnnotationPresent(RolesAllowed.class);
    }

}
