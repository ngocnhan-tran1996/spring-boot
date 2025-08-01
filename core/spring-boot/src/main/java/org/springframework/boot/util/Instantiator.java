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

package org.springframework.boot.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Simple factory used to instantiate objects by injecting available parameters.
 *
 * @param <T> the type to instantiate
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.4.0
 */
public class Instantiator<T> {

	private static final Comparator<Constructor<?>> CONSTRUCTOR_COMPARATOR = Comparator
		.<Constructor<?>>comparingInt(Constructor::getParameterCount)
		.reversed();

	private static final FailureHandler throwingFailureHandler = (type, implementationName, failure) -> {
		throw new IllegalArgumentException("Unable to instantiate " + implementationName + " [" + type.getName() + "]",
				failure);
	};

	private final Class<?> type;

	private final Map<Class<?>, Function<Class<?>, @Nullable Object>> availableParameters;

	private final FailureHandler failureHandler;

	/**
	 * Create a new {@link Instantiator} instance for the given type.
	 * @param type the type to instantiate
	 * @param availableParameters consumer used to register available parameters
	 */
	public Instantiator(Class<?> type, Consumer<AvailableParameters> availableParameters) {
		this(type, availableParameters, throwingFailureHandler);
	}

	/**
	 * Create a new {@link Instantiator} instance for the given type.
	 * @param type the type to instantiate
	 * @param availableParameters consumer used to register available parameters
	 * @param failureHandler a {@link FailureHandler} that will be called in case of
	 * failure when instantiating objects
	 * @since 2.7.0
	 */
	public Instantiator(Class<?> type, Consumer<AvailableParameters> availableParameters,
			FailureHandler failureHandler) {
		this.type = type;
		this.availableParameters = getAvailableParameters(availableParameters);
		this.failureHandler = failureHandler;
	}

	private Map<Class<?>, Function<Class<?>, @Nullable Object>> getAvailableParameters(
			Consumer<AvailableParameters> availableParameters) {
		Map<Class<?>, Function<Class<?>, Object>> result = new LinkedHashMap<>();
		availableParameters.accept(new AvailableParameters() {

			@Override
			public void add(Class<?> type, @Nullable Object instance) {
				result.put(type, (factoryType) -> instance);
			}

			@Override
			public void add(Class<?> type, @Nullable Function<Class<?>, @Nullable Object> factory) {
				result.put(type, factory);
			}

		});
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Instantiate the given set of class name, injecting constructor arguments as
	 * necessary.
	 * @param names the class names to instantiate
	 * @return a list of instantiated instances
	 */
	public List<T> instantiate(Collection<String> names) {
		return instantiate(null, names);
	}

	/**
	 * Instantiate the given set of class name, injecting constructor arguments as
	 * necessary.
	 * @param classLoader the source classloader
	 * @param names the class names to instantiate
	 * @return a list of instantiated instances
	 * @since 2.4.8
	 */
	public List<T> instantiate(@Nullable ClassLoader classLoader, Collection<String> names) {
		Assert.notNull(names, "'names' must not be null");
		return instantiate(names.stream().map((name) -> TypeSupplier.forName(classLoader, name)));
	}

	/**
	 * Instantiate the given set of class name, injecting constructor arguments as
	 * necessary.
	 * @param name the class name to instantiate
	 * @return an instantiated instance
	 * @since 3.4.0
	 */
	public @Nullable T instantiate(String name) {
		return instantiate(null, name);
	}

	/**
	 * Instantiate the given set of class name, injecting constructor arguments as
	 * necessary.
	 * @param classLoader the source classloader
	 * @param name the class name to instantiate
	 * @return an instantiated instance
	 * @since 3.4.0
	 */
	public @Nullable T instantiate(@Nullable ClassLoader classLoader, String name) {
		return instantiate(TypeSupplier.forName(classLoader, name));
	}

	/**
	 * Instantiate the given class, injecting constructor arguments as necessary.
	 * @param type the type to instantiate
	 * @return an instantiated instance
	 * @since 3.4.0
	 */
	public @Nullable T instantiateType(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		return instantiate(TypeSupplier.forType(type));
	}

	/**
	 * Instantiate the given set of classes, injecting constructor arguments as necessary.
	 * @param types the types to instantiate
	 * @return a list of instantiated instances
	 * @since 2.4.8
	 */
	public List<T> instantiateTypes(Collection<Class<?>> types) {
		Assert.notNull(types, "'types' must not be null");
		return instantiate(types.stream().map(TypeSupplier::forType));
	}

	/**
	 * Get an injectable argument instance for the given type. This method can be used
	 * when manually instantiating an object without reflection.
	 * @param <A> the argument type
	 * @param type the argument type
	 * @return the argument to inject or {@code null}
	 * @since 3.4.0
	 */
	@SuppressWarnings("unchecked")
	public <A> @Nullable A getArg(Class<A> type) {
		Assert.notNull(type, "'type' must not be null");
		Function<Class<?>, Object> parameter = getAvailableParameter(type);
		Assert.state(parameter != null, "Unknown argument type " + type.getName());
		return (A) parameter.apply(this.type);
	}

	private List<T> instantiate(Stream<TypeSupplier> typeSuppliers) {
		return typeSuppliers.map(this::instantiate).sorted(AnnotationAwareOrderComparator.INSTANCE).toList();
	}

	private @Nullable T instantiate(TypeSupplier typeSupplier) {
		try {
			Class<?> type = typeSupplier.get();
			Assert.state(this.type.isAssignableFrom(type), () -> type + " is not assignable to " + this.type);
			return instantiate(type);
		}
		catch (Throwable ex) {
			this.failureHandler.handleFailure(this.type, typeSupplier.getName(), ex);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private T instantiate(Class<?> type) throws Exception {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		Arrays.sort(constructors, CONSTRUCTOR_COMPARATOR);
		for (Constructor<?> constructor : constructors) {
			Object[] args = getArgs(constructor.getParameterTypes());
			if (args != null) {
				ReflectionUtils.makeAccessible(constructor);
				return (T) constructor.newInstance(args);
			}
		}
		throw new IllegalAccessException("Class [" + type.getName() + "] has no suitable constructor");
	}

	private Object @Nullable [] getArgs(Class<?>[] parameterTypes) {
		Object[] args = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			Function<Class<?>, Object> parameter = getAvailableParameter(parameterTypes[i]);
			if (parameter == null) {
				return null;
			}
			args[i] = parameter.apply(this.type);
		}
		return args;
	}

	private @Nullable Function<Class<?>, Object> getAvailableParameter(Class<?> parameterType) {
		for (Map.Entry<Class<?>, Function<Class<?>, Object>> entry : this.availableParameters.entrySet()) {
			if (entry.getKey().isAssignableFrom(parameterType)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Callback used to register available parameters.
	 */
	public interface AvailableParameters {

		/**
		 * Add a parameter with an instance value.
		 * @param type the parameter type
		 * @param instance the instance that should be injected
		 */
		void add(Class<?> type, @Nullable Object instance);

		/**
		 * Add a parameter with an instance factory.
		 * @param type the parameter type
		 * @param factory the factory used to create the instance that should be injected
		 */
		void add(Class<?> type, Function<Class<?>, @Nullable Object> factory);

	}

	/**
	 * {@link Supplier} that provides a class type.
	 */
	private interface TypeSupplier {

		String getName();

		Class<?> get() throws ClassNotFoundException;

		static TypeSupplier forName(@Nullable ClassLoader classLoader, String name) {
			return new TypeSupplier() {

				@Override
				public String getName() {
					return name;
				}

				@Override
				public Class<?> get() throws ClassNotFoundException {
					return ClassUtils.forName(name, classLoader);
				}

			};
		}

		static TypeSupplier forType(Class<?> type) {
			return new TypeSupplier() {

				@Override
				public String getName() {
					return type.getName();
				}

				@Override
				public Class<?> get() {
					return type;
				}

			};
		}

	}

	/**
	 * Strategy for handling a failure that occurs when instantiating a type.
	 *
	 * @since 2.7.0
	 */
	public interface FailureHandler {

		/**
		 * Handle the {@code failure} that occurred when instantiating the {@code type}
		 * that was expected to be of the given {@code typeSupplier}.
		 * @param type the type
		 * @param implementationName the name of the implementation type
		 * @param failure the failure that occurred
		 */
		void handleFailure(Class<?> type, String implementationName, Throwable failure);

	}

}
