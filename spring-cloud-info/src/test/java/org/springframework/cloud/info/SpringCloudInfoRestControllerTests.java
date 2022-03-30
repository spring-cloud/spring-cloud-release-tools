/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.info;

import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.info.SpringCloudInfoService.SpringCloudVersion;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.cloud.info.SpringCloudInfoTestData.springCloudVersions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ryan Baxter
 */
@WebMvcTest(SpringCloudInfoRestController.class)
@RunWith(SpringRunner.class)
public class SpringCloudInfoRestControllerTests {

	@Rule
	public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	@MockBean
	SpringCloudInfoService springCloudInfoService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation).uris().withHost("spring-cloud-info.cfapps.io")
						.withPort(80))
				.build();
	}

	@Test
	public void version() throws Exception {
		doReturn(new SpringCloudVersion("Greenwich.RELEASE")).when(springCloudInfoService)
				.getSpringCloudVersion(eq("2.1.1.RELEASE"));
		this.mockMvc
				.perform(get("/springcloudversion/springboot/{bootVersion}", "2.1.1.RELEASE")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("springcloudversion",
						pathParameters(parameterWithName("bootVersion").description("The Spring Boot version")),
						responseFields(fieldWithPath("version").description("Spring Cloud version"))));
	}

	@Test
	public void versions() throws Exception {
		doReturn(springCloudVersions.stream().map(v -> v.replaceFirst("v", "")).collect(Collectors.toList()))
				.when(springCloudInfoService).getSpringCloudVersions();
		this.mockMvc.perform(get("/springcloudversions").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(document("springcloudversions",
						responseFields(fieldWithPath("[]").description("An array versions"))));
	}

	@Test
	public void bomVersions() throws Exception {
		doReturn(SpringCloudInfoTestData.releaseVersions).when(springCloudInfoService)
				.getReleaseVersions(eq("Finchley.SR1"));
		this.mockMvc.perform(get("/bomversions/Finchley.SR1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(document("bomversions"));
	}

	@Test
	public void milestones() throws Exception {
		doReturn(SpringCloudInfoTestData.milestoneStrings.keySet()).when(springCloudInfoService).getMilestones();
		this.mockMvc.perform(get("/milestones").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(document("milestones"));
	}

	@Test
	public void milestoneDueDate() throws Exception {
		doReturn(new SpringCloudInfoService.Milestone("2019-07-31")).when(springCloudInfoService)
				.getMilestoneDueDate(eq("Hoxton.RELEASE"));
		this.mockMvc.perform(get("/milestones/{release}/duedate", "Hoxton.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("milestoneduedate",
						pathParameters(parameterWithName("release").description("The Spring Cloud release train name")),
						responseFields(fieldWithPath("dueDate").description("Spring Cloud milestone due date"))));
	}

}
