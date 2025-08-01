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

package org.springframework.boot.context.properties.bind;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * {@link DataObjectBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Lasse Wulff
 */
class JavaBeanBinder implements DataObjectBinder {

	private static final String HAS_KNOWN_BINDABLE_PROPERTIES_CACHE = JavaBeanBinder.class.getName()
			+ ".HAS_KNOWN_BINDABLE_PROPERTIES_CACHE";

	static final JavaBeanBinder INSTANCE = new JavaBeanBinder();

	@Override
	public <T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder) {
		boolean hasKnownBindableProperties = target.getValue() != null && hasKnownBindableProperties(name, context);
		Bean<T> bean = Bean.get(target, context, hasKnownBindableProperties);
		if (bean == null) {
			return null;
		}
		BeanSupplier<T> beanSupplier = bean.getSupplier(target);
		boolean bound = bind(propertyBinder, bean, beanSupplier, context);
		return (bound ? beanSupplier.get() : null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> @Nullable T create(Bindable<T> target, Context context) {
		Class<T> type = (Class<T>) target.getType().resolve();
		return (type != null) ? BeanUtils.instantiateClass(type) : null;
	}

	private boolean hasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
		Map<ConfigurationPropertyName, Boolean> cache = getHasKnownBindablePropertiesCache(context);
		Boolean hasKnownBindableProperties = cache.get(name);
		if (hasKnownBindableProperties == null) {
			hasKnownBindableProperties = computeHasKnownBindableProperties(name, context);
			cache.put(name, hasKnownBindableProperties);
		}
		return hasKnownBindableProperties;
	}

	private boolean computeHasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
		for (ConfigurationPropertySource source : context.getSources()) {
			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private Map<ConfigurationPropertyName, Boolean> getHasKnownBindablePropertiesCache(Context context) {
		Object cache = context.getCache().get(HAS_KNOWN_BINDABLE_PROPERTIES_CACHE);
		if (cache == null) {
			cache = new ConcurrentHashMap<ConfigurationPropertyName, Boolean>();
			context.getCache().put(HAS_KNOWN_BINDABLE_PROPERTIES_CACHE, cache);
		}
		return (Map<ConfigurationPropertyName, Boolean>) cache;
	}

	private <T> boolean bind(DataObjectPropertyBinder propertyBinder, Bean<T> bean, BeanSupplier<T> beanSupplier,
			Context context) {
		boolean bound = false;
		for (BeanProperty beanProperty : bean.getProperties().values()) {
			bound |= bind(beanSupplier, propertyBinder, beanProperty);
			context.clearConfigurationProperty();
		}
		return bound;
	}

	private <T> boolean bind(BeanSupplier<T> beanSupplier, DataObjectPropertyBinder propertyBinder,
			BeanProperty property) {
		String propertyName = determinePropertyName(property);
		ResolvableType type = property.getType();
		Supplier<Object> value = property.getValue(beanSupplier);
		Annotation[] annotations = property.getAnnotations();
		Object bound = propertyBinder.bindProperty(propertyName,
				Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations));
		if (bound == null) {
			return false;
		}
		if (property.isSettable()) {
			property.setValue(beanSupplier, bound);
		}
		else if (value == null || !bound.equals(value.get())) {
			throw new IllegalStateException("No setter found for property: " + property.getName());
		}
		return true;
	}

	private String determinePropertyName(BeanProperty property) {
		return Arrays.stream((property.getAnnotations() != null) ? property.getAnnotations() : new Annotation[0])
			.filter((annotation) -> annotation.annotationType() == Name.class)
			.findFirst()
			.map(Name.class::cast)
			.map(Name::value)
			.orElse(property.getName());
	}

	/**
	 * The properties of a bean that may be bound.
	 */
	static class BeanProperties {

		private final Map<String, BeanProperty> properties = new LinkedHashMap<>();

		private final ResolvableType type;

		private final Class<?> resolvedType;

		BeanProperties(ResolvableType type, Class<?> resolvedType) {
			this.type = type;
			this.resolvedType = resolvedType;
			addProperties(resolvedType);
		}

		private void addProperties(Class<?> type) {
			while (type != null && !Object.class.equals(type)) {
				Method[] declaredMethods = getSorted(type, this::getDeclaredMethods, Method::getName);
				Field[] declaredFields = getSorted(type, Class::getDeclaredFields, Field::getName);
				addProperties(declaredMethods, declaredFields);
				type = type.getSuperclass();
			}
		}

		private Method[] getDeclaredMethods(Class<?> type) {
			Method[] methods = type.getDeclaredMethods();
			Set<Method> result = new LinkedHashSet<>(methods.length);
			for (Method method : methods) {
				result.add(BridgeMethodResolver.findBridgedMethod(method));
			}
			return result.toArray(new Method[0]);
		}

		private <S, E> E[] getSorted(S source, Function<S, E[]> elements, Function<E, String> name) {
			E[] result = elements.apply(source);
			Arrays.sort(result, Comparator.comparing(name));
			return result;
		}

		protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
			@Nullable Method[] methods = new Method[declaredMethods.length];
			for (int i = 0; i < declaredMethods.length; i++) {
				methods[i] = isCandidate(declaredMethods[i]) ? declaredMethods[i] : null;
			}
			for (Method method : methods) {
				addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
			}
			for (Method method : methods) {
				addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
			}
			for (Method method : methods) {
				addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
			}
			for (Field field : declaredFields) {
				addField(field);
			}
		}

		private boolean isCandidate(Method method) {
			int modifiers = method.getModifiers();
			return !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isAbstract(modifiers)
					&& !Modifier.isStatic(modifiers) && !method.isBridge()
					&& !Object.class.equals(method.getDeclaringClass())
					&& !Class.class.equals(method.getDeclaringClass()) && method.getName().indexOf('$') == -1;
		}

		private void addMethodIfPossible(@Nullable Method method, String prefix, int parameterCount,
				BiConsumer<BeanProperty, Method> consumer) {
			if (method != null && method.getParameterCount() == parameterCount && method.getName().startsWith(prefix)
					&& method.getName().length() > prefix.length()) {
				String propertyName = Introspector.decapitalize(method.getName().substring(prefix.length()));
				consumer.accept(this.properties.computeIfAbsent(propertyName, this::getBeanProperty), method);
			}
		}

		private BeanProperty getBeanProperty(String name) {
			return new BeanProperty(name, this.type);
		}

		private void addField(Field field) {
			BeanProperty property = this.properties.get(field.getName());
			if (property != null) {
				property.addField(field);
			}
		}

		protected final ResolvableType getType() {
			return this.type;
		}

		protected final Class<?> getResolvedType() {
			return this.resolvedType;
		}

		final Map<String, BeanProperty> getProperties() {
			return this.properties;
		}

		static BeanProperties of(Bindable<?> bindable) {
			ResolvableType type = bindable.getType();
			Class<?> resolvedType = type.resolve(Object.class);
			return new BeanProperties(type, resolvedType);
		}

	}

	/**
	 * The bean being bound.
	 *
	 * @param <T> the bean type
	 */
	static class Bean<T> extends BeanProperties {

		Bean(ResolvableType type, Class<?> resolvedType) {
			super(type, resolvedType);
		}

		@SuppressWarnings("unchecked")
		BeanSupplier<T> getSupplier(Bindable<T> target) {
			return new BeanSupplier<>(() -> {
				T instance = null;
				if (target.getValue() != null) {
					instance = target.getValue().get();
				}
				if (instance == null) {
					instance = (T) BeanUtils.instantiateClass(getResolvedType());
				}
				return instance;
			});
		}

		@SuppressWarnings("unchecked")
		static <T> @Nullable Bean<T> get(Bindable<T> bindable, Context context, boolean canCallGetValue) {
			ResolvableType type = bindable.getType();
			Class<?> resolvedType = type.resolve(Object.class);
			Supplier<T> value = bindable.getValue();
			T instance = null;
			if (canCallGetValue && value != null) {
				instance = value.get();
				resolvedType = (instance != null) ? instance.getClass() : resolvedType;
			}
			if (instance == null && !isInstantiable(resolvedType)) {
				return null;
			}
			Map<CacheKey, Bean<?>> cache = getCache(context);
			CacheKey cacheKey = new CacheKey(type, resolvedType);
			Bean<?> bean = cache.get(cacheKey);
			if (bean == null) {
				bean = new Bean<>(type, resolvedType);
				cache.put(cacheKey, bean);
			}
			return (Bean<T>) bean;
		}

		@SuppressWarnings("unchecked")
		private static Map<CacheKey, Bean<?>> getCache(Context context) {
			Map<CacheKey, Bean<?>> cache = (Map<CacheKey, Bean<?>>) context.getCache().get(Bean.class);
			if (cache == null) {
				cache = new ConcurrentHashMap<>();
				context.getCache().put(Bean.class, cache);
			}
			return cache;
		}

		private static boolean isInstantiable(Class<?> type) {
			if (type.isInterface()) {
				return false;
			}
			try {
				type.getDeclaredConstructor();
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		private record CacheKey(ResolvableType type, Class<?> resolvedType) {

		}

	}

	private static class BeanSupplier<T> implements Supplier<T> {

		private final Supplier<T> factory;

		private @Nullable T instance;

		BeanSupplier(Supplier<T> factory) {
			this.factory = factory;
		}

		@Override
		public T get() {
			if (this.instance == null) {
				this.instance = this.factory.get();
			}
			return this.instance;
		}

	}

	/**
	 * A bean property being bound.
	 */
	static class BeanProperty {

		private final String name;

		private final ResolvableType declaringClassType;

		private @Nullable Method getter;

		private @Nullable Method setter;

		private @Nullable Field field;

		BeanProperty(String name, ResolvableType declaringClassType) {
			this.name = DataObjectPropertyName.toDashedForm(name);
			this.declaringClassType = declaringClassType;
		}

		void addGetter(Method getter) {
			if (this.getter == null || this.getter.getName().startsWith("is")) {
				this.getter = getter;
			}
		}

		void addSetter(Method setter) {
			if (this.setter == null || isBetterSetter(setter)) {
				this.setter = setter;
			}
		}

		private boolean isBetterSetter(Method setter) {
			return this.getter != null && this.getter.getReturnType().equals(setter.getParameterTypes()[0]);
		}

		void addField(Field field) {
			if (this.field == null) {
				this.field = field;
			}
		}

		String getName() {
			return this.name;
		}

		ResolvableType getType() {
			if (this.setter != null) {
				MethodParameter methodParameter = new MethodParameter(this.setter, 0);
				return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
			}
			Assert.state(this.getter != null, "'getter' must not be null");
			MethodParameter methodParameter = new MethodParameter(this.getter, -1);
			return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
		}

		Annotation @Nullable [] getAnnotations() {
			try {
				return (this.field != null) ? this.field.getDeclaredAnnotations() : null;
			}
			catch (Exception ex) {
				return null;
			}
		}

		@Nullable Supplier<Object> getValue(Supplier<?> instance) {
			if (this.getter == null) {
				return null;
			}
			return () -> {
				Assert.state(this.getter != null, "'getter' must not be null");
				try {
					this.getter.setAccessible(true);
					return this.getter.invoke(instance.get());
				}
				catch (Exception ex) {
					if (isUninitializedKotlinProperty(ex)) {
						return null;
					}
					throw new IllegalStateException("Unable to get value for property " + this.name, ex);
				}
			};
		}

		private boolean isUninitializedKotlinProperty(Exception ex) {
			return (ex instanceof InvocationTargetException invocationTargetException)
					&& "kotlin.UninitializedPropertyAccessException"
						.equals(invocationTargetException.getTargetException().getClass().getName());
		}

		boolean isSettable() {
			return this.setter != null;
		}

		void setValue(Supplier<?> instance, Object value) {
			Assert.state(this.setter != null, "'setter' must not be null");
			try {
				this.setter.setAccessible(true);
				this.setter.invoke(instance.get(), value);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to set value for property " + this.name, ex);
			}
		}

		@Nullable Method getGetter() {
			return this.getter;
		}

		@Nullable Method getSetter() {
			return this.setter;
		}

		@Nullable Field getField() {
			return this.field;
		}

	}

}
