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
package org.springframework.vault.repository.mapping;

import org.springframework.data.keyvalue.core.mapping.AnnotationBasedKeySpaceResolver;
import org.springframework.data.keyvalue.core.mapping.BasicKeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link VaultPersistentEntity} implementation.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public class BasicVaultPersistentEntity<T> extends BasicKeyValuePersistentEntity<T, VaultPersistentProperty>
		implements VaultPersistentEntity<T> {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final String backend;

	private final @Nullable Expression backendExpression;

	/**
	 * Creates new {@link BasicVaultPersistentEntity}.
	 * @param information must not be {@literal null}.
	 * @param keySpaceResolver can be {@literal null}.
	 */
	public BasicVaultPersistentEntity(TypeInformation<T> information, @Nullable KeySpaceResolver keySpaceResolver) {
		super(information, type -> {

			if (keySpaceResolver != null) {
				return keySpaceResolver.resolveKeySpace(type);
			}

			String keyspace = AnnotationBasedKeySpaceResolver.INSTANCE.resolveKeySpace(type);
			if (StringUtils.hasText(keyspace)) {

				// fallback to use keyspace resolution and SpEL expression handling of
				// BasicKeyValuePersistentEntity.
				return null;
			}

			return SimpleClassNameKeySpaceResolver.INSTANCE.resolveKeySpace(type);
		});

		Secret annotation = findAnnotation(Secret.class);

		if (annotation != null && StringUtils.hasText(annotation.backend())) {

			this.backend = annotation.backend();
			this.backendExpression = detectExpression(this.backend);

		}
		else {
			this.backend = "secret";
			this.backendExpression = null;
		}
	}

	/**
	 * Returns a SpEL {@link Expression} if the given {@link String} is actually an
	 * expression that does not evaluate to a {@link LiteralExpression} (indicating that
	 * no subsequent evaluation is necessary).
	 * @param potentialExpression can be {@literal null}
	 * @return
	 */
	@Nullable
	private static Expression detectExpression(String potentialExpression) {

		Expression expression = PARSER.parseExpression(potentialExpression, ParserContext.TEMPLATE_EXPRESSION);
		return expression instanceof LiteralExpression ? null : expression;
	}

	@Override
	public String getKeySpace() {
		return String.format("%s/%s", getSecretBackend(), super.getKeySpace());
	}

	@Override
	public String getSecretBackend() {

		return this.backendExpression == null //
				? this.backend //
				: this.backendExpression.getValue(getEvaluationContext(null), String.class);
	}

}
