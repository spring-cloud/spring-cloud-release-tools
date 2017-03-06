package org.springframework.cloud.release;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	private static final String BOOT_STARTER_ARTIFACT_ID = "spring-boot-starter-parent";
	private static final String DEPENDENCIES_POM = "spring-cloud-dependencies/pom.xml";
	private static final String CLOUD_DEPENDENCIES_ARTIFACT_ID = "spring-cloud-dependencies-parent";
	private static final Pattern SC_VERSION_PATTERN = Pattern.compile("^(spring-cloud-.*)\\.version$");

	private final File projectRootDir;
	private final String bootPom;
	private final String dependenciesPom;
	private final PomReader pomReader = new PomReader();

	PomParser(File projectRootDir) {
		this(projectRootDir, STARTER_POM, DEPENDENCIES_POM);
	}

	PomParser(File projectRootDir, String bootPom, String dependenciesPom) {
		this.projectRootDir = projectRootDir;
		this.bootPom = bootPom;
		this.dependenciesPom = dependenciesPom;
	}

	Versions bootVersion() {
		Model model = pom(this.bootPom);
		String bootArtifactId = model.getParent().getArtifactId();
		log.debug("Boot artifact id is equal to [{}]", bootArtifactId);
		if (!BOOT_STARTER_ARTIFACT_ID.equals(bootArtifactId)) {
			throw new IllegalStateException("The pom doesn't have a [" + BOOT_STARTER_ARTIFACT_ID + "] artifact id");
		}
		String bootVersion = model.getParent().getVersion();
		log.debug("Boot version is equal to [{}]", bootVersion);
		return new Versions(bootVersion);
	}

	private Model pom(String pom) {
		if (pom == null) {
			throw new IllegalStateException("Pom is not present");
		}
		File pomFile = new File(this.projectRootDir, pom);
		if (!pomFile.exists()) {
			throw new IllegalStateException("Pom is not present");
		}
		return this.pomReader.readPom(pomFile);
	}

	Versions springCloudVersions() {
		Model model = pom(this.dependenciesPom);
		String buildArtifact = model.getParent().getArtifactId();
		log.debug("[{}] artifact id is equal to [{}]", CLOUD_DEPENDENCIES_ARTIFACT_ID, buildArtifact);
		if (!CLOUD_DEPENDENCIES_ARTIFACT_ID.equals(buildArtifact)) {
			throw new IllegalStateException("The pom doesn't have a [" + CLOUD_DEPENDENCIES_ARTIFACT_ID + "] artifact id");
		}
		String buildVersion = model.getParent().getVersion();
		log.debug("Spring Cloud Build version is equal to [{}]", buildVersion);
		Set<Project> projects = model.getProperties().entrySet()
				.stream()
				.filter(propertyMatchesSCPattern())
				.map(toProject())
				.collect(Collectors.toSet());
		return new Versions(buildVersion, projects);
	}

	private Predicate<Map.Entry<Object, Object>> propertyMatchesSCPattern() {
		return entry -> SC_VERSION_PATTERN.matcher(entry.getKey().toString()).matches();
	}

	private Function<Map.Entry<Object, Object>, Project> toProject() {
		return entry -> {
			Matcher matcher = SC_VERSION_PATTERN.matcher(entry.getKey().toString());
			// you have to first match to get info about the group
			matcher.matches();
			String name = matcher.group(1);
			return new Project(name, entry.getValue().toString());
		};
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