
package com.badlogic.gdx;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

import org.junit.Test;

/** Tests the best-effort failure classification used by {@link Preferences#save(PreferencesSaveCallback)}. */
public class PreferencesSaveResultTest {

	@Test
	public void nullThrowableIsUnknown () {
		assertEquals(PreferencesSaveResult.UNKNOWN, PreferencesSaveResult.from(null));
	}

	@Test
	public void plainExceptionIsUnknown () {
		assertEquals(PreferencesSaveResult.UNKNOWN, PreferencesSaveResult.from(new RuntimeException("boom")));
	}

	@Test
	public void securityExceptionIsAccessDenied () {
		assertEquals(PreferencesSaveResult.ACCESS_DENIED, PreferencesSaveResult.from(new SecurityException("denied")));
	}

	@Test
	public void nioAccessDeniedExceptionIsAccessDenied () {
		assertEquals(PreferencesSaveResult.ACCESS_DENIED, PreferencesSaveResult.from(new AccessDeniedException("/prefs/x.xml")));
	}

	/** {@link FileNotFoundException} is thrown for many non-access reasons (missing parent directory, ...), so without explicit
	 * permission evidence it must degrade to IO_ERROR. */
	@Test
	public void bareFileNotFoundIsNotAccessDenied () {
		assertEquals(PreferencesSaveResult.IO_ERROR,
			PreferencesSaveResult.from(new FileNotFoundException("/prefs/missing/parent/prefs.xml")));
	}

	@Test
	public void fileNotFoundWithPermissionMessageIsAccessDenied () {
		assertEquals(PreferencesSaveResult.ACCESS_DENIED,
			PreferencesSaveResult.from(new FileNotFoundException("/prefs/p.xml (Permission denied)")));
	}

	@Test
	public void windowsAccessDeniedMessageIsDetected () {
		assertEquals(PreferencesSaveResult.ACCESS_DENIED,
			PreferencesSaveResult.from(new IOException("/prefs/p.xml (Access is denied)")));
	}

	@Test
	public void posixDiskFullMessageIsDiskFull () {
		assertEquals(PreferencesSaveResult.DISK_FULL,
			PreferencesSaveResult.from(new IOException("/prefs/p.xml (No space left on device)")));
	}

	@Test
	public void windowsDiskFullMessageIsDiskFull () {
		assertEquals(PreferencesSaveResult.DISK_FULL,
			PreferencesSaveResult.from(new IOException("There is not enough space on the disk")));
	}

	@Test
	public void quotaExceededErrorIsDiskFull () {
		// GWT surfaces localStorage quota errors as JavaScriptException-like messages carrying the JS error name.
		assertEquals(PreferencesSaveResult.DISK_FULL,
			PreferencesSaveResult.from(new RuntimeException("(QuotaExceededError): The quota has been exceeded")));
	}

	/** Backends wrap I/O exceptions (e.g. FileHandle.write wraps in GdxRuntimeException), so classification must walk the cause
	 * chain and prefer the most specific finding over a generic wrapper level. */
	@Test
	public void causeChainIsWalkedAndMostSpecificWins () {
		RuntimeException wrapped = new RuntimeException("Error writing preferences", new AccessDeniedException("/prefs/p.xml"));
		assertEquals(PreferencesSaveResult.ACCESS_DENIED, PreferencesSaveResult.from(wrapped));

		RuntimeException wrappedDiskFull = new RuntimeException("Error writing preferences",
			new IOException("/prefs/p.xml (No space left on device)"));
		assertEquals(PreferencesSaveResult.DISK_FULL, PreferencesSaveResult.from(wrappedDiskFull));

		RuntimeException wrappedIo = new RuntimeException("Error writing preferences", new IOException("kaboom"));
		assertEquals(PreferencesSaveResult.IO_ERROR, PreferencesSaveResult.from(wrappedIo));
	}

	@Test
	public void selfCausingExceptionTerminates () {
		// Throwable.initCause(this) is rejected by the JDK, so the cycle is simulated through getCause().
		assertEquals(PreferencesSaveResult.UNKNOWN, PreferencesSaveResult.from(new CyclicThrowable()));
	}

	static class CyclicThrowable extends RuntimeException {
		@Override
		public Throwable getCause () {
			return this;
		}
	}
}
