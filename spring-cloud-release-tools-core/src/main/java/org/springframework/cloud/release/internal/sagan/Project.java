package org.springframework.cloud.release.internal.sagan;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
		return "Project{" + "id='" + this.id + '\'' + ", name='" + this.name + '\'' + ", repoUrl='"
				+ this.repoUrl + '\'' + ", siteUrl='" + this.siteUrl + '\'' + ", category='"
				+ this.category + '\'' + ", stackOverflowTags='" + this.stackOverflowTags + '\''
				+ ", projectReleases=" + this.projectReleases + ", stackOverflowTagList="
				+ this.stackOverflowTagList + ", aggregator=" + this.aggregator + '}';
	}
}
