package ru.flashsafe.launcher.util;

/**
 * Operating system utilities.
 * 
 * Implementation based on the following JavaFX util class: {@link http
 * ://grepcode
 * .com/file/repo1.maven.org/maven2/net.java.openjfx.backport/openjfx-
 * 78-backport/1.8.0-ea-b96.1/com/sun/javafx/PlatformUtil.java}
 * 
 * {@code OSUtils} was introduced to remove platform-dependent API from the
 * project.
 *
 */
public class OSUtils {

    private static final String operatingSystem = System.getProperty("os.name");

    private static final boolean WINDOWS = operatingSystem.startsWith("Windows");

    private static final boolean MAC = operatingSystem.startsWith("Mac");

    private static final boolean LINUX = operatingSystem.startsWith("Linux");

    private static final boolean SOLARIS = operatingSystem.startsWith("SunOS");

    private OSUtils() {
    }

    public static boolean isWindows() {
        return WINDOWS;
    }

    public static boolean isLinux() {
        return LINUX;
    }

    public static boolean isMacOS() {
        return MAC;
    }

    public static boolean isSolaris() {
        return SOLARIS;
    }

    public static boolean isUnix() {
        return LINUX || SOLARIS;
    }
}
