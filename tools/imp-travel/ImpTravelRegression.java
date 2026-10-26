import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretLaboratoryRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretLibraryRoom;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.json.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Runs the actual unmodified engine first, then compares the variant against its fixtures. */
public class ImpTravelRegression {
    private static final long[] SEEDS = {0,1,2,3,4,5,37,99,1234,1234567,987654321,31415926535L};

    public static void main(String[] args) throws Exception {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        cfg.updatesPerSecond = -1;
        HeadlessApplication app = new HeadlessApplication(new ApplicationAdapter(){}, cfg);
        try {
            FileUtils.setDefaultFileProperties(Files.FileType.Absolute,
                    java.nio.file.Files.createTempDirectory("imp-regression-").toString()+"/");
            Game.version = "4.0.2";
            Game.versionCode = 922;
            GamesInProgress.curSlot = 1;
            // Tutorial placement deliberately uses an unseeded RNG in upstream.
            // Use the same completed journal in both builds to compare seeded content.
            SPDSettings.intro(false);
            Badges.loadGlobal();
            for(Document document:Document.values()) for(String page:document.pageNames()) {
                document.findPage(page);
            }
            Document.ADVENTURERS_GUIDE.readPage(Document.GUIDE_INTRO);
            if (args[0].equals("diagnose")) {
                init(0, 0);
                Dungeon.level=Dungeon.newLevel();
                dump(Dungeon.level);
                Dungeon.saveLevel(1);
                Dungeon.level=Dungeon.loadLevel(1);
                System.out.println("AFTER LOAD");
                dump(Dungeon.level);
                return;
            }
            if (args[0].equals("start")) {
                init(0, 0);
                Level first=Dungeon.newLevel();Dungeon.level=first;
                require(countStrength()==0, "strength grant must wait until prefix generation finishes");
                Class.forName("com.shatteredpixel.shatteredpixeldungeon.ImpTravel")
                        .getMethod("prepare", Level.class).invoke(null, first);
                int count = countUpgrades();
                require(count == 15, "new game must carry 15 upgrade scrolls; got "+count);
                require(countStrength()==10, "new game must carry 10 strength potions; got "+countStrength());
                for(Item item:Dungeon.hero.belongings.backpack.items)
                    if(item instanceof PotionOfStrength) require(item.isIdentified(), "starting strength must be identified");
                Dungeon.saveGame(1);
                Dungeon.loadGame(1);
                require(countStrength()==10 && countUpgrades()==15, "starting grants must survive save/reload without duplication");
                int last=Collections.max(Dungeon.generatedLevels);
                for(int depth=last+1;depth<=24;depth++) {
                    Dungeon.depth=depth;
                    Dungeon.level=Dungeon.newLevel();
                    requireNoNaturalStartingItems(Dungeon.level);
                }
                System.out.println("PASS starting inventory");
                return;
            }
            if (args[0].equals("legacy-strength")) {
                init(0, 0);
                Item beacon=null;
                for(Item item:Dungeon.hero.belongings.backpack.items)
                    if(item.getClass().getSimpleName().equals("ImpBeacon")) beacon=item;
                require(beacon!=null, "legacy beacon present");
                Bundle legacy=new Bundle();
                beacon.storeInBundle(legacy);
                legacy.remove("strength_relocated");
                // Older APKs stored no strength-relocation flag.
                beacon.restoreFromBundle(legacy);
                int natural=0;
                for(int depth=1;depth<=24;depth++) {
                    Dungeon.depth=depth;
                    Dungeon.level=Dungeon.newLevel();
                    for(Heap heap:Dungeon.level.heaps.valueList()) for(Item item:heap.items)
                        if(item instanceof PotionOfStrength) natural+=item.quantity();
                }
                require(natural==10, "legacy saves must retain ten natural strength potions; got "+natural);
                require(countStrength()==0, "legacy saves must not receive another starting grant");
                System.out.println("PASS legacy saves retain natural strength potions");
                return;
            }
            if (args[0].equals("visits")) {
                init(0, 0);
                Level first=Dungeon.newLevel();Dungeon.level=first;
                Class.forName("com.shatteredpixel.shatteredpixeldungeon.ImpTravel")
                        .getMethod("prepare", Level.class).invoke(null, first);
                Dungeon.switchLevel(first, -1);
                Dungeon.depth=2;
                Level second=Dungeon.loadLevel(1);
                Dungeon.switchLevel(second, -1);
                require(Statistics.deepestFloor==2, "visiting saved floor 2 must update progression");
                require(Statistics.qualifiedForNoKilling, "regular floor must qualify for no-killing");
                System.out.println("PASS real visits to saved floors");
                return;
            }
            if (args[0].equals("no-free-badge")) {
                init(0, 0);
                Level first=Dungeon.newLevel();Dungeon.level=first;
                require(!Statistics.completedWithNoKilling, "baseline starts without badge qualification");
                Class.forName("com.shatteredpixel.shatteredpixeldungeon.ImpTravel")
                        .getMethod("prepare", Level.class).invoke(null, first);
                require(!Statistics.completedWithNoKilling, "pre-generation must not award a no-killing badge");
                System.out.println("PASS no free badge from generating floors");
                return;
            }
            if (args[0].equals("landing")) {
                init(0, 0);
                Level first=Dungeon.newLevel();Dungeon.level=first;
                Class<?> travel=Class.forName("com.shatteredpixel.shatteredpixeldungeon.ImpTravel");
                travel.getMethod("prepare", Level.class).invoke(null, first);
                Item beacon=null;
                for(Item item:Dungeon.hero.belongings.backpack.items)
                    if(item.getClass().getSimpleName().equals("ImpBeacon")) beacon=item;
                require(beacon!=null, "travel item present");
                Bundle data=new Bundle();beacon.storeInBundle(data);
                Dungeon.depth=data.getInt("imp_depth");
                Level target=Dungeon.loadLevel(1);
                int blocked=data.getInt("imp_cell");
                com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward ward=
                        new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding.Ward();
                ward.pos=blocked;target.mobs.add(ward);
                java.lang.reflect.Method resolve;
                try { resolve=travel.getMethod("resolveArrival", Level.class, int.class); }
                catch(NoSuchMethodException e) { throw new AssertionError("travel must revalidate an occupied destination"); }
                int safe=(Integer)resolve.invoke(null,target,-3);
                require(safe!=blocked && target.findMob(safe)==null && target.passable[safe],
                        "return must not overlap an immovable ward");
                Bundle stored=new Bundle();stored.put("beacon",beacon);
                Item restored=(Item)stored.get("beacon");
                Bundle again=new Bundle();restored.storeInBundle(again);
                require(again.getInt("imp_depth")==data.getInt("imp_depth")
                        && again.getInt("imp_cell")==blocked, "travel destination survives item save/load");
                System.out.println("PASS safe landing and travel-item round trip");
                return;
            }
            Path fixturePath = Paths.get(args[1]);
            JSONObject fixtures = args[0].equals("reference") ? new JSONObject()
                    : new JSONObject(java.nio.file.Files.readString(fixturePath));
            int checked = 0;
            for (int challenge : new int[]{0, Challenges.NO_SCROLLS}) {
                for (int index=0; index<SEEDS.length; index++) {
                    long seed = SEEDS[index];
                    init(seed, challenge);
                    String run = seed+":"+challenge;
                    if (args[0].equals("reference")) {
                        JSONObject levels = new JSONObject();
                        for (int depth=1; depth<=26; depth++) {
                            Dungeon.depth = depth;
                            Dungeon.level = Dungeon.newLevel();
                            levels.put(""+depth, snapshot(Dungeon.level));
                            if(depth==24) require(Dungeon.LimitedDrops.STRENGTH_POTIONS.count==10,
                                    "original main floors 1-24 must schedule ten strength potions");
                        }
                        fixtures.put(run, levels);
                    } else {
                        Level first = Dungeon.newLevel();
                        Dungeon.level = first;
                        Class.forName("com.shatteredpixel.shatteredpixeldungeon.ImpTravel")
                                .getMethod("prepare", Level.class).invoke(null, first);
                        require(countUpgrades()==15, "15 starting upgrades for "+run);
                        require(countStrength()==10, "10 starting strength potions for "+run);
                        require(Dungeon.depth==1 && Dungeon.branch==0 && Dungeon.level==first,
                                "preparation must restore floor 1");
                        require(Statistics.deepestFloor==1, "generation must not count as visiting floors");
                        int last = Collections.max(Dungeon.generatedLevels);
                        require(last>=17 && last<=19, "real Imp must be on 17..19");
                        String generatorAtImp = generator();
                        boolean found = false;
                        for (int depth=1; depth<=last; depth++) {
                            Dungeon.depth=depth;
                            Level level = Dungeon.loadLevel(1);
                            JSONObject want=fixtures.getJSONObject(run).getJSONObject(""+depth);
                            JSONObject got=snapshot(level);
                            for (String key : new String[]{"map","loot","mobs"}) {
                                require(want.getString(key).equals(got.getString(key)),
                                        run+" floor "+depth+" "+key+" differs from upstream"
                                        +(key.equals("loot") ? "\nEXPECTED "+want.optString("lootDetail")+"\nGOT "+got.optString("lootDetail") : ""));
                            }
                            requireNoNaturalStartingItems(level);
                            if(depth==last) {
                                require(want.getString("rewards").equals(got.getString("rewards")),
                                        run+" Imp rewards differ from upstream");
                                require(want.getString("generator").equals(generatorAtImp),
                                        run+" generator deck state differs at Imp");
                                require(want.getInt("strengthDrops")==Dungeon.LimitedDrops.STRENGTH_POTIONS.count,
                                        run+" strength-drop counter differs at Imp");
                                for(Mob mob:level.mobs) if(mob instanceof Imp) found=true;
                            }
                            checked++;
                        }
                        require(found, "destination floor must contain actual Imp");
                        // Continue normal generation after the saved prefix, exercising future floors too.
                        for(int depth=last+1; depth<=26; depth++) {
                            Dungeon.depth=depth;
                            Dungeon.level=Dungeon.newLevel();
                            requireNoNaturalStartingItems(Dungeon.level);
                            JSONObject want=fixtures.getJSONObject(run).getJSONObject(""+depth);
                            JSONObject got=snapshot(Dungeon.level);
                            require(want.getInt("strengthDrops")==Dungeon.LimitedDrops.STRENGTH_POTIONS.count,
                                    run+" later floor "+depth+" strength-drop counter differs");
                            for(String key:new String[]{"map","loot","mobs","rewards","generator"})
                                require(want.getString(key).equals(got.getString(key)),
                                        run+" later floor "+depth+" "+key+" differs");
                            checked++;
                        }
                    }
                }
            }
            if(args[0].equals("reference")) {
                java.nio.file.Files.writeString(fixturePath, fixtures.toString(), StandardCharsets.UTF_8);
                System.out.println("PASS upstream fixtures: 24 runs, 624 floors");
            } else System.out.println("PASS original generation retained: "+checked+" floors, 24 runs");
        } finally { app.exit(); }
    }

    private static void init(long seed, int challenge) {
        SPDSettings.challenges(challenge);
        GamesInProgress.selectedClass=HeroClass.values()[(int)(seed%6)];
        Dungeon.seed=seed;
        Dungeon.customSeedText="regression"; //disables remains, like a user-entered seed
        Dungeon.init();
    }

    private static int countUpgrades() {
        int result=0;
        for(Item item:Dungeon.hero.belongings.backpack.items)
            if(item instanceof ScrollOfUpgrade) result+=item.quantity();
        return result;
    }

    private static int countStrength() {
        int result=0;
        for(Item item:Dungeon.hero.belongings.backpack.items)
            if(item instanceof PotionOfStrength) result+=item.quantity();
        return result;
    }

    private static void requireNoNaturalStartingItems(Level level) {
        for(Heap heap:level.heaps.valueList()) for(Item item:heap.items) {
            require(!(item instanceof ScrollOfUpgrade), "natural upgrade remains");
            require(!(item instanceof PotionOfStrength), "natural strength potion remains");
        }
    }

    private static void dump(Level level) {
        ArrayList<String> out=new ArrayList<>();
        for(Heap heap:level.heaps.valueList()) for(Item item:heap.items)
            if(!(item instanceof ScrollOfUpgrade) && !(item instanceof PotionOfStrength)) out.add(heap.pos+":"+heap.type+":"+item.getClass().getSimpleName()+":"+itemData(item));
        Collections.sort(out);for(String line:out) System.out.println(line);
    }

    private static JSONObject snapshot(Level level) throws Exception {
        ArrayList<String> loot=new ArrayList<>(), mobs=new ArrayList<>(), rewards=new ArrayList<>();
        for(Heap heap:level.heaps.valueList()) {
            for(Item item:heap.items) if(!(item instanceof ScrollOfUpgrade) && !(item instanceof PotionOfStrength)) {
                // Upstream ShopRoom chooses bags by inventory savings, with HashMap identity
                // order breaking ties. Bag subtype is not seed-defined even in the original.
                String kind=item instanceof Bag ? "INVENTORY_DEPENDENT_BAG" : item.getClass().getName();
                if(level instanceof RegularLevel) {
                    Object room=((RegularLevel)level).room(heap.pos);
                    // These two original room tables use Class-keyed HashMaps, whose identity
                    // iteration order is JVM-dependent. Still compare counts, cells, and item state.
                    if(room instanceof SecretLaboratoryRoom && item instanceof Potion)
                        kind="UPSTREAM_CLASS_IDENTITY_POTION";
                    if(room instanceof SecretLibraryRoom && item instanceof Scroll)
                        kind="UPSTREAM_CLASS_IDENTITY_SCROLL";
                }
                loot.add(heap.pos+":"+heap.type+":"+kind+":"+itemData(item));
            }
        }
        for(Mob mob:level.mobs) {
            Bundle data=new Bundle(); mob.storeInBundle(data);
            // Serialization includes actor time and ID. Both should be preserved by pre-generation.
            mobs.add(mob.getClass().getName()+":"+canonical(new JSONObject(data.toString())));
        }
        for(Item item:Imp.Quest.rewardOptions) rewards.add(item.getClass().getName()+":"+itemData(item));
        Collections.sort(loot); Collections.sort(mobs);
        return new JSONObject().put("map",hash(Arrays.toString(level.map)))
                .put("strengthDrops",Dungeon.LimitedDrops.STRENGTH_POTIONS.count)
                .put("lootDetail",loot.toString())
                .put("loot",hash(loot.toString())).put("mobs",hash(mobs.toString()))
                .put("rewards",hash(rewards.toString())).put("generator",generator());
    }

    private static String itemData(Item item) {
        Bundle data=new Bundle();item.storeInBundle(data);
        return canonical(new JSONObject(data.toString()));
    }
    private static String generator() throws Exception {
        Bundle data=new Bundle();Generator.storeInBundle(data);
        return hash(canonical(new JSONObject(data.toString())));
    }
    private static String canonical(Object value) {
        if(value instanceof JSONObject) {
            JSONObject obj=(JSONObject)value;TreeSet<String> keys=new TreeSet<>(obj.keySet());
            StringBuilder out=new StringBuilder("{");
            for(String key:keys) {
                // Throwing-weapon set IDs use SecureRandom; actor IDs are runtime identities.
                if(key.equals("set_id") || key.equals("id")) continue;
                out.append(JSONObject.quote(key)).append(':').append(canonical(obj.get(key))).append(',');
            }
            return out.append('}').toString();
        }
        if(value instanceof JSONArray) {
            JSONArray arr=(JSONArray)value;StringBuilder out=new StringBuilder("[");
            for(int i=0;i<arr.length();i++) out.append(canonical(arr.get(i))).append(',');
            return out.append(']').toString();
        }
        return String.valueOf(value);
    }
    private static String hash(String value) throws Exception {
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();for(byte b:digest) out.append(String.format("%02x",b));
        return out.toString();
    }
    private static void require(boolean value,String message) {
        if(!value) throw new AssertionError(message);
    }
}
