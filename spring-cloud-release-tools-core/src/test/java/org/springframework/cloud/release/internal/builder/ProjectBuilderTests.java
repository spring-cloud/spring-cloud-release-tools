package org.springframework.cloud.release.internal.builder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.release.internal.ReleaserProperties;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectBuilderTests {

	@Before
	public void checkOs() {
		Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
	}

	@Test
	public void should_successfully_execute_a_command_when_after_running_there_is_no_html_file_with_unresolved_tag() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBuild().setCommand("ls -al");
		properties.setWorkingDir(file("/projects/builder/resolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		builder.build();

		then(asString(file("/projects/builder/resolved/resolved.log")))
				.contains("total 0")
				.contains("file.txt");
	}

	@Test
	public void should_throw_exception_when_after_running_there_is_an_html_file_with_unresolved_tag() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBuild().setCommand("ls -al");
		properties.setWorkingDir(file("/projects/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		thenThrownBy(builder::build).hasMessageContaining("contains a tag that wasn't resolved properly");
	}

	@Test
	public void should_throw_exception_when_command_took_too_long_to_execute() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getBuild().setCommand("sleep 1");
		properties.getBuild().setWaitTimeInMinutes(0);
		properties.setWorkingDir(file("/projects/builder/unresolved").getPath());
		ProjectBuilder builder = new ProjectBuilder(properties, executor(properties));

		thenThrownBy(builder::build).hasMessageContaining("Build waiting time of [0] minutes exceeded");
	}

	private ProcessExecutor executor(ReleaserProperties properties) {
		return new ProcessExecutor(properties) {
			@Override ProcessBuilder builder(String[] commands, String workingDir) {
				return super.builder(commands, workingDir)
						.redirectOutput(file("/projects/builder/resolved/resolved.log"));
			}
		};
	}

	private File file(String relativePath) {
		try {
			File root = new File(ProjectBuilderTests.class.getResource("/").toURI());
			File file = new File(root, relativePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			return file;
		}
		catch (IOException | URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

}