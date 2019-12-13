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

package org.springframework.cloud.release.internal.tech;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception to be thrown if one wants to continue with the build and throw this exception
 * at the end of the release.
 *
 * @author Marcin Grzejszczak
 */
public class BuildUnstableException extends RuntimeException implements Serializable {

	/**
	 * Description of the exception.
	 */
	public static final String DESCRIPTION = "[BUILD UNSTABLE] WARNING!";

	/**
	 * Exit code of the exception.
	 */
	public static final String EXIT_CODE = "BUILD_UNSTABLE";

	private static final Logger log = LoggerFactory
			.getLogger(BuildUnstableException.class);

	/**
	 * List of exceptions causing this instability.
	 */
	private final List<Throwable> exceptions = new ArrayList<>();

	public BuildUnstableException(Throwable cause) {
		super(cause);
		log.error("\n\n" + DESCRIPTION, cause);
		this.exceptions.add(cause);
	}

	public BuildUnstableException(String message, List<Throwable> throwables) {
		this(throwables);
		log.error("\n\n" + DESCRIPTION + message + " with causes " + throwables);
	}

	@JsonCreator
	public BuildUnstableException(@JsonProperty List<Throwable> throwables) {
		this.exceptions.addAll(throwables);
	}

	public BuildUnstableException(String message) {
		super(message);
		log.error("\n\n" + DESCRIPTION + message);
	}

	public BuildUnstableException(String message, Throwable cause) {
		super(message, cause);
		log.error("\n\n" + DESCRIPTION + message, cause);
		this.exceptions.add(cause);
	}

	public List<Throwable> getExceptions() {
		return this.exceptions;
	}

}
