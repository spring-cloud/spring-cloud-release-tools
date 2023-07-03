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

package releaser.internal.template;

import java.io.File;
import java.io.IOException;

import com.github.jknack.handlebars.Template;
import releaser.internal.ReleaserProperties;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.project.Projects;
import releaser.internal.tech.HandlebarsHelper;

/**
 * @author Marcin Grzejszczak
 */
public class TemplateGenerator {

	private static final String EMAIL_TEMPLATE = "email";

	private static final String BLOG_TEMPLATE = "blog";

	private static final String TWITTER_TEMPLATE = "tweet";

	private static final String RELEASE_NOTES_TEMPLATE = "notes";

	private final File emailOutput;

	private final File blogOutput;

	private final File tweetOutput;

	private final File releaseNotesOutput;

	private final ProjectGitHubHandler handler;

	private final ReleaserProperties props;

	public TemplateGenerator(ReleaserProperties props, ProjectGitHubHandler handler) {
		this.props = props;
		this.handler = handler;
		this.emailOutput = new File("target/email.txt");
		this.blogOutput = new File("target/blog.md");
		this.tweetOutput = new File("target/tweet.txt");
		this.releaseNotesOutput = new File("target/notes.md");
	}

	TemplateGenerator(ReleaserProperties props, File output, ProjectGitHubHandler handler) {
		this.props = props;
		this.emailOutput = output;
		this.blogOutput = output;
		this.tweetOutput = output;
		this.releaseNotesOutput = output;
		this.handler = handler;
	}

	public File email(Projects projects) {
		File emailOutput = file(this.emailOutput);
		String releaseVersion = parsedVersion(projects);
		Template template = template(EMAIL_TEMPLATE);
		return new EmailTemplateGenerator(template, releaseVersion, emailOutput).email();
	}

	private File file(File file) {
		try {
			if (file.exists()) {
				file.delete();
			}
			File parentFile = file.getParentFile();
			parentFile.mkdirs();
			if (!file.createNewFile()) {
				throw new IllegalStateException("Couldn't create a file [" + file + "]");
			}
			return file;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public File blog(Projects projects) {
		File blogOutput = file(this.blogOutput);
		String releaseVersion = parsedVersion(projects);
		Template template = template(BLOG_TEMPLATE);
		return new BlogTemplateGenerator(template, releaseVersion, blogOutput, projects, this.handler).blog();
	}

	public File tweet(Projects projects) {
		File output = file(this.tweetOutput);
		String releaseVersion = parsedVersion(projects);
		Template template = template(TWITTER_TEMPLATE);
		return new TwitterTemplateGenerator(template, releaseVersion, output).tweet();
	}

	public File releaseNotes(Projects projects) {
		File output = file(this.releaseNotesOutput);
		String releaseVersion = parsedVersion(projects);
		Template template = template(RELEASE_NOTES_TEMPLATE);
		return new ReleaseNotesTemplateGenerator(template, releaseVersion, output, projects, this.handler)
				.releaseNotes();
	}

	private String parsedVersion(Projects projects) {
		if (this.props.getMetaRelease().isEnabled()) {
			return projects.releaseTrain(this.props).version;
		}
		String version = this.props.getPom().getBranch();
		if (version.startsWith("v")) {
			return version.substring(1);
		}
		return version;
	}

	private Template template(String template) {
		return HandlebarsHelper.template(this.props.getTemplate().getTemplateFolder(), template);
	}

}
