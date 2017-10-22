package org.springframework.cloud.release.internal.sagan;

/**
 * @author Marcin Grzejszczak
 */
public interface SaganClient {
	Project getProject(String projectName);

	Release getRelease(String projectName, String releaseVersion);

	Release createOrUpdateRelease(String projectName, ReleaseUpdate releaseUpdate);
}
