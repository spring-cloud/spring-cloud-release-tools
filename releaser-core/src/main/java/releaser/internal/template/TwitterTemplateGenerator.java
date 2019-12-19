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

package releaser.internal.template;

import java.io.File;
import java.nio.file.Files;

import com.github.jknack.handlebars.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marcin Grzejszczak
 */
class TwitterTemplateGenerator {

	private static final Logger log = LoggerFactory
			.getLogger(TwitterTemplateGenerator.class);

	private final Template template;

	private final String releaseVersion;

	private final File output;

	TwitterTemplateGenerator(Template template, String releaseVersion, File output) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.output = output;
	}

	File tweet() {
		try {
			String tweet = this.template.apply(this.releaseVersion);
			Files.write(this.output.toPath(), tweet.getBytes());
			return this.output;
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Exception occurred while trying to generate a twitter template", e);
		}
	}

}
