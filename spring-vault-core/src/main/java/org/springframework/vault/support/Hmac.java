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
package org.springframework.vault.support;

import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Value object representing Hmac digest.
 *
 * @author Luander Ribeiro
 * @author Mark Paluch
 * @since 2.0
 */
public class Hmac {

	private final String hmac;

	private Hmac(String hmac) {
		this.hmac = hmac;
	}

	/**
	 * Factory method to create a {@link Hmac} from the given {@code hmac}.
	 * @param hmac the Hmac digest, must not be {@literal null} or empty.
	 * @return the {@link Hmac} encapsulating {@code hmac}.
	 */
	public static Hmac of(String hmac) {

		Assert.hasText(hmac, "Hmac digest must not be null or empty");

		return new Hmac(hmac);
	}

	public String getHmac() {
		return this.hmac;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Hmac))
			return false;
		Hmac other = (Hmac) o;
		return this.hmac.equals(other.hmac);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.hmac);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getClass().getSimpleName());
		sb.append(" [hmac='").append(this.hmac).append('\'');
		sb.append(']');
		return sb.toString();
	}

}
