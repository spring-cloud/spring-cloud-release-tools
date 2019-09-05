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

package org.springframework.cloud.release.cloud.docs;

import java.io.File;

import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;

public class SpringCloudDocsAccessor {

	public static CustomProjectDocumentationUpdater updater(ProjectGitHandler handler) {
		return new SpringCloudCustomProjectDocumentationUpdater(handler);
	}

	public static CustomProjectDocumentationUpdater testUpdater(ProjectGitHandler handler,
			String version) {
		return new TestCustomProjectDocumentationUpdater(handler, version);
	}

}

class TestCustomProjectDocumentationUpdater
		extends SpringCloudCustomProjectDocumentationUpdater {

	private final String version;

	public TestCustomProjectDocumentationUpdater(ProjectGitHandler gitHandler,
			String version) {
		super(gitHandler);
		this.version = version;
	}

	@Override
	String readIndexHtmlContents(File indexHtml) {
		return response();
	}

	private String response() {
		return "<!DOCTYPE HTML>\n" + "\n" + "<meta charset=\"UTF-8\">\n"
				+ "<meta http-equiv=\"refresh\" content=\"1; url=https://cloud.spring.io/spring-cloud-static/"
				+ this.version + "/\">\n" + "\n" + "<script>\n"
				+ "  window.location.href = \"https://cloud.spring.io/spring-cloud-static/"
				+ this.version + "/\"\n" + "</script>\n" + "\n"
				+ "<title>Page Redirection</title>\n" + "\n"
				+ "<!-- Note: don't tell people to `click` the link, just tell them that it is a link. -->\n"
				+ "If you are not redirected automatically, follow the <a href='https://cloud.spring.io/spring-cloud-static/"
				+ this.version + "/'>link to latest release</a>\n";
	}

}