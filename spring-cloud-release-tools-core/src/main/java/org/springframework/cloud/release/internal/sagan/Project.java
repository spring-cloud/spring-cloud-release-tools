package org.springframework.cloud.release.internal.sagan;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Project {
	public String id = "";
	public String name = "";
	public String repoUrl = "";
	public String siteUrl = "";
	public String category = "";
	public String stackOverflowTags;
	public List<Release> projectReleases = new ArrayList<>();
	public List<String> stackOverflowTagList = new ArrayList<>();
	public Boolean aggregator;

	@Override public String toString() {
		return "Project{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", repoUrl='"
				+ repoUrl + '\'' + ", siteUrl='" + siteUrl + '\'' + ", category='"
				+ category + '\'' + ", stackOverflowTags='" + stackOverflowTags + '\''
				+ ", projectReleases=" + projectReleases + ", stackOverflowTagList="
				+ stackOverflowTagList + ", aggregator=" + aggregator + '}';
	}
}
