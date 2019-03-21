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

package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import org.junit.Test;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class TemplateGeneratorTests {

	ReleaserProperties props = new ReleaserProperties();

	ProjectGitHandler handler = new ProjectGitHandler(this.props) {
		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			if (releaseVersion.projectName.equals("spring-cloud-foo")) {
				return "";
			}
			return "http://foo.bar.com";
		}
	};

	@Test
	public void should_generate_email_from_template_for_tag_with_v_prefix() {
		this.props.getPom().setBranch("vDalston.RELEASE");

		File generatedMail = new TemplateGenerator(this.props, this.handler)
				.email(new Projects());

		then(generatedMail).hasContent(expectedEmail());
	}

	@Test
	public void should_generate_email_from_template_when_output_folder_is_missing() {
		this.props.getPom().setBranch("vDalston.RELEASE");

		File generatedMail = new TemplateGenerator(this.props,
				new File("target/foo/bar/baz/template.txt"), this.handler)
						.email(new Projects());

		then(generatedMail).hasContent(expectedEmail());
	}

	@Test
	public void should_generate_email_from_template_for_tag_without_v_prefix() {
		this.props.getPom().setBranch("Dalston.RELEASE");

		File generatedMail = new TemplateGenerator(this.props, this.handler)
				.email(new Projects());

		then(generatedMail).hasContent(expectedEmail());
	}

	@Test
	public void should_generate_tweet_from_template_for_tag_with_v_prefix() {
		this.props.getPom().setBranch("vDalston.RELEASE");

		File generatedTweet = new TemplateGenerator(this.props, this.handler)
				.tweet(new Projects());

		then(generatedTweet).hasContent(expectedTweet());
	}

	@Test
	public void should_generate_tweet_from_template_for_tag_without_v_prefix() {
		this.props.getPom().setBranch("Dalston.RELEASE");

		File generatedTweet = new TemplateGenerator(this.props, this.handler)
				.tweet(new Projects());

		then(generatedTweet).hasContent(expectedTweet());
	}

	@Test
	public void should_generate_blog_from_template_for_tag_with_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("vDalston.RELEASE");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RELEASE"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RELEASE"));
				add(new ProjectVersion("spring-cloud-foo", "1.0.2.RELEASE"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("General Availability (RELEASE) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Maven Central]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth	| 1.0.0.RELEASE	| ([issues](http://foo.bar.com))")
				.contains("| Spring Cloud Foo\t| 1.0.2.RELEASE\t| &nbsp;")
				.contains("<version>Dalston.RELEASE</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.RELEASE'");
	}

	@Test
	public void should_generate_blog_from_template_for_tag_without_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("Dalston.RELEASE");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RELEASE"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RELEASE"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("General Availability (RELEASE) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Maven Central]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth\t| 1.0.0.RELEASE\t| ([issues](http://foo.bar.com))")
				.contains("<version>Dalston.RELEASE</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.RELEASE'");
	}

	@Test
	public void should_generate_sr_blog_from_template_for_tag_with_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("vDalston.SR1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RELEASE"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RELEASE"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("Service Release 1 (SR1) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Maven Central]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth	| 1.0.0.RELEASE	| ([issues](http://foo.bar.com))")
				.contains("<version>Dalston.SR1</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.SR1'");
	}

	@Test
	public void should_generate_sr_blog_from_template_for_tag_without_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("Dalston.SR1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RELEASE"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RELEASE"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("Service Release 1 (SR1) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Maven Central]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth	| 1.0.0.RELEASE	| ([issues](http://foo.bar.com))")
				.contains("<version>Dalston.SR1</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.SR1'");
	}

	@Test
	public void should_generate_milestone_blog_from_template_for_tag_with_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("vDalston.M1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.M1"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.M1"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("Milestone 1 (M1) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Spring Milestone]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth\t| 1.0.0.M1\t| ([issues](http://foo.bar.com))")
				.contains("<id>spring-milestones</id>")
				.contains("url 'http://repo.spring.io/milestone'")
				.contains("<version>Dalston.M1</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.M1'");
	}

	@Test
	public void should_generate_milestone_blog_from_template_for_tag_without_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("Dalston.M1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.M1"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.M1"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("Milestone 1 (M1) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Spring Milestone]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth\t| 1.0.0.M1\t| ([issues](http://foo.bar.com))")
				.contains("<id>spring-milestones</id>")
				.contains("url 'http://repo.spring.io/milestone'")
				.contains("<version>Dalston.M1</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.M1'");
	}

	@Test
	public void should_generate_rc_blog_from_template_for_tag_with_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("vDalston.RC1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RC1"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RC1"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("Release Candidate 1 (RC1) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Spring Milestone]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth\t| 1.0.0.RC1\t| ([issues](http://foo.bar.com))")
				.contains("<id>spring-milestones</id>")
				.contains("url 'http://repo.spring.io/milestone'")
				.contains("<version>Dalston.RC1</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.RC1'");
	}

	@Test
	public void should_generate_rc_blog_from_template_for_tag_without_v_prefix_release()
			throws IOException {
		this.props.getPom().setBranch("Dalston.RC1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RC1"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RC1"));
			}
		});

		File generatedBlog = new TemplateGenerator(this.props, this.handler)
				.blog(projects);

		then(content(generatedBlog))
				.contains("Release Candidate 1 (RC1) of the [Spring Cloud Dalston]")
				.contains("The release can be found in [Spring Milestone]")
				.contains("### Spring Cloud Sleuth")
				.contains(
						"| Spring Cloud Sleuth\t| 1.0.0.RC1\t| ([issues](http://foo.bar.com))")
				.contains("<id>spring-milestones</id>")
				.contains("url 'http://repo.spring.io/milestone'")
				.contains("<version>Dalston.RC1</version>").contains(
						"mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.RC1'");
	}

	@Test
	public void should_generate_release_notes_template_when_url_exists()
			throws IOException {
		ProjectGitHandler handler = new ProjectGitHandler(this.props) {
			@Override
			public String milestoneUrl(ProjectVersion releaseVersion) {
				return "http://foo.bar.com?closed=1";
			}
		};
		this.props.getPom().setBranch("Dalston.RC1");
		Projects projects = new Projects(new HashSet<ProjectVersion>() {
			{
				add(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RC1"));
				add(new ProjectVersion("spring-cloud-consul", "1.0.1.RC1"));
				add(new ProjectVersion("spring-boot-dependencies", "1.0.1.RC1"));
			}
		});

		File generatedOutput = new TemplateGenerator(this.props, handler)
				.releaseNotes(projects);

		then(content(generatedOutput)).contains("# Dalston.RC1").contains(
				"Spring Cloud Sleuth `1.0.0.RC1` ([issues](http://foo.bar.com?closed=1))")
				.contains(
						"Spring Cloud Consul `1.0.1.RC1` ([issues](http://foo.bar.com?closed=1))")
				.doesNotContain("Boot");
	}

	private String content(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	private String expectedEmail() {
		return "Title:\n" + "Spring Cloud Dalston.RELEASE available\n\n" + "Content:\n"
				+ "All,\n" + "\n"
				+ "On behalf of the team and the community, I'm excited to announce Spring Cloud Dalston RELEASE Train release.\n"
				+ "\n" + "link to blog post\n" + "link to twitter\n\n" + "Cheers,\n";
	}

	private String expectedTweet() {
		return "The Dalston.RELEASE version of @springcloud has been released!";
	}

}
