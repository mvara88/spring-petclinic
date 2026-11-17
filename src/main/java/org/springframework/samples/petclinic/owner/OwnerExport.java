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

/**
 * Read-only JSON representation of an {@link Owner} with its pets and visits, used by
 * {@link OwnerExportController}.
 */
public record OwnerExport(Integer id, String firstName, String lastName, String address, String city, String telephone,
		List<PetExport> pets) {

	static OwnerExport from(Owner owner) {
		return new OwnerExport(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getAddress(),
				owner.getCity(), owner.getTelephone(), owner.getPets().stream().map(PetExport::from).toList());
	}

	public record PetExport(Integer id, String name, LocalDate birthDate, String type, List<VisitExport> visits) {

		static PetExport from(Pet pet) {
			PetType type = pet.getType();
			return new PetExport(pet.getId(), pet.getName(), pet.getBirthDate(), type != null ? type.getName() : null,
					pet.getVisits().stream().map(VisitExport::from).toList());
		}

	}

	public record VisitExport(Integer id, LocalDate date, String description) {

		static VisitExport from(Visit visit) {
			return new VisitExport(visit.getId(), visit.getDate(), visit.getDescription());
		}

	}

}
