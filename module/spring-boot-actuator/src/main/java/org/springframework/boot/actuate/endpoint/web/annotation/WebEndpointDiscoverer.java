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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationFilter;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.AdditionalPathsMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer.WebEndpointDiscovererRuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * {@link EndpointDiscoverer} for {@link ExposableWebEndpoint web endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@ImportRuntimeHints(WebEndpointDiscovererRuntimeHints.class)
public class WebEndpointDiscoverer extends EndpointDiscoverer<ExposableWebEndpoint, WebOperation>
		implements WebEndpointsSupplier {

	private final List<PathMapper> endpointPathMappers;

	private final List<AdditionalPathsMapper> additionalPathsMappers;

	private final RequestPredicateFactory requestPredicateFactory;

	/**
	 * Create a new {@link WebEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param endpointMediaTypes the endpoint media types
	 * @param endpointPathMappers the endpoint path mappers
	 * @param additionalPathsMappers the
	 * @param invokerAdvisors invoker advisors to apply
	 * @param endpointFilters endpoint filters to apply
	 * @param operationFilters operation filters to apply
	 * @since 3.4.0
	 */
	public WebEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
			EndpointMediaTypes endpointMediaTypes, @Nullable List<PathMapper> endpointPathMappers,
			@Nullable List<AdditionalPathsMapper> additionalPathsMappers,
			Collection<OperationInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<ExposableWebEndpoint>> endpointFilters,
			Collection<OperationFilter<WebOperation>> operationFilters) {
		super(applicationContext, parameterValueMapper, invokerAdvisors, endpointFilters, operationFilters);
		this.endpointPathMappers = (endpointPathMappers != null) ? endpointPathMappers : Collections.emptyList();
		this.additionalPathsMappers = (additionalPathsMappers != null) ? additionalPathsMappers
				: Collections.emptyList();
		this.requestPredicateFactory = new RequestPredicateFactory(endpointMediaTypes);
	}

	@Override
	protected ExposableWebEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<WebOperation> operations) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, id);
		return new DiscoveredWebEndpoint(this, endpointBean, id, rootPath, defaultAccess, operations,
				this.additionalPathsMappers);
	}

	@Override
	protected WebOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, endpointId);
		WebOperationRequestPredicate requestPredicate = this.requestPredicateFactory.getRequestPredicate(rootPath,
				operationMethod);
		return new DiscoveredWebOperation(endpointId, operationMethod, invoker, requestPredicate);
	}

	@Override
	protected OperationKey createOperationKey(WebOperation operation) {
		return new OperationKey(operation.getRequestPredicate(),
				() -> "web request predicate " + operation.getRequestPredicate());
	}

	static class WebEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection().registerType(WebEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
