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
            if (Boolean.getBoolean("imp.experienceControlPair")) {
                java.util.Map<java.lang.reflect.Field,Object[]> vaultArrays=new java.util.LinkedHashMap<>();
                for(String name:new String[]{"T2SolveItems","T3SolveItems"}) {
                    java.lang.reflect.Field field=com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel.class.getDeclaredField(name);
                    field.setAccessible(true);
                    vaultArrays.put(field,((Object[])field.get(null)).clone());
                }
                java.io.PrintStream original=System.out;
                java.io.ByteArrayOutputStream control=new java.io.ByteArrayOutputStream();
                java.io.ByteArrayOutputStream variant=new java.io.ByteArrayOutputStream();
                try {
                    resetUnseededOracleState(vaultArrays);
                    System.setProperty("imp.disableExperience","true");
                    System.setOut(new java.io.PrintStream(control,true,"UTF-8"));
                    AnalyzerOracle.main(args);
                    resetUnseededOracleState(vaultArrays);
                    System.clearProperty("imp.disableExperience");
                    System.setOut(new java.io.PrintStream(variant,true,"UTF-8"));
                    AnalyzerOracle.main(args);
                } finally {
                    System.setOut(original);
                    System.clearProperty("imp.disableExperience");
                }
                original.println(new org.json.JSONObject()
                        .put("control",new org.json.JSONObject(control.toString("UTF-8")))
                        .put("variant",new org.json.JSONObject(variant.toString("UTF-8"))));
            } else {
                AnalyzerOracle.main(args);
            }
        } finally {
            app.exit();
        }
    }

    private static void resetUnseededOracleState(java.util.Map<java.lang.reflect.Field,Object[]> vaultArrays) throws Exception {
        // Vanilla Collections.shuffle uses its own unseeded JDK RNG. Hold that nuisance
        // choice fixed for the paired audit; no Android/production RNG is changed.
        java.lang.reflect.Field shuffle=java.util.Collections.class.getDeclaredField("r");
        shuffle.setAccessible(true);
        shuffle.set(null,new java.util.Random(0));
        for(java.util.Map.Entry<java.lang.reflect.Field,Object[]> entry:vaultArrays.entrySet()) {
            Object[] target=(Object[])entry.getKey().get(null);
            System.arraycopy(entry.getValue(),0,target,0,target.length);
        }
    }
}
