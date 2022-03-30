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

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Milestone;
import com.jcabi.github.Milestones;
import com.jcabi.github.Repo;
import com.jcabi.github.Repos;
import com.jcabi.http.Request;
import com.jcabi.http.RequestURI;
import com.jcabi.http.Response;
import com.jcabi.http.request.DefaultResponse;
import com.jcabi.http.response.JsonResponse;
import com.jcabi.immutable.Array;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.info.exceptions.SpringCloudVersionNotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.info.SpringCloudInfoTestData.milestoneStrings;
import static org.springframework.cloud.info.SpringCloudRelease.SPRING_CLOUD_RELEASE_TAGS_PATH;

/**
 * @author Ryan Baxter
 */
public class InitializrSpringCloudInfoServiceTests {

	@Test
	public void getSpringCloudVersionBomRangesMissingTest() {
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		when(rest.getForObject(anyString(), eq(Map.class))).thenReturn(new HashMap());
		InitializrSpringCloudInfoService service = new InitializrSpringCloudInfoService(rest, github, githubPomReader);
		try {
			service.getSpringCloudVersion("2.1.0");
			fail("Exception should have been thrown");
		}
		catch (SpringCloudVersionNotFoundException e) {
			assertThat(e.getCause().getMessage(), Matchers.startsWith("bom-ranges"));
		}
	}

	@Test
	public void getSpringCloudVersionSpringCloudMissingTest() {
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		Map<String, Map<String, String>> info = new HashMap<>();
		info.put("bom-ranges", new HashMap<>());
		when(rest.getForObject(anyString(), eq(Map.class))).thenReturn(info);
		InitializrSpringCloudInfoService service = new InitializrSpringCloudInfoService(rest, github, githubPomReader);
		try {
			service.getSpringCloudVersion("2.1.0");
			fail("Exception should have been thrown");
		}
		catch (SpringCloudVersionNotFoundException e) {
			assertThat(e.getCause().getMessage(), Matchers.startsWith("spring-cloud"));
		}
	}

	@Test
	public void getSpringCloudReleaseVersionTest() throws Exception {
		String bomVersion = "v2020.0.0-SNAPSHOT";
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		when(githubPomReader
				.readPomFromUrl(eq(String.format(SpringCloudRelease.SPRING_CLOUD_STARTER_PARENT_RAW, bomVersion))))
						.thenReturn(new MavenXpp3Reader().read(new FileReader(
								new ClassPathResource("spring-cloud-starter-parent-pom.xml").getFile())));
		when(githubPomReader.readPomFromUrl(
				eq(String.format(SpringCloudRelease.SPRING_CLOUD_RELEASE_DEPENDENCIES_RAW, bomVersion))))
						.thenReturn(new MavenXpp3Reader().read(
								new FileReader(new ClassPathResource("spring-cloud-dependencies-pom.xml").getFile())));
		InitializrSpringCloudInfoService service = spy(
				new InitializrSpringCloudInfoService(rest, github, githubPomReader));
		doReturn(Arrays.asList(new String[] { bomVersion })).when(service).getSpringCloudVersions();
		Map<String, String> releaseVersionsResult = service.getReleaseVersions(bomVersion);
		assertThat(releaseVersionsResult, Matchers.equalTo(SpringCloudInfoTestData.releaseVersions));
	}

	@Test(expected = SpringCloudVersionNotFoundException.class)
	public void getSpringCloudReleaseVersionNotFoundTest() throws Exception {
		String bomVersion = "vFooBar.BUILD-SNAPSHOT";
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		when(githubPomReader
				.readPomFromUrl(eq(String.format(SpringCloudRelease.SPRING_CLOUD_STARTER_PARENT_RAW, bomVersion))))
						.thenReturn(new MavenXpp3Reader().read(new FileReader(
								new ClassPathResource("spring-cloud-starter-parent-pom.xml").getFile())));
		when(githubPomReader.readPomFromUrl(
				eq(String.format(SpringCloudRelease.SPRING_CLOUD_RELEASE_DEPENDENCIES_RAW, bomVersion))))
						.thenReturn(new MavenXpp3Reader().read(
								new FileReader(new ClassPathResource("spring-cloud-dependencies-pom.xml").getFile())));
		InitializrSpringCloudInfoService service = spy(
				new InitializrSpringCloudInfoService(rest, github, githubPomReader));
		doReturn(new ArrayList()).when(service).getSpringCloudVersions();
		service.getReleaseVersions(bomVersion);
	}

	@Test
	public void getSpringCloudVersionsTest() throws Exception {
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		Response response = mock(Response.class);
		Request request = mock(Request.class);
		RequestURI requestURI = mock(RequestURI.class);
		JsonResponse jsonResponse = new JsonResponse(new DefaultResponse(request, 200, "", new Array<>(),
				IOUtils.toByteArray(new ClassPathResource("spring-cloud-versions.json").getInputStream())));
		doReturn(request).when(requestURI).back();
		doReturn(requestURI).when(requestURI).path(eq(SPRING_CLOUD_RELEASE_TAGS_PATH));
		doReturn(requestURI).when(request).uri();
		doReturn(jsonResponse).when(response).as(eq(JsonResponse.class));
		doReturn(response).when(request).fetch();
		doReturn(request).when(github).entry();
		InitializrSpringCloudInfoService service = spy(
				new InitializrSpringCloudInfoService(rest, github, githubPomReader));
		assertThat(service.getSpringCloudVersions(), Matchers.equalTo(SpringCloudInfoTestData.springCloudVersions
				.stream().map(v -> v.replaceFirst("v", "")).collect(Collectors.toList())));
	}

	@Test
	public void getMilestoneDueDateTest() throws Exception {
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		Repos repos = mock(Repos.class);
		Repo repo = mock(Repo.class);
		Milestones milestones = mock(Milestones.class);
		Iterable iterable = buildMilestonesIterable();
		doReturn(iterable).when(milestones).iterate(any(Map.class));
		doReturn(milestones).when(repo).milestones();
		doReturn(repo).when(repos).get(any(Coordinates.class));
		doReturn(repos).when(github).repos();
		InitializrSpringCloudInfoService service = spy(
				new InitializrSpringCloudInfoService(rest, github, githubPomReader));
		assertThat(service.getMilestoneDueDate("Finchley.SR4"),
				Matchers.equalTo(new SpringCloudInfoService.Milestone("No Due Date")));
		assertThat(service.getMilestoneDueDate("Hoxton.RELEASE"),
				Matchers.equalTo(new SpringCloudInfoService.Milestone("2019-07-31")));
	}

	@Test
	public void getMilestonesTest() throws Exception {
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		Repos repos = mock(Repos.class);
		Repo repo = mock(Repo.class);
		Milestones milestones = mock(Milestones.class);
		Iterable iterable = buildMilestonesIterable();
		doReturn(iterable).when(milestones).iterate(any(Map.class));
		doReturn(milestones).when(repo).milestones();
		doReturn(repo).when(repos).get(any(Coordinates.class));
		doReturn(repos).when(github).repos();
		InitializrSpringCloudInfoService service = spy(
				new InitializrSpringCloudInfoService(rest, github, githubPomReader));
		assertThat(service.getMilestones(), Matchers.equalTo(milestoneStrings.keySet()));

	}

	private Iterable<Milestone> buildMilestonesIterable() throws IOException {
		JsonBuilderFactory builderFactory = Json.createBuilderFactory(new HashMap<>());
		List<Milestone> milestonesList = new ArrayList<>();
		for (String m : milestoneStrings.keySet()) {
			Milestone milestone = mock(Milestone.class);
			String dueDate = milestoneStrings.get(m);
			JsonObjectBuilder builder = builderFactory.createObjectBuilder().add("title", m);
			if (dueDate == null) {
				builder.add("due_on", JsonValue.NULL);
			}
			else {
				builder.add("due_on", dueDate);
			}
			doReturn(builder.build()).when(milestone).json();
			milestonesList.add(milestone);
		}
		return new Iterable() {
			@Override
			public Iterator iterator() {
				return milestonesList.iterator();
			}
		};
	}

	@Test
	public void getSpringCloudVersionTest() throws Exception {
		RestTemplate rest = mock(RestTemplate.class);
		Github github = mock(Github.class);
		GithubPomReader githubPomReader = mock(GithubPomReader.class);
		Map<String, String> springCloudVersions = generateSpringCloudData();
		Map<String, Map<String, String>> springCloud = new HashMap<>();
		springCloud.put("spring-cloud", springCloudVersions);
		Map<String, Map<String, Map<String, String>>> info = new HashMap<>();
		info.put("bom-ranges", springCloud);
		when(rest.getForObject(anyString(), eq(Map.class))).thenReturn(info);
		InitializrSpringCloudInfoService service = new InitializrSpringCloudInfoService(rest, github, githubPomReader);
		String version = service.getSpringCloudVersion("2.1.0.RELEASE").getVersion();
		assertThat(version, Matchers.equalTo("Greenwich.SR1"));
		version = service.getSpringCloudVersion("2.1.4.RELEASE").getVersion();
		assertThat(version, Matchers.equalTo("Greenwich.SR1"));
		version = service.getSpringCloudVersion("2.1.5.RELEASE").getVersion();
		assertThat(version, Matchers.equalTo("Greenwich.BUILD-SNAPSHOT"));
		version = service.getSpringCloudVersion("1.5.5.RELEASE").getVersion();
		assertThat(version, Matchers.equalTo("Edgware.SR5"));
		version = service.getSpringCloudVersion("1.5.21.BUILD-SNAPSHOT").getVersion();
		assertThat(version, Matchers.equalTo("Edgware.BUILD-SNAPSHOT"));
	}

	private Map<String, String> generateSpringCloudData() {
		Map<String, String> data = new HashMap<>();
		data.put("Edgware.SR5", "Spring Boot >=1.5.0.RELEASE and <=1.5.20.RELEASE");
		data.put("Edgware.BUILD-SNAPSHOT", "Spring Boot >=1.5.21.BUILD-SNAPSHOT and <2.0.0.M1");
		data.put("Finchley.M2", "Spring Boot >=2.0.0.M3 and <2.0.0.M5");
		data.put("Finchley.M3", "Spring Boot >=2.0.0.M5 and <=2.0.0.M5");
		data.put("Finchley.M4", "Spring Boot >=2.0.0.M6 and <=2.0.0.M6");
		data.put("Finchley.RC1", "Spring Boot >=2.0.1.RELEASE and <2.0.2.RELEASE");
		data.put("Finchley.RC2", "Spring Boot >=2.0.2.RELEASE and <2.0.3.RELEASE");
		data.put("Finchley.SR3", "Spring Boot >=2.0.3.RELEASE and <2.0.999.BUILD-SNAPSHOT");
		data.put("Finchley.BUILD-SNAPSHO", "Spring Boot >=2.0.999.BUILD-SNAPSHOT and <2.1.0.M3");
		data.put("Greenwich.M1", "Spring Boot >=2.1.0.M3 and <2.1.0.RELEASE");
		data.put("Greenwich.SR1", "Spring Boot >=2.1.0.RELEASE and <2.1.5.BUILD-SNAPSHOT");
		data.put("Greenwich.BUILD-SNAPSHOT", "Spring Boot >=2.1.5.BUILD-SNAPSHOT");
		return data;

	}

}
