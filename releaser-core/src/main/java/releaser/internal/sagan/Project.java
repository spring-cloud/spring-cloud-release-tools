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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.core.style.ToStringCreator;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project {

	private String name;

	private String slug;

	private String repositoryUrl;

	private String status;

	public List<Release> releases;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	public void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<Release> getReleases() {
		return this.releases;
	}

	public void setReleases(List<Release> releases) {
		this.releases = releases;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", name).append("slug", slug)
				.append("repositoryUrl", repositoryUrl).append("status", status).append("releases", releases)
				.toString();

	}

}
