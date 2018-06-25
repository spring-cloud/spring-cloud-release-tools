package org.springframework.cloud.release.internal.git;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;

/**
 * @author Marcin Grzejszczak
 */
public class GitTestUtils {

	public static void setOriginOnProjectToTmp(File origin, File project)
			throws GitAPIException, MalformedURLException {
		try(Git git = openGitProject(project)) {
			RemoteRemoveCommand remove = git.remoteRemove();
			remove.setName("origin");
			remove.call();
			RemoteSetUrlCommand command = git.remoteSetUrl();
			command.setUri(new URIish(origin.toURI().toURL()));
			command.setName("origin");
			command.setPush(true);
			command.call();
		}
	}

	public static Git openGitProject(File project) {
		return new GitRepo.JGitFactory().open(project);
	}

	public static File clonedProject(File baseDir, File projectToClone) throws IOException {
		GitRepo projectRepo = new GitRepo(baseDir);
		projectRepo.cloneProject(new URIish(projectToClone.toURI().toURL()));
		return baseDir;
	}
}
