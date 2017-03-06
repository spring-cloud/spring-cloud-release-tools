package org.springframework.cloud.release.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Marcin Grzejszczak
 */
@ConfigurationProperties("releaser")
public class ReleaserProperties {

	/**
	 * URL to Spring Cloud Release Git repository
	 */
	private String springCloudReleaseGitUrl = "https://github.com/spring-cloud/spring-cloud-release.git";

	/**
	 * Where should the Spring Cloud Release repo get cloned to. If {@code null} defaults to a temporary directory
	 */
	private String cloneDestinationDir;

	/**
	 * Which branch of Spring Cloud Release should be checked out. Defaults to {@code master}
	 */
	private String branch = "master";

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

	public String getBranch() {
		return this.branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}
}
