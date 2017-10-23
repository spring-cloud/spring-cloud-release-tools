package org.springframework.cloud.release.internal.sagan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Release {
	public String releaseStatus = "";
	public String refDocUrl = "";
	public String apiDocUrl = "";
	public String groupId = "";
	public String artifactId = "";
	public Repository repository;
	public String version = "";
	public boolean current;
	public boolean generalAvailability;
	public boolean preRelease;
	public String versionDisplayName = "";
	public boolean snapshot;

	@Override public String toString() {
		return "Release{" + "releaseStatus='" + releaseStatus + '\'' + ", refDocUrl='"
				+ refDocUrl + '\'' + ", apiDocUrl='" + apiDocUrl + '\'' + ", groupId='"
				+ groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", repository="
				+ repository + ", version='" + version + '\'' + ", current=" + current
				+ ", generalAvailability=" + generalAvailability + ", preRelease="
				+ preRelease + ", versionDisplayName='" + versionDisplayName + '\''
				+ ", snapshot=" + snapshot + '}';
	}
}
