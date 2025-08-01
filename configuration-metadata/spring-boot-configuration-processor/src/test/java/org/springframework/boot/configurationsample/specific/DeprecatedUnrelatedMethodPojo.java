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

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.ConfigurationProperties;

/**
 * Demonstrate that an unrelated setter is not taken into account to detect the deprecated
 * flag.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("not.deprecated")
public class DeprecatedUnrelatedMethodPojo {

	private Integer counter;

	private boolean flag;

	public Integer getCounter() {
		return this.counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}

	@Deprecated
	public void setCounter(String counterAsString) {
		this.counter = Integer.valueOf(counterAsString);
	}

	public boolean isFlag() {
		return this.flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	@Deprecated
	public void setFlag(Boolean flag) {
		this.flag = flag;
	}

}
