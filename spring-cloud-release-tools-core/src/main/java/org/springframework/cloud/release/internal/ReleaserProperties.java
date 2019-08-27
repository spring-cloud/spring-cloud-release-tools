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

package org.springframework.cloud.release.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.lang.SerializationUtils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Since, we are making a deep copy of this object, remember to have all the nested
 * classes to implement the Serializable interface.
 *
 * @author Marcin Grzejszczak
 */
@ConfigurationProperties("releaser")
public class ReleaserProperties implements Serializable {

	/**
	 * By default Releaser assumes running the program from the current working directory.
	 * If you want to change this behaviour - just change this value.
	 */
	private String workingDir;

	/**
	 * If set to {@code true} will run only post release tasks.
	 */
	private boolean postReleaseTasksOnly = false;

	private Git git = new Git();

	private Pom pom = new Pom();

	private Maven maven = new Maven();

	private Bash bash = new Bash();

	private Gradle gradle = new Gradle();

	private Sagan sagan = new Sagan();

	private Template template = new Template();

	private Versions versions = new Versions();

	/**
	 * Project name to its version - overrides all versions retrieved from a release train
	 * repository like Spring Cloud Release.
	 */
	private Map<String, String> fixedVersions = new LinkedHashMap<>();

	private MetaRelease metaRelease = new MetaRelease();

	public String getWorkingDir() {
		return StringUtils.hasText(this.workingDir) ? this.workingDir
				: System.getProperty("user.dir");
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	public Git getGit() {
		return this.git;
	}

	public void setGit(Git git) {
		this.git = git;
	}

	public Pom getPom() {
		return this.pom;
	}

	public void setPom(Pom pom) {
		this.pom = pom;
	}

	public Maven getMaven() {
		return this.maven;
	}

	public void setMaven(Maven maven) {
		this.maven = maven;
	}

	public Gradle getGradle() {
		return this.gradle;
	}

	public void setGradle(Gradle gradle) {
		this.gradle = gradle;
	}

	public Bash getBash() {
		return bash;
	}

	public void setBash(Bash bash) {
		this.bash = bash;
	}

	public Map<String, String> getFixedVersions() {
		return this.fixedVersions;
	}

	public void setFixedVersions(Map<String, String> fixedVersions) {
		this.fixedVersions = fixedVersions;
	}

	public MetaRelease getMetaRelease() {
		return this.metaRelease;
	}

	public void setMetaRelease(MetaRelease metaRelease) {
		this.metaRelease = metaRelease;
	}

	public Sagan getSagan() {
		return this.sagan;
	}

	public void setSagan(Sagan sagan) {
		this.sagan = sagan;
	}

	public Template getTemplate() {
		return this.template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	public boolean isPostReleaseTasksOnly() {
		return this.postReleaseTasksOnly;
	}

	public void setPostReleaseTasksOnly(boolean postReleaseTasksOnly) {
		this.postReleaseTasksOnly = postReleaseTasksOnly;
	}

	public Versions getVersions() {
		return this.versions;
	}

	public void setVersions(Versions versions) {
		this.versions = versions;
	}

	@Override
	public String toString() {
		return "ReleaserProperties{" + "workingDir='" + this.workingDir + '\'' + ", git="
				+ this.git + ", pom=" + this.pom + ", maven=" + this.maven + ", gradle="
				+ this.gradle + ", sagan=" + this.sagan + ", fixedVersions="
				+ this.fixedVersions + ", metaRelease=" + this.metaRelease + ", template="
				+ this.template + ", versions=" + this.versions + '}';
	}

	public ReleaserProperties copy() {
		return (ReleaserProperties) SerializationUtils.clone(this);
	}

	public static class MetaRelease implements Serializable {

		/**
		 * Are we releasing the whole suite of apps or only one?
		 */
		private boolean enabled = false;

		/**
		 * Name of the release train project.
		 */
		private String releaseTrainProjectName = "spring-cloud-release";

		/**
		 * All the names of dependencies that should be updated with the release train
		 * project version.
		 */
		private List<String> releaseTrainDependencyNames = Arrays.asList("spring-cloud",
				"spring-cloud-dependencies", "spring-cloud-starter");

		/**
		 * The URL of the Git organization. We'll append each project's name to it.
		 */
		private String gitOrgUrl = "https://github.com/spring-cloud";

		/**
		 * Names of projects to skip deployment for meta-release.
		 */
		private List<String> projectsToSkip = new ArrayList<String>() {
			{
				this.add("spring-boot");
				this.add("spring-cloud-stream");
				this.add("spring-cloud-task");
			}
		};

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getGitOrgUrl() {
			return this.gitOrgUrl;
		}

		public void setGitOrgUrl(String gitOrgUrl) {
			this.gitOrgUrl = gitOrgUrl;
		}

		public String getReleaseTrainProjectName() {
			return this.releaseTrainProjectName;
		}

		public void setReleaseTrainProjectName(String releaseTrainProjectName) {
			this.releaseTrainProjectName = releaseTrainProjectName;
		}

		public List<String> getReleaseTrainDependencyNames() {
			return this.releaseTrainDependencyNames;
		}

		public void setReleaseTrainDependencyNames(
				List<String> releaseTrainDependencyNames) {
			this.releaseTrainDependencyNames = releaseTrainDependencyNames;
		}

		public List<String> getProjectsToSkip() {
			return this.projectsToSkip;
		}

		public void setProjectsToSkip(List<String> projectsToSkip) {
			this.projectsToSkip = projectsToSkip;
		}

		@Override
		public String toString() {
			return "MetaRelease{" + "enabled=" + this.enabled
					+ ", releaseTrainProjectName='" + this.releaseTrainProjectName + '\''
					+ ", gitOrgUrl='" + this.gitOrgUrl + '\'' + ", projectsToSkip="
					+ this.projectsToSkip + '}';
		}

	}

	public static class Git implements Serializable {

		/**
		 * URL to a release train repository.
		 */
		private String releaseTrainBomUrl = "https://github.com/spring-cloud/spring-cloud-release";

		/**
		 * URL to the documentation Git repository.
		 */
		private String documentationUrl = "https://github.com/spring-cloud/spring-cloud-static";

		/**
		 * URL to the release train project page repository.
		 */
		private String springProjectUrl = "https://github.com/spring-projects/spring-cloud";

		/**
		 * URL to test samples.
		 */
		private String testSamplesProjectUrl = "https://github.com/spring-cloud/spring-cloud-core-tests";

		/**
		 * URL to the release train documentation.
		 */
		private String releaseTrainDocsUrl = "https://github.com/spring-cloud-samples/scripts";

		/**
		 * URL to the release train wiki.
		 */
		private String releaseTrainWikiUrl = "https://github.com/spring-projects/spring-cloud.wiki";

		/**
		 * Branch to check out for the documentation project.
		 */
		private String documentationBranch = "gh-pages";

		/**
		 * Branch to check out for the release train project.
		 */
		private String springProjectBranch = "gh-pages";

		/**
		 * Branch to check out for the test samples.
		 */
		private String testSamplesBranch = "master";

		/**
		 * Branch to check out for the release train docs.
		 */
		private String releaseTrainDocsBranch = "master";

		/**
		 * Page prefix for the release train wiki. E.g. for
		 * [Spring-Cloud-Finchley-Release-Notes] it would be [Spring-Cloud].
		 */
		private String releaseTrainWikiPagePrefix = "Spring-Cloud";

		/**
		 * Where should the release train repo get cloned to. If {@code null} defaults to
		 * a temporary directory.
		 */
		private String cloneDestinationDir;

		/**
		 * If {@code true} then should fill the map of versions from Git. If {@code false}
		 * then picks fixed versions.
		 */
		private boolean fetchVersionsFromGit = true;

		/**
		 * GitHub OAuth token to be used to interact with GitHub repo.
		 */
		private String oauthToken = "";

		/**
		 * Optional Git username. If not passed keys will be used for authentication.
		 */
		private String username;

		/**
		 * Optional Git password. If not passed keys will be used for authentication.
		 */
		private String password;

		/**
		 * In order not to iterate endlessly over milestones we introduce a threshold of
		 * milestones that we will go through to find the matching milestone.
		 */
		private Integer numberOfCheckedMilestones = 50;

		/**
		 * If {@code false}, will not update the documentation repository.
		 */
		private boolean updateDocumentationRepo = true;

		/**
		 * If set to {@code false}, will not update Spring Guides for a release train.
		 */
		private boolean updateSpringGuides = true;

		/**
		 * If set to {@code false}, will not update start.spring.io for a release train.
		 */
		private boolean updateStartSpringIo = true;

		/**
		 * If set to {@code false}, will not update the Spring Project for a release
		 * train. E.g. for Spring Cloud will not update https://cloud.spring.io .
		 */
		private boolean updateSpringProject = true;

		/**
		 * If set to {@code false}, will not update the test samples.
		 */
		private boolean runUpdatedSamples = true;

		/**
		 * If set to {@code false}, will not update the release train docs.
		 */
		private boolean updateReleaseTrainDocs = true;

		/**
		 * If set to {@code false}, will not clone and update the release train wiki.
		 */
		private boolean updateReleaseTrainWiki = true;

		/**
		 * If set to {@code false}, will not clone and update the samples for all
		 * projects.
		 */
		private boolean updateAllTestSamples = true;

		/**
		 * Project to urls mapping. For each project will clone the test project and will
		 * update its versions.
		 */
		private Map<String, List<String>> allTestSampleUrls = new HashMap<String, List<String>>() {
			{
				this.put("spring-cloud-sleuth", Arrays.asList(
						"https://github.com/spring-cloud-samples/sleuth-issues",
						"https://github.com/spring-cloud-samples/sleuth-documentation-apps"));
				this.put("spring-cloud-contract", Arrays.asList(
						"https://github.com/spring-cloud-samples/spring-cloud-contract-samples",
						"https://github.com/spring-cloud-samples/the-legacy-app",
						"https://github.com/spring-cloud-samples/sc-contract-car-rental"));
			}
		};

		public String getReleaseTrainBomUrl() {
			return this.releaseTrainBomUrl;
		}

		public void setReleaseTrainBomUrl(String releaseTrainBomUrl) {
			this.releaseTrainBomUrl = releaseTrainBomUrl;
		}

		public String getDocumentationUrl() {
			return this.documentationUrl;
		}

		public void setDocumentationUrl(String documentationUrl) {
			this.documentationUrl = documentationUrl;
		}

		public String getSpringProjectUrl() {
			return this.springProjectUrl;
		}

		public void setSpringProjectUrl(String springProjectUrl) {
			this.springProjectUrl = springProjectUrl;
		}

		public String getTestSamplesProjectUrl() {
			return this.testSamplesProjectUrl;
		}

		public void setTestSamplesProjectUrl(String testSamplesProjectUrl) {
			this.testSamplesProjectUrl = testSamplesProjectUrl;
		}

		public String getTestSamplesBranch() {
			return this.testSamplesBranch;
		}

		public void setTestSamplesBranch(String testSamplesBranch) {
			this.testSamplesBranch = testSamplesBranch;
		}

		public String getDocumentationBranch() {
			return this.documentationBranch;
		}

		public void setDocumentationBranch(String documentationBranch) {
			this.documentationBranch = documentationBranch;
		}

		public String getSpringProjectBranch() {
			return this.springProjectBranch;
		}

		public void setSpringProjectBranch(String springProjectBranch) {
			this.springProjectBranch = springProjectBranch;
		}

		public boolean isUpdateDocumentationRepo() {
			return this.updateDocumentationRepo;
		}

		public void setUpdateDocumentationRepo(boolean updateDocumentationRepo) {
			this.updateDocumentationRepo = updateDocumentationRepo;
		}

		public boolean isRunUpdatedSamples() {
			return this.runUpdatedSamples;
		}

		public void setRunUpdatedSamples(boolean runUpdatedSamples) {
			this.runUpdatedSamples = runUpdatedSamples;
		}

		public boolean isUpdateAllTestSamples() {
			return this.updateAllTestSamples;
		}

		public void setUpdateAllTestSamples(boolean updateAllTestSamples) {
			this.updateAllTestSamples = updateAllTestSamples;
		}

		public String getCloneDestinationDir() {
			return this.cloneDestinationDir;
		}

		public void setCloneDestinationDir(String cloneDestinationDir) {
			this.cloneDestinationDir = cloneDestinationDir;
		}

		public String getOauthToken() {
			return this.oauthToken;
		}

		public void setOauthToken(String oauthToken) {
			this.oauthToken = oauthToken;
		}

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Integer getNumberOfCheckedMilestones() {
			return this.numberOfCheckedMilestones;
		}

		public void setNumberOfCheckedMilestones(Integer numberOfCheckedMilestones) {
			this.numberOfCheckedMilestones = numberOfCheckedMilestones;
		}

		public boolean isFetchVersionsFromGit() {
			return this.fetchVersionsFromGit;
		}

		public void setFetchVersionsFromGit(boolean fetchVersionsFromGit) {
			this.fetchVersionsFromGit = fetchVersionsFromGit;
		}

		public boolean isUpdateSpringGuides() {
			return this.updateSpringGuides;
		}

		public void setUpdateSpringGuides(boolean updateSpringGuides) {
			this.updateSpringGuides = updateSpringGuides;
		}

		public boolean isUpdateStartSpringIo() {
			return this.updateStartSpringIo;
		}

		public void setUpdateStartSpringIo(boolean updateStartSpringIo) {
			this.updateStartSpringIo = updateStartSpringIo;
		}

		public boolean isUpdateSpringProject() {
			return this.updateSpringProject;
		}

		public void setUpdateSpringProject(boolean updateSpringProject) {
			this.updateSpringProject = updateSpringProject;
		}

		public String getReleaseTrainDocsUrl() {
			return this.releaseTrainDocsUrl;
		}

		public void setReleaseTrainDocsUrl(String releaseTrainDocsUrl) {
			this.releaseTrainDocsUrl = releaseTrainDocsUrl;
		}

		public String getReleaseTrainDocsBranch() {
			return this.releaseTrainDocsBranch;
		}

		public void setReleaseTrainDocsBranch(String releaseTrainDocsBranch) {
			this.releaseTrainDocsBranch = releaseTrainDocsBranch;
		}

		public boolean isUpdateReleaseTrainDocs() {
			return this.updateReleaseTrainDocs;
		}

		public void setUpdateReleaseTrainDocs(boolean updateReleaseTrainDocs) {
			this.updateReleaseTrainDocs = updateReleaseTrainDocs;
		}

		public Map<String, List<String>> getAllTestSampleUrls() {
			return this.allTestSampleUrls;
		}

		public void setAllTestSampleUrls(Map<String, List<String>> allTestSampleUrls) {
			this.allTestSampleUrls = allTestSampleUrls;
		}

		public String getReleaseTrainWikiUrl() {
			return this.releaseTrainWikiUrl;
		}

		public void setReleaseTrainWikiUrl(String releaseTrainWikiUrl) {
			this.releaseTrainWikiUrl = releaseTrainWikiUrl;
		}

		public boolean isUpdateReleaseTrainWiki() {
			return this.updateReleaseTrainWiki;
		}

		public void setUpdateReleaseTrainWiki(boolean updateReleaseTrainWiki) {
			this.updateReleaseTrainWiki = updateReleaseTrainWiki;
		}

		public String getReleaseTrainWikiPagePrefix() {
			return this.releaseTrainWikiPagePrefix;
		}

		public void setReleaseTrainWikiPagePrefix(String releaseTrainWikiPagePrefix) {
			this.releaseTrainWikiPagePrefix = releaseTrainWikiPagePrefix;
		}

		@Override
		public String toString() {
			return "Git{" + "releaseTrainBomUrl='" + this.releaseTrainBomUrl + '\''
					+ ", documentationUrl='" + this.documentationUrl + '\''
					+ ", documentationBranch='" + this.documentationBranch + '\''
					+ ", releaseTrainWikiUrl='" + this.releaseTrainWikiUrl + '\''
					+ ", updateDocumentationRepo=" + this.updateDocumentationRepo
					+ ", springProjectUrl=" + this.springProjectUrl
					+ ", springProjectBranch=" + this.springProjectBranch
					+ ", releaseTrainWikiPagePrefix=" + this.releaseTrainWikiPagePrefix
					+ ", cloneDestinationDir='" + this.cloneDestinationDir + '\''
					+ ", fetchVersionsFromGit=" + this.fetchVersionsFromGit
					+ ", numberOfCheckedMilestones=" + this.numberOfCheckedMilestones
					+ ", updateSpringGuides=" + this.updateSpringGuides
					+ ", updateSpringProject=" + this.updateSpringProject
					+ ", sampleUrlsSize=" + this.allTestSampleUrls.size() + '}';
		}

	}

	public static class Pom implements Serializable {

		/**
		 * Which branch of release train BOM should be checked out. Defaults to
		 * {@code master}.
		 */
		private String branch = "master";

		/**
		 * Subfolder of the pom that contains the {@code spring-boot-starer-parent}
		 * dependency.
		 */
		private String pomWithBootStarterParent = "spring-cloud-starter-parent/pom.xml";

		/**
		 * Subfolder of the pom that contains the versions for the release train.
		 */
		private String thisTrainBom = "spring-cloud-dependencies/pom.xml";

		/**
		 * The pattern to match a version property in a BOM.
		 */
		private String bomVersionPattern = "^(spring-cloud-.*)\\.version$";

		/**
		 * List of regular expressions of ignored poms. Defaults to test projects and
		 * samples.
		 */
		@SuppressWarnings("unchecked")
		private List<String> ignoredPomRegex = Arrays.asList("^.*\\.git/.*$",
				"^.*spring-cloud-contract-maven-plugin/src/test/projects/.*$",
				"^.*spring-cloud-contract-maven-plugin/target/.*$",
				"^.*samples/standalone/[a-z]+/.*$");

		public String getBranch() {
			return this.branch;
		}

		public void setBranch(String branch) {
			this.branch = branch;
		}

		public List<String> getIgnoredPomRegex() {
			return this.ignoredPomRegex;
		}

		public void setIgnoredPomRegex(List<String> ignoredPomRegex) {
			this.ignoredPomRegex = ignoredPomRegex;
		}

		public String getPomWithBootStarterParent() {
			return this.pomWithBootStarterParent;
		}

		public void setPomWithBootStarterParent(String pomWithBootStarterParent) {
			this.pomWithBootStarterParent = pomWithBootStarterParent;
		}

		public String getThisTrainBom() {
			return this.thisTrainBom;
		}

		public void setThisTrainBom(String thisTrainBom) {
			this.thisTrainBom = thisTrainBom;
		}

		public String getBomVersionPattern() {
			return this.bomVersionPattern;
		}

		public void setBomVersionPattern(String bomVersionPattern) {
			this.bomVersionPattern = bomVersionPattern;
		}

		@Override
		public String toString() {
			return "Pom{" + "branch='" + this.branch + '\''
					+ ", pomWithBootStarterParent='" + this.pomWithBootStarterParent
					+ '\'' + ", thisTrainBom='" + this.thisTrainBom + '\''
					+ ", bomVersionPattern='" + this.bomVersionPattern + '\''
					+ ", ignoredPomRegex=" + this.ignoredPomRegex + '}';
		}

	}

	public static class Maven implements Serializable {

		/**
		 * Placeholder for system properties.
		 */
		public static final String SYSTEM_PROPS_PLACEHOLDER = "{{systemProps}}";

		/**
		 * Command to be executed to build the project.
		 */
		private String buildCommand = "./mvnw clean install -B -Pdocs {{systemProps}}";

		/**
		 * Command to be executed to deploy a built project.
		 */
		private String deployCommand = "./mvnw deploy -DskipTests -B -Pfast,deploy {{systemProps}}";

		/**
		 * Command to be executed to build and deploy guides project only.
		 */
		private String deployGuidesCommand = "./mvnw clean verify deploy -B -Pguides,integration -pl guides {{systemProps}}";

		/**
		 * Command to be executed to publish documentation. If present "{{version}}" will
		 * be replaced by the provided version.
		 */
		private String[] publishDocsCommands = { "mkdir -p target",
				"wget https://raw.githubusercontent.com/spring-cloud/"
						+ "spring-cloud-build/master/"
						+ "docs/src/main/asciidoc/ghpages.sh -O target/gh-pages.sh",
				"chmod +x target/gh-pages.sh", "./target/gh-pages.sh -v {{version}} -c" };

		/**
		 * Command to be executed to generate release train documentation.
		 */
		private String generateReleaseTrainDocsCommand = "bash release_train.sh "
				+ "--retrieveversions --version {{version}} --ghpages --auto";

		/**
		 * Additional system properties that should be passed to the build / deploy
		 * commands. If present in other commands "{{systemProps}}" will be substituted
		 * with this property.
		 */
		private String systemProperties = "";

		/**
		 * Max wait time in minutes for the process to finish.
		 */
		private long waitTimeInMinutes = 20;

		public String getBuildCommand() {
			return this.buildCommand;
		}

		public void setBuildCommand(String buildCommand) {
			this.buildCommand = buildCommand;
		}

		public long getWaitTimeInMinutes() {
			return this.waitTimeInMinutes;
		}

		public void setWaitTimeInMinutes(long waitTimeInMinutes) {
			this.waitTimeInMinutes = waitTimeInMinutes;
		}

		public String getDeployCommand() {
			return this.deployCommand;
		}

		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		public String getDeployGuidesCommand() {
			return this.deployGuidesCommand;
		}

		public void setDeployGuidesCommand(String deployGuidesCommand) {
			this.deployGuidesCommand = deployGuidesCommand;
		}

		public String[] getPublishDocsCommands() {
			return this.publishDocsCommands;
		}

		public void setPublishDocsCommands(String[] publishDocsCommands) {
			this.publishDocsCommands = publishDocsCommands;
		}

		public String getGenerateReleaseTrainDocsCommand() {
			return this.generateReleaseTrainDocsCommand;
		}

		public void setGenerateReleaseTrainDocsCommand(
				String generateReleaseTrainDocsCommand) {
			this.generateReleaseTrainDocsCommand = generateReleaseTrainDocsCommand;
		}

		public String getSystemProperties() {
			return this.systemProperties;
		}

		public void setSystemProperties(String systemProperties) {
			this.systemProperties = systemProperties;
		}

		@Override
		public String toString() {
			return "Maven{" + "buildCommand='" + this.buildCommand + '\''
					+ ", deployCommand='" + this.deployCommand + '\''
					+ ", publishDocsCommands=" + Arrays.toString(this.publishDocsCommands)
					+ "generateReleaseTrainDocsCommand='"
					+ this.generateReleaseTrainDocsCommand + '\'' + ", waitTimeInMinutes="
					+ this.waitTimeInMinutes + '}';
		}

	}

	public static class Bash implements Serializable {

		/**
		 * Placeholder for system properties.
		 */
		public static final String SYSTEM_PROPS_PLACEHOLDER = "{{systemProps}}";

		/**
		 * Command to be executed to build the project.
		 */
		private String buildCommand = "echo \"{{systemProps}}\"";

		/**
		 * Command to be executed to deploy a built project.
		 */
		private String deployCommand = "echo \"{{systemProps}}\"";

		/**
		 * Command to be executed to build and deploy guides project only.
		 */
		private String deployGuidesCommand = "echo \"{{systemProps}}\"";

		/**
		 * Command to be executed to publish documentation. If present "{{version}}" will
		 * be replaced by the provided version.
		 */
		private String[] publishDocsCommands = { "mkdir -p target",
				"echo \"{{version}}\"" };

		/**
		 * Command to be executed to generate release train documentation.
		 */
		private String generateReleaseTrainDocsCommand = "echo \"{{version}}\"";

		/**
		 * Additional system properties that should be passed to the build / deploy
		 * commands. If present in other commands "{{systemProps}}" will be substituted
		 * with this property.
		 */
		private String systemProperties = "";

		/**
		 * Max wait time in minutes for the process to finish.
		 */
		private long waitTimeInMinutes = 20;

		public String getBuildCommand() {
			return this.buildCommand;
		}

		public void setBuildCommand(String buildCommand) {
			this.buildCommand = buildCommand;
		}

		public long getWaitTimeInMinutes() {
			return this.waitTimeInMinutes;
		}

		public void setWaitTimeInMinutes(long waitTimeInMinutes) {
			this.waitTimeInMinutes = waitTimeInMinutes;
		}

		public String getDeployCommand() {
			return this.deployCommand;
		}

		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		public String getDeployGuidesCommand() {
			return this.deployGuidesCommand;
		}

		public void setDeployGuidesCommand(String deployGuidesCommand) {
			this.deployGuidesCommand = deployGuidesCommand;
		}

		public String[] getPublishDocsCommands() {
			return this.publishDocsCommands;
		}

		public void setPublishDocsCommands(String[] publishDocsCommands) {
			this.publishDocsCommands = publishDocsCommands;
		}

		public String getGenerateReleaseTrainDocsCommand() {
			return this.generateReleaseTrainDocsCommand;
		}

		public void setGenerateReleaseTrainDocsCommand(
				String generateReleaseTrainDocsCommand) {
			this.generateReleaseTrainDocsCommand = generateReleaseTrainDocsCommand;
		}

		public String getSystemProperties() {
			return this.systemProperties;
		}

		public void setSystemProperties(String systemProperties) {
			this.systemProperties = systemProperties;
		}

		@Override
		public String toString() {
			return "Maven{" + "buildCommand='" + this.buildCommand + '\''
					+ ", deployCommand='" + this.deployCommand + '\''
					+ ", publishDocsCommands=" + Arrays.toString(this.publishDocsCommands)
					+ "generateReleaseTrainDocsCommand='"
					+ this.generateReleaseTrainDocsCommand + '\'' + ", waitTimeInMinutes="
					+ this.waitTimeInMinutes + '}';
		}

	}

	public static class Gradle implements Serializable {

		/**
		 * A mapping that should be applied to {@code gradle.properties} in order to
		 * perform a substitution of properties. The mapping is from a property inside
		 * {@code gradle.properties} to the projects name. Example.
		 *
		 * In {@code gradle.properties} you have {@code verifierVersion=1.0.0} . You want
		 * this property to get updated with the value of {@code spring-cloud-contract}
		 * version. Then it's enough to do the mapping like this for this Releaser's
		 * property: {@code verifierVersion=spring-cloud-contract}.
		 */
		private Map<String, String> gradlePropsSubstitution = new HashMap<String, String>() {
			{
				this.put("bootVersion", "spring-boot");
				this.put("BOOT_VERSION", "spring-boot");
				this.put("bomVersion", "spring-cloud-release");
				this.put("BOM_VERSION", "spring-cloud-release");
				this.put("springCloudBuildVersion", "spring-cloud-build");
			}
		};

		/**
		 * List of regular expressions of ignored gradle props. Defaults to test projects
		 * and samples.
		 */
		@SuppressWarnings("unchecked")
		private List<String> ignoredGradleRegex = Arrays.asList(
				"^.*spring-cloud-contract-maven-plugin/src/test/projects/.*$",
				"^.*spring-cloud-contract-maven-plugin/target/.*$",
				"^.*samples/standalone/[a-z]+/.*$");

		/**
		 * Placeholder for system properties.
		 */
		public static final String SYSTEM_PROPS_PLACEHOLDER = "{{systemProps}}";

		/**
		 * Command to be executed to build the project.
		 */
		private String buildCommand = "./gradlew clean build publishToMavenLocal {{systemProps}}";

		/**
		 * Command to be executed to deploy a built project.
		 */
		private String deployCommand = "./gradlew clean build publish {{systemProps}}";

		/**
		 * Command to be executed to build and deploy guides project only.
		 */
		private String deployGuidesCommand = "./gradlew clean build deployGuides {{systemProps}}";

		/**
		 * Command to be executed to publish documentation. If present "{{version}}" will
		 * be replaced by the provided version.
		 */
		private String[] publishDocsCommands = { "echo 'TODO'" };

		/**
		 * Command to be executed to generate release train documentation.
		 */
		private String generateReleaseTrainDocsCommand = "echo 'TODO'";

		/**
		 * Additional system properties that should be passed to the build / deploy
		 * commands. If present in other commands "{{systemProps}}" will be substituted
		 * with this property.
		 */
		private String systemProperties = "";

		/**
		 * Max wait time in minutes for the process to finish.
		 */
		private long waitTimeInMinutes = 20;

		public String getBuildCommand() {
			return this.buildCommand;
		}

		public void setBuildCommand(String buildCommand) {
			this.buildCommand = buildCommand;
		}

		public long getWaitTimeInMinutes() {
			return this.waitTimeInMinutes;
		}

		public void setWaitTimeInMinutes(long waitTimeInMinutes) {
			this.waitTimeInMinutes = waitTimeInMinutes;
		}

		public String getDeployCommand() {
			return this.deployCommand;
		}

		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		public String getDeployGuidesCommand() {
			return this.deployGuidesCommand;
		}

		public void setDeployGuidesCommand(String deployGuidesCommand) {
			this.deployGuidesCommand = deployGuidesCommand;
		}

		public String[] getPublishDocsCommands() {
			return this.publishDocsCommands;
		}

		public void setPublishDocsCommands(String[] publishDocsCommands) {
			this.publishDocsCommands = publishDocsCommands;
		}

		public String getGenerateReleaseTrainDocsCommand() {
			return this.generateReleaseTrainDocsCommand;
		}

		public void setGenerateReleaseTrainDocsCommand(
				String generateReleaseTrainDocsCommand) {
			this.generateReleaseTrainDocsCommand = generateReleaseTrainDocsCommand;
		}

		public String getSystemProperties() {
			return this.systemProperties;
		}

		public void setSystemProperties(String systemProperties) {
			this.systemProperties = systemProperties;
		}

		public Map<String, String> getGradlePropsSubstitution() {
			return this.gradlePropsSubstitution;
		}

		public void setGradlePropsSubstitution(
				Map<String, String> gradlePropsSubstitution) {
			this.gradlePropsSubstitution = gradlePropsSubstitution;
		}

		public List<String> getIgnoredGradleRegex() {
			return this.ignoredGradleRegex;
		}

		public void setIgnoredGradleRegex(List<String> ignoredGradleRegex) {
			this.ignoredGradleRegex = ignoredGradleRegex;
		}

		@Override
		public String toString() {
			return new StringJoiner(", ", Gradle.class.getSimpleName() + "[", "]")
					.add("gradlePropsSubstitution=" + gradlePropsSubstitution)
					.add("ignoredGradleRegex=" + ignoredGradleRegex)
					.add("buildCommand='" + buildCommand + "'")
					.add("deployCommand='" + deployCommand + "'")
					.add("deployGuidesCommand='" + deployGuidesCommand + "'")
					.add("publishDocsCommands=" + Arrays.toString(publishDocsCommands))
					.add("generateReleaseTrainDocsCommand='"
							+ generateReleaseTrainDocsCommand + "'")
					.add("systemProperties='" + systemProperties + "'")
					.add("waitTimeInMinutes=" + waitTimeInMinutes).toString();
		}

	}

	public static class Sagan implements Serializable {

		/**
		 * If set to {@code false} will not update Sagan.
		 */
		private boolean updateSagan = true;

		/**
		 * URL to the Sagan API.
		 */
		private String baseUrl = "https://spring.io";

		/**
		 * Folder with asciidoctor files for docs.
		 */
		private String docsAdocsFile = "docs/src/main/asciidoc";

		/**
		 * Name of the ascii doc file with core part of this project's Sagan project page.
		 * Linked with {@link this#docsAdocsFile}.
		 */
		private String indexSectionFileName = "sagan-index.adoc";

		/**
		 * Name of the ascii doc file with boot part of this project's Sagan project page.
		 * Linked with {@link this#docsAdocsFile}.
		 */
		private String bootSectionFileName = "sagan-boot.adoc";

		public String getBaseUrl() {
			return this.baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public boolean isUpdateSagan() {
			return this.updateSagan;
		}

		public void setUpdateSagan(boolean updateSagan) {
			this.updateSagan = updateSagan;
		}

		public String getDocsAdocsFile() {
			return this.docsAdocsFile;
		}

		public void setDocsAdocsFile(String docsAdocsFile) {
			this.docsAdocsFile = docsAdocsFile;
		}

		public String getIndexSectionFileName() {
			return this.indexSectionFileName;
		}

		public void setIndexSectionFileName(String indexSectionFileName) {
			this.indexSectionFileName = indexSectionFileName;
		}

		public String getBootSectionFileName() {
			return this.bootSectionFileName;
		}

		public void setBootSectionFileName(String bootSectionFileName) {
			this.bootSectionFileName = bootSectionFileName;
		}

		@Override
		public String toString() {
			return "Sagan{" + "baseUrl='" + this.baseUrl + '\'' + '}';
		}

	}

	public static class Template implements Serializable {

		/**
		 * Folder in which blog, email etc. templates are stored.
		 */
		private String templateFolder = "cloud";

		public String getTemplateFolder() {
			return this.templateFolder;
		}

		public void setTemplateFolder(String templateFolder) {
			this.templateFolder = templateFolder;
		}

		@Override
		public String toString() {
			return "Template{" + "templateFolder='" + this.templateFolder + '\'' + '}';
		}

	}

	public static class Versions implements Serializable {

		/**
		 * Url to a file containing all the versions. Defaults to YAML from
		 * start.spring.io.
		 */
		private String allVersionsFileUrl = "https://raw.githubusercontent.com/spring-io/start.spring.io/master/start-site/src/main/resources/application.yml";

		/**
		 * Name in the YAML from initilizr for BOM mappings.
		 */
		private String bomName = "spring-cloud";

		public String getAllVersionsFileUrl() {
			return this.allVersionsFileUrl;
		}

		public void setAllVersionsFileUrl(String allVersionsFileUrl) {
			this.allVersionsFileUrl = allVersionsFileUrl;
		}

		public String getBomName() {
			return bomName;
		}

		public void setBomName(String bomName) {
			this.bomName = bomName;
		}

		@Override
		public String toString() {
			return "Versions{" + "allVersionsFileUrl='" + this.allVersionsFileUrl + '\''
					+ ", bomName='" + this.bomName + '\'' + '}';
		}

	}

}
