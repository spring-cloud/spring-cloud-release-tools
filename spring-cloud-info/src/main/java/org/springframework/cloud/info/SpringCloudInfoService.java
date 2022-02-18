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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.springframework.cloud.info.exceptions.SpringCloudMilestoneNotFoundException;
import org.springframework.cloud.info.exceptions.SpringCloudVersionNotFoundException;

/**
 * @author Ryan Baxter
 */
public interface SpringCloudInfoService {

	SpringCloudVersion getSpringCloudVersion(String bootVersion) throws SpringCloudVersionNotFoundException;

	Collection<String> getSpringCloudVersions() throws IOException;

	Map<String, String> getReleaseVersions(String bomVersion) throws SpringCloudVersionNotFoundException, IOException;

	Collection<String> getMilestones() throws IOException;

	Milestone getMilestoneDueDate(String name) throws SpringCloudMilestoneNotFoundException, IOException;

	class Milestone {

		private String dueDate;

		public Milestone() {
		}

		public Milestone(String dueDate) {
			this.dueDate = dueDate;
		}

		public String getDueDate() {
			return dueDate;
		}

		public void setDueDate(String dueDate) {
			this.dueDate = dueDate;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Milestone milestone = (Milestone) o;
			return Objects.equals(getDueDate(), milestone.getDueDate());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getDueDate());
		}

	}

	class SpringCloudVersion {

		private String version;

		public SpringCloudVersion() {
		}

		public SpringCloudVersion(String version) {
			this.version = version;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SpringCloudVersion that = (SpringCloudVersion) o;
			return Objects.equals(getVersion(), that.getVersion());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getVersion());
		}

	}

}
