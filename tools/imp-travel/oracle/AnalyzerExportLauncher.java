package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.watabou.utils.FileUtils;

/** Runs the actual APK core, including ImpTravel.prepare and saved-prefix reloads. */
public final class AnalyzerExportLauncher {
    public static void main(String[] args) throws Exception {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        cfg.updatesPerSecond = -1;
        HeadlessApplication app = new HeadlessApplication(new ApplicationAdapter(){}, cfg);
        try {
            FileUtils.setDefaultFileProperties(Files.FileType.Absolute,
                    java.nio.file.Files.createTempDirectory("imp-analyzer-").toString()+"/");
            com.watabou.noosa.Game.version = "4.0.2-INDEV";
            com.watabou.noosa.Game.versionCode = 922;
            if (args.length == 1 && args[0].equals("--check-artifact-guard")) {
                java.lang.reflect.Method record = AnalyzerOracle.class.getDeclaredMethod("itemRecord",
                        com.shatteredpixel.shatteredpixeldungeon.levels.Level.class,
                        com.shatteredpixel.shatteredpixeldungeon.items.Item.class,
                        String.class, int.class, Integer.class, String.class, String.class, Object.class, Object.class);
                record.setAccessible(true);
                com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass artifact =
                        new com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass();
                boolean rejected = false;
                try { record.invoke(null, null, artifact, "imp_quest", 0, null, null, null, null, null); }
                catch (java.lang.reflect.InvocationTargetException e) {
                    if (!(e.getCause() instanceof IllegalStateException)
                            || !e.getCause().getMessage().contains("transfer")) throw e;
                    rejected = true;
                }
                if (!rejected) throw new AssertionError("An unupgraded Imp artifact was falsely reported as +5");
                artifact.transferUpgrade(5);
                record.invoke(null, null, artifact, "imp_quest", 0, null, null, null, null, null);
                System.out.println("PASS artifact comparison rejects missing transfer upgrades");
                return;
            }
            AnalyzerOracle.main(args);
        } finally {
            app.exit();
        }
    }
}
