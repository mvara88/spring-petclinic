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

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.sql=DEBUG")
public class PetClinicIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private RestTemplateBuilder builder;

	@Test
	void findAll() {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void ownersExport() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<List<Map<String, Object>>> result = template
			.exchange(RequestEntity.get("/api/v1/owners/export").build(), new ParameterizedTypeReference<>() {
			});
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isNotEmpty();
		assertThat(result.getBody().get(0)).containsEntry("id", 1).containsEntry("lastName", "Franklin");
	}

	@Test
	void ownersSearchByCity() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<List<Map<String, Object>>> result = template.exchange(
				RequestEntity.get("/api/v1/owners/search?city={city}", "madison").build(),
				new ParameterizedTypeReference<>() {
				});
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).extracting(owner -> owner.get("lastName"))
			.containsExactly("Escobito", "Franklin", "McTavish", "Schroeder");
	}

	@Test
	void ownersSearchByCityRejectsInvalidCity() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
			.isThrownBy(() -> template.getForEntity("/api/v1/owners/search?city={city}", String.class, "Mad%"))
			.satisfies(ex -> assertThat(ex.getResponseBodyAsString()).contains("may only contain letters"));
	}

	@Test
	void ownerList() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/owners?lastName=").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, "--spring.docker.compose.lifecycle-management=NONE");
	}

}
