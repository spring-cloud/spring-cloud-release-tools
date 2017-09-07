package org.springframework.cloud.release.internal.options;

/**
 * Converts input arguments to a {@link Options}
 *
 * @author Marcin Grzejszczak
 */
public interface Parser {
	Options parse(String[] args);
}
