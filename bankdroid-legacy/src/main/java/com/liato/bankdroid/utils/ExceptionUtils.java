package com.liato.bankdroid.utils;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import timber.log.Timber;

public class ExceptionUtils {
    private static final String PREFIX = "com.liato.bankdroid.";

    /**
     * Make it look as if Bankdroid was the ultimate cause of this exception.
     *
     * @param t This exception will be modified to look as if Bankdroid caused it
     */
    public static void blameBankdroid(Throwable t) {
        Throwable ultimateCause = getUltimateCause(t);
        if (ultimateCause == null) {
            Timber.w(new RuntimeException(t),
                    "Unable to find ultimate cause for %s", t.getClass());
            return;
        }

        StackTraceElement[] bankdroidifiedStacktrace =
                bankdroidifyStacktrace(ultimateCause.getStackTrace());
        if (bankdroidifiedStacktrace.length == ultimateCause.getStackTrace().length) {
            // Unable to bankdroidify stacktrace => already done
            return;
        }

        Throwable fakeCause = cloneException(t);
        if (fakeCause == null) {
            Timber.w("Unable to clone Exception of type %s", t.getClass());
            return;
        }
        fakeCause.setStackTrace(bankdroidifiedStacktrace);

        ultimateCause.initCause(fakeCause);
    }

    @VisibleForTesting
    static Throwable getUltimateCause(Throwable t) {
        Throwable ultimateCause = t;
        while (ultimateCause.getCause() != null) {
            ultimateCause = ultimateCause.getCause();
        }
        return ultimateCause;
    }

    /**
     * Clone message and stacktrace but not the cause.
     */
    @Nullable
    @VisibleForTesting
    static <T extends Throwable> T cloneException(T wrap_me) {
        Class<?> newClass = wrap_me.getClass();
        while (newClass != null) {
            try {
                T returnMe =
                        (T) newClass.getConstructor(String.class).newInstance(wrap_me.getMessage());
                returnMe.setStackTrace(wrap_me.getStackTrace());
                return returnMe;
            } catch (InvocationTargetException e) {
                newClass = newClass.getSuperclass();
            } catch (NoSuchMethodException e) {
                newClass = newClass.getSuperclass();
            } catch (InstantiationException e) {
                newClass = newClass.getSuperclass();
            } catch (IllegalAccessException e) {
                newClass = newClass.getSuperclass();
            }
        }

        return null;
    }

    /**
     * Remove all initial non-Bankdroid frames from a stack.
     *
     * @return A copy of rawStack but with the initial non-Bankdroid frames removed
     */
    @VisibleForTesting
    static StackTraceElement[] bankdroidifyStacktrace(final StackTraceElement[] rawStack) {
        for (int i = 0; i < rawStack.length; i++) {
            StackTraceElement stackTraceElement = rawStack[i];
            if (stackTraceElement.getClassName().startsWith(PREFIX)) {
                return Arrays.copyOfRange(rawStack, i, rawStack.length);
            }
        }

        // No Bankdroid stack frames found, never mind
        return rawStack;
    }
}
