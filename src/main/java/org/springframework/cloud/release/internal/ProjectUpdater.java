package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectUpdater {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final File destinationDir;
	private final ReleaserProperties properties;
	private final GitProjectRepo gitProjectRepo;
	private final PomUpdater pomUpdater = new PomUpdater();

	public ProjectUpdater(ReleaserProperties properties) {
		try {
			this.destinationDir = properties.getCloneDestinationDir() != null ?
					new File(properties.getCloneDestinationDir()) :
					Files.createTempDirectory("releaser").toFile();
			this.properties = properties;
			this.gitProjectRepo = new GitProjectRepo(this.destinationDir);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to create a temporary folder", e);
		}
	}

	public void updateProject(File projectRoot) {
		File clonedScRelease = this.gitProjectRepo.cloneProject(
				URI.create(this.properties.getSpringCloudReleaseGitUrl()));
		this.gitProjectRepo.checkout(clonedScRelease, this.properties.getBranch());
		SCReleasePomParser SCReleasePomParser = new SCReleasePomParser(clonedScRelease);
		Versions versions = SCReleasePomParser.allVersions();
		if (!this.pomUpdater.shouldProjectBeUpdated(projectRoot, versions)) {
			log.info("Project is not on the list of projects to be updated. Skipping.");
			return;
		}
		File rootPom = new File(projectRoot, "pom.xml");
		ModelWrapper rootPomModel = this.pomUpdater.readModel(rootPom);
		processAllPoms(projectRoot, new PomWalker(rootPomModel, versions, this.pomUpdater));
	}

	private void processAllPoms(File projectRoot, PomWalker pomWalker) {
		try {
			Files.walkFileTree(projectRoot.toPath(), pomWalker);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private class PomWalker extends SimpleFileVisitor<Path> {

		private  static final String POM_XML = "pom.xml";

		private final ModelWrapper rootPom;
		private final Versions versions;
		private final PomUpdater pomUpdater;

		private PomWalker(ModelWrapper rootPom, Versions versions, PomUpdater pomUpdater) {
			this.rootPom = rootPom;
			this.versions = versions;
			this.pomUpdater = pomUpdater;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
			File file = path.toFile();
			if (POM_XML.equals(file.getName())) {
				ModelWrapper model = this.pomUpdater.updateModel(this.rootPom.projectName(), file, this.versions);
				this.pomUpdater.overwritePomIfDirty(model, file);
			}
			return FileVisitResult.CONTINUE;
		}
	}

}
