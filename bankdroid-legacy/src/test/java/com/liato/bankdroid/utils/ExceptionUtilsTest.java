package com.liato.bankdroid.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;

import eu.nullbyte.android.urllib.Urllib;
import not.bankdroid.at.all.ExceptionFactory;

public class ExceptionUtilsTest {
    @Test
    public void testBlameBankdroid() {
        Exception e = ExceptionFactory.getException();
        ExceptionUtils.blameBankdroid(e);
        String after = toStringWithStacktrace(e);

        String[] afterLines = after.split("\n");
        int lastCausedByIndex = 0;
        for (int i = 0; i < afterLines.length; i++) {
            if (afterLines[i].startsWith("Caused by: ")) {
                lastCausedByIndex = i;
            }
        }
        Assert.assertNotEquals(after, 0, lastCausedByIndex);
        Assert.assertTrue(after,
                afterLines[lastCausedByIndex + 1].startsWith("\tat com.liato.bankdroid."));
        Assert.assertTrue(after,
                afterLines[lastCausedByIndex - 1].contains("--- END OF ACTUAL EXCEPTION STACK ---"));
    }

    /**
     * Like {@link #testBlameBankdroid()} but with an Exception with a cause.
     */
    @Test
    public void testBlameBankdroidWithCause() {
        Exception e = ExceptionFactory.getExceptionWithCause();
        ExceptionUtils.blameBankdroid(e);
        String after = toStringWithStacktrace(e);

        String[] afterLines = after.split("\n");
        int firstCausedByIndex = 0;
        for (int i = 0; i < afterLines.length; i++) {
            if (afterLines[i].startsWith("Caused by: ")) {
                firstCausedByIndex = i;
                break;
            }
        }
        Assert.assertNotEquals(after, 0, firstCausedByIndex);
        Assert.assertFalse(after,
                afterLines[firstCausedByIndex - 1].contains("--- END OF ACTUAL EXCEPTION STACK ---"));
        Assert.assertTrue(after,
                afterLines[firstCausedByIndex + 1].startsWith("\tat not.bankdroid.at.all."));

        int lastCausedByIndex = 0;
        for (int i = 0; i < afterLines.length; i++) {
            if (afterLines[i].startsWith("Caused by: ")) {
                lastCausedByIndex = i;
            }
        }
        Assert.assertNotEquals(after, 0, lastCausedByIndex);
        Assert.assertTrue(after,
                afterLines[lastCausedByIndex - 1].contains("--- END OF ACTUAL EXCEPTION STACK ---"));
        Assert.assertTrue(after,
                afterLines[lastCausedByIndex + 1].startsWith("\tat com.liato.bankdroid."));
    }

    @Test
    public void testBlameBankdroidAlreadyToBlame() {
        // Creating it here we're already inside of Bankdroid code, blaming bankdroid should be a
        // no-op
        Exception e = new Exception();

        String before = toStringWithStacktrace(e);

        ExceptionUtils.blameBankdroid(e);
        String after = toStringWithStacktrace(e);

        Assert.assertEquals(before, after);
    }

    private String toStringWithStacktrace(Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.close();
        return stringWriter.toString();
    }

    @Test
    public void testBankdroidifyStacktrace() {
        StackTraceElement[] bankdroidified = new StackTraceElement[] {
                new StackTraceElement("not.bankdroid.SomeClass", "someMethod", "SomeClass.java", 42),
                new StackTraceElement("com.liato.bankdroid.SomeOtherClass", "someOtherMethod", "SomeOtherClass.java", 43),
        };
        bankdroidified = ExceptionUtils.bankdroidifyStacktrace(bankdroidified);

        StackTraceElement[] expected = new StackTraceElement[] {
                new StackTraceElement("com.liato.bankdroid.SomeOtherClass", "someOtherMethod", "SomeOtherClass.java", 43),
        };

        Assert.assertArrayEquals(expected, bankdroidified);
        Assert.assertArrayEquals(expected, ExceptionUtils.bankdroidifyStacktrace(bankdroidified));
    }

    @Test
    public void testCloneExceptionWonky() {
        ExceptionFactory.WonkyException raw = ExceptionFactory.getWonkyException();

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        ConnectException cloned = ExceptionUtils.cloneException(raw);

        assert cloned != null;
        Assert.assertEquals(raw.getMessage(), cloned.getMessage());
        Assert.assertArrayEquals(raw.getStackTrace(), cloned.getStackTrace());
        Assert.assertEquals(
                "Cloning an uninstantiable Exception should return an instance of its super class",
                raw.getClass().getSuperclass(), cloned.getClass());
    }

    @Test
    @SuppressWarnings({"PMD.AvoidCatchingNPE"})
    public void testCloneExceptionNPE() {
        NullPointerException raw = null;
        try {
            //noinspection ConstantConditions
            new Urllib(null);
            Assert.fail("Exception expected");
        } catch (NullPointerException e) {
            raw = e;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        NullPointerException cloned = ExceptionUtils.cloneException(raw);

        assert cloned != null;
        Assert.assertEquals(raw.getMessage(), cloned.getMessage());
        Assert.assertArrayEquals(raw.getStackTrace(), cloned.getStackTrace());
        Assert.assertEquals(raw.getClass(), cloned.getClass());
    }
}
