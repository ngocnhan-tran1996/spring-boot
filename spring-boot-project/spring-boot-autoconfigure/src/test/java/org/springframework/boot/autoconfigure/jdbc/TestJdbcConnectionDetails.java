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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.boot.jdbc.DatabaseDriver;

/**
 * {@link JdbcConnectionDetails} used in tests.
 *
 * @author Moritz Halbritter
 */
class TestJdbcConnectionDetails implements JdbcConnectionDetails {

	@Override
	public String getJdbcUrl() {
		return "jdbc:customdb://customdb.example.com:12345/database-1";
	}

	@Override
	public String getUsername() {
		return "user-1";
	}

	@Override
	public String getPassword() {
		return "password-1";
	}

	@Override
	public String getDriverClassName() {
		return DatabaseDriver.POSTGRESQL.getDriverClassName();
	}

	@Override
	public String getXaDataSourceClassName() {
		return DatabaseDriver.POSTGRESQL.getXaDataSourceClassName();
	}

}
