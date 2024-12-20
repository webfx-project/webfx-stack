/*******************************************************************************
 * Copyright 2015 Alexander Casall, Manuel Mauky
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package dev.webfx.stack.ui.validation.mvvmfx;

import javafx.beans.value.ObservableStringValue;

import java.util.Objects;

/**
 * This class represents a single validation message for an error or a warning. It consists of a string message and a
 * {@link Severity}.
 *
 * @author manuel.mauky
 */
public class ValidationMessage {
	
	private final ObservableStringValue messageProperty;
	
	private final Severity severity;
	
	public ValidationMessage(Severity severity, ObservableStringValue messageProperty) {
		this.severity = Objects.requireNonNull(severity);
		this.messageProperty = Objects.requireNonNull(messageProperty);
	}
	
	
	public static ValidationMessage warning(ObservableStringValue messageProperty) {
		return new ValidationMessage(Severity.WARNING, messageProperty);
	}
	
	public static ValidationMessage error(ObservableStringValue messageProperty) {
		return new ValidationMessage(Severity.ERROR, messageProperty);
	}
	
	public ObservableStringValue messageProperty() {
		return messageProperty;
	}

	public String getMessage() {
		return messageProperty.get();
	}
	
	public Severity getSeverity() {
		return severity;
	}
	
	@Override
	public String toString() {
		return "ValidationMessage{" +
			   "messageProperty='" + messageProperty + '\'' +
			   ", severity=" + severity +
			   '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ValidationMessage))
			return false;

		ValidationMessage that = (ValidationMessage) o;

		return messageProperty.equals(that.messageProperty) && severity == that.severity;

	}

	@Override
	public int hashCode() {
		int result = messageProperty.hashCode();
		result = 31 * result + severity.hashCode();
		return result;
	}
}
