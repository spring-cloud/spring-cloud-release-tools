package org.springframework.cloud.release.internal.options;

import java.util.ArrayList;
import java.util.List;

public class OptionsBuilder {
	private Boolean fullRelease = false;
	private Boolean interactive = true;
	private List<String> taskNames = new ArrayList<>();
	private String startFrom = "";
	private String range = "";

	public OptionsBuilder fullRelease(Boolean fullRelease) {
		this.fullRelease = fullRelease;
		return this;
	}

	public OptionsBuilder interactive(Boolean interactive) {
		this.interactive = interactive;
		return this;
	}

	public OptionsBuilder taskNames(List<String> taskNames) {
		this.taskNames = taskNames;
		return this;
	}

	public OptionsBuilder startFrom(String startFrom) {
		this.startFrom = startFrom;
		return this;
	}

	public OptionsBuilder range(String range) {
		this.range = range;
		return this;
	}

	public Options options() {
		return new Options(this.fullRelease, this.interactive, this.taskNames, this.startFrom,
				this.range);
	}
}