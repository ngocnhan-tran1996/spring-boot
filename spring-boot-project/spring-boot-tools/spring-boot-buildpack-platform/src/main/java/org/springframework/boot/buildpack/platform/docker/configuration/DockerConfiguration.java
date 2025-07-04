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

package org.springframework.boot.buildpack.platform.docker.configuration;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
 * {@link org.springframework.boot.buildpack.platform.build.BuilderDockerConfiguration}.
 */
@Deprecated(since = "3.5.0", forRemoval = true)
@SuppressWarnings("removal")
public final class DockerConfiguration {

	private final DockerHostConfiguration host;

	private final DockerRegistryAuthentication builderAuthentication;

	private final DockerRegistryAuthentication publishAuthentication;

	private final boolean bindHostToBuilder;

	public DockerConfiguration() {
		this(null, null, null, false);
	}

	private DockerConfiguration(DockerHostConfiguration host, DockerRegistryAuthentication builderAuthentication,
			DockerRegistryAuthentication publishAuthentication, boolean bindHostToBuilder) {
		this.host = host;
		this.builderAuthentication = builderAuthentication;
		this.publishAuthentication = publishAuthentication;
		this.bindHostToBuilder = bindHostToBuilder;
	}

	public DockerHostConfiguration getHost() {
		return this.host;
	}

	public boolean isBindHostToBuilder() {
		return this.bindHostToBuilder;
	}

	public DockerRegistryAuthentication getBuilderRegistryAuthentication() {
		return this.builderAuthentication;
	}

	public DockerRegistryAuthentication getPublishRegistryAuthentication() {
		return this.publishAuthentication;
	}

	public DockerConfiguration withHost(String address, boolean secure, String certificatePath) {
		Assert.notNull(address, "'address' must not be null");
		return new DockerConfiguration(DockerHostConfiguration.forAddress(address, secure, certificatePath),
				this.builderAuthentication, this.publishAuthentication, this.bindHostToBuilder);
	}

	public DockerConfiguration withContext(String context) {
		Assert.notNull(context, "'context' must not be null");
		return new DockerConfiguration(DockerHostConfiguration.forContext(context), this.builderAuthentication,
				this.publishAuthentication, this.bindHostToBuilder);
	}

	public DockerConfiguration withBindHostToBuilder(boolean bindHostToBuilder) {
		return new DockerConfiguration(this.host, this.builderAuthentication, this.publishAuthentication,
				bindHostToBuilder);
	}

	public DockerConfiguration withBuilderRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "'token' must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryTokenAuthentication(token),
				this.publishAuthentication, this.bindHostToBuilder);
	}

	public DockerConfiguration withBuilderRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");
		return new DockerConfiguration(this.host, new DockerRegistryUserAuthentication(username, password, url, email),
				this.publishAuthentication, this.bindHostToBuilder);
	}

	public DockerConfiguration withPublishRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "'token' must not be null");
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryTokenAuthentication(token), this.bindHostToBuilder);
	}

	public DockerConfiguration withPublishRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "'username' must not be null");
		Assert.notNull(password, "'password' must not be null");
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryUserAuthentication(username, password, url, email), this.bindHostToBuilder);
	}

	public DockerConfiguration withEmptyPublishRegistryAuthentication() {
		return new DockerConfiguration(this.host, this.builderAuthentication,
				new DockerRegistryUserAuthentication("", "", "", ""), this.bindHostToBuilder);
	}

	/**
	 * Docker host configuration.
	 *
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
	 * {@link DockerHostConfiguration}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	public static class DockerHostConfiguration {

		private final String address;

		private final String context;

		private final boolean secure;

		private final String certificatePath;

		public DockerHostConfiguration(String address, String context, boolean secure, String certificatePath) {
			this.address = address;
			this.context = context;
			this.secure = secure;
			this.certificatePath = certificatePath;
		}

		public String getAddress() {
			return this.address;
		}

		public String getContext() {
			return this.context;
		}

		public boolean isSecure() {
			return this.secure;
		}

		public String getCertificatePath() {
			return this.certificatePath;
		}

		public static DockerHostConfiguration forAddress(String address) {
			return new DockerHostConfiguration(address, null, false, null);
		}

		public static DockerHostConfiguration forAddress(String address, boolean secure, String certificatePath) {
			return new DockerHostConfiguration(address, null, secure, certificatePath);
		}

		static DockerHostConfiguration forContext(String context) {
			return new DockerHostConfiguration(null, context, false, null);
		}

		/**
		 * Adapts a {@link DockerHostConfiguration} to a
		 * {@link DockerConnectionConfiguration}.
		 * @param configuration the configuration to adapt
		 * @return the adapted configuration
		 * @since 3.5.0
		 */
		public static DockerConnectionConfiguration asConnectionConfiguration(DockerHostConfiguration configuration) {
			if (configuration != null && StringUtils.hasLength(configuration.context)) {
				return new DockerConnectionConfiguration.Context(configuration.context);
			}
			if (configuration != null && StringUtils.hasLength(configuration.address)) {
				return new DockerConnectionConfiguration.Host(configuration.address, configuration.secure,
						configuration.certificatePath);
			}
			return null;
		}

	}

}
