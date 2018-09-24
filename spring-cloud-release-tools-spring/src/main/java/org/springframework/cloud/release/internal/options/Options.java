package org.springframework.cloud.release.internal.options;

import java.util.List;
import java.util.stream.Collectors;

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
		this.taskNames = taskNames.stream()
				.map(this::removeQuotingChars).collect(
				Collectors.toList());
		this.startFrom = removeQuotingChars(startFrom);
		this.range = removeQuotingChars(range);
	}

	private String removeQuotingChars(String string) {
		if (string != null && string.startsWith("'") && string.endsWith("'")) {
			return string.substring(1, string.length() - 1);
		}
		return string;
	}

	@Override public String toString() {
		return "Options{" + "metaRelease=" + this.metaRelease + ", fullRelease=" + this.fullRelease
				+ ", interactive=" + this.interactive + ", taskNames=" + this.taskNames
				+ ", startFrom='" + this.startFrom + '\'' + ", range='" + this.range + '\'' + '}';
	}
}
