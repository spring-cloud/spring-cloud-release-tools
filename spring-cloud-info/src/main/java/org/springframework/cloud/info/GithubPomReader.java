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
import java.io.StringReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.springframework.web.client.RestTemplate;

/**
 * @author Ryan Baxter
 */
public class GithubPomReader {

	private MavenXpp3Reader reader;

	private RestTemplate rest;

	public GithubPomReader(MavenXpp3Reader reader, RestTemplate rest) {
		this.reader = reader;
		this.rest = rest;
	}

	public Model readPomFromUrl(String url) throws IOException, XmlPullParserException {
		StringReader pomString = new StringReader(rest.getForObject(url, String.class));
		try {
			return reader.read(pomString);
		}
		finally {
			pomString.close();
		}
	}

}
