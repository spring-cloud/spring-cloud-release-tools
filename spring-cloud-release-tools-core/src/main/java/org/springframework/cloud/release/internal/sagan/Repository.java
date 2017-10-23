package org.springframework.cloud.release.internal.sagan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Marcin Grzejszczak
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Repository {
	public String id;
	public String name;
	public String url;
	public Boolean snapshotsEnabled;

	@Override public String toString() {
		return "Repository{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", url='"
				+ url + '\'' + ", snapshotsEnabled=" + snapshotsEnabled + '}';
	}
}
