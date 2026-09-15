package net.minecraft.world.entity;

import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.boat.ChestRaft;
import net.minecraft.world.entity.vehicle.boat.Raft;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class EntityTypes {
    private static final float MAGIC_HORSE_WIDTH = 1.3964844F;
    private static final int DISPLAY_TRACKING_RANGE = 10;
    public static final EntityType<Boat> ACACIA_BOAT = register(
        EntityTypeIds.ACACIA_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.ACACIA_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> ACACIA_CHEST_BOAT = register(
        EntityTypeIds.ACACIA_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.ACACIA_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Allay> ALLAY = register(
        EntityTypeIds.ALLAY,
        EntityType.Builder.of(Allay::new, MobCategory.CREATURE)
            .sized(0.35F, 0.6F)
            .eyeHeight(0.36F)
            .ridingOffset(0.04F)
            .clientTrackingRange(8)
            .updateInterval(2)
    );
    public static final EntityType<AreaEffectCloud> AREA_EFFECT_CLOUD = register(
        EntityTypeIds.AREA_EFFECT_CLOUD,
        EntityType.Builder.<AreaEffectCloud>of(AreaEffectCloud::new, MobCategory.MISC)
            .noLootTable()
            .fireImmune()
            .sized(6.0F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Armadillo> ARMADILLO = register(
        EntityTypeIds.ARMADILLO, EntityType.Builder.of(Armadillo::new, MobCategory.CREATURE).sized(0.7F, 0.65F).eyeHeight(0.26F).clientTrackingRange(10)
    );
    public static final EntityType<ArmorStand> ARMOR_STAND = register(
        EntityTypeIds.ARMOR_STAND,
        EntityType.Builder.<ArmorStand>of(ArmorStand::new, MobCategory.MISC).sized(0.5F, 1.975F).eyeHeight(1.7775F).clientTrackingRange(10)
    );
    public static final EntityType<Arrow> ARROW = register(
        EntityTypeIds.ARROW,
        EntityType.Builder.<Arrow>of(Arrow::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
    );
    public static final EntityType<Axolotl> AXOLOTL = register(
        EntityTypeIds.AXOLOTL, EntityType.Builder.of(Axolotl::new, MobCategory.AXOLOTLS).sized(0.75F, 0.42F).eyeHeight(0.2751F).clientTrackingRange(10)
    );
    public static final EntityType<ChestRaft> BAMBOO_CHEST_RAFT = register(
        EntityTypeIds.BAMBOO_CHEST_RAFT,
        EntityType.Builder.of(chestRaftFactory(() -> Items.BAMBOO_CHEST_RAFT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Raft> BAMBOO_RAFT = register(
        EntityTypeIds.BAMBOO_RAFT,
        EntityType.Builder.of(raftFactory(() -> Items.BAMBOO_RAFT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Bat> BAT = register(
        EntityTypeIds.BAT, EntityType.Builder.of(Bat::new, MobCategory.AMBIENT).sized(0.5F, 0.9F).eyeHeight(0.45F).clientTrackingRange(5)
    );
    public static final EntityType<Bee> BEE = register(
        EntityTypeIds.BEE, EntityType.Builder.of(Bee::new, MobCategory.CREATURE).sized(0.55F, 0.5F).eyeHeight(0.3F).clientTrackingRange(8)
    );
    public static final EntityType<Boat> BIRCH_BOAT = register(
        EntityTypeIds.BIRCH_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.BIRCH_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> BIRCH_CHEST_BOAT = register(
        EntityTypeIds.BIRCH_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.BIRCH_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Blaze> BLAZE = register(
        EntityTypeIds.BLAZE, EntityType.Builder.of(Blaze::new, MobCategory.MONSTER).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Display.BlockDisplay> BLOCK_DISPLAY = register(
        EntityTypeIds.BLOCK_DISPLAY,
        EntityType.Builder.of(Display.BlockDisplay::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10).updateInterval(1)
    );
    public static final EntityType<Bogged> BOGGED = register(
        EntityTypeIds.BOGGED,
        EntityType.Builder.of(Bogged::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Breeze> BREEZE = register(
        EntityTypeIds.BREEZE,
        EntityType.Builder.of(Breeze::new, MobCategory.MONSTER).sized(0.6F, 1.77F).eyeHeight(1.3452F).clientTrackingRange(10).notInPeaceful()
    );
    public static final EntityType<BreezeWindCharge> BREEZE_WIND_CHARGE = register(
        EntityTypeIds.BREEZE_WIND_CHARGE,
        EntityType.Builder.<BreezeWindCharge>of(BreezeWindCharge::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.3125F, 0.3125F)
            .eyeHeight(0.0F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Camel> CAMEL = register(
        EntityTypeIds.CAMEL, EntityType.Builder.of(Camel::new, MobCategory.CREATURE).sized(1.7F, 2.375F).eyeHeight(2.275F).clientTrackingRange(10)
    );
    public static final EntityType<CamelHusk> CAMEL_HUSK = register(
        EntityTypeIds.CAMEL_HUSK, EntityType.Builder.of(CamelHusk::new, MobCategory.MONSTER).sized(1.7F, 2.375F).eyeHeight(2.275F).clientTrackingRange(10)
    );
    public static final EntityType<Cat> CAT = register(
        EntityTypeIds.CAT,
        EntityType.Builder.of(Cat::new, MobCategory.CREATURE).sized(0.6F, 0.7F).eyeHeight(0.35F).passengerAttachments(0.5125F).clientTrackingRange(8)
    );
    public static final EntityType<CaveSpider> CAVE_SPIDER = register(
        EntityTypeIds.CAVE_SPIDER,
        EntityType.Builder.of(CaveSpider::new, MobCategory.MONSTER).sized(0.7F, 0.5F).eyeHeight(0.45F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Boat> CHERRY_BOAT = register(
        EntityTypeIds.CHERRY_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.CHERRY_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> CHERRY_CHEST_BOAT = register(
        EntityTypeIds.CHERRY_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.CHERRY_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<MinecartChest> CHEST_MINECART = register(
        EntityTypeIds.CHEST_MINECART,
        EntityType.Builder.of(MinecartChest::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<Chicken> CHICKEN = register(
        EntityTypeIds.CHICKEN,
        EntityType.Builder.of(Chicken::new, MobCategory.CREATURE)
            .sized(0.4F, 0.7F)
            .eyeHeight(0.644F)
            .passengerAttachments(new Vec3(0.0, 0.7, -0.1))
            .clientTrackingRange(10)
    );
    public static final EntityType<Cod> COD = register(
        EntityTypeIds.COD, EntityType.Builder.of(Cod::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.3F).eyeHeight(0.195F).clientTrackingRange(4)
    );
    public static final EntityType<CopperGolem> COPPER_GOLEM = register(
        EntityTypeIds.COPPER_GOLEM, EntityType.Builder.of(CopperGolem::new, MobCategory.MISC).sized(0.49F, 0.98F).eyeHeight(0.8125F).clientTrackingRange(10)
    );
    public static final EntityType<MinecartCommandBlock> COMMAND_BLOCK_MINECART = register(
        EntityTypeIds.COMMAND_BLOCK_MINECART,
        EntityType.Builder.of(MinecartCommandBlock::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
    );
    public static final EntityType<Cow> COW = register(
        EntityTypeIds.COW,
        EntityType.Builder.of(Cow::new, MobCategory.CREATURE).sized(0.9F, 1.4F).eyeHeight(1.3F).passengerAttachments(1.36875F).clientTrackingRange(10)
    );
    public static final EntityType<Creaking> CREAKING = register(
        EntityTypeIds.CREAKING,
        EntityType.Builder.of(Creaking::new, MobCategory.MONSTER).sized(0.9F, 2.7F).eyeHeight(2.3F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Creeper> CREEPER = register(
        EntityTypeIds.CREEPER, EntityType.Builder.of(Creeper::new, MobCategory.MONSTER).sized(0.6F, 1.7F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Boat> DARK_OAK_BOAT = register(
        EntityTypeIds.DARK_OAK_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.DARK_OAK_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> DARK_OAK_CHEST_BOAT = register(
        EntityTypeIds.DARK_OAK_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.DARK_OAK_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Dolphin> DOLPHIN = register(
        EntityTypeIds.DOLPHIN, EntityType.Builder.of(Dolphin::new, MobCategory.WATER_CREATURE).sized(0.9F, 0.6F).eyeHeight(0.3F)
    );
    public static final EntityType<Donkey> DONKEY = register(
        EntityTypeIds.DONKEY,
        EntityType.Builder.of(Donkey::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.5F)
            .eyeHeight(1.425F)
            .passengerAttachments(1.1125F)
            .clientTrackingRange(10)
    );
    public static final EntityType<DragonFireball> DRAGON_FIREBALL = register(
        EntityTypeIds.DRAGON_FIREBALL,
        EntityType.Builder.<DragonFireball>of(DragonFireball::new, MobCategory.MISC).noLootTable().sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<Drowned> DROWNED = register(
        EntityTypeIds.DROWNED,
        EntityType.Builder.of(Drowned::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<ThrownEgg> EGG = register(
        EntityTypeIds.EGG,
        EntityType.Builder.<ThrownEgg>of(ThrownEgg::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<ElderGuardian> ELDER_GUARDIAN = register(
        EntityTypeIds.ELDER_GUARDIAN,
        EntityType.Builder.of(ElderGuardian::new, MobCategory.MONSTER)
            .sized(1.9975F, 1.9975F)
            .eyeHeight(0.99875F)
            .passengerAttachments(2.350625F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<EnderMan> ENDERMAN = register(
        EntityTypeIds.ENDERMAN,
        EntityType.Builder.of(EnderMan::new, MobCategory.MONSTER)
            .sized(0.6F, 2.9F)
            .eyeHeight(2.55F)
            .passengerAttachments(2.80625F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Endermite> ENDERMITE = register(
        EntityTypeIds.ENDERMITE,
        EntityType.Builder.of(Endermite::new, MobCategory.MONSTER)
            .sized(0.4F, 0.3F)
            .eyeHeight(0.13F)
            .passengerAttachments(0.2375F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<EnderDragon> ENDER_DRAGON = register(
        EntityTypeIds.ENDER_DRAGON,
        EntityType.Builder.of(EnderDragon::new, MobCategory.MONSTER).fireImmune().sized(16.0F, 8.0F).passengerAttachments(3.0F).clientTrackingRange(10)
    );
    public static final EntityType<ThrownEnderpearl> ENDER_PEARL = register(
        EntityTypeIds.ENDER_PEARL,
        EntityType.Builder.<ThrownEnderpearl>of(ThrownEnderpearl::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<EndCrystal> END_CRYSTAL = register(
        EntityTypeIds.END_CRYSTAL,
        EntityType.Builder.<EndCrystal>of(EndCrystal::new, MobCategory.MISC)
            .noLootTable()
            .fireImmune()
            .sized(2.0F, 2.0F)
            .clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Evoker> EVOKER = register(
        EntityTypeIds.EVOKER,
        EntityType.Builder.of(Evoker::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<EvokerFangs> EVOKER_FANGS = register(
        EntityTypeIds.EVOKER_FANGS,
        EntityType.Builder.<EvokerFangs>of(EvokerFangs::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.8F).clientTrackingRange(6).updateInterval(2)
    );
    public static final EntityType<ThrownExperienceBottle> EXPERIENCE_BOTTLE = register(
        EntityTypeIds.EXPERIENCE_BOTTLE,
        EntityType.Builder.<ThrownExperienceBottle>of(ThrownExperienceBottle::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<ExperienceOrb> EXPERIENCE_ORB = register(
        EntityTypeIds.EXPERIENCE_ORB,
        EntityType.Builder.<ExperienceOrb>of(ExperienceOrb::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(20)
    );
    public static final EntityType<EyeOfEnder> EYE_OF_ENDER = register(
        EntityTypeIds.EYE_OF_ENDER,
        EntityType.Builder.<EyeOfEnder>of(EyeOfEnder::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(4)
    );
    public static final EntityType<FallingBlockEntity> FALLING_BLOCK = register(
        EntityTypeIds.FALLING_BLOCK,
        EntityType.Builder.<FallingBlockEntity>of(FallingBlockEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.98F, 0.98F)
            .clientTrackingRange(10)
            .updateInterval(20)
    );
    public static final EntityType<LargeFireball> FIREBALL = register(
        EntityTypeIds.FIREBALL,
        EntityType.Builder.<LargeFireball>of(LargeFireball::new, MobCategory.MISC).noLootTable().sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<FireworkRocketEntity> FIREWORK_ROCKET = register(
        EntityTypeIds.FIREWORK_ROCKET,
        EntityType.Builder.<FireworkRocketEntity>of(FireworkRocketEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Fox> FOX = register(
        EntityTypeIds.FOX,
        EntityType.Builder.of(Fox::new, MobCategory.CREATURE)
            .sized(0.6F, 0.7F)
            .eyeHeight(0.4F)
            .passengerAttachments(new Vec3(0.0, 0.6375, -0.25))
            .clientTrackingRange(8)
            .immuneTo(BlockTags.FOX_IMMUNE_TO)
    );
    public static final EntityType<Frog> FROG = register(
        EntityTypeIds.FROG,
        EntityType.Builder.of(Frog::new, MobCategory.CREATURE).sized(0.5F, 0.5F).passengerAttachments(new Vec3(0.0, 0.375, -0.25)).clientTrackingRange(10)
    );
    public static final EntityType<MinecartFurnace> FURNACE_MINECART = register(
        EntityTypeIds.FURNACE_MINECART,
        EntityType.Builder.of(MinecartFurnace::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<Ghast> GHAST = register(
        EntityTypeIds.GHAST,
        EntityType.Builder.of(Ghast::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(4.0F, 4.0F)
            .eyeHeight(2.6F)
            .passengerAttachments(4.0625F)
            .ridingOffset(0.5F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<HappyGhast> HAPPY_GHAST = register(
        EntityTypeIds.HAPPY_GHAST,
        EntityType.Builder.of(HappyGhast::new, MobCategory.CREATURE)
            .sized(4.0F, 4.0F)
            .eyeHeight(2.6F)
            .passengerAttachments(new Vec3(0.0, 4.0, 1.7), new Vec3(-1.7, 4.0, 0.0), new Vec3(0.0, 4.0, -1.7), new Vec3(1.7, 4.0, 0.0))
            .ridingOffset(0.5F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Giant> GIANT = register(
        EntityTypeIds.GIANT,
        EntityType.Builder.of(Giant::new, MobCategory.MONSTER)
            .sized(3.6F, 12.0F)
            .eyeHeight(10.44F)
            .ridingOffset(-3.75F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<GlowItemFrame> GLOW_ITEM_FRAME = register(
        EntityTypeIds.GLOW_ITEM_FRAME,
        EntityType.Builder.<GlowItemFrame>of(GlowItemFrame::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.0F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<GlowSquid> GLOW_SQUID = register(
        EntityTypeIds.GLOW_SQUID,
        EntityType.Builder.of(GlowSquid::new, MobCategory.UNDERGROUND_WATER_CREATURE).sized(0.8F, 0.8F).eyeHeight(0.4F).clientTrackingRange(10)
    );
    public static final EntityType<Goat> GOAT = register(
        EntityTypeIds.GOAT, EntityType.Builder.of(Goat::new, MobCategory.CREATURE).sized(0.9F, 1.3F).passengerAttachments(1.1125F).clientTrackingRange(10)
    );
    public static final EntityType<Guardian> GUARDIAN = register(
        EntityTypeIds.GUARDIAN,
        EntityType.Builder.of(Guardian::new, MobCategory.MONSTER)
            .sized(0.85F, 0.85F)
            .eyeHeight(0.425F)
            .passengerAttachments(0.975F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Hoglin> HOGLIN = register(
        EntityTypeIds.HOGLIN,
        EntityType.Builder.of(Hoglin::new, MobCategory.MONSTER).sized(1.3964844F, 1.4F).passengerAttachments(1.49375F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<MinecartHopper> HOPPER_MINECART = register(
        EntityTypeIds.HOPPER_MINECART,
        EntityType.Builder.of(MinecartHopper::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<Horse> HORSE = register(
        EntityTypeIds.HORSE,
        EntityType.Builder.of(Horse::new, MobCategory.CREATURE).sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.44375F).clientTrackingRange(10)
    );
    public static final EntityType<Husk> HUSK = register(
        EntityTypeIds.HUSK,
        EntityType.Builder.of(Husk::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.075F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Illusioner> ILLUSIONER = register(
        EntityTypeIds.ILLUSIONER,
        EntityType.Builder.of(Illusioner::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Interaction> INTERACTION = register(
        EntityTypeIds.INTERACTION, EntityType.Builder.of(Interaction::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10)
    );
    public static final EntityType<IronGolem> IRON_GOLEM = register(
        EntityTypeIds.IRON_GOLEM, EntityType.Builder.of(IronGolem::new, MobCategory.MISC).sized(1.4F, 2.7F).clientTrackingRange(10)
    );
    public static final EntityType<ItemEntity> ITEM = register(
        EntityTypeIds.ITEM,
        EntityType.Builder.<ItemEntity>of(ItemEntity::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .eyeHeight(0.2125F)
            .clientTrackingRange(6)
            .updateInterval(20)
    );
    public static final EntityType<Display.ItemDisplay> ITEM_DISPLAY = register(
        EntityTypeIds.ITEM_DISPLAY,
        EntityType.Builder.of(Display.ItemDisplay::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10).updateInterval(1)
    );
    public static final EntityType<ItemFrame> ITEM_FRAME = register(
        EntityTypeIds.ITEM_FRAME,
        EntityType.Builder.<ItemFrame>of(ItemFrame::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.0F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Boat> JUNGLE_BOAT = register(
        EntityTypeIds.JUNGLE_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.JUNGLE_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> JUNGLE_CHEST_BOAT = register(
        EntityTypeIds.JUNGLE_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.JUNGLE_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<LeashFenceKnotEntity> LEASH_KNOT = register(
        EntityTypeIds.LEASH_KNOT,
        EntityType.Builder.<LeashFenceKnotEntity>of(LeashFenceKnotEntity::new, MobCategory.MISC)
            .noLootTable()
            .noSave()
            .sized(0.375F, 0.5F)
            .eyeHeight(0.0625F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<LightningBolt> LIGHTNING_BOLT = register(
        EntityTypeIds.LIGHTNING_BOLT,
        EntityType.Builder.of(LightningBolt::new, MobCategory.MISC)
            .noLootTable()
            .noSave()
            .sized(0.0F, 0.0F)
            .clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Llama> LLAMA = register(
        EntityTypeIds.LLAMA,
        EntityType.Builder.of(Llama::new, MobCategory.CREATURE)
            .sized(0.9F, 1.87F)
            .eyeHeight(1.7765F)
            .passengerAttachments(new Vec3(0.0, 1.37, -0.3))
            .clientTrackingRange(10)
    );
    public static final EntityType<LlamaSpit> LLAMA_SPIT = register(
        EntityTypeIds.LLAMA_SPIT,
        EntityType.Builder.<LlamaSpit>of(LlamaSpit::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<MagmaCube> MAGMA_CUBE = register(
        EntityTypeIds.MAGMA_CUBE,
        EntityType.Builder.of(MagmaCube::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.52F, 0.52F)
            .eyeHeight(0.325F)
            .spawnDimensionsScale(4.0F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Boat> MANGROVE_BOAT = register(
        EntityTypeIds.MANGROVE_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.MANGROVE_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> MANGROVE_CHEST_BOAT = register(
        EntityTypeIds.MANGROVE_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.MANGROVE_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Mannequin> MANNEQUIN = register(
        EntityTypeIds.MANNEQUIN,
        EntityType.Builder.of(Mannequin::create, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .vehicleAttachment(Avatar.DEFAULT_VEHICLE_ATTACHMENT)
            .clientTrackingRange(32)
            .updateInterval(2)
    );
    public static final EntityType<Marker> MARKER = register(
        EntityTypeIds.MARKER, EntityType.Builder.of(Marker::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(0)
    );
    public static final EntityType<Minecart> MINECART = register(
        EntityTypeIds.MINECART,
        EntityType.Builder.of(Minecart::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<MushroomCow> MOOSHROOM = register(
        EntityTypeIds.MOOSHROOM,
        EntityType.Builder.of(MushroomCow::new, MobCategory.CREATURE).sized(0.9F, 1.4F).eyeHeight(1.3F).passengerAttachments(1.36875F).clientTrackingRange(10)
    );
    public static final EntityType<Mule> MULE = register(
        EntityTypeIds.MULE,
        EntityType.Builder.of(Mule::new, MobCategory.CREATURE).sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.2125F).clientTrackingRange(8)
    );
    public static final EntityType<Nautilus> NAUTILUS = register(
        EntityTypeIds.NAUTILUS,
        EntityType.Builder.of(Nautilus::new, MobCategory.WATER_CREATURE)
            .sized(0.875F, 0.95F)
            .passengerAttachments(1.1375F)
            .eyeHeight(0.2751F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Boat> OAK_BOAT = register(
        EntityTypeIds.OAK_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.OAK_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> OAK_CHEST_BOAT = register(
        EntityTypeIds.OAK_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.OAK_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Ocelot> OCELOT = register(
        EntityTypeIds.OCELOT, EntityType.Builder.of(Ocelot::new, MobCategory.CREATURE).sized(0.6F, 0.7F).passengerAttachments(0.6375F).clientTrackingRange(10)
    );
    public static final EntityType<OminousItemSpawner> OMINOUS_ITEM_SPAWNER = register(
        EntityTypeIds.OMINOUS_ITEM_SPAWNER,
        EntityType.Builder.of(OminousItemSpawner::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(8)
    );
    public static final EntityType<Painting> PAINTING = register(
        EntityTypeIds.PAINTING,
        EntityType.Builder.<Painting>of(Painting::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(Integer.MAX_VALUE)
    );
    public static final EntityType<Boat> PALE_OAK_BOAT = register(
        EntityTypeIds.PALE_OAK_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.PALE_OAK_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> PALE_OAK_CHEST_BOAT = register(
        EntityTypeIds.PALE_OAK_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.PALE_OAK_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Panda> PANDA = register(
        EntityTypeIds.PANDA, EntityType.Builder.of(Panda::new, MobCategory.CREATURE).sized(1.3F, 1.25F).clientTrackingRange(10)
    );
    public static final EntityType<Parched> PARCHED = register(
        EntityTypeIds.PARCHED,
        EntityType.Builder.of(Parched::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).ridingOffset(-0.7F).clientTrackingRange(8).notInPeaceful()
    );
    public static final EntityType<Parrot> PARROT = register(
        EntityTypeIds.PARROT,
        EntityType.Builder.of(Parrot::new, MobCategory.CREATURE).sized(0.5F, 0.9F).eyeHeight(0.54F).passengerAttachments(0.4625F).clientTrackingRange(8)
    );
    public static final EntityType<Phantom> PHANTOM = register(
        EntityTypeIds.PHANTOM,
        EntityType.Builder.of(Phantom::new, MobCategory.MONSTER)
            .sized(0.9F, 0.5F)
            .eyeHeight(0.175F)
            .passengerAttachments(0.3375F)
            .ridingOffset(-0.125F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Pig> PIG = register(
        EntityTypeIds.PIG, EntityType.Builder.of(Pig::new, MobCategory.CREATURE).sized(0.9F, 0.9F).passengerAttachments(0.86875F).clientTrackingRange(10)
    );
    public static final EntityType<Piglin> PIGLIN = register(
        EntityTypeIds.PIGLIN,
        EntityType.Builder.of(Piglin::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.79F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
    );
    public static final EntityType<PiglinBrute> PIGLIN_BRUTE = register(
        EntityTypeIds.PIGLIN_BRUTE,
        EntityType.Builder.of(PiglinBrute::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.79F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Pillager> PILLAGER = register(
        EntityTypeIds.PILLAGER,
        EntityType.Builder.of(Pillager::new, MobCategory.MONSTER)
            .canSpawnFarFromPlayer()
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<PolarBear> POLAR_BEAR = register(
        EntityTypeIds.POLAR_BEAR,
        EntityType.Builder.of(PolarBear::new, MobCategory.CREATURE).immuneTo(BlockTags.POLAR_BEAR_IMMUNE_TO).sized(1.4F, 1.4F).clientTrackingRange(10)
    );
    public static final EntityType<ThrownSplashPotion> SPLASH_POTION = register(
        EntityTypeIds.SPLASH_POTION,
        EntityType.Builder.<ThrownSplashPotion>of(ThrownSplashPotion::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<ThrownLingeringPotion> LINGERING_POTION = register(
        EntityTypeIds.LINGERING_POTION,
        EntityType.Builder.<ThrownLingeringPotion>of(ThrownLingeringPotion::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Pufferfish> PUFFERFISH = register(
        EntityTypeIds.PUFFERFISH, EntityType.Builder.of(Pufferfish::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.7F).eyeHeight(0.455F).clientTrackingRange(4)
    );
    public static final EntityType<Rabbit> RABBIT = register(
        EntityTypeIds.RABBIT, EntityType.Builder.of(Rabbit::new, MobCategory.CREATURE).sized(0.49F, 0.6F).eyeHeight(0.59F).clientTrackingRange(8)
    );
    public static final EntityType<Ravager> RAVAGER = register(
        EntityTypeIds.RAVAGER,
        EntityType.Builder.of(Ravager::new, MobCategory.MONSTER)
            .sized(1.95F, 2.2F)
            .passengerAttachments(new Vec3(0.0, 2.2625, -0.0625))
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<Salmon> SALMON = register(
        EntityTypeIds.SALMON, EntityType.Builder.of(Salmon::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.4F).eyeHeight(0.26F).clientTrackingRange(4)
    );
    public static final EntityType<Sheep> SHEEP = register(
        EntityTypeIds.SHEEP,
        EntityType.Builder.of(Sheep::new, MobCategory.CREATURE).sized(0.9F, 1.3F).eyeHeight(1.235F).passengerAttachments(1.2375F).clientTrackingRange(10)
    );
    public static final EntityType<Shulker> SHULKER = register(
        EntityTypeIds.SHULKER,
        EntityType.Builder.of(Shulker::new, MobCategory.MONSTER).fireImmune().canSpawnFarFromPlayer().sized(1.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(10)
    );
    public static final EntityType<ShulkerBullet> SHULKER_BULLET = register(
        EntityTypeIds.SHULKER_BULLET,
        EntityType.Builder.<ShulkerBullet>of(ShulkerBullet::new, MobCategory.MISC).noLootTable().sized(0.3125F, 0.3125F).clientTrackingRange(8)
    );
    public static final EntityType<Silverfish> SILVERFISH = register(
        EntityTypeIds.SILVERFISH,
        EntityType.Builder.of(Silverfish::new, MobCategory.MONSTER)
            .sized(0.4F, 0.3F)
            .eyeHeight(0.13F)
            .passengerAttachments(0.2375F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Skeleton> SKELETON = register(
        EntityTypeIds.SKELETON,
        EntityType.Builder.of(Skeleton::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F)
            .eyeHeight(1.74F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<SkeletonHorse> SKELETON_HORSE = register(
        EntityTypeIds.SKELETON_HORSE,
        EntityType.Builder.of(SkeletonHorse::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.6F)
            .eyeHeight(1.52F)
            .passengerAttachments(1.31875F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Slime> SLIME = register(
        EntityTypeIds.SLIME,
        EntityType.Builder.of(Slime::new, MobCategory.MONSTER)
            .sized(0.52F, 0.52F)
            .eyeHeight(0.325F)
            .spawnDimensionsScale(4.0F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<SmallFireball> SMALL_FIREBALL = register(
        EntityTypeIds.SMALL_FIREBALL,
        EntityType.Builder.<SmallFireball>of(SmallFireball::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.3125F, 0.3125F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Sniffer> SNIFFER = register(
        EntityTypeIds.SNIFFER,
        EntityType.Builder.of(Sniffer::new, MobCategory.CREATURE)
            .sized(1.9F, 1.75F)
            .eyeHeight(1.05F)
            .passengerAttachments(2.09375F)
            .nameTagOffset(2.05F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Snowball> SNOWBALL = register(
        EntityTypeIds.SNOWBALL,
        EntityType.Builder.<Snowball>of(Snowball::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<SnowGolem> SNOW_GOLEM = register(
        EntityTypeIds.SNOW_GOLEM,
        EntityType.Builder.of(SnowGolem::new, MobCategory.MISC)
            .immuneTo(BlockTags.SNOW_GOLEM_IMMUNE_TO)
            .sized(0.7F, 1.9F)
            .eyeHeight(1.7F)
            .clientTrackingRange(8)
    );
    public static final EntityType<MinecartSpawner> SPAWNER_MINECART = register(
        EntityTypeIds.SPAWNER_MINECART,
        EntityType.Builder.of(MinecartSpawner::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<SpectralArrow> SPECTRAL_ARROW = register(
        EntityTypeIds.SPECTRAL_ARROW,
        EntityType.Builder.<SpectralArrow>of(SpectralArrow::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
    );
    public static final EntityType<Spider> SPIDER = register(
        EntityTypeIds.SPIDER,
        EntityType.Builder.of(Spider::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F)
            .eyeHeight(0.65F)
            .passengerAttachments(0.765F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Boat> SPRUCE_BOAT = register(
        EntityTypeIds.SPRUCE_BOAT,
        EntityType.Builder.of(boatFactory(() -> Items.SPRUCE_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ChestBoat> SPRUCE_CHEST_BOAT = register(
        EntityTypeIds.SPRUCE_CHEST_BOAT,
        EntityType.Builder.of(chestBoatFactory(() -> Items.SPRUCE_CHEST_BOAT), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
    );
    public static final EntityType<Squid> SQUID = register(
        EntityTypeIds.SQUID, EntityType.Builder.of(Squid::new, MobCategory.WATER_CREATURE).sized(0.8F, 0.8F).eyeHeight(0.4F).clientTrackingRange(8)
    );
    public static final EntityType<Stray> STRAY = register(
        EntityTypeIds.STRAY,
        EntityType.Builder.of(Stray::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F)
            .eyeHeight(1.74F)
            .ridingOffset(-0.7F)
            .immuneTo(BlockTags.STRAY_IMMUNE_TO)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Strider> STRIDER = register(
        EntityTypeIds.STRIDER, EntityType.Builder.of(Strider::new, MobCategory.CREATURE).fireImmune().sized(0.9F, 1.7F).clientTrackingRange(10)
    );
    public static final EntityType<SulfurCube> SULFUR_CUBE = register(
        EntityTypeIds.SULFUR_CUBE,
        EntityType.Builder.of(SulfurCube::new, MobCategory.MONSTER).sized(0.49F, 0.49F).eyeHeight(0.175F).spawnDimensionsScale(2.0F).clientTrackingRange(10)
    );
    public static final EntityType<Tadpole> TADPOLE = register(
        EntityTypeIds.TADPOLE, EntityType.Builder.of(Tadpole::new, MobCategory.CREATURE).sized(0.4F, 0.3F).eyeHeight(0.19500001F).clientTrackingRange(10)
    );
    public static final EntityType<Display.TextDisplay> TEXT_DISPLAY = register(
        EntityTypeIds.TEXT_DISPLAY,
        EntityType.Builder.of(Display.TextDisplay::new, MobCategory.MISC).noLootTable().sized(0.0F, 0.0F).clientTrackingRange(10).updateInterval(1)
    );
    public static final EntityType<PrimedTnt> TNT = register(
        EntityTypeIds.TNT,
        EntityType.Builder.<PrimedTnt>of(PrimedTnt::new, MobCategory.MISC)
            .noLootTable()
            .fireImmune()
            .sized(0.98F, 0.98F)
            .eyeHeight(0.15F)
            .clientTrackingRange(10)
            .updateInterval(10)
    );
    public static final EntityType<MinecartTNT> TNT_MINECART = register(
        EntityTypeIds.TNT_MINECART,
        EntityType.Builder.of(MinecartTNT::new, MobCategory.MISC).noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );
    public static final EntityType<TraderLlama> TRADER_LLAMA = register(
        EntityTypeIds.TRADER_LLAMA,
        EntityType.Builder.of(TraderLlama::new, MobCategory.CREATURE)
            .sized(0.9F, 1.87F)
            .eyeHeight(1.7765F)
            .passengerAttachments(new Vec3(0.0, 1.37, -0.3))
            .clientTrackingRange(10)
    );
    public static final EntityType<ThrownTrident> TRIDENT = register(
        EntityTypeIds.TRIDENT,
        EntityType.Builder.<ThrownTrident>of(ThrownTrident::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
    );
    public static final EntityType<TropicalFish> TROPICAL_FISH = register(
        EntityTypeIds.TROPICAL_FISH,
        EntityType.Builder.of(TropicalFish::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.4F).eyeHeight(0.26F).clientTrackingRange(4)
    );
    public static final EntityType<Turtle> TURTLE = register(
        EntityTypeIds.TURTLE,
        EntityType.Builder.of(Turtle::new, MobCategory.CREATURE).sized(1.2F, 0.4F).passengerAttachments(new Vec3(0.0, 0.55625, -0.25)).clientTrackingRange(10)
    );
    public static final EntityType<Vex> VEX = register(
        EntityTypeIds.VEX,
        EntityType.Builder.of(Vex::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.4F, 0.8F)
            .eyeHeight(0.51875F)
            .passengerAttachments(0.7375F)
            .ridingOffset(0.04F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Villager> VILLAGER = register(
        EntityTypeIds.VILLAGER, EntityType.Builder.of(Villager::new, MobCategory.MISC).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10)
    );
    public static final EntityType<Vindicator> VINDICATOR = register(
        EntityTypeIds.VINDICATOR,
        EntityType.Builder.of(Vindicator::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.6F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<WanderingTrader> WANDERING_TRADER = register(
        EntityTypeIds.WANDERING_TRADER,
        EntityType.Builder.of(WanderingTrader::new, MobCategory.CREATURE).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10)
    );
    public static final EntityType<Warden> WARDEN = register(
        EntityTypeIds.WARDEN,
        EntityType.Builder.of(Warden::new, MobCategory.MONSTER)
            .sized(0.9F, 2.9F)
            .passengerAttachments(3.15F)
            .attach(EntityAttachment.WARDEN_CHEST, 0.0F, 1.6F, 0.0F)
            .clientTrackingRange(16)
            .fireImmune()
            .notInPeaceful()
    );
    public static final EntityType<WindCharge> WIND_CHARGE = register(
        EntityTypeIds.WIND_CHARGE,
        EntityType.Builder.<WindCharge>of(WindCharge::new, MobCategory.MISC)
            .noLootTable()
            .sized(0.3125F, 0.3125F)
            .eyeHeight(0.0F)
            .clientTrackingRange(4)
            .updateInterval(10)
    );
    public static final EntityType<Witch> WITCH = register(
        EntityTypeIds.WITCH,
        EntityType.Builder.of(Witch::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.62F)
            .passengerAttachments(2.2625F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<WitherBoss> WITHER = register(
        EntityTypeIds.WITHER,
        EntityType.Builder.of(WitherBoss::new, MobCategory.MONSTER)
            .fireImmune()
            .immuneTo(BlockTags.WITHER_IMMUNE_TO)
            .sized(0.9F, 3.5F)
            .clientTrackingRange(10)
            .notInPeaceful()
    );
    public static final EntityType<WitherSkeleton> WITHER_SKELETON = register(
        EntityTypeIds.WITHER_SKELETON,
        EntityType.Builder.of(WitherSkeleton::new, MobCategory.MONSTER)
            .fireImmune()
            .immuneTo(BlockTags.WITHER_SKELETON_IMMUNE_TO)
            .sized(0.7F, 2.4F)
            .eyeHeight(2.1F)
            .ridingOffset(-0.875F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<WitherSkull> WITHER_SKULL = register(
        EntityTypeIds.WITHER_SKULL,
        EntityType.Builder.<WitherSkull>of(WitherSkull::new, MobCategory.MISC).noLootTable().sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
    );
    public static final EntityType<Wolf> WOLF = register(
        EntityTypeIds.WOLF,
        EntityType.Builder.of(Wolf::new, MobCategory.CREATURE)
            .sized(0.6F, 0.85F)
            .eyeHeight(0.68F)
            .passengerAttachments(new Vec3(0.0, 0.81875, -0.0625))
            .clientTrackingRange(10)
    );
    public static final EntityType<Zoglin> ZOGLIN = register(
        EntityTypeIds.ZOGLIN,
        EntityType.Builder.of(Zoglin::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(1.3964844F, 1.4F)
            .passengerAttachments(1.49375F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Zombie> ZOMBIE = register(
        EntityTypeIds.ZOMBIE,
        EntityType.Builder.<Zombie>of(Zombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<ZombieHorse> ZOMBIE_HORSE = register(
        EntityTypeIds.ZOMBIE_HORSE,
        EntityType.Builder.of(ZombieHorse::new, MobCategory.MONSTER)
            .sized(1.3964844F, 1.6F)
            .eyeHeight(1.52F)
            .passengerAttachments(1.31875F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ZombieNautilus> ZOMBIE_NAUTILUS = register(
        EntityTypeIds.ZOMBIE_NAUTILUS,
        EntityType.Builder.of(ZombieNautilus::new, MobCategory.MONSTER)
            .sized(0.875F, 0.95F)
            .passengerAttachments(1.1375F)
            .eyeHeight(0.2751F)
            .clientTrackingRange(10)
    );
    public static final EntityType<ZombieVillager> ZOMBIE_VILLAGER = register(
        EntityTypeIds.ZOMBIE_VILLAGER,
        EntityType.Builder.of(ZombieVillager::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .passengerAttachments(2.125F)
            .ridingOffset(-0.7F)
            .eyeHeight(1.74F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<ZombifiedPiglin> ZOMBIFIED_PIGLIN = register(
        EntityTypeIds.ZOMBIFIED_PIGLIN,
        EntityType.Builder.of(ZombifiedPiglin::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.6F, 1.95F)
            .eyeHeight(1.79F)
            .passengerAttachments(2.0F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8)
            .notInPeaceful()
    );
    public static final EntityType<Player> PLAYER = register(
        EntityTypeIds.PLAYER,
        EntityType.Builder.<Player>createNothing(MobCategory.MISC)
            .noSave()
            .noSummon()
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .vehicleAttachment(Avatar.DEFAULT_VEHICLE_ATTACHMENT)
            .clientTrackingRange(32)
            .updateInterval(2)
    );
    public static final EntityType<FishingHook> FISHING_BOBBER = register(
        EntityTypeIds.FISHING_BOBBER,
        EntityType.Builder.<FishingHook>of(FishingHook::new, MobCategory.MISC)
            .noLootTable()
            .noSave()
            .noSummon()
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(5)
    );
    static final Set<EntityType<?>> OP_ONLY_CUSTOM_DATA = Set.of(FALLING_BLOCK, COMMAND_BLOCK_MINECART, SPAWNER_MINECART);

    private static EntityType.EntityFactory<Boat> boatFactory(final Supplier<Item> boatItem) {
        return (entityType, level) -> new Boat(entityType, level, boatItem);
    }

    private static EntityType.EntityFactory<ChestBoat> chestBoatFactory(final Supplier<Item> dropItem) {
        return (entityType, level) -> new ChestBoat(entityType, level, dropItem);
    }

    private static EntityType.EntityFactory<Raft> raftFactory(final Supplier<Item> dropItem) {
        return (entityType, level) -> new Raft(entityType, level, dropItem);
    }

    private static EntityType.EntityFactory<ChestRaft> chestRaftFactory(final Supplier<Item> dropItem) {
        return (entityType, level) -> new ChestRaft(entityType, level, dropItem);
    }

    private static <T extends Entity> EntityType<T> register(final ResourceKey<EntityType<?>> id, final EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(id));
    }
}