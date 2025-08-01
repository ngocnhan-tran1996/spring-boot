/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.jpa.web;

import java.util.List;

import smoketest.jpa.domain.Note;
import smoketest.jpa.repository.NoteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

	@Autowired
	private NoteRepository noteRepository;

	@GetMapping("/")
	@Transactional(readOnly = true)
	public ModelAndView index() {
		List<Note> notes = this.noteRepository.findAll();
		ModelAndView modelAndView = new ModelAndView("index");
		modelAndView.addObject("notes", notes);
		return modelAndView;
	}

}
