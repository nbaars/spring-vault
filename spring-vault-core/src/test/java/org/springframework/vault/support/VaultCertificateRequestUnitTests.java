/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.vault.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link VaultCertificateRequest}.
 *
 * @author Mark Paluch
 */
class VaultCertificateRequestUnitTests {

	@Test
	void shouldRejectUnconfiguredBuilder() {
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> VaultCertificateRequest.builder().build());
	}

	@Test
	void shouldBuildRequestWithCommonName() {

		VaultCertificateRequest request = VaultCertificateRequest.builder().commonName("hello.com").build();

		assertThat(request.getCommonName()).isEqualTo("hello.com");
	}

	@Test
	void shouldBuildFullyConfiguredRequest() {

		VaultCertificateRequest request = VaultCertificateRequest.builder() //
			.commonName("hello.com") //
			.withAltName("alt") //
			.withIpSubjectAltName("127.0.0.1") //
			.withUriSubjectAltName("hello.world") //
			.withOtherSans("email;UTF-8:me@example.com") //
			.excludeCommonNameFromSubjectAltNames() //
			.format("pem") //
			.privateKeyFormat("der") //
			.build();

		assertThat(request.getCommonName()).isEqualTo("hello.com");
		assertThat(request.getAltNames()).hasSize(1).contains("alt");
		assertThat(request.getIpSubjectAltNames()).containsOnly("127.0.0.1");
		assertThat(request.getUriSubjectAltNames()).containsOnly("hello.world");
		assertThat(request.getOtherSans()).containsOnly("email;UTF-8:me@example.com");
		assertThat(request.isExcludeCommonNameFromSubjectAltNames()).isTrue();
		assertThat(request.getFormat()).isEqualTo("pem");
		assertThat(request.getPrivateKeyFormat()).isEqualTo("der");
	}

}
