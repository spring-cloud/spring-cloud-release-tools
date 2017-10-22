package org.springframework.cloud.release.internal.sagan;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marcin Grzejszczak
 */
public class Project {
	public String id = "";
	public String name = "";
	public String repoUrl = "";
	public String siteUrl = "";
	public String category = "";
	public String stackOverflowTags;
	public List<Release> projectReleases = new ArrayList<>();
	public List<String> stackOverflowTagList = new ArrayList<>();
	public boolean aggregator;
}
