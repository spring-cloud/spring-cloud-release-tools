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

package org.springframework.cloud.release.internal.spring;

import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.release.internal.tech.MakeBuildUnstableException;

public class ExecutionResult {

	private List<Throwable> exceptions = new LinkedList<>();

	public ExecutionResult() {
	}

	public ExecutionResult(Throwable throwable) {
		this.exceptions.add(throwable);
	}

	public ExecutionResult(List<Throwable> throwables) {
		this.exceptions.addAll(throwables);
	}

	public RuntimeException foundExceptions() {
		if (this.exceptions.size() == 1) {
			Throwable throwable = this.exceptions.get(0);
			return throwable instanceof RuntimeException ? (RuntimeException) throwable
					: new RuntimeException(throwable);
		}
		if (isUnstable()) {
			return new MergedUnstableThrowable(this.exceptions);
		}
		return new MergedThrowable(this.exceptions);
	}

	public ExecutionResult merge(ExecutionResult other) {
		ExecutionResult merged = new ExecutionResult(this.exceptions);
		merged.exceptions.addAll(other.exceptions);
		return merged;
	}

	public boolean isUnstable() {
		return !this.exceptions.isEmpty() && this.exceptions.stream()
				.allMatch(t -> t instanceof MakeBuildUnstableException);
	}

	public boolean isFailure() {
		return !this.exceptions.isEmpty() && this.exceptions.stream()
				.anyMatch(t -> !(t instanceof MakeBuildUnstableException));
	}

	public boolean isFailureOrUnstable() {
		return !this.exceptions.isEmpty();
	}

	public static ExecutionResult success() {
		return new ExecutionResult();
	}

	public static ExecutionResult failure(Throwable throwable) {
		return new ExecutionResult(throwable);
	}

	public static ExecutionResult unstable(Throwable ex) {
		return new ExecutionResult(ex instanceof MakeBuildUnstableException ? ex
				: new MakeBuildUnstableException(ex));
	}

	private static final class MergedThrowable extends RuntimeException {

		private MergedThrowable(List<Throwable> throwables) {
			super("Failed due to the following exceptions " + throwables);
		}

	}

	private static final class MergedUnstableThrowable
			extends MakeBuildUnstableException {

		private MergedUnstableThrowable(List<Throwable> throwables) {
			super("Unstable due to the following exceptions " + throwables);
		}

	}

}
