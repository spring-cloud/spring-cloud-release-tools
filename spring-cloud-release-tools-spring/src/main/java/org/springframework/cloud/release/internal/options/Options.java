package org.springframework.cloud.release.internal.options;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marcin Grzejszczak
 */
public class Options {
	public Boolean interactive = true;
	public List<String> taskNames = new ArrayList<>();
	public String startFrom = "";
	public String range = "";
	public String releaserBranch = "";

	Options(Boolean interactive, List<String> taskNames, String startFrom,
			String range, String releaserBranch) {
		this.interactive = interactive;
		this.taskNames = taskNames;
		this.startFrom = startFrom;
		this.range = range;
		this.releaserBranch = releaserBranch;
	}
}
