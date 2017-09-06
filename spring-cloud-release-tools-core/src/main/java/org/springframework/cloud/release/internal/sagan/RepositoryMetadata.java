package org.springframework.cloud.release.internal.sagan;

/**
 * @author Marcin Grzejszczak
 */
class RepositoryMetadata {
	private String id;
	private String name;
	private String url;
	private boolean snapshotsEnabled;

	public RepositoryMetadata() {
	}

	public RepositoryMetadata(String id, String name, String url,
			boolean snapshotsEnabled) {
		this.id = id;
		this.name = name;
		this.url = url;
		this.snapshotsEnabled = snapshotsEnabled;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isSnapshotsEnabled() {
		return this.snapshotsEnabled;
	}

	public void setSnapshotsEnabled(boolean snapshotsEnabled) {
		this.snapshotsEnabled = snapshotsEnabled;
	}
}
