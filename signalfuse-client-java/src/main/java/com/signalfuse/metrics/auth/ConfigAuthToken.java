package com.signalfuse.metrics.auth;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * <p/>
 * Use the default metric upload token discovered from one of the following locations (in order of
 * decreasing precedence):
 * <ol>
 * <li>The system property <tt>com.signalfuse.signalfuse.metricToken</tt></li>
 * <li>The environment variable <tt>SIGNALFUSE_METRIC_TOKEN</tt></li>
 * <li>The <tt>com.signalfuse.signalfuse.metricToken</tt> from the file <tt>~/.com.signalfuse.signalfuse/com.signalfuse.signalfuse.conf</tt></li>
 * <li>The <tt>com.signalfuse.signalfuse.metricToken</tt> from the file <tt>~/.sfsession</tt></li>
 * <li>The <tt>com.signalfuse.signalfuse.metricToken</tt> from the file <tt>/etc/com.signalfuse.signalfuse/com.signalfuse.signalfuse.conf</tt></li>
 * </ol>
 * <p/>
 * This behavior is the default; this method is provided for people who like to be explicit in their
 * code.
 * 
 * @author jack
 */
public class ConfigAuthToken implements AuthToken {
    @Override
    public String getAuthToken() {
        String token = System.getProperty("com.signalfuse.signalfuse.metricToken");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        token = System.getenv("SIGNALFUSE_METRIC_TOKEN");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        File homedir = new File(System.getProperty("user.home"));
        File[] possibleTokenLocations = {
                new File(homedir, ".com.signalfuse.signalfuse/com.signalfuse.signalfuse.conf"), new File(homedir, ".sfsession"),
                new File("/etc/com.signalfuse.signalfuse/com.signalfuse.signalfuse.conf") };
        for (File f : possibleTokenLocations) {
            try {
                if (!f.canRead()) {
                    continue;
                }
            } catch (SecurityException e) {
                // Cannot read. Continue
                continue;
            }
            try {
                token = readTokenFromFile(f);
                if (token != null && !token.isEmpty()) {
                    return token;
                }
            } catch (FileNotFoundException e) {
                // Ignore this location
            }
        }
        throw new NoAuthTokenException("Unable to find token!");
    }

    private static String readTokenFromFile(File tokenFile) throws FileNotFoundException {
        Scanner scanner = null;
        try {
            scanner = new Scanner(tokenFile);
            return scanner.useDelimiter("\\Z").next();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}
