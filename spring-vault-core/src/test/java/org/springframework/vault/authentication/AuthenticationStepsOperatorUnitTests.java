/*
 * Copyright 2017-2024 the original author or authors.
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
package org.springframework.vault.authentication;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.AuthenticationSteps.Node;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.vault.authentication.AuthenticationSteps.HttpRequestBuilder.post;

/**
 * Unit tests for {@link AuthenticationStepsOperator}.
 *
 * @author Mark Paluch
 */
class AuthenticationStepsOperatorUnitTests {

	@Test
	void justTokenShouldLogin() {

		AuthenticationSteps steps = AuthenticationSteps.just(VaultToken.of("my-token"));

		login(steps).as(StepVerifier::create) //
			.expectNext(VaultToken.of("my-token")) //
			.verifyComplete();
	}

	@Test
	void supplierOfStringShouldLoginWithMap() {

		AuthenticationSteps steps = AuthenticationSteps.fromSupplier(() -> "my-token").login(VaultToken::of);

		login(steps).as(StepVerifier::create) //
			.expectNext(VaultToken.of("my-token")) //
			.verifyComplete();
	}

	@Test
	void fileResourceCredentialSupplierShouldBeLoaded() {

		AuthenticationSteps steps = AuthenticationSteps
			.fromSupplier(new ResourceCredentialSupplier(new ClassPathResource("kube-jwt-token")))
			.login(VaultToken::of);

		login(steps).as(StepVerifier::create) //
			.consumeNextWith(actual -> {
				assertThat(actual.getToken()).startsWith("eyJhbGciOiJSUz");
			})
			.verifyComplete();
	}

	@Test
	void absentFileResourceCredentialSupplierShouldFail() {

		AuthenticationSteps steps = AuthenticationSteps
			.fromSupplier(new ResourceCredentialSupplier(new ByteArrayResource("eyJhbGciOiJSUz".getBytes()) {
				@Override
				public InputStream getInputStream() throws IOException {
					throw new IOException("Oops!");
				}
			}))
			.login(VaultToken::of);

		login(steps).as(StepVerifier::create) //
			.verifyError(VaultException.class);
	}

	@Test
	void inputStreamResourceCredentialSupplierShouldBeLoaded() {

		AuthenticationSteps steps = AuthenticationSteps
			.fromSupplier(new ResourceCredentialSupplier(new ByteArrayResource("eyJhbGciOiJSUz".getBytes())))
			.login(VaultToken::of);

		login(steps).as(StepVerifier::create) //
			.consumeNextWith(actual -> {
				assertThat(actual.getToken()).startsWith("eyJhbGciOiJSUz");
			})
			.verifyComplete();
	}

	@Test
	void anyCredentialSupplierShouldBeLoaded() {

		AuthenticationSteps steps = AuthenticationSteps.fromSupplier(() -> "eyJhbGciOiJSUz").login(VaultToken::of);

		login(steps).as(StepVerifier::create) //
			.consumeNextWith(actual -> {
				assertThat(actual.getToken()).startsWith("eyJhbGciOiJSUz");
			})
			.verifyComplete();
	}

	@Test
	void justLoginRequestShouldLogin() {

		ClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/auth/cert/login");
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		response.setBody(
				"{" + "\"auth\":{\"client_token\":\"my-token\", \"renewable\": true, \"lease_duration\": 10}" + "}");
		ClientHttpConnector connector = (method, uri, fn) -> fn.apply(request).then(Mono.just(response));

		WebClient webClient = WebClient.builder().clientConnector(connector).build();

		AuthenticationSteps steps = AuthenticationSteps
			.just(post("/auth/{path}/login", "cert").as(VaultResponse.class));

		login(steps, webClient).as(StepVerifier::create) //
			.expectNext(VaultToken.of("my-token")) //
			.verifyComplete();
	}

	@Test
	void justLoginShouldFail() {

		ClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/auth/cert/login");
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.BAD_REQUEST);
		ClientHttpConnector connector = (method, uri, fn) -> fn.apply(request).then(Mono.just(response));

		WebClient webClient = WebClient.builder().clientConnector(connector).build();

		AuthenticationSteps steps = AuthenticationSteps
			.just(post("/auth/{path}/login", "cert").as(VaultResponse.class));

		login(steps, webClient).as(StepVerifier::create) //
			.expectError() //
			.verify();
	}

	@Test
	void zipWithShouldRequestTwoItems() {

		ClientHttpRequest leftRequest = new MockClientHttpRequest(HttpMethod.GET, "/auth/login/left");
		MockClientHttpResponse leftResponse = new MockClientHttpResponse(HttpStatus.OK);
		leftResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		leftResponse.setBody("{" + "\"request_id\": \"left\"}");

		ClientHttpRequest rightRequest = new MockClientHttpRequest(HttpMethod.GET, "/auth/login/right");
		MockClientHttpResponse rightResponse = new MockClientHttpResponse(HttpStatus.OK);
		rightResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		rightResponse.setBody("{" + "\"request_id\": \"right\"}");

		ClientHttpConnector connector = (method, uri, fn) -> {

			if (uri.toString().contains("left")) {
				return fn.apply(leftRequest).then(Mono.just(leftResponse));
			}

			return fn.apply(rightRequest).then(Mono.just(rightResponse));
		};

		WebClient webClient = WebClient.builder().clientConnector(connector).build();

		Node<VaultResponse> left = AuthenticationSteps
			.fromHttpRequest(post("/auth/login/left").as(VaultResponse.class));

		Node<VaultResponse> right = AuthenticationSteps
			.fromHttpRequest(post("/auth/login/right").as(VaultResponse.class));

		AuthenticationSteps steps = left.zipWith(right)
			.login(it -> VaultToken.of(it.getLeft().getRequestId() + "-" + it.getRight().getRequestId()));

		login(steps, webClient).as(StepVerifier::create) //
			.expectNext(VaultToken.of("left-right")) //
			.verifyComplete();
	}

	private Mono<VaultToken> login(AuthenticationSteps steps) {

		AuthenticationStepsOperator operator = new AuthenticationStepsOperator(steps, WebClient.create());
		return operator.getVaultToken();
	}

	private Mono<VaultToken> login(AuthenticationSteps steps, WebClient webClient) {

		AuthenticationStepsOperator operator = new AuthenticationStepsOperator(steps, webClient);
		return operator.getVaultToken();
	}

}
