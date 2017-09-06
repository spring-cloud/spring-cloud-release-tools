package org.springframework.cloud.release.internal.sagan;

import java.util.List;

/**
 * @author Marcin Grzejszczak
 */
class ProjectMetadata {
	private String id = "";
	private String name = "";
	private String repoUrl = "";
	private String siteUrl = "";
	private String category = "";
	private String stackOverflowTags;
	private boolean aggregator;
	private List<String> stackOverflowTagList;
}
