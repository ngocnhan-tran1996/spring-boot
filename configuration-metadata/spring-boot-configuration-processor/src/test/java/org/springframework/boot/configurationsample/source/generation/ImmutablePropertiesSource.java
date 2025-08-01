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

package org.springframework.boot.configurationsample.source.generation;

import org.springframework.boot.configurationsample.ConfigurationPropertiesSource;
import org.springframework.boot.configurationsample.DefaultValue;

@ConfigurationPropertiesSource
public class ImmutablePropertiesSource {

	/**
	 * Description of this simple property.
	 */
	@SuppressWarnings("unused")
	private final String name;

	/**
	 * Whether it is enabled.
	 */
	@SuppressWarnings("unused")
	private final boolean enabled;

	public ImmutablePropertiesSource(@DefaultValue("boot") String name, boolean enabled) {
		this.name = name;
		this.enabled = enabled;
	}

}
