/*
 * Copyright 2020-2022 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility to parse a Go format duration into {@link Duration}.
 *
 * @author Mark Paluch
 * @since 2.3
 * @see <a href="https://golang.org/pkg/time/#ParseDuration">Go ParseDuration</a>
 */
public class DurationParser {

	private static final Pattern PARSE_PATTERN = Pattern.compile("([0-9]+)(ns|us|ms|s|m|h|d)");

	private static final Pattern VERIFY_PATTERN = Pattern.compile("(([0-9]+)(ns|us|ms|s|m|h|d))+");

	/**
	 * Parse a Go format duration into a {@link Duration} object.
	 * @param duration the duration string to parse in Go's duration format.
	 * @return the duration object. Can be {@literal null} if {@code duration} is empty.
	 * @throws IllegalArgumentException if unable to parse the requested duration.
	 */
	@Nullable
	public static Duration parseDuration(String duration) {

		if (ObjectUtils.isEmpty(duration)) {
			return null;
		}

		if ("0".equals(duration)) {
			return Duration.ZERO;
		}

		if (!VERIFY_PATTERN.matcher(duration.toLowerCase(Locale.ENGLISH)).matches()) {
			throw new IllegalArgumentException(String.format("Cannot parse '%s' into a Duration", duration));
		}

		Matcher matcher = PARSE_PATTERN.matcher(duration.toLowerCase(Locale.ENGLISH));
		Duration result = Duration.ZERO;
		while (matcher.find()) {

			int num = Integer.parseInt(matcher.group(1));
			String typ = matcher.group(2);

			switch (typ) {
				case "ns":
					result = result.plus(Duration.ofNanos(num));
					break;
				case "us":
					result = result.plus(Duration.ofNanos(num * 1000));
					break;
				case "ms":
					result = result.plus(Duration.ofMillis(num));
					break;
				case "s":
					result = result.plus(Duration.ofSeconds(num));
					break;
				case "m":
					result = result.plus(Duration.ofMinutes(num));
					break;
				case "h":
					result = result.plus(Duration.ofHours(num));
					break;
				case "d":
					result = result.plus(Duration.ofDays(num));
					break;
				case "w":
					result = result.plus(Duration.ofDays(num * 7));
					break;
			}
		}

		return result;
	}

	/**
	 * Format a {@link Duration} into the Go format representation.
	 * @param duration the duration object to format.
	 * @return the duration formatted in Go's duration format.
	 */
	public static String formatDuration(Duration duration) {

		StringBuilder builder = new StringBuilder();

		for (TemporalUnit unit : duration.getUnits()) {

			if (unit == ChronoUnit.MINUTES) {
				builder.append(duration.get(unit)).append('m');
			}
			if (unit == ChronoUnit.HOURS) {
				builder.append(duration.get(unit)).append('h');
			}
			if (unit == ChronoUnit.SECONDS) {
				builder.append(duration.get(unit)).append('s');
			}
			if (unit == ChronoUnit.MILLIS) {
				builder.append(duration.get(unit)).append("ms");
			}
			if (unit == ChronoUnit.NANOS) {
				builder.append(duration.get(unit)).append("ns");
			}
		}

		return builder.toString();
	}

}
