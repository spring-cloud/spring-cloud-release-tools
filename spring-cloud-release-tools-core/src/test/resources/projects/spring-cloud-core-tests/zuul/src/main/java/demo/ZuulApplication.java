/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package demo;

import javax.servlet.http.HttpServletResponse;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableZuulServer
public class ZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZuulApplication.class, args);
	}

}

@Component
class SampleStaticResponseFilter extends ZuulFilter {

	private static final String URI = "/static";

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 0;
	}

	@Override
	public boolean shouldFilter() {
		String path = RequestContext.getCurrentContext().getRequest().getRequestURI();
		if (checkPath(path))
			return true;
		if (checkPath("/" + path))
			return true;
		return false;
	}

	private boolean checkPath(String path) {
		return URI.equals(path);
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		// Set the default response code for static filters to be 200
		ctx.setResponseStatusCode(HttpServletResponse.SC_OK);
		// first StaticResponseFilter instance to match wins, others do not set body
		// and/or status
		if (ctx.getResponseBody() == null) {
			ctx.setResponseBody("Hello World");
			ctx.setSendZuulResponse(false);
		}
		return null;
	}

}