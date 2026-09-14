package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.Reflection;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

public class DeveloperTool extends Item {

    private static final String AC_USE = "USE";
    private static final int PAGE_SIZE = 6;

    private static final String[] EXOTIC_POTION_NAMES = new String[]{
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion$PotionToExotic",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing$Cleanse",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration$DivineInspirationTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShroudingFog",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds"
    };

    private static final String[] EXOTIC_SCROLL_NAMES = new String[]{
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll$ScrollToExotic",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge$ChallengeArena",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment$WndConfirmCancel",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment$WndEnchantSelect",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment$WndGlyphSelect",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForesight",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis$WndMetamorphChoose",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis$WndMetamorphReplace",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPrismaticImage",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong$Enthralled"
    };

    private static final String[] ALCHEMY_NAMES = new String[]{
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb$ArcaneBombFuse",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$ConjuredBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$DoubleBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$EnhanceBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$Fuse",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.FlashBangBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb$HolyDamage",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker$NoisemakerFuse",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShrapnelBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.SmokeBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.WoollyBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation$AquaHealing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall$FeatherBuff",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight$HTBoost",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize$WndAlchemizeItem",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning$BeaconTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.InventorySpell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap$ReclaimedTrap",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Spell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental$InvisAlly",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.TargetedSpell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy$Recipe"
    };

    private static final String[] SPECIAL_ITEM_NAMES = new String[]{
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose$GhostHero",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose$NoRoseDamage",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose$Petal",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband$StolenTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey$KeyReplacementTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey$KeyWall",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight$CharAwareness",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight$HeapAwareness",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass$sandBag",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook$ExploitHandler",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust$DustGhostSpawner",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust$DustWraith",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe"
    };

    private static final String[] ALL_ITEM_NAMES = new String[]{
            "com.shatteredpixel.shatteredpixeldungeon.items.Amulet",
            "com.shatteredpixel.shatteredpixeldungeon.items.Ankh",
            "com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin",
            "com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal",
            "com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal$WarriorShield",
            "com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop",
            "com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal",
            "com.shatteredpixel.shatteredpixeldungeon.items.Gold",
            "com.shatteredpixel.shatteredpixeldungeon.items.Heap",
            "com.shatteredpixel.shatteredpixeldungeon.items.Honeypot",
            "com.shatteredpixel.shatteredpixeldungeon.items.Honeypot$ShatteredPot",
            "com.shatteredpixel.shatteredpixeldungeon.items.ItemStatusHandler",
            "com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon",
            "com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown",
            "com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal",
            "com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.LostBackpack",
            "com.shatteredpixel.shatteredpixeldungeon.items.Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.Recipe$SimpleRecipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.Stylus",
            "com.shatteredpixel.shatteredpixeldungeon.items.TengusMask",
            "com.shatteredpixel.shatteredpixeldungeon.items.Torch",
            "com.shatteredpixel.shatteredpixeldungeon.items.Waterskin",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor$Glyph",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.AntiEntropy",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Corrosion",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Displacement",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Metabolism",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Multiplicity",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Overgrowth",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Stench",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Affection",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Camouflage",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Entanglement",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Repulsion",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Stone",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Thorns",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity$DeferedDamage",
            "com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity$ViscosityTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose$GhostHero",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose$NoRoseDamage",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose$Petal",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband$StolenTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey$KeyReplacementTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey$KeyWall",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight$CharAwareness",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight$HeapAwareness",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass$sandBag",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook",
            "com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook$ExploitHandler",
            "com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag",
            "com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster",
            "com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier",
            "com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb$ArcaneBombFuse",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$ConjuredBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$DoubleBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$EnhanceBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb$Fuse",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.FlashBangBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb$HolyDamage",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker$NoisemakerFuse",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShrapnelBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.SmokeBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.bombs.WoollyBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Berry",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Berry$SeedCounter",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit$Chunks",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit$CookFruit",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty$FishLeftover",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.PhantomMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat$oneMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat$threeMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat$twoMeat",
            "com.shatteredpixel.shatteredpixeldungeon.items.food.SupplyRation",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.AlchemyPage",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.DocumentPage",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.GuidePage",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.Guidebook",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage$Caves",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage$City",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage$Halls",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage$Prison",
            "com.shatteredpixel.shatteredpixeldungeon.items.journal.RegionLorePage$Sewers",
            "com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey",
            "com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey",
            "com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey",
            "com.shatteredpixel.shatteredpixeldungeon.items.keys.Key",
            "com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion$SeedToPotion",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation$AquaHealing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall$FeatherBuff",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight$HTBoost",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion$PotionToExotic",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing$Cleanse",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration$DivineInspirationTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShroudingFog",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina",
            "com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust$DustGhostSpawner",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust$DustWraith",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard",
            "com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.BowFragment",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.BrokenHilt",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.BrokenStaff",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.CloakScrap",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.RemainsItem",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.SealShard",
            "com.shatteredpixel.shatteredpixeldungeon.items.remains.TornPage",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfElements",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEvasion",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce$BrawlersStance",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth$DropsToEquipTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth$TriesToDropTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.InventoryScroll",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll$ScrollToStone",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll$ScrollToExotic",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge$ChallengeArena",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDivination",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment$WndConfirmCancel",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment$WndEnchantSelect",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment$WndGlyphSelect",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfForesight",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis$WndMetamorphChoose",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis$WndMetamorphReplace",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMysticalEnergy",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPrismaticImage",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong",
            "com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong$Enthralled",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize$WndAlchemizeItem",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning$BeaconTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.InventorySpell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap$ReclaimedTrap",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental$InvisAlly",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.TargetedSpell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy",
            "com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression$Aggression",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAugmentation",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDeepSleep",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfDetectMagic",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFlock",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition$IntuitionUseTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser$CenserGasTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ChaoticCenser$GasSpewer",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.CrackedSpyglass",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.DimensionalSundial",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.EyeOfNewt",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MimicTooth",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MossyClump",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.PetrifiedSeed",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SaltCube",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion$ThrownUseTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion$WandUseTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrapMechanism",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket$UpgradeTrinket",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst$RandomTrinket",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst$Recipe",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst$WndTrinket",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.VialOfBlood",
            "com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$AbortRetryFail",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$Alarm",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$Bubbles",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$BurnAndFreeze",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$ConeOfColors",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$CurseEquipment",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$CursedEffect",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$Explosion",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$FireBall",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$ForestFire",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$Geyser",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$GravityChaos",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$HealthTransfer",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$HeroShapeShift",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$InterFloorTeleport",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$Levitate",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$LightningBolt",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$MassInvuln",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$Petrify",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$RandomAreaEffect",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$RandomGas",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$RandomPlant",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$RandomTeleport",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$RandomTransmogrify",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$RandomWand",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SelfOoze",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SheepPolymorph",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SinkHole",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SpawnGoldenMimic",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SpawnRegrowth",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SummonMonsters",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SummonSheep",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand$SuperNova",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave$BWaveOnHitTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave$BlastWave",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave$Knockback",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning$LightningCharge",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth$EarthGuardian",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth$RockArmor",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile$MagicCharge",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth$Dewcatcher",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth$Lotus",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth$Seed",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth$Seedpod",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding$Ward",
            "com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding$WardSentry",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon$Enchantment",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Annoying",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Dazzling",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Displacing",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive$ExplosiveCurseBomb",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Friendly",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Polarized",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Sacrificial",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward$WaywardBuff",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking$BlockBuff",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blooming",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Corrupting",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim$GrimTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic$ConservedDamage",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic$KineticTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky$LuckProc",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.AssassinsBlade",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow$ChargedShot",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail$SpinAbilityTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon$Charger",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff$DefensiveStance",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield$GuardTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade$RunicSlashTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai$ComboStrikeTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar$SwordDance",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle$HarvestBleedTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword$CleaveTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarHammer",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Bolas",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.FishingSpear",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ForceCube",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang$CircleBack",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Javelin",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Kunai",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon$PlaceHolder",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon$UpgradedSetTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken$ShurikenInstantTracker",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingClub",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingHammer",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpear",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Tomahawk",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Trident",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.AdrenalineDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.BlindingDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.ChillingDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.CleansingDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.DisplacingDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.HealingDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.HolyDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.IncendiaryDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.ParalyticDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.RotDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.ShockingDart",
            "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart"
    };

    private static final String[] ALL_MOB_NAMES = new String[]{
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Acidic",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Albino",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredBrute",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredBrute$ArmoredRage",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bandit",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute$BruteRage",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CausticSlime",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalGuardian",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire$SpireSpike",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalWisp",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalWisp$LightBeam",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100$LightningBolt",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM200",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM201",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300$FallingRockBuff",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DelayedRockFall",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonSpawner",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$DKBarrior",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$DKGhoul",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$DKGolem",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$DKMonk",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$DKWarlock",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$KingDamager",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing$Summoning",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EbonyMimic",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental$AllyNewBornElemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental$ChaosElemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental$FireElemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental$FrostElemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental$NewbornFireElemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental$ShockElemental",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye$DeathGaze",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FetidRat",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalCore",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalSentry",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalSpinner",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul$GhoulLifeLink",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollExile",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer$Boulder",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer$GnollRockFall",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer$RockArmor",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGuard",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollSapper",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollTrickster",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Golem",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GreatCrab",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Guard",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.HermitCrab",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk$Focus",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer$NecroSkeleton",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer$NecroSkeletonSprite",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer$SummoningBlockDamage",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.PhantomPiranha",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotHeart",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RotLasher",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Scorpio",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Senior",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman$BlueShaman",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman$EarthenBolt",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman$PurpleShaman",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Shaman$RedShaman",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Skeleton",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SpectralNecromancer",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$BombAbility",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$BombItem",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$FireAbility",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$FireBlob",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$ShockerAbility",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$ShockerBlob",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu$ShockerItem",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.VaultMob",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.VaultRat",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock$DarkBolt",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa$Larva",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa$YogEye",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa$YogRipper",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa$YogScorpio",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$BrightFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$BurningFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$DarkBolt",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$DarkFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$LightBeam",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$RottingFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$RustedFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist$SoiledFist",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith$Quest",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost$Quest",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp$Quest",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.ImpShopkeeper",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage$MirrorInvis",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrismaticImage",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RatKing",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultLaser",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.VaultSentry",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker",
            "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker$Quest"
    };


    {
        image = ItemSpriteSheet.STYLUS;
        unique = true;
        defaultAction = AC_USE;
    }

    @Override
    public String name() { return "개발자 도구"; }

    @Override
    public String desc() {
        return "녹픽던 실험실 LAB2 개발자 도구다. 아이템 생성, 다중 장착, 빠른 강화, 마법부여/저주, 몬스터 소환을 지원한다.";
    }

    @Override public boolean isUpgradable() { return false; }
    @Override public boolean isIdentified() { return true; }
    @Override public int value() { return 0; }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = new ArrayList<>();
        actions.add(AC_USE);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_USE.equals(action)) return "실험실 열기";
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (AC_USE.equals(action)) showMain(hero);
    }

    private void showMain(final Hero hero) {
        GameScene.show(new WndOptions(
                "녹픽던 실험실 LAB2",
                "기능을 골라.",
                "아이템 생성",
                "빠른 강화 +1/+10/+100",
                "반지/유물 다중 장착",
                "마법부여/저주 지정",
                "몬스터 소환",
                "강화 주문서 받기"
        ) {
            @Override protected void onSelect(int index) {
                switch (index) {
                    case 0: showSpawnRoot(hero); break;
                    case 1: selectUpgradeItem(hero); break;
                    case 2: showMultiEquip(hero); break;
                    case 3: showEnchantRoot(hero); break;
                    case 4: showMobPage(hero, loadClasses(ALL_MOB_NAMES, Mob.class), 0); break;
                    case 5: showQuickUpgradeScrolls(hero); break;
                }
            }
        });
    }

    // ---------- ITEM SPAWNER ----------

    private void showSpawnRoot(final Hero hero) {
        GameScene.show(new WndOptions(
                "아이템 생성",
                "분류를 골라. 각 아이템은 1/10/100개 생성 가능.",
                "장비",
                "일반 소모품",
                "신비로운 주문서/물약",
                "연금술/폭탄/주문",
                "퀘스트/유물 특수품",
                "모든 아이템 (위험)"
        ) {
            @Override protected void onSelect(int index) {
                switch (index) {
                    case 0: showGearCategories(hero); break;
                    case 1: showConsumableCategories(hero); break;
                    case 2: showExoticCategories(hero); break;
                    case 3: showItemPage(hero, "연금술/폭탄/주문", loadClasses(ALCHEMY_NAMES, Item.class), 0); break;
                    case 4: showItemPage(hero, "특수/퀘스트", loadClasses(SPECIAL_ITEM_NAMES, Item.class), 0); break;
                    case 5: showItemPage(hero, "모든 아이템 (위험)", loadClasses(ALL_ITEM_NAMES, Item.class), 0); break;
                }
            }
        });
    }

    private void showGearCategories(final Hero hero) {
        GameScene.show(new WndOptions("장비", "종류를 골라.",
                "근접 무기", "투척 무기", "방어구", "마법막대", "반지", "유물", "장신구") {
            @Override protected void onSelect(int index) {
                switch (index) {
                    case 0: showItemPage(hero, "근접 무기", concatCats(Generator.Category.WEP_T1, Generator.Category.WEP_T2, Generator.Category.WEP_T3, Generator.Category.WEP_T4, Generator.Category.WEP_T5), 0); break;
                    case 1: showItemPage(hero, "투척 무기", concatCats(Generator.Category.MIS_T1, Generator.Category.MIS_T2, Generator.Category.MIS_T3, Generator.Category.MIS_T4, Generator.Category.MIS_T5), 0); break;
                    case 2: showItemPage(hero, "방어구", Generator.Category.ARMOR.classes, 0); break;
                    case 3: showItemPage(hero, "마법막대", Generator.Category.WAND.classes, 0); break;
                    case 4: showItemPage(hero, "반지", Generator.Category.RING.classes, 0); break;
                    case 5: showItemPage(hero, "유물", Generator.Category.ARTIFACT.classes, 0); break;
                    case 6: showItemPage(hero, "장신구", Generator.Category.TRINKET.classes, 0); break;
                }
            }
        });
    }

    private void showConsumableCategories(final Hero hero) {
        GameScene.show(new WndOptions("일반 소모품", "종류를 골라.",
                "물약", "주문서", "씨앗", "룬석", "음식") {
            @Override protected void onSelect(int index) {
                switch (index) {
                    case 0: showItemPage(hero, "물약", Generator.Category.POTION.classes, 0); break;
                    case 1: showItemPage(hero, "주문서", Generator.Category.SCROLL.classes, 0); break;
                    case 2: showItemPage(hero, "씨앗", Generator.Category.SEED.classes, 0); break;
                    case 3: showItemPage(hero, "룬석", Generator.Category.STONE.classes, 0); break;
                    case 4: showItemPage(hero, "음식", Generator.Category.FOOD.classes, 0); break;
                }
            }
        });
    }

    private void showExoticCategories(final Hero hero) {
        GameScene.show(new WndOptions("신비로운 아이템", "종류를 골라.", "신비로운 물약", "신비로운 주문서") {
            @Override protected void onSelect(int index) {
                if (index == 0) showItemPage(hero, "신비로운 물약", loadClasses(EXOTIC_POTION_NAMES, Item.class), 0);
                else if (index == 1) showItemPage(hero, "신비로운 주문서", loadClasses(EXOTIC_SCROLL_NAMES, Item.class), 0);
            }
        });
    }

    private void showItemPage(final Hero hero, final String title, final Class<?>[] classes, final int page) {
        if (classes == null || classes.length == 0) {
            GLog.w("이 분류에서 생성 가능한 클래스를 못 찾았어.");
            return;
        }
        final int start = page * PAGE_SIZE;
        final int end = Math.min(start + PAGE_SIZE, classes.length);
        final int count = end - start;
        final boolean prev = page > 0;
        final boolean next = end < classes.length;
        String[] opts = new String[count + (prev ? 1 : 0) + (next ? 1 : 0)];
        for (int i = 0; i < count; i++) opts[i] = itemClassName(classes[start+i]);
        int p=count;
        if (prev) opts[p++]="← 이전";
        if (next) opts[p]="다음 →";

        GameScene.show(new WndOptions(title, "아이템 선택", opts) {
            @Override protected void onSelect(int index) {
                if (index < count) {
                    showQuantity(hero, title, classes, page, classes[start+index]);
                    return;
                }
                int nav=count;
                if (prev) {
                    if (index == nav) { showItemPage(hero, title, classes, page-1); return; }
                    nav++;
                }
                if (next && index == nav) showItemPage(hero, title, classes, page+1);
            }
        });
    }

    private void showQuantity(final Hero hero, final String title, final Class<?>[] classes, final int page, final Class<?> cls) {
        GameScene.show(new WndOptions(itemClassName(cls), "몇 개 생성할까?", "1개", "10개", "100개") {
            @Override protected void onSelect(int index) {
                int qty = index == 0 ? 1 : index == 1 ? 10 : 100;
                spawnItems(hero, cls, qty);
                showItemPage(hero, title, classes, page);
            }
        });
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void spawnItems(Hero hero, Class<?> cls, int qty) {
        try {
            Item probe = (Item) Reflection.newInstance((Class) cls);
            if (probe == null) { GLog.w("생성 실패: " + cls.getSimpleName()); return; }
            if (probe.stackable) {
                probe.quantity(qty);
                prepareAndCollect(hero, probe);
            } else {
                int made=0;
                for (int i=0; i<qty; i++) {
                    Item item = (Item) Reflection.newInstance((Class) cls);
                    if (item == null) continue;
                    prepareAndCollect(hero, item);
                    made++;
                }
                GLog.i(itemClassName(cls) + " x" + made + " 생성");
            }
        } catch (Throwable t) {
            GLog.w("생성 중 오류: " + cls.getSimpleName());
        }
    }

    private void prepareAndCollect(Hero hero, Item item) {
        try { item.identify(); } catch (Throwable ignored) {}
        item.cursed = false;
        item.cursedKnown = true;
        if (!item.collect()) Dungeon.level.drop(item, hero.pos);
        Item.updateQuickslot();
    }

    // ---------- FAST UPGRADE ----------

    private void selectUpgradeItem(final Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() { return "강화할 아이템을 골라."; }
            @Override public boolean itemSelectable(Item item) { return item != DeveloperTool.this && item.isUpgradable(); }
            @Override public void onSelect(Item item) { if (item != null) showUpgradeAmount(hero, item); }
        });
    }

    private void showUpgradeAmount(final Hero hero, final Item item) {
        GameScene.show(new WndOptions(item.title(), "몇 단계 강화할까?", "+1", "+10", "+100") {
            @Override protected void onSelect(int index) {
                int n = index == 0 ? 1 : index == 1 ? 10 : 100;
                int done=0;
                for (int i=0; i<n && item.isUpgradable(); i++) {
                    item.upgrade();
                    done++;
                }
                try { item.identify(); } catch (Throwable ignored) {}
                Item.updateQuickslot();
                GLog.i(item.name() + " : +" + done + "단계 적용");
            }
        });
    }

    private void showQuickUpgradeScrolls(final Hero hero) {
        GameScene.show(new WndOptions("강화의 주문서", "몇 장 받을까?", "1장", "10장", "100장") {
            @Override protected void onSelect(int index) {
                int qty = index == 0 ? 1 : index == 1 ? 10 : 100;
                ScrollOfUpgrade s = new ScrollOfUpgrade();
                s.quantity(qty);
                s.identify();
                if (!s.collect()) Dungeon.level.drop(s, hero.pos);
                Item.updateQuickslot();
            }
        });
    }

    // ---------- MULTI EQUIP ----------

    private void showMultiEquip(final Hero hero) {
        GameScene.show(new WndOptions("다중 장착", "추가 장착한 반지/유물은 가방에 남지만 효과는 장착 상태로 적용돼.",
                "반지/유물 추가 장착", "추가 장착 전부 해제") {
            @Override protected void onSelect(int index) {
                if (index == 0) selectExtraEquip(hero);
                else if (index == 1) unequipAllExtras(hero);
            }
        });
    }

    private void selectExtraEquip(final Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() { return "추가 장착할 반지 또는 유물을 골라."; }
            @Override public boolean itemSelectable(Item item) {
                return (item instanceof Ring || item instanceof Artifact)
                        && item instanceof KindofMisc
                        && !((KindofMisc)item).labExtraEquipped();
            }
            @Override public void onSelect(Item item) {
                if (item instanceof KindofMisc) {
                    if (((KindofMisc)item).labEquipExtra(hero)) GLog.i(item.name() + " 추가 장착됨");
                }
            }
        });
    }

    private void unequipAllExtras(Hero hero) {
        ArrayList<Item> copy = new ArrayList<>(hero.belongings.backpack.items);
        int n=0;
        for (Item i : copy) {
            if (i instanceof KindofMisc && ((KindofMisc)i).labExtraEquipped()) {
                if (((KindofMisc)i).doUnequip(hero, false, false)) n++;
            }
        }
        hero.updateHT(false);
        Item.updateQuickslot();
        GLog.i("추가 장착 " + n + "개 해제");
    }

    // ---------- ENCHANT / GLYPH / CURSE ----------

    private void showEnchantRoot(final Hero hero) {
        GameScene.show(new WndOptions("마법부여 / 저주", "직접 고르기.",
                "무기 마법부여/저주", "방어구 상형문자/저주", "효과 제거", "일반 저주 ON/OFF") {
            @Override protected void onSelect(int index) {
                switch (index) {
                    case 0: selectWeaponEffect(hero); break;
                    case 1: selectArmorEffect(hero); break;
                    case 2: selectEffectRemoval(hero); break;
                    case 3: selectGenericCurse(hero); break;
                }
            }
        });
    }

    private void selectWeaponEffect(final Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() { return "마법부여할 무기를 골라."; }
            @Override public boolean itemSelectable(Item item) { return item instanceof Weapon; }
            @Override public void onSelect(Item item) {
                if (item instanceof Weapon) {
                    Class<?>[] all = concatArrays(Weapon.Enchantment.common, Weapon.Enchantment.uncommon, Weapon.Enchantment.rare, Weapon.Enchantment.curses);
                    showWeaponEffectPage((Weapon)item, all, 0);
                }
            }
        });
    }

    private void showWeaponEffectPage(final Weapon weapon, final Class<?>[] classes, final int page) {
        final int start=page*PAGE_SIZE, end=Math.min(start+PAGE_SIZE, classes.length), count=end-start;
        final boolean prev=page>0, next=end<classes.length;
        String[] opts=new String[count+(prev?1:0)+(next?1:0)];
        for(int i=0;i<count;i++) opts[i]=enchantName(classes[start+i]);
        int p=count; if(prev) opts[p++]="← 이전"; if(next) opts[p]="다음 →";
        GameScene.show(new WndOptions("무기 효과", "원하는 마법부여/저주를 선택", opts){
            @Override protected void onSelect(int index){
                if(index<count){
                    try {
                        Weapon.Enchantment e=(Weapon.Enchantment)Reflection.newInstance((Class)classes[start+index]);
                        weapon.enchant(e);
                        weapon.cursed = e != null && e.curse();
                        weapon.cursedKnown=true; weapon.identify(); Item.updateQuickslot();
                    } catch(Throwable t){ GLog.w("마법부여 실패"); }
                    return;
                }
                int nav=count; if(prev){ if(index==nav){showWeaponEffectPage(weapon,classes,page-1);return;} nav++; }
                if(next&&index==nav) showWeaponEffectPage(weapon,classes,page+1);
            }
        });
    }

    private void selectArmorEffect(final Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() { return "상형문자를 넣을 방어구를 골라."; }
            @Override public boolean itemSelectable(Item item) { return item instanceof Armor; }
            @Override public void onSelect(Item item) {
                if (item instanceof Armor) {
                    Class<?>[] all = concatArrays(Armor.Glyph.common, Armor.Glyph.uncommon, Armor.Glyph.rare, Armor.Glyph.curses);
                    showArmorEffectPage((Armor)item, all, 0);
                }
            }
        });
    }

    private void showArmorEffectPage(final Armor armor, final Class<?>[] classes, final int page) {
        final int start=page*PAGE_SIZE, end=Math.min(start+PAGE_SIZE, classes.length), count=end-start;
        final boolean prev=page>0, next=end<classes.length;
        String[] opts=new String[count+(prev?1:0)+(next?1:0)];
        for(int i=0;i<count;i++) opts[i]=glyphName(classes[start+i]);
        int p=count; if(prev) opts[p++]="← 이전"; if(next) opts[p]="다음 →";
        GameScene.show(new WndOptions("방어구 효과", "원하는 상형문자/저주를 선택", opts){
            @Override protected void onSelect(int index){
                if(index<count){
                    try {
                        Armor.Glyph g=(Armor.Glyph)Reflection.newInstance((Class)classes[start+index]);
                        armor.inscribe(g);
                        armor.cursed = g != null && g.curse();
                        armor.cursedKnown=true; armor.identify(); Item.updateQuickslot();
                    } catch(Throwable t){ GLog.w("상형문자 적용 실패"); }
                    return;
                }
                int nav=count; if(prev){ if(index==nav){showArmorEffectPage(armor,classes,page-1);return;} nav++; }
                if(next&&index==nav) showArmorEffectPage(armor,classes,page+1);
            }
        });
    }

    private void selectEffectRemoval(final Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() { return "효과를 지울 무기/방어구를 골라."; }
            @Override public boolean itemSelectable(Item item) { return item instanceof Weapon || item instanceof Armor; }
            @Override public void onSelect(Item item) {
                if (item instanceof Weapon) ((Weapon)item).enchant(null);
                if (item instanceof Armor) ((Armor)item).inscribe(null);
                item.cursed=false; item.cursedKnown=true; Item.updateQuickslot();
            }
        });
    }

    private void selectGenericCurse(final Hero hero) {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override public String textPrompt() { return "일반 저주 상태를 바꿀 아이템을 골라."; }
            @Override public boolean itemSelectable(Item item) { return item != DeveloperTool.this; }
            @Override public void onSelect(final Item item) {
                if (item == null) return;
                GameScene.show(new WndOptions(item.title(), "일반 저주 상태", "저주 ON", "저주 OFF") {
                    @Override protected void onSelect(int index) {
                        item.cursed = index == 0;
                        item.cursedKnown = true;
                        Item.updateQuickslot();
                    }
                });
            }
        });
    }

    // ---------- MOB SPAWNER ----------

    private void showMobPage(final Hero hero, final Class<?>[] classes, final int page) {
        if (classes == null || classes.length == 0) { GLog.w("소환 가능한 몬스터 클래스를 못 찾았어."); return; }
        final int start=page*PAGE_SIZE, end=Math.min(start+PAGE_SIZE, classes.length), count=end-start;
        final boolean prev=page>0, next=end<classes.length;
        String[] opts=new String[count+(prev?1:0)+(next?1:0)];
        for(int i=0;i<count;i++) opts[i]=mobClassName(classes[start+i]);
        int p=count; if(prev) opts[p++]="← 이전"; if(next) opts[p]="다음 →";
        GameScene.show(new WndOptions("몬스터 소환", "보스/특수몹은 현재 층 이벤트를 망가뜨릴 수 있음.", opts){
            @Override protected void onSelect(int index){
                if(index<count){ selectMobCell(hero, classes[start+index]); return; }
                int nav=count; if(prev){ if(index==nav){showMobPage(hero,classes,page-1);return;} nav++; }
                if(next&&index==nav) showMobPage(hero,classes,page+1);
            }
        });
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void selectMobCell(final Hero hero, final Class<?> cls) {
        GameScene.selectCell(new CellSelector.Listener() {
            @Override public void onSelect(Integer cell) {
                if (cell == null) return;
                if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.passable[cell] || Actor.findChar(cell) != null) {
                    GLog.w("빈 이동 가능 칸을 골라."); return;
                }
                try {
                    Mob mob=(Mob)Reflection.newInstance((Class)cls);
                    if(mob==null){GLog.w("몬스터 생성 실패");return;}
                    mob.pos=cell;
                    mob.state=mob.WANDERING;
                    GameScene.add(mob);
                    Dungeon.observe();
                    GLog.i(mobClassName(cls)+" 소환됨");
                } catch(Throwable t){ GLog.w("이 몬스터는 여기서 생성하지 못했어."); }
            }
            @Override public String prompt() { return mobClassName(cls)+" 소환 위치 선택"; }
        });
    }

    // ---------- HELPERS ----------

    private Class<?>[] concatCats(Generator.Category... cats) {
        ArrayList<Class<?>> out=new ArrayList<>();
        for(Generator.Category c:cats) out.addAll(Arrays.asList(c.classes));
        return out.toArray(new Class<?>[0]);
    }

    private Class<?>[] concatArrays(Class<?>[]... arrays) {
        ArrayList<Class<?>> out=new ArrayList<>();
        for(Class<?>[] a:arrays) out.addAll(Arrays.asList(a));
        return out.toArray(new Class<?>[0]);
    }

    private Class<?>[] loadClasses(String[] names, Class<?> base) {
        ArrayList<Class<?>> out=new ArrayList<>();
        for(String n:names){
            try{
                Class<?> c=Class.forName(n);
                if(base.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()) && !c.isInterface()) out.add(c);
            }catch(Throwable ignored){}
        }
        return out.toArray(new Class<?>[0]);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String itemClassName(Class<?> cls) {
        try {
            Item i=(Item)Reflection.newInstance((Class)cls);
            if(i!=null){ try{i.identify(false);}catch(Throwable ignored){} return i.name(); }
        } catch(Throwable ignored){}
        return cls.getSimpleName();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String mobClassName(Class<?> cls) {
        try {
            Mob m=(Mob)Reflection.newInstance((Class)cls);
            if(m!=null) return m.name();
        } catch(Throwable ignored){}
        return cls.getSimpleName();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String enchantName(Class<?> cls) {
        try {
            Weapon.Enchantment e=(Weapon.Enchantment)Reflection.newInstance((Class)cls);
            if(e!=null) return (e.curse()?"[저주] ":"")+e.name();
        }catch(Throwable ignored){}
        return cls.getSimpleName();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String glyphName(Class<?> cls) {
        try {
            Armor.Glyph g=(Armor.Glyph)Reflection.newInstance((Class)cls);
            if(g!=null) return (g.curse()?"[저주] ":"")+g.name();
        }catch(Throwable ignored){}
        return cls.getSimpleName();
    }
}
