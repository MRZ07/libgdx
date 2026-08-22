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

import java.io.FileNotFoundException;
import java.io.IOException;

/** The outcome of a verified preferences persist through {@link Preferences#save(PreferencesSaveCallback)}.
 * <p>
 * Backends map their native results onto this enum on a best-effort basis because not every backend reports detailed failure
 * reasons:
 * </p>
 * <ul>
 * <li>Android: {@code SharedPreferences.Editor#commit()} only returns a boolean, so failures are reported as
 * {@link #IO_ERROR}.</li>
 * <li>LWJGL3/LWJGL/Headless: exceptions are classified via {@link #from(Throwable)} ({@link FileNotFoundException} maps to
 * {@link #ACCESS_DENIED}).</li>
 * <li>iOS: {@code NSDictionary#writeToFile:atomically:} only returns a boolean, so failures are reported as
 * {@link #IO_ERROR}.</li>
 * <li>GWT: Local Storage quota errors are reported as {@link #IO_ERROR}.</li>
 * </ul>
 * {@link #DISK_FULL} is reserved for backends that can detect it; currently none of the shipped backends does. */
public enum PreferencesSaveResult {
	/** The preferences were persisted. */
	SUCCESS,
	/** The write failed because storage ran out of space. */
	DISK_FULL,
	/** The write failed because the target location could not be accessed. */
	ACCESS_DENIED,
	/** The write failed with an I/O error that was not classified more specifically. */
	IO_ERROR,
	/** The backend did not report a failure reason. */
	UNKNOWN;

	/** Best-effort classification of an exception thrown while persisting. Walks the exception cause chain, because backends
	 * commonly wrap the originating I/O exception.
	 * @param t the exception to classify, may be null
	 * @return {@link #ACCESS_DENIED} for access-related exceptions, {@link #IO_ERROR} for other I/O errors, {@link #UNKNOWN}
	 *         otherwise */
	public static PreferencesSaveResult from (Throwable t) {
		while (t != null && t.getCause() != t) {
			if (t instanceof FileNotFoundException || t instanceof SecurityException) return ACCESS_DENIED;
			if (t instanceof IOException) return IO_ERROR;
			t = t.getCause();
		}
		return UNKNOWN;
	}
}
