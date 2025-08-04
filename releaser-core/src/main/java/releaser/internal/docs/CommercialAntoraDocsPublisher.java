/*
 * Copyright 2013-2025 the original author or authors.
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

package releaser.internal.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.transfermanager.ParallelUploadConfig;
import com.google.cloud.storage.transfermanager.TransferManager;
import com.google.cloud.storage.transfermanager.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.StringUtils;

/**
 * This code assumes that the environment variable GOOGLE_APPLICATION_CREDENTIALS is set
 * to the path to a json file containing a GCP service account.
 *
 * @author Ryan Baxter
 */
public class CommercialAntoraDocsPublisher implements AntoraDocsPublisher {

	private static final Logger log = LoggerFactory.getLogger(CommercialAntoraDocsPublisher.class);

	private static final String GOOGLE_APPLICATION_CREDENTIALS_VAR_NAME = "GOOGLE_APPLICATION_CREDENTIALS";

	private final TransferManager transferManager;

	public CommercialAntoraDocsPublisher(TransferManager transferManager) {
		this.transferManager = transferManager;
	}

	@Override
	public void publish(File project, ReleaserProperties properties) throws IOException {
		checkAndLog();
		String antoraSite = project.getAbsolutePath() + "/target/antora/site";
		// TransferManager transferManager =
		// TransferManagerConfig.newBuilder().build().getService();
		ParallelUploadConfig.UploadBlobInfoFactory uploadBlobInfoFactory = (String bucketName, String fileName) -> {
			ProjectVersion version = new ProjectVersion(project);
			String blobLocation = version.projectName + "/reference" + fileName.substring(antoraSite.length());
			Optional<MediaType> mediaTypeOptional = MediaTypeFactory.getMediaType(blobLocation);
			BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(bucketName, blobLocation);
			mediaTypeOptional.ifPresent(mediaType -> blobInfoBuilder.setContentType(mediaType.toString()));
			return blobInfoBuilder.build();
		};
		ParallelUploadConfig parallelUploadConfig = ParallelUploadConfig.newBuilder()
				.setUploadBlobInfoFactory(uploadBlobInfoFactory)
				.setBucketName(properties.getAntora().getGcpBucketName()).build();
		// Create a list to store the file paths
		List<Path> filePaths = new ArrayList<>();
		// Get all files in the directory
		try (Stream<Path> pathStream = Files.walk(Path.of(antoraSite))) {
			pathStream.filter(Files::isRegularFile).forEach(filePaths::add);
		}
		List<UploadResult> results = transferManager.uploadFiles(filePaths, parallelUploadConfig).getUploadResults();
		for (UploadResult result : results) {
			log.info("Upload for {} completed with status {}", result.getInput().getName(), result.getStatus());
		}
	}

	private void checkAndLog() {
		if (!StringUtils.hasText(System.getenv(GOOGLE_APPLICATION_CREDENTIALS_VAR_NAME))) {
			log.warn("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set, "
					+ "publishing commercial Antora docs MAY fail.");
		}
	}

}
