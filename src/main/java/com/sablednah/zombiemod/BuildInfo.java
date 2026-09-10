package com.sablednah.zombiemod;

import java.io.InputStream;
import java.util.Properties;

/**
 * Which build this is.
 *
 * <p>A version number answers "which release". During development that is a different question
 * from "which bytes", and the gap between them cost three sessions time in a single day across
 * these mods: a jar rebuilt under an unchanged version number broke an already-shipped consumer,
 * a dependency range admitted a jar it could not run against, and nobody could tell from a
 * filename whether an instance jar was stale.
 *
 * <p><b>The log line is the half that matters.</b> A stamp inside the jar says what is on disk;
 * the startup line says what actually <em>ran</em>, which is the question a bug report needs
 * answered.
 *
 * <p>It answers a question this mod has in a sharper form than its siblings: ZombieMod ships
 * <b>three jars per release</b>, one per Minecraft version, and they differ only in a filename
 * suffix. The stamp names the branch, so a report from a server running the wrong line of the
 * three says so in its own log.
 *
 * <p>Read from a generated properties file rather than the manifest, because this has to work in
 * a dev run too, where the mod loads from a classes directory and there is no jar to carry a
 * manifest. The manifest carries the same values for anything inspecting a jar without loading it.
 *
 * <p>Format agreed across the SableCraft mods (LegendQuest {@code bdd63809}); copied rather than
 * shared, because five copies of sixty lines is less coupling than an artifact everyone depends
 * on for a diagnostic that must never be able to break a build.
 */
public final class BuildInfo {

    /** Namespaced: a bare {@code /build.properties} would collide with every other mod doing the
     *  same thing on a shared classpath, and reading a sibling mod's stamp is worse than having
     *  none — it would be confidently wrong. */
    private static final String RESOURCE = "/" + ZombieMod.MOD_ID + "/build.properties";

    private static final String COMMIT;
    private static final String BRANCH;
    private static final String TIME;
    private static final String VERSION;

    static {
        String commit = "unknown", branch = "unknown", time = "unknown", version = "unknown";
        try (InputStream in = BuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                commit = p.getProperty("commit", commit);
                branch = p.getProperty("branch", branch);
                time = p.getProperty("time", time);
                version = p.getProperty("version", version);
            }
        } catch (Exception ignored) {
            // catch (Exception) is deliberate, not defensive habit. Properties.load throws
            // IllegalArgumentException on a malformed unicode escape - NOT IOException - so the
            // obvious catch (IOException) would compile, read correctly, pass review, and take the mod
            // down at class-init as an ExceptionInInitializerError. Failing to load over a
            // diagnostic. Absence is a null stream and easy; corruption is a throw.
            //
            // The degrade is all-or-nothing, and that depends on the ORDER above rather than on
            // anything said here: load() completes or throws before the first getProperty, so a
            // half-parsed file cannot leave a real-looking commit beside three unknowns - which
            // would be worse than no stamp, because it looks like an answer. Keep the
            // assignments after load(), never interleaved with it.
        }
        COMMIT = commit;
        BRANCH = branch;
        TIME = time;
        VERSION = version;
    }

    public static String commit() {
        return COMMIT;
    }

    public static String branch() {
        return BRANCH;
    }

    /** The <b>commit's</b> timestamp, UTC, not when gradle was run — a wall clock here changes
     *  the generated resource on every invocation and defeats Gradle's up-to-date checks. The
     *  SHA already says which bytes these are, so this answers the one question left. */
    public static String time() {
        return TIME;
    }

    public static String version() {
        return VERSION;
    }

    /** The one-line form for the startup log: {@code 3.4.0 (build a1b2c3d4 on master,
     *  2026-09-10T07:24:24Z)}. A {@code -dirty} suffix on the commit means it was built with
     *  uncommitted changes, which is worth seeing in somebody's log before you spend an hour
     *  reproducing against a tag. */
    public static String describe() {
        return VERSION + " (build " + COMMIT + " on " + BRANCH + ", " + TIME + ")";
    }

    private BuildInfo() {}
}
