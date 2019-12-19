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

package org.springframework.cloud.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan Baxter
 */
public final class SpringCloudInfoTestData {

	public static Map<String, String> releaseVersions = new HashMap<>();

	public static List<String> springCloudVersions = new ArrayList<>();

	public static Map<String, String> milestoneStrings = new HashMap<>();

	static {
		releaseVersions.put("spring-boot", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-aws", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-bus", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-cloudfoundry", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-commons", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-config", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-consul", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-contract", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-function", "2.1.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-gateway", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-gcp", "1.1.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-kubernetes", "1.1.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-netflix", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-openfeign", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-security", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-sleuth", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-stream", "Germantown.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-task", "2.0.0.RELEASE");
		releaseVersions.put("spring-cloud-vault", "2.2.0.BUILD-SNAPSHOT");
		releaseVersions.put("spring-cloud-zookeeper", "2.2.0.BUILD-SNAPSHOT");
	}

	static {
		springCloudVersions.add("vGreenwich.SR1");
		springCloudVersions.add("vGreenwich.RELEASE");
		springCloudVersions.add("vGreenwich.RC2");
		springCloudVersions.add("vGreenwich.RC1");
		springCloudVersions.add("vGreenwich.M3");
		springCloudVersions.add("vGreenwich.M2");
		springCloudVersions.add("vGreenwich.M1");
		springCloudVersions.add("vFinchley.SR3");
		springCloudVersions.add("vFinchley.SR2");
		springCloudVersions.add("vFinchley.SR1");
		springCloudVersions.add("vFinchley.RELEASE");
		springCloudVersions.add("vFinchley.RC2");
		springCloudVersions.add("vFinchley.RC1");
		springCloudVersions.add("vFinchley.M9");
		springCloudVersions.add("vFinchley.M8");
		springCloudVersions.add("vFinchley.M7");
		springCloudVersions.add("vFinchley.M6");
		springCloudVersions.add("vFinchley.M5");
		springCloudVersions.add("vFinchley.M3");
		springCloudVersions.add("vFinchley.M2");
		springCloudVersions.add("vFinchley.M1");
		springCloudVersions.add("vEdgware.SR5");
		springCloudVersions.add("vEdgware.SR4");
		springCloudVersions.add("vEdgware.SR3");
		springCloudVersions.add("vEdgware.SR2");
		springCloudVersions.add("vEdgware.SR1");
		springCloudVersions.add("vEdgware.RELEASE");
		springCloudVersions.add("vEdgware.M1");
		springCloudVersions.add("vDalston.SR5");
		springCloudVersions.add("vDalston.SR4");
	}

	static {
		milestoneStrings.put("Hoxton.M1", "2019-05-23T07:00:00Z");
		milestoneStrings.put("Hoxton.M2", "2019-06-27T07:00:00Z");
		milestoneStrings.put("Hoxton.RELEASE", "2019-07-31T07:00:00Z");
		milestoneStrings.put("Finchley.SR4", null);
	}

	private SpringCloudInfoTestData() {
		throw new IllegalStateException("Can't instantiate utility class");
	}

}
