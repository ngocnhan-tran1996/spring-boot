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

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link AssertableWebApplicationContext}.
 *
 * @author Phillip Webb
 * @see ApplicationContextAssertProviderTests
 */
class AssertableWebApplicationContextTests {

	@Test
	@SuppressWarnings("resource")
	void getShouldReturnProxy() {
		AssertableWebApplicationContext context = AssertableWebApplicationContext
			.get(() -> mock(ConfigurableWebApplicationContext.class));
		assertThat(context).isInstanceOf(ConfigurableWebApplicationContext.class);
	}

	@Test
	void getWhenHasAdditionalInterfaceShouldReturnProxy() {
		try (ConfigurableWebApplicationContext context = AssertableWebApplicationContext.get(
				() -> mock(ConfigurableWebApplicationContext.class,
						withSettings().extraInterfaces(AdditionalContextInterface.class)),
				AdditionalContextInterface.class)) {
			assertThat(context).isInstanceOf(ConfigurableWebApplicationContext.class)
				.isInstanceOf(AdditionalContextInterface.class);
		}
	}

}
