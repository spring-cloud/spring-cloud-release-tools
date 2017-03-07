package org.springframework.cloud.release.internal;

import java.io.File;

import org.apache.maven.model.Model;

/**
 * @author Marcin Grzejszczak
 */
public class TestPomReader {

	PomReader pomReader = new PomReader();

	public Model readPom(File pom) {
		return this.pomReader.readPom(pom);
	}
}
