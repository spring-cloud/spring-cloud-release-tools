package org.springframework.cloud.release.internal.options;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marcin Grzejszczak
 */
public class Options {
	public Boolean metaRelease;
	public Boolean fullRelease;
	public Boolean interactive;
	public List<String> taskNames;
	public String startFrom = "";
	public String range = "";

	Options(Boolean metaRelease, Boolean fullRelease,
			Boolean interactive, List<String> taskNames, String startFrom,
			String range) {
		this.metaRelease = metaRelease;
		this.fullRelease = fullRelease;
		this.interactive = interactive;
		this.taskNames = taskNames;
		this.startFrom = startFrom;
		this.range = range;
	}
}
