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
package org.springframework.cloud.release;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.release.SpringCloudConstants.BUILD_ARTIFACT_ID;
import static org.springframework.cloud.release.SpringCloudConstants.CLOUD_DEPENDENCIES_ARTIFACT_ID;

/**
 * @author Marcin Grzejszczak
 */
class PomUpdater {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final PomReader pomReader = new PomReader();

	boolean shouldProjectBeUpdated(File rootPom, Versions versions) {
		Model model = this.pomReader.readPom(rootPom);
		if (!versions.shouldBeUpdated(model.getArtifactId())) {
			log.info("Skipping project [{}] since it's not on the list of projects to update", model.getArtifactId());
			return false;
		}
		log.info("Project [{}] will have its dependencies updated", model.getArtifactId());
		return true;
	}

	ModelWrapper updatePom(File pom, Versions versions) {
		Model model = this.pomReader.readPom(pom);
		boolean dirty = false;
		if (isParentPom(model)) {
			dirty = updateRootParentIfPossible(versions, model) || dirty ;
			dirty = updateVersionIfPossible(versions, model) || dirty ;
		} else {
			dirty = updateParentVersionIfPossible(versions, model) || dirty;
		}
		dirty = updateProperties(versions, model) || dirty;
		return new ModelWrapper(model, dirty);
	}

	private boolean updateParentVersionIfPossible(Versions versions, Model model) {
		String version = versions.versionForProject(model.getArtifactId());
		if (StringUtils.isEmpty(version)) {
			log.warn("There was no version set for project [{}], skipping parent version setting", model.getArtifactId());
			return false;
		}
		log.info("Setting parent [{}] version to [{}]", model.getArtifactId(), version);
		model.getParent().setVersion(version);
		return true;
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

	private boolean updateRootParentIfPossible(Versions versions, Model model) {
		String parentArtifactId = model.getParent().getArtifactId();
		if (BUILD_ARTIFACT_ID.equals(parentArtifactId) ||
				CLOUD_DEPENDENCIES_ARTIFACT_ID.equals(parentArtifactId)) {
			log.info("Setting version of parent to [{}]", parentArtifactId);
			model.getParent().setVersion(versions.scBuildVersion);
			return true;
		}
		log.warn("The parent pom should be referencing Spring Cloud Build but it's not. Won't update it");
		return false;
	}

	private boolean updateVersionIfPossible(Versions versions, Model model) {
		String version = versions.versionForProject(model.getArtifactId());
		if (StringUtils.isEmpty(version)) {
			log.warn("There was no version set for project [{}], skipping version setting", model.getArtifactId());
			return false;
		}
		log.info("Setting [{}] version to [{}]", model.getArtifactId(), version);
		model.setVersion(version);
		return true;
	}

	/**
	 * All child poms have a relative path to a parent folder. The parent one
	 * has an empty path.
	 */
	private boolean isParentPom(Model model) {
		return StringUtils.isEmpty(model.getParent().getRelativePath());
	}
}

class ModelWrapper {
	final Model model;
	final boolean dirty;

	ModelWrapper(Model model, boolean dirty) {
		this.model = model;
		this.dirty = dirty;
	}
}