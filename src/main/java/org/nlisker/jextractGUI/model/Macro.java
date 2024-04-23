package org.nlisker.jextractGUI.model;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Macro {

	@Getter
	StringProperty name, value;

	private Macro(String name, String value) {
		this.name = new SimpleStringProperty(name);
		this.value = new SimpleStringProperty(value);
	}

	@Override
	public String toString() {
		return name.get() + "=" + value.get();
	}

	public static Optional<Macro> fromString(String string) {
		String[] tokens = string.split("=");
		if (tokens.length == 2) {
			return Optional.of(new Macro(tokens[0].strip(), tokens[1].strip()));
		}
		return Optional.empty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name.get());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Macro other)) return false;
		return Objects.equals(name.get(), other.name.get());
	}
}