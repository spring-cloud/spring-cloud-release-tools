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

package org.springframework.cloud.release.internal.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.PomUpdateAcceptanceTests;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectCommandExecutorTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	File temporaryFolder;

	@Before
	public void checkOs() throws Exception {
		Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.temporaryFolder);
	}

	ProjectCommandExecutor projectBuilder(ReleaserProperties properties) {
		return new ProjectCommandExecutor(properties) {
			@Override
			ProcessExecutor executor(String workingDir) {
				return testExecutor(workingDir);
			}
		};
	}

	@Test
	public void should_successfully_execute_a_command_when_after_running_there_is_no_html_file_with_unresolved_tag()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("ls -al");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
	}

	@Test
	public void should_successfully_execute_a_command_when_path_is_provided_explicitly()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("ls -al");
		properties.setWorkingDir(new File("/foo/bar").getAbsolutePath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
				tmpFile("/builder/resolved").getPath());

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
	}

	@Test
	public void should_successfully_execute_a_build_command_for_milestone_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.M1"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo");
	}

	@Test
	public void should_successfully_execute_a_build_command_for_rc_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.RC1"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo")
				.doesNotContain("-Pguides");
	}

	@Test
	public void should_successfully_execute_a_build_command_for_release_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.RELEASE"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo");
	}

	@Test
	public void should_successfully_execute_a_build_command_for_sr_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.SR1"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo");
	}

	@Test
	public void should_successfully_execute_a_command_when_system_props_placeholder_is_present()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo {{systemProps}}");
		properties.getBash().setSystemProperties("-Dhello=world -Dfoo=bar");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("-Dhello=world -Dfoo=bar");
	}

	@Test
	public void should_successfully_execute_a_command_when_system_props_placeholder_is_present_and_there_are_no_sys_props()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo foo {{systemProps}}");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo");
	}

	@Test
	public void should_successfully_execute_a_command_when_system_props_placeholder_is_present_without_system_props()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo {{systemProps}}");
		properties.getBash().setSystemProperties("hello=world foo=bar");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("hello=world foo=bar");
	}

	@Test
	public void should_successfully_execute_a_command_when_system_props_placeholder_is_present_inside_command()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo {{systemProps}} bar");
		properties.getBash().setSystemProperties("-Dhello=world -Dfoo=bar");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("-Dhello=world -Dfoo=bar bar");
	}

	@Test
	public void should_successfully_pass_system_props_when_build_gets_executed_without_explicit_system_props()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("echo bar");
		properties.getBash().setSystemProperties("-Dhello=world -Dfoo=bar");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("bar -Dhello=world -Dfoo=bar");
	}

	@Test
	public void should_throw_exception_when_after_running_there_is_an_html_file_with_unresolved_tag() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("ls -al");
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		thenThrownBy(
				() -> builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT")))
						.hasMessageContaining(
								"contains a tag that wasn't resolved properly");
	}

	@Test
	public void should_throw_exception_when_command_took_too_long_to_execute() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("sleep 1");
		properties.getBash().setWaitTimeInMinutes(0);
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		thenThrownBy(
				() -> builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT")))
						.hasMessageContaining(
								"Process waiting time of [0] minutes exceeded");
	}

	@Test
	public void should_successfully_execute_a_deploy_command() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("ls -al");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_milestone_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.M1"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo")
				.doesNotContain("-Pguides");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_milestone_version_for_maven()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		new ProcessExecutor(properties.getWorkingDir())
				.runCommand(new String[] { "touch", "pom.xml" }, 1);
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.M1"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("foo -Pmilestone").doesNotContain("-Pguides");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_rc_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.RC1"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo")
				.doesNotContain("-Pguides");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_rc_version_for_maven()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		new ProcessExecutor(properties.getWorkingDir())
				.runCommand(new String[] { "touch", "pom.xml" }, 1);
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.RC1"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("foo -Pmilestone").doesNotContain("-Pguides");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_release_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.RELEASE"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_release_version_for_maven()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		new ProcessExecutor(properties.getWorkingDir())
				.runCommand(new String[] { "touch", "pom.xml" }, 1);
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.RELEASE"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("foo -Pcentral");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_for_sr_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("echo foo");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.SR1"));

		then(asString(tmpFile("/builder/resolved/resolved.log"))).contains("foo");
	}

	@Test
	public void should_successfully_execute_a_deploy_command_with_sys_props_placeholder()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("echo \"{{systemProps}}\"");
		properties.getBash().setSystemProperties("-Dhello=hello-world");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("-Dhello=hello-world");
	}

	@Test
	public void should_successfully_pass_system_props_when_deploy_gets_executed_without_explicit_system_props()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("echo ");
		properties.getBash().setSystemProperties("-Dhello=hello-world");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		builder.deploy(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("-Dhello=hello-world");
	}

	@Test
	public void should_throw_exception_when_deploy_command_took_too_long_to_execute() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setDeployCommand("sleep 1");
		properties.getBash().setWaitTimeInMinutes(0);
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		thenThrownBy(
				() -> builder.deploy(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT")))
						.hasMessageContaining(
								"Process waiting time of [0] minutes exceeded");
	}

	@Test
	public void should_successfully_execute_a_publish_docs_command() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setPublishDocsCommands(new String[] { "ls -al", "ls -al" });
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		TestProcessExecutor executor = testExecutor(properties.getWorkingDir());
		ProjectCommandExecutor builder = new ProjectCommandExecutor(properties) {
			@Override
			ProcessExecutor executor(String workingDir) {
				return executor;
			}
		};

		builder.publishDocs("");

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("resolved.log");
		then(executor.counter).isEqualTo(2);
	}

	@Test
	public void should_successfully_execute_a_publish_docs_command_with_sys_props_placeholder()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setPublishDocsCommands(
				new String[] { "echo {{systemProps}} 1", "echo {{systemProps}} 2" });
		properties.getBash().setSystemProperties("-Dhello=world -Dfoo=bar");
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		TestProcessExecutor executor = testExecutor(properties.getWorkingDir());
		ProjectCommandExecutor builder = new ProjectCommandExecutor(properties) {
			@Override
			ProcessExecutor executor(String workingDir) {
				return executor;
			}
		};

		builder.publishDocs("");

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("-Dhello=world -Dfoo=bar 2");
		then(executor.counter).isEqualTo(2);
	}

	@Test
	public void should_successfully_execute_a_publish_docs_command_and_substitute_the_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash()
				.setPublishDocsCommands(new String[] { "echo '{{version}}'" });
		properties.setWorkingDir(tmpFile("/builder/resolved").getPath());
		TestProcessExecutor executor = testExecutor(properties.getWorkingDir());
		ProjectCommandExecutor builder = new ProjectCommandExecutor(properties) {
			@Override
			ProcessExecutor executor(String workingDir) {
				return executor;
			}
		};

		builder.publishDocs("1.1.0.RELEASE");

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("1.1.0.RELEASE");
	}

	@Test
	public void should_successfully_execute_an_update_docs_command_and_substitute_the_version()
			throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setGenerateReleaseTrainDocsCommand("echo '{{version}}'");
		File resolved = tmpFile("/builder/resolved");
		properties.setWorkingDir(resolved.getPath());
		TestProcessExecutor executor = testExecutor(properties.getWorkingDir());
		ProjectCommandExecutor builder = new ProjectCommandExecutor(properties) {
			@Override
			ProcessExecutor executor(String workingDir) {
				return executor;
			}
		};

		builder.generateReleaseTrainDocs("1.1.0.RELEASE", resolved.getAbsolutePath());

		then(asString(tmpFile("/builder/resolved/resolved.log")))
				.contains("1.1.0.RELEASE");
	}

	@Test
	public void should_throw_exception_when_publish_docs_command_took_too_long_to_execute() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash()
				.setPublishDocsCommands(new String[] { "sleep 1", "sleep 1" });
		properties.getBash().setWaitTimeInMinutes(0);
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectCommandExecutor builder = projectBuilder(properties);

		thenThrownBy(() -> builder.publishDocs(""))
				.hasMessageContaining("Process waiting time of [0] minutes exceeded");
	}

	@Test
	public void should_throw_exception_when_process_exits_with_invalid_code() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBash().setBuildCommand("exit 1");
		properties.setWorkingDir(tmpFile("/builder/unresolved").getPath());
		ProjectCommandExecutor builder = new ProjectCommandExecutor(properties) {
			@Override
			ProcessExecutor executor(String workingDir) {
				return new ProcessExecutor(properties.getWorkingDir()) {
					@Override
					Process startProcess(ProcessBuilder builder) {
						return processWithInvalidExitCode();
					}
				};
			}
		};

		thenThrownBy(
				() -> builder.build(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT")))
						.hasMessageContaining(
								"The process has exited with exit code [1]");
	}

	private Process processWithInvalidExitCode() {
		return new Process() {
			@Override
			public OutputStream getOutputStream() {
				return null;
			}

			@Override
			public InputStream getInputStream() {
				return null;
			}

			@Override
			public InputStream getErrorStream() {
				return null;
			}

			@Override
			public int waitFor() {
				return 0;
			}

			@Override
			public int exitValue() {
				return 1;
			}

			@Override
			public void destroy() {

			}
		};
	}

	private TestProcessExecutor testExecutor(String workingDir) {
		return new TestProcessExecutor(workingDir);
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI());
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	class TestProcessExecutor extends ProcessExecutor {

		int counter = 0;

		TestProcessExecutor(String workingDir) {
			super(workingDir);
		}

		@Override
		ProcessBuilder builder(String[] commands, String workingDir) {
			this.counter++;
			return super.builder(commands, workingDir)
					.redirectOutput(tmpFile("/builder/resolved/resolved.log"));
		}

	}

}
