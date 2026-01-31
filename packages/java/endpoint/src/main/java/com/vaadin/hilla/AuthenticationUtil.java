/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.hilla;

import java.util.function.Function;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for authentication related tasks.
 */
public class AuthenticationUtil {

    private static final String DEFAULT_ROLE_PREFIX = "ROLE_";

    /**
     * Gets the authenticated user from the Spring SecurityContextHolder.
     *
     * @return the authenticated user or {@code null}
     */
    public static Authentication getSecurityHolderAuthentication() {
        var authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return authentication;

    }

    /**
     * Gets a function for checking if the authenticated user from the Spring
     * SecurityContextHolder is in a given role. Given role is prefixed with
     * 'ROLE_' by default.
     *
     * @return a function for checking if the given user has the given role
     */
    public static Function<String, Boolean> getSecurityHolderRoleChecker() {
        return getSecurityHolderRoleChecker(DEFAULT_ROLE_PREFIX);
    }

    /**
     * Gets a function for checking if the authenticated user from the Spring
     * SecurityContextHolder is in a given role.
     *
     * @param rolePrefix
     *            Prefix for the given role.
     * @return a function for checking if the given user has the given role
     */
    public static Function<String, Boolean> getSecurityHolderRoleChecker(
            String rolePrefix) {
        var authentication = getSecurityHolderAuthentication();
        if (authentication == null) {
            return role -> false;
        }

        return role -> {
            var roleWithPrefix = (rolePrefix != null && role != null
                    && !role.startsWith(rolePrefix)) ? rolePrefix + role : role;
            return authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority
                            .getAuthority().equals(roleWithPrefix));
        };
    }

}
