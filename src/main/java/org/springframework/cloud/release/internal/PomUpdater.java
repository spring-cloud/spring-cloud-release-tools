/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class PomUpdater {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final PomReader pomReader = new PomReader();
	private final PomWriter pomWriter = new PomWriter();

	/**
	 * Basing on the contents of the root pom and the versions will decide whether
	 * the project should be updated or not.
	 *
	 * @param rootFolder - root folder of the project
	 * @param versions - list of dependencies to be updated
	 * @return {@code true} if the project is on the list of projects to be updated
	 */
	boolean shouldProjectBeUpdated(File rootFolder, Versions versions) {
		File rootPom = new File(rootFolder, "pom.xml");
		Model model = this.pomReader.readPom(rootPom);
		if (!versions.shouldBeUpdated(model.getArtifactId())) {
			log.info("Skipping project [{}] since it's not on the list of projects to update", model.getArtifactId());
			return false;
		} else if (versions.versionAlreadySet(model.getArtifactId(), model.getVersion())) {
			log.info("Version has already been set. The project shouldn't be updated.");
			return false;
		}
		log.info("Project [{}] will have its dependencies updated", model.getArtifactId());
		return true;
	}

	ModelWrapper readModel(File pom) {
		return new ModelWrapper(this.pomReader.readPom(pom), false);
	}

	/**
	 * Updates the root / child module model
	 *
	 * @param rootProjectName - name of the artifactId of the root project
	 * @param pom - file with the pom
	 * @param versions - versions to update
	 * @return updated model
	 */
	ModelWrapper updateModel(String rootProjectName, File pom, Versions versions) {
		Model model = this.pomReader.readPom(pom);
		boolean dirty = false;
		dirty = updateParentIfPossible(rootProjectName, versions, model) || dirty ;
		dirty = updateVersionIfPossible(rootProjectName, versions, model) || dirty ;
		dirty = updateProperties(versions, model) || dirty;
		return new ModelWrapper(model, dirty);
	}

	/**
	 * Overwrites the pom.xml with data from {@link ModelWrapper} only if there were
	 * any changes in the model.
	 *
	 * @return - the pom file
	 */
	File overwritePomIfDirty(ModelWrapper wrapper, File pom) {
		if (wrapper.dirty) {
			log.debug("There were changes in the pom so file will be overridden");
			this.pomWriter.write(wrapper.model, pom);
			log.info("Successfully stored [{}]", pom);
		}
		return pom;
	}


	private boolean updateProperties(Versions versions, Model model) {
		final AtomicBoolean atomicBoolean = new AtomicBoolean();
		versions.projects
				.forEach(project -> {
					Properties properties = model.getProperties();
					String projectVersionKey = project.name + ".version";
					if (properties.containsKey(projectVersionKey)) {
						Object previous = properties.setProperty(projectVersionKey, project.version);
						log.info("Updated property [{}] => [{}]. Previous version [{}]",
								projectVersionKey, project.version, previous);
						atomicBoolean.set(true);
					}
				});
		return atomicBoolean.get();
	}

	private boolean updateParentIfPossible(String rootProjectName, Versions versions, Model model) {
		if (model.getParent() == null || StringUtils.isEmpty(model.getParent().getVersion())) {
			return false;
		}
		String parentArtifactId = model.getParent().getArtifactId();
		String version = versions.versionForProject(parentArtifactId);
		if (StringUtils.isEmpty(version)) {
			if (StringUtils.hasText(model.getParent().getRelativePath())) {
				version = versions.versionForProject(rootProjectName);
			} else {
				log.warn("There is no info on the [{}] version", model.getArtifactId());
				return false;
			}
		}
		log.info("Setting version of parent [{}] to [{}] for module [{}]", parentArtifactId, version, model.getArtifactId());
		model.getParent().setVersion(version);
		return true;
	}

	private boolean updateVersionIfPossible(String rootProjectName, Versions versions, Model model) {
		String version = versions.versionForProject(rootProjectName);
		if (StringUtils.isEmpty(version) || StringUtils.isEmpty(model.getVersion())) {
			log.warn("There was no version set for project [{}], skipping version setting for module [{}]", rootProjectName, model.getArtifactId());
			return false;
		}
		log.info("Setting [{}] version to [{}]", model.getArtifactId(), version);
		model.setVersion(version);
		return true;
	}
}

class ModelWrapper {
	final Model model;
	final boolean dirty;

	ModelWrapper(Model model, boolean dirty) {
		this.model = model;
		this.dirty = dirty;
	}

	String projectName() {
		return this.model.getArtifactId();
	}
}

class PomWriter {
	void write(Model model, File pom) {
		try(Writer writer = new FileWriter(pom)) {
			MavenXpp3Writer pomWriter = new MavenXpp3Writer();
			pomWriter.write(writer, model);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to write file", e);
		}
	}
}