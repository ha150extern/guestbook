/*
 * Copyright 2014-2021 the original author or authors.
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
package guestbook;

import io.github.wimdeblauwe.hsbt.mvc.HtmxResponse;
import io.github.wimdeblauwe.hsbt.mvc.HxRequest;
import jakarta.validation.Valid;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A controller to handle web requests to manage {@link GuestbookEntry}s
 *
 * @author Paul Henke
 * @author Oliver Drotbohm
 */
@Controller
class GuestbookController {

	private final GuestbookRepository guestbook;

	/**
	 * Creates a new {@link GuestbookController} using the given {@link GuestbookRepository}. Spring will look for a bean
	 * of type {@link GuestbookRepository} and hand this into this class when an instance is created.
	 *
	 * @param guestbook must not be {@literal null}
	 */
	GuestbookController(GuestbookRepository guestbook) {

		Assert.notNull(guestbook, "Guestbook must not be null!");
		this.guestbook = guestbook;
	}

	/**
	 * Handles requests to the application root URI. Note, that you can use {@code redirect:} as prefix to trigger a
	 * browser redirect instead of simply rendering a view.
	 *
	 * @return a redirect string
	 */
	@GetMapping(path = "/")
	String index() {
		return "redirect:/guestbook";
	}

	/**
	 * Handles requests to access the guestbook. Obtains all currently available {@link GuestbookEntry}s and puts them
	 * into the {@link Model} that's used to render the view.
	 *
	 * @param model the model that's used to render the view
	 * @param form the form to be added to the model
	 * @return a view name
	 */
	@GetMapping(path = "/guestbook")
	String guestBook(Model model, @ModelAttribute(binding = false) GuestbookForm form) {

		model.addAttribute("entries", guestbook.findAll());
		model.addAttribute("form", form);

		return "guestbook";
	}

	/**
	 * Handles requests to create a new {@link GuestbookEntry}. Spring MVC automatically validates and binds the HTML form
	 * to the {@code form} parameter. Validation or binding errors, if any, are exposed via the {@code
	 * errors} parameter.
	 *
	 * @param form the form submitted by the user
	 * @param errors an object that stores any form validation or data binding errors
	 * @param model the model that's used to render the view
	 * @return a redirect string
	 */
	@PostMapping(path = "/guestbook")
	String addEntry(@Valid @ModelAttribute("form") GuestbookForm form, Errors errors, Model model) {

		if (errors.hasErrors()) {
			return guestBook(model, form);
		}

		guestbook.save(form.toNewEntry());

		return "redirect:/guestbook";
	}

	/**
	 * Deletes a {@link GuestbookEntry}. This request can only be performed by authenticated users with admin privileges.
	 * Also note how the path variable used in the {@link DeleteMapping} annotation is bound to an {@link Optional}
	 * parameter of the controller method using the {@link PathVariable} annotation. If the entry couldn't be found, that
	 * {@link Optional} will be empty.
	 *
	 * @param entry an {@link Optional} with the {@link GuestbookEntry} to delete
	 * @return a redirect string
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping(path = "/guestbook/{entry}")
	String removeEntry(@PathVariable Optional<GuestbookEntry> entry) {

		return entry.map(it -> {

			guestbook.delete(it);
		
			return "redirect:/guestbook";

		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
	
	/*
	 * Methode für Editieren
	 * */
	@GetMapping("/guestbook/edit/{id}")
	public String showUpdateForm(@PathVariable("id") String id, Model model) {
	    GuestbookEntry entry = guestbook.findById(Long.parseLong(id))
	      .orElseThrow(() -> new IllegalArgumentException("Invalid Id:" + id));
	    
	    model.addAttribute("entry", entry);
	    return "update-entry";
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping(path="/guestbook/update/{entry}")
	String updateEntry(@Valid @ModelAttribute("form") GuestbookForm form, @PathVariable Optional<GuestbookEntry> entry){
		
		return entry.map(it -> {
			it.setName(form.getName());
			it.setText(form.getText());
			it.setEmail(form.getEmail());
			guestbook.save(it);
		
			return "redirect:/guestbook";

		}).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		
	}
}
