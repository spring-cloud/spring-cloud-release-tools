package org.springframework.cloud.release.internal.pom;

import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.springframework.cloud.release.internal.ReleaserProperties;

import static org.junit.Assert.*;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectPomUpdaterTests {

	@Test public void should_convert_fixed_versions_to_updated_fixed_versions() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getFixedVersions().put("spring-cloud-task", "2.0.0.RELEASE");
		properties.getFixedVersions().put("spring-cloud-openfeign", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-consul", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-zookeeper", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-stream", "Elmhurst.RELEASE");
		properties.getFixedVersions().put("spring-cloud-config", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-cloudfoundry", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-netflix", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-vault", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-security", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-commons", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-sleuth", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-aws", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-contract", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-release", "Finchley.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-build", "2.0.3.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-bus", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-function", "1.0.0.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-starter-build", "Finchley.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-boot", "2.0.3.RELEASE");
		properties.getFixedVersions().put("spring-cloud-gateway", "2.0.1.BUILD-SNAPSHOT");
		ProjectPomUpdater updater = new ProjectPomUpdater(properties);

		Map<String, String> fixedVersions = updater.fixedVersions()
				.stream()
				.collect(Collectors.toMap(
						projectVersion -> projectVersion.projectName,
						projectVersion -> projectVersion.version));

		BDDAssertions.then(fixedVersions)
				.containsEntry("spring-boot", "2.0.3.RELEASE")
				.containsEntry("spring-boot-dependencies", "2.0.3.RELEASE")
				.containsEntry("spring-boot-starter", "2.0.3.RELEASE")
				.containsEntry("spring-cloud-build", "2.0.3.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-dependencies", "2.0.3.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-release", "Finchley.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud", "Finchley.BUILD-SNAPSHOT");
	}
}