/*
 * Copyright 2018-2024 the original author or authors.
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
package org.springframework.vault.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.vault.VaultException;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultResponseSupport;
import org.springframework.vault.support.VaultToken;
import org.springframework.vault.support.WrappedMetadata;
import org.springframework.vault.util.IntegrationTestSupport;
import org.springframework.vault.util.RequiresVaultVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link VaultWrappingTemplate} through
 * {@link VaultWrappingOperations}.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@RequiresVaultVersion(VaultWrappingTemplateIntegrationTests.WRAPPING_ENDPOINT_INTRODUCED_IN_VERSION)
@ContextConfiguration(classes = VaultIntegrationTestConfiguration.class)
class VaultWrappingTemplateIntegrationTests extends IntegrationTestSupport {

	static final String WRAPPING_ENDPOINT_INTRODUCED_IN_VERSION = "0.6.2";

	@Autowired
	VaultOperations vaultOperations;

	VaultWrappingOperations wrappingOperations;

	@BeforeEach
	void before() {

		this.wrappingOperations = this.vaultOperations.opsForWrapping();
	}

	@Test
	void shouldCreateWrappedSecret() {

		Map<String, String> map = Collections.singletonMap("key", "value");

		WrappedMetadata metadata = this.wrappingOperations.wrap(map, Duration.ofSeconds(100));

		assertThat(metadata.getTtl()).isEqualTo(Duration.ofSeconds(100));
		assertThat(metadata.getToken()).isNotNull();
		assertThat(metadata.getCreationTime()).isBefore(Instant.now().plusSeconds(60))
			.isAfter(Instant.now().minusSeconds(60));
	}

	@Test
	void shouldLookupWrappedSecret() {

		Map<String, String> map = Collections.singletonMap("key", "value");

		WrappedMetadata metadata = this.wrappingOperations.wrap(map, Duration.ofSeconds(100));

		WrappedMetadata lookup = this.wrappingOperations.lookup(metadata.getToken());

		assertThat(lookup.getTtl()).isEqualTo(Duration.ofSeconds(100));
		assertThat(lookup.getToken()).isNotNull();
		assertThat(lookup.getCreationTime()).isBefore(Instant.now().plusSeconds(60))
			.isAfter(Instant.now().minusSeconds(60));
	}

	@Test
	void shouldReadWrappedSecret() {

		Map<String, String> map = Collections.singletonMap("key", "value");

		WrappedMetadata metadata = this.wrappingOperations.wrap(map, Duration.ofSeconds(100));
		VaultResponse response = this.wrappingOperations.read(metadata.getToken());

		assertThat(response.getRequiredData()).isEqualTo(Collections.singletonMap("key", "value"));
	}

	@Test
	void shouldReadWrappedTypedSecret() {

		Map<String, String> map = Collections.singletonMap("key", "value");

		WrappedMetadata metadata = this.wrappingOperations.wrap(map, Duration.ofSeconds(100));
		VaultResponseSupport<Secret> response = this.wrappingOperations.read(metadata.getToken(), Secret.class);

		assertThat(response.getRequiredData()).isEqualTo(new Secret("value"));
	}

	@Test
	void shouldReturnNullForNonExistentSecret() {

		assertThat(this.wrappingOperations.read(VaultToken.of("foo"))).isNull();
		assertThat(this.wrappingOperations.read(VaultToken.of("foo"), Map.class)).isNull();
	}

	@Test
	void shouldLookupAbsentSecret() {

		WrappedMetadata lookup = this.wrappingOperations.lookup(VaultToken.of("foo"));

		assertThat(lookup).isNull();
	}

	@Test
	void shouldRewrapSecret() {

		Map<String, String> map = Collections.singletonMap("key", "value");

		WrappedMetadata metadata = this.wrappingOperations.wrap(map, Duration.ofSeconds(100));

		WrappedMetadata rewrap = this.wrappingOperations.rewrap(metadata.getToken());

		assertThat(rewrap.getTtl()).isEqualTo(Duration.ofSeconds(100));
		assertThat(rewrap.getToken()).isNotEqualTo(metadata.getToken());
		assertThat(rewrap.getCreationTime()).isBefore(Instant.now().plusSeconds(60))
			.isAfter(Instant.now().minusSeconds(60));
	}

	@Test
	void shouldRewrapAbsentSecret() {
		assertThatExceptionOfType(VaultException.class)
			.isThrownBy(() -> this.wrappingOperations.rewrap(VaultToken.of("foo")));
	}

	static final class Secret {

		private final String key;

		Secret(@JsonProperty("key") String key) {
			this.key = key;
		}

		public String getKey() {
			return this.key;
		}

		public String toString() {
			return "VaultWrappingTemplateIntegrationTests.Secret(key=" + this.getKey() + ")";
		}

		public boolean equals(final Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Secret))
				return false;
			final Secret other = (Secret) o;
			final Object this$key = this.getKey();
			final Object other$key = other.getKey();
			if (this$key == null ? other$key != null : !this$key.equals(other$key))
				return false;
			return true;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $key = this.getKey();
			result = result * PRIME + ($key == null ? 43 : $key.hashCode());
			return result;
		}

	}

}
