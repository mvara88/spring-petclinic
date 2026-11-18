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

package org.springframework.samples.petclinic.vet;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link VetController}
 */

@WebMvcTest(VetController.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetRepository vets;

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(Lists.newArrayList(james(), helen()));
		given(this.vets.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(james(), helen())));
		given(this.vets.findSpecialties()).willReturn(List.of(radiology(), surgery()));

	}

	private Specialty radiology() {
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		return radiology;
	}

	private Specialty surgery() {
		Specialty surgery = new Specialty();
		surgery.setId(2);
		surgery.setName("surgery");
		return surgery;
	}

	@Test
	void showVetListHtml() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(model().attribute("specialties", hasSize(2)))
			.andExpect(model().attribute("selectedSpecialty", nullValue()))
			.andExpect(view().name("vets/vetList"));

	}

	@Test
	void showVetListFilteredBySpecialty() throws Exception {
		given(this.vets.findBySpecialtyName(eq("radiology"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(helen())));

		mockMvc.perform(get("/vets.html").param("specialty", "radiology"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listVets", hasSize(1)))
			.andExpect(model().attribute("selectedSpecialty", "radiology"))
			.andExpect(view().name("vets/vetList"))
			.andExpect(content().string(containsString("<option value=\"radiology\" selected=\"selected\">")));

		verify(this.vets, never()).findAll(any(Pageable.class));
	}

	@Test
	void showVetListWithBlankSpecialtyShowsAllVets() throws Exception {
		mockMvc.perform(get("/vets.html").param("specialty", "  "))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listVets", hasSize(2)))
			.andExpect(model().attribute("selectedSpecialty", nullValue()));

		verify(this.vets, never()).findBySpecialtyName(any(), any(Pageable.class));
	}

	@Test
	void showVetListPaginationLinksKeepSpecialtyFilter() throws Exception {
		given(this.vets.findBySpecialtyName(eq("radiology"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(helen()), PageRequest.of(0, 5), 6));

		mockMvc.perform(get("/vets.html").param("specialty", "radiology"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("totalPages", 2))
			.andExpect(content().string(containsString("/vets.html?page=2&amp;specialty=radiology")));
	}

	@Test
	void showVetListRedirectKeepsSpecialtyFilter() throws Exception {
		given(this.vets.findBySpecialtyName(eq("radiology"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(helen())));

		mockMvc.perform(get("/vets.html").param("page", "3").param("specialty", "radiology"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/vets.html?page=1&specialty=radiology"));
	}

	@Test
	void showVetListRedirectsOutOfBoundsPageToFirstPage() throws Exception {
		for (int page : List.of(0, 2)) {
			mockMvc.perform(get("/vets.html").param("page", Integer.toString(page)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/vets.html?page=1"));
		}
	}

	@Test
	void showResourcesVetList() throws Exception {
		ResultActions actions = mockMvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		actions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.vetList[0].id").value(1));
	}

}
