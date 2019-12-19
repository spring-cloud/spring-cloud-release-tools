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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project {

	public String id;

	public String name;

	public String repoUrl;

	public String siteUrl;

	public String category;

	public String stackOverflowTags;

	public List<Release> projectReleases = new ArrayList<>();

	public Object projectSamples = new Object();

	public Object nonMostCurrentReleases = new ArrayList<>();

	public List<String> stackOverflowTagList = new ArrayList<>();

	public Object mostCurrentRelease = new MostCurrentRelease();

	public Boolean aggregator;

	public String rawBootConfig;

	public String rawOverview;

	public int displayOrder = Integer.MAX_VALUE;

	public boolean topLevelProject = true;

	@Override
	public String toString() {
		return "Project{" + "id='" + this.id + '\'' + ", name='" + this.name + '\''
				+ ", repoUrl='" + this.repoUrl + '\'' + ", siteUrl='" + this.siteUrl
				+ '\'' + ", category='" + this.category + '\'' + ", stackOverflowTags='"
				+ this.stackOverflowTags + '\'' + ", projectReleases="
				+ this.projectReleases + ", stackOverflowTagList="
				+ this.stackOverflowTagList + ", aggregator=" + this.aggregator
				+ ", rawBootConfig='" + this.rawBootConfig + '\'' + ", rawOverview='"
				+ this.rawOverview + '\'' + '}';
	}

	static class MostCurrentRelease {

		public boolean present;

	}

}
