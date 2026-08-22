/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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

package com.badlogic.gdx;

import java.io.IOException;
import java.util.Locale;

/** The outcome of a verified preferences persist through {@link Preferences#save(PreferencesSaveCallback)}.
 * <p>
 * Backends map their native results onto this enum on a best-effort basis because not every backend reports detailed failure
 * reasons. The documented mapping is:
 * </p>
 * <ul>
 * <li>Android: {@code SharedPreferences.Editor#commit()} only returns a boolean, so failures are reported as
 * {@link #IO_ERROR}.</li>
 * <li>LWJGL3/LWJGL/Headless: exceptions are classified via {@link #from(Throwable)}.</li>
 * <li>iOS: {@code NSDictionary#writeToFile:atomically:} only returns a boolean, so failures are reported as
 * {@link #IO_ERROR}.</li>
 * <li>GWT: Local Storage failures are classified from the thrown error; quota errors map to {@link #DISK_FULL}.</li>
 * </ul>
 * {@link #UNKNOWN} makes an undetectable failure reason explicit instead of hiding it behind a more specific value. */
public enum PreferencesSaveResult {
	/** The preferences were persisted. */
	SUCCESS,
	/** The write failed because storage ran out of space or a storage quota was exceeded. */
	DISK_FULL,
	/** The write failed because access to the target location was denied. */
	ACCESS_DENIED,
	/** The write failed with an I/O error that was not classified more specifically. */
	IO_ERROR,
	/** The backend did not report a failure reason. */
	UNKNOWN;

	// Lowercase substrings matched against exception messages. Best effort by nature: operating systems and browsers localize or
	// word these differently, unmatched messages degrade to IO_ERROR/UNKNOWN rather than misreporting.
	private static final String[] ACCESS_DENIED_MESSAGES = {"permission denied", "access denied", "access is denied"};
	private static final String[] DISK_FULL_MESSAGES = {"no space left on device", "not enough space", "disk is full",
		"quotaexceedederror", "quota exceeded"};

	/** Best-effort classification of an exception thrown while persisting. Walks the exception cause chain, because backends
	 * commonly wrap the originating I/O exception, and returns the most specific classification found.
	 *
	 * A bare {@link java.io.FileNotFoundException} is <em>not</em> reported as {@link #ACCESS_DENIED}: it is thrown for many
	 * reasons besides denied access, e.g. a missing parent directory. Only explicit permission evidence (a
	 * {@code java.nio.file.AccessDeniedException}, a {@code SecurityException}, or a permission-related message) yields
	 * {@link #ACCESS_DENIED}.
	 *
	 * @param t the exception to classify, may be null
	 * @return the most specific result found; {@link #IO_ERROR} if any {@link IOException} was found without a more specific
	 *         cause; {@link #UNKNOWN} otherwise */
	public static PreferencesSaveResult from (Throwable t) {
		boolean ioError = false;
		while (t != null && t.getCause() != t) {
			PreferencesSaveResult specific = classify(t);
			if (specific != null) return specific;
			if (t instanceof IOException) ioError = true;
			t = t.getCause();
		}
		return ioError ? IO_ERROR : UNKNOWN;
	}

	/** Classifies a single exception level, or returns null if this level alone is not decisive. */
	private static PreferencesSaveResult classify (Throwable t) {
		if (t instanceof SecurityException || isNamedAccessDeniedException(t)) return ACCESS_DENIED;
		String message = t.getMessage();
		if (message == null) return null;
		String lower = message.toLowerCase(Locale.ROOT);
		if (containsAny(lower, ACCESS_DENIED_MESSAGES)) return ACCESS_DENIED;
		if (containsAny(lower, DISK_FULL_MESSAGES)) return DISK_FULL;
		return null;
	}

	/** Detects {@code java.nio.file.AccessDeniedException} by its fully qualified name while walking the class hierarchy. The name
	 * check instead of {@code instanceof} keeps this class GWT-compatible, where {@code java.nio.file} is not emulated. */
	private static boolean isNamedAccessDeniedException (Throwable t) {
		for (Class<?> c = t.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			if ("java.nio.file.AccessDeniedException".equals(c.getName())) return true;
		}
		return false;
	}

	private static boolean containsAny (String message, String[] candidates) {
		for (String candidate : candidates) {
			if (message.contains(candidate)) return true;
		}
		return false;
	}
}
