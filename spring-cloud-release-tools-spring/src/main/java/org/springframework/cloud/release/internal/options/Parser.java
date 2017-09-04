package org.springframework.cloud.release.internal.options;

/**
 * @author Marcin Grzejszczak
 */
public interface Parser {
	Options parse(String[] args);
}
