/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.samples.petclinic.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies that only non-sensitive actuator endpoints are exposed over HTTP.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ActuatorExposureIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	private RestTemplateBuilder builder;

	private RestTemplate template;

	@BeforeEach
	void setUp() {
		this.template = this.builder.baseUri("http://localhost:" + this.port).build();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/actuator/health", "/actuator/info" })
	void safeEndpointsAreExposed(String path) {
		ResponseEntity<String> result = this.template.getForEntity(path, String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/actuator/env", "/actuator/heapdump", "/actuator/threaddump", "/actuator/configprops",
			"/actuator/beans", "/actuator/loggers", "/actuator/mappings", "/actuator/metrics", "/actuator/caches",
			"/actuator/conditions", "/actuator/scheduledtasks", "/actuator/sbom" })
	void sensitiveEndpointsAreNotExposed(String path) {
		assertThatExceptionOfType(HttpClientErrorException.NotFound.class)
			.isThrownBy(() -> this.template.getForEntity(path, String.class));
	}

	@Test
	void discoveryPageOnlyListsSafeEndpoints() {
		String body = this.template.getForObject("/actuator", String.class);
		assertThat(body).contains("/actuator/health", "/actuator/info")
			.doesNotContain("/actuator/env", "/actuator/heapdump", "/actuator/configprops");
	}

}
