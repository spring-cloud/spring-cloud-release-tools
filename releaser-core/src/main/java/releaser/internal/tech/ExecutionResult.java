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

package releaser.internal.tech;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Task execution result. Contains a list of exceptions thrown while running the task.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionResult implements Serializable {

	private List<Exception> exceptions = new LinkedList<>();

	/**
	 * Consider an enum in the future.
	 */
	private boolean skipped;

	public ExecutionResult() {
	}

	public ExecutionResult(Exception throwable) {
		this.exceptions.add(throwable);
		this.skipped = false;
	}

	public ExecutionResult(List<Exception> throwables) {
		this.exceptions.addAll(throwables);
		this.skipped = false;
	}

	public ExecutionResult(boolean skipped) {
		this.skipped = skipped;
	}

	public static ExecutionResult success() {
		return new ExecutionResult();
	}

	public static ExecutionResult failure(Exception throwable) {
		return new ExecutionResult(throwable);
	}

	public static ExecutionResult failure(List<Exception> throwables) {
		return new ExecutionResult(throwables);
	}

	public static ExecutionResult unstable(Exception ex) {
		return new ExecutionResult(ex instanceof BuildUnstableException ? ex
				: new BuildUnstableException(ex));
	}

	public static ExecutionResult skipped() {
		return new ExecutionResult(true);
	}

	public RuntimeException foundExceptions() {
		if (this.exceptions.isEmpty()) {
			return null;
		}
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
				.allMatch(t -> t instanceof BuildUnstableException);
	}

	public String toStringResult() {
		return isUnstable() ? "UNSTABLE"
				: isFailure() ? "FAILURE" : isSkipped() ? "SKIPPED" : "SUCCESS";
	}

	public boolean isFailure() {
		return !this.exceptions.isEmpty() && this.exceptions.stream()
				.anyMatch(t -> !(t instanceof BuildUnstableException));
	}

	public boolean isSuccess() {
		return this.exceptions.isEmpty();
	}

	public boolean isFailureOrUnstable() {
		return !this.exceptions.isEmpty();
	}

	public List<Exception> getExceptions() {
		return this.exceptions;
	}

	public void setExceptions(List<Exception> exceptions) {
		this.exceptions = exceptions;
	}

	public boolean isSkipped() {
		return this.skipped;
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}

	private static final class MergedThrowable extends RuntimeException
			implements Serializable {

		private MergedThrowable(List<Exception> throwables) {
			super("Failed due to the following exceptions " + throwables);
		}

	}

	private static final class MergedUnstableThrowable extends BuildUnstableException
			implements Serializable {

		private MergedUnstableThrowable(List<Exception> throwables) {
			super("Unstable due to the following exceptions " + throwables);
		}

	}

}
