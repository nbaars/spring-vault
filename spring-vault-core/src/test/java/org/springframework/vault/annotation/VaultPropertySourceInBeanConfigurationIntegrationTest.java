/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.vault.annotation;

import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.vault.core.VaultIntegrationTestConfiguration;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.util.VaultExtension;
import org.springframework.vault.util.VaultInitializer;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for {@link VaultPropertySource}.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(VaultExtension.class)
@ContextConfiguration
class VaultPropertySourceInBeanConfigurationIntegrationTest {

	@VaultPropertySource({ "secret/myapp" })
	static class Config extends VaultIntegrationTestConfiguration {

		@Bean
		ClientClass clientClass(@Value("${myapp}") String myapp) {
			return new ClientClass(myapp);
		}

	}

	@Autowired
	ClientClass clientClass;

	@BeforeAll
	static void beforeClass(VaultInitializer initializer) {

		VaultOperations vaultOperations = initializer.prepare().getVaultOperations();

		vaultOperations.write("secret/myapp", Collections.singletonMap("myapp", "myvalue"));
	}

	@Test
	void clientClassShouldContainResolvedProperty() {
		assertThat(this.clientClass.getMyapp()).isEqualTo("myvalue");
	}

	static class ClientClass {

		String myapp;

		ClientClass(String myapp) {
			this.myapp = myapp;
		}

		String getMyapp() {
			return this.myapp;
		}

		void setMyapp(String myapp) {
			this.myapp = myapp;
		}

	}

}
