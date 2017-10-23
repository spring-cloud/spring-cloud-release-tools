package org.springframework.cloud.release.internal.sagan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ReleaseUpdate {
	public String groupId = "";
	public String artifactId = "";
	public String version = "";
	public String releaseStatus = "";
	public String refDocUrl = "";
	public String apiDocUrl = "";
	public Boolean current;
	public Repository repository;

	@Override public String toString() {
		return "ReleaseUpdate{" + "groupId='" + groupId + '\'' + ", artifactId='"
				+ artifactId + '\'' + ", version='" + version + '\'' + ", releaseStatus='"
				+ releaseStatus + '\'' + ", refDocUrl='" + refDocUrl + '\''
				+ ", apiDocUrl='" + apiDocUrl + '\'' + ", repository=" + repository + '}';
	}
}
