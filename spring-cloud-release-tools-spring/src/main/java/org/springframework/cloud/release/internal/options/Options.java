package org.springframework.cloud.release.internal.options;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marcin Grzejszczak
 */
public class Options {
	public Boolean fullRelease = true;
	public Boolean interactive = true;
	public List<String> taskNames = new ArrayList<>();
	public String startFrom = "";
	public String range = "";

	Options(Boolean fullRelease, Boolean interactive, List<String> taskNames, String startFrom,
			String range) {
		this.fullRelease = fullRelease;
		this.interactive = interactive;
		this.taskNames = taskNames;
		this.startFrom = startFrom;
		this.range = range;
	}
}
