package org.springframework.cloud.release.internal.pom;

import java.io.File;

import org.apache.maven.model.Model;
import org.springframework.cloud.release.internal.pom.PomReader;

/**
 * @author Marcin Grzejszczak
 */
public class TestPomReader {

	PomReader pomReader = new PomReader();

	public Model readPom(File pom) {
		return this.pomReader.readPom(pom);
	}
}
