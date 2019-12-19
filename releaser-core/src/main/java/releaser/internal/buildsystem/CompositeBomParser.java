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

package releaser.internal.buildsystem;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

class CompositeBomParser implements BomParser {

	private final List<BomParser> parsers;

	CompositeBomParser(List<BomParser> parsers) {
		this.parsers = parsers;
	}

	@Override
	public boolean isApplicable(File clonedBom) {
		return this.parsers.stream().anyMatch(b -> b.isApplicable(clonedBom));
	}

	@Override
	public VersionsFromBom versionsFromBom(File thisProjectRoot) {
		return firstMatching(thisProjectRoot).versionsFromBom(thisProjectRoot);
	}

	private BomParser firstMatching(File thisProjectRoot) {
		return this.parsers.stream().filter(b -> b.isApplicable(thisProjectRoot))
				.findFirst().orElseThrow(
						() -> new IllegalStateException("Can't find a matching parser"));
	}

	@Override
	public List<CustomBomParser> customBomParsers() {
		return this.parsers.stream().flatMap(b -> b.customBomParsers().stream())
				.collect(Collectors.toList());
	}

}
