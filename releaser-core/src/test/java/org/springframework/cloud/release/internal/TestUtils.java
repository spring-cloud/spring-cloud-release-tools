package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.util.FileUtils;

public class TestUtils {

	public static void prepareLocalRepo() throws IOException {
		prepareLocalRepo("target/test-classes/projects/", "spring-cloud-release");
	}

	private static void prepareLocalRepo(String buildDir, String repoPath) throws IOException {
		File dotGit = new File(buildDir + repoPath + "/.git");
		File git = new File(buildDir + repoPath + "/git");
		if (git.exists()) {
			if (dotGit.exists()) {
				FileUtils.delete(dotGit, FileUtils.RECURSIVE);
			}
		}
		git.renameTo(dotGit);
	}

}