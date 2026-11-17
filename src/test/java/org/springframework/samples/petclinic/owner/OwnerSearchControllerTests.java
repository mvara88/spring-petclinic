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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for {@link OwnerSearchController}
 */
@WebMvcTest(OwnerSearchController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerSearchControllerTests {

	private static final Sort EXPECTED_SORT = Sort.by("lastName", "firstName", "id");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner owner(int id, String firstName, String lastName, String city) {
		Owner owner = new Owner();
		owner.setId(id);
		owner.setFirstName(firstName);
		owner.setLastName(lastName);
		owner.setAddress("110 W. Liberty St.");
		owner.setCity(city);
		owner.setTelephone("6085551023");
		return owner;
	}

	@Test
	void searchByCityReturnsMatchingOwnersAsJson() throws Exception {
		given(this.owners.findByCityStartingWithIgnoreCase("Madison", EXPECTED_SORT))
			.willReturn(List.of(owner(8, "Maria", "Escobito", "Madison"), owner(1, "George", "Franklin", "Madison")));

		mockMvc.perform(get("/api/v1/owners/search").param("city", "Madison").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].id").value(8))
			.andExpect(jsonPath("$[0].lastName").value("Escobito"))
			.andExpect(jsonPath("$[0].city").value("Madison"))
			.andExpect(jsonPath("$[0].pets", hasSize(0)))
			.andExpect(jsonPath("$[1].lastName").value("Franklin"));
	}

	@Test
	void searchByCityStripsSurroundingWhitespace() throws Exception {
		given(this.owners.findByCityStartingWithIgnoreCase("Sun Prairie", EXPECTED_SORT))
			.willReturn(List.of(owner(2, "Betty", "Davis", "Sun Prairie")));

		mockMvc.perform(get("/api/v1/owners/search").param("city", "  Sun Prairie "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].city").value("Sun Prairie"));
	}

	@Test
	void searchByCityReturnsEmptyArrayWhenNoOwnersMatch() throws Exception {
		given(this.owners.findByCityStartingWithIgnoreCase("Atlantis", EXPECTED_SORT)).willReturn(List.of());

		mockMvc.perform(get("/api/v1/owners/search").param("city", "Atlantis"))
			.andExpect(status().isOk())
			.andExpect(content().json("[]"));
		verify(this.owners).findByCityStartingWithIgnoreCase("Atlantis", EXPECTED_SORT);
	}

	@Test
	void searchByCityAcceptsPunctuationUsedInCityNames() throws Exception {
		given(this.owners.findByCityStartingWithIgnoreCase("Coeur d'Alene-St. Maries", EXPECTED_SORT))
			.willReturn(List.of());

		mockMvc.perform(get("/api/v1/owners/search").param("city", "Coeur d'Alene-St. Maries"))
			.andExpect(status().isOk());
	}

	@Test
	void searchByCityRejectsMissingCity() throws Exception {
		mockMvc.perform(get("/api/v1/owners/search"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid owner search"))
			.andExpect(jsonPath("$.detail").value("city is required"));
		verify(this.owners, never()).findByCityStartingWithIgnoreCase(anyString(), any(Sort.class));
	}

	@Test
	void searchByCityRejectsBlankCity() throws Exception {
		mockMvc.perform(get("/api/v1/owners/search").param("city", "   "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("city must not be blank"));
		verify(this.owners, never()).findByCityStartingWithIgnoreCase(anyString(), any(Sort.class));
	}

	@Test
	void searchByCityRejectsTooLongCity() throws Exception {
		mockMvc.perform(get("/api/v1/owners/search").param("city", "a".repeat(81)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("city must be at most 80 characters"));
		verify(this.owners, never()).findByCityStartingWithIgnoreCase(anyString(), any(Sort.class));
	}

	@Test
	void searchByCityRejectsInvalidCharacters() throws Exception {
		mockMvc.perform(get("/api/v1/owners/search").param("city", "Mad%"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("may only contain letters")));
		verify(this.owners, never()).findByCityStartingWithIgnoreCase(anyString(), any(Sort.class));
	}

}
