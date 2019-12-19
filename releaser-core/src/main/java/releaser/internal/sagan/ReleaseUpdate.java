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

package releaser.internal.sagan;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseUpdate {

	public String groupId = "";

	public String artifactId = "";

	public String version = "";

	public String releaseStatus = "";

	public String refDocUrl = "";

	public String apiDocUrl = "";

	public Boolean current;

	public Repository repository;

	@Override
	public String toString() {
		return "ReleaseUpdate{" + "groupId='" + this.groupId + '\'' + ", artifactId='"
				+ this.artifactId + '\'' + ", version='" + this.version + '\''
				+ ", releaseStatus='" + this.releaseStatus + '\'' + ", refDocUrl='"
				+ this.refDocUrl + '\'' + ", apiDocUrl='" + this.apiDocUrl + '\''
				+ ", repository=" + this.repository + '}';
	}

}
