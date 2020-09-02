package com.intumit.license;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;


public class InitialReport {
    private static boolean hasError;
    private static List<InitialError> errors = new ArrayList<InitialError>();

    public static boolean hasError() {
        return hasError;
    }

    public static void addError(InitialError error) {
        hasError = true;
        errors.add(error);
    }

    public static String getErrorMessage() {
        return StringUtils.join(errors, "\n");
    }
}
