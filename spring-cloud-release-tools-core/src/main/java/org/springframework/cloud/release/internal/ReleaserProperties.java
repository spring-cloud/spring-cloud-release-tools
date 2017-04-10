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
package org.springframework.cloud.release.internal;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author Marcin Grzejszczak
 */
@ConfigurationProperties("releaser")
public class ReleaserProperties {

	/**
	 * By default Releaser assumes running the program from the current working directory.
	 * If you want to change this behaviour - just change this value.
	 */
	private String workingDir;

	private Git git = new Git();

	private Pom pom = new Pom();

	private Maven maven = new Maven();

	public static class Git {

		/**
		 * URL to Spring Cloud Release Git repository
		 */
		private String springCloudReleaseGitUrl = "https://github.com/spring-cloud/spring-cloud-release";

		/**
		 * Where should the Spring Cloud Release repo get cloned to. If {@code null} defaults to a temporary directory
		 */
		private String cloneDestinationDir;

		/**
		 * GitHub OAuth token to be used to interact with GitHub repo
		 */
		private String oauthToken;

		public String getSpringCloudReleaseGitUrl() {
			return this.springCloudReleaseGitUrl;
		}

		public void setSpringCloudReleaseGitUrl(String springCloudReleaseGitUrl) {
			this.springCloudReleaseGitUrl = springCloudReleaseGitUrl;
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
	}

	public static class Pom {

		/**
		 * Which branch of Spring Cloud Release should be checked out. Defaults to {@code master}
		 */
		private String branch = "master";

		/**
		 * List of regular expressions of ignored poms. Defaults to test projects and samples.
		 */
		@SuppressWarnings("unchecked")
		private List<String> ignoredPomRegex = Arrays.asList(new String[] {
				"^.*spring-cloud-contract-maven-plugin/src/test/projects/.*$",
				"^.*samples/standalone.*$"
		});

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
	}

	public static class Maven {

		/**
		 * Command to be executed to build the project
		 */
		private String buildCommand = "./mvnw clean install -Pdocs";

		/**
		 * Command to be executed to deploy a built project
		 */
		private String deployCommand = "./mvnw deploy -DskipTests -Pfast";

		/**
		 * Command to be executed to deploy a built project. If present "{{version}}" will be replaced by the
		 * provided version
		 */
		private String[] publishDocsCommands = {
				"mkdir -p target",
				"wget https://raw.githubusercontent.com/spring-cloud/spring-cloud-build/master/docs/src/main/asciidoc/ghpages.sh -O target/gh-pages.sh",
				"chmod +x target/gh-pages.sh",
				"./target/gh-pages.sh -v {{version}} -c"
		};

		/**
		 * Max wait time in minutes for the process to finish
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
			return deployCommand;
		}

		public void setDeployCommand(String deployCommand) {
			this.deployCommand = deployCommand;
		}

		public String[] getPublishDocsCommands() {
			return this.publishDocsCommands;
		}

		public void setPublishDocsCommands(String[] publishDocsCommands) {
			this.publishDocsCommands = publishDocsCommands;
		}
	}

	public String getWorkingDir() {
		return StringUtils.hasText(this.workingDir) ?
				this.workingDir : System.getProperty("user.dir");
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
}
