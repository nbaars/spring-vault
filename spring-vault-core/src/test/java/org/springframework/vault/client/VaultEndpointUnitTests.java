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
package org.springframework.vault.client;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultEndpoint}.
 *
 * @author Mark Paluch
 */
class VaultEndpointUnitTests {

	@Test
	void shouldCreateEndpointFromHostAndPort() {

		VaultEndpoint endpoint = VaultEndpoint.create("host", 1234);

		assertThat(endpoint.getScheme()).isEqualTo("https");
		assertThat(endpoint.getHost()).isEqualTo("host");
		assertThat(endpoint.getPort()).isEqualTo(1234);
	}

	@Test
	void shouldCreateEndpointFromURI() {

		VaultEndpoint endpoint = VaultEndpoint.from(URI.create("http://127.0.0.1:443/"));

		assertThat(endpoint.getScheme()).isEqualTo("http");
		assertThat(endpoint.getHost()).isEqualTo("127.0.0.1");
		assertThat(endpoint.getPort()).isEqualTo(443);
		assertThat(endpoint.getPath()).isEqualTo(VaultEndpoint.API_VERSION);
	}

	@Test
	void shouldCreateEndpointFromURIWithoutPort() {

		VaultEndpoint endpoint = VaultEndpoint.from(URI.create("http://127.0.0.1"));

		assertThat(endpoint.getScheme()).isEqualTo("http");
		assertThat(endpoint.getHost()).isEqualTo("127.0.0.1");
		assertThat(endpoint.getPort()).isEqualTo(80);
		assertThat(endpoint.getPath()).isEqualTo(VaultEndpoint.API_VERSION);
	}

	@Test
	void shouldCreateEndpointWithPath() {

		VaultEndpoint endpoint = VaultEndpoint.from(URI.create("http://127.0.0.1/context"));

		assertThat(endpoint.getPath()).isEqualTo("context");
		assertThat(endpoint.createUri("foo")).isEqualTo(URI.create("http://127.0.0.1:80/context/foo"));
	}

}
