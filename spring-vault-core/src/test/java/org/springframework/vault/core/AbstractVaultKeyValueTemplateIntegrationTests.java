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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.domain.Person;
import org.springframework.vault.util.IntegrationTestSupport;
import org.springframework.vault.util.RequiresVaultVersion;
import org.springframework.vault.util.VaultInitializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link VaultKeyValue2Template}.
 *
 * @author Mark Paluch
 */
@RequiresVaultVersion(VaultInitializer.VERSIONING_INTRODUCED_WITH_VALUE)
abstract class AbstractVaultKeyValueTemplateIntegrationTests extends IntegrationTestSupport {

	private final String path;

	private final KeyValueBackend apiVersion;

	@Autowired
	VaultOperations vaultOperations;

	VaultKeyValueOperations kvOperations;

	AbstractVaultKeyValueTemplateIntegrationTests(String path, KeyValueBackend apiVersion) {
		this.path = path;
		this.apiVersion = apiVersion;
	}

	@BeforeEach
	void before() {
		this.kvOperations = this.vaultOperations.opsForKeyValue(this.path, this.apiVersion);
	}

	@Test
	void shouldReportExpectedApiVersion() {
		assertThat(this.kvOperations.getApiVersion()).isEqualTo(this.apiVersion);
	}

	@Test
	void shouldCreateSecret() {

		Map<String, String> secret = Collections.singletonMap("key", "value");

		String key = UUID.randomUUID().toString();

		this.kvOperations.put(key, secret);

		assertThat(this.kvOperations.list("/")).contains(key);
	}

	@Test
	void shouldReadSecret() {

		Map<String, String> secret = Collections.singletonMap("key", "value");

		String key = UUID.randomUUID().toString();

		this.kvOperations.put(key, secret);

		assertThat(this.kvOperations.get(key).getRequiredData()).containsEntry("key", "value");
	}

	@Test
	void shouldReadAbsentSecret() {

		assertThat(this.kvOperations.get("absent")).isNull();
		assertThat(this.kvOperations.get("absent", Person.class)).isNull();
	}

	@Test
	void shouldReadComplexSecret() {

		Person person = new Person();
		person.setFirstname("Walter");
		person.setLastname("Heisenberg");

		this.kvOperations.put("my-secret", person);

		assertThat(this.kvOperations.get("my-secret").getRequiredData()).containsEntry("firstname", "Walter");
		assertThat(this.kvOperations.get("my-secret", Person.class).getRequiredData()).isEqualTo(person);
	}

	@Test
	void shouldDeleteSecret() {

		Map<String, String> secret = Collections.singletonMap("key", "value");

		String key = UUID.randomUUID().toString();

		this.kvOperations.put(key, secret);
		this.kvOperations.delete(key);

		assertThat(this.kvOperations.get(key)).isNull();
	}

}
