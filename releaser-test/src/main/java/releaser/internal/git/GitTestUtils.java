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

package releaser.internal.git;

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
public final class GitTestUtils {

	private GitTestUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static void setOriginOnProjectToTmp(File origin, File project)
			throws GitAPIException, MalformedURLException {
		try (Git git = openGitProject(project)) {
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

	public static Git initGitProject(File project) {
		return new GitRepo.JGitFactory().init(project);
	}

	public static File clonedProject(File baseDir, File projectToClone) throws IOException {
		GitRepo projectRepo = new GitRepo(baseDir);
		return projectRepo.cloneProject(new URIish(projectToClone.toURI().toURL()));
	}

}
