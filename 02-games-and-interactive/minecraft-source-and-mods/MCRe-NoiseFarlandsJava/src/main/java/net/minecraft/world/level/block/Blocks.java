package net.minecraft.world.level.block;

import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class Blocks {
    private static final BlockBehaviour.StatePredicate NOT_CLOSED_SHULKER = (statex, level, pos) -> level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity
        ? shulkerBoxBlockEntity.isClosed()
        : true;
    private static final BlockBehaviour.StatePredicate NOT_EXTENDED_PISTON = (statex, level, pos) -> !statex.getValue(PistonBaseBlock.EXTENDED);
    public static final Block AIR = register(BlockItemIds.AIR, AirBlock::new, BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().air());
    public static final Block STONE = register(
        BlockItemIds.STONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block GRANITE = register(
        BlockItemIds.GRANITE,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block POLISHED_GRANITE = register(
        BlockItemIds.POLISHED_GRANITE,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block DIORITE = register(
        BlockItemIds.DIORITE,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block POLISHED_DIORITE = register(
        BlockItemIds.POLISHED_DIORITE,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block ANDESITE = register(
        BlockItemIds.ANDESITE,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block POLISHED_ANDESITE = register(
        BlockItemIds.POLISHED_ANDESITE,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block GRASS_BLOCK = register(
        BlockItemIds.GRASS_BLOCK, GrassBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).randomTicks().strength(0.6F).sound(SoundType.GRASS)
    );
    public static final Block DIRT = register(BlockItemIds.DIRT, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL));
    public static final Block COARSE_DIRT = register(
        BlockItemIds.COARSE_DIRT, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL)
    );
    public static final Block PODZOL = register(
        BlockItemIds.PODZOL, SnowyBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(0.5F).sound(SoundType.GRAVEL)
    );
    public static final Block COBBLESTONE = register(
        BlockItemIds.COBBLESTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block OAK_PLANKS = register(
        BlockItemIds.OAK_PLANKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block SPRUCE_PLANKS = register(
        BlockItemIds.SPRUCE_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PODZOL)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block BIRCH_PLANKS = register(
        BlockItemIds.BIRCH_PLANKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block JUNGLE_PLANKS = register(
        BlockItemIds.JUNGLE_PLANKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block ACACIA_PLANKS = register(
        BlockItemIds.ACACIA_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block CHERRY_PLANKS = register(
        BlockItemIds.CHERRY_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.CHERRY_WOOD)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_PLANKS = register(
        BlockItemIds.DARK_OAK_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_WOOD = register(
        BlockItemIds.PALE_OAK_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block PALE_OAK_PLANKS = register(
        BlockItemIds.PALE_OAK_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block MANGROVE_PLANKS = register(
        BlockItemIds.MANGROVE_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block BAMBOO_PLANKS = register(
        BlockItemIds.BAMBOO_PLANKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.BAMBOO_WOOD)
            .ignitedByLava()
    );
    public static final Block BAMBOO_MOSAIC = register(
        BlockItemIds.BAMBOO_MOSAIC,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.BAMBOO_WOOD)
            .ignitedByLava()
    );
    public static final Block OAK_SAPLING = register(
        BlockItemIds.OAK_SAPLING,
        p -> new SaplingBlock(TreeGrower.OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SPRUCE_SAPLING = register(
        BlockItemIds.SPRUCE_SAPLING,
        p -> new SaplingBlock(TreeGrower.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BIRCH_SAPLING = register(
        BlockItemIds.BIRCH_SAPLING,
        p -> new SaplingBlock(TreeGrower.BIRCH, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block JUNGLE_SAPLING = register(
        BlockItemIds.JUNGLE_SAPLING,
        p -> new SaplingBlock(TreeGrower.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ACACIA_SAPLING = register(
        BlockItemIds.ACACIA_SAPLING,
        p -> new SaplingBlock(TreeGrower.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CHERRY_SAPLING = register(
        BlockItemIds.CHERRY_SAPLING,
        p -> new SaplingBlock(TreeGrower.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CHERRY_SAPLING)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block DARK_OAK_SAPLING = register(
        BlockItemIds.DARK_OAK_SAPLING,
        p -> new SaplingBlock(TreeGrower.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PALE_OAK_SAPLING = register(
        BlockItemIds.PALE_OAK_SAPLING,
        p -> new SaplingBlock(TreeGrower.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MANGROVE_PROPAGULE = register(
        BlockItemIds.MANGROVE_PROPAGULE,
        p -> new MangrovePropaguleBlock(TreeGrower.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BEDROCK = register(
        BlockItemIds.BEDROCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .isValidSpawn(Blocks::never)
    );
    public static final Block WATER = register(
        BlockIds.WATER,
        p -> new LiquidBlock(Fluids.WATER, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .replaceable()
            .noCollision()
            .strength(100.0F)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
    );
    public static final Block LAVA = register(
        BlockIds.LAVA,
        p -> new LiquidBlock(Fluids.LAVA, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.FIRE)
            .replaceable()
            .noCollision()
            .randomTicks()
            .strength(100.0F)
            .lightLevel(statex -> 15)
            .pushReaction(PushReaction.DESTROY)
            .noLootTable()
            .liquid()
            .sound(SoundType.EMPTY)
    );
    public static final Block SAND = register(
        BlockItemIds.SAND,
        p -> new SandBlock(new ColorRGBA(14406560), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
    );
    public static final Block SUSPICIOUS_SAND = register(
        BlockItemIds.SUSPICIOUS_SAND,
        p -> new BrushableBlock(SAND, SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .instrument(NoteBlockInstrument.SNARE)
            .strength(0.25F)
            .sound(SoundType.SUSPICIOUS_SAND)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block RED_SAND = register(
        BlockItemIds.RED_SAND,
        p -> new SandBlock(new ColorRGBA(11098145), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
    );
    public static final Block GRAVEL = register(
        BlockItemIds.GRAVEL,
        p -> new ColoredFallingBlock(new ColorRGBA(-8356741), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.SNARE).strength(0.6F).sound(SoundType.GRAVEL)
    );
    public static final Block SUSPICIOUS_GRAVEL = register(
        BlockItemIds.SUSPICIOUS_GRAVEL,
        p -> new BrushableBlock(GRAVEL, SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.SNARE)
            .strength(0.25F)
            .sound(SoundType.SUSPICIOUS_GRAVEL)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block GOLD_ORE = register(
        BlockItemIds.GOLD_ORE,
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_GOLD_ORE = register(
        BlockItemIds.DEEPSLATE_GOLD_ORE,
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        BlockBehaviour.Properties.ofLegacyCopy(GOLD_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block IRON_ORE = register(
        BlockItemIds.IRON_ORE,
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_IRON_ORE = register(
        BlockItemIds.DEEPSLATE_IRON_ORE,
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        BlockBehaviour.Properties.ofLegacyCopy(IRON_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block COAL_ORE = register(
        BlockItemIds.COAL_ORE,
        p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_COAL_ORE = register(
        BlockItemIds.DEEPSLATE_COAL_ORE,
        p -> new DropExperienceBlock(UniformInt.of(0, 2), p),
        BlockBehaviour.Properties.ofLegacyCopy(COAL_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block NETHER_GOLD_ORE = register(
        BlockItemIds.NETHER_GOLD_ORE,
        p -> new DropExperienceBlock(UniformInt.of(0, 1), p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F)
            .sound(SoundType.NETHER_GOLD_ORE)
    );
    public static final Block OAK_LOG = register(BlockItemIds.OAK_LOG, RotatedPillarBlock::new, logProperties(MapColor.WOOD, MapColor.PODZOL, SoundType.WOOD));
    public static final Block SPRUCE_LOG = register(
        BlockItemIds.SPRUCE_LOG, RotatedPillarBlock::new, logProperties(MapColor.PODZOL, MapColor.COLOR_BROWN, SoundType.WOOD)
    );
    public static final Block BIRCH_LOG = register(
        BlockItemIds.BIRCH_LOG, RotatedPillarBlock::new, logProperties(MapColor.SAND, MapColor.QUARTZ, SoundType.WOOD)
    );
    public static final Block JUNGLE_LOG = register(
        BlockItemIds.JUNGLE_LOG, RotatedPillarBlock::new, logProperties(MapColor.DIRT, MapColor.PODZOL, SoundType.WOOD)
    );
    public static final Block ACACIA_LOG = register(
        BlockItemIds.ACACIA_LOG, RotatedPillarBlock::new, logProperties(MapColor.COLOR_ORANGE, MapColor.STONE, SoundType.WOOD)
    );
    public static final Block CHERRY_LOG = register(
        BlockItemIds.CHERRY_LOG, RotatedPillarBlock::new, logProperties(MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_GRAY, SoundType.CHERRY_WOOD)
    );
    public static final Block DARK_OAK_LOG = register(
        BlockItemIds.DARK_OAK_LOG, RotatedPillarBlock::new, logProperties(MapColor.COLOR_BROWN, MapColor.COLOR_BROWN, SoundType.WOOD)
    );
    public static final Block PALE_OAK_LOG = register(
        BlockItemIds.PALE_OAK_LOG, RotatedPillarBlock::new, logProperties(PALE_OAK_PLANKS.defaultMapColor(), PALE_OAK_WOOD.defaultMapColor(), SoundType.WOOD)
    );
    public static final Block MANGROVE_LOG = register(
        BlockItemIds.MANGROVE_LOG, RotatedPillarBlock::new, logProperties(MapColor.COLOR_RED, MapColor.PODZOL, SoundType.WOOD)
    );
    public static final Block MANGROVE_ROOTS = register(
        BlockItemIds.MANGROVE_ROOTS,
        MangroveRootsBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PODZOL)
            .instrument(NoteBlockInstrument.BASS)
            .strength(0.7F)
            .sound(SoundType.MANGROVE_ROOTS)
            .noOcclusion()
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
            .noOcclusion()
            .ignitedByLava()
    );
    public static final Block MUDDY_MANGROVE_ROOTS = register(
        BlockItemIds.MUDDY_MANGROVE_ROOTS,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(0.7F).sound(SoundType.MUDDY_MANGROVE_ROOTS)
    );
    public static final Block BAMBOO_BLOCK = register(
        BlockItemIds.BAMBOO_BLOCK, RotatedPillarBlock::new, logProperties(MapColor.COLOR_YELLOW, MapColor.PLANT, SoundType.BAMBOO_WOOD)
    );
    public static final Block STRIPPED_SPRUCE_LOG = register(
        BlockItemIds.STRIPPED_SPRUCE_LOG, RotatedPillarBlock::new, logProperties(MapColor.PODZOL, MapColor.PODZOL, SoundType.WOOD)
    );
    public static final Block STRIPPED_BIRCH_LOG = register(
        BlockItemIds.STRIPPED_BIRCH_LOG, RotatedPillarBlock::new, logProperties(MapColor.SAND, MapColor.SAND, SoundType.WOOD)
    );
    public static final Block STRIPPED_JUNGLE_LOG = register(
        BlockItemIds.STRIPPED_JUNGLE_LOG, RotatedPillarBlock::new, logProperties(MapColor.DIRT, MapColor.DIRT, SoundType.WOOD)
    );
    public static final Block STRIPPED_ACACIA_LOG = register(
        BlockItemIds.STRIPPED_ACACIA_LOG, RotatedPillarBlock::new, logProperties(MapColor.COLOR_ORANGE, MapColor.COLOR_ORANGE, SoundType.WOOD)
    );
    public static final Block STRIPPED_CHERRY_LOG = register(
        BlockItemIds.STRIPPED_CHERRY_LOG, RotatedPillarBlock::new, logProperties(MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_PINK, SoundType.CHERRY_WOOD)
    );
    public static final Block STRIPPED_DARK_OAK_LOG = register(
        BlockItemIds.STRIPPED_DARK_OAK_LOG, RotatedPillarBlock::new, logProperties(MapColor.COLOR_BROWN, MapColor.COLOR_BROWN, SoundType.WOOD)
    );
    public static final Block STRIPPED_PALE_OAK_LOG = register(
        BlockItemIds.STRIPPED_PALE_OAK_LOG,
        RotatedPillarBlock::new,
        logProperties(PALE_OAK_PLANKS.defaultMapColor(), PALE_OAK_PLANKS.defaultMapColor(), SoundType.WOOD)
    );
    public static final Block STRIPPED_OAK_LOG = register(
        BlockItemIds.STRIPPED_OAK_LOG, RotatedPillarBlock::new, logProperties(MapColor.WOOD, MapColor.WOOD, SoundType.WOOD)
    );
    public static final Block STRIPPED_MANGROVE_LOG = register(
        BlockItemIds.STRIPPED_MANGROVE_LOG, RotatedPillarBlock::new, logProperties(MapColor.COLOR_RED, MapColor.COLOR_RED, SoundType.WOOD)
    );
    public static final Block STRIPPED_BAMBOO_BLOCK = register(
        BlockItemIds.STRIPPED_BAMBOO_BLOCK, RotatedPillarBlock::new, logProperties(MapColor.COLOR_YELLOW, MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD)
    );
    public static final Block OAK_WOOD = register(
        BlockItemIds.OAK_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block SPRUCE_WOOD = register(
        BlockItemIds.SPRUCE_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block BIRCH_WOOD = register(
        BlockItemIds.BIRCH_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block JUNGLE_WOOD = register(
        BlockItemIds.JUNGLE_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block ACACIA_WOOD = register(
        BlockItemIds.ACACIA_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block CHERRY_WOOD = register(
        BlockItemIds.CHERRY_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(SoundType.CHERRY_WOOD)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_WOOD = register(
        BlockItemIds.DARK_OAK_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block MANGROVE_WOOD = register(
        BlockItemIds.MANGROVE_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STRIPPED_OAK_WOOD = register(
        BlockItemIds.STRIPPED_OAK_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STRIPPED_SPRUCE_WOOD = register(
        BlockItemIds.STRIPPED_SPRUCE_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STRIPPED_BIRCH_WOOD = register(
        BlockItemIds.STRIPPED_BIRCH_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STRIPPED_JUNGLE_WOOD = register(
        BlockItemIds.STRIPPED_JUNGLE_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STRIPPED_ACACIA_WOOD = register(
        BlockItemIds.STRIPPED_ACACIA_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block STRIPPED_CHERRY_WOOD = register(
        BlockItemIds.STRIPPED_CHERRY_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_PINK)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(SoundType.CHERRY_WOOD)
            .ignitedByLava()
    );
    public static final Block STRIPPED_DARK_OAK_WOOD = register(
        BlockItemIds.STRIPPED_DARK_OAK_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STRIPPED_PALE_OAK_WOOD = register(
        BlockItemIds.STRIPPED_PALE_OAK_WOOD,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block STRIPPED_MANGROVE_WOOD = register(
        BlockItemIds.STRIPPED_MANGROVE_WOOD, RotatedPillarBlock::new, logProperties(MapColor.COLOR_RED, MapColor.COLOR_RED, SoundType.WOOD)
    );
    public static final Block OAK_LEAVES = register(BlockItemIds.OAK_LEAVES, p -> new TintedParticleLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS));
    public static final Block SPRUCE_LEAVES = register(
        BlockItemIds.SPRUCE_LEAVES, p -> new TintedParticleLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS)
    );
    public static final Block BIRCH_LEAVES = register(
        BlockItemIds.BIRCH_LEAVES, p -> new TintedParticleLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS)
    );
    public static final Block JUNGLE_LEAVES = register(
        BlockItemIds.JUNGLE_LEAVES, p -> new TintedParticleLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS)
    );
    public static final Block ACACIA_LEAVES = register(
        BlockItemIds.ACACIA_LEAVES, p -> new TintedParticleLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS)
    );
    public static final Block CHERRY_LEAVES = register(
        BlockItemIds.CHERRY_LEAVES,
        p -> new UntintedParticleLeavesBlock(0.1F, ParticleTypes.CHERRY_LEAVES, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .strength(0.2F)
            .randomTicks()
            .sound(SoundType.CHERRY_LEAVES)
            .noOcclusion()
            .isValidSpawn(Blocks::ocelotOrParrot)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block DARK_OAK_LEAVES = register(
        BlockItemIds.DARK_OAK_LEAVES, p -> new TintedParticleLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS)
    );
    public static final Block PALE_OAK_LEAVES = register(
        BlockItemIds.PALE_OAK_LEAVES,
        p -> new UntintedParticleLeavesBlock(0.02F, ParticleTypes.PALE_OAK_LEAVES, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(0.2F)
            .randomTicks()
            .sound(SoundType.GRASS)
            .noOcclusion()
            .isValidSpawn(Blocks::ocelotOrParrot)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block MANGROVE_LEAVES = register(
        BlockItemIds.MANGROVE_LEAVES, p -> new MangroveLeavesBlock(0.01F, p), leavesProperties(SoundType.GRASS)
    );
    public static final Block AZALEA_LEAVES = register(
        BlockItemIds.AZALEA_LEAVES,
        p -> new UntintedParticleLeavesBlock(0.01F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, -9399763), p),
        leavesProperties(SoundType.AZALEA_LEAVES)
    );
    public static final Block FLOWERING_AZALEA_LEAVES = register(
        BlockItemIds.FLOWERING_AZALEA_LEAVES,
        p -> new UntintedParticleLeavesBlock(0.01F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, -9399763), p),
        leavesProperties(SoundType.AZALEA_LEAVES)
    );
    public static final Block SPONGE = register(
        BlockItemIds.SPONGE, SpongeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.SPONGE)
    );
    public static final Block WET_SPONGE = register(
        BlockItemIds.WET_SPONGE, WetSpongeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.WET_SPONGE)
    );
    public static final Block GLASS = register(
        BlockItemIds.GLASS,
        TransparentBlock::new,
        BlockBehaviour.Properties.of()
            .instrument(NoteBlockInstrument.HAT)
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
    );
    public static final Block LAPIS_ORE = register(
        BlockItemIds.LAPIS_ORE,
        p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_LAPIS_ORE = register(
        BlockItemIds.DEEPSLATE_LAPIS_ORE,
        p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
        BlockBehaviour.Properties.ofLegacyCopy(LAPIS_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block LAPIS_BLOCK = register(
        BlockItemIds.LAPIS_BLOCK, BlockBehaviour.Properties.of().mapColor(MapColor.LAPIS).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DISPENSER = register(
        BlockItemIds.DISPENSER,
        DispenserBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F)
    );
    public static final Block SANDSTONE = register(
        BlockItemIds.SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block CHISELED_SANDSTONE = register(
        BlockItemIds.CHISELED_SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block CUT_SANDSTONE = register(
        BlockItemIds.CUT_SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block NOTE_BLOCK = register(
        BlockItemIds.NOTE_BLOCK,
        NoteBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava()
    );
    public static final ColorCollection<Block> BED = ColorCollection.registerBlocks(
        BlockItemIds.BED,
        Blocks::register,
        BedBlock::new,
        color -> BlockBehaviour.Properties.of()
            .mapColor(statex -> statex.getValue(BedBlock.PART) == BedPart.FOOT ? color.getMapColor() : MapColor.WOOL)
            .sound(SoundType.WOOD)
            .strength(0.2F)
            .bounceRestitution(0.75F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block POWERED_RAIL = register(
        BlockItemIds.POWERED_RAIL, PoweredRailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final Block DETECTOR_RAIL = register(
        BlockItemIds.DETECTOR_RAIL, DetectorRailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final Block STICKY_PISTON = register(BlockItemIds.STICKY_PISTON, p -> new PistonBaseBlock(true, p), pistonProperties());
    public static final Block COBWEB = register(
        BlockItemIds.COBWEB,
        WebBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL)
            .sound(SoundType.COBWEB)
            .forceSolidOn()
            .noCollision()
            .requiresCorrectToolForDrops()
            .strength(4.0F)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SHORT_GRASS = register(
        BlockItemIds.SHORT_GRASS,
        TallGrassBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XYZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block FERN = register(
        BlockItemIds.FERN,
        TallGrassBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XYZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block DEAD_BUSH = register(
        BlockItemIds.DEAD_BUSH,
        DryVegetationBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BUSH = register(
        BlockItemIds.BUSH,
        BushBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SHORT_DRY_GRASS = register(
        BlockItemIds.SHORT_DRY_GRASS,
        ShortDryGrassBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .ignitedByLava()
            .offsetType(BlockBehaviour.OffsetType.XYZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block TALL_DRY_GRASS = register(
        BlockItemIds.TALL_DRY_GRASS,
        TallDryGrassBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .ignitedByLava()
            .offsetType(BlockBehaviour.OffsetType.XYZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SEAGRASS = register(
        BlockItemIds.SEAGRASS,
        SeagrassBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block TALL_SEAGRASS = register(
        BlockIds.TALL_SEAGRASS,
        TallSeagrassBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PISTON = register(BlockItemIds.PISTON, p -> new PistonBaseBlock(false, p), pistonProperties());
    public static final Block PISTON_HEAD = register(
        BlockIds.PISTON_HEAD,
        PistonHeadBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F).noLootTable().pushReaction(PushReaction.BLOCK)
    );
    public static final ColorCollection<Block> WOOL = ColorCollection.registerBlocks(
        BlockItemIds.WOOL,
        Blocks::register,
        (var0, p) -> new Block(p),
        color -> BlockBehaviour.Properties.of()
            .mapColor(color.getMapColor())
            .instrument(NoteBlockInstrument.GUITAR)
            .strength(0.8F)
            .sound(SoundType.WOOL)
            .ignitedByLava()
    );
    public static final Block MOVING_PISTON = register(
        BlockIds.MOVING_PISTON,
        MovingPistonBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .forceSolidOn()
            .strength(-1.0F)
            .dynamicShape()
            .noLootTable()
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block DANDELION = register(
        BlockItemIds.DANDELION,
        p -> new FlowerBlock(MobEffects.SATURATION, 0.35F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block GOLDEN_DANDELION = register(
        BlockItemIds.GOLDEN_DANDELION,
        p -> new FlowerBlock(MobEffects.SATURATION, 0.35F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block TORCHFLOWER = register(
        BlockItemIds.TORCHFLOWER,
        p -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block POPPY = register(
        BlockItemIds.POPPY,
        p -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BLUE_ORCHID = register(
        BlockItemIds.BLUE_ORCHID,
        p -> new FlowerBlock(MobEffects.SATURATION, 0.35F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ALLIUM = register(
        BlockItemIds.ALLIUM,
        p -> new FlowerBlock(MobEffects.FIRE_RESISTANCE, 3.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block AZURE_BLUET = register(
        BlockItemIds.AZURE_BLUET,
        p -> new FlowerBlock(MobEffects.BLINDNESS, 11.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block RED_TULIP = register(
        BlockItemIds.RED_TULIP,
        p -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ORANGE_TULIP = register(
        BlockItemIds.ORANGE_TULIP,
        p -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block WHITE_TULIP = register(
        BlockItemIds.WHITE_TULIP,
        p -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PINK_TULIP = register(
        BlockItemIds.PINK_TULIP,
        p -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block OXEYE_DAISY = register(
        BlockItemIds.OXEYE_DAISY,
        p -> new FlowerBlock(MobEffects.REGENERATION, 7.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CORNFLOWER = register(
        BlockItemIds.CORNFLOWER,
        p -> new FlowerBlock(MobEffects.JUMP_BOOST, 5.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block WITHER_ROSE = register(
        BlockItemIds.WITHER_ROSE,
        p -> new WitherRoseBlock(MobEffects.WITHER, 7.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block LILY_OF_THE_VALLEY = register(
        BlockItemIds.LILY_OF_THE_VALLEY,
        p -> new FlowerBlock(MobEffects.POISON, 11.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BROWN_MUSHROOM = register(
        BlockItemIds.BROWN_MUSHROOM,
        p -> new MushroomBlock(TreeFeatures.HUGE_BROWN_MUSHROOM, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .lightLevel(statex -> 1)
            .postProcess(Blocks::postProcessSelf)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block RED_MUSHROOM = register(
        BlockItemIds.RED_MUSHROOM,
        p -> new MushroomBlock(TreeFeatures.HUGE_RED_MUSHROOM, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .postProcess(Blocks::postProcessSelf)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block GOLD_BLOCK = register(
        BlockItemIds.GOLD_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD)
            .instrument(NoteBlockInstrument.BELL)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.METAL)
    );
    public static final Block IRON_BLOCK = register(
        BlockItemIds.IRON_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
            .sound(SoundType.IRON)
    );
    public static final Block BRICKS = register(
        BlockItemIds.BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block TNT = register(
        BlockItemIds.TNT,
        TntBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).instabreak().sound(SoundType.GRASS).ignitedByLava().isRedstoneConductor(Blocks::never)
    );
    public static final Block BOOKSHELF = register(
        BlockItemIds.BOOKSHELF,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block CHISELED_BOOKSHELF = register(
        BlockItemIds.CHISELED_BOOKSHELF,
        ChiseledBookShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(1.5F)
            .sound(SoundType.CHISELED_BOOKSHELF)
            .ignitedByLava()
    );
    public static final Block ACACIA_SHELF = register(
        BlockItemIds.ACACIA_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(ACACIA_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block BAMBOO_SHELF = register(
        BlockItemIds.BAMBOO_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block BIRCH_SHELF = register(
        BlockItemIds.BIRCH_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(BIRCH_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block CHERRY_SHELF = register(
        BlockItemIds.CHERRY_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(CHERRY_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block CRIMSON_SHELF = register(
        BlockItemIds.CRIMSON_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.CRIMSON_STEM)
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block DARK_OAK_SHELF = register(
        BlockItemIds.DARK_OAK_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_LOG.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block JUNGLE_SHELF = register(
        BlockItemIds.JUNGLE_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_LOG.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block MANGROVE_SHELF = register(
        BlockItemIds.MANGROVE_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_LOG.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block OAK_SHELF = register(
        BlockItemIds.OAK_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block PALE_OAK_SHELF = register(
        BlockItemIds.PALE_OAK_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block SPRUCE_SHELF = register(
        BlockItemIds.SPRUCE_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_LOG.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block WARPED_SHELF = register(
        BlockItemIds.WARPED_SHELF,
        ShelfBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WARPED_STEM)
            .instrument(NoteBlockInstrument.BASS)
            .sound(SoundType.SHELF)
            .ignitedByLava()
            .strength(2.0F, 3.0F)
    );
    public static final Block MOSSY_COBBLESTONE = register(
        BlockItemIds.MOSSY_COBBLESTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block OBSIDIAN = register(
        BlockItemIds.OBSIDIAN,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(50.0F, 1200.0F)
    );
    public static final Block TORCH = register(
        BlockItemIds.TORCH,
        p -> new TorchBlock(ParticleTypes.FLAME, p),
        BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(statex -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block WALL_TORCH = register(
        BlockIds.WALL_TORCH,
        p -> new WallTorchBlock(ParticleTypes.FLAME, p),
        wallVariant(TORCH, true).noCollision().instabreak().lightLevel(statex -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block FIRE = register(
        BlockIds.FIRE,
        FireBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.FIRE)
            .replaceable()
            .noCollision()
            .instabreak()
            .lightLevel(statex -> 15)
            .sound(SoundType.WOOL)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SOUL_FIRE = register(
        BlockIds.SOUL_FIRE,
        SoulFireBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE)
            .replaceable()
            .noCollision()
            .instabreak()
            .lightLevel(statex -> 10)
            .sound(SoundType.WOOL)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SPAWNER = register(
        BlockItemIds.SPAWNER,
        SpawnerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(5.0F)
            .sound(SoundType.SPAWNER)
            .noOcclusion()
    );
    public static final Block CREAKING_HEART = register(
        BlockItemIds.CREAKING_HEART,
        CreakingHeartBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).strength(10.0F).sound(SoundType.CREAKING_HEART)
    );
    public static final Block OAK_STAIRS = registerLegacyStair(BlockItemIds.OAK_STAIRS, OAK_PLANKS);
    public static final Block CHEST = register(
        BlockItemIds.CHEST,
        p -> new ChestBlock(() -> BlockEntityTypes.CHEST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block REDSTONE_WIRE = register(
        BlockItemIds.REDSTONE_DUST, RedStoneWireBlock::new, BlockBehaviour.Properties.of().noCollision().instabreak().pushReaction(PushReaction.DESTROY)
    );
    public static final Block DIAMOND_ORE = register(
        BlockItemIds.DIAMOND_ORE,
        p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_DIAMOND_ORE = register(
        BlockItemIds.DEEPSLATE_DIAMOND_ORE,
        p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
        BlockBehaviour.Properties.ofLegacyCopy(DIAMOND_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block DIAMOND_BLOCK = register(
        BlockItemIds.DIAMOND_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)
    );
    public static final Block CRAFTING_TABLE = register(
        BlockItemIds.CRAFTING_TABLE,
        CraftingTableBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block WHEAT = register(
        BlockItemIds.WHEAT_CROP,
        CropBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(statex -> statex.getValue(CropBlock.AGE) >= 6 ? MapColor.COLOR_YELLOW : MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block FARMLAND = register(
        BlockItemIds.FARMLAND,
        FarmlandBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT)
            .randomTicks()
            .strength(0.6F)
            .sound(SoundType.GRAVEL)
            .isViewBlocking(Blocks::always)
            .isSuffocating(Blocks::always)
    );
    public static final Block FURNACE = register(
        BlockItemIds.FURNACE,
        FurnaceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.5F)
            .lightLevel(litBlockEmission(13))
    );
    public static final Block OAK_SIGN = register(
        BlockItemIds.OAK_SIGN,
        p -> new StandingSignBlock(WoodType.OAK, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
    );
    public static final Block SPRUCE_SIGN = register(
        BlockItemIds.SPRUCE_SIGN,
        p -> new StandingSignBlock(WoodType.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BIRCH_SIGN = register(
        BlockItemIds.BIRCH_SIGN,
        p -> new StandingSignBlock(WoodType.BIRCH, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
    );
    public static final Block ACACIA_SIGN = register(
        BlockItemIds.ACACIA_SIGN,
        p -> new StandingSignBlock(WoodType.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block CHERRY_SIGN = register(
        BlockItemIds.CHERRY_SIGN,
        p -> new StandingSignBlock(WoodType.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(CHERRY_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block JUNGLE_SIGN = register(
        BlockItemIds.JUNGLE_SIGN,
        p -> new StandingSignBlock(WoodType.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_SIGN = register(
        BlockItemIds.DARK_OAK_SIGN,
        p -> new StandingSignBlock(WoodType.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_SIGN = register(
        BlockItemIds.PALE_OAK_SIGN,
        p -> new StandingSignBlock(WoodType.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block MANGROVE_SIGN = register(
        BlockItemIds.MANGROVE_SIGN,
        p -> new StandingSignBlock(WoodType.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BAMBOO_SIGN = register(
        BlockItemIds.BAMBOO_SIGN,
        p -> new StandingSignBlock(WoodType.BAMBOO, p),
        BlockBehaviour.Properties.of()
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block OAK_DOOR = register(
        BlockItemIds.OAK_DOOR,
        p -> new DoorBlock(BlockSetType.OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block LADDER = register(
        BlockItemIds.LADDER,
        LadderBlock::new,
        BlockBehaviour.Properties.of().forceSolidOff().strength(0.4F).sound(SoundType.LADDER).noOcclusion().pushReaction(PushReaction.DESTROY)
    );
    public static final Block RAIL = register(
        BlockItemIds.RAIL, RailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final Block COBBLESTONE_STAIRS = registerLegacyStair(BlockItemIds.COBBLESTONE_STAIRS, COBBLESTONE);
    public static final Block OAK_WALL_SIGN = register(
        BlockIds.OAK_WALL_SIGN,
        p -> new WallSignBlock(WoodType.OAK, p),
        wallVariant(OAK_SIGN, true).mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
    );
    public static final Block SPRUCE_WALL_SIGN = register(
        BlockIds.SPRUCE_WALL_SIGN,
        p -> new WallSignBlock(WoodType.SPRUCE, p),
        wallVariant(SPRUCE_SIGN, true)
            .mapColor(SPRUCE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BIRCH_WALL_SIGN = register(
        BlockIds.BIRCH_WALL_SIGN,
        p -> new WallSignBlock(WoodType.BIRCH, p),
        wallVariant(BIRCH_SIGN, true).mapColor(MapColor.SAND).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
    );
    public static final Block ACACIA_WALL_SIGN = register(
        BlockIds.ACACIA_WALL_SIGN,
        p -> new WallSignBlock(WoodType.ACACIA, p),
        wallVariant(ACACIA_SIGN, true)
            .mapColor(MapColor.COLOR_ORANGE)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block CHERRY_WALL_SIGN = register(
        BlockIds.CHERRY_WALL_SIGN,
        p -> new WallSignBlock(WoodType.CHERRY, p),
        wallVariant(CHERRY_SIGN, true)
            .mapColor(CHERRY_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block JUNGLE_WALL_SIGN = register(
        BlockIds.JUNGLE_WALL_SIGN,
        p -> new WallSignBlock(WoodType.JUNGLE, p),
        wallVariant(JUNGLE_SIGN, true)
            .mapColor(JUNGLE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_WALL_SIGN = register(
        BlockIds.DARK_OAK_WALL_SIGN,
        p -> new WallSignBlock(WoodType.DARK_OAK, p),
        wallVariant(DARK_OAK_SIGN, true)
            .mapColor(DARK_OAK_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_WALL_SIGN = register(
        BlockIds.PALE_OAK_WALL_SIGN,
        p -> new WallSignBlock(WoodType.PALE_OAK, p),
        wallVariant(PALE_OAK_SIGN, true)
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block MANGROVE_WALL_SIGN = register(
        BlockIds.MANGROVE_WALL_SIGN,
        p -> new WallSignBlock(WoodType.MANGROVE, p),
        wallVariant(MANGROVE_SIGN, true)
            .mapColor(MANGROVE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BAMBOO_WALL_SIGN = register(
        BlockIds.BAMBOO_WALL_SIGN,
        p -> new WallSignBlock(WoodType.BAMBOO, p),
        wallVariant(BAMBOO_SIGN, true)
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block OAK_HANGING_SIGN = register(
        BlockItemIds.OAK_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(OAK_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block SPRUCE_HANGING_SIGN = register(
        BlockItemIds.SPRUCE_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BIRCH_HANGING_SIGN = register(
        BlockItemIds.BIRCH_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.BIRCH, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
    );
    public static final Block ACACIA_HANGING_SIGN = register(
        BlockItemIds.ACACIA_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block CHERRY_HANGING_SIGN = register(
        BlockItemIds.CHERRY_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_PINK)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block JUNGLE_HANGING_SIGN = register(
        BlockItemIds.JUNGLE_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_HANGING_SIGN = register(
        BlockItemIds.DARK_OAK_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_HANGING_SIGN = register(
        BlockItemIds.PALE_OAK_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block CRIMSON_HANGING_SIGN = register(
        BlockItemIds.CRIMSON_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.CRIMSON, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
    );
    public static final Block WARPED_HANGING_SIGN = register(
        BlockItemIds.WARPED_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.WARPED, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
    );
    public static final Block MANGROVE_HANGING_SIGN = register(
        BlockItemIds.MANGROVE_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BAMBOO_HANGING_SIGN = register(
        BlockItemIds.BAMBOO_HANGING_SIGN,
        p -> new CeilingHangingSignBlock(WoodType.BAMBOO, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block OAK_WALL_HANGING_SIGN = register(
        BlockIds.OAK_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.OAK, p),
        wallVariant(OAK_HANGING_SIGN, true)
            .mapColor(OAK_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block SPRUCE_WALL_HANGING_SIGN = register(
        BlockIds.SPRUCE_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.SPRUCE, p),
        wallVariant(SPRUCE_HANGING_SIGN, true)
            .mapColor(MapColor.WOOD)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block BIRCH_WALL_HANGING_SIGN = register(
        BlockIds.BIRCH_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.BIRCH, p),
        wallVariant(BIRCH_HANGING_SIGN, true)
            .mapColor(MapColor.SAND)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block ACACIA_WALL_HANGING_SIGN = register(
        BlockIds.ACACIA_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.ACACIA, p),
        wallVariant(ACACIA_HANGING_SIGN, true)
            .mapColor(MapColor.COLOR_ORANGE)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block CHERRY_WALL_HANGING_SIGN = register(
        BlockIds.CHERRY_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.CHERRY, p),
        wallVariant(CHERRY_HANGING_SIGN, true)
            .mapColor(MapColor.TERRACOTTA_PINK)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block JUNGLE_WALL_HANGING_SIGN = register(
        BlockIds.JUNGLE_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.JUNGLE, p),
        wallVariant(JUNGLE_HANGING_SIGN, true)
            .mapColor(JUNGLE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_WALL_HANGING_SIGN = register(
        BlockIds.DARK_OAK_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.DARK_OAK, p),
        wallVariant(DARK_OAK_HANGING_SIGN, true)
            .mapColor(DARK_OAK_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_WALL_HANGING_SIGN = register(
        BlockIds.PALE_OAK_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.PALE_OAK, p),
        wallVariant(PALE_OAK_HANGING_SIGN, true)
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block MANGROVE_WALL_HANGING_SIGN = register(
        BlockIds.MANGROVE_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.MANGROVE, p),
        wallVariant(MANGROVE_HANGING_SIGN, true)
            .mapColor(MANGROVE_LOG.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block CRIMSON_WALL_HANGING_SIGN = register(
        BlockIds.CRIMSON_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.CRIMSON, p),
        wallVariant(CRIMSON_HANGING_SIGN, true)
            .mapColor(MapColor.CRIMSON_STEM)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
    );
    public static final Block WARPED_WALL_HANGING_SIGN = register(
        BlockIds.WARPED_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.WARPED, p),
        wallVariant(WARPED_HANGING_SIGN, true).mapColor(MapColor.WARPED_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
    );
    public static final Block BAMBOO_WALL_HANGING_SIGN = register(
        BlockIds.BAMBOO_WALL_HANGING_SIGN,
        p -> new WallHangingSignBlock(WoodType.BAMBOO, p),
        wallVariant(BAMBOO_HANGING_SIGN, true)
            .mapColor(MapColor.COLOR_YELLOW)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .ignitedByLava()
    );
    public static final Block LEVER = register(
        BlockItemIds.LEVER,
        LeverBlock::new,
        BlockBehaviour.Properties.of().noCollision().strength(0.5F).sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
    );
    public static final Block STONE_PRESSURE_PLATE = register(
        BlockItemIds.STONE_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.STONE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .noCollision()
            .strength(0.5F)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block IRON_DOOR = register(
        BlockItemIds.IRON_DOOR,
        p -> new DoorBlock(BlockSetType.IRON, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F).noOcclusion().pushReaction(PushReaction.DESTROY)
    );
    public static final Block OAK_PRESSURE_PLATE = register(
        BlockItemIds.OAK_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SPRUCE_PRESSURE_PLATE = register(
        BlockItemIds.SPRUCE_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BIRCH_PRESSURE_PLATE = register(
        BlockItemIds.BIRCH_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.BIRCH, p),
        BlockBehaviour.Properties.of()
            .mapColor(BIRCH_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block JUNGLE_PRESSURE_PLATE = register(
        BlockItemIds.JUNGLE_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ACACIA_PRESSURE_PLATE = register(
        BlockItemIds.ACACIA_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(ACACIA_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CHERRY_PRESSURE_PLATE = register(
        BlockItemIds.CHERRY_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(CHERRY_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block DARK_OAK_PRESSURE_PLATE = register(
        BlockItemIds.DARK_OAK_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PALE_OAK_PRESSURE_PLATE = register(
        BlockItemIds.PALE_OAK_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MANGROVE_PRESSURE_PLATE = register(
        BlockItemIds.MANGROVE_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BAMBOO_PRESSURE_PLATE = register(
        BlockItemIds.BAMBOO_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.BAMBOO, p),
        BlockBehaviour.Properties.of()
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block REDSTONE_ORE = register(
        BlockItemIds.REDSTONE_ORE,
        RedStoneOreBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .randomTicks()
            .lightLevel(litBlockEmission(9))
            .strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_REDSTONE_ORE = register(
        BlockItemIds.DEEPSLATE_REDSTONE_ORE,
        RedStoneOreBlock::new,
        BlockBehaviour.Properties.ofLegacyCopy(REDSTONE_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block REDSTONE_TORCH = register(
        BlockItemIds.REDSTONE_TORCH,
        RedstoneTorchBlock::new,
        BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(litBlockEmission(7)).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block REDSTONE_WALL_TORCH = register(
        BlockIds.REDSTONE_WALL_TORCH,
        RedstoneWallTorchBlock::new,
        wallVariant(REDSTONE_TORCH, true).noCollision().instabreak().lightLevel(litBlockEmission(7)).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block STONE_BUTTON = register(BlockItemIds.STONE_BUTTON, p -> new ButtonBlock(BlockSetType.STONE, 20, p), buttonProperties());
    public static final Block SNOW = register(
        BlockItemIds.SNOW,
        SnowLayerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW)
            .replaceable()
            .forceSolidOff()
            .randomTicks()
            .strength(0.1F)
            .requiresCorrectToolForDrops()
            .sound(SoundType.SNOW)
            .isViewBlocking((statex, level, pos) -> statex.getValue(SnowLayerBlock.LAYERS) >= 8)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ICE = register(
        BlockItemIds.ICE,
        IceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.ICE)
            .friction(0.98F)
            .randomTicks()
            .strength(0.5F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isValidSpawn((statex, blockGetter, blockPos, entityType) -> entityType == EntityTypes.POLAR_BEAR)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block SNOW_BLOCK = register(
        BlockItemIds.SNOW_BLOCK, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).requiresCorrectToolForDrops().strength(0.2F).sound(SoundType.SNOW)
    );
    public static final Block CACTUS = register(
        BlockItemIds.CACTUS,
        CactusBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().strength(0.4F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
    );
    public static final Block CACTUS_FLOWER = register(
        BlockItemIds.CACTUS_FLOWER,
        CactusFlowerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .noCollision()
            .instabreak()
            .ignitedByLava()
            .sound(SoundType.CACTUS_FLOWER)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CLAY = register(
        BlockItemIds.CLAY, BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).instrument(NoteBlockInstrument.FLUTE).strength(0.6F).sound(SoundType.GRAVEL)
    );
    public static final Block SUGAR_CANE = register(
        BlockItemIds.SUGAR_CANE,
        SugarCaneBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block JUKEBOX = register(
        BlockItemIds.JUKEBOX,
        JukeboxBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F, 6.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block OAK_FENCE = register(
        BlockItemIds.OAK_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block NETHERRACK = register(
        BlockItemIds.NETHERRACK,
        NetherrackBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(0.4F)
            .sound(SoundType.NETHERRACK)
    );
    public static final Block SOUL_SAND = register(
        BlockItemIds.SOUL_SAND,
        SoulSandBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .instrument(NoteBlockInstrument.COW_BELL)
            .strength(0.5F)
            .speedFactor(0.4F)
            .sound(SoundType.SOUL_SAND)
            .isValidSpawn(Blocks::always)
            .isRedstoneConductor(Blocks::always)
            .isViewBlocking(Blocks::always)
            .isSuffocating(Blocks::always)
            .postProcess(Blocks::postProcessAbove)
    );
    public static final Block SOUL_SOIL = register(
        BlockItemIds.SOUL_SOIL, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.SOUL_SOIL)
    );
    public static final Block BASALT = register(
        BlockItemIds.BASALT,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.25F, 4.2F)
            .sound(SoundType.BASALT)
    );
    public static final Block POLISHED_BASALT = register(
        BlockItemIds.POLISHED_BASALT,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.25F, 4.2F)
            .sound(SoundType.BASALT)
    );
    public static final Block SOUL_TORCH = register(
        BlockItemIds.SOUL_TORCH,
        p -> new TorchBlock(ParticleTypes.SOUL_FIRE_FLAME, p),
        BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(statex -> 10).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block SOUL_WALL_TORCH = register(
        BlockIds.SOUL_WALL_TORCH,
        p -> new WallTorchBlock(ParticleTypes.SOUL_FIRE_FLAME, p),
        wallVariant(SOUL_TORCH, true).noCollision().instabreak().lightLevel(statex -> 10).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block COPPER_TORCH = register(
        BlockItemIds.COPPER_TORCH,
        p -> new TorchBlock(ParticleTypes.COPPER_FIRE_FLAME, p),
        BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(statex -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block COPPER_WALL_TORCH = register(
        BlockIds.COPPER_WALL_TORCH,
        p -> new WallTorchBlock(ParticleTypes.COPPER_FIRE_FLAME, p),
        wallVariant(COPPER_TORCH, true).noCollision().instabreak().lightLevel(statex -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block GLOWSTONE = register(
        BlockItemIds.GLOWSTONE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .instrument(NoteBlockInstrument.PLING)
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .lightLevel(statex -> 15)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block NETHER_PORTAL = register(
        BlockIds.NETHER_PORTAL,
        NetherPortalBlock::new,
        BlockBehaviour.Properties.of()
            .noCollision()
            .randomTicks()
            .strength(-1.0F)
            .sound(SoundType.GLASS)
            .lightLevel(statex -> 11)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block CARVED_PUMPKIN = register(
        BlockItemIds.CARVED_PUMPKIN,
        CarvedPumpkinBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(1.0F)
            .sound(SoundType.WOOD)
            .isValidSpawn(Blocks::always)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block JACK_O_LANTERN = register(
        BlockItemIds.JACK_O_LANTERN,
        CarvedPumpkinBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(1.0F)
            .sound(SoundType.WOOD)
            .lightLevel(statex -> 15)
            .isValidSpawn(Blocks::always)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CAKE = register(
        BlockItemIds.CAKE,
        CakeBlock::new,
        BlockBehaviour.Properties.of().forceSolidOn().strength(0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
    );
    public static final Block REPEATER = register(
        BlockItemIds.REPEATER, RepeaterBlock::new, BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
    );
    public static final ColorCollection<Block> STAINED_GLASS = ColorCollection.registerBlocks(
        BlockItemIds.STAINED_GLASS,
        Blocks::register,
        StainedGlassBlock::new,
        color -> BlockBehaviour.Properties.of()
            .mapColor(color.getMapColor())
            .instrument(NoteBlockInstrument.HAT)
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
    );
    public static final Block OAK_TRAPDOOR = register(
        BlockItemIds.OAK_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block SPRUCE_TRAPDOOR = register(
        BlockItemIds.SPRUCE_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PODZOL)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block BIRCH_TRAPDOOR = register(
        BlockItemIds.BIRCH_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.BIRCH, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block JUNGLE_TRAPDOOR = register(
        BlockItemIds.JUNGLE_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block ACACIA_TRAPDOOR = register(
        BlockItemIds.ACACIA_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block CHERRY_TRAPDOOR = register(
        BlockItemIds.CHERRY_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_TRAPDOOR = register(
        BlockItemIds.DARK_OAK_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_TRAPDOOR = register(
        BlockItemIds.PALE_OAK_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block MANGROVE_TRAPDOOR = register(
        BlockItemIds.MANGROVE_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block BAMBOO_TRAPDOOR = register(
        BlockItemIds.BAMBOO_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.BAMBOO, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .ignitedByLava()
    );
    public static final Block STONE_BRICKS = register(
        BlockItemIds.STONE_BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block MOSSY_STONE_BRICKS = register(
        BlockItemIds.MOSSY_STONE_BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block CRACKED_STONE_BRICKS = register(
        BlockItemIds.CRACKED_STONE_BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block CHISELED_STONE_BRICKS = register(
        BlockItemIds.CHISELED_STONE_BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block PACKED_MUD = register(
        BlockItemIds.PACKED_MUD, BlockBehaviour.Properties.ofLegacyCopy(DIRT).strength(1.0F, 3.0F).sound(SoundType.PACKED_MUD)
    );
    public static final Block MUD_BRICKS = register(
        BlockItemIds.MUD_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 3.0F)
            .sound(SoundType.MUD_BRICKS)
    );
    public static final Block INFESTED_STONE = register(
        BlockItemIds.INFESTED_STONE, p -> new InfestedBlock(STONE, p), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
    );
    public static final Block INFESTED_COBBLESTONE = register(
        BlockItemIds.INFESTED_COBBLESTONE, p -> new InfestedBlock(COBBLESTONE, p), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
    );
    public static final Block INFESTED_STONE_BRICKS = register(
        BlockItemIds.INFESTED_STONE_BRICKS, p -> new InfestedBlock(STONE_BRICKS, p), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
    );
    public static final Block INFESTED_MOSSY_STONE_BRICKS = register(
        BlockItemIds.INFESTED_MOSSY_STONE_BRICKS, p -> new InfestedBlock(MOSSY_STONE_BRICKS, p), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
    );
    public static final Block INFESTED_CRACKED_STONE_BRICKS = register(
        BlockItemIds.INFESTED_CRACKED_STONE_BRICKS, p -> new InfestedBlock(CRACKED_STONE_BRICKS, p), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
    );
    public static final Block INFESTED_CHISELED_STONE_BRICKS = register(
        BlockItemIds.INFESTED_CHISELED_STONE_BRICKS, p -> new InfestedBlock(CHISELED_STONE_BRICKS, p), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
    );
    public static final Block BROWN_MUSHROOM_BLOCK = register(
        BlockItemIds.BROWN_MUSHROOM_BLOCK,
        HugeMushroomBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block RED_MUSHROOM_BLOCK = register(
        BlockItemIds.RED_MUSHROOM_BLOCK,
        HugeMushroomBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block MUSHROOM_STEM = register(
        BlockItemIds.MUSHROOM_STEM,
        HugeMushroomBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block IRON_BARS = register(
        BlockItemIds.IRON_BARS,
        IronBarsBlock::new,
        BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON).noOcclusion()
    );
    public static final WeatheringCopperCollection<Block> COPPER_BARS = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_BARS,
        Blocks::register,
        (s, p) -> new IronBarsBlock(p),
        WeatheringCopperBarsBlock::new,
        p -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()
    );
    public static final Block IRON_CHAIN = register(
        BlockItemIds.IRON_CHAIN,
        ChainBlock::new,
        BlockBehaviour.Properties.of().forceSolidOn().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.CHAIN).noOcclusion()
    );
    public static final WeatheringCopperCollection<Block> COPPER_CHAIN = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_CHAIN,
        Blocks::register,
        (s, p) -> new ChainBlock(p),
        WeatheringCopperChainBlock::new,
        p -> BlockBehaviour.Properties.of().forceSolidOn().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.CHAIN).noOcclusion()
    );
    public static final Block GLASS_PANE = register(
        BlockItemIds.GLASS_PANE,
        IronBarsBlock::new,
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
    );
    public static final Block PUMPKIN = register(
        BlockItemIds.PUMPKIN,
        PumpkinBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.DIDGERIDOO)
            .strength(1.0F)
            .sound(SoundType.WOOD)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MELON = register(
        BlockItemIds.MELON,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0F).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block ATTACHED_PUMPKIN_STEM = register(
        BlockIds.ATTACHED_PUMPKIN_STEM,
        p -> new AttachedStemBlock(
            BlockItemIds.PUMPKIN_CROP.block(), BlockItemIds.PUMPKIN.block(), BlockItemIds.PUMPKIN_CROP.item(), BlockTags.SUPPORTS_PUMPKIN_STEM, p
        ),
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block ATTACHED_MELON_STEM = register(
        BlockIds.ATTACHED_MELON_STEM,
        p -> new AttachedStemBlock(
            BlockItemIds.MELON_CROP.block(), BlockItemIds.MELON.block(), BlockItemIds.MELON_CROP.item(), BlockTags.SUPPORTS_MELON_STEM, p
        ),
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block PUMPKIN_STEM = register(
        BlockItemIds.PUMPKIN_CROP,
        p -> new StemBlock(
            BlockItemIds.PUMPKIN.block(),
            BlockIds.ATTACHED_PUMPKIN_STEM,
            BlockItemIds.PUMPKIN_CROP.item(),
            BlockTags.SUPPORTS_PUMPKIN_STEM,
            BlockTags.SUPPORTS_PUMPKIN_STEM_FRUIT,
            p
        ),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.HARD_CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MELON_STEM = register(
        BlockItemIds.MELON_CROP,
        p -> new StemBlock(
            BlockItemIds.MELON.block(),
            BlockIds.ATTACHED_MELON_STEM,
            BlockItemIds.MELON_CROP.item(),
            BlockTags.SUPPORTS_MELON_STEM,
            BlockTags.SUPPORTS_MELON_STEM_FRUIT,
            p
        ),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.HARD_CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block VINE = register(
        BlockItemIds.VINE,
        VineBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .randomTicks()
            .strength(0.2F)
            .sound(SoundType.VINE)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block GLOW_LICHEN = register(
        BlockItemIds.GLOW_LICHEN,
        GlowLichenBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.GLOW_LICHEN)
            .replaceable()
            .noCollision()
            .strength(0.2F)
            .sound(SoundType.GLOW_LICHEN)
            .lightLevel(GlowLichenBlock.emission(7))
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block RESIN_CLUMP = register(
        BlockItemIds.RESIN_CLUMP,
        MultifaceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .replaceable()
            .noCollision()
            .sound(SoundType.RESIN)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block OAK_FENCE_GATE = register(
        BlockItemIds.OAK_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block BRICK_STAIRS = registerLegacyStair(BlockItemIds.BRICK_STAIRS, BRICKS);
    public static final Block STONE_BRICK_STAIRS = registerLegacyStair(BlockItemIds.STONE_BRICK_STAIRS, STONE_BRICKS);
    public static final Block MUD_BRICK_STAIRS = registerLegacyStair(BlockItemIds.MUD_BRICK_STAIRS, MUD_BRICKS);
    public static final Block MYCELIUM = register(
        BlockItemIds.MYCELIUM,
        MyceliumBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).randomTicks().strength(0.6F).sound(SoundType.GRASS)
    );
    public static final Block LILY_PAD = register(
        BlockItemIds.LILY_PAD,
        LilyPadBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion().pushReaction(PushReaction.DESTROY)
    );
    public static final Block RESIN_BLOCK = register(
        BlockItemIds.RESIN_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.RESIN)
    );
    public static final Block RESIN_BRICKS = register(
        BlockItemIds.RESIN_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .sound(SoundType.RESIN_BRICKS)
            .strength(1.5F, 6.0F)
    );
    public static final Block RESIN_BRICK_STAIRS = registerLegacyStair(BlockItemIds.RESIN_BRICK_STAIRS, RESIN_BRICKS);
    public static final Block RESIN_BRICK_SLAB = register(
        BlockItemIds.RESIN_BRICK_SLAB,
        p -> new SlabBlock(p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .sound(SoundType.RESIN_BRICKS)
            .strength(1.5F, 6.0F)
    );
    public static final Block RESIN_BRICK_WALL = register(
        BlockItemIds.RESIN_BRICK_WALL,
        p -> new WallBlock(p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .sound(SoundType.RESIN_BRICKS)
            .strength(1.5F, 6.0F)
    );
    public static final Block CHISELED_RESIN_BRICKS = register(
        BlockItemIds.CHISELED_RESIN_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .sound(SoundType.RESIN_BRICKS)
            .strength(1.5F, 6.0F)
    );
    public static final Block NETHER_BRICKS = register(
        BlockItemIds.NETHER_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.NETHER_BRICKS)
    );
    public static final Block NETHER_BRICK_FENCE = register(
        BlockItemIds.NETHER_BRICK_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.NETHER_BRICKS)
    );
    public static final Block NETHER_BRICK_STAIRS = registerLegacyStair(BlockItemIds.NETHER_BRICK_STAIRS, NETHER_BRICKS);
    public static final Block NETHER_WART = register(
        BlockItemIds.NETHER_WART,
        NetherWartBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().randomTicks().sound(SoundType.NETHER_WART).pushReaction(PushReaction.DESTROY)
    );
    public static final Block ENCHANTING_TABLE = register(
        BlockItemIds.ENCHANTING_TABLE,
        EnchantingTableBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .lightLevel(statex -> 7)
            .strength(5.0F, 1200.0F)
    );
    public static final Block BREWING_STAND = register(
        BlockItemIds.BREWING_STAND,
        BrewingStandBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).lightLevel(statex -> 1).noOcclusion()
    );
    public static final Block CAULDRON = register(
        BlockItemIds.CAULDRON,
        CauldronBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F).noOcclusion()
    );
    public static final Block WATER_CAULDRON = register(
        BlockIds.WATER_CAULDRON,
        p -> new LayeredCauldronBlock(Biome.Precipitation.RAIN, CauldronInteractions.WATER, p),
        BlockBehaviour.Properties.ofLegacyCopy(CAULDRON)
    );
    public static final Block LAVA_CAULDRON = register(
        BlockIds.LAVA_CAULDRON, LavaCauldronBlock::new, BlockBehaviour.Properties.ofLegacyCopy(CAULDRON).lightLevel(statex -> 15)
    );
    public static final Block POWDER_SNOW_CAULDRON = register(
        BlockIds.POWDER_SNOW_CAULDRON,
        p -> new LayeredCauldronBlock(Biome.Precipitation.SNOW, CauldronInteractions.POWDER_SNOW, p),
        BlockBehaviour.Properties.ofLegacyCopy(CAULDRON)
    );
    public static final Block END_PORTAL = register(
        BlockIds.END_PORTAL,
        EndPortalBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .noCollision()
            .lightLevel(statex -> 15)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block END_PORTAL_FRAME = register(
        BlockItemIds.END_PORTAL_FRAME,
        EndPortalFrameBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.GLASS)
            .lightLevel(statex -> 1)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
    );
    public static final Block END_STONE = register(
        BlockItemIds.END_STONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 9.0F)
    );
    public static final Block DRAGON_EGG = register(
        BlockItemIds.DRAGON_EGG,
        DragonEggBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(3.0F, 9.0F)
            .lightLevel(statex -> 1)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block REDSTONE_LAMP = register(
        BlockItemIds.REDSTONE_LAMP,
        RedstoneLampBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE)
            .lightLevel(litBlockEmission(15))
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .isValidSpawn(Blocks::always)
    );
    public static final Block COCOA = register(
        BlockItemIds.COCOA_CROP,
        CocoaBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .randomTicks()
            .strength(0.2F, 3.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SANDSTONE_STAIRS = registerLegacyStair(BlockItemIds.SANDSTONE_STAIRS, SANDSTONE);
    public static final Block EMERALD_ORE = register(
        BlockItemIds.EMERALD_ORE,
        p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
    );
    public static final Block DEEPSLATE_EMERALD_ORE = register(
        BlockItemIds.DEEPSLATE_EMERALD_ORE,
        p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
        BlockBehaviour.Properties.ofLegacyCopy(EMERALD_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final Block ENDER_CHEST = register(
        BlockItemIds.ENDER_CHEST,
        EnderChestBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(22.5F, 600.0F).lightLevel(statex -> 7)
    );
    public static final Block TRIPWIRE_HOOK = register(
        BlockItemIds.TRIPWIRE_HOOK,
        TripWireHookBlock::new,
        BlockBehaviour.Properties.of().noCollision().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
    );
    public static final Block TRIPWIRE = register(
        BlockItemIds.TRIPWIRE, p -> new TripWireBlock(TRIPWIRE_HOOK, p), BlockBehaviour.Properties.of().noCollision().pushReaction(PushReaction.DESTROY)
    );
    public static final Block EMERALD_BLOCK = register(
        BlockItemIds.EMERALD_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.EMERALD)
            .instrument(NoteBlockInstrument.BIT)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
    );
    public static final Block SPRUCE_STAIRS = registerLegacyStair(BlockItemIds.SPRUCE_STAIRS, SPRUCE_PLANKS);
    public static final Block BIRCH_STAIRS = registerLegacyStair(BlockItemIds.BIRCH_STAIRS, BIRCH_PLANKS);
    public static final Block JUNGLE_STAIRS = registerLegacyStair(BlockItemIds.JUNGLE_STAIRS, JUNGLE_PLANKS);
    public static final Block COMMAND_BLOCK = register(
        BlockItemIds.COMMAND_BLOCK,
        p -> new CommandBlock(false, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
    );
    public static final Block BEACON = register(
        BlockItemIds.BEACON,
        BeaconBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIAMOND)
            .instrument(NoteBlockInstrument.HAT)
            .strength(3.0F)
            .lightLevel(statex -> 15)
            .noOcclusion()
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block COBBLESTONE_WALL = register(
        BlockItemIds.COBBLESTONE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLESTONE).forceSolidOn()
    );
    public static final Block MOSSY_COBBLESTONE_WALL = register(
        BlockItemIds.MOSSY_COBBLESTONE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLESTONE).forceSolidOn()
    );
    public static final Block FLOWER_POT = register(BlockItemIds.FLOWER_POT, p -> new FlowerPotBlock(AIR, p), flowerPotProperties());
    public static final Block POTTED_TORCHFLOWER = register(BlockIds.POTTED_TORCHFLOWER, p -> new FlowerPotBlock(TORCHFLOWER, p), flowerPotProperties());
    public static final Block POTTED_OAK_SAPLING = register(BlockIds.POTTED_OAK_SAPLING, p -> new FlowerPotBlock(OAK_SAPLING, p), flowerPotProperties());
    public static final Block POTTED_SPRUCE_SAPLING = register(
        BlockIds.POTTED_SPRUCE_SAPLING, p -> new FlowerPotBlock(SPRUCE_SAPLING, p), flowerPotProperties()
    );
    public static final Block POTTED_BIRCH_SAPLING = register(BlockIds.POTTED_BIRCH_SAPLING, p -> new FlowerPotBlock(BIRCH_SAPLING, p), flowerPotProperties());
    public static final Block POTTED_JUNGLE_SAPLING = register(
        BlockIds.POTTED_JUNGLE_SAPLING, p -> new FlowerPotBlock(JUNGLE_SAPLING, p), flowerPotProperties()
    );
    public static final Block POTTED_ACACIA_SAPLING = register(
        BlockIds.POTTED_ACACIA_SAPLING, p -> new FlowerPotBlock(ACACIA_SAPLING, p), flowerPotProperties()
    );
    public static final Block POTTED_CHERRY_SAPLING = register(
        BlockIds.POTTED_CHERRY_SAPLING, p -> new FlowerPotBlock(CHERRY_SAPLING, p), flowerPotProperties()
    );
    public static final Block POTTED_DARK_OAK_SAPLING = register(
        BlockIds.POTTED_DARK_OAK_SAPLING, p -> new FlowerPotBlock(DARK_OAK_SAPLING, p), flowerPotProperties()
    );
    public static final Block POTTED_PALE_OAK_SAPLING = register(
        BlockIds.POTTED_PALE_OAK_SAPLING, p -> new FlowerPotBlock(PALE_OAK_SAPLING, p), flowerPotProperties()
    );
    public static final Block POTTED_MANGROVE_PROPAGULE = register(
        BlockIds.POTTED_MANGROVE_PROPAGULE, p -> new FlowerPotBlock(MANGROVE_PROPAGULE, p), flowerPotProperties()
    );
    public static final Block POTTED_FERN = register(BlockIds.POTTED_FERN, p -> new FlowerPotBlock(FERN, p), flowerPotProperties());
    public static final Block POTTED_DANDELION = register(BlockIds.POTTED_DANDELION, p -> new FlowerPotBlock(DANDELION, p), flowerPotProperties());
    public static final Block POTTED_GOLDEN_DANDELION = register(
        BlockIds.POTTED_GOLDEN_DANDELION, p -> new FlowerPotBlock(GOLDEN_DANDELION, p), flowerPotProperties()
    );
    public static final Block POTTED_POPPY = register(BlockIds.POTTED_POPPY, p -> new FlowerPotBlock(POPPY, p), flowerPotProperties());
    public static final Block POTTED_BLUE_ORCHID = register(BlockIds.POTTED_BLUE_ORCHID, p -> new FlowerPotBlock(BLUE_ORCHID, p), flowerPotProperties());
    public static final Block POTTED_ALLIUM = register(BlockIds.POTTED_ALLIUM, p -> new FlowerPotBlock(ALLIUM, p), flowerPotProperties());
    public static final Block POTTED_AZURE_BLUET = register(BlockIds.POTTED_AZURE_BLUET, p -> new FlowerPotBlock(AZURE_BLUET, p), flowerPotProperties());
    public static final Block POTTED_RED_TULIP = register(BlockIds.POTTED_RED_TULIP, p -> new FlowerPotBlock(RED_TULIP, p), flowerPotProperties());
    public static final Block POTTED_ORANGE_TULIP = register(BlockIds.POTTED_ORANGE_TULIP, p -> new FlowerPotBlock(ORANGE_TULIP, p), flowerPotProperties());
    public static final Block POTTED_WHITE_TULIP = register(BlockIds.POTTED_WHITE_TULIP, p -> new FlowerPotBlock(WHITE_TULIP, p), flowerPotProperties());
    public static final Block POTTED_PINK_TULIP = register(BlockIds.POTTED_PINK_TULIP, p -> new FlowerPotBlock(PINK_TULIP, p), flowerPotProperties());
    public static final Block POTTED_OXEYE_DAISY = register(BlockIds.POTTED_OXEYE_DAISY, p -> new FlowerPotBlock(OXEYE_DAISY, p), flowerPotProperties());
    public static final Block POTTED_CORNFLOWER = register(BlockIds.POTTED_CORNFLOWER, p -> new FlowerPotBlock(CORNFLOWER, p), flowerPotProperties());
    public static final Block POTTED_LILY_OF_THE_VALLEY = register(
        BlockIds.POTTED_LILY_OF_THE_VALLEY, p -> new FlowerPotBlock(LILY_OF_THE_VALLEY, p), flowerPotProperties()
    );
    public static final Block POTTED_WITHER_ROSE = register(BlockIds.POTTED_WITHER_ROSE, p -> new FlowerPotBlock(WITHER_ROSE, p), flowerPotProperties());
    public static final Block POTTED_RED_MUSHROOM = register(BlockIds.POTTED_RED_MUSHROOM, p -> new FlowerPotBlock(RED_MUSHROOM, p), flowerPotProperties());
    public static final Block POTTED_BROWN_MUSHROOM = register(
        BlockIds.POTTED_BROWN_MUSHROOM, p -> new FlowerPotBlock(BROWN_MUSHROOM, p), flowerPotProperties()
    );
    public static final Block POTTED_DEAD_BUSH = register(BlockIds.POTTED_DEAD_BUSH, p -> new FlowerPotBlock(DEAD_BUSH, p), flowerPotProperties());
    public static final Block POTTED_CACTUS = register(BlockIds.POTTED_CACTUS, p -> new FlowerPotBlock(CACTUS, p), flowerPotProperties());
    public static final Block CARROTS = register(
        BlockItemIds.CARROT_CROP,
        CarrotBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block POTATOES = register(
        BlockItemIds.POTATO_CROP,
        PotatoBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block OAK_BUTTON = register(BlockItemIds.OAK_BUTTON, p -> new ButtonBlock(BlockSetType.OAK, 30, p), buttonProperties());
    public static final Block SPRUCE_BUTTON = register(BlockItemIds.SPRUCE_BUTTON, p -> new ButtonBlock(BlockSetType.SPRUCE, 30, p), buttonProperties());
    public static final Block BIRCH_BUTTON = register(BlockItemIds.BIRCH_BUTTON, p -> new ButtonBlock(BlockSetType.BIRCH, 30, p), buttonProperties());
    public static final Block JUNGLE_BUTTON = register(BlockItemIds.JUNGLE_BUTTON, p -> new ButtonBlock(BlockSetType.JUNGLE, 30, p), buttonProperties());
    public static final Block ACACIA_BUTTON = register(BlockItemIds.ACACIA_BUTTON, p -> new ButtonBlock(BlockSetType.ACACIA, 30, p), buttonProperties());
    public static final Block CHERRY_BUTTON = register(BlockItemIds.CHERRY_BUTTON, p -> new ButtonBlock(BlockSetType.CHERRY, 30, p), buttonProperties());
    public static final Block DARK_OAK_BUTTON = register(BlockItemIds.DARK_OAK_BUTTON, p -> new ButtonBlock(BlockSetType.DARK_OAK, 30, p), buttonProperties());
    public static final Block PALE_OAK_BUTTON = register(BlockItemIds.PALE_OAK_BUTTON, p -> new ButtonBlock(BlockSetType.PALE_OAK, 30, p), buttonProperties());
    public static final Block MANGROVE_BUTTON = register(BlockItemIds.MANGROVE_BUTTON, p -> new ButtonBlock(BlockSetType.MANGROVE, 30, p), buttonProperties());
    public static final Block BAMBOO_BUTTON = register(BlockItemIds.BAMBOO_BUTTON, p -> new ButtonBlock(BlockSetType.BAMBOO, 30, p), buttonProperties());
    public static final Block SKELETON_SKULL = register(
        BlockItemIds.SKELETON_SKULL,
        p -> new SkullBlock(SkullBlock.Types.SKELETON, p),
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.SKELETON).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block SKELETON_WALL_SKULL = register(
        BlockIds.SKELETON_WALL_SKULL,
        p -> new WallSkullBlock(SkullBlock.Types.SKELETON, p),
        wallVariant(SKELETON_SKULL, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block WITHER_SKELETON_SKULL = register(
        BlockItemIds.WITHER_SKELETON_SKULL,
        WitherSkullBlock::new,
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.WITHER_SKELETON).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block WITHER_SKELETON_WALL_SKULL = register(
        BlockIds.WITHER_SKELETON_WALL_SKULL,
        WitherWallSkullBlock::new,
        wallVariant(WITHER_SKELETON_SKULL, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block ZOMBIE_HEAD = register(
        BlockItemIds.ZOMBIE_HEAD,
        p -> new SkullBlock(SkullBlock.Types.ZOMBIE, p),
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.ZOMBIE).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block ZOMBIE_WALL_HEAD = register(
        BlockIds.ZOMBIE_WALL_HEAD,
        p -> new WallSkullBlock(SkullBlock.Types.ZOMBIE, p),
        wallVariant(ZOMBIE_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block PLAYER_HEAD = register(
        BlockItemIds.PLAYER_HEAD,
        PlayerHeadBlock::new,
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.CUSTOM_HEAD).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block PLAYER_WALL_HEAD = register(
        BlockIds.PLAYER_WALL_HEAD, PlayerWallHeadBlock::new, wallVariant(PLAYER_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block CREEPER_HEAD = register(
        BlockItemIds.CREEPER_HEAD,
        p -> new SkullBlock(SkullBlock.Types.CREEPER, p),
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.CREEPER).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block CREEPER_WALL_HEAD = register(
        BlockIds.CREEPER_WALL_HEAD,
        p -> new WallSkullBlock(SkullBlock.Types.CREEPER, p),
        wallVariant(CREEPER_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block DRAGON_HEAD = register(
        BlockItemIds.DRAGON_HEAD,
        p -> new SkullBlock(SkullBlock.Types.DRAGON, p),
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.DRAGON).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block DRAGON_WALL_HEAD = register(
        BlockIds.DRAGON_WALL_HEAD,
        p -> new WallSkullBlock(SkullBlock.Types.DRAGON, p),
        wallVariant(DRAGON_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block PIGLIN_HEAD = register(
        BlockItemIds.PIGLIN_HEAD,
        p -> new SkullBlock(SkullBlock.Types.PIGLIN, p),
        BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.PIGLIN).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block PIGLIN_WALL_HEAD = register(
        BlockIds.PIGLIN_WALL_HEAD, PiglinWallSkullBlock::new, wallVariant(PIGLIN_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block ANVIL = register(
        BlockItemIds.ANVIL,
        AnvilBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 1200.0F)
            .sound(SoundType.ANVIL)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block CHIPPED_ANVIL = register(
        BlockItemIds.CHIPPED_ANVIL,
        AnvilBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 1200.0F)
            .sound(SoundType.ANVIL)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block DAMAGED_ANVIL = register(
        BlockItemIds.DAMAGED_ANVIL,
        AnvilBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 1200.0F)
            .sound(SoundType.ANVIL)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block TRAPPED_CHEST = register(
        BlockItemIds.TRAPPED_CHEST,
        TrappedChestBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block LIGHT_WEIGHTED_PRESSURE_PLATE = register(
        BlockItemIds.LIGHT_WEIGHTED_PRESSURE_PLATE,
        p -> new WeightedPressurePlateBlock(15, BlockSetType.GOLD, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block HEAVY_WEIGHTED_PRESSURE_PLATE = register(
        BlockItemIds.HEAVY_WEIGHTED_PRESSURE_PLATE,
        p -> new WeightedPressurePlateBlock(150, BlockSetType.IRON, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY)
    );
    public static final Block COMPARATOR = register(
        BlockItemIds.COMPARATOR, ComparatorBlock::new, BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
    );
    public static final Block DAYLIGHT_DETECTOR = register(
        BlockItemIds.DAYLIGHT_DETECTOR,
        DaylightDetectorBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block REDSTONE_BLOCK = register(
        BlockItemIds.REDSTONE_BLOCK,
        PoweredBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.FIRE)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block NETHER_QUARTZ_ORE = register(
        BlockItemIds.NETHER_QUARTZ_ORE,
        p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F)
            .sound(SoundType.NETHER_ORE)
    );
    public static final Block HOPPER = register(
        BlockItemIds.HOPPER,
        HopperBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion()
    );
    public static final Block QUARTZ_BLOCK = register(
        BlockItemIds.QUARTZ_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block CHISELED_QUARTZ_BLOCK = register(
        BlockItemIds.CHISELED_QUARTZ_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block QUARTZ_PILLAR = register(
        BlockItemIds.QUARTZ_PILLAR,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block QUARTZ_STAIRS = registerLegacyStair(BlockItemIds.QUARTZ_STAIRS, QUARTZ_BLOCK);
    public static final Block ACTIVATOR_RAIL = register(
        BlockItemIds.ACTIVATOR_RAIL, PoweredRailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
    );
    public static final Block DROPPER = register(
        BlockItemIds.DROPPER,
        DropperBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F)
    );
    public static final ColorCollection<Block> DYED_TERRACOTTA = ColorCollection.registerBlocks(
        BlockItemIds.DYED_TERRACOTTA,
        Blocks::register,
        (var0, p) -> new Block(p),
        color -> BlockBehaviour.Properties.of()
            .mapColor(color.getTerracottaColor())
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.25F, 4.2F)
    );
    public static final ColorCollection<Block> STAINED_GLASS_PANE = ColorCollection.registerBlocks(
        BlockItemIds.STAINED_GLASS_PANE,
        Blocks::register,
        StainedGlassPaneBlock::new,
        var0 -> BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
    );
    public static final Block ACACIA_STAIRS = registerLegacyStair(BlockItemIds.ACACIA_STAIRS, ACACIA_PLANKS);
    public static final Block CHERRY_STAIRS = registerLegacyStair(BlockItemIds.CHERRY_STAIRS, CHERRY_PLANKS);
    public static final Block DARK_OAK_STAIRS = registerLegacyStair(BlockItemIds.DARK_OAK_STAIRS, DARK_OAK_PLANKS);
    public static final Block PALE_OAK_STAIRS = registerLegacyStair(BlockItemIds.PALE_OAK_STAIRS, PALE_OAK_PLANKS);
    public static final Block MANGROVE_STAIRS = registerLegacyStair(BlockItemIds.MANGROVE_STAIRS, MANGROVE_PLANKS);
    public static final Block BAMBOO_STAIRS = registerLegacyStair(BlockItemIds.BAMBOO_STAIRS, BAMBOO_PLANKS);
    public static final Block BAMBOO_MOSAIC_STAIRS = registerLegacyStair(BlockItemIds.BAMBOO_MOSAIC_STAIRS, BAMBOO_MOSAIC);
    public static final Block SLIME_BLOCK = register(
        BlockItemIds.SLIME_BLOCK,
        SlimeBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).friction(0.8F).bounceRestitution(1.0F).sound(SoundType.SLIME_BLOCK).noOcclusion()
    );
    public static final Block BARRIER = register(
        BlockItemIds.BARRIER,
        BarrierBlock::new,
        BlockBehaviour.Properties.of()
            .strength(-1.0F, 3600000.8F)
            .mapColor(waterloggedMapColor(MapColor.NONE))
            .noLootTable()
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .noTerrainParticles()
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block LIGHT = register(
        BlockItemIds.LIGHT,
        LightBlock::new,
        BlockBehaviour.Properties.of()
            .replaceable()
            .strength(-1.0F, 3600000.8F)
            .mapColor(waterloggedMapColor(MapColor.NONE))
            .noLootTable()
            .noOcclusion()
            .lightLevel(LightBlock.LIGHT_EMISSION)
    );
    public static final Block IRON_TRAPDOOR = register(
        BlockItemIds.IRON_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.IRON, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F).noOcclusion().isValidSpawn(Blocks::never)
    );
    public static final Block PRISMARINE = register(
        BlockItemIds.PRISMARINE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block PRISMARINE_BRICKS = register(
        BlockItemIds.PRISMARINE_BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block DARK_PRISMARINE = register(
        BlockItemIds.DARK_PRISMARINE,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block PRISMARINE_STAIRS = registerLegacyStair(BlockItemIds.PRISMARINE_STAIRS, PRISMARINE);
    public static final Block PRISMARINE_BRICK_STAIRS = registerLegacyStair(BlockItemIds.PRISMARINE_BRICK_STAIRS, PRISMARINE_BRICKS);
    public static final Block DARK_PRISMARINE_STAIRS = registerLegacyStair(BlockItemIds.DARK_PRISMARINE_STAIRS, DARK_PRISMARINE);
    public static final Block PRISMARINE_SLAB = register(
        BlockItemIds.PRISMARINE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block PRISMARINE_BRICK_SLAB = register(
        BlockItemIds.PRISMARINE_BRICK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block DARK_PRISMARINE_SLAB = register(
        BlockItemIds.DARK_PRISMARINE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
    );
    public static final Block SEA_LANTERN = register(
        BlockItemIds.SEA_LANTERN,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ)
            .instrument(NoteBlockInstrument.HAT)
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .lightLevel(statex -> 15)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block HAY_BLOCK = register(
        BlockItemIds.HAY_BLOCK,
        HayBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BANJO).strength(0.5F).sound(SoundType.GRASS)
    );
    public static final ColorCollection<Block> CARPET = ColorCollection.registerBlocks(
        BlockItemIds.CARPET,
        Blocks::register,
        WoolCarpetBlock::new,
        color -> BlockBehaviour.Properties.of().mapColor(color.getMapColor()).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
    );
    public static final Block TERRACOTTA = register(
        BlockItemIds.TERRACOTTA,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.25F, 4.2F)
    );
    public static final Block COAL_BLOCK = register(
        BlockItemIds.COAL_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
    );
    public static final Block PACKED_ICE = register(
        BlockItemIds.PACKED_ICE,
        BlockBehaviour.Properties.of().mapColor(MapColor.ICE).instrument(NoteBlockInstrument.CHIME).friction(0.98F).strength(0.5F).sound(SoundType.GLASS)
    );
    public static final Block SUNFLOWER = register(
        BlockItemIds.SUNFLOWER,
        TallFlowerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block LILAC = register(
        BlockItemIds.LILAC,
        TallFlowerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ROSE_BUSH = register(
        BlockItemIds.ROSE_BUSH,
        TallFlowerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PEONY = register(
        BlockItemIds.PEONY,
        TallFlowerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block TALL_GRASS = register(
        BlockItemIds.TALL_GRASS,
        DoublePlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block LARGE_FERN = register(
        BlockItemIds.LARGE_FERN,
        DoublePlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final ColorCollection<Block> BANNER = ColorCollection.registerBlocks(
        BlockItemIds.BANNER,
        Blocks::register,
        BannerBlock::new,
        var0 -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final ColorCollection<Block> WALL_BANNER = ColorCollection.registerBlocks(
        BlockIds.WALL_BANNER,
        Blocks::register,
        WallBannerBlock::new,
        color -> wallVariant(BANNER.pick(color), true)
            .mapColor(MapColor.WOOD)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(1.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block RED_SANDSTONE = register(
        BlockItemIds.RED_SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block CHISELED_RED_SANDSTONE = register(
        BlockItemIds.CHISELED_RED_SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block CUT_RED_SANDSTONE = register(
        BlockItemIds.CUT_RED_SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
    );
    public static final Block RED_SANDSTONE_STAIRS = registerLegacyStair(BlockItemIds.RED_SANDSTONE_STAIRS, RED_SANDSTONE);
    public static final Block OAK_SLAB = register(
        BlockItemIds.OAK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block SPRUCE_SLAB = register(
        BlockItemIds.SPRUCE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PODZOL)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block BIRCH_SLAB = register(
        BlockItemIds.BIRCH_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block JUNGLE_SLAB = register(
        BlockItemIds.JUNGLE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block ACACIA_SLAB = register(
        BlockItemIds.ACACIA_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block CHERRY_SLAB = register(
        BlockItemIds.CHERRY_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.CHERRY_WOOD)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_SLAB = register(
        BlockItemIds.DARK_OAK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_SLAB = register(
        BlockItemIds.PALE_OAK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block MANGROVE_SLAB = register(
        BlockItemIds.MANGROVE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block BAMBOO_SLAB = register(
        BlockItemIds.BAMBOO_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.BAMBOO_WOOD)
            .ignitedByLava()
    );
    public static final Block BAMBOO_MOSAIC_SLAB = register(
        BlockItemIds.BAMBOO_MOSAIC_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.BAMBOO_WOOD)
            .ignitedByLava()
    );
    public static final Block STONE_SLAB = register(
        BlockItemIds.STONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block SMOOTH_STONE_SLAB = register(
        BlockItemIds.SMOOTH_STONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block SANDSTONE_SLAB = register(
        BlockItemIds.SANDSTONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block CUT_SANDSTONE_SLAB = register(
        BlockItemIds.CUT_SANDSTONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block PETRIFIED_OAK_SLAB = register(
        BlockItemIds.PETRIFIED_OAK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block COBBLESTONE_SLAB = register(
        BlockItemIds.COBBLESTONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block BRICK_SLAB = register(
        BlockItemIds.BRICK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block STONE_BRICK_SLAB = register(
        BlockItemIds.STONE_BRICK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block MUD_BRICK_SLAB = register(
        BlockItemIds.MUD_BRICK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 3.0F)
            .sound(SoundType.MUD_BRICKS)
    );
    public static final Block NETHER_BRICK_SLAB = register(
        BlockItemIds.NETHER_BRICK_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.NETHER_BRICKS)
    );
    public static final Block QUARTZ_SLAB = register(
        BlockItemIds.QUARTZ_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block RED_SANDSTONE_SLAB = register(
        BlockItemIds.RED_SANDSTONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
    );
    public static final Block CUT_RED_SANDSTONE_SLAB = register(
        BlockItemIds.CUT_RED_SANDSTONE_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
    );
    public static final Block PURPUR_SLAB = register(
        BlockItemIds.PURPUR_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_MAGENTA)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
    );
    public static final Block SMOOTH_STONE = register(
        BlockItemIds.SMOOTH_STONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block SMOOTH_SANDSTONE = register(
        BlockItemIds.SMOOTH_SANDSTONE,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block SMOOTH_QUARTZ = register(
        BlockItemIds.SMOOTH_QUARTZ,
        BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
    );
    public static final Block SMOOTH_RED_SANDSTONE = register(
        BlockItemIds.SMOOTH_RED_SANDSTONE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
    );
    public static final Block SPRUCE_FENCE_GATE = register(
        BlockItemIds.SPRUCE_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block BIRCH_FENCE_GATE = register(
        BlockItemIds.BIRCH_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.BIRCH, p),
        BlockBehaviour.Properties.of()
            .mapColor(BIRCH_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block JUNGLE_FENCE_GATE = register(
        BlockItemIds.JUNGLE_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block ACACIA_FENCE_GATE = register(
        BlockItemIds.ACACIA_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(ACACIA_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block CHERRY_FENCE_GATE = register(
        BlockItemIds.CHERRY_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(CHERRY_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block DARK_OAK_FENCE_GATE = register(
        BlockItemIds.DARK_OAK_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block PALE_OAK_FENCE_GATE = register(
        BlockItemIds.PALE_OAK_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block MANGROVE_FENCE_GATE = register(
        BlockItemIds.MANGROVE_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block BAMBOO_FENCE_GATE = register(
        BlockItemIds.BAMBOO_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.BAMBOO, p),
        BlockBehaviour.Properties.of()
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
    );
    public static final Block SPRUCE_FENCE = register(
        BlockItemIds.SPRUCE_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block BIRCH_FENCE = register(
        BlockItemIds.BIRCH_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(BIRCH_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block JUNGLE_FENCE = register(
        BlockItemIds.JUNGLE_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block ACACIA_FENCE = register(
        BlockItemIds.ACACIA_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(ACACIA_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block CHERRY_FENCE = register(
        BlockItemIds.CHERRY_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(CHERRY_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.CHERRY_WOOD)
    );
    public static final Block DARK_OAK_FENCE = register(
        BlockItemIds.DARK_OAK_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block PALE_OAK_FENCE = register(
        BlockItemIds.PALE_OAK_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block MANGROVE_FENCE = register(
        BlockItemIds.MANGROVE_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .ignitedByLava()
            .sound(SoundType.WOOD)
    );
    public static final Block BAMBOO_FENCE = register(
        BlockItemIds.BAMBOO_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.BAMBOO_WOOD)
            .ignitedByLava()
    );
    public static final Block SPRUCE_DOOR = register(
        BlockItemIds.SPRUCE_DOOR,
        p -> new DoorBlock(BlockSetType.SPRUCE, p),
        BlockBehaviour.Properties.of()
            .mapColor(SPRUCE_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BIRCH_DOOR = register(
        BlockItemIds.BIRCH_DOOR,
        p -> new DoorBlock(BlockSetType.BIRCH, p),
        BlockBehaviour.Properties.of()
            .mapColor(BIRCH_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block JUNGLE_DOOR = register(
        BlockItemIds.JUNGLE_DOOR,
        p -> new DoorBlock(BlockSetType.JUNGLE, p),
        BlockBehaviour.Properties.of()
            .mapColor(JUNGLE_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ACACIA_DOOR = register(
        BlockItemIds.ACACIA_DOOR,
        p -> new DoorBlock(BlockSetType.ACACIA, p),
        BlockBehaviour.Properties.of()
            .mapColor(ACACIA_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CHERRY_DOOR = register(
        BlockItemIds.CHERRY_DOOR,
        p -> new DoorBlock(BlockSetType.CHERRY, p),
        BlockBehaviour.Properties.of()
            .mapColor(CHERRY_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block DARK_OAK_DOOR = register(
        BlockItemIds.DARK_OAK_DOOR,
        p -> new DoorBlock(BlockSetType.DARK_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(DARK_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PALE_OAK_DOOR = register(
        BlockItemIds.PALE_OAK_DOOR,
        p -> new DoorBlock(BlockSetType.PALE_OAK, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MANGROVE_DOOR = register(
        BlockItemIds.MANGROVE_DOOR,
        p -> new DoorBlock(BlockSetType.MANGROVE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MANGROVE_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BAMBOO_DOOR = register(
        BlockItemIds.BAMBOO_DOOR,
        p -> new DoorBlock(BlockSetType.BAMBOO, p),
        BlockBehaviour.Properties.of()
            .mapColor(BAMBOO_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block END_ROD = register(
        BlockItemIds.END_ROD,
        EndRodBlock::new,
        BlockBehaviour.Properties.of().forceSolidOff().instabreak().lightLevel(statex -> 14).sound(SoundType.WOOD).noOcclusion()
    );
    public static final Block CHORUS_PLANT = register(
        BlockItemIds.CHORUS_PLANT,
        ChorusPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .forceSolidOff()
            .strength(0.4F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CHORUS_FLOWER = register(
        BlockItemIds.CHORUS_FLOWER,
        p -> new ChorusFlowerBlock(CHORUS_PLANT, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .forceSolidOff()
            .randomTicks()
            .strength(0.4F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block PURPUR_BLOCK = register(
        BlockItemIds.PURPUR_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_MAGENTA)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block PURPUR_PILLAR = register(
        BlockItemIds.PURPUR_PILLAR,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_MAGENTA)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block PURPUR_STAIRS = registerLegacyStair(BlockItemIds.PURPUR_STAIRS, PURPUR_BLOCK);
    public static final Block END_STONE_BRICKS = register(
        BlockItemIds.END_STONE_BRICKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 9.0F)
    );
    public static final Block TORCHFLOWER_CROP = register(
        BlockItemIds.TORCHFLOWER_CROP,
        TorchflowerCropBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PITCHER_CROP = register(
        BlockItemIds.PITCHER_CROP,
        PitcherCropBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PITCHER_PLANT = register(
        BlockItemIds.PITCHER_PLANT,
        DoublePlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.CROP)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BEETROOTS = register(
        BlockItemIds.BEETROOT_CROP,
        BeetrootBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block DIRT_PATH = register(
        BlockItemIds.DIRT_PATH,
        DirtPathBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT)
            .strength(0.65F)
            .sound(SoundType.GRASS)
            .isViewBlocking(Blocks::always)
            .isSuffocating(Blocks::always)
    );
    public static final Block END_GATEWAY = register(
        BlockIds.END_GATEWAY,
        EndGatewayBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .noCollision()
            .lightLevel(statex -> 15)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block REPEATING_COMMAND_BLOCK = register(
        BlockItemIds.REPEATING_COMMAND_BLOCK,
        p -> new CommandBlock(false, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
    );
    public static final Block CHAIN_COMMAND_BLOCK = register(
        BlockItemIds.CHAIN_COMMAND_BLOCK,
        p -> new CommandBlock(true, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
    );
    public static final Block FROSTED_ICE = register(
        BlockIds.FROSTED_ICE,
        FrostedIceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.ICE)
            .friction(0.98F)
            .strength(0.5F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isValidSpawn((statex, blockGetter, blockPos, entityType) -> entityType == EntityTypes.POLAR_BEAR)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block MAGMA_BLOCK = register(
        BlockItemIds.MAGMA_BLOCK,
        MagmaBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .lightLevel(statex -> 3)
            .strength(0.5F)
            .isValidSpawn((statex, blockGetter, blockPos, entityType) -> entityType.fireImmune())
            .postProcess(Blocks::postProcessAbove)
            .emissiveRendering(var0 -> true)
    );
    public static final Block NETHER_WART_BLOCK = register(
        BlockItemIds.NETHER_WART_BLOCK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.WART_BLOCK)
    );
    public static final Block RED_NETHER_BRICKS = register(
        BlockItemIds.RED_NETHER_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.NETHER_BRICKS)
    );
    public static final Block BONE_BLOCK = register(
        BlockItemIds.BONE_BLOCK,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .instrument(NoteBlockInstrument.XYLOPHONE)
            .requiresCorrectToolForDrops()
            .strength(2.0F)
            .sound(SoundType.BONE_BLOCK)
    );
    public static final Block STRUCTURE_VOID = register(
        BlockItemIds.STRUCTURE_VOID,
        StructureVoidBlock::new,
        BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().noTerrainParticles().pushReaction(PushReaction.DESTROY)
    );
    public static final Block OBSERVER = register(
        BlockItemIds.OBSERVER,
        ObserverBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .strength(3.0F)
            .requiresCorrectToolForDrops()
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block SHULKER_BOX = register(BlockItemIds.SHULKER_BOX, p -> new ShulkerBoxBlock(null, p), shulkerBoxProperties(MapColor.COLOR_PURPLE));
    public static final ColorCollection<Block> DYED_SHULKER_BOX = ColorCollection.registerBlocks(
        BlockItemIds.DYED_SHULKER_BOX,
        Blocks::register,
        ShulkerBoxBlock::new,
        color -> shulkerBoxProperties(color == DyeColor.PURPLE ? MapColor.TERRACOTTA_PURPLE : color.getMapColor())
    );
    public static final ColorCollection<Block> GLAZED_TERRACOTTA = ColorCollection.registerBlocks(
        BlockItemIds.GLAZED_TERRACOTTA,
        Blocks::register,
        (var0, p) -> new GlazedTerracottaBlock(p),
        color -> BlockBehaviour.Properties.of()
            .mapColor(color.getMapColor())
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.4F)
            .pushReaction(PushReaction.PUSH_ONLY)
    );
    public static final ColorCollection<Block> CONCRETE = ColorCollection.registerBlocks(
        BlockItemIds.CONCRETE,
        Blocks::register,
        (var0, p) -> new Block(p),
        color -> BlockBehaviour.Properties.of()
            .mapColor(color.getMapColor())
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.8F)
    );
    public static final ColorCollection<Block> CONCRETE_POWDER = ColorCollection.registerBlocks(
        BlockItemIds.CONCRETE_POWDER,
        Blocks::register,
        (color, p) -> new ConcretePowderBlock(CONCRETE.pick(color), p),
        color -> BlockBehaviour.Properties.of().mapColor(color.getMapColor()).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
    );
    public static final Block KELP = register(
        BlockItemIds.KELP,
        KelpBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block KELP_PLANT = register(
        BlockIds.KELP_PLANT,
        KelpPlantBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WATER).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block DRIED_KELP_BLOCK = register(
        BlockItemIds.DRIED_KELP_BLOCK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.5F, 2.5F).sound(SoundType.GRASS)
    );
    public static final Block TURTLE_EGG = register(
        BlockItemIds.TURTLE_EGG,
        TurtleEggBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .forceSolidOn()
            .strength(0.5F)
            .sound(SoundType.METAL)
            .randomTicks()
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SNIFFER_EGG = register(
        BlockItemIds.SNIFFER_EGG,
        SnifferEggBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.METAL).noOcclusion()
    );
    public static final Block DRIED_GHAST = register(
        BlockItemIds.DRIED_GHAST,
        DriedGhastBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).forceSolidOn().instabreak().sound(SoundType.DRIED_GHAST).noOcclusion().randomTicks()
    );
    public static final Block DEAD_TUBE_CORAL_BLOCK = register(
        BlockItemIds.DEAD_TUBE_CORAL_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block DEAD_BRAIN_CORAL_BLOCK = register(
        BlockItemIds.DEAD_BRAIN_CORAL_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block DEAD_BUBBLE_CORAL_BLOCK = register(
        BlockItemIds.DEAD_BUBBLE_CORAL_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block DEAD_FIRE_CORAL_BLOCK = register(
        BlockItemIds.DEAD_FIRE_CORAL_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block DEAD_HORN_CORAL_BLOCK = register(
        BlockItemIds.DEAD_HORN_CORAL_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block TUBE_CORAL_BLOCK = register(
        BlockItemIds.TUBE_CORAL_BLOCK,
        p -> new CoralBlock(DEAD_TUBE_CORAL_BLOCK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLUE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .sound(SoundType.CORAL_BLOCK)
    );
    public static final Block BRAIN_CORAL_BLOCK = register(
        BlockItemIds.BRAIN_CORAL_BLOCK,
        p -> new CoralBlock(DEAD_BRAIN_CORAL_BLOCK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PINK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .sound(SoundType.CORAL_BLOCK)
    );
    public static final Block BUBBLE_CORAL_BLOCK = register(
        BlockItemIds.BUBBLE_CORAL_BLOCK,
        p -> new CoralBlock(DEAD_BUBBLE_CORAL_BLOCK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .sound(SoundType.CORAL_BLOCK)
    );
    public static final Block FIRE_CORAL_BLOCK = register(
        BlockItemIds.FIRE_CORAL_BLOCK,
        p -> new CoralBlock(DEAD_FIRE_CORAL_BLOCK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .sound(SoundType.CORAL_BLOCK)
    );
    public static final Block HORN_CORAL_BLOCK = register(
        BlockItemIds.HORN_CORAL_BLOCK,
        p -> new CoralBlock(DEAD_HORN_CORAL_BLOCK, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
            .sound(SoundType.CORAL_BLOCK)
    );
    public static final Block DEAD_TUBE_CORAL = register(
        BlockItemIds.DEAD_TUBE_CORAL,
        BaseCoralPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_BRAIN_CORAL = register(
        BlockItemIds.DEAD_BRAIN_CORAL,
        BaseCoralPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_BUBBLE_CORAL = register(
        BlockItemIds.DEAD_BUBBLE_CORAL,
        BaseCoralPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_FIRE_CORAL = register(
        BlockItemIds.DEAD_FIRE_CORAL,
        BaseCoralPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_HORN_CORAL = register(
        BlockItemIds.DEAD_HORN_CORAL,
        BaseCoralPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block TUBE_CORAL = register(
        BlockItemIds.TUBE_CORAL,
        p -> new CoralPlantBlock(DEAD_TUBE_CORAL, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block BRAIN_CORAL = register(
        BlockItemIds.BRAIN_CORAL,
        p -> new CoralPlantBlock(DEAD_BRAIN_CORAL, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block BUBBLE_CORAL = register(
        BlockItemIds.BUBBLE_CORAL,
        p -> new CoralPlantBlock(DEAD_BUBBLE_CORAL, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block FIRE_CORAL = register(
        BlockItemIds.FIRE_CORAL,
        p -> new CoralPlantBlock(DEAD_FIRE_CORAL, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block HORN_CORAL = register(
        BlockItemIds.HORN_CORAL,
        p -> new CoralPlantBlock(DEAD_HORN_CORAL, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block DEAD_TUBE_CORAL_FAN = register(
        BlockItemIds.DEAD_TUBE_CORAL_FAN,
        BaseCoralFanBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_BRAIN_CORAL_FAN = register(
        BlockItemIds.DEAD_BRAIN_CORAL_FAN,
        BaseCoralFanBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_BUBBLE_CORAL_FAN = register(
        BlockItemIds.DEAD_BUBBLE_CORAL_FAN,
        BaseCoralFanBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_FIRE_CORAL_FAN = register(
        BlockItemIds.DEAD_FIRE_CORAL_FAN,
        BaseCoralFanBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_HORN_CORAL_FAN = register(
        BlockItemIds.DEAD_HORN_CORAL_FAN,
        BaseCoralFanBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block TUBE_CORAL_FAN = register(
        BlockItemIds.TUBE_CORAL_FAN,
        p -> new CoralFanBlock(DEAD_TUBE_CORAL_FAN, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block BRAIN_CORAL_FAN = register(
        BlockItemIds.BRAIN_CORAL_FAN,
        p -> new CoralFanBlock(DEAD_BRAIN_CORAL_FAN, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block BUBBLE_CORAL_FAN = register(
        BlockItemIds.BUBBLE_CORAL_FAN,
        p -> new CoralFanBlock(DEAD_BUBBLE_CORAL_FAN, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block FIRE_CORAL_FAN = register(
        BlockItemIds.FIRE_CORAL_FAN,
        p -> new CoralFanBlock(DEAD_FIRE_CORAL_FAN, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block HORN_CORAL_FAN = register(
        BlockItemIds.HORN_CORAL_FAN,
        p -> new CoralFanBlock(DEAD_HORN_CORAL_FAN, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block DEAD_TUBE_CORAL_WALL_FAN = register(
        BlockIds.DEAD_TUBE_CORAL_WALL_FAN,
        BaseCoralWallFanBlock::new,
        wallVariant(DEAD_TUBE_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_BRAIN_CORAL_WALL_FAN = register(
        BlockIds.DEAD_BRAIN_CORAL_WALL_FAN,
        BaseCoralWallFanBlock::new,
        wallVariant(DEAD_BRAIN_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_BUBBLE_CORAL_WALL_FAN = register(
        BlockIds.DEAD_BUBBLE_CORAL_WALL_FAN,
        BaseCoralWallFanBlock::new,
        wallVariant(DEAD_BUBBLE_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_FIRE_CORAL_WALL_FAN = register(
        BlockIds.DEAD_FIRE_CORAL_WALL_FAN,
        BaseCoralWallFanBlock::new,
        wallVariant(DEAD_FIRE_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block DEAD_HORN_CORAL_WALL_FAN = register(
        BlockIds.DEAD_HORN_CORAL_WALL_FAN,
        BaseCoralWallFanBlock::new,
        wallVariant(DEAD_HORN_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_GRAY)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .noCollision()
            .instabreak()
    );
    public static final Block TUBE_CORAL_WALL_FAN = register(
        BlockIds.TUBE_CORAL_WALL_FAN,
        p -> new CoralWallFanBlock(DEAD_TUBE_CORAL_WALL_FAN, p),
        wallVariant(TUBE_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_BLUE)
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BRAIN_CORAL_WALL_FAN = register(
        BlockIds.BRAIN_CORAL_WALL_FAN,
        p -> new CoralWallFanBlock(DEAD_BRAIN_CORAL_WALL_FAN, p),
        wallVariant(BRAIN_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_PINK)
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BUBBLE_CORAL_WALL_FAN = register(
        BlockIds.BUBBLE_CORAL_WALL_FAN,
        p -> new CoralWallFanBlock(DEAD_BUBBLE_CORAL_WALL_FAN, p),
        wallVariant(BUBBLE_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_PURPLE)
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block FIRE_CORAL_WALL_FAN = register(
        BlockIds.FIRE_CORAL_WALL_FAN,
        p -> new CoralWallFanBlock(DEAD_FIRE_CORAL_WALL_FAN, p),
        wallVariant(FIRE_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_RED)
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block HORN_CORAL_WALL_FAN = register(
        BlockIds.HORN_CORAL_WALL_FAN,
        p -> new CoralWallFanBlock(DEAD_HORN_CORAL_WALL_FAN, p),
        wallVariant(HORN_CORAL_FAN, false)
            .mapColor(MapColor.COLOR_YELLOW)
            .noCollision()
            .instabreak()
            .sound(SoundType.WET_GRASS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SEA_PICKLE = register(
        BlockItemIds.SEA_PICKLE,
        SeaPickleBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN)
            .lightLevel(statex -> SeaPickleBlock.isDead(statex) ? 0 : 3 + 3 * statex.getValue(SeaPickleBlock.PICKLES))
            .sound(SoundType.SLIME_BLOCK)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BLUE_ICE = register(
        BlockItemIds.BLUE_ICE,
        HalfTransparentBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(2.8F).friction(0.989F).sound(SoundType.GLASS)
    );
    public static final Block CONDUIT = register(
        BlockItemIds.CONDUIT,
        ConduitBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIAMOND)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.HAT)
            .strength(3.0F)
            .lightLevel(statex -> 15)
            .noOcclusion()
    );
    public static final Block BAMBOO_SAPLING = register(
        BlockIds.BAMBOO_SAPLING,
        BambooSaplingBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .forceSolidOn()
            .randomTicks()
            .instabreak()
            .noCollision()
            .strength(1.0F)
            .sound(SoundType.BAMBOO_SAPLING)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block BAMBOO = register(
        BlockItemIds.BAMBOO,
        BambooStalkBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .forceSolidOn()
            .randomTicks()
            .instabreak()
            .strength(1.0F)
            .sound(SoundType.BAMBOO)
            .noOcclusion()
            .dynamicShape()
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block POTTED_BAMBOO = register(BlockIds.POTTED_BAMBOO, p -> new FlowerPotBlock(BAMBOO, p), flowerPotProperties());
    public static final Block VOID_AIR = register(
        BlockIds.VOID_AIR, AirBlock::new, BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().air()
    );
    public static final Block CAVE_AIR = register(
        BlockIds.CAVE_AIR, AirBlock::new, BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().air()
    );
    public static final Block BUBBLE_COLUMN = register(
        BlockIds.BUBBLE_COLUMN,
        BubbleColumnBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .replaceable()
            .noCollision()
            .noLootTable()
            .pushReaction(PushReaction.DESTROY)
            .liquid()
            .sound(SoundType.EMPTY)
    );
    public static final Block POLISHED_GRANITE_STAIRS = registerLegacyStair(BlockItemIds.POLISHED_GRANITE_STAIRS, POLISHED_GRANITE);
    public static final Block SMOOTH_RED_SANDSTONE_STAIRS = registerLegacyStair(BlockItemIds.SMOOTH_RED_SANDSTONE_STAIRS, SMOOTH_RED_SANDSTONE);
    public static final Block MOSSY_STONE_BRICK_STAIRS = registerLegacyStair(BlockItemIds.MOSSY_STONE_BRICK_STAIRS, MOSSY_STONE_BRICKS);
    public static final Block POLISHED_DIORITE_STAIRS = registerLegacyStair(BlockItemIds.POLISHED_DIORITE_STAIRS, POLISHED_DIORITE);
    public static final Block MOSSY_COBBLESTONE_STAIRS = registerLegacyStair(BlockItemIds.MOSSY_COBBLESTONE_STAIRS, MOSSY_COBBLESTONE);
    public static final Block END_STONE_BRICK_STAIRS = registerLegacyStair(BlockItemIds.END_STONE_BRICK_STAIRS, END_STONE_BRICKS);
    public static final Block STONE_STAIRS = registerLegacyStair(BlockItemIds.STONE_STAIRS, STONE);
    public static final Block SMOOTH_SANDSTONE_STAIRS = registerLegacyStair(BlockItemIds.SMOOTH_SANDSTONE_STAIRS, SMOOTH_SANDSTONE);
    public static final Block SMOOTH_QUARTZ_STAIRS = registerLegacyStair(BlockItemIds.SMOOTH_QUARTZ_STAIRS, SMOOTH_QUARTZ);
    public static final Block GRANITE_STAIRS = registerLegacyStair(BlockItemIds.GRANITE_STAIRS, GRANITE);
    public static final Block ANDESITE_STAIRS = registerLegacyStair(BlockItemIds.ANDESITE_STAIRS, ANDESITE);
    public static final Block RED_NETHER_BRICK_STAIRS = registerLegacyStair(BlockItemIds.RED_NETHER_BRICK_STAIRS, RED_NETHER_BRICKS);
    public static final Block POLISHED_ANDESITE_STAIRS = registerLegacyStair(BlockItemIds.POLISHED_ANDESITE_STAIRS, POLISHED_ANDESITE);
    public static final Block DIORITE_STAIRS = registerLegacyStair(BlockItemIds.DIORITE_STAIRS, DIORITE);
    public static final Block POLISHED_GRANITE_SLAB = register(
        BlockItemIds.POLISHED_GRANITE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_GRANITE)
    );
    public static final Block SMOOTH_RED_SANDSTONE_SLAB = register(
        BlockItemIds.SMOOTH_RED_SANDSTONE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SMOOTH_RED_SANDSTONE)
    );
    public static final Block MOSSY_STONE_BRICK_SLAB = register(
        BlockItemIds.MOSSY_STONE_BRICK_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MOSSY_STONE_BRICKS)
    );
    public static final Block POLISHED_DIORITE_SLAB = register(
        BlockItemIds.POLISHED_DIORITE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_DIORITE)
    );
    public static final Block MOSSY_COBBLESTONE_SLAB = register(
        BlockItemIds.MOSSY_COBBLESTONE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MOSSY_COBBLESTONE)
    );
    public static final Block END_STONE_BRICK_SLAB = register(
        BlockItemIds.END_STONE_BRICK_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(END_STONE_BRICKS)
    );
    public static final Block SMOOTH_SANDSTONE_SLAB = register(
        BlockItemIds.SMOOTH_SANDSTONE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SMOOTH_SANDSTONE)
    );
    public static final Block SMOOTH_QUARTZ_SLAB = register(
        BlockItemIds.SMOOTH_QUARTZ_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SMOOTH_QUARTZ)
    );
    public static final Block GRANITE_SLAB = register(BlockItemIds.GRANITE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(GRANITE));
    public static final Block ANDESITE_SLAB = register(BlockItemIds.ANDESITE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(ANDESITE));
    public static final Block RED_NETHER_BRICK_SLAB = register(
        BlockItemIds.RED_NETHER_BRICK_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(RED_NETHER_BRICKS)
    );
    public static final Block POLISHED_ANDESITE_SLAB = register(
        BlockItemIds.POLISHED_ANDESITE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_ANDESITE)
    );
    public static final Block DIORITE_SLAB = register(BlockItemIds.DIORITE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DIORITE));
    public static final Block BRICK_WALL = register(BlockItemIds.BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(BRICKS).forceSolidOn());
    public static final Block PRISMARINE_WALL = register(
        BlockItemIds.PRISMARINE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(PRISMARINE).forceSolidOn()
    );
    public static final Block RED_SANDSTONE_WALL = register(
        BlockItemIds.RED_SANDSTONE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(RED_SANDSTONE).forceSolidOn()
    );
    public static final Block MOSSY_STONE_BRICK_WALL = register(
        BlockItemIds.MOSSY_STONE_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MOSSY_STONE_BRICKS).forceSolidOn()
    );
    public static final Block GRANITE_WALL = register(BlockItemIds.GRANITE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(GRANITE).forceSolidOn());
    public static final Block STONE_BRICK_WALL = register(
        BlockItemIds.STONE_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(STONE_BRICKS).forceSolidOn()
    );
    public static final Block MUD_BRICK_WALL = register(
        BlockItemIds.MUD_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MUD_BRICKS).forceSolidOn()
    );
    public static final Block NETHER_BRICK_WALL = register(
        BlockItemIds.NETHER_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(NETHER_BRICKS).forceSolidOn()
    );
    public static final Block ANDESITE_WALL = register(
        BlockItemIds.ANDESITE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(ANDESITE).forceSolidOn()
    );
    public static final Block RED_NETHER_BRICK_WALL = register(
        BlockItemIds.RED_NETHER_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(RED_NETHER_BRICKS).forceSolidOn()
    );
    public static final Block SANDSTONE_WALL = register(
        BlockItemIds.SANDSTONE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SANDSTONE).forceSolidOn()
    );
    public static final Block END_STONE_BRICK_WALL = register(
        BlockItemIds.END_STONE_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(END_STONE_BRICKS).forceSolidOn()
    );
    public static final Block DIORITE_WALL = register(BlockItemIds.DIORITE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DIORITE).forceSolidOn());
    public static final Block SCAFFOLDING = register(
        BlockItemIds.SCAFFOLDING,
        ScaffoldingBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SAND)
            .noCollision()
            .sound(SoundType.SCAFFOLDING)
            .dynamicShape()
            .isValidSpawn(Blocks::never)
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block LOOM = register(
        BlockItemIds.LOOM,
        LoomBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block BARREL = register(
        BlockItemIds.BARREL,
        BarrelBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block SMOKER = register(
        BlockItemIds.SMOKER,
        SmokerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.5F)
            .lightLevel(litBlockEmission(13))
    );
    public static final Block BLAST_FURNACE = register(
        BlockItemIds.BLAST_FURNACE,
        BlastFurnaceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.5F)
            .lightLevel(litBlockEmission(13))
    );
    public static final Block CARTOGRAPHY_TABLE = register(
        BlockItemIds.CARTOGRAPHY_TABLE,
        CartographyTableBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block FLETCHING_TABLE = register(
        BlockItemIds.FLETCHING_TABLE,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block GRINDSTONE = register(
        BlockItemIds.GRINDSTONE,
        GrindstoneBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.STONE)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block LECTERN = register(
        BlockItemIds.LECTERN,
        LecternBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block SMITHING_TABLE = register(
        BlockItemIds.SMITHING_TABLE,
        SmithingTableBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block STONECUTTER = register(
        BlockItemIds.STONECUTTER,
        StonecutterBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F)
    );
    public static final Block BELL = register(
        BlockItemIds.BELL,
        BellBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().strength(5.0F).sound(SoundType.ANVIL).pushReaction(PushReaction.DESTROY)
    );
    public static final Block LANTERN = register(
        BlockItemIds.LANTERN,
        LanternBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .forceSolidOn()
            .strength(3.5F)
            .sound(SoundType.LANTERN)
            .lightLevel(statex -> 15)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SOUL_LANTERN = register(
        BlockItemIds.SOUL_LANTERN,
        LanternBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .forceSolidOn()
            .strength(3.5F)
            .sound(SoundType.LANTERN)
            .lightLevel(statex -> 10)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final WeatheringCopperCollection<Block> COPPER_LANTERN = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_LANTERN,
        Blocks::register,
        (s, p) -> new LanternBlock(p),
        WeatheringLanternBlock::new,
        p -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .forceSolidOn()
            .strength(3.5F)
            .sound(SoundType.LANTERN)
            .lightLevel(statex -> 15)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CAMPFIRE = register(
        BlockItemIds.CAMPFIRE,
        p -> new CampfireBlock(true, 1, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PODZOL)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .lightLevel(litBlockEmission(15))
            .noOcclusion()
            .ignitedByLava()
    );
    public static final Block SOUL_CAMPFIRE = register(
        BlockItemIds.SOUL_CAMPFIRE,
        p -> new CampfireBlock(false, 2, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PODZOL)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(SoundType.WOOD)
            .lightLevel(litBlockEmission(10))
            .noOcclusion()
            .ignitedByLava()
    );
    public static final Block SWEET_BERRY_BUSH = register(
        BlockItemIds.SWEET_BERRY_CROP,
        SweetBerryBushBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .randomTicks()
            .noCollision()
            .sound(SoundType.SWEET_BERRY_BUSH)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block WARPED_STEM = register(BlockItemIds.WARPED_STEM, RotatedPillarBlock::new, netherStemProperties(MapColor.WARPED_STEM));
    public static final Block STRIPPED_WARPED_STEM = register(
        BlockItemIds.STRIPPED_WARPED_STEM, RotatedPillarBlock::new, netherStemProperties(MapColor.WARPED_STEM)
    );
    public static final Block WARPED_HYPHAE = register(
        BlockItemIds.WARPED_HYPHAE,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
    );
    public static final Block STRIPPED_WARPED_HYPHAE = register(
        BlockItemIds.STRIPPED_WARPED_HYPHAE,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
    );
    public static final Block WARPED_NYLIUM = register(
        BlockItemIds.WARPED_NYLIUM,
        NyliumBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WARPED_NYLIUM)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(0.4F)
            .sound(SoundType.NYLIUM)
            .randomTicks()
    );
    public static final Block WARPED_FUNGUS = register(
        BlockItemIds.WARPED_FUNGUS,
        p -> new NetherFungusBlock(TreeFeatures.WARPED_FUNGUS_PLANTED, WARPED_NYLIUM, BlockTags.SUPPORTS_WARPED_FUNGUS, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instabreak().noCollision().sound(SoundType.FUNGUS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block WARPED_WART_BLOCK = register(
        BlockItemIds.WARPED_WART_BLOCK, BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_WART_BLOCK).strength(1.0F).sound(SoundType.WART_BLOCK)
    );
    public static final Block WARPED_ROOTS = register(
        BlockItemIds.WARPED_ROOTS,
        p -> new NetherRootsBlock(BlockTags.SUPPORTS_WARPED_ROOTS, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.ROOTS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block NETHER_SPROUTS = register(
        BlockItemIds.NETHER_SPROUTS,
        NetherSproutsBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.NETHER_SPROUTS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CRIMSON_STEM = register(BlockItemIds.CRIMSON_STEM, RotatedPillarBlock::new, netherStemProperties(MapColor.CRIMSON_STEM));
    public static final Block STRIPPED_CRIMSON_STEM = register(
        BlockItemIds.STRIPPED_CRIMSON_STEM, RotatedPillarBlock::new, netherStemProperties(MapColor.CRIMSON_STEM)
    );
    public static final Block CRIMSON_HYPHAE = register(
        BlockItemIds.CRIMSON_HYPHAE,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
    );
    public static final Block STRIPPED_CRIMSON_HYPHAE = register(
        BlockItemIds.STRIPPED_CRIMSON_HYPHAE,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
    );
    public static final Block CRIMSON_NYLIUM = register(
        BlockItemIds.CRIMSON_NYLIUM,
        NyliumBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.CRIMSON_NYLIUM)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(0.4F)
            .sound(SoundType.NYLIUM)
            .randomTicks()
    );
    public static final Block CRIMSON_FUNGUS = register(
        BlockItemIds.CRIMSON_FUNGUS,
        p -> new NetherFungusBlock(TreeFeatures.CRIMSON_FUNGUS_PLANTED, CRIMSON_NYLIUM, BlockTags.SUPPORTS_CRIMSON_FUNGUS, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).instabreak().noCollision().sound(SoundType.FUNGUS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block SHROOMLIGHT = register(
        BlockItemIds.SHROOMLIGHT,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.SHROOMLIGHT).lightLevel(statex -> 15)
    );
    public static final Block WEEPING_VINES = register(
        BlockItemIds.WEEPING_VINES,
        WeepingVinesBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .randomTicks()
            .noCollision()
            .instabreak()
            .sound(SoundType.WEEPING_VINES)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block WEEPING_VINES_PLANT = register(
        BlockIds.WEEPING_VINES_PLANT,
        WeepingVinesPlantBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).noCollision().instabreak().sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.DESTROY)
    );
    public static final Block TWISTING_VINES = register(
        BlockItemIds.TWISTING_VINES,
        TwistingVinesBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .randomTicks()
            .noCollision()
            .instabreak()
            .sound(SoundType.WEEPING_VINES)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block TWISTING_VINES_PLANT = register(
        BlockIds.TWISTING_VINES_PLANT,
        TwistingVinesPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .noCollision()
            .instabreak()
            .sound(SoundType.WEEPING_VINES)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CRIMSON_ROOTS = register(
        BlockItemIds.CRIMSON_ROOTS,
        p -> new NetherRootsBlock(BlockTags.SUPPORTS_CRIMSON_ROOTS, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.ROOTS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CRIMSON_PLANKS = register(
        BlockItemIds.CRIMSON_PLANKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_STEM).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD)
    );
    public static final Block WARPED_PLANKS = register(
        BlockItemIds.WARPED_PLANKS,
        BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD)
    );
    public static final Block CRIMSON_SLAB = register(
        BlockItemIds.CRIMSON_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.NETHER_WOOD)
    );
    public static final Block WARPED_SLAB = register(
        BlockItemIds.WARPED_SLAB,
        SlabBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.NETHER_WOOD)
    );
    public static final Block CRIMSON_PRESSURE_PLATE = register(
        BlockItemIds.CRIMSON_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.CRIMSON, p),
        BlockBehaviour.Properties.of()
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block WARPED_PRESSURE_PLATE = register(
        BlockItemIds.WARPED_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.WARPED, p),
        BlockBehaviour.Properties.of()
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASS)
            .noCollision()
            .strength(0.5F)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CRIMSON_FENCE = register(
        BlockItemIds.CRIMSON_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.NETHER_WOOD)
    );
    public static final Block WARPED_FENCE = register(
        BlockItemIds.WARPED_FENCE,
        FenceBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.NETHER_WOOD)
    );
    public static final Block CRIMSON_TRAPDOOR = register(
        BlockItemIds.CRIMSON_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.CRIMSON, p),
        BlockBehaviour.Properties.of()
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
    );
    public static final Block WARPED_TRAPDOOR = register(
        BlockItemIds.WARPED_TRAPDOOR,
        p -> new TrapDoorBlock(BlockSetType.WARPED, p),
        BlockBehaviour.Properties.of()
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
    );
    public static final Block CRIMSON_FENCE_GATE = register(
        BlockItemIds.CRIMSON_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.CRIMSON, p),
        BlockBehaviour.Properties.of().mapColor(CRIMSON_PLANKS.defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
    );
    public static final Block WARPED_FENCE_GATE = register(
        BlockItemIds.WARPED_FENCE_GATE,
        p -> new FenceGateBlock(WoodType.WARPED, p),
        BlockBehaviour.Properties.of().mapColor(WARPED_PLANKS.defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
    );
    public static final Block CRIMSON_STAIRS = registerLegacyStair(BlockItemIds.CRIMSON_STAIRS, CRIMSON_PLANKS);
    public static final Block WARPED_STAIRS = registerLegacyStair(BlockItemIds.WARPED_STAIRS, WARPED_PLANKS);
    public static final Block CRIMSON_BUTTON = register(BlockItemIds.CRIMSON_BUTTON, p -> new ButtonBlock(BlockSetType.CRIMSON, 30, p), buttonProperties());
    public static final Block WARPED_BUTTON = register(BlockItemIds.WARPED_BUTTON, p -> new ButtonBlock(BlockSetType.WARPED, 30, p), buttonProperties());
    public static final Block CRIMSON_DOOR = register(
        BlockItemIds.CRIMSON_DOOR,
        p -> new DoorBlock(BlockSetType.CRIMSON, p),
        BlockBehaviour.Properties.of()
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block WARPED_DOOR = register(
        BlockItemIds.WARPED_DOOR,
        p -> new DoorBlock(BlockSetType.WARPED, p),
        BlockBehaviour.Properties.of()
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .strength(3.0F)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CRIMSON_SIGN = register(
        BlockItemIds.CRIMSON_SIGN,
        p -> new StandingSignBlock(WoodType.CRIMSON, p),
        BlockBehaviour.Properties.of()
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .forceSolidOn()
            .noCollision()
            .strength(1.0F)
    );
    public static final Block WARPED_SIGN = register(
        BlockItemIds.WARPED_SIGN,
        p -> new StandingSignBlock(WoodType.WARPED, p),
        BlockBehaviour.Properties.of()
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .forceSolidOn()
            .noCollision()
            .strength(1.0F)
    );
    public static final Block CRIMSON_WALL_SIGN = register(
        BlockIds.CRIMSON_WALL_SIGN,
        p -> new WallSignBlock(WoodType.CRIMSON, p),
        wallVariant(CRIMSON_SIGN, true)
            .mapColor(CRIMSON_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .forceSolidOn()
            .noCollision()
            .strength(1.0F)
    );
    public static final Block WARPED_WALL_SIGN = register(
        BlockIds.WARPED_WALL_SIGN,
        p -> new WallSignBlock(WoodType.WARPED, p),
        wallVariant(WARPED_SIGN, true)
            .mapColor(WARPED_PLANKS.defaultMapColor())
            .instrument(NoteBlockInstrument.BASS)
            .forceSolidOn()
            .noCollision()
            .strength(1.0F)
    );
    public static final Block STRUCTURE_BLOCK = register(
        BlockItemIds.STRUCTURE_BLOCK,
        StructureBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
    );
    public static final Block JIGSAW = register(
        BlockItemIds.JIGSAW,
        JigsawBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
    );
    public static final Block TEST_BLOCK = register(
        BlockItemIds.TEST_BLOCK, TestBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0F, 3600000.0F).noLootTable()
    );
    public static final Block TEST_INSTANCE_BLOCK = register(
        BlockItemIds.TEST_INSTANCE_BLOCK,
        TestInstanceBlock::new,
        BlockBehaviour.Properties.of().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable().isViewBlocking(Blocks::never)
    );
    public static final Block COMPOSTER = register(
        BlockItemIds.COMPOSTER,
        ComposterBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.6F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block TARGET = register(
        BlockItemIds.TARGET, TargetBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.5F).sound(SoundType.GRASS)
    );
    public static final Block BEE_NEST = register(
        BlockItemIds.BEE_NEST,
        BeehiveBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASS)
            .strength(0.3F)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );
    public static final Block BEEHIVE = register(
        BlockItemIds.BEEHIVE,
        BeehiveBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.6F).sound(SoundType.WOOD).ignitedByLava()
    );
    public static final Block HONEY_BLOCK = register(
        BlockItemIds.HONEY_BLOCK,
        HoneyBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).speedFactor(0.4F).jumpFactor(0.5F).noOcclusion().sound(SoundType.HONEY_BLOCK)
    );
    public static final Block HONEYCOMB_BLOCK = register(
        BlockItemIds.HONEYCOMB_BLOCK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6F).sound(SoundType.CORAL_BLOCK)
    );
    public static final Block NETHERITE_BLOCK = register(
        BlockItemIds.NETHERITE_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
    );
    public static final Block ANCIENT_DEBRIS = register(
        BlockItemIds.ANCIENT_DEBRIS,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(30.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)
    );
    public static final Block CRYING_OBSIDIAN = register(
        BlockItemIds.CRYING_OBSIDIAN,
        CryingObsidianBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(50.0F, 1200.0F)
            .lightLevel(statex -> 10)
    );
    public static final Block RESPAWN_ANCHOR = register(
        BlockItemIds.RESPAWN_ANCHOR,
        RespawnAnchorBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(50.0F, 1200.0F)
            .lightLevel(statex -> RespawnAnchorBlock.getScaledChargeLevel(statex, 15))
    );
    public static final Block POTTED_CRIMSON_FUNGUS = register(
        BlockIds.POTTED_CRIMSON_FUNGUS, p -> new FlowerPotBlock(CRIMSON_FUNGUS, p), flowerPotProperties()
    );
    public static final Block POTTED_WARPED_FUNGUS = register(BlockIds.POTTED_WARPED_FUNGUS, p -> new FlowerPotBlock(WARPED_FUNGUS, p), flowerPotProperties());
    public static final Block POTTED_CRIMSON_ROOTS = register(BlockIds.POTTED_CRIMSON_ROOTS, p -> new FlowerPotBlock(CRIMSON_ROOTS, p), flowerPotProperties());
    public static final Block POTTED_WARPED_ROOTS = register(BlockIds.POTTED_WARPED_ROOTS, p -> new FlowerPotBlock(WARPED_ROOTS, p), flowerPotProperties());
    public static final Block LODESTONE = register(
        BlockItemIds.LODESTONE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(3.5F)
            .sound(SoundType.LODESTONE)
            .pushReaction(PushReaction.BLOCK)
    );
    public static final Block BLACKSTONE = register(
        BlockItemIds.BLACKSTONE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block BLACKSTONE_STAIRS = registerLegacyStair(BlockItemIds.BLACKSTONE_STAIRS, BLACKSTONE);
    public static final Block BLACKSTONE_WALL = register(
        BlockItemIds.BLACKSTONE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).forceSolidOn()
    );
    public static final Block BLACKSTONE_SLAB = register(
        BlockItemIds.BLACKSTONE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).strength(2.0F, 6.0F)
    );
    public static final Block POLISHED_BLACKSTONE = register(
        BlockItemIds.POLISHED_BLACKSTONE, BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).strength(2.0F, 6.0F)
    );
    public static final Block POLISHED_BLACKSTONE_BRICKS = register(
        BlockItemIds.POLISHED_BLACKSTONE_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE).strength(1.5F, 6.0F)
    );
    public static final Block CRACKED_POLISHED_BLACKSTONE_BRICKS = register(
        BlockItemIds.CRACKED_POLISHED_BLACKSTONE_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE_BRICKS)
    );
    public static final Block CHISELED_POLISHED_BLACKSTONE = register(
        BlockItemIds.CHISELED_POLISHED_BLACKSTONE, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE).strength(1.5F, 6.0F)
    );
    public static final Block POLISHED_BLACKSTONE_BRICK_SLAB = register(
        BlockItemIds.POLISHED_BLACKSTONE_BRICK_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE_BRICKS).strength(2.0F, 6.0F)
    );
    public static final Block POLISHED_BLACKSTONE_BRICK_STAIRS = registerLegacyStair(BlockItemIds.POLISHED_BLACKSTONE_BRICK_STAIRS, POLISHED_BLACKSTONE_BRICKS);
    public static final Block POLISHED_BLACKSTONE_BRICK_WALL = register(
        BlockItemIds.POLISHED_BLACKSTONE_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE_BRICKS).forceSolidOn()
    );
    public static final Block GILDED_BLACKSTONE = register(
        BlockItemIds.GILDED_BLACKSTONE, BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).sound(SoundType.GILDED_BLACKSTONE)
    );
    public static final Block POLISHED_BLACKSTONE_STAIRS = registerLegacyStair(BlockItemIds.POLISHED_BLACKSTONE_STAIRS, POLISHED_BLACKSTONE);
    public static final Block POLISHED_BLACKSTONE_SLAB = register(
        BlockItemIds.POLISHED_BLACKSTONE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE)
    );
    public static final Block POLISHED_BLACKSTONE_PRESSURE_PLATE = register(
        BlockItemIds.POLISHED_BLACKSTONE_PRESSURE_PLATE,
        p -> new PressurePlateBlock(BlockSetType.POLISHED_BLACKSTONE, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .noCollision()
            .strength(0.5F)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block POLISHED_BLACKSTONE_BUTTON = register(
        BlockItemIds.POLISHED_BLACKSTONE_BUTTON, p -> new ButtonBlock(BlockSetType.STONE, 20, p), buttonProperties()
    );
    public static final Block POLISHED_BLACKSTONE_WALL = register(
        BlockItemIds.POLISHED_BLACKSTONE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE).forceSolidOn()
    );
    public static final Block CHISELED_NETHER_BRICKS = register(
        BlockItemIds.CHISELED_NETHER_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.NETHER_BRICKS)
    );
    public static final Block CRACKED_NETHER_BRICKS = register(
        BlockItemIds.CRACKED_NETHER_BRICKS,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.NETHER)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 6.0F)
            .sound(SoundType.NETHER_BRICKS)
    );
    public static final Block QUARTZ_BRICKS = register(BlockItemIds.QUARTZ_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(QUARTZ_BLOCK));
    public static final Block CANDLE = register(BlockItemIds.CANDLE, CandleBlock::new, candleProperties(MapColor.SAND));
    public static final ColorCollection<Block> DYED_CANDLE = ColorCollection.registerBlocks(
        BlockItemIds.DYED_CANDLE,
        Blocks::register,
        (var0, p) -> new CandleBlock(p),
        color -> candleProperties(color == DyeColor.WHITE ? MapColor.WOOL : color.getMapColor())
    );
    public static final Block CANDLE_CAKE = register(
        BlockIds.CANDLE_CAKE, p -> new CandleCakeBlock(CANDLE, p), BlockBehaviour.Properties.ofLegacyCopy(CAKE).lightLevel(litBlockEmission(3))
    );
    public static final ColorCollection<Block> DYED_CANDLE_CAKE = ColorCollection.registerBlocks(
        BlockIds.DYED_CANDLE_CAKE,
        Blocks::register,
        (s, p) -> new CandleCakeBlock(DYED_CANDLE.pick(s), p),
        var0 -> BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
    );
    public static final Block AMETHYST_BLOCK = register(
        BlockItemIds.AMETHYST_BLOCK,
        AmethystBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops()
    );
    public static final Block BUDDING_AMETHYST = register(
        BlockItemIds.BUDDING_AMETHYST,
        BuddingAmethystBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .randomTicks()
            .strength(1.5F)
            .sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block AMETHYST_CLUSTER = register(
        BlockItemIds.AMETHYST_CLUSTER,
        p -> new AmethystClusterBlock(7.0F, 10.0F, p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .forceSolidOn()
            .noOcclusion()
            .sound(SoundType.AMETHYST_CLUSTER)
            .strength(1.5F)
            .lightLevel(statex -> 5)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block LARGE_AMETHYST_BUD = register(
        BlockItemIds.LARGE_AMETHYST_BUD,
        p -> new AmethystClusterBlock(5.0F, 10.0F, p),
        BlockBehaviour.Properties.ofLegacyCopy(AMETHYST_CLUSTER).sound(SoundType.MEDIUM_AMETHYST_BUD).lightLevel(statex -> 4)
    );
    public static final Block MEDIUM_AMETHYST_BUD = register(
        BlockItemIds.MEDIUM_AMETHYST_BUD,
        p -> new AmethystClusterBlock(4.0F, 10.0F, p),
        BlockBehaviour.Properties.ofLegacyCopy(AMETHYST_CLUSTER).sound(SoundType.LARGE_AMETHYST_BUD).lightLevel(statex -> 2)
    );
    public static final Block SMALL_AMETHYST_BUD = register(
        BlockItemIds.SMALL_AMETHYST_BUD,
        p -> new AmethystClusterBlock(3.0F, 8.0F, p),
        BlockBehaviour.Properties.ofLegacyCopy(AMETHYST_CLUSTER).sound(SoundType.SMALL_AMETHYST_BUD).lightLevel(statex -> 1)
    );
    public static final Block TUFF = register(
        BlockItemIds.TUFF,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_GRAY)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.TUFF)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block TUFF_SLAB = register(BlockItemIds.TUFF_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF));
    public static final Block TUFF_STAIRS = register(
        BlockItemIds.TUFF_STAIRS, p -> new StairBlock(TUFF.defaultBlockState(), p), BlockBehaviour.Properties.ofLegacyCopy(TUFF)
    );
    public static final Block TUFF_WALL = register(BlockItemIds.TUFF_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF).forceSolidOn());
    public static final Block POLISHED_TUFF = register(BlockItemIds.POLISHED_TUFF, BlockBehaviour.Properties.ofLegacyCopy(TUFF).sound(SoundType.POLISHED_TUFF));
    public static final Block POLISHED_TUFF_SLAB = register(
        BlockItemIds.POLISHED_TUFF_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_TUFF)
    );
    public static final Block POLISHED_TUFF_STAIRS = register(
        BlockItemIds.POLISHED_TUFF_STAIRS, p -> new StairBlock(POLISHED_TUFF.defaultBlockState(), p), BlockBehaviour.Properties.ofLegacyCopy(POLISHED_TUFF)
    );
    public static final Block POLISHED_TUFF_WALL = register(
        BlockItemIds.POLISHED_TUFF_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_TUFF).forceSolidOn()
    );
    public static final Block CHISELED_TUFF = register(BlockItemIds.CHISELED_TUFF, BlockBehaviour.Properties.ofLegacyCopy(TUFF));
    public static final Block TUFF_BRICKS = register(BlockItemIds.TUFF_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(TUFF).sound(SoundType.TUFF_BRICKS));
    public static final Block TUFF_BRICK_SLAB = register(BlockItemIds.TUFF_BRICK_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS));
    public static final Block TUFF_BRICK_STAIRS = register(
        BlockItemIds.TUFF_BRICK_STAIRS, p -> new StairBlock(TUFF_BRICKS.defaultBlockState(), p), BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS)
    );
    public static final Block TUFF_BRICK_WALL = register(
        BlockItemIds.TUFF_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS).forceSolidOn()
    );
    public static final Block CHISELED_TUFF_BRICKS = register(BlockItemIds.CHISELED_TUFF_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS));
    public static final Block SULFUR = register(
        BlockItemIds.SULFUR,
        BlockBehaviour.Properties.of()
            .sound(SoundType.SULFUR)
            .mapColor(MapColor.COLOR_YELLOW)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block POTENT_SULFUR = register(
        BlockItemIds.POTENT_SULFUR, PotentSulfurBlock::new, BlockBehaviour.Properties.ofFullCopy(SULFUR).sound(SoundType.POTENT_SULFUR).mapColor(MapColor.GOLD)
    );
    public static final Block SULFUR_SLAB = registerSlab(BlockItemIds.SULFUR_SLAB, SULFUR);
    public static final Block SULFUR_STAIRS = registerStair(BlockItemIds.SULFUR_STAIRS, SULFUR);
    public static final Block SULFUR_WALL = registerWall(BlockItemIds.SULFUR_WALL, SULFUR);
    public static final Block POLISHED_SULFUR = register(BlockItemIds.POLISHED_SULFUR, BlockBehaviour.Properties.ofLegacyCopy(SULFUR));
    public static final Block POLISHED_SULFUR_SLAB = registerSlab(BlockItemIds.POLISHED_SULFUR_SLAB, POLISHED_SULFUR);
    public static final Block POLISHED_SULFUR_STAIRS = registerStair(BlockItemIds.POLISHED_SULFUR_STAIRS, POLISHED_SULFUR);
    public static final Block POLISHED_SULFUR_WALL = registerWall(BlockItemIds.POLISHED_SULFUR_WALL, POLISHED_SULFUR);
    public static final Block SULFUR_BRICKS = register(BlockItemIds.SULFUR_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(SULFUR));
    public static final Block SULFUR_BRICK_SLAB = registerSlab(BlockItemIds.SULFUR_BRICK_SLAB, SULFUR_BRICKS);
    public static final Block SULFUR_BRICK_STAIRS = registerStair(BlockItemIds.SULFUR_BRICK_STAIRS, SULFUR_BRICKS);
    public static final Block SULFUR_BRICK_WALL = registerWall(BlockItemIds.SULFUR_BRICK_WALL, SULFUR_BRICKS);
    public static final Block CHISELED_SULFUR = register(BlockItemIds.CHISELED_SULFUR, BlockBehaviour.Properties.ofLegacyCopy(SULFUR));
    public static final Block CINNABAR = register(
        BlockItemIds.CINNABAR,
        BlockBehaviour.Properties.of()
            .sound(SoundType.CINNABAR)
            .mapColor(MapColor.COLOR_RED)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
    );
    public static final Block CINNABAR_SLAB = registerSlab(BlockItemIds.CINNABAR_SLAB, CINNABAR);
    public static final Block CINNABAR_STAIRS = registerStair(BlockItemIds.CINNABAR_STAIRS, CINNABAR);
    public static final Block CINNABAR_WALL = registerWall(BlockItemIds.CINNABAR_WALL, CINNABAR);
    public static final Block POLISHED_CINNABAR = register(BlockItemIds.POLISHED_CINNABAR, BlockBehaviour.Properties.ofLegacyCopy(CINNABAR));
    public static final Block POLISHED_CINNABAR_SLAB = registerSlab(BlockItemIds.POLISHED_CINNABAR_SLAB, POLISHED_CINNABAR);
    public static final Block POLISHED_CINNABAR_STAIRS = registerStair(BlockItemIds.POLISHED_CINNABAR_STAIRS, POLISHED_CINNABAR);
    public static final Block POLISHED_CINNABAR_WALL = registerWall(BlockItemIds.POLISHED_CINNABAR_WALL, POLISHED_CINNABAR);
    public static final Block CINNABAR_BRICKS = register(BlockItemIds.CINNABAR_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(CINNABAR));
    public static final Block CINNABAR_BRICK_SLAB = registerSlab(BlockItemIds.CINNABAR_BRICK_SLAB, CINNABAR_BRICKS);
    public static final Block CINNABAR_BRICK_STAIRS = registerStair(BlockItemIds.CINNABAR_BRICK_STAIRS, CINNABAR_BRICKS);
    public static final Block CINNABAR_BRICK_WALL = registerWall(BlockItemIds.CINNABAR_BRICK_WALL, CINNABAR_BRICKS);
    public static final Block CHISELED_CINNABAR = register(BlockItemIds.CHISELED_CINNABAR, BlockBehaviour.Properties.ofLegacyCopy(CINNABAR));
    public static final Block CALCITE = register(
        BlockItemIds.CALCITE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.CALCITE)
            .requiresCorrectToolForDrops()
            .strength(0.75F)
    );
    public static final Block TINTED_GLASS = register(
        BlockItemIds.TINTED_GLASS,
        TintedGlassBlock::new,
        BlockBehaviour.Properties.ofLegacyCopy(GLASS)
            .mapColor(MapColor.COLOR_GRAY)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
    );
    public static final Block POWDER_SNOW = register(
        BlockItemIds.POWDER_SNOW,
        PowderSnowBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW)
            .strength(0.25F)
            .sound(SoundType.POWDER_SNOW)
            .dynamicShape()
            .noOcclusion()
            .isRedstoneConductor(Blocks::never)
    );
    public static final Block SCULK_SENSOR = register(
        BlockItemIds.SCULK_SENSOR,
        SculkSensorBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .strength(1.5F)
            .sound(SoundType.SCULK_SENSOR)
            .lightLevel(statex -> 1)
            .emissiveRendering(statex -> SculkSensorBlock.getPhase(statex) == SculkSensorPhase.ACTIVE)
    );
    public static final Block CALIBRATED_SCULK_SENSOR = register(
        BlockItemIds.CALIBRATED_SCULK_SENSOR, CalibratedSculkSensorBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SCULK_SENSOR)
    );
    public static final Block SCULK = register(
        BlockItemIds.SCULK, SculkBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2F).sound(SoundType.SCULK)
    );
    public static final Block SCULK_VEIN = register(
        BlockItemIds.SCULK_VEIN,
        SculkVeinBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .forceSolidOn()
            .noCollision()
            .strength(0.2F)
            .sound(SoundType.SCULK_VEIN)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SCULK_CATALYST = register(
        BlockItemIds.SCULK_CATALYST,
        SculkCatalystBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 3.0F).sound(SoundType.SCULK_CATALYST).lightLevel(statex -> 6)
    );
    public static final Block SCULK_SHRIEKER = register(
        BlockItemIds.SCULK_SHRIEKER,
        SculkShriekerBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 3.0F).sound(SoundType.SCULK_SHRIEKER)
    );
    public static final WeatheringCopperCollection<Block> COPPER_BLOCK = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_BLOCK, Blocks::register, (var0, p) -> new Block(p), WeatheringCopperFullBlock::new, statex -> {
            BlockBehaviour.Properties var10000 = BlockBehaviour.Properties.of();
            MapColor var10001 = switch (statex) {
                case UNAFFECTED -> MapColor.COLOR_ORANGE;
                case EXPOSED -> MapColor.TERRACOTTA_LIGHT_GRAY;
                case WEATHERED -> MapColor.WARPED_STEM;
                case OXIDIZED -> MapColor.WARPED_NYLIUM;
            };

            return var10000.mapColor(var10001).requiresCorrectToolForDrops().strength(3.0F, 6.0F).instrument(switch (statex) {
                case UNAFFECTED -> NoteBlockInstrument.TRUMPET;
                case EXPOSED -> NoteBlockInstrument.TRUMPET_EXPOSED;
                case WEATHERED -> NoteBlockInstrument.TRUMPET_WEATHERED;
                case OXIDIZED -> NoteBlockInstrument.TRUMPET_OXIDIZED;
            }).sound(SoundType.COPPER);
        }
    );
    public static final Block COPPER_ORE = register(
        BlockItemIds.COPPER_ORE, p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.ofLegacyCopy(IRON_ORE)
    );
    public static final Block DEEPSLATE_COPPER_ORE = register(
        BlockItemIds.DEEPSLATE_COPPER_ORE,
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        BlockBehaviour.Properties.ofLegacyCopy(COPPER_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
    );
    public static final WeatheringCopperCollection<Block> CUT_COPPER = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.CUT_COPPER,
        Blocks::register,
        (var0, p) -> new Block(p),
        WeatheringCopperFullBlock::new,
        statex -> BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK.weathering().pick(statex))
    );
    public static final WeatheringCopperCollection<Block> CHISELED_COPPER = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.CHISELED_COPPER,
        Blocks::register,
        (var0, p) -> new Block(p),
        WeatheringCopperFullBlock::new,
        statex -> BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK.weathering().pick(statex))
    );
    public static final WeatheringCopperCollection<Block> CUT_COPPER_STAIRS = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.CUT_COPPER_STAIRS,
        Blocks::register,
        (statex, p) -> new StairBlock(CUT_COPPER.waxed().pick(statex).defaultBlockState(), p),
        (statex, p) -> new WeatheringCopperStairBlock(statex, CUT_COPPER.weathering().pick(statex).defaultBlockState(), p),
        statex -> BlockBehaviour.Properties.ofFullCopy(CUT_COPPER.weathering().pick(statex))
    );
    public static final WeatheringCopperCollection<Block> CUT_COPPER_SLAB = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.CUT_COPPER_SLAB,
        Blocks::register,
        (var0, p) -> new SlabBlock(p),
        WeatheringCopperSlabBlock::new,
        statex -> BlockBehaviour.Properties.ofFullCopy(CUT_COPPER.weathering().pick(statex))
    );
    public static final WeatheringCopperCollection<Block> COPPER_DOOR = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_DOOR,
        Blocks::register,
        (var0, p) -> new DoorBlock(BlockSetType.COPPER, p),
        (statex, p) -> new WeatheringCopperDoorBlock(BlockSetType.COPPER, statex, p),
        statex -> BlockBehaviour.Properties.of()
            .mapColor(var1x -> COPPER_BLOCK.weathering().pick(statex).defaultMapColor())
            .strength(3.0F, 6.0F)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final WeatheringCopperCollection<Block> COPPER_TRAPDOOR = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_TRAPDOOR,
        Blocks::register,
        (var0, p) -> new TrapDoorBlock(BlockSetType.COPPER, p),
        (statex, p) -> new WeatheringCopperTrapDoorBlock(BlockSetType.COPPER, statex, p),
        statex -> BlockBehaviour.Properties.of()
            .mapColor(var1x -> COPPER_BLOCK.weathering().pick(statex).defaultMapColor())
            .strength(3.0F, 6.0F)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .isValidSpawn(Blocks::never)
    );
    public static final WeatheringCopperCollection<Block> COPPER_GRATE = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_GRATE,
        Blocks::register,
        (var0, p) -> new WaterloggedTransparentBlock(p),
        WeatheringCopperGrateBlock::new,
        statex -> BlockBehaviour.Properties.of()
            .strength(3.0F, 6.0F)
            .sound(SoundType.COPPER_GRATE)
            .mapColor(var1x -> COPPER_BLOCK.weathering().pick(statex).defaultMapColor())
            .noOcclusion()
            .requiresCorrectToolForDrops()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
    );
    public static final WeatheringCopperCollection<Block> COPPER_BULB = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_BULB,
        Blocks::register,
        (s, p) -> new CopperBulbBlock(p),
        WeatheringCopperBulbBlock::new,
        p -> {
            return BlockBehaviour.Properties.of()
                .mapColor(var1x -> COPPER_BLOCK.waxed().pick(p).defaultMapColor())
                .strength(3.0F, 6.0F)
                .sound(SoundType.COPPER_BULB)
                .requiresCorrectToolForDrops()
                .isRedstoneConductor(Blocks::never)
                .lightLevel(litBlockEmission(switch (p) {
                    case UNAFFECTED -> 15;
                    case EXPOSED -> 12;
                    case WEATHERED -> 8;
                    case OXIDIZED -> 4;
                }));
        }
    );
    public static final WeatheringCopperCollection<Block> COPPER_CHEST = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_CHEST,
        Blocks::register,
        (statex, p) -> new CopperChestBlock(statex, CopperChestBlock.getHingeSound(statex, true), CopperChestBlock.getHingeSound(statex, false), p),
        (statex, p) -> new WeatheringCopperChestBlock(statex, CopperChestBlock.getHingeSound(statex, true), CopperChestBlock.getHingeSound(statex, false), p),
        statex -> BlockBehaviour.Properties.of()
            .mapColor(var1x -> COPPER_BLOCK.weathering().pick(statex).defaultMapColor())
            .strength(3.0F, 6.0F)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops()
    );
    public static final WeatheringCopperCollection<Block> COPPER_GOLEM_STATUE = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.COPPER_GOLEM_STATUE,
        Blocks::register,
        CopperGolemStatueBlock::new,
        WeatheringCopperGolemStatueBlock::new,
        statex -> BlockBehaviour.Properties.of()
            .mapColor(var1x -> COPPER_BLOCK.weathering().pick(statex).defaultMapColor())
            .strength(3.0F, 6.0F)
            .sound(SoundType.COPPER_GOLEM_STATUE)
            .pushReaction(PushReaction.DESTROY)
            .noOcclusion()
    );
    public static final WeatheringCopperCollection<Block> LIGHTNING_ROD = WeatheringCopperCollection.registerBlocks(
        BlockItemIds.LIGHTNING_ROD,
        Blocks::register,
        (var0, p) -> new LightningRodBlock(p),
        WeatheringLightningRodBlock::new,
        statex -> BlockBehaviour.Properties.of()
            .mapColor(var1x -> COPPER_BLOCK.weathering().pick(statex).defaultMapColor())
            .forceSolidOn()
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.COPPER)
            .noOcclusion()
    );
    public static final Block DRIPSTONE_BLOCK = register(
        BlockItemIds.DRIPSTONE_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.DRIPSTONE_BLOCK)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 1.0F)
    );
    public static final Block POINTED_DRIPSTONE = register(
        BlockItemIds.POINTED_DRIPSTONE,
        p -> new PointedDripstoneBlock(DRIPSTONE_BLOCK.defaultBlockState(), p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .noOcclusion()
            .sound(SoundType.POINTED_DRIPSTONE)
            .randomTicks()
            .strength(1.5F, 3.0F)
            .dynamicShape()
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
            .noOcclusion()
    );
    public static final Block SULFUR_SPIKE = register(
        BlockItemIds.SULFUR_SPIKE,
        p -> new SulfurSpikeBlock(SULFUR.defaultBlockState(), p),
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .forceSolidOn()
            .instrument(NoteBlockInstrument.BASEDRUM)
            .noOcclusion()
            .sound(SoundType.SULFUR_SPIKE)
            .randomTicks()
            .strength(1.5F, 3.0F)
            .dynamicShape()
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never)
            .noOcclusion()
    );
    public static final Block CAVE_VINES = register(
        BlockItemIds.GLOW_BERRY_CROP,
        CaveVinesBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .randomTicks()
            .noCollision()
            .lightLevel(CaveVines.emission(14))
            .instabreak()
            .sound(SoundType.CAVE_VINES)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block CAVE_VINES_PLANT = register(
        BlockIds.CAVE_VINES_PLANT,
        CaveVinesPlantBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .lightLevel(CaveVines.emission(14))
            .instabreak()
            .sound(SoundType.CAVE_VINES)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block SPORE_BLOSSOM = register(
        BlockItemIds.SPORE_BLOSSOM,
        SporeBlossomBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().noCollision().sound(SoundType.SPORE_BLOSSOM).pushReaction(PushReaction.DESTROY)
    );
    public static final Block AZALEA = register(
        BlockItemIds.AZALEA,
        AzaleaBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .forceSolidOff()
            .instabreak()
            .sound(SoundType.AZALEA)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block FLOWERING_AZALEA = register(
        BlockItemIds.FLOWERING_AZALEA,
        AzaleaBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .forceSolidOff()
            .instabreak()
            .sound(SoundType.FLOWERING_AZALEA)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MOSS_CARPET = register(
        BlockItemIds.MOSS_CARPET,
        CarpetBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1F).sound(SoundType.MOSS_CARPET).pushReaction(PushReaction.DESTROY)
    );
    public static final Block PINK_PETALS = register(
        BlockItemIds.PINK_PETALS,
        FlowerBedBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().sound(SoundType.PINK_PETALS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block WILDFLOWERS = register(
        BlockItemIds.WILDFLOWERS,
        FlowerBedBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().sound(SoundType.PINK_PETALS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block LEAF_LITTER = register(
        BlockItemIds.LEAF_LITTER,
        LeafLitterBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .replaceable()
            .noCollision()
            .sound(SoundType.LEAF_LITTER)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block MOSS_BLOCK = register(
        BlockItemIds.MOSS_BLOCK,
        p -> new BonemealableFeaturePlacerBlock(CaveFeatures.MOSS_PATCH_BONEMEAL, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1F).sound(SoundType.MOSS).pushReaction(PushReaction.DESTROY)
    );
    public static final Block BIG_DRIPLEAF = register(
        BlockItemIds.BIG_DRIPLEAF,
        BigDripleafBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).forceSolidOff().strength(0.1F).sound(SoundType.BIG_DRIPLEAF).pushReaction(PushReaction.DESTROY)
    );
    public static final Block BIG_DRIPLEAF_STEM = register(
        BlockIds.BIG_DRIPLEAF_STEM,
        BigDripleafStemBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().strength(0.1F).sound(SoundType.BIG_DRIPLEAF).pushReaction(PushReaction.DESTROY)
    );
    public static final Block SMALL_DRIPLEAF = register(
        BlockItemIds.SMALL_DRIPLEAF,
        SmallDripleafBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.SMALL_DRIPLEAF)
            .offsetType(BlockBehaviour.OffsetType.XYZ)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block HANGING_ROOTS = register(
        BlockItemIds.HANGING_ROOTS,
        HangingRootsBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT)
            .replaceable()
            .noCollision()
            .instabreak()
            .sound(SoundType.HANGING_ROOTS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block ROOTED_DIRT = register(
        BlockItemIds.ROOTED_DIRT, RootedDirtBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.ROOTED_DIRT)
    );
    public static final Block MUD = register(
        BlockItemIds.MUD,
        MudBlock::new,
        BlockBehaviour.Properties.ofLegacyCopy(DIRT)
            .mapColor(MapColor.TERRACOTTA_CYAN)
            .isValidSpawn(Blocks::always)
            .isRedstoneConductor(Blocks::always)
            .isViewBlocking(Blocks::always)
            .isSuffocating(Blocks::always)
            .sound(SoundType.MUD)
    );
    public static final Block DEEPSLATE = register(
        BlockItemIds.DEEPSLATE,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.DEEPSLATE)
    );
    public static final Block COBBLED_DEEPSLATE = register(
        BlockItemIds.COBBLED_DEEPSLATE, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE).strength(3.5F, 6.0F)
    );
    public static final Block COBBLED_DEEPSLATE_STAIRS = registerLegacyStair(BlockItemIds.COBBLED_DEEPSLATE_STAIRS, COBBLED_DEEPSLATE);
    public static final Block COBBLED_DEEPSLATE_SLAB = register(
        BlockItemIds.COBBLED_DEEPSLATE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE)
    );
    public static final Block COBBLED_DEEPSLATE_WALL = register(
        BlockItemIds.COBBLED_DEEPSLATE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).forceSolidOn()
    );
    public static final Block POLISHED_DEEPSLATE = register(
        BlockItemIds.POLISHED_DEEPSLATE, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.POLISHED_DEEPSLATE)
    );
    public static final Block POLISHED_DEEPSLATE_STAIRS = registerLegacyStair(BlockItemIds.POLISHED_DEEPSLATE_STAIRS, POLISHED_DEEPSLATE);
    public static final Block POLISHED_DEEPSLATE_SLAB = register(
        BlockItemIds.POLISHED_DEEPSLATE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_DEEPSLATE)
    );
    public static final Block POLISHED_DEEPSLATE_WALL = register(
        BlockItemIds.POLISHED_DEEPSLATE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_DEEPSLATE).forceSolidOn()
    );
    public static final Block DEEPSLATE_TILES = register(
        BlockItemIds.DEEPSLATE_TILES, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.DEEPSLATE_TILES)
    );
    public static final Block DEEPSLATE_TILE_STAIRS = registerLegacyStair(BlockItemIds.DEEPSLATE_TILE_STAIRS, DEEPSLATE_TILES);
    public static final Block DEEPSLATE_TILE_SLAB = register(
        BlockItemIds.DEEPSLATE_TILE_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_TILES)
    );
    public static final Block DEEPSLATE_TILE_WALL = register(
        BlockItemIds.DEEPSLATE_TILE_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_TILES).forceSolidOn()
    );
    public static final Block DEEPSLATE_BRICKS = register(
        BlockItemIds.DEEPSLATE_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.DEEPSLATE_BRICKS)
    );
    public static final Block DEEPSLATE_BRICK_STAIRS = registerLegacyStair(BlockItemIds.DEEPSLATE_BRICK_STAIRS, DEEPSLATE_BRICKS);
    public static final Block DEEPSLATE_BRICK_SLAB = register(
        BlockItemIds.DEEPSLATE_BRICK_SLAB, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_BRICKS)
    );
    public static final Block DEEPSLATE_BRICK_WALL = register(
        BlockItemIds.DEEPSLATE_BRICK_WALL, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_BRICKS).forceSolidOn()
    );
    public static final Block CHISELED_DEEPSLATE = register(
        BlockItemIds.CHISELED_DEEPSLATE, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.DEEPSLATE_BRICKS)
    );
    public static final Block CRACKED_DEEPSLATE_BRICKS = register(
        BlockItemIds.CRACKED_DEEPSLATE_BRICKS, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_BRICKS)
    );
    public static final Block CRACKED_DEEPSLATE_TILES = register(BlockItemIds.CRACKED_DEEPSLATE_TILES, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_TILES));
    public static final Block INFESTED_DEEPSLATE = register(
        BlockItemIds.INFESTED_DEEPSLATE,
        p -> new InfestedRotatedPillarBlock(DEEPSLATE, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).sound(SoundType.DEEPSLATE)
    );
    public static final Block SMOOTH_BASALT = register(BlockItemIds.SMOOTH_BASALT, BlockBehaviour.Properties.ofLegacyCopy(BASALT));
    public static final Block RAW_IRON_BLOCK = register(
        BlockItemIds.RAW_IRON_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.RAW_IRON).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
    );
    public static final Block RAW_COPPER_BLOCK = register(
        BlockItemIds.RAW_COPPER_BLOCK,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F)
    );
    public static final Block RAW_GOLD_BLOCK = register(
        BlockItemIds.RAW_GOLD_BLOCK,
        BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
    );
    public static final Block POTTED_AZALEA = register(BlockIds.POTTED_AZALEA_BUSH, p -> new FlowerPotBlock(AZALEA, p), flowerPotProperties());
    public static final Block POTTED_FLOWERING_AZALEA = register(
        BlockIds.POTTED_FLOWERING_AZALEA_BUSH, p -> new FlowerPotBlock(FLOWERING_AZALEA, p), flowerPotProperties()
    );
    public static final Block OCHRE_FROGLIGHT = register(
        BlockItemIds.OCHRE_FROGLIGHT,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.3F).lightLevel(statex -> 15).sound(SoundType.FROGLIGHT)
    );
    public static final Block VERDANT_FROGLIGHT = register(
        BlockItemIds.VERDANT_FROGLIGHT,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.GLOW_LICHEN).strength(0.3F).lightLevel(statex -> 15).sound(SoundType.FROGLIGHT)
    );
    public static final Block PEARLESCENT_FROGLIGHT = register(
        BlockItemIds.PEARLESCENT_FROGLIGHT,
        RotatedPillarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3F).lightLevel(statex -> 15).sound(SoundType.FROGLIGHT)
    );
    public static final Block FROGSPAWN = register(
        BlockItemIds.FROGSPAWN,
        FrogspawnBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .instabreak()
            .noOcclusion()
            .noCollision()
            .sound(SoundType.FROGSPAWN)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block REINFORCED_DEEPSLATE = register(
        BlockItemIds.REINFORCED_DEEPSLATE,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .sound(SoundType.DEEPSLATE)
            .strength(55.0F, 1200.0F)
    );
    public static final Block DECORATED_POT = register(
        BlockItemIds.DECORATED_POT,
        DecoratedPotBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.0F, 0.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
    );
    public static final Block CRAFTER = register(
        BlockItemIds.CRAFTER, CrafterBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 3.5F)
    );
    public static final Block TRIAL_SPAWNER = register(
        BlockItemIds.TRIAL_SPAWNER,
        TrialSpawnerBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .lightLevel(statex -> statex.getValue(TrialSpawnerBlock.STATE).lightLevel())
            .strength(50.0F)
            .sound(SoundType.TRIAL_SPAWNER)
            .isViewBlocking(Blocks::never)
            .noOcclusion()
    );
    public static final Block VAULT = register(
        BlockItemIds.VAULT,
        VaultBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .noOcclusion()
            .sound(SoundType.VAULT)
            .lightLevel(statex -> statex.getValue(VaultBlock.STATE).lightLevel())
            .strength(50.0F)
            .isViewBlocking(Blocks::never)
    );
    public static final Block HEAVY_CORE = register(
        BlockItemIds.HEAVY_CORE,
        HeavyCoreBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .instrument(NoteBlockInstrument.SNARE)
            .sound(SoundType.HEAVY_CORE)
            .strength(10.0F)
            .pushReaction(PushReaction.NORMAL)
            .explosionResistance(1200.0F)
    );
    public static final Block PALE_MOSS_BLOCK = register(
        BlockItemIds.PALE_MOSS_BLOCK,
        p -> new BonemealableFeaturePlacerBlock(VegetationFeatures.PALE_MOSS_PATCH_BONEMEAL, p),
        BlockBehaviour.Properties.of()
            .ignitedByLava()
            .mapColor(MapColor.COLOR_LIGHT_GRAY)
            .strength(0.1F)
            .sound(SoundType.MOSS)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block PALE_MOSS_CARPET = register(
        BlockItemIds.PALE_MOSS_CARPET,
        MossyCarpetBlock::new,
        BlockBehaviour.Properties.of()
            .ignitedByLava()
            .mapColor(PALE_MOSS_BLOCK.defaultMapColor())
            .strength(0.1F)
            .sound(SoundType.MOSS_CARPET)
            .pushReaction(PushReaction.DESTROY)
            .noOcclusion()
    );
    public static final Block PALE_HANGING_MOSS = register(
        BlockItemIds.PALE_HANGING_MOSS,
        HangingMossBlock::new,
        BlockBehaviour.Properties.of()
            .ignitedByLava()
            .mapColor(PALE_MOSS_BLOCK.defaultMapColor())
            .noCollision()
            .sound(SoundType.MOSS_CARPET)
            .pushReaction(PushReaction.DESTROY)
    );
    public static final Block OPEN_EYEBLOSSOM = register(
        BlockItemIds.OPEN_EYEBLOSSOM,
        p -> new EyeblossomBlock(EyeblossomBlock.Type.OPEN, p),
        BlockBehaviour.Properties.of()
            .mapColor(CREAKING_HEART.defaultMapColor())
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
            .randomTicks()
    );
    public static final Block CLOSED_EYEBLOSSOM = register(
        BlockItemIds.CLOSED_EYEBLOSSOM,
        p -> new EyeblossomBlock(EyeblossomBlock.Type.CLOSED, p),
        BlockBehaviour.Properties.of()
            .mapColor(PALE_OAK_LEAVES.defaultMapColor())
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY)
            .randomTicks()
    );
    public static final Block POTTED_OPEN_EYEBLOSSOM = register(
        BlockIds.POTTED_OPEN_EYEBLOSSOM, p -> new FlowerPotBlock(OPEN_EYEBLOSSOM, p), flowerPotProperties().randomTicks()
    );
    public static final Block POTTED_CLOSED_EYEBLOSSOM = register(
        BlockIds.POTTED_CLOSED_EYEBLOSSOM, p -> new FlowerPotBlock(CLOSED_EYEBLOSSOM, p), flowerPotProperties().randomTicks()
    );
    public static final Block FIREFLY_BUSH = register(
        BlockItemIds.FIREFLY_BUSH,
        FireflyBushBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .ignitedByLava()
            .lightLevel(statex -> 2)
            .noCollision()
            .instabreak()
            .sound(SoundType.SWEET_BERRY_BUSH)
            .pushReaction(PushReaction.DESTROY)
    );

    private static ToIntFunction<BlockState> litBlockEmission(final int lightEmission) {
        return state -> state.getValue(BlockStateProperties.LIT) ? lightEmission : 0;
    }

    private static Function<BlockState, MapColor> waterloggedMapColor(final MapColor mapColor) {
        return blockState -> blockState.getValue(BlockStateProperties.WATERLOGGED) ? MapColor.WATER : mapColor;
    }

    private static Boolean never(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos, final EntityType<?> entityType) {
        return false;
    }

    private static Boolean always(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos, final EntityType<?> entityType) {
        return true;
    }

    private static Boolean ocelotOrParrot(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos, final EntityType<?> entityType) {
        return entityType == EntityTypes.OCELOT || entityType == EntityTypes.PARROT;
    }

    private static BlockBehaviour.Properties logProperties(final MapColor topColor, final MapColor sideColor, final SoundType soundType) {
        return BlockBehaviour.Properties.of()
            .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? topColor : sideColor)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F)
            .sound(soundType)
            .ignitedByLava();
    }

    private static BlockBehaviour.Properties netherStemProperties(final MapColor mapColor) {
        return BlockBehaviour.Properties.of().mapColor(state -> mapColor).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM);
    }

    private static boolean always(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos) {
        return true;
    }

    private static boolean never(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos) {
        return false;
    }

    private static BlockPos postProcessSelf(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos) {
        return blockPos;
    }

    private static BlockPos postProcessAbove(final BlockState state, final BlockGetter blockGetter, final BlockPos blockPos) {
        return blockPos.above();
    }

    private static BlockBehaviour.Properties leavesProperties(final SoundType soundType) {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .strength(0.2F)
            .randomTicks()
            .sound(soundType)
            .noOcclusion()
            .isValidSpawn(Blocks::ocelotOrParrot)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor(Blocks::never);
    }

    private static BlockBehaviour.Properties shulkerBoxProperties(final MapColor mapColor) {
        return BlockBehaviour.Properties.of()
            .mapColor(mapColor)
            .forceSolidOn()
            .strength(2.0F)
            .dynamicShape()
            .noOcclusion()
            .isSuffocating(NOT_CLOSED_SHULKER)
            .isViewBlocking(NOT_CLOSED_SHULKER)
            .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties pistonProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(1.5F)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(NOT_EXTENDED_PISTON)
            .isViewBlocking(NOT_EXTENDED_PISTON)
            .pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties buttonProperties() {
        return BlockBehaviour.Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties flowerPotProperties() {
        return BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties candleProperties(final MapColor color) {
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .noOcclusion()
            .strength(0.1F)
            .sound(SoundType.CANDLE)
            .lightLevel(CandleBlock.LIGHT_EMISSION)
            .pushReaction(PushReaction.DESTROY);
    }

    private static Block registerLegacyStair(final BlockItemId id, final Block base) {
        return register(id.block(), p -> new StairBlock(base.defaultBlockState(), p), BlockBehaviour.Properties.ofLegacyCopy(base));
    }

    private static Block registerStair(final BlockItemId id, final Block base) {
        return register(id, p -> new StairBlock(base.defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static Block registerSlab(final BlockItemId id, final Block base) {
        return register(id, SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(base));
    }

    private static Block registerWall(final BlockItemId id, final Block base) {
        return register(id, WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(base).forceSolidOn());
    }

    private static BlockBehaviour.Properties wallVariant(final Block standingBlock, final boolean copyName) {
        BlockBehaviour.Properties wallProperties = BlockBehaviour.Properties.of().overrideLootTable(standingBlock.getLootTable());
        if (copyName) {
            wallProperties = wallProperties.overrideDescription(standingBlock.getDescriptionId());
        }

        return wallProperties;
    }

    private static Block register(final BlockItemId id, final Function<BlockBehaviour.Properties, Block> factory, final BlockBehaviour.Properties properties) {
        return register(id.block(), factory, properties);
    }

    private static Block register(
        final ResourceKey<Block> id, final Function<BlockBehaviour.Properties, Block> factory, final BlockBehaviour.Properties properties
    ) {
        Block block = factory.apply(properties.setId(id));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block register(final BlockItemId id, final BlockBehaviour.Properties properties) {
        return register(id.block(), properties);
    }

    private static Block register(final ResourceKey<Block> id, final BlockBehaviour.Properties properties) {
        return register(id, Block::new, properties);
    }

    static {
        for (Block block : BuiltInRegistries.BLOCK) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                Block.BLOCK_STATE_REGISTRY.add(state);
                state.initCache();
            }
        }
    }
}