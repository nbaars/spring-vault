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

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.vault.VaultException;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

/**
 * AWS IAM authentication using signed HTTP requests to query the current identity.
 * <p>
 * AWS IAM authentication creates a {@link Aws4Signer signed} HTTP request that is
 * executed by Vault to get the identity of the signer using AWS STS
 * {@literal GetCallerIdentity}. A signature requires
 * {@link software.amazon.awssdk.auth.credentials.AwsCredentials} to calculate the
 * signature.
 * <p>
 * This authentication requires AWS' Java SDK to sign request parameters and calculate the
 * signature key. Using an appropriate
 * {@link software.amazon.awssdk.auth.credentials.AwsCredentialsProvider} allows
 * authentication within AWS-EC2 instances with an assigned profile, within ECS and Lambda
 * instances.
 *
 * @author Mark Paluch
 * @since 1.1
 * @see AwsIamAuthenticationOptions
 * @see software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
 * @see RestOperations
 * @see <a href="https://www.vaultproject.io/docs/auth/aws.html">Auth Backend: aws
 * (IAM)</a>
 * @see <a href=
 * "https://docs.aws.amazon.com/STS/latest/APIReference/API_GetCallerIdentity.html">AWS:
 * GetCallerIdentity</a>
 */
public class AwsIamAuthentication implements ClientAuthentication, AuthenticationStepsFactory {

	private static final Log logger = LogFactory.getLog(AwsIamAuthentication.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String REQUEST_BODY = "Action=GetCallerIdentity&Version=2011-06-15";

	private static final String REQUEST_BODY_BASE64_ENCODED = Base64Utils.encodeToString(REQUEST_BODY.getBytes());

	private final AwsIamAuthenticationOptions options;

	private final RestOperations vaultRestOperations;

	/**
	 * Create a new {@link AwsIamAuthentication} specifying
	 * {@link AwsIamAuthenticationOptions}, a Vault and an AWS-Metadata-specific
	 * {@link RestOperations}.
	 * @param options must not be {@literal null}.
	 * @param vaultRestOperations must not be {@literal null}.
	 */
	public AwsIamAuthentication(AwsIamAuthenticationOptions options, RestOperations vaultRestOperations) {

		Assert.notNull(options, "AwsIamAuthenticationOptions must not be null");
		Assert.notNull(vaultRestOperations, "Vault RestOperations must not be null");

		this.options = options;
		this.vaultRestOperations = vaultRestOperations;
	}

	/**
	 * Creates a {@link AuthenticationSteps} for AWS-IAM authentication given
	 * {@link AwsIamAuthenticationOptions}. The resulting {@link AuthenticationSteps}
	 * reuse eagerly-fetched {@link AwsCredentials} to prevent blocking I/O during
	 * authentication.
	 * @param options must not be {@literal null}.
	 * @return {@link AuthenticationSteps} for AWS-IAM authentication.
	 * @since 2.2
	 */
	public static AuthenticationSteps createAuthenticationSteps(AwsIamAuthenticationOptions options) {

		Assert.notNull(options, "AwsIamAuthenticationOptions must not be null");

		AwsCredentials credentials = options.getCredentialsProvider().resolveCredentials();
		Region region = options.getRegionProvider().getRegion();

		return createAuthenticationSteps(options, credentials, region);
	}

	protected static AuthenticationSteps createAuthenticationSteps(AwsIamAuthenticationOptions options,
			AwsCredentials credentials, Region region) {

		return AuthenticationSteps.fromSupplier(() -> createRequestBody(options, credentials, region)) //
			.login(AuthenticationUtil.getLoginPath(options.getPath()));
	}

	@Override
	public VaultToken login() throws VaultException {
		return createTokenUsingAwsIam();
	}

	@Override
	public AuthenticationSteps getAuthenticationSteps() {
		return createAuthenticationSteps(this.options, this.options.getCredentialsProvider().resolveCredentials(),
				this.options.getRegionProvider().getRegion());
	}

	@SuppressWarnings("unchecked")
	private VaultToken createTokenUsingAwsIam() {

		Map<String, String> login = createRequestBody(this.options);

		try {

			VaultResponse response = this.vaultRestOperations
				.postForObject(AuthenticationUtil.getLoginPath(this.options.getPath()), login, VaultResponse.class);

			Assert.state(response != null && response.getAuth() != null, "Auth field must not be null");

			if (logger.isDebugEnabled()) {

				if (response.getAuth().get("metadata") instanceof Map) {
					Map<Object, Object> metadata = (Map<Object, Object>) response.getAuth().get("metadata");
					logger.debug(String.format("Login successful using AWS-IAM authentication for user id %s, ARN %s",
							metadata.get("client_user_id"), metadata.get("canonical_arn")));
				}
				else {
					logger.debug("Login successful using AWS-IAM authentication");
				}
			}

			return LoginTokenUtil.from(response.getAuth());
		}
		catch (RestClientException e) {
			throw VaultLoginException.create("AWS-IAM", e);
		}
	}

	/**
	 * Create the request body to perform a Vault login using the AWS-IAM authentication
	 * method.
	 * @param options must not be {@literal null}.
	 * @return the map containing body key-value pairs.
	 */
	protected static Map<String, String> createRequestBody(AwsIamAuthenticationOptions options) {
		return createRequestBody(options, options.getCredentialsProvider().resolveCredentials(),
				options.getRegionProvider().getRegion());
	}

	/**
	 * Create the request body to perform a Vault login using the AWS-IAM authentication
	 * method.
	 * @param options must not be {@literal null}.
	 * @param credentials must not be {@literal null}.
	 * @param region must not be {@literal null}.
	 * @return the map containing body key-value pairs.
	 */
	private static Map<String, String> createRequestBody(AwsIamAuthenticationOptions options,
			AwsCredentials credentials, Region region) {

		Map<String, String> login = new HashMap<>();

		login.put("iam_http_request_method", "POST");
		login.put("iam_request_url", Base64Utils.encodeToString(options.getEndpointUri().toString().getBytes()));
		login.put("iam_request_body", REQUEST_BODY_BASE64_ENCODED);

		String headerJson = getSignedHeaders(options, credentials, region);

		login.put("iam_request_headers", Base64Utils.encodeToString(headerJson.getBytes()));

		if (!ObjectUtils.isEmpty(options.getRole())) {
			login.put("role", options.getRole());
		}
		return login;
	}

	private static String getSignedHeaders(AwsIamAuthenticationOptions options, AwsCredentials credentials,
			Region region) {

		Map<String, List<String>> headers = createIamRequestHeaders(options);

		SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
			.contentStreamProvider(() -> new ByteArrayInputStream(REQUEST_BODY.getBytes()))
			.headers(headers)
			.method(SdkHttpMethod.POST)
			.uri(options.getEndpointUri());
		SdkHttpFullRequest request = builder.build();

		Aws4Signer signer = Aws4Signer.create();
		Aws4SignerParams signerParams = Aws4SignerParams.builder()
			.awsCredentials(credentials)
			.signingName("sts")
			.signingRegion(region)
			.build();
		SdkHttpFullRequest signedRequest = signer.sign(request, signerParams);

		Map<String, Object> map = new LinkedHashMap<>();

		for (Entry<String, List<String>> entry : signedRequest.headers().entrySet()) {
			map.put(entry.getKey(), entry.getValue());
		}

		try {
			return OBJECT_MAPPER.writeValueAsString(map);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException("Cannot serialize headers to JSON", e);
		}
	}

	private static Map<String, List<String>> createIamRequestHeaders(AwsIamAuthenticationOptions options) {

		Map<String, List<String>> headers = new LinkedHashMap<>();

		headers.put(HttpHeaders.CONTENT_LENGTH, Collections.singletonList("" + REQUEST_BODY.length()));
		headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED_VALUE));

		if (StringUtils.hasText(options.getServerId())) {
			headers.put("X-Vault-AWS-IAM-Server-ID", Collections.singletonList(options.getServerId()));
		}

		return headers;
	}

}
