/*
 * Copyright 2019-2024 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Builder that can be used to configure and create a {@link RestTemplate}. Provides
 * convenience methods to configure {@link #requestFactory(ClientHttpRequestFactory)
 * ClientHttpRequestFactory}, {@link #errorHandler(ResponseErrorHandler) error handlers}
 * and {@link #defaultHeader(String, String) default headers}.
 *
 * By default the built {@link RestTemplate} will attempt to use the most suitable
 * {@link ClientHttpRequestFactory} using {@link ClientHttpRequestFactoryFactory#create}.
 *
 * @author Mark Paluch
 * @since 2.2
 * @see ClientHttpRequestFactoryFactory
 * @see RestTemplateCustomizer
 */
public class RestTemplateBuilder {

	private @Nullable VaultEndpointProvider endpointProvider;

	private Supplier<ClientHttpRequestFactory> requestFactory = () -> ClientHttpRequestFactoryFactory
		.create(new ClientOptions(), SslConfiguration.unconfigured());

	private @Nullable ResponseErrorHandler errorHandler;

	private final Map<String, String> defaultHeaders = new LinkedHashMap<>();

	private final List<RestTemplateCustomizer> customizers = new ArrayList<>();

	private final Set<RestTemplateRequestCustomizer<ClientHttpRequest>> requestCustomizers = new LinkedHashSet<>();

	private RestTemplateBuilder() {
	}

	/**
	 * Create a new {@link RestTemplateBuilder}.
	 * @return a new {@link RestTemplateBuilder}.
	 */
	public static RestTemplateBuilder builder() {
		return new RestTemplateBuilder();
	}

	/**
	 * Set the {@link VaultEndpoint} that should be used with the {@link RestTemplate}.
	 * @param endpoint the {@link VaultEndpoint} provider.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder endpoint(VaultEndpoint endpoint) {
		return endpointProvider(SimpleVaultEndpointProvider.of(endpoint));
	}

	/**
	 * Set the {@link VaultEndpointProvider} that should be used with the
	 * {@link RestTemplate}.
	 * @param provider the {@link VaultEndpoint} provider.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder endpointProvider(VaultEndpointProvider provider) {

		Assert.notNull(provider, "VaultEndpointProvider must not be null");

		this.endpointProvider = provider;

		return this;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactory the request factory.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder requestFactory(ClientHttpRequestFactory requestFactory) {

		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");

		return requestFactory(() -> requestFactory);
	}

	/**
	 * Set the {@link Supplier} of {@link ClientHttpRequestFactory} that should be called
	 * each time we {@link #build()} a new {@link RestTemplate} instance.
	 * @param requestFactory the supplier for the request factory.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder requestFactory(Supplier<ClientHttpRequestFactory> requestFactory) {

		Assert.notNull(requestFactory, "Supplier of ClientHttpRequestFactory must not be null");

		this.requestFactory = requestFactory;
		return this;
	}

	/**
	 * Set the {@link ResponseErrorHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param errorHandler the error handler to use.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder errorHandler(ResponseErrorHandler errorHandler) {

		Assert.notNull(errorHandler, "ErrorHandler must not be null");

		this.errorHandler = errorHandler;
		return this;
	}

	/**
	 * Add a default header that will be set if not already present on the outgoing
	 * {@link HttpRequest}.
	 * @param name the name of the header.
	 * @param value the header value.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder defaultHeader(String name, String value) {

		Assert.hasText(name, "Header name must not be null or empty");

		this.defaultHeaders.put(name, value);

		return this;
	}

	/**
	 * Add the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
	 * applied to the {@link RestTemplate}. Customizers are applied in the order that they
	 * were added.
	 * @param customizer the template customizers to add.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	public RestTemplateBuilder customizers(RestTemplateCustomizer... customizer) {

		this.customizers.addAll(Arrays.asList(customizer));

		return this;
	}

	/**
	 * Add the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
	 * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
	 * order that they were added.
	 * @param requestCustomizers the request customizers to add.
	 * @return {@code this} {@link RestTemplateBuilder}.
	 */
	@SuppressWarnings("unchecked")
	public RestTemplateBuilder requestCustomizers(RestTemplateRequestCustomizer<?>... requestCustomizers) {

		Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");

		this.requestCustomizers.addAll((List) Arrays.asList(requestCustomizers));
		return this;
	}

	/**
	 * Build a new {@link RestTemplate}. {@link VaultEndpoint} must be set.
	 *
	 * Applies also {@link ResponseErrorHandler} and {@link RestTemplateCustomizer} if
	 * configured.
	 * @return a new {@link RestTemplate}.
	 */
	public RestTemplate build() {

		Assert.state(this.endpointProvider != null, "VaultEndpointProvider must not be null");

		RestTemplate restTemplate = createTemplate();

		if (this.errorHandler != null) {
			restTemplate.setErrorHandler(this.errorHandler);
		}

		this.customizers.forEach(customizer -> customizer.customize(restTemplate));

		return restTemplate;
	}

	/**
	 * Create the {@link RestTemplate} to use.
	 * @return the {@link RestTemplate} to use.
	 */
	protected RestTemplate createTemplate() {

		ClientHttpRequestFactory requestFactory = this.requestFactory.get();

		LinkedHashMap<String, String> defaultHeaders = new LinkedHashMap<>(this.defaultHeaders);
		LinkedHashSet<RestTemplateRequestCustomizer<ClientHttpRequest>> requestCustomizers = new LinkedHashSet<>(
				this.requestCustomizers);

		RestTemplate restTemplate = VaultClients.createRestTemplate(this.endpointProvider,
				new RestTemplateBuilderClientHttpRequestFactoryWrapper(requestFactory, requestCustomizers));

		restTemplate.getInterceptors().add((httpRequest, bytes, clientHttpRequestExecution) -> {

			HttpHeaders headers = httpRequest.getHeaders();
			defaultHeaders.forEach((key, value) -> {
				if (!headers.containsKey(key)) {
					headers.add(key, value);
				}
			});

			return clientHttpRequestExecution.execute(httpRequest, bytes);
		});

		return restTemplate;
	}

	static class RestTemplateBuilderClientHttpRequestFactoryWrapper extends AbstractClientHttpRequestFactoryWrapper {

		private final Set<RestTemplateRequestCustomizer<ClientHttpRequest>> requestCustomizers;

		RestTemplateBuilderClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory,
				Set<RestTemplateRequestCustomizer<ClientHttpRequest>> requestCustomizers) {

			super(requestFactory);
			this.requestCustomizers = requestCustomizers;
		}

		@Override
		protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod,
				ClientHttpRequestFactory requestFactory) throws IOException {

			ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);

			this.requestCustomizers.forEach(it -> it.customize(request));

			return request;
		}

	}

}
