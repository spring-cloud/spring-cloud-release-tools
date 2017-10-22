package org.springframework.cloud.release.internal.sagan;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseUpdate {
	public String groupId = "";
	public String artifactId = "";
	public String version = "";
	public String releaseStatus = "";
	public String refDocUrl = "";
	public String apiDocUrl = "";

	@Override public String toString() {
		return "ReleaseUpdate{" + "groupId='" + groupId + '\'' + ", artifactId='"
				+ artifactId + '\'' + ", version='" + version + '\'' + ", releaseStatus='"
				+ releaseStatus + '\'' + ", refDocUrl='" + refDocUrl + '\''
				+ ", apiDocUrl='" + apiDocUrl + '\'' + '}';
	}
}
