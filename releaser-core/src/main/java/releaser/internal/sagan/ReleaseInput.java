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

import org.springframework.core.style.ToStringCreator;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseInput {

	private String version = "";

	private String referenceDocUrl = "";

	private String apiDocUrl = "";

	private boolean isAntora = true;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getReferenceDocUrl() {
		return this.referenceDocUrl;
	}

	public void setReferenceDocUrl(String referenceDocUrl) {
		this.referenceDocUrl = referenceDocUrl;
	}

	public String getApiDocUrl() {
		return this.apiDocUrl;
	}

	public void setApiDocUrl(String apiDocUrl) {
		this.apiDocUrl = apiDocUrl;
	}

	public boolean isAntora() {
		return this.isAntora;
	}

	public void setAntora(boolean antora) {
		this.isAntora = antora;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("version", version).append("referenceDocUrl", referenceDocUrl)
				.append("apiDocUrl", apiDocUrl).append("isAntora", isAntora).toString();

	}

}
