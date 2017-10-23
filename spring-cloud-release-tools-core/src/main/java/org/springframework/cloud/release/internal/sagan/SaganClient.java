package org.springframework.cloud.release.internal.sagan;

import java.util.List;

/**
 * @author Marcin Grzejszczak
 */
public interface SaganClient {
	Project getProject(String projectName);

	Release getRelease(String projectName, String releaseVersion);

	Project updateRelease(String projectName, List<ReleaseUpdate> releaseUpdate);
}
