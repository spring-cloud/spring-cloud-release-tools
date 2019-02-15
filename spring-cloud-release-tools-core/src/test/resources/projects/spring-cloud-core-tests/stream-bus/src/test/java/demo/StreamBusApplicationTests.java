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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.bus.SpringCloudBusClient;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class StreamBusApplicationTests {

	@Autowired
	private Source source;

	@Autowired
	private SpringCloudBusClient bus;

	@Autowired
	private MessageCollector collector;

	@Test
	public void streaminess() throws Exception {
		Message<String> message = MessageBuilder.withPayload("Hello").build();
		this.source.output().send(message);
		Message<?> received = this.collector.forChannel(this.source.output()).take();
		assertThat(received.getPayload()).isEqualTo(message.getPayload());
		assertThat(received.getHeaders()).containsKeys("contentType", "id", "timestamp");
	}

	@Test
	public void business() throws Exception {
		Message<RefreshRemoteApplicationEvent> message = MessageBuilder
				.withPayload(new RefreshRemoteApplicationEvent(this, "me", "you"))
				.build();
		this.bus.springCloudBusOutput().send(message);
		String payload = (String) this.collector
				.forChannel(this.bus.springCloudBusOutput()).take().getPayload();
		assertTrue("Wrong payload: " + payload, payload.contains("\"type\""));
	}

}
