package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.io.IOException;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.template.TemplateGenerator;

/**
 * @author Marcin Grzejszczak
 */
public class TestDocumentationUpdater extends DocumentationUpdater {

	public TestDocumentationUpdater(ReleaserProperties properties,
			TestProjectDocumentationUpdater updater, TestReleaseContentsUpdater testRelease) {
		super(properties, updater, testRelease);
	}

	public static class TestReleaseContentsUpdater extends ReleaseTrainContentsUpdater {

		public TestReleaseContentsUpdater(ReleaserProperties properties, ProjectGitHandler handler, TemplateGenerator templateGenerator) {
			super(properties, handler, templateGenerator);
		}

		@Override
		public File updateProjectRepo(Projects projects) {
			return super.updateProjectRepo(projects);
		}
	}

	public static class TestProjectDocumentationUpdater extends ProjectDocumentationUpdater {

		private final String version;

		public TestProjectDocumentationUpdater(ReleaserProperties properties, ProjectGitHandler gitHandler, String version) {
			super(properties, gitHandler);
			this.version = version;
		}

		@Override
		String readIndexHtmlContents(File indexHtml) {
			return response();
		}

		private String response() {
			return "<!DOCTYPE HTML>\n"
					+ "\n"
					+ "<meta charset=\"UTF-8\">\n"
					+ "<meta http-equiv=\"refresh\" content=\"1; url=http://cloud.spring.io/spring-cloud-static/" + this.version + "/\">\n"
					+ "\n"
					+ "<script>\n"
					+ "  window.location.href = \"http://cloud.spring.io/spring-cloud-static/" + this.version + "/\"\n"
					+ "</script>\n"
					+ "\n"
					+ "<title>Page Redirection</title>\n"
					+ "\n"
					+ "<!-- Note: don't tell people to `click` the link, just tell them that it is a link. -->\n"
					+ "If you are not redirected automatically, follow the <a href='http://cloud.spring.io/spring-cloud-static/" + this.version + "/'>link to latest release</a>\n";
		}
	}
}


