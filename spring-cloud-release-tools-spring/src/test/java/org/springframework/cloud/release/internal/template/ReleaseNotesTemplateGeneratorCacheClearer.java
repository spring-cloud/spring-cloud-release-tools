package org.springframework.cloud.release.internal.template;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseNotesTemplateGeneratorCacheClearer {

	public static void clear() {
		ReleaseNotesTemplateGenerator.CACHE.clear();
	}
}
