package org.springframework.cloud.release;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the poms for a given project and populates versions.
 *
 * @author Marcin Grzejszczak
 */
class PomParser {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String STARTER_POM = "spring-cloud-starter-parent/pom.xml";
	private static final String BOOT_STARTER_ARTIFACTID = "spring-boot-starter-parent";
	private static final String DEPENDENCIES_POM = "spring-cloud-dependencies/pom.xml";

	private final File projectRootDir;
	private final String bootPom;
	private final PomReader pomReader = new PomReader();

	PomParser(File projectRootDir) {
		this(projectRootDir, STARTER_POM);
	}

	PomParser(File projectRootDir, String bootPom) {
		this.projectRootDir = projectRootDir;
		this.bootPom = bootPom;
	}

	String bootVersion() {
		File bootPom = new File(this.projectRootDir, this.bootPom);
		if (!bootPom.exists()) {
			throw new IllegalStateException("Pom with boot version is not present");
		}
		Model model = this.pomReader.readPom(bootPom);
		String bootArtifactId = model.getParent().getArtifactId();
		if (log.isDebugEnabled()) {
			log.debug("Boot artifact id is equal to [{}]", bootArtifactId);
		}
		if (!BOOT_STARTER_ARTIFACTID.equals(bootArtifactId)) {
			throw new IllegalStateException("The pom doesn't have a boot version");
		}
		String bootVersion = model.getParent().getVersion();
		if (log.isDebugEnabled()) {
			log.debug("Boot version is equal to [{}]", bootVersion);
		}
		return bootVersion;
	}
}

class PomReader {

	Model readPom(File pom) {
		try(Reader reader = new FileReader(pom)) {
			MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
			return xpp3Reader.read(reader);
		}
		catch (XmlPullParserException | IOException e) {
			throw new IllegalStateException("Failed to read file", e);
		}
	}
}