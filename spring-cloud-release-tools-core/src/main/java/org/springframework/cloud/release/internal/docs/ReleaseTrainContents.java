package org.springframework.cloud.release.internal.docs;

import java.util.List;
import java.util.Objects;

public class ReleaseTrainContents {
	final Title title;
	final List<Row> rows;

	ReleaseTrainContents(Title title, List<Row> rows) {
		this.title = title;
		this.rows = rows;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ReleaseTrainContents contents = (ReleaseTrainContents) o;
		return Objects.equals(this.title, contents.title) &&
				Objects.equals(this.rows, contents.rows);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.title, this.rows);
	}

	public Title getTitle() {
		return this.title;
	}

	public List<Row> getRows() {
		return this.rows;
	}
}

