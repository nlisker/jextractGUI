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
package org.nlisker.jextractGUI;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/// Used with [DisplayNameGeneration] to make unit tests human-readable.
///
/// @see https://docs.junit.org/current/user-guide/#writing-tests-display-names
public class ReplaceCamelCase extends DisplayNameGenerator.ReplaceUnderscores {

	@Override
	public String generateDisplayNameForClass(Class<?> testClass) {
		String name = addColons(super.generateDisplayNameForClass(testClass));
		name = addSpaces(name);
		return name.replaceAll("Test", "should");
	}

	@Override
	public String generateDisplayNameForNestedClass(List<Class<?>> enclosingInstanceTypes, Class<?> nestedClass) {
		String name = addColons(super.generateDisplayNameForNestedClass(enclosingInstanceTypes, nestedClass));
		name = addSpaces(name);
		return name.toLowerCase();
	}

	@Override
	public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
		String name = addColons(testMethod.getName());
		name = addSpaces(name);
		return name.toLowerCase();
	}

	/// Adds spaces before capital letters and numbers.
	private static String addSpaces(String name) {
		return name.replaceAll("([A-Z])", " $1").replaceAll("([0-9]+)", " $1");
	}

	/// Adds colons instead of '__'.
	private static String addColons(String name) {
		return name.replaceAll("__", ": ");
	}
}