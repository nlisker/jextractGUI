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