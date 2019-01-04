/*
 *  Copyright 2013-2019 the original author or authors.
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
package org.springframework.cloud.release.internal.pom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.change.AbstractVersionChanger;
import org.codehaus.mojo.versions.change.VersionChange;
import org.codehaus.mojo.versions.change.VersionChanger;
import org.codehaus.mojo.versions.change.VersionChangerFactory;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.stax2.XMLInputFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author Marcin Grzejszczak
 */
class PomUpdater {

	private static final Logger log = LoggerFactory.getLogger(PomUpdater.class);

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
		File rootPom = rootPom(rootFolder);
		if (!rootPom.exists()) {
			return false;
		}
		Model model = this.pomReader.readPom(rootPom);
		if (model == null) {
			log.info("Failed to read the model");
			return false;
		}
		String artifactId =  artifactId(model);
		if (!versions.shouldBeUpdated(artifactId)) {
			log.info("Skipping project [{}] since it's not on the list of projects to update", model.getArtifactId());
			return false;
		}
		log.info("Project [{}] will have its dependencies updated", model.getArtifactId());
		return true;
	}

	boolean hasSkipDeployment(Model model) {
		String property = model.getProperties()
				.getProperty("maven.deploy.skip");
		boolean hasSkipDeploymentProperty = Boolean.parseBoolean(property);
		if (hasSkipDeploymentProperty) {
			return true;
		}
		if (model.getBuild() == null) {
			return false;
		}
		boolean plugins = model.getBuild()
				.getPlugins()
				.stream()
				.filter(plugin -> "maven-deploy-plugin".equalsIgnoreCase(plugin.getArtifactId()))
				.map(this::skipFromConfiguration)
				.findFirst()
				.orElse(false);
		if (plugins) {
			return true;
		}
		if (model.getBuild()
				.getPluginManagement() == null) {
			return false;
		}
		return model.getBuild()
				.getPluginManagement()
				.getPlugins()
				.stream()
				.filter(plugin -> "maven-deploy-plugin".equalsIgnoreCase(plugin.getArtifactId()))
				.map(this::skipFromConfiguration)
				.findFirst()
				.orElse(false);
	}

	private Boolean skipFromConfiguration(Plugin plugin) {
		if (!(plugin.getConfiguration() instanceof Xpp3Dom)) {
			return false;
		}
		Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
		Xpp3Dom skip = configuration.getChild("skip");
		return skip != null && Boolean.parseBoolean(skip.getValue());
	}

	private File rootPom(File rootFolder) {
		if (rootFolder.getName().endsWith(".xml")) {
			return rootFolder;
		}
		return new File(rootFolder, "pom.xml");
	}

	private String artifactId(Model model) {
		boolean parent = model.getArtifactId().endsWith("-parent");
		if (!parent) {
			return model.getArtifactId();
		}
		return model.getArtifactId().substring(0, model.getArtifactId().indexOf("-parent"));
	}

	ModelWrapper readModel(File pom) {
		return new ModelWrapper(this.pomReader.readPom(pom));
	}

	/**q
	 * Updates the root / child module model
	 *
	 * @param rootPom - root project model
	 * @param pom - file with the pom
	 * @param versions - versions to update
	 * @return updated model
	 */
	ModelWrapper updateModel(ModelWrapper rootPom, File pom, Versions versions) {
		Model model = this.pomReader.readPom(pom);
		List<VersionChange> sourceChanges = new ArrayList<>();
		sourceChanges = updateParentIfPossible(rootPom, versions, model, sourceChanges);
		sourceChanges = updateVersionIfPossible(rootPom, versions, model, sourceChanges);
		return new ModelWrapper(model, sourceChanges, versions);
	}

	/**
	 * Overwrites the pom.xml with data from {@link ModelWrapper} only if there were
	 * any changes in the model.
	 *
	 * @return - the pom file
	 */
	File overwritePomIfDirty(ModelWrapper updatedPomModel, Versions versions, File pom) {
		if (updatedPomModel.isDirty()) {
			log.debug("There were changes in the pom so file will be overridden");
			this.pomWriter.write(updatedPomModel, versions, pom);
			log.info("Successfully stored [{}]", pom);
		}
		return pom;
	}

	private List<VersionChange> updateParentIfPossible(ModelWrapper wrapper, Versions versions,
			Model model, List<VersionChange> sourceChanges) {
		String rootProjectName = wrapper.projectName();
		List<VersionChange> changes = new ArrayList<>(sourceChanges);
		if (model.getParent() == null || isEmpty(model.getParent().getVersion())) {
			log.debug("Can't set the value for parent... Will return {}", sourceChanges);
			return changes;
		}
		String parentGroupId = model.getParent().getGroupId();
		String parentArtifactId = model.getParent().getArtifactId();
		log.debug("Searching for a version of parent [{}:{}]", parentGroupId, parentArtifactId);
		String oldVersion = model.getParent().getVersion();
		String version = versions.versionForProject(parentArtifactId);
		log.debug("Found version is [{}]", version);
		if (isEmpty(version)) {
			if (hasText(model.getParent().getRelativePath())) {
				version = versions.versionForProject(rootProjectName);
			} else {
				log.warn("There is no info on the [{}:{}] version", parentGroupId, parentArtifactId);
				return changes;
			}
		}
		if (oldVersion.equals(version)) {
			log.debug("Won't update the version of parent [{}:{}] since you're already using the proper one", parentGroupId, parentArtifactId);
			return changes;
		}
		log.info("Setting version of parent [{}] to [{}] for module [{}]", parentArtifactId,
				version, model.getArtifactId());
		if (hasText(version)) {
			changes.add(new VersionChange(parentGroupId, parentArtifactId, oldVersion, version));
		}
		return changes;
	}

	private List<VersionChange> updateVersionIfPossible(ModelWrapper wrapper, Versions versions,
			Model model, List<VersionChange> sourceChanges) {
		String rootProjectName = wrapper.projectName();
		String rootProjectGroupId = wrapper.groupId();
		List<VersionChange> changes = new ArrayList<>(sourceChanges);
		String groupId = groupId(model);
		String artifactId = model.getArtifactId();
		if (model.getGroupId() != null && !model.getGroupId().equals(rootProjectGroupId)) {
			log.info("Will not update project [{}] since its group id [{}] is not equal the parent group id [{}]", model.getArtifactId(), model.getGroupId(), rootProjectGroupId);
			return changes;
		}
		log.debug("Searching for a version [{}:{}]", groupId, artifactId);
		String oldVersion = model.getVersion();
		String version = versions.versionForProject(rootProjectName);
		log.debug("Found version is [{}]", version);
		if (isEmpty(version) || isEmpty(model.getVersion())) {
			log.debug("There was no version set for project [{}], skipping version setting for module [{}]", rootProjectName, model.getArtifactId());
			return changes;
		}
		if (oldVersion.equals(version)) {
			log.debug("Won't update the version of module [{}]:[{}] since you're already using the proper one", groupId, artifactId);
			return changes;
		}
		log.info("Setting [{}] version to [{}]", artifactId, version);
		changes.add(new VersionChange(groupId, artifactId, oldVersion, version));
		return changes;
	}

	private String groupId(Model model) {
		if (hasText(model.getGroupId())) {
			return model.getGroupId();
		}
		if (model.getParent() != null) {
			return model.getParent().getGroupId();
		}
		return "";
	}
}

class ModelWrapper {
	final Model model;
	final Versions versions;
	final List<VersionChange> sourceChanges = new ArrayList<>();

	ModelWrapper(Model model, List<VersionChange> sourceChanges, Versions versions) {
		this.model = model;
		this.versions = versions;
		this.sourceChanges.addAll(sourceChanges);
	}

	ModelWrapper(Model model) {
		this.model = model;
		this.versions = Versions.EMPTY_VERSION;
	}

	String projectName() {
		return this.model.getArtifactId();
	}

	String groupId() {
		if (this.model.getGroupId() != null) {
			return this.model.getGroupId();
		}
		return this.model.getParent() != null ?
				this.model.getParent().getGroupId() : "";
	}

	boolean isDirty() {
		return !this.sourceChanges.isEmpty() || this.versions.shouldSetProperty(this.model.getProperties());
	}
}

class PomWriter {

	private static final Logger log = LoggerFactory.getLogger(PomWriter.class);

	void write(ModelWrapper wrapper, Versions versions, File pom) {
		try {
			VersionChangerFactory versionChangerFactory = new VersionChangerFactory();
			StringBuilder input = PomHelper.readXmlFile(pom);
			ModifiedPomXMLEventReader parsedPom = newModifiedPomXER(input);
			versionChangerFactory.setPom(parsedPom);
			LoggerToMavenLog loggerToMavenLog = new LoggerToMavenLog(PomWriter.log);
			versionChangerFactory.setLog(loggerToMavenLog);
			versionChangerFactory.setModel(wrapper.model);
			log.info("Applying version / parent / plugin / project changes to the pom [{}]", pom);
			VersionChanger changer = versionChangerFactory.newVersionChanger( true,
					true, true, true);
			for (VersionChange versionChange : wrapper.sourceChanges) {
				changer.apply(versionChange);
			}
			log.debug("Applying properties changes to the pom [{}]", pom);
			new PropertyVersionChanger(wrapper, versions, parsedPom, loggerToMavenLog)
					.apply(null);
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(pom))) {
				bw.write(input.toString());
			}
			log.debug("Flushed changes to the pom file [{}]", pom);
		} catch (Exception e) {
			log.error("Exception occurred while trying to apply changes to the POM", e);
		}
	}

	/**
	 * Creates a {@link org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader} from a StringBuilder.
	 *
	 * @param input The XML to read and modify.
	 * @return The {@link org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader}.
	 */
	private ModifiedPomXMLEventReader newModifiedPomXER(StringBuilder input) {
		ModifiedPomXMLEventReader newPom = null;
		try {
			XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
			inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
			newPom = new ModifiedPomXMLEventReader(input, inputFactory);
		}
		catch (XMLStreamException e) {
			log.error("Exception occurred while trying to parse pom", e);
		}
		return newPom;
	}
}

class PropertyVersionChanger extends AbstractVersionChanger {

	private final Versions versions;
	private final PropertyStorer propertyStorer;

	PropertyVersionChanger(ModelWrapper wrapper, Versions versions, ModifiedPomXMLEventReader pom, Log log) {
		super(wrapper.model, pom, log);
		this.versions = versions;
		this.propertyStorer = new PropertyStorer(log, pom);
	}

	PropertyVersionChanger(ModelWrapper wrapper, Versions versions, ModifiedPomXMLEventReader pom, Log log, PropertyStorer propertyStorer) {
		super(wrapper.model, pom, log);
		this.versions = versions;
		this.propertyStorer = propertyStorer;
	}

	@Override public void apply(final VersionChange versionChange) {
		this.versions.projects
				.stream()
				.filter(project -> {
					Properties properties = getModel().getProperties();
					String projectVersionKey = propertyName(project);
					if (!properties.containsKey(projectVersionKey)) {
						return false;
					}
					String version = properties.getProperty(projectVersionKey);
					return !version.equals(project.version);
				})
				.forEach(this.propertyStorer::setPropertyVersionIfApplicable);
	}

	private String propertyName(Project project) {
		return project.name + ".version";
	}
}

class PropertyStorer {

	private final Log log;
	private final ModifiedPomXMLEventReader pom;

	PropertyStorer(Log log, ModifiedPomXMLEventReader pom) {
		this.log = log;
		this.pom = pom;
	}

	void setPropertyVersionIfApplicable(Project project) {
		String propertyName = propertyName(project);
		if (setPropertyVersion(propertyName, project.version)) {
			this.log.info("Updating property [" + propertyName + "] to version [" + project.version + "]");
		}
	}

	private String propertyName(Project project) {
		return project.name + ".version";
	}

	private boolean setPropertyVersion(String propertyName, String version) {
		try {
			if (StringUtils.isEmpty(version)) {
				this.log.warn("Version for [" + propertyName + "] is empty. Will not set it");
				return false;
			}
			return PomHelper.setPropertyVersion(this.pom, null, propertyName, version);
		}
		catch (XMLStreamException e) {
			this.log.error("Exception occurred while trying to set property version", e);
			return false;
		}
	}
}

class LoggerToMavenLog implements Log {

	private final Logger logger;

	LoggerToMavenLog(Logger logger) {
		this.logger = logger;
	}

	@Override public boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}

	@Override public void debug(CharSequence content) {
		this.logger.debug(content.toString());
	}

	@Override public void debug(CharSequence content, Throwable error) {
		this.logger.debug(content.toString(), error);
	}

	@Override public void debug(Throwable error) {
		this.debug("Exception occurred", error);
	}

	@Override public boolean isInfoEnabled() {
		return this.logger.isInfoEnabled();
	}

	@Override public void info(CharSequence content) {
		this.logger.info(content.toString());
	}

	@Override public void info(CharSequence content, Throwable error) {
		this.logger.info(content.toString(), error);
	}

	@Override public void info(Throwable error) {
		this.info("Exception occurred", error);
	}

	@Override public boolean isWarnEnabled() {
		return this.logger.isWarnEnabled();
	}

	@Override public void warn(CharSequence content) {
		this.logger.warn(content.toString());
	}

	@Override public void warn(CharSequence content, Throwable error) {
		this.logger.warn(content.toString(), error);
	}

	@Override public void warn(Throwable error) {
		this.warn("Exception occurred", error);
	}

	@Override public boolean isErrorEnabled() {
		return this.logger.isErrorEnabled();
	}

	@Override public void error(CharSequence content) {
		this.logger.error(content.toString());
	}

	@Override public void error(CharSequence content, Throwable error) {
		this.logger.error(content.toString(), error);
	}

	@Override public void error(Throwable error) {
		this.error("Exception occurred", error);
	}
}