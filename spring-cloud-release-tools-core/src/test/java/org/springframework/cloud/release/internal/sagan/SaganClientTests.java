package org.springframework.cloud.release.internal.sagan;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@AutoConfigureStubRunner(ids = "sagan:sagan-site")
public class SaganClientTests {

	@Value("${stubrunner.runningstubs.sagan-site.port}") Integer saganPort;


}