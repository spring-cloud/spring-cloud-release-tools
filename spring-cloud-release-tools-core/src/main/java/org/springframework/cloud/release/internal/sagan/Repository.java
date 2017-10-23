package org.springframework.cloud.release.internal.sagan;

/**
 * @author Marcin Grzejszczak
 */
public class Repository {
	public String id;
	public String name;
	public String url;
	public boolean snapshotsEnabled;

	@Override public String toString() {
		return "Repository{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", url='"
				+ url + '\'' + ", snapshotsEnabled=" + snapshotsEnabled + '}';
	}
}
