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

package org.springframework.boot.data.mongodb.autoconfigure;

import java.util.Optional;

import com.mongodb.ClientSessionOptions;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails.GridFs;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive mongo
 * support.
 * <p>
 * Registers a {@link ReactiveMongoTemplate} bean if no other bean of the same type is
 * configured.
 *
 * @author Mark Paluch
 * @author Artsiom Yudovin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0.0
 */
@AutoConfiguration(after = MongoReactiveAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
@ConditionalOnBean(MongoClient.class)
@EnableConfigurationProperties(MongoProperties.class)
@Import(MongoDataConfiguration.class)
public final class MongoReactiveDataAutoConfiguration {

	private final MongoConnectionDetails connectionDetails;

	MongoReactiveDataAutoConfiguration(MongoConnectionDetails connectionDetails) {
		this.connectionDetails = connectionDetails;
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveMongoDatabaseFactory.class)
	SimpleReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient mongo, MongoProperties properties) {
		String database = properties.getDatabase();
		if (database == null) {
			database = this.connectionDetails.getConnectionString().getDatabase();
		}
		return new SimpleReactiveMongoDatabaseFactory(mongo, database);
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveMongoOperations.class)
	ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			MongoConverter converter) {
		return new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter);
	}

	@Bean
	@ConditionalOnMissingBean(DataBufferFactory.class)
	DefaultDataBufferFactory dataBufferFactory() {
		return new DefaultDataBufferFactory();
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveGridFsOperations.class)
	ReactiveGridFsTemplate reactiveGridFsTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			MappingMongoConverter mappingMongoConverter, DataBufferFactory dataBufferFactory) {
		return new ReactiveGridFsTemplate(dataBufferFactory,
				new GridFsReactiveMongoDatabaseFactory(reactiveMongoDatabaseFactory, this.connectionDetails),
				mappingMongoConverter,
				(this.connectionDetails.getGridFs() != null) ? this.connectionDetails.getGridFs().getBucket() : null);
	}

	/**
	 * {@link ReactiveMongoDatabaseFactory} decorator to use {@link GridFs#getGridFs()}
	 * from the {@link MongoConnectionDetails} when set.
	 */
	static class GridFsReactiveMongoDatabaseFactory implements ReactiveMongoDatabaseFactory {

		private final ReactiveMongoDatabaseFactory delegate;

		private final MongoConnectionDetails connectionDetails;

		GridFsReactiveMongoDatabaseFactory(ReactiveMongoDatabaseFactory delegate,
				MongoConnectionDetails connectionDetails) {
			this.delegate = delegate;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public boolean hasCodecFor(Class<?> type) {
			return this.delegate.hasCodecFor(type);
		}

		@Override
		public Mono<MongoDatabase> getMongoDatabase() throws DataAccessException {
			String gridFsDatabase = getGridFsDatabase(this.connectionDetails);
			if (StringUtils.hasText(gridFsDatabase)) {
				return this.delegate.getMongoDatabase(gridFsDatabase);
			}
			return this.delegate.getMongoDatabase();
		}

		private String getGridFsDatabase(MongoConnectionDetails connectionDetails) {
			return (connectionDetails.getGridFs() != null) ? connectionDetails.getGridFs().getDatabase() : null;
		}

		@Override
		public Mono<MongoDatabase> getMongoDatabase(String dbName) throws DataAccessException {
			return this.delegate.getMongoDatabase(dbName);
		}

		@Override
		public <T> Optional<Codec<T>> getCodecFor(Class<T> type) {
			return this.delegate.getCodecFor(type);
		}

		@Override
		public PersistenceExceptionTranslator getExceptionTranslator() {
			return this.delegate.getExceptionTranslator();
		}

		@Override
		public CodecRegistry getCodecRegistry() {
			return this.delegate.getCodecRegistry();
		}

		@Override
		public Mono<ClientSession> getSession(ClientSessionOptions options) {
			return this.delegate.getSession(options);
		}

		@Override
		public ReactiveMongoDatabaseFactory withSession(ClientSession session) {
			return this.delegate.withSession(session);
		}

		@Override
		public boolean isTransactionActive() {
			return this.delegate.isTransactionActive();
		}

	}

}
