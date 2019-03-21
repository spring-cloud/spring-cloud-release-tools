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

package demo;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

// import org.springframework.http.HttpMethod;
// import org.springframework.http.client.ClientHttpRequest;
// import org.springframework.security.oauth2.client.OAuth2RestTemplate;
// import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class RibbonClientApplicationTests {

	// FIXME boot 2.0.0 spring-security 5.0 missing oauth
	/*
	 * @Autowired
	 *
	 * @LoadBalanced private OAuth2RestTemplate oauth2RestTemplate;
	 *
	 * private MockHttpServletRequest request = new MockHttpServletRequest();
	 *
	 * @Rule public ExpectedException expected = ExpectedException.none();
	 *
	 * @After public void clean() { RequestContextHolder.resetRequestAttributes(); }
	 */

	@Test
	@Ignore
	public void oauth2RestTemplateHasLoadBalancer() throws Exception {
		/*
		 * // Just to prove that the interceptor is present... ClientHttpRequest request =
		 * oauth2RestTemplate.getRequestFactory() .createRequest(new
		 * URI("https://nosuchservice"), HttpMethod.GET);
		 * expected.expectMessage("No instances available for nosuchservice");
		 * request.execute();
		 */
	}

	/*
	 * @Test public void useRestTemplate() throws Exception { // There's nowhere to get an
	 * access token so it should fail, but in a sensible way
	 * this.expected.expect(UserRedirectRequiredException.class); RequestContextHolder
	 * .setRequestAttributes(new ServletRequestAttributes(this.request));
	 * this.oauth2RestTemplate.getForEntity("https://foo/bar", String.class); }
	 */

}
