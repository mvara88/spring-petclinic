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

import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * REST endpoint searching owners, with their pets and visits, by city.
 */
@RestController
@RequestMapping("/api/v1/owners")
class OwnerSearchController {

	static final int CITY_MAX_LENGTH = 80;

	private final OwnerRepository owners;

	public OwnerSearchController(OwnerRepository owners) {
		this.owners = owners;
	}

	/**
	 * Find owners whose city starts with the given value, ignoring case and surrounding
	 * whitespace. Results are ordered by last name, then first name.
	 * @param city the city, or the start of it, to search for
	 * @return the matching owners, or an empty list if none match
	 */
	@GetMapping("/search")
	public List<OwnerExport> searchByCity(@RequestParam @NotBlank(message = "must not be blank") @Size(
			max = CITY_MAX_LENGTH,
			message = "must be at most " + CITY_MAX_LENGTH + " characters") @Pattern(regexp = "[\\p{L} .'-]*",
					message = "may only contain letters, spaces, periods, apostrophes and hyphens") String city) {
		Sort sort = Sort.by("lastName", "firstName", "id");
		return this.owners.findByCityStartingWithIgnoreCase(city.strip(), sort)
			.stream()
			.map(OwnerExport::from)
			.toList();
	}

	@ExceptionHandler
	ProblemDetail handleInvalidCity(HandlerMethodValidationException ex) {
		List<String> errors = ex.getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage).toList();
		return badRequest("city " + String.join(", ", errors));
	}

	@ExceptionHandler
	ProblemDetail handleMissingCity(MissingServletRequestParameterException ex) {
		return badRequest(ex.getParameterName() + " is required");
	}

	private static ProblemDetail badRequest(String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Invalid owner search");
		return problem;
	}

}
