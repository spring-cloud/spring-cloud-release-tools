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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.springframework.cloud.info.SpringCloudInfoService.SpringCloudVersion;
import org.springframework.cloud.info.exceptions.SpringCloudMilestoneNotFoundException;
import org.springframework.cloud.info.exceptions.SpringCloudVersionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ryan Baxter
 */
@RestController
public class SpringCloudInfoRestController {

	private SpringCloudInfoService versionService;

	public SpringCloudInfoRestController(SpringCloudInfoService versionService) {
		this.versionService = versionService;
	}

	@GetMapping("/springcloudversion/springboot/{bootVersion}")
	public SpringCloudVersion version(@PathVariable String bootVersion) {
		try {
			return versionService.getSpringCloudVersion(bootVersion);
		}
		catch (SpringCloudVersionNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	@GetMapping("/springcloudversions")
	public Collection<String> versions() throws IOException {
		return versionService.getSpringCloudVersions();
	}

	@GetMapping("/bomversions/{bomVersion}")
	public Map<String, String> bomVersions(@PathVariable String bomVersion) throws IOException {
		try {
			return versionService.getReleaseVersions(bomVersion);
		}
		catch (SpringCloudVersionNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}

	}

	@GetMapping("/milestones")
	public Collection<String> milestones() throws IOException {
		return versionService.getMilestones();
	}

	@GetMapping("/milestones/{name}/duedate")
	public SpringCloudInfoService.Milestone milestoneDueDate(@PathVariable String name) throws IOException {
		try {
			return versionService.getMilestoneDueDate(name);
		}
		catch (SpringCloudMilestoneNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

}
