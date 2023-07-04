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

package releaser.internal;

import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.validation.constraints.NotBlank;

import org.apache.commons.lang.SerializationUtils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Since, we are making a deep copy of this object, remember to have all the nested
 * classes to implement the Serializable interface.
 *
 * @author Marcin Grzejszczak
 */
@ConfigurationProperties("releaser")
@Validated
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

	/**
	 * If set to {@code true} will not run post release tasks.
	 */
	private boolean skipPostReleaseTasks = false;

	private Flow flow = new Flow();

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
		return StringUtils.hasText(this.workingDir) ? this.workingDir : System.getProperty("user.dir");
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	public Flow getFlow() {
		return this.flow;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
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

	public boolean isSkipPostReleaseTasks() {
		return this.skipPostReleaseTasks;
	}

	public void setSkipPostReleaseTasks(boolean skipPostReleaseTasks) {
		this.skipPostReleaseTasks = skipPostReleaseTasks;
	}

	public Versions getVersions() {
		return this.versions;
	}

	public void setVersions(Versions versions) {
		this.versions = versions;
	}

	@Override
	public String toString() {
		return "ReleaserProperties{" + "workingDir='" + this.workingDir + '\'' + ", git=" + this.git + ", pom="
				+ this.pom + ", maven=" + this.maven + ", gradle=" + this.gradle + ", sagan=" + this.sagan
				+ ", fixedVersions=" + this.fixedVersions + ", metaRelease=" + this.metaRelease + ", template="
				+ this.template + ", versions=" + this.versions + '}';
	}

	public ReleaserProperties copy() {
		return (ReleaserProperties) SerializationUtils.clone(this);
	}

	/**
	 * Abstraction over command execution using different languages and frameworks.
	 */
	public interface Command {

		/**
		 * @return build command
		 */
		String getBuildCommand();

		/**
		 * @param buildCommand to set
		 */
		void setBuildCommand(String buildCommand);

		/**
		 * @return deploy command
		 */
		String getDeployCommand();

		/**
		 * @param deployCommand to set
		 */
		void setDeployCommand(String deployCommand);

		/**
		 * @return deploy guides command
		 */
		String getDeployGuidesCommand();

		/**
		 * @param deployGuidesCommand to set
		 */
		void setDeployGuidesCommand(String deployGuidesCommand);

		/**
		 * @return docs publishing commands
		 */
		String getPublishDocsCommand();

		/**
		 * @param publishDocsCommand to set
		 */
		void setPublishDocsCommand(String publishDocsCommand);

		/**
		 * @return generate release train docs command
		 */
		String getGenerateReleaseTrainDocsCommand();

		/**
		 * @param generateReleaseTrainDocsCommand to set
		 */
		void setGenerateReleaseTrainDocsCommand(String generateReleaseTrainDocsCommand);

		/**
		 * @return system properties
		 */
		String getSystemProperties();

		/**
		 * @param systemProperties to set
		 */
		void setSystemProperties(String systemProperties);

	}

	public static class MetaRelease implements Serializable {

		/**
		 * Are we releasing the whole suite of apps or only one?
		 */
		private boolean enabled = false;

		/**
		 * Name of the release train project.
		 */
		@NotBlank
		private String releaseTrainProjectName;

		/**
		 * All the names of dependencies that should be updated with the release train
		 * project version.
		 */
		private List<String> releaseTrainDependencyNames = new ArrayList<>();

		/**
		 * The URL of the Git organization. We'll append each project's name to it.
		 */
		@NotBlank
		private String gitOrgUrl;

		/**
		 * Names of projects to skip deployment for meta-release.
		 */
		private List<String> projectsToSkip = new ArrayList<String>();

		/**
		 * If provided, allows to provide groups of projects that can be ran in parallel.
		 * E.g.
		 * {@code --releaser.meta-release.release-groups[0]=projectA,projectB,projectC}
		 * {@code --releaser.meta-release.release-groups[1]=projectD,projectE}
		 * {@code --releaser.meta-release.release-groups[2]=projectF,projectG} The order
		 * is still provided by the list of versions passed to the releaser. Basing on
		 * that order, and this value we are able to build a flow with projects.
		 */
		private List<String> releaseGroups = new LinkedList<>();

		/**
		 * Timeout in minutes during which we're waiting for a single composite task per a
		 * project to be executed. That means that if set to e.g. 180 then a release
		 * process for a single project should take at most 180 minutes.
		 */
		private int releaseGroupTimeoutInMinutes = 180;

		/**
		 * Number of threads per release group. E.g. for thread count of 4 if there are 6
		 * projects in a release group, 4 of them will be executed in parallel and 2 will
		 * wait for their turn.
		 */
		private int releaseGroupThreadCount = 4;

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

		public void setReleaseTrainDependencyNames(List<String> releaseTrainDependencyNames) {
			this.releaseTrainDependencyNames = releaseTrainDependencyNames;
		}

		public List<String> getProjectsToSkip() {
			return this.projectsToSkip;
		}

		public void setProjectsToSkip(List<String> projectsToSkip) {
			this.projectsToSkip = projectsToSkip;
		}

		public List<String> getReleaseGroups() {
			return this.releaseGroups;
		}

		public void setReleaseGroups(List<String> releaseGroups) {
			this.releaseGroups = releaseGroups;
		}

		public int getReleaseGroupTimeoutInMinutes() {
			return this.releaseGroupTimeoutInMinutes;
		}

		public void setReleaseGroupTimeoutInMinutes(int releaseGroupTimeoutInMinutes) {
			this.releaseGroupTimeoutInMinutes = releaseGroupTimeoutInMinutes;
		}

		public int getReleaseGroupThreadCount() {
			return this.releaseGroupThreadCount;
		}

		public void setReleaseGroupThreadCount(int releaseGroupThreadCount) {
			this.releaseGroupThreadCount = releaseGroupThreadCount;
		}

		@Override
		public String toString() {
			return "MetaRelease{" + "enabled=" + enabled + ", releaseTrainProjectName='" + releaseTrainProjectName
					+ '\'' + ", releaseTrainDependencyNames=" + releaseTrainDependencyNames + ", gitOrgUrl='"
					+ gitOrgUrl + '\'' + ", projectsToSkip=" + projectsToSkip + ", releaseGroups=" + releaseGroups
					+ ", releaseGroupTimeoutInMinutes=" + releaseGroupTimeoutInMinutes + ", releaseGroupThreadCount="
					+ releaseGroupThreadCount + '}';
		}

	}

	public static class Flow implements Serializable {

		/**
		 * Should the default flow of jobs be preserved. If set to {@code false} will not
		 * register any jobs as beans, and it will be up to you to set the whole
		 * configuration of jobs.
		 */
		private boolean defaultEnabled = true;

		public boolean isDefaultEnabled() {
			return this.defaultEnabled;
		}

		public void setDefaultEnabled(boolean defaultEnabled) {
			this.defaultEnabled = defaultEnabled;
		}

	}

	public static class Git implements Serializable {

		/**
		 * Absolute path to a directory with cache for OkHTTP calls to GitHub.
		 */
		@NotBlank
		private String cacheDirectory = temporaryDirectory();

		/**
		 * URL to a release train repository.
		 */
		@NotBlank
		private String releaseTrainBomUrl;

		/**
		 * URL to the documentation Git repository.
		 */
		private String documentationUrl;

		/**
		 * The organization name on Github.
		 */
		@NotBlank
		private String orgName;

		/**
		 * URL to the release train project page repository.
		 */
		private String springProjectUrl;

		/**
		 * URL to test samples.
		 */
		private String testSamplesProjectUrl;

		/**
		 * URL to the release train documentation.
		 */
		private String releaseTrainDocsUrl;

		/**
		 * URL to the release train wiki.
		 */
		private String releaseTrainWikiUrl;

		/**
		 * Branch to check out for the documentation project.
		 */
		private String documentationBranch;

		/**
		 * Branch to check out for the release train project.
		 */
		// TODO: seems to only be for gh-docs? we guess when we could use this?
		private String springProjectBranch;

		/**
		 * Branch to check out for the test samples.
		 */
		private String testSamplesBranch;

		/**
		 * Branch to check out for the release train.
		 */
		private String releaseTrainBranch;

		/**
		 * Branch to check out for the release train docs.
		 */
		private String releaseTrainDocsBranch;

		/**
		 * Page prefix for the release train wiki. E.g. for
		 * [Spring-Cloud-Finchley-Release-Notes] it would be [Spring-Cloud].
		 */
		private String releaseTrainWikiPagePrefix;

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
		 * URL to the fat jar with Github Changelog Generator.
		 */
		private String githubChangelogGeneratorUrl = "https://github.com/spring-io/github-changelog-generator/releases/download/v0.0.8/github-changelog-generator.jar";

		/**
		 * In order not to iterate endlessly over milestones we introduce a threshold of
		 * milestones that we will go through to find the matching milestone.
		 */
		private Integer numberOfCheckedMilestones = 50;

		/**
		 * If {@code false}, will not update the documentation repository.
		 */
		private boolean updateDocumentationRepo = false;

		/**
		 * If set to {@code false}, will not update Github milestones.
		 */
		private boolean updateGithubMilestones = false;

		/**
		 * If set to {@code false}, will not create release notes for milestone.
		 */
		private boolean createReleaseNotesForMilestone = false;

		/**
		 * If set to {@code false}, will not update Spring Guides for a release train.
		 */
		private boolean updateSpringGuides = false;

		/**
		 * If set to {@code false}, will not update start.spring.io for a release train.
		 */
		private boolean updateStartSpringIo = false;

		/**
		 * If set to {@code false}, will not update the Spring Project for a release
		 * train. E.g. for Spring Cloud will not update https://cloud.spring.io .
		 */
		private boolean updateSpringProject = false;

		/**
		 * If set to {@code false}, will not update the test samples.
		 */
		private boolean runUpdatedSamples = false;

		/**
		 * If set to {@code false}, will not update the release train docs.
		 */
		private boolean updateReleaseTrainDocs = false;

		// TODO: Spring Cloud specific?
		/**
		 * If set to {@code false}, will not clone and update the release train wiki.
		 */
		private boolean updateReleaseTrainWiki = false;

		/**
		 * If set to {@code false}, will not clone and update the samples for all
		 * projects.
		 */
		private boolean updateAllTestSamples = false;

		/**
		 * Project to urls mapping. For each project will clone the test project and will
		 * update its versions.
		 */
		private Map<String, List<String>> allTestSampleUrls = new HashMap<>();

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

		public String getReleaseTrainBranch() {
			return this.releaseTrainBranch;
		}

		public void setReleaseTrainBranch(String releaseTrainBranch) {
			this.releaseTrainBranch = releaseTrainBranch;
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

		public boolean isUpdateGithubMilestones() {
			return this.updateGithubMilestones;
		}

		public void setUpdateGithubMilestones(boolean updateGithubMilestones) {
			this.updateGithubMilestones = updateGithubMilestones;
		}

		public boolean isCreateReleaseNotesForMilestone() {
			return createReleaseNotesForMilestone;
		}

		public void setCreateReleaseNotesForMilestone(boolean createReleaseNotesForMilestone) {
			this.createReleaseNotesForMilestone = createReleaseNotesForMilestone;
		}

		public String getGithubChangelogGeneratorUrl() {
			return githubChangelogGeneratorUrl;
		}

		public void setGithubChangelogGeneratorUrl(String githubChangelogGeneratorUrl) {
			this.githubChangelogGeneratorUrl = githubChangelogGeneratorUrl;
		}

		public String getOrgName() {
			return this.orgName;
		}

		public void setOrgName(String orgName) {
			this.orgName = orgName;
		}

		public String getCacheDirectory() {
			return cacheDirectory;
		}

		public void setCacheDirectory(String cacheDirectory) {
			this.cacheDirectory = cacheDirectory;
		}

		@Override
		public String toString() {
			return "Git{" + "releaseTrainBomUrl='" + this.releaseTrainBomUrl + '\'' + ", documentationUrl='"
					+ this.documentationUrl + '\'' + ", documentationBranch='" + this.documentationBranch + '\''
					+ ", releaseTrainBranch='" + this.releaseTrainBranch + '\'' + ", releaseTrainWikiUrl='"
					+ this.releaseTrainWikiUrl + '\'' + ", updateDocumentationRepo=" + this.updateDocumentationRepo
					+ ", springProjectUrl=" + this.springProjectUrl + ", springProjectBranch="
					+ this.springProjectBranch + ", releaseTrainWikiPagePrefix=" + this.releaseTrainWikiPagePrefix
					+ ", cloneDestinationDir='" + this.cloneDestinationDir + '\'' + ", fetchVersionsFromGit="
					+ this.fetchVersionsFromGit + ", numberOfCheckedMilestones=" + this.numberOfCheckedMilestones
					+ ", updateSpringGuides=" + this.updateSpringGuides + ", updateSpringProject="
					+ this.updateSpringProject + ", sampleUrlsSize=" + this.allTestSampleUrls.size() + '}';
		}

		private static String temporaryDirectory() {
			try {
				return Files.createTempDirectory("github-cache").toAbsolutePath().toString();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	public static class Pom implements Serializable {

		/**
		 * Which branch of release train BOM should be checked out. Defaults to
		 * {@code main}.
		 */
		private String branch = "main";

		/**
		 * Subfolder of the pom that contains the {@code spring-boot-starer-parent}
		 * dependency.
		 */
		private String pomWithBootStarterParent;

		/**
		 * Subfolder of the pom that contains the versions for the release train.
		 */
		@NotBlank
		private String thisTrainBom;

		/**
		 * The pattern to match a version property in a BOM. Remember to catch the
		 * dependency name in a group. E.g. "^(spring-cloud-.*)\\.version$".
		 */
		@NotBlank
		private String bomVersionPattern;

		/**
		 * List of regular expressions of ignored poms. Defaults to test projects and
		 * samples.
		 */
		@SuppressWarnings("unchecked")
		private List<String> ignoredPomRegex = Collections.singletonList("^.*\\.git/.*$");

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
			return "Pom{" + "branch='" + this.branch + '\'' + ", pomWithBootStarterParent='"
					+ this.pomWithBootStarterParent + '\'' + ", thisTrainBom='" + this.thisTrainBom + '\''
					+ ", bomVersionPattern='" + this.bomVersionPattern + '\'' + ", ignoredPomRegex="
					+ this.ignoredPomRegex + '}';
		}

	}

	public static class Maven implements Serializable, Command {

		/**
		 * Placeholder for system properties.
		 */
		public static final String SYSTEM_PROPS_PLACEHOLDER = "{{systemProps}}";

		/**
		 * Placeholder for profile. If not used, profile will be appended at the end.
		 */
		public static final String PROFILE_PROPS_PLACEHOLDER = "{{profile}}";

		/**
		 * Command to be executed to build the project. If present "{{version}}" will be
		 * replaced by the provided version. "{{nextVersion}}" with the bumped snapshot
		 * version and "{{oldVersion}}" with the version before version updating.
		 **/
		private String buildCommand = "./mvnw clean install -B -Pdocs {{systemProps}}";

		/**
		 * Command to be executed to deploy a built project. If present "{{version}}" will
		 * be replaced by the provided version. "{{nextVersion}}" with the bumped snapshot
		 * version and "{{oldVersion}}" with the version before version updating.
		 **/
		private String deployCommand = "./mvnw deploy -DskipTests -B -Pfast,deploy {{systemProps}}";

		/**
		 * Command to be executed to build and deploy guides project only. If present
		 * "{{version}}" will be replaced by the provided version. "{{nextVersion}}" with
		 * the bumped snapshot version and "{{oldVersion}}" with the version before
		 * version updating.
		 **/
		private String deployGuidesCommand = "./mvnw clean verify deploy -B -Pguides,integration -pl guides {{systemProps}}";

		/**
		 * Command to be executed to publish documentation. If present "{{version}}" will
		 * be replaced by the provided version.
		 */
		private String publishDocsCommand = "./mvnw deploy -DskipTests -B -Pfast,deploy,docs -pl docs {{systemProps}}";

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

		@Override
		public String getBuildCommand() {
			return this.buildCommand;
		}

		@Override
		public void setBuildCommand(String buildCommand) {
			this.buildCommand = buildCommand;
		}

		public long getWaitTimeInMinutes() {
			return this.waitTimeInMinutes;
		}

		public void setWaitTimeInMinutes(long waitTimeInMinutes) {
			this.waitTimeInMinutes = waitTimeInMinutes;
		}

		@Override
		public String getDeployCommand() {
			return this.deployCommand;
		}

		@Override
		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		@Override
		public String getDeployGuidesCommand() {
			return this.deployGuidesCommand;
		}

		@Override
		public void setDeployGuidesCommand(String deployGuidesCommand) {
			this.deployGuidesCommand = deployGuidesCommand;
		}

		@Override
		public String getPublishDocsCommand() {
			return this.publishDocsCommand;
		}

		@Override
		public void setPublishDocsCommand(String publishDocsCommand) {
			this.publishDocsCommand = publishDocsCommand;
		}

		@Override
		public String getGenerateReleaseTrainDocsCommand() {
			return this.generateReleaseTrainDocsCommand;
		}

		@Override
		public void setGenerateReleaseTrainDocsCommand(String generateReleaseTrainDocsCommand) {
			this.generateReleaseTrainDocsCommand = generateReleaseTrainDocsCommand;
		}

		@Override
		public String getSystemProperties() {
			return this.systemProperties;
		}

		@Override
		public void setSystemProperties(String systemProperties) {
			this.systemProperties = systemProperties;
		}

		@Override
		public String toString() {
			return "Maven{" + "buildCommand='" + this.buildCommand + '\'' + ", deployCommand='" + this.deployCommand
					+ '\'' + ", publishDocsCommand=" + this.publishDocsCommand + "generateReleaseTrainDocsCommand='"
					+ this.generateReleaseTrainDocsCommand + '\'' + ", waitTimeInMinutes=" + this.waitTimeInMinutes
					+ '}';
		}

	}

	public static class Bash implements Serializable, Command {

		/**
		 * Placeholder for system properties.
		 */
		public static final String SYSTEM_PROPS_PLACEHOLDER = "{{systemProps}}";

		/**
		 * Command to be executed to build the project. If present "{{version}}" will be
		 * replaced by the provided version. "{{nextVersion}}" with the bumped snapshot
		 * version and "{{oldVersion}}" with the version before version updating.
		 */
		private String buildCommand = "echo \"{{systemProps}}\"";

		/**
		 * Command to be executed to deploy a built project. If present "{{version}}" will
		 * be replaced by the provided version. "{{nextVersion}}" with the bumped snapshot
		 * version and "{{oldVersion}}" with the version before version updating.
		 */
		private String deployCommand = "echo \"{{systemProps}}\"";

		/**
		 * Command to be executed to build and deploy guides project only. If present
		 * "{{version}}" will be replaced by the provided version. "{{nextVersion}}" with
		 * the bumped snapshot version and "{{oldVersion}}" with the version before
		 * version updating.
		 */
		private String deployGuidesCommand = "echo \"{{systemProps}}\"";

		/**
		 * Command to be executed to publish documentation. If present "{{version}}" will
		 * be replaced by the provided version. "{{nextVersion}}" with the bumped snapshot
		 * version and "{{oldVersion}}" with the version before version updating.
		 */
		private String publishDocsCommand = "mkdir -p target && echo \"{{version}}\"";

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

		@Override
		public String getBuildCommand() {
			return this.buildCommand;
		}

		@Override
		public void setBuildCommand(String buildCommand) {
			this.buildCommand = buildCommand;
		}

		public long getWaitTimeInMinutes() {
			return this.waitTimeInMinutes;
		}

		public void setWaitTimeInMinutes(long waitTimeInMinutes) {
			this.waitTimeInMinutes = waitTimeInMinutes;
		}

		@Override
		public String getDeployCommand() {
			return this.deployCommand;
		}

		@Override
		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		@Override
		public String getDeployGuidesCommand() {
			return this.deployGuidesCommand;
		}

		@Override
		public void setDeployGuidesCommand(String deployGuidesCommand) {
			this.deployGuidesCommand = deployGuidesCommand;
		}

		@Override
		public String getPublishDocsCommand() {
			return this.publishDocsCommand;
		}

		@Override
		public void setPublishDocsCommand(String publishDocsCommand) {
			this.publishDocsCommand = publishDocsCommand;
		}

		@Override
		public String getGenerateReleaseTrainDocsCommand() {
			return this.generateReleaseTrainDocsCommand;
		}

		@Override
		public void setGenerateReleaseTrainDocsCommand(String generateReleaseTrainDocsCommand) {
			this.generateReleaseTrainDocsCommand = generateReleaseTrainDocsCommand;
		}

		@Override
		public String getSystemProperties() {
			return this.systemProperties;
		}

		@Override
		public void setSystemProperties(String systemProperties) {
			this.systemProperties = systemProperties;
		}

		@Override
		public String toString() {
			return "Bash{" + "buildCommand='" + this.buildCommand + '\'' + ", deployCommand='" + this.deployCommand
					+ '\'' + ", publishDocsCommand=" + this.publishDocsCommand + "generateReleaseTrainDocsCommand='"
					+ this.generateReleaseTrainDocsCommand + '\'' + ", waitTimeInMinutes=" + this.waitTimeInMinutes
					+ '}';
		}

	}

	public static class Gradle implements Serializable, Command {

		/**
		 * Placeholder for system properties.
		 */
		public static final String SYSTEM_PROPS_PLACEHOLDER = "{{systemProps}}";

		/**
		 * A mapping that should be applied to {@code gradle.properties} in order to
		 * perform a substitution of properties. The mapping is from a property inside
		 * {@code gradle.properties} to the projects name. Example.
		 * <p>
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
		private List<String> ignoredGradleRegex = new ArrayList<>();

		/**
		 * Command to be executed to build the project If present "{{version}}" will be
		 * replaced by the provided version. "{{nextVersion}}" with the bumped snapshot
		 * version and "{{oldVersion}}" with the version before version updating.
		 **/
		private String buildCommand = "./gradlew clean build publishToMavenLocal --console=plain -PnextVersion={{nextVersion}} -PoldVersion={{oldVersion}} -PcurrentVersion={{version}} {{systemProps}}";

		/**
		 * Command to be executed to deploy a built project.
		 */
		private String deployCommand = "./gradlew publish --console=plain -PnextVersion={{nextVersion}} -PoldVersion={{oldVersion}} -PcurrentVersion={{version}} {{systemProps}}";

		/**
		 * Command to be executed to build and deploy guides project only.
		 */
		private String deployGuidesCommand = "./gradlew clean build deployGuides --console=plain -PnextVersion={{nextVersion}} -PoldVersion={{oldVersion}} -PcurrentVersion={{version}} {{systemProps}}";

		/**
		 * Command to be executed to publish documentation. If present "{{version}}" will
		 * be replaced by the provided version.
		 */
		private String publishDocsCommand = "./gradlew publishDocs --console=plain -PnextVersion={{nextVersion}} -PoldVersion={{oldVersion}} -PcurrentVersion={{version}} {{systemProps}}";

		/**
		 * Command to be executed to generate release train documentation.
		 */
		private String generateReleaseTrainDocsCommand = "./gradlew generateReleaseTrainDocs --console=plain -PnextVersion={{nextVersion}} -PoldVersion={{oldVersion}} -PcurrentVersion={{version}} {{systemProps}}";

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

		@Override
		public String getBuildCommand() {
			return this.buildCommand;
		}

		@Override
		public void setBuildCommand(String buildCommand) {
			this.buildCommand = buildCommand;
		}

		public long getWaitTimeInMinutes() {
			return this.waitTimeInMinutes;
		}

		public void setWaitTimeInMinutes(long waitTimeInMinutes) {
			this.waitTimeInMinutes = waitTimeInMinutes;
		}

		@Override
		public String getDeployCommand() {
			return this.deployCommand;
		}

		@Override
		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		@Override
		public String getDeployGuidesCommand() {
			return this.deployGuidesCommand;
		}

		@Override
		public void setDeployGuidesCommand(String deployGuidesCommand) {
			this.deployGuidesCommand = deployGuidesCommand;
		}

		@Override
		public String getPublishDocsCommand() {
			return this.publishDocsCommand;
		}

		@Override
		public void setPublishDocsCommand(String publishDocsCommand) {
			this.publishDocsCommand = publishDocsCommand;
		}

		@Override
		public String getGenerateReleaseTrainDocsCommand() {
			return this.generateReleaseTrainDocsCommand;
		}

		@Override
		public void setGenerateReleaseTrainDocsCommand(String generateReleaseTrainDocsCommand) {
			this.generateReleaseTrainDocsCommand = generateReleaseTrainDocsCommand;
		}

		@Override
		public String getSystemProperties() {
			return this.systemProperties;
		}

		@Override
		public void setSystemProperties(String systemProperties) {
			this.systemProperties = systemProperties;
		}

		public Map<String, String> getGradlePropsSubstitution() {
			return this.gradlePropsSubstitution;
		}

		public void setGradlePropsSubstitution(Map<String, String> gradlePropsSubstitution) {
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
					.add("ignoredGradleRegex=" + ignoredGradleRegex).add("buildCommand='" + buildCommand + "'")
					.add("deployCommand='" + deployCommand + "'")
					.add("deployGuidesCommand='" + deployGuidesCommand + "'")
					.add("publishDocsCommand=" + publishDocsCommand)
					.add("generateReleaseTrainDocsCommand='" + generateReleaseTrainDocsCommand + "'")
					.add("systemProperties='" + systemProperties + "'").add("waitTimeInMinutes=" + waitTimeInMinutes)
					.toString();
		}

	}

	public static class Sagan implements Serializable {

		/**
		 * If set to {@code false} will not update Sagan.
		 */
		private boolean updateSagan = false;

		/**
		 * URL to the Sagan API.
		 */
		private String baseUrl = "https://api.spring.io";

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
		 * Should template generation be enabled.
		 */
		private boolean enabled;

		/**
		 * Folder in which blog, email etc. templates are stored.
		 */
		private String templateFolder;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

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
		private String bomName;

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
			return "Versions{" + "allVersionsFileUrl='" + this.allVersionsFileUrl + '\'' + ", bomName='" + this.bomName
					+ '\'' + '}';
		}

	}

}
