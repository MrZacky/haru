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
package com.vaadin.hilla.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.auth.AnonymousAllowed;

@SuppressWarnings("unused")
public class EndpointAccessCheckerTest {
    private static final String ROLE_USER = "ROLE_USER";

    private EndpointAccessChecker checker;
    private HttpServletRequest requestMock;

    @Before
    public void before() {
        checker = new EndpointAccessChecker(false);
        requestMock = mock(HttpServletRequest.class);
        when(requestMock.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(requestMock.isUserInRole("ROLE_USER")).thenReturn(true);
    }

    private void createAnonymousContext() {
        when(requestMock.getUserPrincipal()).thenReturn(null);
    }

    private void assertAccessGranted(Class<?> test, String methodName)
            throws Exception {
        Method method = test.getMethod(methodName);
        if (method.getDeclaringClass().equals(test)) {
            assertNull(checker.check(method, requestMock));
        } else {
            assertNull(checker.check(test, requestMock));
        }
    }

    private void assertAccessDenied(Class<?> test, String methodName)
            throws Exception {
        Method method = test.getMethod(methodName);
        if (method.getDeclaringClass().equals(test)) {
            assertNotNull(checker.check(method, requestMock));
        } else {
            assertNotNull(checker.check(test, requestMock));
        }
    }

    @Test
    public void should_Fail_When_NoAuthentication() throws Exception {
        class Test {
            public void test() {
            }
        }
        createAnonymousContext();
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_Fail_When_Authentication_And_matching_token()
            throws Exception {
        class Test {
            public void test() {
            }
        }
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_Pass_When_PermitAll() throws Exception {
        @PermitAll
        class Test {
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_Fail_When_DenyAllClass() throws Exception {
        @DenyAll
        class Test {
            public void test() {
            }
        }
        assertAccessDenied(Test.class, "test");
    }

    @Test()
    public void should_Pass_When_DenyAllClass_ValidRoleMethod()
            throws Exception {
        @DenyAll
        class Test {
            @RolesAllowed(ROLE_USER)
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test()
    public void should_Pass_When_DenyAllClass_PermitAllMethod()
            throws Exception {
        @DenyAll
        class Test {
            @PermitAll
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test()
    public void should_Fail_When_InvalidRoleClass() throws Exception {
        @RolesAllowed({ "ROLE_ADMIN" })
        class Test {
            public void test() {
            }
        }
        assertAccessDenied(Test.class, "test");
    }

    @Test()
    public void should_Pass_When_InvalidRoleClass_ValidRoleMethod()
            throws Exception {
        @RolesAllowed({ "ROLE_ADMIN" })
        class Test {
            @RolesAllowed(ROLE_USER)
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test()
    public void should_Pass_When_InvalidRoleClass_PermitAllMethod()
            throws Exception {
        @RolesAllowed({ "ROLE_ADMIN" })
        class Test {
            @PermitAll
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test()
    public void should_Pass_When_ValidRoleClass() throws Exception {
        @RolesAllowed(ROLE_USER)
        class Test {
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_AllowAnonymousAccess_When_ClassIsAnnotated()
            throws Exception {
        @AnonymousAllowed
        class Test {
            public void test() {
            }
        }

        createAnonymousContext();
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_AllowAnonymousAccess_When_MethodIsAnnotated()
            throws Exception {
        class Test {
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_NotAllowAnonymousAccess_When_NoAnnotationsPresent()
            throws Exception {
        class Test {
            public void test() {
            }
        }
        createAnonymousContext();
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_AllowAnyAuthenticatedAccess_When_PermitAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @PermitAll
            @AnonymousAllowed
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_AllowAnonymousAccess_When_PermitAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @PermitAll
            @AnonymousAllowed
            public void test() {
            }
        }

        createAnonymousContext();
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_AllowAnyAuthenticatedAccess_When_RolesAllowedAndAnonymousAllowed()
            throws Exception {
        class Test {
            @RolesAllowed("ADMIN")
            @AnonymousAllowed
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_AllowAnonymousAccess_When_RolesAllowedAndAnonymousAllowed()
            throws Exception {
        class Test {
            @RolesAllowed("ADMIN")
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_DisallowAnyAuthenticatedAccess_When_DenyAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @DenyAll
            @AnonymousAllowed
            public void test() {
            }
        }
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_DisallowNotMatchingRoleAccess_When_RolesAllowedAndPermitAll()
            throws Exception {
        class Test {
            @RolesAllowed("ADMIN")
            @PermitAll
            public void test() {
            }
        }
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_AllowSpecificRoleAccess_When_RolesAllowedAndPermitAll()
            throws Exception {
        class Test {
            @RolesAllowed(ROLE_USER)
            @PermitAll
            public void test() {
            }
        }
        assertAccessGranted(Test.class, "test");
    }

    @Test
    public void should_DisallowAnonymousAccess_When_DenyAllAndAnonymousAllowed()
            throws Exception {
        class Test {
            @DenyAll
            @AnonymousAllowed
            public void test() {
            }
        }
        createAnonymousContext();
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_DisallowAnonymousAccess_When_AnonymousAllowedIsOverriddenWithDenyAll()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @DenyAll
            public void test() {
            }
        }

        createAnonymousContext();
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_DisallowAnonymousAccess_When_AnonymousAllowedIsOverriddenWithRolesAllowed()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @RolesAllowed(ROLE_USER)
            public void test() {
            }
        }

        createAnonymousContext();
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_DisallowAnonymousAccess_When_AnonymousAllowedIsOverriddenWithPermitAll()
            throws Exception {
        @AnonymousAllowed
        class Test {
            @PermitAll
            public void test() {
            }
        }

        createAnonymousContext();
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void should_showHelpfulMessage_When_accessDeniedInDevMode()
            throws Exception {
        EndpointAccessChecker devChecker = new EndpointAccessChecker(true);
        class Test {
            public void test() {
            }
        }
        Method method = Test.class.getMethod("test");
        String accessDeniedMessage = devChecker.check(method, requestMock);
        assertEquals(EndpointAccessChecker.ACCESS_DENIED_MSG_DEV_MODE,
                accessDeniedMessage);
        assertTrue(accessDeniedMessage
                .contains(PermitAll.class.getSimpleName()));
        assertTrue(accessDeniedMessage
                .contains(RolesAllowed.class.getSimpleName()));
        assertTrue(accessDeniedMessage
                .contains(AnonymousAllowed.class.getSimpleName()));
    }

    @Test
    public void should_notShowHelpfulMessage_When_accessDeniedInProductionMode()
            throws Exception {
        EndpointAccessChecker prodChecker = new EndpointAccessChecker(false);
        class Test {
            public void test() {
            }
        }
        Method method = Test.class.getMethod("test");
        String accessDeniedMessage = prodChecker.check(method, requestMock);
        assertEquals(EndpointAccessChecker.ACCESS_DENIED_MSG,
                accessDeniedMessage);
    }

    @Test
    public void access_denied_for_inherited_method_when_Endpoint_is_not_annotated()
            throws Exception {

        class ParentEndpoint {
            public String sayHello() {
                return "Hello from ParentEndpoint";
            }
        }

        class Test extends ParentEndpoint {

            public void test() {
            }
        }

        assertAccessDenied(Test.class, "sayHello");
    }

    @Test
    public void access_granted_when_Endpoint_overrides_and_annotates_inherited_method()
            throws Exception {

        class ParentEndpoint {
            public String sayHello() {
                return "Hello from ParentEndpoint";
            }
        }

        class Test extends ParentEndpoint {

            @Override
            @AnonymousAllowed
            public String sayHello() {
                return "Hello from Test Endpoint";
            }

            public void test() {
            }
        }

        assertAccessGranted(Test.class, "sayHello");
        assertAccessDenied(Test.class, "test");
    }

    @Test
    public void access_granted_for_inherited_method_when_Endpoint_is_annotated()
            throws Exception {

        class ParentEndpoint {
            public String sayHello() {
                return "Hello from ParentEndpoint";
            }
        }

        @AnonymousAllowed
        class Test extends ParentEndpoint {

            public void test() {
            }
        }

        assertAccessGranted(Test.class, "sayHello");
        assertAccessGranted(Test.class, "test");
    }

}
