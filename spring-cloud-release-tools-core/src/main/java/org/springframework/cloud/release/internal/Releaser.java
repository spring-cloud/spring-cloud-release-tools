package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.build.ProjectBuilder;
import org.springframework.cloud.release.internal.pom.ProjectUpdater;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class Releaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ReleaserProperties properties;
	private final ProjectUpdater projectUpdater;
	private final ProjectBuilder projectBuilder;

	public Releaser(ReleaserProperties properties,
			ProjectUpdater projectUpdater, ProjectBuilder projectBuilder) {
		this.properties = properties;
		this.projectUpdater = projectUpdater;
		this.projectBuilder = projectBuilder;
	}

	public void release() {
		try {
			String workingDir = StringUtils.hasText(this.properties.getWorkingDir()) ?
					this.properties.getWorkingDir() : System.getProperty("user.dir");
			log.info("Will run the application for root folder [{}]", workingDir);
			this.projectUpdater.updateProject(new File(workingDir));
			log.info("Project was successfully updated.\nPress ENTER to build the project");
			System.in.read();
			this.projectBuilder.build();
			log.info("Project was successfully built.\nPress ENTER to commit, tag and push the tag");
			System.in.read();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
