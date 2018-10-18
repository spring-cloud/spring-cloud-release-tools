package org.springframework.cloud.release.internal.docs;

import java.util.Objects;

/**
 * @author Marcin Grzejszczak
 */
public class Title {

	final String lastGaTrainName;
	final String currentGaTrainName;
	final String currentSnapshotTrainName;

	Title(String[] row) {
		int initialIndex = row.length == 4 ? 1 : 2;
		this.lastGaTrainName = row[initialIndex].trim();
		this.currentGaTrainName = row[initialIndex + 1].trim();
		this.currentSnapshotTrainName = row[initialIndex + 2].trim();
	}

	Title(String lastGaTrainName, String currentGaTrainName, String currentSnapshotTrainName) {
		this.lastGaTrainName = lastGaTrainName.trim();
		this.currentGaTrainName = currentGaTrainName.trim();
		this.currentSnapshotTrainName = currentSnapshotTrainName.trim();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Title title = (Title) o;
		return Objects.equals(this.lastGaTrainName, title.lastGaTrainName) &&
				Objects.equals(this.currentGaTrainName, title.currentGaTrainName) &&
				Objects
						.equals(this.currentSnapshotTrainName, title.currentSnapshotTrainName);
	}

	@Override
	public int hashCode() {
		return Objects
				.hash(this.lastGaTrainName, this.currentGaTrainName, this.currentSnapshotTrainName);
	}

	public String getLastGaTrainName() {
		return this.lastGaTrainName;
	}

	public String getCurrentGaTrainName() {
		return this.currentGaTrainName;
	}

	public String getCurrentSnapshotTrainName() {
		return this.currentSnapshotTrainName;
	}
}
