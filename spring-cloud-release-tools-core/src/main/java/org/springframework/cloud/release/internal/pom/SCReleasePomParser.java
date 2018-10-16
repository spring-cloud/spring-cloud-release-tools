/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the poms for a given project and populates versions from Spring Cloud Release
 *
 * @author Marcin Grzejszczak
 */
class SCReleasePomParser {

	private static final Logger log = LoggerFactory.getLogger(SCReleasePomParser.class);

	private static final String STARTER_POM = "spring-cloud-starter-parent/pom.xml";
	private static final String DEPENDENCIES_POM = "spring-cloud-dependencies/pom.xml";
	private static final Pattern SC_VERSION_PATTERN = Pattern.compile("^(spring-cloud-.*)\\.version$");

	private final File springCloudReleaseDir;
	private final String bootPom;
	private final String dependenciesPomPath;
	private final PomReader pomReader = new PomReader();

	SCReleasePomParser(File springCloudReleaseDir) {
		this(springCloudReleaseDir, STARTER_POM, DEPENDENCIES_POM);
	}

	SCReleasePomParser(File springCloudReleaseDir, String bootPom, String dependenciesPom) {
		this.springCloudReleaseDir = springCloudReleaseDir;
		this.bootPom = bootPom;
		this.dependenciesPomPath = dependenciesPom;
	}

	Versions allVersions() {
		Versions boot = bootVersion();
		Versions cloud = springCloudVersions();
		return new Versions(boot.bootVersion, cloud.scBuildVersion, allProjects(boot, cloud));
	}

	private Set<Project> allProjects(Versions boot, Versions cloud) {
		Set<Project> allProjects = new HashSet<>();
		allProjects.addAll(boot.projects);
		allProjects.addAll(cloud.projects);
		return allProjects;
	}

	Versions bootVersion() {
		Model model = pom(this.bootPom);
		String bootArtifactId = model.getParent().getArtifactId();
		log.debug("Boot artifact id is equal to [{}]", bootArtifactId);
		if (!SpringCloudConstants.BOOT_STARTER_PARENT_ARTIFACT_ID.equals(bootArtifactId)) {
			throw new IllegalStateException("The pom doesn't have a [" + SpringCloudConstants.BOOT_STARTER_PARENT_ARTIFACT_ID
					+ "] artifact id");
		}
		String bootVersion = model.getParent().getVersion();
		log.debug("Boot version is equal to [{}]", bootVersion);
		return new Versions(bootVersion);
	}

	private Model pom(String pom) {
		if (pom == null) {
			throw new IllegalStateException("Pom is not present");
		}
		File pomFile = new File(this.springCloudReleaseDir, pom);
		if (!pomFile.exists()) {
			throw new IllegalStateException("Pom is not present");
		}
		return this.pomReader.readPom(pomFile);
	}

	Versions springCloudVersions() {
		Model model = pom(this.dependenciesPomPath);
		String buildArtifact = model.getParent().getArtifactId();
		log.debug("[{}] artifact id is equal to [{}]", SpringCloudConstants.CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, buildArtifact);
		if (!SpringCloudConstants.CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID.equals(buildArtifact)) {
			throw new IllegalStateException("The pom doesn't have a [" + SpringCloudConstants.CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID
					+ "] artifact id");
		}
		String buildVersion = model.getParent().getVersion();
		log.debug("Spring Cloud Build version is equal to [{}]", buildVersion);
		Set<Project> projects = model.getProperties().entrySet()
				.stream()
				.filter(propertyMatchesSCPattern())
				.map(toProject())
				.collect(Collectors.toSet());
		String scReleaseVersion = model.getVersion();
		projects.add(new Project("spring-cloud-release", scReleaseVersion));
		return new Versions(buildVersion, projects);
	}

	private Predicate<Map.Entry<Object, Object>> propertyMatchesSCPattern() {
		return entry -> SC_VERSION_PATTERN.matcher(entry.getKey().toString()).matches();
	}

	private Function<Map.Entry<Object, Object>, Project> toProject() {
		return entry -> {
			Matcher matcher = SC_VERSION_PATTERN.matcher(entry.getKey().toString());
			// you have to first match to get info about the group
			matcher.matches();
			String name = matcher.group(1);
			return new Project(name, entry.getValue().toString());
		};
	}
}

