/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.hilla.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.vaadin.hilla.EndpointController;
import com.vaadin.hilla.EndpointControllerMockBuilder;

@RunWith(SpringRunner.class)
@WebMvcTest
@Import({ TestEndpoints.class, MyRestController.class })
public class EndpointWithRestControllerTest {

    private MockMvc mockMvcForEndpoint;

    @Autowired
    private MockMvc mockMvcForRest;

    @Autowired
    private ApplicationContext applicationContext;

    @Rule
    public TemporaryFolder projectFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        projectFolder.newFolder("build");

        EndpointControllerMockBuilder controllerMockBuilder = new EndpointControllerMockBuilder();
        EndpointController controller = controllerMockBuilder
                .withApplicationContext(applicationContext).build();
        controller.registerEndpoints();
        mockMvcForEndpoint = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    // https://github.com/vaadin/flow/issues/8010
    public void shouldNotExposePrivateAndProtectedFields_when_CallingFromRestAPIs()
            throws Exception {
        String result = mockMvcForRest
                .perform(
                        get("/api/get").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertEquals("{\"name\":\"Bond\"}", result);
    }

    @Test
    // https://github.com/vaadin/flow/issues/8034
    public void should_FollowJacksonPropertiesApproach_when_CallingFromHillaEndpoint() {
        try {
            String result = callEndpointMethod("getBeanWithPrivateFields");
            assertEquals("{\"name\":\"Bond\"}", result);
        } catch (Exception e) {
            fail("failed to serialize a bean with private fields");
        }
    }

    @Test
    // https://github.com/vaadin/flow/issues/8034
    public void should_BeAbleToSerializeABeanWithZonedDateTimeField() {
        try {
            String result = callEndpointMethod("getBeanWithZonedDateTimeField");
            assertNotNull(result);
            assertNotEquals("", result);
        } catch (Exception e) {
            fail("failed to serialize a bean with ZonedDateTime field");
        }
    }

    @Test
    // https://github.com/vaadin/flow/issues/8067
    public void should_RepsectJacksonAnnotation_when_serializeBean()
            throws Exception {
        String result = callEndpointMethod("getBeanWithJacksonAnnotation");
        // Jackson 3 uses alphabetical property ordering by default
        assertEquals("{\"bookId\":null,\"name\":null,\"rating\":2}", result);
    }

    @Test
    // https://github.com/vaadin/hilla/issues/396
    public void should_SerializeByteArrayIntoArrayOfNumbers() {
        try {
            String result = callEndpointMethod("getByteArray");
            assertNotNull(result);
            assertEquals("[1,2,3,4]", result);
            assertNotEquals("", result);
            assertNotEquals("AQIDBA==", result);
        } catch (Exception e) {
            fail("failed to serialize a byte[] field");
        }
    }

    @Test
    public void should_serializeLocalTimeInExpectedFormat_when_UsingSpringBoot()
            throws Exception {
        String result = callEndpointMethod("getLocalTime");
        assertEquals("\"08:00:00\"", result);
    }

    private String callEndpointMethod(String methodName) throws Exception {
        String endpointName = TestEndpoints.class.getSimpleName();
        String requestUrl = String.format("/%s/%s", endpointName, methodName);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post(requestUrl)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        return mockMvcForEndpoint.perform(requestBuilder).andReturn()
                .getResponse().getContentAsString();
    }

}
