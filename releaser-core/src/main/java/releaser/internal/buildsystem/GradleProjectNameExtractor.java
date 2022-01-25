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

package releaser.internal.buildsystem;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GradleProjectNameExtractor {

	private static final Pattern VERSION_PATTERN = Pattern.compile("^([a-zA-Z0-9]+)Version$");

	String projectName(Map<String, String> substitution, Object key) {
		String projectName = key.toString();
		if (substitution.containsKey(key)) {
			projectName = substitution.get(key);
		}
		else {
			Matcher matcher = VERSION_PATTERN.matcher(projectName);
			boolean versionMatches = matcher.matches();
			if (versionMatches) {
				projectName = matcher.group(1);
			}
		}
		projectName = projectName.replaceAll("([A-Z])", "-$1").toLowerCase();
		return projectName;
	}

}
