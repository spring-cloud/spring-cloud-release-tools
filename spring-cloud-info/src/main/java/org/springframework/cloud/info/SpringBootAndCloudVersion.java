/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.info;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * @author Ryan Baxter
 */
public class SpringBootAndCloudVersion {

	private String bootStartVersion;

	private ComparableVersion comparableBootStartVersion;

	private boolean startVersionInclusive;

	private String bootEndVersion;

	private ComparableVersion comparableBootEndVersion;

	private boolean endVersionInclusive;

	private String springCloudVersion;

	public SpringBootAndCloudVersion(String bootStartVersion,
			boolean statVersionInclusive, String bootEndVersion,
			boolean endVersionInclusive, String springCloudVersion) {
		this.bootEndVersion = bootEndVersion;
		this.comparableBootEndVersion = new ComparableVersion(bootEndVersion);
		this.endVersionInclusive = endVersionInclusive;
		this.bootStartVersion = bootStartVersion;
		this.comparableBootStartVersion = new ComparableVersion(bootStartVersion);
		this.startVersionInclusive = statVersionInclusive;
		this.springCloudVersion = springCloudVersion;
	}

	String getBootStartVersion() {
		return bootStartVersion;
	}

	void setBootStartVersion(String bootStartVersion) {
		this.bootStartVersion = bootStartVersion;
	}

	String getBootEndVersion() {
		return bootEndVersion;
	}

	void setBootEndVersion(String bootEndVersion) {
		this.bootEndVersion = bootEndVersion;
	}

	String getSpringCloudVersion() {
		return springCloudVersion;
	}

	void setSpringCloudVersion(String springCloudVersion) {
		this.springCloudVersion = springCloudVersion;
	}

	boolean matchesSpringBootVersion(String versionToCheck) {
		return matchesSpringBootVersion(new ComparableVersion(versionToCheck));
	}

	boolean matchesSpringBootVersion(ComparableVersion versionToCheck) {
		int startVersionComparison = comparableBootStartVersion.compareTo(versionToCheck);
		int endVersionComparison = versionToCheck.compareTo(comparableBootEndVersion);
		return ((startVersionInclusive && startVersionComparison == 0)
				|| startVersionComparison <= -1)
				&& ((endVersionInclusive && endVersionComparison == 0)
						|| endVersionComparison <= -1);
	}

}
