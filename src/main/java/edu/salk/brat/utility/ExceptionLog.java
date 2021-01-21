package edu.salk.brat.utility;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by christian.goeschl on 10/19/16.
 */
public class ExceptionLog {
    public static String StackTraceToString(final Throwable throwable){
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
