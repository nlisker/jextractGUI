/*
 * Copyright 2026 Nir Lisker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nlisker.jextractGUI.jextract;

import java.util.List;
import java.util.regex.Pattern;

import org.nlisker.jextractGUI.model.Displayable.MainHeader;

/// The result of a jextract invocation (parsing ([Parser#populateHeaderItem]) or bindings generation ([Extractor#runCommand]) on
/// a main header. Since jextract's error reporting is not fleshed out (https://bugs.openjdk.org/browse/CODETOOLS-7904194), ad-hoc
/// handling is required. If jextract fails with an exception or logs a failure (even if a warning is also logged), its `status`
/// is [Status#ERROR]; if it logs a warning, its `status` is [Status#WARNING]; otherwise its `status` is [Status#SUCCESS].
/// `errorOutput` is populated for non-success invocations.
public record JextractResult(MainHeader header, Status status, String errorOutput) {

	public enum Status {
		SUCCESS,
		WARNING,
		ERROR
	}

	/// Creates a [Status#SUCCESS] result.
	public static JextractResult success(MainHeader header) {
		return new JextractResult(header, Status.SUCCESS, "");
	}

	/// Creates a result based on the `errorOutput`.
	public static JextractResult ofErrorOutput(MainHeader header, String errorOutput) {
		Status status = parseErrorOutput(errorOutput);
		return new JextractResult(header, status, errorOutput);
	}

	/// Creates a [Status#ERROR] result with an exception message.
	public static JextractResult ofException(MainHeader header, Throwable cause) {
		String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
		return new JextractResult(header, Status.ERROR, message);
	}

	public boolean isSuccess() {
		return status == Status.SUCCESS;
	}

	public boolean hasWarning() {
		return status == Status.WARNING;
	}

	public boolean hasError() {
		return status == Status.ERROR;
	}

	/// Currently, jextract's error stream output starts lines with `file:line:col: severity: `. [String#contains] doesn't work
	/// because variables can be named "error".
	private static final Pattern PREFIX = Pattern.compile("^.*:\\d+:\\d+: (?<severity>error|warning): ", Pattern.MULTILINE);

	/// Finds the highest severity in the error output.
	// package-private for tests
	static Status parseErrorOutput(String errorOutput) {
		List<String> severities = PREFIX.matcher(errorOutput).results()
				.map(result -> result.group("severity"))
				.toList();
		if (severities.contains("error")) {
			return Status.ERROR;
		}
		if (severities.contains("warning")) {
			return Status.WARNING;
		}
		return Status.SUCCESS;
	}
}