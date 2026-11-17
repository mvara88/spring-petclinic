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

package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for {@link OwnerExportController}
 */
@WebMvcTest(OwnerExportController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerExportControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner george() {
		Owner george = new Owner();
		george.setId(1);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");
		PetType cat = new PetType();
		cat.setId(1);
		cat.setName("cat");
		Pet leo = new Pet();
		leo.setId(1);
		leo.setName("Leo");
		leo.setBirthDate(LocalDate.of(2010, 9, 7));
		leo.setType(cat);
		Visit visit = new Visit();
		visit.setId(1);
		visit.setDate(LocalDate.of(2013, 1, 1));
		visit.setDescription("rabies shot");
		leo.addVisit(visit);
		george.addPet(leo);
		return george;
	}

	private Owner betty() {
		Owner betty = new Owner();
		betty.setId(2);
		betty.setFirstName("Betty");
		betty.setLastName("Davis");
		betty.setAddress("638 Cardinal Ave.");
		betty.setCity("Sun Prairie");
		betty.setTelephone("6085551749");
		return betty;
	}

	@Test
	void exportOwnersReturnsOwnersWithPetsAndVisitsAsJson() throws Exception {
		given(this.owners.findAll(Sort.by("id"))).willReturn(List.of(george(), betty()));

		mockMvc.perform(get("/api/v1/owners/export").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].firstName").value("George"))
			.andExpect(jsonPath("$[0].lastName").value("Franklin"))
			.andExpect(jsonPath("$[0].address").value("110 W. Liberty St."))
			.andExpect(jsonPath("$[0].city").value("Madison"))
			.andExpect(jsonPath("$[0].telephone").value("6085551023"))
			.andExpect(jsonPath("$[0].pets", hasSize(1)))
			.andExpect(jsonPath("$[0].pets[0].id").value(1))
			.andExpect(jsonPath("$[0].pets[0].name").value("Leo"))
			.andExpect(jsonPath("$[0].pets[0].birthDate").value("2010-09-07"))
			.andExpect(jsonPath("$[0].pets[0].type").value("cat"))
			.andExpect(jsonPath("$[0].pets[0].visits", hasSize(1)))
			.andExpect(jsonPath("$[0].pets[0].visits[0].id").value(1))
			.andExpect(jsonPath("$[0].pets[0].visits[0].date").value("2013-01-01"))
			.andExpect(jsonPath("$[0].pets[0].visits[0].description").value("rabies shot"))
			.andExpect(jsonPath("$[1].id").value(2))
			.andExpect(jsonPath("$[1].firstName").value("Betty"))
			.andExpect(jsonPath("$[1].pets", hasSize(0)));

		verify(this.owners).findAll(Sort.by("id"));
	}

	@Test
	void exportOwnersDoesNotExposeEntityInternals() throws Exception {
		given(this.owners.findAll(Sort.by("id"))).willReturn(List.of(george()));

		mockMvc.perform(get("/api/v1/owners/export"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].new").doesNotExist())
			.andExpect(jsonPath("$[0].pets[0].new").doesNotExist())
			.andExpect(jsonPath("$[0].pets[0].type.id").doesNotExist());
	}

	@Test
	void exportOwnersReturnsEmptyArrayWhenNoOwners() throws Exception {
		given(this.owners.findAll(Sort.by("id"))).willReturn(List.of());

		mockMvc.perform(get("/api/v1/owners/export")).andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void exportOwnersHandlesPetWithoutType() throws Exception {
		Owner george = george();
		george.getPet("Leo").setType(null);
		given(this.owners.findAll(Sort.by("id"))).willReturn(List.of(george));

		mockMvc.perform(get("/api/v1/owners/export"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].pets[0].type").value(nullValue()));
	}

}
