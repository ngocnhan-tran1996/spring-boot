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

package org.springframework.boot.context.properties;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Exception thrown when the application has configured an incompatible set of
 * {@link ConfigurationProperties} keys.
 *
 * @author Brian Clozel
 * @since 2.4.0
 */
public class IncompatibleConfigurationException extends RuntimeException {

	private final List<String> incompatibleKeys;

	public IncompatibleConfigurationException(String... incompatibleKeys) {
		super("The following configuration properties have incompatible values: " + Arrays.toString(incompatibleKeys));
		this.incompatibleKeys = Arrays.asList(incompatibleKeys);
	}

	public Collection<String> getIncompatibleKeys() {
		return this.incompatibleKeys;
	}

}
