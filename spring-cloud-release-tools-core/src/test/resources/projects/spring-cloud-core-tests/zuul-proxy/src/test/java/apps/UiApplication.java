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

package apps;

import java.io.IOException;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@SpringBootApplication
@RestController
public class UiApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(UiApplication.class)
				.properties("spring.config.name:ui").run(args);
	}

	@RequestMapping("/")
	public String home(@RequestParam(required = false) String value) {
		return "Hello " + (value == null ? "World" : value);
	}

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String upload() {
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder
				.fromCurrentContextPath();
		return "<html><body>"
				+ "<form method='post' enctype='multipart/form-data' action='"
				+ builder.path("/upload").build().toUriString() + "'>"
				+ "File to upload: <input type='file' name='file'>"
				+ "<input type='submit' value='Upload'></form>" + "</body></html>";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String accept(@RequestParam MultipartFile file) throws IOException {
		return new String(file.getBytes());
	}

}
