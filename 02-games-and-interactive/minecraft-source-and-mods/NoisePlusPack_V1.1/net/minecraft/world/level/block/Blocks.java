package net.minecraft.world.level.block;

import com.google.common.collect.UnmodifiableIterator;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.references.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
   private static final BlockBehaviour.StatePredicate NOT_CLOSED_SHULKER = (p_309267_, p_309268_, p_309269_) -> p_309268_.getBlockEntity(p_309269_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity
      ? shulkerboxblockentity.isClosed()
      : true;
   private static final BlockBehaviour.StatePredicate NOT_EXTENDED_PISTON = (p_152641_, p_152642_, p_152643_) -> !p_152641_.getValue(PistonBaseBlock.EXTENDED);
   public static final Block AIR = register("air", AirBlock::new, BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().air());
   public static final Block STONE = register(
      "stone",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block GRANITE = register(
      "granite",
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block POLISHED_GRANITE = register(
      "polished_granite",
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block DIORITE = register(
      "diorite",
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block POLISHED_DIORITE = register(
      "polished_diorite",
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block ANDESITE = register(
      "andesite",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block POLISHED_ANDESITE = register(
      "polished_andesite",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block GRASS_BLOCK = register(
      "grass_block", GrassBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).randomTicks().strength(0.6F).sound(SoundType.GRASS)
   );
   public static final Block DIRT = register("dirt", BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL));
   public static final Block COARSE_DIRT = register(
      "coarse_dirt", BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL)
   );
   public static final Block PODZOL = register(
      "podzol", SnowyDirtBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(0.5F).sound(SoundType.GRAVEL)
   );
   public static final Block COBBLESTONE = register(
      "cobblestone",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block OAK_PLANKS = register(
      "oak_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block SPRUCE_PLANKS = register(
      "spruce_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block BIRCH_PLANKS = register(
      "birch_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block JUNGLE_PLANKS = register(
      "jungle_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block ACACIA_PLANKS = register(
      "acacia_planks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block CHERRY_PLANKS = register(
      "cherry_planks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_WHITE)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.CHERRY_WOOD)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_PLANKS = register(
      "dark_oak_planks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BROWN)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_WOOD = register(
      "pale_oak_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block PALE_OAK_PLANKS = register(
      "pale_oak_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block MANGROVE_PLANKS = register(
      "mangrove_planks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_RED)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BAMBOO_PLANKS = register(
      "bamboo_planks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.BAMBOO_WOOD)
         .ignitedByLava()
   );
   public static final Block BAMBOO_MOSAIC = register(
      "bamboo_mosaic",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.BAMBOO_WOOD)
         .ignitedByLava()
   );
   public static final Block OAK_SAPLING = register(
      "oak_sapling",
      p_360165_ -> new SaplingBlock(TreeGrower.OAK, p_360165_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block SPRUCE_SAPLING = register(
      "spruce_sapling",
      p_360114_ -> new SaplingBlock(TreeGrower.SPRUCE, p_360114_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BIRCH_SAPLING = register(
      "birch_sapling",
      p_360061_ -> new SaplingBlock(TreeGrower.BIRCH, p_360061_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block JUNGLE_SAPLING = register(
      "jungle_sapling",
      p_360174_ -> new SaplingBlock(TreeGrower.JUNGLE, p_360174_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block ACACIA_SAPLING = register(
      "acacia_sapling",
      p_360361_ -> new SaplingBlock(TreeGrower.ACACIA, p_360361_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CHERRY_SAPLING = register(
      "cherry_sapling",
      p_360214_ -> new SaplingBlock(TreeGrower.CHERRY, p_360214_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_PINK)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.CHERRY_SAPLING)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block DARK_OAK_SAPLING = register(
      "dark_oak_sapling",
      p_360247_ -> new SaplingBlock(TreeGrower.DARK_OAK, p_360247_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block PALE_OAK_SAPLING = register(
      "pale_oak_sapling",
      p_359971_ -> new SaplingBlock(TreeGrower.PALE_OAK, p_359971_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block MANGROVE_PROPAGULE = register(
      "mangrove_propagule",
      p_360341_ -> new MangrovePropaguleBlock(TreeGrower.MANGROVE, p_360341_),
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
      "bedrock",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
         .isValidSpawn(Blocks::never)
   );
   public static final Block WATER = register(
      "water",
      p_360263_ -> new LiquidBlock(Fluids.WATER, p_360263_),
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
      "lava",
      p_360143_ -> new LiquidBlock(Fluids.LAVA, p_360143_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.FIRE)
         .replaceable()
         .noCollision()
         .randomTicks()
         .strength(100.0F)
         .lightLevel(p_50828_ -> 15)
         .pushReaction(PushReaction.DESTROY)
         .noLootTable()
         .liquid()
         .sound(SoundType.EMPTY)
   );
   public static final Block SAND = register(
      "sand",
      p_390915_ -> new SandBlock(new ColorRGBA(14406560), p_390915_),
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block SUSPICIOUS_SAND = register(
      "suspicious_sand",
      p_359994_ -> new BrushableBlock(SAND, SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND, p_359994_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.SAND)
         .instrument(NoteBlockInstrument.SNARE)
         .strength(0.25F)
         .sound(SoundType.SUSPICIOUS_SAND)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block RED_SAND = register(
      "red_sand",
      p_390920_ -> new SandBlock(new ColorRGBA(11098145), p_390920_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block GRAVEL = register(
      "gravel",
      p_359984_ -> new ColoredFallingBlock(new ColorRGBA(-8356741), p_359984_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.SNARE).strength(0.6F).sound(SoundType.GRAVEL)
   );
   public static final Block SUSPICIOUS_GRAVEL = register(
      "suspicious_gravel",
      p_359974_ -> new BrushableBlock(GRAVEL, SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL, p_359974_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.SNARE)
         .strength(0.25F)
         .sound(SoundType.SUSPICIOUS_GRAVEL)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block GOLD_ORE = register(
      "gold_ore",
      p_360383_ -> new DropExperienceBlock(ConstantInt.of(0), p_360383_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DEEPSLATE_GOLD_ORE = register(
      "deepslate_gold_ore",
      p_360286_ -> new DropExperienceBlock(ConstantInt.of(0), p_360286_),
      BlockBehaviour.Properties.ofLegacyCopy(GOLD_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block IRON_ORE = register(
      "iron_ore",
      p_360159_ -> new DropExperienceBlock(ConstantInt.of(0), p_360159_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DEEPSLATE_IRON_ORE = register(
      "deepslate_iron_ore",
      p_360257_ -> new DropExperienceBlock(ConstantInt.of(0), p_360257_),
      BlockBehaviour.Properties.ofLegacyCopy(IRON_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block COAL_ORE = register(
      "coal_ore",
      p_360258_ -> new DropExperienceBlock(UniformInt.of(0, 2), p_360258_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DEEPSLATE_COAL_ORE = register(
      "deepslate_coal_ore",
      p_360199_ -> new DropExperienceBlock(UniformInt.of(0, 2), p_360199_),
      BlockBehaviour.Properties.ofLegacyCopy(COAL_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block NETHER_GOLD_ORE = register(
      "nether_gold_ore",
      p_360069_ -> new DropExperienceBlock(UniformInt.of(0, 1), p_360069_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(3.0F, 3.0F)
         .sound(SoundType.NETHER_GOLD_ORE)
   );
   public static final Block OAK_LOG = register("oak_log", RotatedPillarBlock::new, logProperties(MapColor.WOOD, MapColor.PODZOL, SoundType.WOOD));
   public static final Block SPRUCE_LOG = register("spruce_log", RotatedPillarBlock::new, logProperties(MapColor.PODZOL, MapColor.COLOR_BROWN, SoundType.WOOD));
   public static final Block BIRCH_LOG = register("birch_log", RotatedPillarBlock::new, logProperties(MapColor.SAND, MapColor.QUARTZ, SoundType.WOOD));
   public static final Block JUNGLE_LOG = register("jungle_log", RotatedPillarBlock::new, logProperties(MapColor.DIRT, MapColor.PODZOL, SoundType.WOOD));
   public static final Block ACACIA_LOG = register("acacia_log", RotatedPillarBlock::new, logProperties(MapColor.COLOR_ORANGE, MapColor.STONE, SoundType.WOOD));
   public static final Block CHERRY_LOG = register(
      "cherry_log", RotatedPillarBlock::new, logProperties(MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_GRAY, SoundType.CHERRY_WOOD)
   );
   public static final Block DARK_OAK_LOG = register(
      "dark_oak_log", RotatedPillarBlock::new, logProperties(MapColor.COLOR_BROWN, MapColor.COLOR_BROWN, SoundType.WOOD)
   );
   public static final Block PALE_OAK_LOG = register(
      "pale_oak_log", RotatedPillarBlock::new, logProperties(PALE_OAK_PLANKS.defaultMapColor(), PALE_OAK_WOOD.defaultMapColor(), SoundType.WOOD)
   );
   public static final Block MANGROVE_LOG = register(
      "mangrove_log", RotatedPillarBlock::new, logProperties(MapColor.COLOR_RED, MapColor.PODZOL, SoundType.WOOD)
   );
   public static final Block MANGROVE_ROOTS = register(
      "mangrove_roots",
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
      "muddy_mangrove_roots",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(0.7F).sound(SoundType.MUDDY_MANGROVE_ROOTS)
   );
   public static final Block BAMBOO_BLOCK = register(
      "bamboo_block", RotatedPillarBlock::new, logProperties(MapColor.COLOR_YELLOW, MapColor.PLANT, SoundType.BAMBOO_WOOD)
   );
   public static final Block STRIPPED_SPRUCE_LOG = register(
      "stripped_spruce_log", RotatedPillarBlock::new, logProperties(MapColor.PODZOL, MapColor.PODZOL, SoundType.WOOD)
   );
   public static final Block STRIPPED_BIRCH_LOG = register(
      "stripped_birch_log", RotatedPillarBlock::new, logProperties(MapColor.SAND, MapColor.SAND, SoundType.WOOD)
   );
   public static final Block STRIPPED_JUNGLE_LOG = register(
      "stripped_jungle_log", RotatedPillarBlock::new, logProperties(MapColor.DIRT, MapColor.DIRT, SoundType.WOOD)
   );
   public static final Block STRIPPED_ACACIA_LOG = register(
      "stripped_acacia_log", RotatedPillarBlock::new, logProperties(MapColor.COLOR_ORANGE, MapColor.COLOR_ORANGE, SoundType.WOOD)
   );
   public static final Block STRIPPED_CHERRY_LOG = register(
      "stripped_cherry_log", RotatedPillarBlock::new, logProperties(MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_PINK, SoundType.CHERRY_WOOD)
   );
   public static final Block STRIPPED_DARK_OAK_LOG = register(
      "stripped_dark_oak_log", RotatedPillarBlock::new, logProperties(MapColor.COLOR_BROWN, MapColor.COLOR_BROWN, SoundType.WOOD)
   );
   public static final Block STRIPPED_PALE_OAK_LOG = register(
      "stripped_pale_oak_log", RotatedPillarBlock::new, logProperties(PALE_OAK_PLANKS.defaultMapColor(), PALE_OAK_PLANKS.defaultMapColor(), SoundType.WOOD)
   );
   public static final Block STRIPPED_OAK_LOG = register(
      "stripped_oak_log", RotatedPillarBlock::new, logProperties(MapColor.WOOD, MapColor.WOOD, SoundType.WOOD)
   );
   public static final Block STRIPPED_MANGROVE_LOG = register(
      "stripped_mangrove_log", RotatedPillarBlock::new, logProperties(MapColor.COLOR_RED, MapColor.COLOR_RED, SoundType.WOOD)
   );
   public static final Block STRIPPED_BAMBOO_BLOCK = register(
      "stripped_bamboo_block", RotatedPillarBlock::new, logProperties(MapColor.COLOR_YELLOW, MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD)
   );
   public static final Block OAK_WOOD = register(
      "oak_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block SPRUCE_WOOD = register(
      "spruce_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block BIRCH_WOOD = register(
      "birch_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block JUNGLE_WOOD = register(
      "jungle_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block ACACIA_WOOD = register(
      "acacia_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block CHERRY_WOOD = register(
      "cherry_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_GRAY)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F)
         .sound(SoundType.CHERRY_WOOD)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_WOOD = register(
      "dark_oak_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block MANGROVE_WOOD = register(
      "mangrove_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_OAK_WOOD = register(
      "stripped_oak_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_SPRUCE_WOOD = register(
      "stripped_spruce_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_BIRCH_WOOD = register(
      "stripped_birch_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_JUNGLE_WOOD = register(
      "stripped_jungle_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_ACACIA_WOOD = register(
      "stripped_acacia_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_CHERRY_WOOD = register(
      "stripped_cherry_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_PINK)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F)
         .sound(SoundType.CHERRY_WOOD)
         .ignitedByLava()
   );
   public static final Block STRIPPED_DARK_OAK_WOOD = register(
      "stripped_dark_oak_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STRIPPED_PALE_OAK_WOOD = register(
      "stripped_pale_oak_wood",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block STRIPPED_MANGROVE_WOOD = register(
      "stripped_mangrove_wood", RotatedPillarBlock::new, logProperties(MapColor.COLOR_RED, MapColor.COLOR_RED, SoundType.WOOD)
   );
   public static final Block OAK_LEAVES = register(
      "oak_leaves", p_390918_ -> new TintedParticleLeavesBlock(0.01F, p_390918_), leavesProperties(SoundType.GRASS)
   );
   public static final Block SPRUCE_LEAVES = register(
      "spruce_leaves", p_390914_ -> new TintedParticleLeavesBlock(0.01F, p_390914_), leavesProperties(SoundType.GRASS)
   );
   public static final Block BIRCH_LEAVES = register(
      "birch_leaves", p_390912_ -> new TintedParticleLeavesBlock(0.01F, p_390912_), leavesProperties(SoundType.GRASS)
   );
   public static final Block JUNGLE_LEAVES = register(
      "jungle_leaves", p_390909_ -> new TintedParticleLeavesBlock(0.01F, p_390909_), leavesProperties(SoundType.GRASS)
   );
   public static final Block ACACIA_LEAVES = register(
      "acacia_leaves", p_390911_ -> new TintedParticleLeavesBlock(0.01F, p_390911_), leavesProperties(SoundType.GRASS)
   );
   public static final Block CHERRY_LEAVES = register(
      "cherry_leaves",
      p_390917_ -> new UntintedParticleLeavesBlock(0.1F, ParticleTypes.CHERRY_LEAVES, p_390917_),
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
      "dark_oak_leaves", p_390908_ -> new TintedParticleLeavesBlock(0.01F, p_390908_), leavesProperties(SoundType.GRASS)
   );
   public static final Block PALE_OAK_LEAVES = register(
      "pale_oak_leaves",
      p_390913_ -> new UntintedParticleLeavesBlock(0.02F, ParticleTypes.PALE_OAK_LEAVES, p_390913_),
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
      "mangrove_leaves", p_390919_ -> new MangroveLeavesBlock(0.01F, p_390919_), leavesProperties(SoundType.GRASS)
   );
   public static final Block AZALEA_LEAVES = register(
      "azalea_leaves",
      p_390910_ -> new UntintedParticleLeavesBlock(0.01F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, -9399763), p_390910_),
      leavesProperties(SoundType.AZALEA_LEAVES)
   );
   public static final Block FLOWERING_AZALEA_LEAVES = register(
      "flowering_azalea_leaves",
      p_390916_ -> new UntintedParticleLeavesBlock(0.01F, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, -9399763), p_390916_),
      leavesProperties(SoundType.AZALEA_LEAVES)
   );
   public static final Block SPONGE = register(
      "sponge", SpongeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.SPONGE)
   );
   public static final Block WET_SPONGE = register(
      "wet_sponge", WetSpongeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.WET_SPONGE)
   );
   public static final Block GLASS = register(
      "glass",
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
      "lapis_ore",
      p_360015_ -> new DropExperienceBlock(UniformInt.of(2, 5), p_360015_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DEEPSLATE_LAPIS_ORE = register(
      "deepslate_lapis_ore",
      p_360268_ -> new DropExperienceBlock(UniformInt.of(2, 5), p_360268_),
      BlockBehaviour.Properties.ofLegacyCopy(LAPIS_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block LAPIS_BLOCK = register(
      "lapis_block", BlockBehaviour.Properties.of().mapColor(MapColor.LAPIS).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DISPENSER = register(
      "dispenser",
      DispenserBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F)
   );
   public static final Block SANDSTONE = register(
      "sandstone", BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block CHISELED_SANDSTONE = register(
      "chiseled_sandstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block CUT_SANDSTONE = register(
      "cut_sandstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block NOTE_BLOCK = register(
      "note_block",
      NoteBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava()
   );
   public static final Block WHITE_BED = registerBed("white_bed", DyeColor.WHITE);
   public static final Block ORANGE_BED = registerBed("orange_bed", DyeColor.ORANGE);
   public static final Block MAGENTA_BED = registerBed("magenta_bed", DyeColor.MAGENTA);
   public static final Block LIGHT_BLUE_BED = registerBed("light_blue_bed", DyeColor.LIGHT_BLUE);
   public static final Block YELLOW_BED = registerBed("yellow_bed", DyeColor.YELLOW);
   public static final Block LIME_BED = registerBed("lime_bed", DyeColor.LIME);
   public static final Block PINK_BED = registerBed("pink_bed", DyeColor.PINK);
   public static final Block GRAY_BED = registerBed("gray_bed", DyeColor.GRAY);
   public static final Block LIGHT_GRAY_BED = registerBed("light_gray_bed", DyeColor.LIGHT_GRAY);
   public static final Block CYAN_BED = registerBed("cyan_bed", DyeColor.CYAN);
   public static final Block PURPLE_BED = registerBed("purple_bed", DyeColor.PURPLE);
   public static final Block BLUE_BED = registerBed("blue_bed", DyeColor.BLUE);
   public static final Block BROWN_BED = registerBed("brown_bed", DyeColor.BROWN);
   public static final Block GREEN_BED = registerBed("green_bed", DyeColor.GREEN);
   public static final Block RED_BED = registerBed("red_bed", DyeColor.RED);
   public static final Block BLACK_BED = registerBed("black_bed", DyeColor.BLACK);
   public static final Block POWERED_RAIL = register(
      "powered_rail", PoweredRailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
   );
   public static final Block DETECTOR_RAIL = register(
      "detector_rail", DetectorRailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
   );
   public static final Block STICKY_PISTON = register("sticky_piston", p_360185_ -> new PistonBaseBlock(true, p_360185_), pistonProperties());
   public static final Block COBWEB = register(
      "cobweb",
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
      "short_grass",
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
      "fern",
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
      "dead_bush",
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
      "bush",
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
      "short_dry_grass",
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
      "tall_dry_grass",
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
      "seagrass",
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
      "tall_seagrass",
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
   public static final Block PISTON = register("piston", p_360326_ -> new PistonBaseBlock(false, p_360326_), pistonProperties());
   public static final Block PISTON_HEAD = register(
      "piston_head",
      PistonHeadBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F).noLootTable().pushReaction(PushReaction.BLOCK)
   );
   public static final Block WHITE_WOOL = register(
      "white_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block ORANGE_WOOL = register(
      "orange_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block MAGENTA_WOOL = register(
      "magenta_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_MAGENTA)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block LIGHT_BLUE_WOOL = register(
      "light_blue_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_LIGHT_BLUE)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block YELLOW_WOOL = register(
      "yellow_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block LIME_WOOL = register(
      "lime_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_LIGHT_GREEN)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block PINK_WOOL = register(
      "pink_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block GRAY_WOOL = register(
      "gray_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block LIGHT_GRAY_WOOL = register(
      "light_gray_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_LIGHT_GRAY)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block CYAN_WOOL = register(
      "cyan_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block PURPLE_WOOL = register(
      "purple_wool",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_PURPLE)
         .instrument(NoteBlockInstrument.GUITAR)
         .strength(0.8F)
         .sound(SoundType.WOOL)
         .ignitedByLava()
   );
   public static final Block BLUE_WOOL = register(
      "blue_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block BROWN_WOOL = register(
      "brown_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block GREEN_WOOL = register(
      "green_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block RED_WOOL = register(
      "red_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block BLACK_WOOL = register(
      "black_wool",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.GUITAR).strength(0.8F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block MOVING_PISTON = register(
      "moving_piston",
      MovingPistonBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .forceSolidOn()
         .strength(-1.0F)
         .dynamicShape()
         .noLootTable()
         .noOcclusion()
         .isRedstoneConductor(Blocks::never)
         .isSuffocating(Blocks::never)
         .isViewBlocking(Blocks::never)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block DANDELION = register(
      "dandelion",
      p_360079_ -> new FlowerBlock(MobEffects.SATURATION, 0.35F, p_360079_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block TORCHFLOWER = register(
      "torchflower",
      p_360366_ -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, p_360366_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block POPPY = register(
      "poppy",
      p_360231_ -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, p_360231_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BLUE_ORCHID = register(
      "blue_orchid",
      p_360180_ -> new FlowerBlock(MobEffects.SATURATION, 0.35F, p_360180_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block ALLIUM = register(
      "allium",
      p_360129_ -> new FlowerBlock(MobEffects.FIRE_RESISTANCE, 3.0F, p_360129_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block AZURE_BLUET = register(
      "azure_bluet",
      p_360348_ -> new FlowerBlock(MobEffects.BLINDNESS, 11.0F, p_360348_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block RED_TULIP = register(
      "red_tulip",
      p_360062_ -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p_360062_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block ORANGE_TULIP = register(
      "orange_tulip",
      p_359997_ -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p_359997_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block WHITE_TULIP = register(
      "white_tulip",
      p_360081_ -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p_360081_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block PINK_TULIP = register(
      "pink_tulip",
      p_360233_ -> new FlowerBlock(MobEffects.WEAKNESS, 7.0F, p_360233_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block OXEYE_DAISY = register(
      "oxeye_daisy",
      p_360135_ -> new FlowerBlock(MobEffects.REGENERATION, 7.0F, p_360135_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CORNFLOWER = register(
      "cornflower",
      p_390907_ -> new FlowerBlock(MobEffects.JUMP_BOOST, 5.0F, p_390907_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block WITHER_ROSE = register(
      "wither_rose",
      p_360044_ -> new WitherRoseBlock(MobEffects.WITHER, 7.0F, p_360044_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block LILY_OF_THE_VALLEY = register(
      "lily_of_the_valley",
      p_360161_ -> new FlowerBlock(MobEffects.POISON, 11.0F, p_360161_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .instabreak()
         .sound(SoundType.GRASS)
         .offsetType(BlockBehaviour.OffsetType.XZ)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BROWN_MUSHROOM = register(
      "brown_mushroom",
      p_360171_ -> new MushroomBlock(TreeFeatures.HUGE_BROWN_MUSHROOM, p_360171_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BROWN)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .lightLevel(p_50892_ -> 1)
         .hasPostProcess(Blocks::always)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block RED_MUSHROOM = register(
      "red_mushroom",
      p_359970_ -> new MushroomBlock(TreeFeatures.HUGE_RED_MUSHROOM, p_359970_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_RED)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.GRASS)
         .hasPostProcess(Blocks::always)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block GOLD_BLOCK = register(
      "gold_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.GOLD)
         .instrument(NoteBlockInstrument.BELL)
         .requiresCorrectToolForDrops()
         .strength(3.0F, 6.0F)
         .sound(SoundType.METAL)
   );
   public static final Block IRON_BLOCK = register(
      "iron_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 6.0F)
         .sound(SoundType.IRON)
   );
   public static final Block BRICKS = register(
      "bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block TNT = register(
      "tnt",
      TntBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).instabreak().sound(SoundType.GRASS).ignitedByLava().isRedstoneConductor(Blocks::never)
   );
   public static final Block BOOKSHELF = register(
      "bookshelf",
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block CHISELED_BOOKSHELF = register(
      "chiseled_bookshelf",
      ChiseledBookShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .instrument(NoteBlockInstrument.BASS)
         .strength(1.5F)
         .sound(SoundType.CHISELED_BOOKSHELF)
         .ignitedByLava()
   );
   public static final Block ACACIA_SHELF = register(
      "acacia_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(ACACIA_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block BAMBOO_SHELF = register(
      "bamboo_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(BAMBOO_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block BIRCH_SHELF = register(
      "birch_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(BIRCH_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block CHERRY_SHELF = register(
      "cherry_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(CHERRY_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block CRIMSON_SHELF = register(
      "crimson_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.CRIMSON_STEM)
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block DARK_OAK_SHELF = register(
      "dark_oak_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DARK_OAK_LOG.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block JUNGLE_SHELF = register(
      "jungle_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(JUNGLE_LOG.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block MANGROVE_SHELF = register(
      "mangrove_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MANGROVE_LOG.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block OAK_SHELF = register(
      "oak_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block PALE_OAK_SHELF = register(
      "pale_oak_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block SPRUCE_SHELF = register(
      "spruce_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(SPRUCE_LOG.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block WARPED_SHELF = register(
      "warped_shelf",
      ShelfBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WARPED_STEM)
         .instrument(NoteBlockInstrument.BASS)
         .sound(SoundType.SHELF)
         .ignitedByLava()
         .strength(2.0F, 3.0F)
   );
   public static final Block MOSSY_COBBLESTONE = register(
      "mossy_cobblestone",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block OBSIDIAN = register(
      "obsidian",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(50.0F, 1200.0F)
   );
   public static final Block TORCH = register(
      "torch",
      p_360008_ -> new TorchBlock(ParticleTypes.FLAME, p_360008_),
      BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(p_50755_ -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block WALL_TORCH = register(
      "wall_torch",
      p_360318_ -> new WallTorchBlock(ParticleTypes.FLAME, p_360318_),
      wallVariant(TORCH, true).noCollision().instabreak().lightLevel(p_422061_ -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block FIRE = register(
      "fire",
      FireBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.FIRE)
         .replaceable()
         .noCollision()
         .instabreak()
         .lightLevel(p_422060_ -> 15)
         .sound(SoundType.WOOL)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block SOUL_FIRE = register(
      "soul_fire",
      SoulFireBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_LIGHT_BLUE)
         .replaceable()
         .noCollision()
         .instabreak()
         .lightLevel(p_422059_ -> 10)
         .sound(SoundType.WOOL)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block SPAWNER = register(
      "spawner",
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
      "creaking_heart",
      CreakingHeartBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).strength(10.0F).sound(SoundType.CREAKING_HEART)
   );
   public static final Block OAK_STAIRS = registerLegacyStair("oak_stairs", OAK_PLANKS);
   public static final Block CHEST = register(
      "chest",
      p_422051_ -> new ChestBlock(() -> BlockEntityType.CHEST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, p_422051_),
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block REDSTONE_WIRE = register(
      "redstone_wire", RedStoneWireBlock::new, BlockBehaviour.Properties.of().noCollision().instabreak().pushReaction(PushReaction.DESTROY)
   );
   public static final Block DIAMOND_ORE = register(
      "diamond_ore",
      p_360350_ -> new DropExperienceBlock(UniformInt.of(3, 7), p_360350_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DEEPSLATE_DIAMOND_ORE = register(
      "deepslate_diamond_ore",
      p_360045_ -> new DropExperienceBlock(UniformInt.of(3, 7), p_360045_),
      BlockBehaviour.Properties.ofLegacyCopy(DIAMOND_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block DIAMOND_BLOCK = register(
      "diamond_block", BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)
   );
   public static final Block CRAFTING_TABLE = register(
      "crafting_table",
      CraftingTableBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block WHEAT = register(
      "wheat",
      CropBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(p_360397_ -> p_360397_.getValue(CropBlock.AGE) >= 6 ? MapColor.COLOR_YELLOW : MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.CROP)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block FARMLAND = register(
      "farmland",
      FarmBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.DIRT)
         .randomTicks()
         .strength(0.6F)
         .sound(SoundType.GRAVEL)
         .isViewBlocking(Blocks::always)
         .isSuffocating(Blocks::always)
   );
   public static final Block FURNACE = register(
      "furnace",
      FurnaceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(3.5F)
         .lightLevel(litBlockEmission(13))
   );
   public static final Block OAK_SIGN = register(
      "oak_sign",
      p_360342_ -> new StandingSignBlock(WoodType.OAK, p_360342_),
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
   );
   public static final Block SPRUCE_SIGN = register(
      "spruce_sign",
      p_360063_ -> new StandingSignBlock(WoodType.SPRUCE, p_360063_),
      BlockBehaviour.Properties.of()
         .mapColor(SPRUCE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BIRCH_SIGN = register(
      "birch_sign",
      p_360306_ -> new StandingSignBlock(WoodType.BIRCH, p_360306_),
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
   );
   public static final Block ACACIA_SIGN = register(
      "acacia_sign",
      p_360107_ -> new StandingSignBlock(WoodType.ACACIA, p_360107_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block CHERRY_SIGN = register(
      "cherry_sign",
      p_360206_ -> new StandingSignBlock(WoodType.CHERRY, p_360206_),
      BlockBehaviour.Properties.of()
         .mapColor(CHERRY_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block JUNGLE_SIGN = register(
      "jungle_sign",
      p_360295_ -> new StandingSignBlock(WoodType.JUNGLE, p_360295_),
      BlockBehaviour.Properties.of()
         .mapColor(JUNGLE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_SIGN = register(
      "dark_oak_sign",
      p_359982_ -> new StandingSignBlock(WoodType.DARK_OAK, p_359982_),
      BlockBehaviour.Properties.of()
         .mapColor(DARK_OAK_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_SIGN = register(
      "pale_oak_sign",
      p_360292_ -> new StandingSignBlock(WoodType.PALE_OAK, p_360292_),
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block MANGROVE_SIGN = register(
      "mangrove_sign",
      p_360248_ -> new StandingSignBlock(WoodType.MANGROVE, p_360248_),
      BlockBehaviour.Properties.of()
         .mapColor(MANGROVE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BAMBOO_SIGN = register(
      "bamboo_sign",
      p_360368_ -> new StandingSignBlock(WoodType.BAMBOO, p_360368_),
      BlockBehaviour.Properties.of()
         .mapColor(BAMBOO_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block OAK_DOOR = register(
      "oak_door",
      p_360232_ -> new DoorBlock(BlockSetType.OAK, p_360232_),
      BlockBehaviour.Properties.of()
         .mapColor(OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block LADDER = register(
      "ladder",
      LadderBlock::new,
      BlockBehaviour.Properties.of().forceSolidOff().strength(0.4F).sound(SoundType.LADDER).noOcclusion().pushReaction(PushReaction.DESTROY)
   );
   public static final Block RAIL = register("rail", RailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL));
   public static final Block COBBLESTONE_STAIRS = registerLegacyStair("cobblestone_stairs", COBBLESTONE);
   public static final Block OAK_WALL_SIGN = register(
      "oak_wall_sign",
      p_360145_ -> new WallSignBlock(WoodType.OAK, p_360145_),
      wallVariant(OAK_SIGN, true).mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
   );
   public static final Block SPRUCE_WALL_SIGN = register(
      "spruce_wall_sign",
      p_360296_ -> new WallSignBlock(WoodType.SPRUCE, p_360296_),
      wallVariant(SPRUCE_SIGN, true)
         .mapColor(SPRUCE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BIRCH_WALL_SIGN = register(
      "birch_wall_sign",
      p_359986_ -> new WallSignBlock(WoodType.BIRCH, p_359986_),
      wallVariant(BIRCH_SIGN, true).mapColor(MapColor.SAND).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
   );
   public static final Block ACACIA_WALL_SIGN = register(
      "acacia_wall_sign",
      p_360117_ -> new WallSignBlock(WoodType.ACACIA, p_360117_),
      wallVariant(ACACIA_SIGN, true)
         .mapColor(MapColor.COLOR_ORANGE)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block CHERRY_WALL_SIGN = register(
      "cherry_wall_sign",
      p_360211_ -> new WallSignBlock(WoodType.CHERRY, p_360211_),
      wallVariant(CHERRY_SIGN, true)
         .mapColor(CHERRY_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block JUNGLE_WALL_SIGN = register(
      "jungle_wall_sign",
      p_360219_ -> new WallSignBlock(WoodType.JUNGLE, p_360219_),
      wallVariant(JUNGLE_SIGN, true)
         .mapColor(JUNGLE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_WALL_SIGN = register(
      "dark_oak_wall_sign",
      p_359967_ -> new WallSignBlock(WoodType.DARK_OAK, p_359967_),
      wallVariant(DARK_OAK_SIGN, true)
         .mapColor(DARK_OAK_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_WALL_SIGN = register(
      "pale_oak_wall_sign",
      p_360038_ -> new WallSignBlock(WoodType.PALE_OAK, p_360038_),
      wallVariant(PALE_OAK_SIGN, true)
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block MANGROVE_WALL_SIGN = register(
      "mangrove_wall_sign",
      p_360028_ -> new WallSignBlock(WoodType.MANGROVE, p_360028_),
      wallVariant(MANGROVE_SIGN, true)
         .mapColor(MANGROVE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BAMBOO_WALL_SIGN = register(
      "bamboo_wall_sign",
      p_360359_ -> new WallSignBlock(WoodType.BAMBOO, p_360359_),
      wallVariant(BAMBOO_SIGN, true)
         .mapColor(BAMBOO_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block OAK_HANGING_SIGN = register(
      "oak_hanging_sign",
      p_360365_ -> new CeilingHangingSignBlock(WoodType.OAK, p_360365_),
      BlockBehaviour.Properties.of()
         .mapColor(OAK_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block SPRUCE_HANGING_SIGN = register(
      "spruce_hanging_sign",
      p_360054_ -> new CeilingHangingSignBlock(WoodType.SPRUCE, p_360054_),
      BlockBehaviour.Properties.of()
         .mapColor(SPRUCE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BIRCH_HANGING_SIGN = register(
      "birch_hanging_sign",
      p_360077_ -> new CeilingHangingSignBlock(WoodType.BIRCH, p_360077_),
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava()
   );
   public static final Block ACACIA_HANGING_SIGN = register(
      "acacia_hanging_sign",
      p_360183_ -> new CeilingHangingSignBlock(WoodType.ACACIA, p_360183_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block CHERRY_HANGING_SIGN = register(
      "cherry_hanging_sign",
      p_360147_ -> new CeilingHangingSignBlock(WoodType.CHERRY, p_360147_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_PINK)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block JUNGLE_HANGING_SIGN = register(
      "jungle_hanging_sign",
      p_360329_ -> new CeilingHangingSignBlock(WoodType.JUNGLE, p_360329_),
      BlockBehaviour.Properties.of()
         .mapColor(JUNGLE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_HANGING_SIGN = register(
      "dark_oak_hanging_sign",
      p_360346_ -> new CeilingHangingSignBlock(WoodType.DARK_OAK, p_360346_),
      BlockBehaviour.Properties.of()
         .mapColor(DARK_OAK_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_HANGING_SIGN = register(
      "pale_oak_hanging_sign",
      p_360120_ -> new CeilingHangingSignBlock(WoodType.PALE_OAK, p_360120_),
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block CRIMSON_HANGING_SIGN = register(
      "crimson_hanging_sign",
      p_360277_ -> new CeilingHangingSignBlock(WoodType.CRIMSON, p_360277_),
      BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
   );
   public static final Block WARPED_HANGING_SIGN = register(
      "warped_hanging_sign",
      p_359975_ -> new CeilingHangingSignBlock(WoodType.WARPED, p_359975_),
      BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
   );
   public static final Block MANGROVE_HANGING_SIGN = register(
      "mangrove_hanging_sign",
      p_360078_ -> new CeilingHangingSignBlock(WoodType.MANGROVE, p_360078_),
      BlockBehaviour.Properties.of()
         .mapColor(MANGROVE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BAMBOO_HANGING_SIGN = register(
      "bamboo_hanging_sign",
      p_360377_ -> new CeilingHangingSignBlock(WoodType.BAMBOO, p_360377_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block OAK_WALL_HANGING_SIGN = register(
      "oak_wall_hanging_sign",
      p_359979_ -> new WallHangingSignBlock(WoodType.OAK, p_359979_),
      wallVariant(OAK_HANGING_SIGN, true)
         .mapColor(OAK_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block SPRUCE_WALL_HANGING_SIGN = register(
      "spruce_wall_hanging_sign",
      p_360396_ -> new WallHangingSignBlock(WoodType.SPRUCE, p_360396_),
      wallVariant(SPRUCE_HANGING_SIGN, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block BIRCH_WALL_HANGING_SIGN = register(
      "birch_wall_hanging_sign",
      p_360193_ -> new WallHangingSignBlock(WoodType.BIRCH, p_360193_),
      wallVariant(BIRCH_HANGING_SIGN, true)
         .mapColor(MapColor.SAND)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block ACACIA_WALL_HANGING_SIGN = register(
      "acacia_wall_hanging_sign",
      p_359980_ -> new WallHangingSignBlock(WoodType.ACACIA, p_359980_),
      wallVariant(ACACIA_HANGING_SIGN, true)
         .mapColor(MapColor.COLOR_ORANGE)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block CHERRY_WALL_HANGING_SIGN = register(
      "cherry_wall_hanging_sign",
      p_360362_ -> new WallHangingSignBlock(WoodType.CHERRY, p_360362_),
      wallVariant(CHERRY_HANGING_SIGN, true)
         .mapColor(MapColor.TERRACOTTA_PINK)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block JUNGLE_WALL_HANGING_SIGN = register(
      "jungle_wall_hanging_sign",
      p_360046_ -> new WallHangingSignBlock(WoodType.JUNGLE, p_360046_),
      wallVariant(JUNGLE_HANGING_SIGN, true)
         .mapColor(JUNGLE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_WALL_HANGING_SIGN = register(
      "dark_oak_wall_hanging_sign",
      p_360179_ -> new WallHangingSignBlock(WoodType.DARK_OAK, p_360179_),
      wallVariant(DARK_OAK_HANGING_SIGN, true)
         .mapColor(DARK_OAK_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_WALL_HANGING_SIGN = register(
      "pale_oak_wall_hanging_sign",
      p_360316_ -> new WallHangingSignBlock(WoodType.PALE_OAK, p_360316_),
      wallVariant(PALE_OAK_HANGING_SIGN, true)
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block MANGROVE_WALL_HANGING_SIGN = register(
      "mangrove_wall_hanging_sign",
      p_360039_ -> new WallHangingSignBlock(WoodType.MANGROVE, p_360039_),
      wallVariant(MANGROVE_HANGING_SIGN, true)
         .mapColor(MANGROVE_LOG.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block CRIMSON_WALL_HANGING_SIGN = register(
      "crimson_wall_hanging_sign",
      p_359987_ -> new WallHangingSignBlock(WoodType.CRIMSON, p_359987_),
      wallVariant(CRIMSON_HANGING_SIGN, true).mapColor(MapColor.CRIMSON_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
   );
   public static final Block WARPED_WALL_HANGING_SIGN = register(
      "warped_wall_hanging_sign",
      p_360005_ -> new WallHangingSignBlock(WoodType.WARPED, p_360005_),
      wallVariant(WARPED_HANGING_SIGN, true).mapColor(MapColor.WARPED_STEM).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F)
   );
   public static final Block BAMBOO_WALL_HANGING_SIGN = register(
      "bamboo_wall_hanging_sign",
      p_360242_ -> new WallHangingSignBlock(WoodType.BAMBOO, p_360242_),
      wallVariant(BAMBOO_HANGING_SIGN, true)
         .mapColor(MapColor.COLOR_YELLOW)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .ignitedByLava()
   );
   public static final Block LEVER = register(
      "lever", LeverBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.5F).sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
   );
   public static final Block STONE_PRESSURE_PLATE = register(
      "stone_pressure_plate",
      p_360071_ -> new PressurePlateBlock(BlockSetType.STONE, p_360071_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .noCollision()
         .strength(0.5F)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block IRON_DOOR = register(
      "iron_door",
      p_360369_ -> new DoorBlock(BlockSetType.IRON, p_360369_),
      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F).noOcclusion().pushReaction(PushReaction.DESTROY)
   );
   public static final Block OAK_PRESSURE_PLATE = register(
      "oak_pressure_plate",
      p_360098_ -> new PressurePlateBlock(BlockSetType.OAK, p_360098_),
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
      "spruce_pressure_plate",
      p_360378_ -> new PressurePlateBlock(BlockSetType.SPRUCE, p_360378_),
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
      "birch_pressure_plate",
      p_360299_ -> new PressurePlateBlock(BlockSetType.BIRCH, p_360299_),
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
      "jungle_pressure_plate",
      p_360146_ -> new PressurePlateBlock(BlockSetType.JUNGLE, p_360146_),
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
      "acacia_pressure_plate",
      p_360013_ -> new PressurePlateBlock(BlockSetType.ACACIA, p_360013_),
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
      "cherry_pressure_plate",
      p_360209_ -> new PressurePlateBlock(BlockSetType.CHERRY, p_360209_),
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
      "dark_oak_pressure_plate",
      p_360184_ -> new PressurePlateBlock(BlockSetType.DARK_OAK, p_360184_),
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
      "pale_oak_pressure_plate",
      p_360024_ -> new PressurePlateBlock(BlockSetType.PALE_OAK, p_360024_),
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
      "mangrove_pressure_plate",
      p_360168_ -> new PressurePlateBlock(BlockSetType.MANGROVE, p_360168_),
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
      "bamboo_pressure_plate",
      p_360084_ -> new PressurePlateBlock(BlockSetType.BAMBOO, p_360084_),
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
      "redstone_ore",
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
      "deepslate_redstone_ore",
      RedStoneOreBlock::new,
      BlockBehaviour.Properties.ofLegacyCopy(REDSTONE_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block REDSTONE_TORCH = register(
      "redstone_torch",
      RedstoneTorchBlock::new,
      BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(litBlockEmission(7)).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block REDSTONE_WALL_TORCH = register(
      "redstone_wall_torch",
      RedstoneWallTorchBlock::new,
      wallVariant(REDSTONE_TORCH, true).noCollision().instabreak().lightLevel(litBlockEmission(7)).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block STONE_BUTTON = register("stone_button", p_360262_ -> new ButtonBlock(BlockSetType.STONE, 20, p_360262_), buttonProperties());
   public static final Block SNOW = register(
      "snow",
      SnowLayerBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.SNOW)
         .replaceable()
         .forceSolidOff()
         .randomTicks()
         .strength(0.1F)
         .requiresCorrectToolForDrops()
         .sound(SoundType.SNOW)
         .isViewBlocking((p_187417_, p_187418_, p_187419_) -> p_187417_.getValue(SnowLayerBlock.LAYERS) >= 8)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block ICE = register(
      "ice",
      IceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.ICE)
         .friction(0.98F)
         .randomTicks()
         .strength(0.5F)
         .sound(SoundType.GLASS)
         .noOcclusion()
         .isValidSpawn((p_187426_, p_187427_, p_187428_, p_187429_) -> p_187429_ == EntityType.POLAR_BEAR)
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block SNOW_BLOCK = register(
      "snow_block", BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).requiresCorrectToolForDrops().strength(0.2F).sound(SoundType.SNOW)
   );
   public static final Block CACTUS = register(
      "cactus",
      CactusBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().strength(0.4F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
   );
   public static final Block CACTUS_FLOWER = register(
      "cactus_flower",
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
      "clay", BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).instrument(NoteBlockInstrument.FLUTE).strength(0.6F).sound(SoundType.GRAVEL)
   );
   public static final Block SUGAR_CANE = register(
      "sugar_cane",
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
      "jukebox",
      JukeboxBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F, 6.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block OAK_FENCE = register(
      "oak_fence",
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
      "netherrack",
      NetherrackBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(0.4F)
         .sound(SoundType.NETHERRACK)
   );
   public static final Block SOUL_SAND = register(
      "soul_sand",
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
   );
   public static final Block SOUL_SOIL = register(
      "soul_soil", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.SOUL_SOIL)
   );
   public static final Block BASALT = register(
      "basalt",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
         .sound(SoundType.BASALT)
   );
   public static final Block POLISHED_BASALT = register(
      "polished_basalt",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
         .sound(SoundType.BASALT)
   );
   public static final Block SOUL_TORCH = register(
      "soul_torch",
      p_360160_ -> new TorchBlock(ParticleTypes.SOUL_FIRE_FLAME, p_360160_),
      BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(p_50886_ -> 10).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block SOUL_WALL_TORCH = register(
      "soul_wall_torch",
      p_360198_ -> new WallTorchBlock(ParticleTypes.SOUL_FIRE_FLAME, p_360198_),
      wallVariant(SOUL_TORCH, true).noCollision().instabreak().lightLevel(p_152607_ -> 10).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block COPPER_TORCH = register(
      "copper_torch",
      p_422050_ -> new TorchBlock(ParticleTypes.COPPER_FIRE_FLAME, p_422050_),
      BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(p_152605_ -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block COPPER_WALL_TORCH = register(
      "copper_wall_torch",
      p_422055_ -> new WallTorchBlock(ParticleTypes.COPPER_FIRE_FLAME, p_422055_),
      wallVariant(COPPER_TORCH, true).noCollision().instabreak().lightLevel(p_50884_ -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block GLOWSTONE = register(
      "glowstone",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.SAND)
         .instrument(NoteBlockInstrument.PLING)
         .strength(0.3F)
         .sound(SoundType.GLASS)
         .lightLevel(p_50876_ -> 15)
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block NETHER_PORTAL = register(
      "nether_portal",
      NetherPortalBlock::new,
      BlockBehaviour.Properties.of()
         .noCollision()
         .randomTicks()
         .strength(-1.0F)
         .sound(SoundType.GLASS)
         .lightLevel(p_50874_ -> 11)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block CARVED_PUMPKIN = register(
      "carved_pumpkin",
      CarvedPumpkinBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .isValidSpawn(Blocks::always)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block JACK_O_LANTERN = register(
      "jack_o_lantern",
      CarvedPumpkinBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .lightLevel(p_50872_ -> 15)
         .isValidSpawn(Blocks::always)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CAKE = register(
      "cake", CakeBlock::new, BlockBehaviour.Properties.of().forceSolidOn().strength(0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
   );
   public static final Block REPEATER = register(
      "repeater", RepeaterBlock::new, BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
   );
   public static final Block WHITE_STAINED_GLASS = registerStainedGlass("white_stained_glass", DyeColor.WHITE);
   public static final Block ORANGE_STAINED_GLASS = registerStainedGlass("orange_stained_glass", DyeColor.ORANGE);
   public static final Block MAGENTA_STAINED_GLASS = registerStainedGlass("magenta_stained_glass", DyeColor.MAGENTA);
   public static final Block LIGHT_BLUE_STAINED_GLASS = registerStainedGlass("light_blue_stained_glass", DyeColor.LIGHT_BLUE);
   public static final Block YELLOW_STAINED_GLASS = registerStainedGlass("yellow_stained_glass", DyeColor.YELLOW);
   public static final Block LIME_STAINED_GLASS = registerStainedGlass("lime_stained_glass", DyeColor.LIME);
   public static final Block PINK_STAINED_GLASS = registerStainedGlass("pink_stained_glass", DyeColor.PINK);
   public static final Block GRAY_STAINED_GLASS = registerStainedGlass("gray_stained_glass", DyeColor.GRAY);
   public static final Block LIGHT_GRAY_STAINED_GLASS = registerStainedGlass("light_gray_stained_glass", DyeColor.LIGHT_GRAY);
   public static final Block CYAN_STAINED_GLASS = registerStainedGlass("cyan_stained_glass", DyeColor.CYAN);
   public static final Block PURPLE_STAINED_GLASS = registerStainedGlass("purple_stained_glass", DyeColor.PURPLE);
   public static final Block BLUE_STAINED_GLASS = registerStainedGlass("blue_stained_glass", DyeColor.BLUE);
   public static final Block BROWN_STAINED_GLASS = registerStainedGlass("brown_stained_glass", DyeColor.BROWN);
   public static final Block GREEN_STAINED_GLASS = registerStainedGlass("green_stained_glass", DyeColor.GREEN);
   public static final Block RED_STAINED_GLASS = registerStainedGlass("red_stained_glass", DyeColor.RED);
   public static final Block BLACK_STAINED_GLASS = registerStainedGlass("black_stained_glass", DyeColor.BLACK);
   public static final Block OAK_TRAPDOOR = register(
      "oak_trapdoor",
      p_360278_ -> new TrapDoorBlock(BlockSetType.OAK, p_360278_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block SPRUCE_TRAPDOOR = register(
      "spruce_trapdoor",
      p_360040_ -> new TrapDoorBlock(BlockSetType.SPRUCE, p_360040_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PODZOL)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block BIRCH_TRAPDOOR = register(
      "birch_trapdoor",
      p_360212_ -> new TrapDoorBlock(BlockSetType.BIRCH, p_360212_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.SAND)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block JUNGLE_TRAPDOOR = register(
      "jungle_trapdoor",
      p_360006_ -> new TrapDoorBlock(BlockSetType.JUNGLE, p_360006_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.DIRT)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block ACACIA_TRAPDOOR = register(
      "acacia_trapdoor",
      p_360109_ -> new TrapDoorBlock(BlockSetType.ACACIA, p_360109_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block CHERRY_TRAPDOOR = register(
      "cherry_trapdoor",
      p_360150_ -> new TrapDoorBlock(BlockSetType.CHERRY, p_360150_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_WHITE)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_TRAPDOOR = register(
      "dark_oak_trapdoor",
      p_360205_ -> new TrapDoorBlock(BlockSetType.DARK_OAK, p_360205_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BROWN)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_TRAPDOOR = register(
      "pale_oak_trapdoor",
      p_360399_ -> new TrapDoorBlock(BlockSetType.PALE_OAK, p_360399_),
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block MANGROVE_TRAPDOOR = register(
      "mangrove_trapdoor",
      p_359999_ -> new TrapDoorBlock(BlockSetType.MANGROVE, p_359999_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_RED)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block BAMBOO_TRAPDOOR = register(
      "bamboo_trapdoor",
      p_360152_ -> new TrapDoorBlock(BlockSetType.BAMBOO, p_360152_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
         .ignitedByLava()
   );
   public static final Block STONE_BRICKS = register(
      "stone_bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block MOSSY_STONE_BRICKS = register(
      "mossy_stone_bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block CRACKED_STONE_BRICKS = register(
      "cracked_stone_bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block CHISELED_STONE_BRICKS = register(
      "chiseled_stone_bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block PACKED_MUD = register("packed_mud", BlockBehaviour.Properties.ofLegacyCopy(DIRT).strength(1.0F, 3.0F).sound(SoundType.PACKED_MUD));
   public static final Block MUD_BRICKS = register(
      "mud_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 3.0F)
         .sound(SoundType.MUD_BRICKS)
   );
   public static final Block INFESTED_STONE = register(
      "infested_stone", p_360101_ -> new InfestedBlock(STONE, p_360101_), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
   );
   public static final Block INFESTED_COBBLESTONE = register(
      "infested_cobblestone", p_360148_ -> new InfestedBlock(COBBLESTONE, p_360148_), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
   );
   public static final Block INFESTED_STONE_BRICKS = register(
      "infested_stone_bricks", p_360293_ -> new InfestedBlock(STONE_BRICKS, p_360293_), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
   );
   public static final Block INFESTED_MOSSY_STONE_BRICKS = register(
      "infested_mossy_stone_bricks", p_360004_ -> new InfestedBlock(MOSSY_STONE_BRICKS, p_360004_), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
   );
   public static final Block INFESTED_CRACKED_STONE_BRICKS = register(
      "infested_cracked_stone_bricks", p_360177_ -> new InfestedBlock(CRACKED_STONE_BRICKS, p_360177_), BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
   );
   public static final Block INFESTED_CHISELED_STONE_BRICKS = register(
      "infested_chiseled_stone_bricks",
      p_360349_ -> new InfestedBlock(CHISELED_STONE_BRICKS, p_360349_),
      BlockBehaviour.Properties.of().mapColor(MapColor.CLAY)
   );
   public static final Block BROWN_MUSHROOM_BLOCK = register(
      "brown_mushroom_block",
      HugeMushroomBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block RED_MUSHROOM_BLOCK = register(
      "red_mushroom_block",
      HugeMushroomBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block MUSHROOM_STEM = register(
      "mushroom_stem",
      HugeMushroomBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block IRON_BARS = register(
      "iron_bars", IronBarsBlock::new, BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON).noOcclusion()
   );
   public static final WeatheringCopperBlocks COPPER_BARS = WeatheringCopperBlocks.create(
      "copper_bars",
      Blocks::register,
      IronBarsBlock::new,
      WeatheringCopperBarsBlock::new,
      p_422073_ -> BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.COPPER).noOcclusion()
   );
   public static final Block IRON_CHAIN = register(
      "iron_chain",
      ChainBlock::new,
      BlockBehaviour.Properties.of().forceSolidOn().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.CHAIN).noOcclusion()
   );
   public static final WeatheringCopperBlocks COPPER_CHAIN = WeatheringCopperBlocks.create(
      "copper_chain",
      Blocks::register,
      ChainBlock::new,
      WeatheringCopperChainBlock::new,
      p_422064_ -> BlockBehaviour.Properties.of().forceSolidOn().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.CHAIN).noOcclusion()
   );
   public static final Block GLASS_PANE = register(
      "glass_pane", IronBarsBlock::new, BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block PUMPKIN = register(
      net.minecraft.references.Blocks.PUMPKIN,
      PumpkinBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.DIDGERIDOO)
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block MELON = register(
      net.minecraft.references.Blocks.MELON,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0F).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block ATTACHED_PUMPKIN_STEM = register(
      net.minecraft.references.Blocks.ATTACHED_PUMPKIN_STEM,
      p_360228_ -> new AttachedStemBlock(net.minecraft.references.Blocks.PUMPKIN_STEM, net.minecraft.references.Blocks.PUMPKIN, Items.PUMPKIN_SEEDS, p_360228_),
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block ATTACHED_MELON_STEM = register(
      net.minecraft.references.Blocks.ATTACHED_MELON_STEM,
      p_360035_ -> new AttachedStemBlock(net.minecraft.references.Blocks.MELON_STEM, net.minecraft.references.Blocks.MELON, Items.MELON_SEEDS, p_360035_),
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block PUMPKIN_STEM = register(
      net.minecraft.references.Blocks.PUMPKIN_STEM,
      p_360137_ -> new StemBlock(net.minecraft.references.Blocks.PUMPKIN, net.minecraft.references.Blocks.ATTACHED_PUMPKIN_STEM, Items.PUMPKIN_SEEDS, p_360137_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.HARD_CROP)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block MELON_STEM = register(
      net.minecraft.references.Blocks.MELON_STEM,
      p_360085_ -> new StemBlock(net.minecraft.references.Blocks.MELON, net.minecraft.references.Blocks.ATTACHED_MELON_STEM, Items.MELON_SEEDS, p_360085_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .noCollision()
         .randomTicks()
         .instabreak()
         .sound(SoundType.HARD_CROP)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block VINE = register(
      "vine",
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
      "glow_lichen",
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
      "resin_clump",
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
      "oak_fence_gate",
      p_360317_ -> new FenceGateBlock(WoodType.OAK, p_360317_),
      BlockBehaviour.Properties.of()
         .mapColor(OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block BRICK_STAIRS = registerLegacyStair("brick_stairs", BRICKS);
   public static final Block STONE_BRICK_STAIRS = registerLegacyStair("stone_brick_stairs", STONE_BRICKS);
   public static final Block MUD_BRICK_STAIRS = registerLegacyStair("mud_brick_stairs", MUD_BRICKS);
   public static final Block MYCELIUM = register(
      "mycelium", MyceliumBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).randomTicks().strength(0.6F).sound(SoundType.GRASS)
   );
   public static final Block LILY_PAD = register(
      "lily_pad",
      WaterlilyBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().sound(SoundType.LILY_PAD).noOcclusion().pushReaction(PushReaction.DESTROY)
   );
   public static final Block RESIN_BLOCK = register(
      "resin_block", BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.RESIN)
   );
   public static final Block RESIN_BRICKS = register(
      "resin_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .sound(SoundType.RESIN_BRICKS)
         .strength(1.5F, 6.0F)
   );
   public static final Block RESIN_BRICK_STAIRS = registerLegacyStair("resin_brick_stairs", RESIN_BRICKS);
   public static final Block RESIN_BRICK_SLAB = register(
      "resin_brick_slab",
      p_375322_ -> new SlabBlock(p_375322_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .sound(SoundType.RESIN_BRICKS)
         .strength(1.5F, 6.0F)
   );
   public static final Block RESIN_BRICK_WALL = register(
      "resin_brick_wall",
      p_375323_ -> new WallBlock(p_375323_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .sound(SoundType.RESIN_BRICKS)
         .strength(1.5F, 6.0F)
   );
   public static final Block CHISELED_RESIN_BRICKS = register(
      "chiseled_resin_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .sound(SoundType.RESIN_BRICKS)
         .strength(1.5F, 6.0F)
   );
   public static final Block NETHER_BRICKS = register(
      "nether_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.NETHER_BRICKS)
   );
   public static final Block NETHER_BRICK_FENCE = register(
      "nether_brick_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.NETHER_BRICKS)
   );
   public static final Block NETHER_BRICK_STAIRS = registerLegacyStair("nether_brick_stairs", NETHER_BRICKS);
   public static final Block NETHER_WART = register(
      "nether_wart",
      NetherWartBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().randomTicks().sound(SoundType.NETHER_WART).pushReaction(PushReaction.DESTROY)
   );
   public static final Block ENCHANTING_TABLE = register(
      "enchanting_table",
      EnchantingTableBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_RED)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .lightLevel(p_50870_ -> 7)
         .strength(5.0F, 1200.0F)
   );
   public static final Block BREWING_STAND = register(
      "brewing_stand", BrewingStandBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5F).lightLevel(p_50856_ -> 1).noOcclusion()
   );
   public static final Block CAULDRON = register(
      "cauldron", CauldronBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(2.0F).noOcclusion()
   );
   public static final Block WATER_CAULDRON = register(
      "water_cauldron",
      p_360213_ -> new LayeredCauldronBlock(Biome.Precipitation.RAIN, CauldronInteraction.WATER, p_360213_),
      BlockBehaviour.Properties.ofLegacyCopy(CAULDRON)
   );
   public static final Block LAVA_CAULDRON = register(
      "lava_cauldron", LavaCauldronBlock::new, BlockBehaviour.Properties.ofLegacyCopy(CAULDRON).lightLevel(p_187437_ -> 15)
   );
   public static final Block POWDER_SNOW_CAULDRON = register(
      "powder_snow_cauldron",
      p_360154_ -> new LayeredCauldronBlock(Biome.Precipitation.SNOW, CauldronInteraction.POWDER_SNOW, p_360154_),
      BlockBehaviour.Properties.ofLegacyCopy(CAULDRON)
   );
   public static final Block END_PORTAL = register(
      "end_portal",
      EndPortalBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .noCollision()
         .lightLevel(p_152692_ -> 15)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block END_PORTAL_FRAME = register(
      "end_portal_frame",
      EndPortalFrameBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GREEN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .sound(SoundType.GLASS)
         .lightLevel(p_50847_ -> 1)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
   );
   public static final Block END_STONE = register(
      "end_stone",
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 9.0F)
   );
   public static final Block DRAGON_EGG = register(
      "dragon_egg",
      DragonEggBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .strength(3.0F, 9.0F)
         .lightLevel(p_50840_ -> 1)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block REDSTONE_LAMP = register(
      "redstone_lamp",
      RedstoneLampBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_ORANGE)
         .lightLevel(litBlockEmission(15))
         .strength(0.3F)
         .sound(SoundType.GLASS)
         .isValidSpawn(Blocks::always)
   );
   public static final Block COCOA = register(
      "cocoa",
      CocoaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .randomTicks()
         .strength(0.2F, 3.0F)
         .sound(SoundType.WOOD)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block SANDSTONE_STAIRS = registerLegacyStair("sandstone_stairs", SANDSTONE);
   public static final Block EMERALD_ORE = register(
      "emerald_ore",
      p_360000_ -> new DropExperienceBlock(UniformInt.of(3, 7), p_360000_),
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
   );
   public static final Block DEEPSLATE_EMERALD_ORE = register(
      "deepslate_emerald_ore",
      p_360285_ -> new DropExperienceBlock(UniformInt.of(3, 7), p_360285_),
      BlockBehaviour.Properties.ofLegacyCopy(EMERALD_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block ENDER_CHEST = register(
      "ender_chest",
      EnderChestBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(22.5F, 600.0F).lightLevel(p_152690_ -> 7)
   );
   public static final Block TRIPWIRE_HOOK = register(
      "tripwire_hook", TripWireHookBlock::new, BlockBehaviour.Properties.of().noCollision().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)
   );
   public static final Block TRIPWIRE = register(
      "tripwire", p_360021_ -> new TripWireBlock(TRIPWIRE_HOOK, p_360021_), BlockBehaviour.Properties.of().noCollision().pushReaction(PushReaction.DESTROY)
   );
   public static final Block EMERALD_BLOCK = register(
      "emerald_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.EMERALD)
         .instrument(NoteBlockInstrument.BIT)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 6.0F)
         .sound(SoundType.METAL)
   );
   public static final Block SPRUCE_STAIRS = registerLegacyStair("spruce_stairs", SPRUCE_PLANKS);
   public static final Block BIRCH_STAIRS = registerLegacyStair("birch_stairs", BIRCH_PLANKS);
   public static final Block JUNGLE_STAIRS = registerLegacyStair("jungle_stairs", JUNGLE_PLANKS);
   public static final Block COMMAND_BLOCK = register(
      "command_block",
      p_360197_ -> new CommandBlock(false, p_360197_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
   );
   public static final Block BEACON = register(
      "beacon",
      BeaconBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.DIAMOND)
         .instrument(NoteBlockInstrument.HAT)
         .strength(3.0F)
         .lightLevel(p_50854_ -> 15)
         .noOcclusion()
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block COBBLESTONE_WALL = register("cobblestone_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLESTONE).forceSolidOn());
   public static final Block MOSSY_COBBLESTONE_WALL = register(
      "mossy_cobblestone_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLESTONE).forceSolidOn()
   );
   public static final Block FLOWER_POT = register("flower_pot", p_360251_ -> new FlowerPotBlock(AIR, p_360251_), flowerPotProperties());
   public static final Block POTTED_TORCHFLOWER = register("potted_torchflower", p_360121_ -> new FlowerPotBlock(TORCHFLOWER, p_360121_), flowerPotProperties());
   public static final Block POTTED_OAK_SAPLING = register("potted_oak_sapling", p_360309_ -> new FlowerPotBlock(OAK_SAPLING, p_360309_), flowerPotProperties());
   public static final Block POTTED_SPRUCE_SAPLING = register(
      "potted_spruce_sapling", p_360176_ -> new FlowerPotBlock(SPRUCE_SAPLING, p_360176_), flowerPotProperties()
   );
   public static final Block POTTED_BIRCH_SAPLING = register(
      "potted_birch_sapling", p_360042_ -> new FlowerPotBlock(BIRCH_SAPLING, p_360042_), flowerPotProperties()
   );
   public static final Block POTTED_JUNGLE_SAPLING = register(
      "potted_jungle_sapling", p_360294_ -> new FlowerPotBlock(JUNGLE_SAPLING, p_360294_), flowerPotProperties()
   );
   public static final Block POTTED_ACACIA_SAPLING = register(
      "potted_acacia_sapling", p_360123_ -> new FlowerPotBlock(ACACIA_SAPLING, p_360123_), flowerPotProperties()
   );
   public static final Block POTTED_CHERRY_SAPLING = register(
      "potted_cherry_sapling", p_360175_ -> new FlowerPotBlock(CHERRY_SAPLING, p_360175_), flowerPotProperties()
   );
   public static final Block POTTED_DARK_OAK_SAPLING = register(
      "potted_dark_oak_sapling", p_360374_ -> new FlowerPotBlock(DARK_OAK_SAPLING, p_360374_), flowerPotProperties()
   );
   public static final Block POTTED_PALE_OAK_SAPLING = register(
      "potted_pale_oak_sapling", p_360009_ -> new FlowerPotBlock(PALE_OAK_SAPLING, p_360009_), flowerPotProperties()
   );
   public static final Block POTTED_MANGROVE_PROPAGULE = register(
      "potted_mangrove_propagule", p_360261_ -> new FlowerPotBlock(MANGROVE_PROPAGULE, p_360261_), flowerPotProperties()
   );
   public static final Block POTTED_FERN = register("potted_fern", p_360041_ -> new FlowerPotBlock(FERN, p_360041_), flowerPotProperties());
   public static final Block POTTED_DANDELION = register("potted_dandelion", p_360132_ -> new FlowerPotBlock(DANDELION, p_360132_), flowerPotProperties());
   public static final Block POTTED_POPPY = register("potted_poppy", p_360269_ -> new FlowerPotBlock(POPPY, p_360269_), flowerPotProperties());
   public static final Block POTTED_BLUE_ORCHID = register("potted_blue_orchid", p_360142_ -> new FlowerPotBlock(BLUE_ORCHID, p_360142_), flowerPotProperties());
   public static final Block POTTED_ALLIUM = register("potted_allium", p_360058_ -> new FlowerPotBlock(ALLIUM, p_360058_), flowerPotProperties());
   public static final Block POTTED_AZURE_BLUET = register("potted_azure_bluet", p_360124_ -> new FlowerPotBlock(AZURE_BLUET, p_360124_), flowerPotProperties());
   public static final Block POTTED_RED_TULIP = register("potted_red_tulip", p_360253_ -> new FlowerPotBlock(RED_TULIP, p_360253_), flowerPotProperties());
   public static final Block POTTED_ORANGE_TULIP = register(
      "potted_orange_tulip", p_360086_ -> new FlowerPotBlock(ORANGE_TULIP, p_360086_), flowerPotProperties()
   );
   public static final Block POTTED_WHITE_TULIP = register("potted_white_tulip", p_360319_ -> new FlowerPotBlock(WHITE_TULIP, p_360319_), flowerPotProperties());
   public static final Block POTTED_PINK_TULIP = register("potted_pink_tulip", p_360390_ -> new FlowerPotBlock(PINK_TULIP, p_360390_), flowerPotProperties());
   public static final Block POTTED_OXEYE_DAISY = register("potted_oxeye_daisy", p_360210_ -> new FlowerPotBlock(OXEYE_DAISY, p_360210_), flowerPotProperties());
   public static final Block POTTED_CORNFLOWER = register("potted_cornflower", p_360305_ -> new FlowerPotBlock(CORNFLOWER, p_360305_), flowerPotProperties());
   public static final Block POTTED_LILY_OF_THE_VALLEY = register(
      "potted_lily_of_the_valley", p_360302_ -> new FlowerPotBlock(LILY_OF_THE_VALLEY, p_360302_), flowerPotProperties()
   );
   public static final Block POTTED_WITHER_ROSE = register("potted_wither_rose", p_359977_ -> new FlowerPotBlock(WITHER_ROSE, p_359977_), flowerPotProperties());
   public static final Block POTTED_RED_MUSHROOM = register(
      "potted_red_mushroom", p_360271_ -> new FlowerPotBlock(RED_MUSHROOM, p_360271_), flowerPotProperties()
   );
   public static final Block POTTED_BROWN_MUSHROOM = register(
      "potted_brown_mushroom", p_360026_ -> new FlowerPotBlock(BROWN_MUSHROOM, p_360026_), flowerPotProperties()
   );
   public static final Block POTTED_DEAD_BUSH = register("potted_dead_bush", p_360065_ -> new FlowerPotBlock(DEAD_BUSH, p_360065_), flowerPotProperties());
   public static final Block POTTED_CACTUS = register("potted_cactus", p_360229_ -> new FlowerPotBlock(CACTUS, p_360229_), flowerPotProperties());
   public static final Block CARROTS = register(
      "carrots",
      CarrotBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
   );
   public static final Block POTATOES = register(
      "potatoes",
      PotatoBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
   );
   public static final Block OAK_BUTTON = register("oak_button", p_360283_ -> new ButtonBlock(BlockSetType.OAK, 30, p_360283_), buttonProperties());
   public static final Block SPRUCE_BUTTON = register("spruce_button", p_360158_ -> new ButtonBlock(BlockSetType.SPRUCE, 30, p_360158_), buttonProperties());
   public static final Block BIRCH_BUTTON = register("birch_button", p_360056_ -> new ButtonBlock(BlockSetType.BIRCH, 30, p_360056_), buttonProperties());
   public static final Block JUNGLE_BUTTON = register("jungle_button", p_360019_ -> new ButtonBlock(BlockSetType.JUNGLE, 30, p_360019_), buttonProperties());
   public static final Block ACACIA_BUTTON = register("acacia_button", p_360289_ -> new ButtonBlock(BlockSetType.ACACIA, 30, p_360289_), buttonProperties());
   public static final Block CHERRY_BUTTON = register("cherry_button", p_360264_ -> new ButtonBlock(BlockSetType.CHERRY, 30, p_360264_), buttonProperties());
   public static final Block DARK_OAK_BUTTON = register(
      "dark_oak_button", p_360164_ -> new ButtonBlock(BlockSetType.DARK_OAK, 30, p_360164_), buttonProperties()
   );
   public static final Block PALE_OAK_BUTTON = register(
      "pale_oak_button", p_360221_ -> new ButtonBlock(BlockSetType.PALE_OAK, 30, p_360221_), buttonProperties()
   );
   public static final Block MANGROVE_BUTTON = register(
      "mangrove_button", p_360122_ -> new ButtonBlock(BlockSetType.MANGROVE, 30, p_360122_), buttonProperties()
   );
   public static final Block BAMBOO_BUTTON = register("bamboo_button", p_360353_ -> new ButtonBlock(BlockSetType.BAMBOO, 30, p_360353_), buttonProperties());
   public static final Block SKELETON_SKULL = register(
      "skeleton_skull",
      p_360115_ -> new SkullBlock(SkullBlock.Types.SKELETON, p_360115_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.SKELETON).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block SKELETON_WALL_SKULL = register(
      "skeleton_wall_skull",
      p_360388_ -> new WallSkullBlock(SkullBlock.Types.SKELETON, p_360388_),
      wallVariant(SKELETON_SKULL, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block WITHER_SKELETON_SKULL = register(
      "wither_skeleton_skull",
      WitherSkullBlock::new,
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.WITHER_SKELETON).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block WITHER_SKELETON_WALL_SKULL = register(
      "wither_skeleton_wall_skull", WitherWallSkullBlock::new, wallVariant(WITHER_SKELETON_SKULL, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block ZOMBIE_HEAD = register(
      "zombie_head",
      p_360012_ -> new SkullBlock(SkullBlock.Types.ZOMBIE, p_360012_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.ZOMBIE).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block ZOMBIE_WALL_HEAD = register(
      "zombie_wall_head",
      p_360393_ -> new WallSkullBlock(SkullBlock.Types.ZOMBIE, p_360393_),
      wallVariant(ZOMBIE_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block PLAYER_HEAD = register(
      "player_head",
      PlayerHeadBlock::new,
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.CUSTOM_HEAD).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block PLAYER_WALL_HEAD = register(
      "player_wall_head", PlayerWallHeadBlock::new, wallVariant(PLAYER_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block CREEPER_HEAD = register(
      "creeper_head",
      p_360072_ -> new SkullBlock(SkullBlock.Types.CREEPER, p_360072_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.CREEPER).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block CREEPER_WALL_HEAD = register(
      "creeper_wall_head",
      p_360225_ -> new WallSkullBlock(SkullBlock.Types.CREEPER, p_360225_),
      wallVariant(CREEPER_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block DRAGON_HEAD = register(
      "dragon_head",
      p_360307_ -> new SkullBlock(SkullBlock.Types.DRAGON, p_360307_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.DRAGON).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block DRAGON_WALL_HEAD = register(
      "dragon_wall_head",
      p_359968_ -> new WallSkullBlock(SkullBlock.Types.DRAGON, p_359968_),
      wallVariant(DRAGON_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block PIGLIN_HEAD = register(
      "piglin_head",
      p_360128_ -> new SkullBlock(SkullBlock.Types.PIGLIN, p_360128_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.PIGLIN).strength(1.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block PIGLIN_WALL_HEAD = register(
      "piglin_wall_head", PiglinWallSkullBlock::new, wallVariant(PIGLIN_HEAD, true).strength(1.0F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block ANVIL = register(
      "anvil",
      AnvilBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 1200.0F)
         .sound(SoundType.ANVIL)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block CHIPPED_ANVIL = register(
      "chipped_anvil",
      AnvilBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 1200.0F)
         .sound(SoundType.ANVIL)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block DAMAGED_ANVIL = register(
      "damaged_anvil",
      AnvilBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 1200.0F)
         .sound(SoundType.ANVIL)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block TRAPPED_CHEST = register(
      "trapped_chest",
      TrappedChestBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block LIGHT_WEIGHTED_PRESSURE_PLATE = register(
      "light_weighted_pressure_plate",
      p_360163_ -> new WeightedPressurePlateBlock(15, BlockSetType.GOLD, p_360163_),
      BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block HEAVY_WEIGHTED_PRESSURE_PLATE = register(
      "heavy_weighted_pressure_plate",
      p_360190_ -> new WeightedPressurePlateBlock(150, BlockSetType.IRON, p_360190_),
      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY)
   );
   public static final Block COMPARATOR = register(
      "comparator", ComparatorBlock::new, BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
   );
   public static final Block DAYLIGHT_DETECTOR = register(
      "daylight_detector",
      DaylightDetectorBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block REDSTONE_BLOCK = register(
      "redstone_block",
      PoweredBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.FIRE)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 6.0F)
         .sound(SoundType.METAL)
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block NETHER_QUARTZ_ORE = register(
      "nether_quartz_ore",
      p_360304_ -> new DropExperienceBlock(UniformInt.of(2, 5), p_360304_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(3.0F, 3.0F)
         .sound(SoundType.NETHER_ORE)
   );
   public static final Block HOPPER = register(
      "hopper",
      HopperBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion()
   );
   public static final Block QUARTZ_BLOCK = register(
      "quartz_block",
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block CHISELED_QUARTZ_BLOCK = register(
      "chiseled_quartz_block",
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block QUARTZ_PILLAR = register(
      "quartz_pillar",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block QUARTZ_STAIRS = registerLegacyStair("quartz_stairs", QUARTZ_BLOCK);
   public static final Block ACTIVATOR_RAIL = register(
      "activator_rail", PoweredRailBlock::new, BlockBehaviour.Properties.of().noCollision().strength(0.7F).sound(SoundType.METAL)
   );
   public static final Block DROPPER = register(
      "dropper",
      DropperBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F)
   );
   public static final Block WHITE_TERRACOTTA = register(
      "white_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_WHITE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block ORANGE_TERRACOTTA = register(
      "orange_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block MAGENTA_TERRACOTTA = register(
      "magenta_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_MAGENTA)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block LIGHT_BLUE_TERRACOTTA = register(
      "light_blue_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_LIGHT_BLUE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block YELLOW_TERRACOTTA = register(
      "yellow_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_YELLOW)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block LIME_TERRACOTTA = register(
      "lime_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_LIGHT_GREEN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block PINK_TERRACOTTA = register(
      "pink_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_PINK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block GRAY_TERRACOTTA = register(
      "gray_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block LIGHT_GRAY_TERRACOTTA = register(
      "light_gray_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block CYAN_TERRACOTTA = register(
      "cyan_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_CYAN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block PURPLE_TERRACOTTA = register(
      "purple_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_PURPLE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block BLUE_TERRACOTTA = register(
      "blue_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_BLUE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block BROWN_TERRACOTTA = register(
      "brown_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_BROWN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block GREEN_TERRACOTTA = register(
      "green_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_GREEN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block RED_TERRACOTTA = register(
      "red_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_RED)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block BLACK_TERRACOTTA = register(
      "black_terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block WHITE_STAINED_GLASS_PANE = register(
      "white_stained_glass_pane",
      p_360083_ -> new StainedGlassPaneBlock(DyeColor.WHITE, p_360083_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block ORANGE_STAINED_GLASS_PANE = register(
      "orange_stained_glass_pane",
      p_360141_ -> new StainedGlassPaneBlock(DyeColor.ORANGE, p_360141_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block MAGENTA_STAINED_GLASS_PANE = register(
      "magenta_stained_glass_pane",
      p_360256_ -> new StainedGlassPaneBlock(DyeColor.MAGENTA, p_360256_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block LIGHT_BLUE_STAINED_GLASS_PANE = register(
      "light_blue_stained_glass_pane",
      p_359969_ -> new StainedGlassPaneBlock(DyeColor.LIGHT_BLUE, p_359969_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block YELLOW_STAINED_GLASS_PANE = register(
      "yellow_stained_glass_pane",
      p_360023_ -> new StainedGlassPaneBlock(DyeColor.YELLOW, p_360023_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block LIME_STAINED_GLASS_PANE = register(
      "lime_stained_glass_pane",
      p_360303_ -> new StainedGlassPaneBlock(DyeColor.LIME, p_360303_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block PINK_STAINED_GLASS_PANE = register(
      "pink_stained_glass_pane",
      p_360059_ -> new StainedGlassPaneBlock(DyeColor.PINK, p_360059_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block GRAY_STAINED_GLASS_PANE = register(
      "gray_stained_glass_pane",
      p_360014_ -> new StainedGlassPaneBlock(DyeColor.GRAY, p_360014_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block LIGHT_GRAY_STAINED_GLASS_PANE = register(
      "light_gray_stained_glass_pane",
      p_360076_ -> new StainedGlassPaneBlock(DyeColor.LIGHT_GRAY, p_360076_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block CYAN_STAINED_GLASS_PANE = register(
      "cyan_stained_glass_pane",
      p_360153_ -> new StainedGlassPaneBlock(DyeColor.CYAN, p_360153_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block PURPLE_STAINED_GLASS_PANE = register(
      "purple_stained_glass_pane",
      p_360235_ -> new StainedGlassPaneBlock(DyeColor.PURPLE, p_360235_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block BLUE_STAINED_GLASS_PANE = register(
      "blue_stained_glass_pane",
      p_360034_ -> new StainedGlassPaneBlock(DyeColor.BLUE, p_360034_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block BROWN_STAINED_GLASS_PANE = register(
      "brown_stained_glass_pane",
      p_360379_ -> new StainedGlassPaneBlock(DyeColor.BROWN, p_360379_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block GREEN_STAINED_GLASS_PANE = register(
      "green_stained_glass_pane",
      p_360191_ -> new StainedGlassPaneBlock(DyeColor.GREEN, p_360191_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block RED_STAINED_GLASS_PANE = register(
      "red_stained_glass_pane",
      p_360347_ -> new StainedGlassPaneBlock(DyeColor.RED, p_360347_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block BLACK_STAINED_GLASS_PANE = register(
      "black_stained_glass_pane",
      p_360134_ -> new StainedGlassPaneBlock(DyeColor.BLACK, p_360134_),
      BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
   );
   public static final Block ACACIA_STAIRS = registerLegacyStair("acacia_stairs", ACACIA_PLANKS);
   public static final Block CHERRY_STAIRS = registerLegacyStair("cherry_stairs", CHERRY_PLANKS);
   public static final Block DARK_OAK_STAIRS = registerLegacyStair("dark_oak_stairs", DARK_OAK_PLANKS);
   public static final Block PALE_OAK_STAIRS = registerLegacyStair("pale_oak_stairs", PALE_OAK_PLANKS);
   public static final Block MANGROVE_STAIRS = registerLegacyStair("mangrove_stairs", MANGROVE_PLANKS);
   public static final Block BAMBOO_STAIRS = registerLegacyStair("bamboo_stairs", BAMBOO_PLANKS);
   public static final Block BAMBOO_MOSAIC_STAIRS = registerLegacyStair("bamboo_mosaic_stairs", BAMBOO_MOSAIC);
   public static final Block SLIME_BLOCK = register(
      "slime_block", SlimeBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).friction(0.8F).sound(SoundType.SLIME_BLOCK).noOcclusion()
   );
   public static final Block BARRIER = register(
      "barrier",
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
      "light",
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
      "iron_trapdoor",
      p_360265_ -> new TrapDoorBlock(BlockSetType.IRON, p_360265_),
      BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F).noOcclusion().isValidSpawn(Blocks::never)
   );
   public static final Block PRISMARINE = register(
      "prismarine",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block PRISMARINE_BRICKS = register(
      "prismarine_bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block DARK_PRISMARINE = register(
      "dark_prismarine",
      BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block PRISMARINE_STAIRS = registerLegacyStair("prismarine_stairs", PRISMARINE);
   public static final Block PRISMARINE_BRICK_STAIRS = registerLegacyStair("prismarine_brick_stairs", PRISMARINE_BRICKS);
   public static final Block DARK_PRISMARINE_STAIRS = registerLegacyStair("dark_prismarine_stairs", DARK_PRISMARINE);
   public static final Block PRISMARINE_SLAB = register(
      "prismarine_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block PRISMARINE_BRICK_SLAB = register(
      "prismarine_brick_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block DARK_PRISMARINE_SLAB = register(
      "dark_prismarine_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block SEA_LANTERN = register(
      "sea_lantern",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.QUARTZ)
         .instrument(NoteBlockInstrument.HAT)
         .strength(0.3F)
         .sound(SoundType.GLASS)
         .lightLevel(p_152688_ -> 15)
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block HAY_BLOCK = register(
      "hay_block",
      HayBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BANJO).strength(0.5F).sound(SoundType.GRASS)
   );
   public static final Block WHITE_CARPET = register(
      "white_carpet",
      p_360240_ -> new WoolCarpetBlock(DyeColor.WHITE, p_360240_),
      BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block ORANGE_CARPET = register(
      "orange_carpet",
      p_360375_ -> new WoolCarpetBlock(DyeColor.ORANGE, p_360375_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block MAGENTA_CARPET = register(
      "magenta_carpet",
      p_360118_ -> new WoolCarpetBlock(DyeColor.MAGENTA, p_360118_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block LIGHT_BLUE_CARPET = register(
      "light_blue_carpet",
      p_360340_ -> new WoolCarpetBlock(DyeColor.LIGHT_BLUE, p_360340_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block YELLOW_CARPET = register(
      "yellow_carpet",
      p_360394_ -> new WoolCarpetBlock(DyeColor.YELLOW, p_360394_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block LIME_CARPET = register(
      "lime_carpet",
      p_360321_ -> new WoolCarpetBlock(DyeColor.LIME, p_360321_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block PINK_CARPET = register(
      "pink_carpet",
      p_360196_ -> new WoolCarpetBlock(DyeColor.PINK, p_360196_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block GRAY_CARPET = register(
      "gray_carpet",
      p_360356_ -> new WoolCarpetBlock(DyeColor.GRAY, p_360356_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block LIGHT_GRAY_CARPET = register(
      "light_gray_carpet",
      p_360204_ -> new WoolCarpetBlock(DyeColor.LIGHT_GRAY, p_360204_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block CYAN_CARPET = register(
      "cyan_carpet",
      p_360280_ -> new WoolCarpetBlock(DyeColor.CYAN, p_360280_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block PURPLE_CARPET = register(
      "purple_carpet",
      p_360230_ -> new WoolCarpetBlock(DyeColor.PURPLE, p_360230_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block BLUE_CARPET = register(
      "blue_carpet",
      p_360088_ -> new WoolCarpetBlock(DyeColor.BLUE, p_360088_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block BROWN_CARPET = register(
      "brown_carpet",
      p_360207_ -> new WoolCarpetBlock(DyeColor.BROWN, p_360207_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block GREEN_CARPET = register(
      "green_carpet",
      p_360029_ -> new WoolCarpetBlock(DyeColor.GREEN, p_360029_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block RED_CARPET = register(
      "red_carpet",
      p_360169_ -> new WoolCarpetBlock(DyeColor.RED, p_360169_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block BLACK_CARPET = register(
      "black_carpet",
      p_360337_ -> new WoolCarpetBlock(DyeColor.BLACK, p_360337_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.1F).sound(SoundType.WOOL).ignitedByLava()
   );
   public static final Block TERRACOTTA = register(
      "terracotta",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.25F, 4.2F)
   );
   public static final Block COAL_BLOCK = register(
      "coal_block",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
   );
   public static final Block PACKED_ICE = register(
      "packed_ice",
      BlockBehaviour.Properties.of().mapColor(MapColor.ICE).instrument(NoteBlockInstrument.CHIME).friction(0.98F).strength(0.5F).sound(SoundType.GLASS)
   );
   public static final Block SUNFLOWER = register(
      "sunflower",
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
      "lilac",
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
      "rose_bush",
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
      "peony",
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
      "tall_grass",
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
      "large_fern",
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
   public static final Block WHITE_BANNER = register(
      "white_banner",
      p_360057_ -> new BannerBlock(DyeColor.WHITE, p_360057_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block ORANGE_BANNER = register(
      "orange_banner",
      p_360167_ -> new BannerBlock(DyeColor.ORANGE, p_360167_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block MAGENTA_BANNER = register(
      "magenta_banner",
      p_360119_ -> new BannerBlock(DyeColor.MAGENTA, p_360119_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block LIGHT_BLUE_BANNER = register(
      "light_blue_banner",
      p_360105_ -> new BannerBlock(DyeColor.LIGHT_BLUE, p_360105_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block YELLOW_BANNER = register(
      "yellow_banner",
      p_360335_ -> new BannerBlock(DyeColor.YELLOW, p_360335_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block LIME_BANNER = register(
      "lime_banner",
      p_360290_ -> new BannerBlock(DyeColor.LIME, p_360290_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block PINK_BANNER = register(
      "pink_banner",
      p_360125_ -> new BannerBlock(DyeColor.PINK, p_360125_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block GRAY_BANNER = register(
      "gray_banner",
      p_360291_ -> new BannerBlock(DyeColor.GRAY, p_360291_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block LIGHT_GRAY_BANNER = register(
      "light_gray_banner",
      p_360037_ -> new BannerBlock(DyeColor.LIGHT_GRAY, p_360037_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block CYAN_BANNER = register(
      "cyan_banner",
      p_360364_ -> new BannerBlock(DyeColor.CYAN, p_360364_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block PURPLE_BANNER = register(
      "purple_banner",
      p_360254_ -> new BannerBlock(DyeColor.PURPLE, p_360254_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BLUE_BANNER = register(
      "blue_banner",
      p_360048_ -> new BannerBlock(DyeColor.BLUE, p_360048_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BROWN_BANNER = register(
      "brown_banner",
      p_359989_ -> new BannerBlock(DyeColor.BROWN, p_359989_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block GREEN_BANNER = register(
      "green_banner",
      p_360087_ -> new BannerBlock(DyeColor.GREEN, p_360087_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block RED_BANNER = register(
      "red_banner",
      p_360181_ -> new BannerBlock(DyeColor.RED, p_360181_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BLACK_BANNER = register(
      "black_banner",
      p_360330_ -> new BannerBlock(DyeColor.BLACK, p_360330_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block WHITE_WALL_BANNER = register(
      "white_wall_banner",
      p_360238_ -> new WallBannerBlock(DyeColor.WHITE, p_360238_),
      wallVariant(WHITE_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block ORANGE_WALL_BANNER = register(
      "orange_wall_banner",
      p_360080_ -> new WallBannerBlock(DyeColor.ORANGE, p_360080_),
      wallVariant(ORANGE_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block MAGENTA_WALL_BANNER = register(
      "magenta_wall_banner",
      p_360173_ -> new WallBannerBlock(DyeColor.MAGENTA, p_360173_),
      wallVariant(MAGENTA_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block LIGHT_BLUE_WALL_BANNER = register(
      "light_blue_wall_banner",
      p_360111_ -> new WallBannerBlock(DyeColor.LIGHT_BLUE, p_360111_),
      wallVariant(LIGHT_BLUE_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block YELLOW_WALL_BANNER = register(
      "yellow_wall_banner",
      p_359985_ -> new WallBannerBlock(DyeColor.YELLOW, p_359985_),
      wallVariant(YELLOW_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block LIME_WALL_BANNER = register(
      "lime_wall_banner",
      p_360066_ -> new WallBannerBlock(DyeColor.LIME, p_360066_),
      wallVariant(LIME_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block PINK_WALL_BANNER = register(
      "pink_wall_banner",
      p_360311_ -> new WallBannerBlock(DyeColor.PINK, p_360311_),
      wallVariant(PINK_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block GRAY_WALL_BANNER = register(
      "gray_wall_banner",
      p_359998_ -> new WallBannerBlock(DyeColor.GRAY, p_359998_),
      wallVariant(GRAY_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block LIGHT_GRAY_WALL_BANNER = register(
      "light_gray_wall_banner",
      p_360187_ -> new WallBannerBlock(DyeColor.LIGHT_GRAY, p_360187_),
      wallVariant(LIGHT_GRAY_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block CYAN_WALL_BANNER = register(
      "cyan_wall_banner",
      p_360074_ -> new WallBannerBlock(DyeColor.CYAN, p_360074_),
      wallVariant(CYAN_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block PURPLE_WALL_BANNER = register(
      "purple_wall_banner",
      p_360298_ -> new WallBannerBlock(DyeColor.PURPLE, p_360298_),
      wallVariant(PURPLE_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BLUE_WALL_BANNER = register(
      "blue_wall_banner",
      p_360178_ -> new WallBannerBlock(DyeColor.BLUE, p_360178_),
      wallVariant(BLUE_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BROWN_WALL_BANNER = register(
      "brown_wall_banner",
      p_360249_ -> new WallBannerBlock(DyeColor.BROWN, p_360249_),
      wallVariant(BROWN_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block GREEN_WALL_BANNER = register(
      "green_wall_banner",
      p_360073_ -> new WallBannerBlock(DyeColor.GREEN, p_360073_),
      wallVariant(GREEN_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block RED_WALL_BANNER = register(
      "red_wall_banner",
      p_360275_ -> new WallBannerBlock(DyeColor.RED, p_360275_),
      wallVariant(RED_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BLACK_WALL_BANNER = register(
      "black_wall_banner",
      p_360274_ -> new WallBannerBlock(DyeColor.BLACK, p_360274_),
      wallVariant(BLACK_BANNER, true)
         .mapColor(MapColor.WOOD)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(1.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block RED_SANDSTONE = register(
      "red_sandstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block CHISELED_RED_SANDSTONE = register(
      "chiseled_red_sandstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block CUT_RED_SANDSTONE = register(
      "cut_red_sandstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(0.8F)
   );
   public static final Block RED_SANDSTONE_STAIRS = registerLegacyStair("red_sandstone_stairs", RED_SANDSTONE);
   public static final Block OAK_SLAB = register(
      "oak_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block SPRUCE_SLAB = register(
      "spruce_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block BIRCH_SLAB = register(
      "birch_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block JUNGLE_SLAB = register(
      "jungle_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block ACACIA_SLAB = register(
      "acacia_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block CHERRY_SLAB = register(
      "cherry_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_WHITE)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.CHERRY_WOOD)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_SLAB = register(
      "dark_oak_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BROWN)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_SLAB = register(
      "pale_oak_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block MANGROVE_SLAB = register(
      "mangrove_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_RED)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.WOOD)
         .ignitedByLava()
   );
   public static final Block BAMBOO_SLAB = register(
      "bamboo_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.BAMBOO_WOOD)
         .ignitedByLava()
   );
   public static final Block BAMBOO_MOSAIC_SLAB = register(
      "bamboo_mosaic_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.BAMBOO_WOOD)
         .ignitedByLava()
   );
   public static final Block STONE_SLAB = register(
      "stone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block SMOOTH_STONE_SLAB = register(
      "smooth_stone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block SANDSTONE_SLAB = register(
      "sandstone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block CUT_SANDSTONE_SLAB = register(
      "cut_sandstone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block PETRIFIED_OAK_SLAB = register(
      "petrified_oak_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block COBBLESTONE_SLAB = register(
      "cobblestone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block BRICK_SLAB = register(
      "brick_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block STONE_BRICK_SLAB = register(
      "stone_brick_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block MUD_BRICK_SLAB = register(
      "mud_brick_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 3.0F)
         .sound(SoundType.MUD_BRICKS)
   );
   public static final Block NETHER_BRICK_SLAB = register(
      "nether_brick_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.NETHER_BRICKS)
   );
   public static final Block QUARTZ_SLAB = register(
      "quartz_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block RED_SANDSTONE_SLAB = register(
      "red_sandstone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
   );
   public static final Block CUT_RED_SANDSTONE_SLAB = register(
      "cut_red_sandstone_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
   );
   public static final Block PURPUR_SLAB = register(
      "purpur_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_MAGENTA)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
   );
   public static final Block SMOOTH_STONE = register(
      "smooth_stone",
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block SMOOTH_SANDSTONE = register(
      "smooth_sandstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block SMOOTH_QUARTZ = register(
      "smooth_quartz",
      BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
   );
   public static final Block SMOOTH_RED_SANDSTONE = register(
      "smooth_red_sandstone",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
   );
   public static final Block SPRUCE_FENCE_GATE = register(
      "spruce_fence_gate",
      p_360376_ -> new FenceGateBlock(WoodType.SPRUCE, p_360376_),
      BlockBehaviour.Properties.of()
         .mapColor(SPRUCE_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block BIRCH_FENCE_GATE = register(
      "birch_fence_gate",
      p_360260_ -> new FenceGateBlock(WoodType.BIRCH, p_360260_),
      BlockBehaviour.Properties.of()
         .mapColor(BIRCH_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block JUNGLE_FENCE_GATE = register(
      "jungle_fence_gate",
      p_360352_ -> new FenceGateBlock(WoodType.JUNGLE, p_360352_),
      BlockBehaviour.Properties.of()
         .mapColor(JUNGLE_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block ACACIA_FENCE_GATE = register(
      "acacia_fence_gate",
      p_360324_ -> new FenceGateBlock(WoodType.ACACIA, p_360324_),
      BlockBehaviour.Properties.of()
         .mapColor(ACACIA_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block CHERRY_FENCE_GATE = register(
      "cherry_fence_gate",
      p_360018_ -> new FenceGateBlock(WoodType.CHERRY, p_360018_),
      BlockBehaviour.Properties.of()
         .mapColor(CHERRY_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block DARK_OAK_FENCE_GATE = register(
      "dark_oak_fence_gate",
      p_360355_ -> new FenceGateBlock(WoodType.DARK_OAK, p_360355_),
      BlockBehaviour.Properties.of()
         .mapColor(DARK_OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block PALE_OAK_FENCE_GATE = register(
      "pale_oak_fence_gate",
      p_359981_ -> new FenceGateBlock(WoodType.PALE_OAK, p_359981_),
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block MANGROVE_FENCE_GATE = register(
      "mangrove_fence_gate",
      p_360189_ -> new FenceGateBlock(WoodType.MANGROVE, p_360189_),
      BlockBehaviour.Properties.of()
         .mapColor(MANGROVE_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block BAMBOO_FENCE_GATE = register(
      "bamboo_fence_gate",
      p_360172_ -> new FenceGateBlock(WoodType.BAMBOO, p_360172_),
      BlockBehaviour.Properties.of()
         .mapColor(BAMBOO_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
   );
   public static final Block SPRUCE_FENCE = register(
      "spruce_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(SPRUCE_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block BIRCH_FENCE = register(
      "birch_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(BIRCH_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block JUNGLE_FENCE = register(
      "jungle_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(JUNGLE_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block ACACIA_FENCE = register(
      "acacia_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(ACACIA_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block CHERRY_FENCE = register(
      "cherry_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(CHERRY_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.CHERRY_WOOD)
   );
   public static final Block DARK_OAK_FENCE = register(
      "dark_oak_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DARK_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block PALE_OAK_FENCE = register(
      "pale_oak_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block MANGROVE_FENCE = register(
      "mangrove_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MANGROVE_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .ignitedByLava()
         .sound(SoundType.WOOD)
   );
   public static final Block BAMBOO_FENCE = register(
      "bamboo_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(BAMBOO_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.BAMBOO_WOOD)
         .ignitedByLava()
   );
   public static final Block SPRUCE_DOOR = register(
      "spruce_door",
      p_360136_ -> new DoorBlock(BlockSetType.SPRUCE, p_360136_),
      BlockBehaviour.Properties.of()
         .mapColor(SPRUCE_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BIRCH_DOOR = register(
      "birch_door",
      p_360385_ -> new DoorBlock(BlockSetType.BIRCH, p_360385_),
      BlockBehaviour.Properties.of()
         .mapColor(BIRCH_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block JUNGLE_DOOR = register(
      "jungle_door",
      p_360313_ -> new DoorBlock(BlockSetType.JUNGLE, p_360313_),
      BlockBehaviour.Properties.of()
         .mapColor(JUNGLE_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block ACACIA_DOOR = register(
      "acacia_door",
      p_360112_ -> new DoorBlock(BlockSetType.ACACIA, p_360112_),
      BlockBehaviour.Properties.of()
         .mapColor(ACACIA_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CHERRY_DOOR = register(
      "cherry_door",
      p_360067_ -> new DoorBlock(BlockSetType.CHERRY, p_360067_),
      BlockBehaviour.Properties.of()
         .mapColor(CHERRY_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block DARK_OAK_DOOR = register(
      "dark_oak_door",
      p_360236_ -> new DoorBlock(BlockSetType.DARK_OAK, p_360236_),
      BlockBehaviour.Properties.of()
         .mapColor(DARK_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block PALE_OAK_DOOR = register(
      "pale_oak_door",
      p_360266_ -> new DoorBlock(BlockSetType.PALE_OAK, p_360266_),
      BlockBehaviour.Properties.of()
         .mapColor(PALE_OAK_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block MANGROVE_DOOR = register(
      "mangrove_door",
      p_360010_ -> new DoorBlock(BlockSetType.MANGROVE, p_360010_),
      BlockBehaviour.Properties.of()
         .mapColor(MANGROVE_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BAMBOO_DOOR = register(
      "bamboo_door",
      p_360049_ -> new DoorBlock(BlockSetType.BAMBOO, p_360049_),
      BlockBehaviour.Properties.of()
         .mapColor(BAMBOO_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block END_ROD = register(
      "end_rod", EndRodBlock::new, BlockBehaviour.Properties.of().forceSolidOff().instabreak().lightLevel(p_152686_ -> 14).sound(SoundType.WOOD).noOcclusion()
   );
   public static final Block CHORUS_PLANT = register(
      "chorus_plant",
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
      "chorus_flower",
      p_360327_ -> new ChorusFlowerBlock(CHORUS_PLANT, p_360327_),
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
      "purpur_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_MAGENTA)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block PURPUR_PILLAR = register(
      "purpur_pillar",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_MAGENTA)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block PURPUR_STAIRS = registerLegacyStair("purpur_stairs", PURPUR_BLOCK);
   public static final Block END_STONE_BRICKS = register(
      "end_stone_bricks",
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 9.0F)
   );
   public static final Block TORCHFLOWER_CROP = register(
      "torchflower_crop",
      TorchflowerCropBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
   );
   public static final Block PITCHER_CROP = register(
      "pitcher_crop",
      PitcherCropBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
   );
   public static final Block PITCHER_PLANT = register(
      "pitcher_plant",
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
      "beetroots",
      BeetrootBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
   );
   public static final Block DIRT_PATH = register(
      "dirt_path",
      DirtPathBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.DIRT)
         .strength(0.65F)
         .sound(SoundType.GRASS)
         .isViewBlocking(Blocks::always)
         .isSuffocating(Blocks::always)
   );
   public static final Block END_GATEWAY = register(
      "end_gateway",
      EndGatewayBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .noCollision()
         .lightLevel(p_187435_ -> 15)
         .strength(-1.0F, 3600000.0F)
         .noLootTable()
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block REPEATING_COMMAND_BLOCK = register(
      "repeating_command_block",
      p_360166_ -> new CommandBlock(false, p_360166_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
   );
   public static final Block CHAIN_COMMAND_BLOCK = register(
      "chain_command_block",
      p_360371_ -> new CommandBlock(true, p_360371_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
   );
   public static final Block FROSTED_ICE = register(
      "frosted_ice",
      FrostedIceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.ICE)
         .friction(0.98F)
         .strength(0.5F)
         .sound(SoundType.GLASS)
         .noOcclusion()
         .isValidSpawn((p_152645_, p_152646_, p_152647_, p_152648_) -> p_152648_ == EntityType.POLAR_BEAR)
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block MAGMA_BLOCK = register(
      "magma_block",
      MagmaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .lightLevel(p_152684_ -> 3)
         .strength(0.5F)
         .isValidSpawn((p_187421_, p_187422_, p_187423_, p_187424_) -> p_187424_.fireImmune())
         .hasPostProcess(Blocks::always)
         .emissiveRendering(Blocks::always)
   );
   public static final Block NETHER_WART_BLOCK = register(
      "nether_wart_block", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.WART_BLOCK)
   );
   public static final Block RED_NETHER_BRICKS = register(
      "red_nether_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.NETHER_BRICKS)
   );
   public static final Block BONE_BLOCK = register(
      "bone_block",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.SAND)
         .instrument(NoteBlockInstrument.XYLOPHONE)
         .requiresCorrectToolForDrops()
         .strength(2.0F)
         .sound(SoundType.BONE_BLOCK)
   );
   public static final Block STRUCTURE_VOID = register(
      "structure_void",
      StructureVoidBlock::new,
      BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().noTerrainParticles().pushReaction(PushReaction.DESTROY)
   );
   public static final Block OBSERVER = register(
      "observer",
      ObserverBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .strength(3.0F)
         .requiresCorrectToolForDrops()
         .isRedstoneConductor(Blocks::never)
   );
   public static final Block SHULKER_BOX = register(
      "shulker_box", p_359996_ -> new ShulkerBoxBlock(null, p_359996_), shulkerBoxProperties(MapColor.COLOR_PURPLE)
   );
   public static final Block WHITE_SHULKER_BOX = register(
      "white_shulker_box", p_360215_ -> new ShulkerBoxBlock(DyeColor.WHITE, p_360215_), shulkerBoxProperties(MapColor.SNOW)
   );
   public static final Block ORANGE_SHULKER_BOX = register(
      "orange_shulker_box", p_360400_ -> new ShulkerBoxBlock(DyeColor.ORANGE, p_360400_), shulkerBoxProperties(MapColor.COLOR_ORANGE)
   );
   public static final Block MAGENTA_SHULKER_BOX = register(
      "magenta_shulker_box", p_360276_ -> new ShulkerBoxBlock(DyeColor.MAGENTA, p_360276_), shulkerBoxProperties(MapColor.COLOR_MAGENTA)
   );
   public static final Block LIGHT_BLUE_SHULKER_BOX = register(
      "light_blue_shulker_box", p_359993_ -> new ShulkerBoxBlock(DyeColor.LIGHT_BLUE, p_359993_), shulkerBoxProperties(MapColor.COLOR_LIGHT_BLUE)
   );
   public static final Block YELLOW_SHULKER_BOX = register(
      "yellow_shulker_box", p_360075_ -> new ShulkerBoxBlock(DyeColor.YELLOW, p_360075_), shulkerBoxProperties(MapColor.COLOR_YELLOW)
   );
   public static final Block LIME_SHULKER_BOX = register(
      "lime_shulker_box", p_360151_ -> new ShulkerBoxBlock(DyeColor.LIME, p_360151_), shulkerBoxProperties(MapColor.COLOR_LIGHT_GREEN)
   );
   public static final Block PINK_SHULKER_BOX = register(
      "pink_shulker_box", p_360002_ -> new ShulkerBoxBlock(DyeColor.PINK, p_360002_), shulkerBoxProperties(MapColor.COLOR_PINK)
   );
   public static final Block GRAY_SHULKER_BOX = register(
      "gray_shulker_box", p_360095_ -> new ShulkerBoxBlock(DyeColor.GRAY, p_360095_), shulkerBoxProperties(MapColor.COLOR_GRAY)
   );
   public static final Block LIGHT_GRAY_SHULKER_BOX = register(
      "light_gray_shulker_box", p_359992_ -> new ShulkerBoxBlock(DyeColor.LIGHT_GRAY, p_359992_), shulkerBoxProperties(MapColor.COLOR_LIGHT_GRAY)
   );
   public static final Block CYAN_SHULKER_BOX = register(
      "cyan_shulker_box", p_360007_ -> new ShulkerBoxBlock(DyeColor.CYAN, p_360007_), shulkerBoxProperties(MapColor.COLOR_CYAN)
   );
   public static final Block PURPLE_SHULKER_BOX = register(
      "purple_shulker_box", p_360156_ -> new ShulkerBoxBlock(DyeColor.PURPLE, p_360156_), shulkerBoxProperties(MapColor.TERRACOTTA_PURPLE)
   );
   public static final Block BLUE_SHULKER_BOX = register(
      "blue_shulker_box", p_360220_ -> new ShulkerBoxBlock(DyeColor.BLUE, p_360220_), shulkerBoxProperties(MapColor.COLOR_BLUE)
   );
   public static final Block BROWN_SHULKER_BOX = register(
      "brown_shulker_box", p_360099_ -> new ShulkerBoxBlock(DyeColor.BROWN, p_360099_), shulkerBoxProperties(MapColor.COLOR_BROWN)
   );
   public static final Block GREEN_SHULKER_BOX = register(
      "green_shulker_box", p_360392_ -> new ShulkerBoxBlock(DyeColor.GREEN, p_360392_), shulkerBoxProperties(MapColor.COLOR_GREEN)
   );
   public static final Block RED_SHULKER_BOX = register(
      "red_shulker_box", p_360068_ -> new ShulkerBoxBlock(DyeColor.RED, p_360068_), shulkerBoxProperties(MapColor.COLOR_RED)
   );
   public static final Block BLACK_SHULKER_BOX = register(
      "black_shulker_box", p_360032_ -> new ShulkerBoxBlock(DyeColor.BLACK, p_360032_), shulkerBoxProperties(MapColor.COLOR_BLACK)
   );
   public static final Block WHITE_GLAZED_TERRACOTTA = register(
      "white_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.WHITE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block ORANGE_GLAZED_TERRACOTTA = register(
      "orange_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block MAGENTA_GLAZED_TERRACOTTA = register(
      "magenta_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.MAGENTA)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block LIGHT_BLUE_GLAZED_TERRACOTTA = register(
      "light_blue_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.LIGHT_BLUE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block YELLOW_GLAZED_TERRACOTTA = register(
      "yellow_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.YELLOW)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block LIME_GLAZED_TERRACOTTA = register(
      "lime_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.LIME)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block PINK_GLAZED_TERRACOTTA = register(
      "pink_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.PINK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block GRAY_GLAZED_TERRACOTTA = register(
      "gray_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block LIGHT_GRAY_GLAZED_TERRACOTTA = register(
      "light_gray_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.LIGHT_GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block CYAN_GLAZED_TERRACOTTA = register(
      "cyan_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.CYAN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block PURPLE_GLAZED_TERRACOTTA = register(
      "purple_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.PURPLE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block BLUE_GLAZED_TERRACOTTA = register(
      "blue_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.BLUE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block BROWN_GLAZED_TERRACOTTA = register(
      "brown_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.BROWN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block GREEN_GLAZED_TERRACOTTA = register(
      "green_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.GREEN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block RED_GLAZED_TERRACOTTA = register(
      "red_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.RED)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block BLACK_GLAZED_TERRACOTTA = register(
      "black_glazed_terracotta",
      GlazedTerracottaBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(DyeColor.BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.4F)
         .pushReaction(PushReaction.PUSH_ONLY)
   );
   public static final Block WHITE_CONCRETE = register(
      "white_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.WHITE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block ORANGE_CONCRETE = register(
      "orange_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block MAGENTA_CONCRETE = register(
      "magenta_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.MAGENTA).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block LIGHT_BLUE_CONCRETE = register(
      "light_blue_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.LIGHT_BLUE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block YELLOW_CONCRETE = register(
      "yellow_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.YELLOW).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block LIME_CONCRETE = register(
      "lime_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.LIME).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block PINK_CONCRETE = register(
      "pink_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.PINK).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block GRAY_CONCRETE = register(
      "gray_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block LIGHT_GRAY_CONCRETE = register(
      "light_gray_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block CYAN_CONCRETE = register(
      "cyan_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block PURPLE_CONCRETE = register(
      "purple_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.PURPLE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block BLUE_CONCRETE = register(
      "blue_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.BLUE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block BROWN_CONCRETE = register(
      "brown_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.BROWN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block GREEN_CONCRETE = register(
      "green_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.GREEN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block RED_CONCRETE = register(
      "red_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block BLACK_CONCRETE = register(
      "black_concrete",
      BlockBehaviour.Properties.of().mapColor(DyeColor.BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.8F)
   );
   public static final Block WHITE_CONCRETE_POWDER = register(
      "white_concrete_powder",
      p_360053_ -> new ConcretePowderBlock(WHITE_CONCRETE, p_360053_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.WHITE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block ORANGE_CONCRETE_POWDER = register(
      "orange_concrete_powder",
      p_360226_ -> new ConcretePowderBlock(ORANGE_CONCRETE, p_360226_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.ORANGE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block MAGENTA_CONCRETE_POWDER = register(
      "magenta_concrete_powder",
      p_360367_ -> new ConcretePowderBlock(MAGENTA_CONCRETE, p_360367_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.MAGENTA).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block LIGHT_BLUE_CONCRETE_POWDER = register(
      "light_blue_concrete_powder",
      p_360092_ -> new ConcretePowderBlock(LIGHT_BLUE_CONCRETE, p_360092_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.LIGHT_BLUE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block YELLOW_CONCRETE_POWDER = register(
      "yellow_concrete_powder",
      p_360030_ -> new ConcretePowderBlock(YELLOW_CONCRETE, p_360030_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.YELLOW).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block LIME_CONCRETE_POWDER = register(
      "lime_concrete_powder",
      p_360384_ -> new ConcretePowderBlock(LIME_CONCRETE, p_360384_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.LIME).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block PINK_CONCRETE_POWDER = register(
      "pink_concrete_powder",
      p_360093_ -> new ConcretePowderBlock(PINK_CONCRETE, p_360093_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.PINK).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block GRAY_CONCRETE_POWDER = register(
      "gray_concrete_powder",
      p_360110_ -> new ConcretePowderBlock(GRAY_CONCRETE, p_360110_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.GRAY).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block LIGHT_GRAY_CONCRETE_POWDER = register(
      "light_gray_concrete_powder",
      p_360216_ -> new ConcretePowderBlock(LIGHT_GRAY_CONCRETE, p_360216_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.LIGHT_GRAY).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block CYAN_CONCRETE_POWDER = register(
      "cyan_concrete_powder",
      p_360188_ -> new ConcretePowderBlock(CYAN_CONCRETE, p_360188_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.CYAN).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block PURPLE_CONCRETE_POWDER = register(
      "purple_concrete_powder",
      p_360170_ -> new ConcretePowderBlock(PURPLE_CONCRETE, p_360170_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.PURPLE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block BLUE_CONCRETE_POWDER = register(
      "blue_concrete_powder",
      p_360003_ -> new ConcretePowderBlock(BLUE_CONCRETE, p_360003_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.BLUE).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block BROWN_CONCRETE_POWDER = register(
      "brown_concrete_powder",
      p_360050_ -> new ConcretePowderBlock(BROWN_CONCRETE, p_360050_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.BROWN).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block GREEN_CONCRETE_POWDER = register(
      "green_concrete_powder",
      p_360157_ -> new ConcretePowderBlock(GREEN_CONCRETE, p_360157_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.GREEN).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block RED_CONCRETE_POWDER = register(
      "red_concrete_powder",
      p_359991_ -> new ConcretePowderBlock(RED_CONCRETE, p_359991_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.RED).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block BLACK_CONCRETE_POWDER = register(
      "black_concrete_powder",
      p_360334_ -> new ConcretePowderBlock(BLACK_CONCRETE, p_360334_),
      BlockBehaviour.Properties.of().mapColor(DyeColor.BLACK).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)
   );
   public static final Block KELP = register(
      "kelp",
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
      "kelp_plant",
      KelpPlantBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WATER).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block DRIED_KELP_BLOCK = register(
      "dried_kelp_block", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.5F, 2.5F).sound(SoundType.GRASS)
   );
   public static final Block TURTLE_EGG = register(
      "turtle_egg",
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
      "sniffer_egg", SnifferEggBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.METAL).noOcclusion()
   );
   public static final Block DRIED_GHAST = register(
      "dried_ghast",
      DriedGhastBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).forceSolidOn().instabreak().sound(SoundType.DRIED_GHAST).noOcclusion().randomTicks()
   );
   public static final Block DEAD_TUBE_CORAL_BLOCK = register(
      "dead_tube_coral_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GRAY)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block DEAD_BRAIN_CORAL_BLOCK = register(
      "dead_brain_coral_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GRAY)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block DEAD_BUBBLE_CORAL_BLOCK = register(
      "dead_bubble_coral_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GRAY)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block DEAD_FIRE_CORAL_BLOCK = register(
      "dead_fire_coral_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GRAY)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block DEAD_HORN_CORAL_BLOCK = register(
      "dead_horn_coral_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GRAY)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block TUBE_CORAL_BLOCK = register(
      "tube_coral_block",
      p_360245_ -> new CoralBlock(DEAD_TUBE_CORAL_BLOCK, p_360245_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLUE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
         .sound(SoundType.CORAL_BLOCK)
   );
   public static final Block BRAIN_CORAL_BLOCK = register(
      "brain_coral_block",
      p_359990_ -> new CoralBlock(DEAD_BRAIN_CORAL_BLOCK, p_359990_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_PINK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
         .sound(SoundType.CORAL_BLOCK)
   );
   public static final Block BUBBLE_CORAL_BLOCK = register(
      "bubble_coral_block",
      p_360108_ -> new CoralBlock(DEAD_BUBBLE_CORAL_BLOCK, p_360108_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_PURPLE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
         .sound(SoundType.CORAL_BLOCK)
   );
   public static final Block FIRE_CORAL_BLOCK = register(
      "fire_coral_block",
      p_359978_ -> new CoralBlock(DEAD_FIRE_CORAL_BLOCK, p_359978_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_RED)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
         .sound(SoundType.CORAL_BLOCK)
   );
   public static final Block HORN_CORAL_BLOCK = register(
      "horn_coral_block",
      p_360354_ -> new CoralBlock(DEAD_HORN_CORAL_BLOCK, p_360354_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_YELLOW)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
         .sound(SoundType.CORAL_BLOCK)
   );
   public static final Block DEAD_TUBE_CORAL = register(
      "dead_tube_coral",
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
      "dead_brain_coral",
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
      "dead_bubble_coral",
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
      "dead_fire_coral",
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
      "dead_horn_coral",
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
      "tube_coral",
      p_360323_ -> new CoralPlantBlock(DEAD_TUBE_CORAL, p_360323_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BRAIN_CORAL = register(
      "brain_coral",
      p_360094_ -> new CoralPlantBlock(DEAD_BRAIN_CORAL, p_360094_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BUBBLE_CORAL = register(
      "bubble_coral",
      p_360387_ -> new CoralPlantBlock(DEAD_BUBBLE_CORAL, p_360387_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block FIRE_CORAL = register(
      "fire_coral",
      p_360130_ -> new CoralPlantBlock(DEAD_FIRE_CORAL, p_360130_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block HORN_CORAL = register(
      "horn_coral",
      p_360031_ -> new CoralPlantBlock(DEAD_HORN_CORAL, p_360031_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block DEAD_TUBE_CORAL_FAN = register(
      "dead_tube_coral_fan",
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
      "dead_brain_coral_fan",
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
      "dead_bubble_coral_fan",
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
      "dead_fire_coral_fan",
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
      "dead_horn_coral_fan",
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
      "tube_coral_fan",
      p_360259_ -> new CoralFanBlock(DEAD_TUBE_CORAL_FAN, p_360259_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BRAIN_CORAL_FAN = register(
      "brain_coral_fan",
      p_360246_ -> new CoralFanBlock(DEAD_BRAIN_CORAL_FAN, p_360246_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BUBBLE_CORAL_FAN = register(
      "bubble_coral_fan",
      p_360051_ -> new CoralFanBlock(DEAD_BUBBLE_CORAL_FAN, p_360051_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block FIRE_CORAL_FAN = register(
      "fire_coral_fan",
      p_360082_ -> new CoralFanBlock(DEAD_FIRE_CORAL_FAN, p_360082_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block HORN_CORAL_FAN = register(
      "horn_coral_fan",
      p_360237_ -> new CoralFanBlock(DEAD_HORN_CORAL_FAN, p_360237_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block DEAD_TUBE_CORAL_WALL_FAN = register(
      "dead_tube_coral_wall_fan",
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
      "dead_brain_coral_wall_fan",
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
      "dead_bubble_coral_wall_fan",
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
      "dead_fire_coral_wall_fan",
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
      "dead_horn_coral_wall_fan",
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
      "tube_coral_wall_fan",
      p_359983_ -> new CoralWallFanBlock(DEAD_TUBE_CORAL_WALL_FAN, p_359983_),
      wallVariant(TUBE_CORAL_FAN, false).mapColor(MapColor.COLOR_BLUE).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BRAIN_CORAL_WALL_FAN = register(
      "brain_coral_wall_fan",
      p_360070_ -> new CoralWallFanBlock(DEAD_BRAIN_CORAL_WALL_FAN, p_360070_),
      wallVariant(BRAIN_CORAL_FAN, false)
         .mapColor(MapColor.COLOR_PINK)
         .noCollision()
         .instabreak()
         .sound(SoundType.WET_GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BUBBLE_CORAL_WALL_FAN = register(
      "bubble_coral_wall_fan",
      p_360100_ -> new CoralWallFanBlock(DEAD_BUBBLE_CORAL_WALL_FAN, p_360100_),
      wallVariant(BUBBLE_CORAL_FAN, false)
         .mapColor(MapColor.COLOR_PURPLE)
         .noCollision()
         .instabreak()
         .sound(SoundType.WET_GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block FIRE_CORAL_WALL_FAN = register(
      "fire_coral_wall_fan",
      p_360027_ -> new CoralWallFanBlock(DEAD_FIRE_CORAL_WALL_FAN, p_360027_),
      wallVariant(FIRE_CORAL_FAN, false).mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block HORN_CORAL_WALL_FAN = register(
      "horn_coral_wall_fan",
      p_360016_ -> new CoralWallFanBlock(DEAD_HORN_CORAL_WALL_FAN, p_360016_),
      wallVariant(HORN_CORAL_FAN, false)
         .mapColor(MapColor.COLOR_YELLOW)
         .noCollision()
         .instabreak()
         .sound(SoundType.WET_GRASS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block SEA_PICKLE = register(
      "sea_pickle",
      SeaPickleBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_GREEN)
         .lightLevel(p_152680_ -> SeaPickleBlock.isDead(p_152680_) ? 0 : 3 + 3 * p_152680_.getValue(SeaPickleBlock.PICKLES))
         .sound(SoundType.SLIME_BLOCK)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block BLUE_ICE = register(
      "blue_ice", HalfTransparentBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(2.8F).friction(0.989F).sound(SoundType.GLASS)
   );
   public static final Block CONDUIT = register(
      "conduit",
      ConduitBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.DIAMOND)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.HAT)
         .strength(3.0F)
         .lightLevel(p_50804_ -> 15)
         .noOcclusion()
   );
   public static final Block BAMBOO_SAPLING = register(
      "bamboo_sapling",
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
      "bamboo",
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
   public static final Block POTTED_BAMBOO = register("potted_bamboo", p_360331_ -> new FlowerPotBlock(BAMBOO, p_360331_), flowerPotProperties());
   public static final Block VOID_AIR = register("void_air", AirBlock::new, BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().air());
   public static final Block CAVE_AIR = register("cave_air", AirBlock::new, BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().air());
   public static final Block BUBBLE_COLUMN = register(
      "bubble_column",
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
   public static final Block POLISHED_GRANITE_STAIRS = registerLegacyStair("polished_granite_stairs", POLISHED_GRANITE);
   public static final Block SMOOTH_RED_SANDSTONE_STAIRS = registerLegacyStair("smooth_red_sandstone_stairs", SMOOTH_RED_SANDSTONE);
   public static final Block MOSSY_STONE_BRICK_STAIRS = registerLegacyStair("mossy_stone_brick_stairs", MOSSY_STONE_BRICKS);
   public static final Block POLISHED_DIORITE_STAIRS = registerLegacyStair("polished_diorite_stairs", POLISHED_DIORITE);
   public static final Block MOSSY_COBBLESTONE_STAIRS = registerLegacyStair("mossy_cobblestone_stairs", MOSSY_COBBLESTONE);
   public static final Block END_STONE_BRICK_STAIRS = registerLegacyStair("end_stone_brick_stairs", END_STONE_BRICKS);
   public static final Block STONE_STAIRS = registerLegacyStair("stone_stairs", STONE);
   public static final Block SMOOTH_SANDSTONE_STAIRS = registerLegacyStair("smooth_sandstone_stairs", SMOOTH_SANDSTONE);
   public static final Block SMOOTH_QUARTZ_STAIRS = registerLegacyStair("smooth_quartz_stairs", SMOOTH_QUARTZ);
   public static final Block GRANITE_STAIRS = registerLegacyStair("granite_stairs", GRANITE);
   public static final Block ANDESITE_STAIRS = registerLegacyStair("andesite_stairs", ANDESITE);
   public static final Block RED_NETHER_BRICK_STAIRS = registerLegacyStair("red_nether_brick_stairs", RED_NETHER_BRICKS);
   public static final Block POLISHED_ANDESITE_STAIRS = registerLegacyStair("polished_andesite_stairs", POLISHED_ANDESITE);
   public static final Block DIORITE_STAIRS = registerLegacyStair("diorite_stairs", DIORITE);
   public static final Block POLISHED_GRANITE_SLAB = register("polished_granite_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_GRANITE));
   public static final Block SMOOTH_RED_SANDSTONE_SLAB = register(
      "smooth_red_sandstone_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SMOOTH_RED_SANDSTONE)
   );
   public static final Block MOSSY_STONE_BRICK_SLAB = register(
      "mossy_stone_brick_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MOSSY_STONE_BRICKS)
   );
   public static final Block POLISHED_DIORITE_SLAB = register("polished_diorite_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_DIORITE));
   public static final Block MOSSY_COBBLESTONE_SLAB = register(
      "mossy_cobblestone_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MOSSY_COBBLESTONE)
   );
   public static final Block END_STONE_BRICK_SLAB = register("end_stone_brick_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(END_STONE_BRICKS));
   public static final Block SMOOTH_SANDSTONE_SLAB = register("smooth_sandstone_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SMOOTH_SANDSTONE));
   public static final Block SMOOTH_QUARTZ_SLAB = register("smooth_quartz_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SMOOTH_QUARTZ));
   public static final Block GRANITE_SLAB = register("granite_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(GRANITE));
   public static final Block ANDESITE_SLAB = register("andesite_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(ANDESITE));
   public static final Block RED_NETHER_BRICK_SLAB = register(
      "red_nether_brick_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(RED_NETHER_BRICKS)
   );
   public static final Block POLISHED_ANDESITE_SLAB = register(
      "polished_andesite_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_ANDESITE)
   );
   public static final Block DIORITE_SLAB = register("diorite_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DIORITE));
   public static final Block BRICK_WALL = register("brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(BRICKS).forceSolidOn());
   public static final Block PRISMARINE_WALL = register("prismarine_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(PRISMARINE).forceSolidOn());
   public static final Block RED_SANDSTONE_WALL = register(
      "red_sandstone_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(RED_SANDSTONE).forceSolidOn()
   );
   public static final Block MOSSY_STONE_BRICK_WALL = register(
      "mossy_stone_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MOSSY_STONE_BRICKS).forceSolidOn()
   );
   public static final Block GRANITE_WALL = register("granite_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(GRANITE).forceSolidOn());
   public static final Block STONE_BRICK_WALL = register(
      "stone_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(STONE_BRICKS).forceSolidOn()
   );
   public static final Block MUD_BRICK_WALL = register("mud_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(MUD_BRICKS).forceSolidOn());
   public static final Block NETHER_BRICK_WALL = register(
      "nether_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(NETHER_BRICKS).forceSolidOn()
   );
   public static final Block ANDESITE_WALL = register("andesite_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(ANDESITE).forceSolidOn());
   public static final Block RED_NETHER_BRICK_WALL = register(
      "red_nether_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(RED_NETHER_BRICKS).forceSolidOn()
   );
   public static final Block SANDSTONE_WALL = register("sandstone_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SANDSTONE).forceSolidOn());
   public static final Block END_STONE_BRICK_WALL = register(
      "end_stone_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(END_STONE_BRICKS).forceSolidOn()
   );
   public static final Block DIORITE_WALL = register("diorite_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DIORITE).forceSolidOn());
   public static final Block SCAFFOLDING = register(
      "scaffolding",
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
      "loom",
      LoomBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block BARREL = register(
      "barrel",
      BarrelBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block SMOKER = register(
      "smoker",
      SmokerBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(3.5F)
         .lightLevel(litBlockEmission(13))
   );
   public static final Block BLAST_FURNACE = register(
      "blast_furnace",
      BlastFurnaceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(3.5F)
         .lightLevel(litBlockEmission(13))
   );
   public static final Block CARTOGRAPHY_TABLE = register(
      "cartography_table",
      CartographyTableBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block FLETCHING_TABLE = register(
      "fletching_table",
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block GRINDSTONE = register(
      "grindstone",
      GrindstoneBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.STONE)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block LECTERN = register(
      "lectern",
      LecternBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block SMITHING_TABLE = register(
      "smithing_table",
      SmithingTableBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block STONECUTTER = register(
      "stonecutter",
      StonecutterBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F)
   );
   public static final Block BELL = register(
      "bell",
      BellBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().strength(5.0F).sound(SoundType.ANVIL).pushReaction(PushReaction.DESTROY)
   );
   public static final Block LANTERN = register(
      "lantern",
      LanternBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .forceSolidOn()
         .strength(3.5F)
         .sound(SoundType.LANTERN)
         .lightLevel(p_152677_ -> 15)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block SOUL_LANTERN = register(
      "soul_lantern",
      LanternBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .forceSolidOn()
         .strength(3.5F)
         .sound(SoundType.LANTERN)
         .lightLevel(p_187433_ -> 10)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final WeatheringCopperBlocks COPPER_LANTERN = WeatheringCopperBlocks.create(
      "copper_lantern",
      Blocks::register,
      LanternBlock::new,
      WeatheringLanternBlock::new,
      p_422053_ -> BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .forceSolidOn()
         .strength(3.5F)
         .sound(SoundType.LANTERN)
         .lightLevel(p_187431_ -> 15)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CAMPFIRE = register(
      "campfire",
      p_360322_ -> new CampfireBlock(true, 1, p_360322_),
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
      "soul_campfire",
      p_360373_ -> new CampfireBlock(false, 2, p_360373_),
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
      "sweet_berry_bush",
      SweetBerryBushBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollision().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)
   );
   public static final Block WARPED_STEM = register("warped_stem", RotatedPillarBlock::new, netherStemProperties(MapColor.WARPED_STEM));
   public static final Block STRIPPED_WARPED_STEM = register("stripped_warped_stem", RotatedPillarBlock::new, netherStemProperties(MapColor.WARPED_STEM));
   public static final Block WARPED_HYPHAE = register(
      "warped_hyphae",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
   );
   public static final Block STRIPPED_WARPED_HYPHAE = register(
      "stripped_warped_hyphae",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
   );
   public static final Block WARPED_NYLIUM = register(
      "warped_nylium",
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
      "warped_fungus",
      p_360243_ -> new FungusBlock(TreeFeatures.WARPED_FUNGUS_PLANTED, WARPED_NYLIUM, p_360243_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instabreak().noCollision().sound(SoundType.FUNGUS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block WARPED_WART_BLOCK = register(
      "warped_wart_block", BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_WART_BLOCK).strength(1.0F).sound(SoundType.WART_BLOCK)
   );
   public static final Block WARPED_ROOTS = register(
      "warped_roots",
      RootsBlock::new,
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
      "nether_sprouts",
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
   public static final Block CRIMSON_STEM = register("crimson_stem", RotatedPillarBlock::new, netherStemProperties(MapColor.CRIMSON_STEM));
   public static final Block STRIPPED_CRIMSON_STEM = register("stripped_crimson_stem", RotatedPillarBlock::new, netherStemProperties(MapColor.CRIMSON_STEM));
   public static final Block CRIMSON_HYPHAE = register(
      "crimson_hyphae",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
   );
   public static final Block STRIPPED_CRIMSON_HYPHAE = register(
      "stripped_crimson_hyphae",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_HYPHAE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM)
   );
   public static final Block CRIMSON_NYLIUM = register(
      "crimson_nylium",
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
      "crimson_fungus",
      p_360314_ -> new FungusBlock(TreeFeatures.CRIMSON_FUNGUS_PLANTED, CRIMSON_NYLIUM, p_360314_),
      BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).instabreak().noCollision().sound(SoundType.FUNGUS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block SHROOMLIGHT = register(
      "shroomlight", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.SHROOMLIGHT).lightLevel(p_152663_ -> 15)
   );
   public static final Block WEEPING_VINES = register(
      "weeping_vines",
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
      "weeping_vines_plant",
      WeepingVinesPlantBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).noCollision().instabreak().sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.DESTROY)
   );
   public static final Block TWISTING_VINES = register(
      "twisting_vines",
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
      "twisting_vines_plant",
      TwistingVinesPlantBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().instabreak().sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.DESTROY)
   );
   public static final Block CRIMSON_ROOTS = register(
      "crimson_roots",
      RootsBlock::new,
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
      "crimson_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_STEM).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD)
   );
   public static final Block WARPED_PLANKS = register(
      "warped_planks",
      BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD)
   );
   public static final Block CRIMSON_SLAB = register(
      "crimson_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.NETHER_WOOD)
   );
   public static final Block WARPED_SLAB = register(
      "warped_slab",
      SlabBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(WARPED_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.NETHER_WOOD)
   );
   public static final Block CRIMSON_PRESSURE_PLATE = register(
      "crimson_pressure_plate",
      p_360222_ -> new PressurePlateBlock(BlockSetType.CRIMSON, p_360222_),
      BlockBehaviour.Properties.of()
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(0.5F)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block WARPED_PRESSURE_PLATE = register(
      "warped_pressure_plate",
      p_360155_ -> new PressurePlateBlock(BlockSetType.WARPED, p_360155_),
      BlockBehaviour.Properties.of()
         .mapColor(WARPED_PLANKS.defaultMapColor())
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASS)
         .noCollision()
         .strength(0.5F)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CRIMSON_FENCE = register(
      "crimson_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.NETHER_WOOD)
   );
   public static final Block WARPED_FENCE = register(
      "warped_fence",
      FenceBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(WARPED_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F, 3.0F)
         .sound(SoundType.NETHER_WOOD)
   );
   public static final Block CRIMSON_TRAPDOOR = register(
      "crimson_trapdoor",
      p_360287_ -> new TrapDoorBlock(BlockSetType.CRIMSON, p_360287_),
      BlockBehaviour.Properties.of()
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
   );
   public static final Block WARPED_TRAPDOOR = register(
      "warped_trapdoor",
      p_360357_ -> new TrapDoorBlock(BlockSetType.WARPED, p_360357_),
      BlockBehaviour.Properties.of()
         .mapColor(WARPED_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .isValidSpawn(Blocks::never)
   );
   public static final Block CRIMSON_FENCE_GATE = register(
      "crimson_fence_gate",
      p_360381_ -> new FenceGateBlock(WoodType.CRIMSON, p_360381_),
      BlockBehaviour.Properties.of().mapColor(CRIMSON_PLANKS.defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
   );
   public static final Block WARPED_FENCE_GATE = register(
      "warped_fence_gate",
      p_360001_ -> new FenceGateBlock(WoodType.WARPED, p_360001_),
      BlockBehaviour.Properties.of().mapColor(WARPED_PLANKS.defaultMapColor()).forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F)
   );
   public static final Block CRIMSON_STAIRS = registerLegacyStair("crimson_stairs", CRIMSON_PLANKS);
   public static final Block WARPED_STAIRS = registerLegacyStair("warped_stairs", WARPED_PLANKS);
   public static final Block CRIMSON_BUTTON = register("crimson_button", p_360202_ -> new ButtonBlock(BlockSetType.CRIMSON, 30, p_360202_), buttonProperties());
   public static final Block WARPED_BUTTON = register("warped_button", p_360103_ -> new ButtonBlock(BlockSetType.WARPED, 30, p_360103_), buttonProperties());
   public static final Block CRIMSON_DOOR = register(
      "crimson_door",
      p_360308_ -> new DoorBlock(BlockSetType.CRIMSON, p_360308_),
      BlockBehaviour.Properties.of()
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block WARPED_DOOR = register(
      "warped_door",
      p_360033_ -> new DoorBlock(BlockSetType.WARPED, p_360033_),
      BlockBehaviour.Properties.of()
         .mapColor(WARPED_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .strength(3.0F)
         .noOcclusion()
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block CRIMSON_SIGN = register(
      "crimson_sign",
      p_360333_ -> new StandingSignBlock(WoodType.CRIMSON, p_360333_),
      BlockBehaviour.Properties.of()
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .forceSolidOn()
         .noCollision()
         .strength(1.0F)
   );
   public static final Block WARPED_SIGN = register(
      "warped_sign",
      p_360380_ -> new StandingSignBlock(WoodType.WARPED, p_360380_),
      BlockBehaviour.Properties.of().mapColor(WARPED_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).forceSolidOn().noCollision().strength(1.0F)
   );
   public static final Block CRIMSON_WALL_SIGN = register(
      "crimson_wall_sign",
      p_360279_ -> new WallSignBlock(WoodType.CRIMSON, p_360279_),
      wallVariant(CRIMSON_SIGN, true)
         .mapColor(CRIMSON_PLANKS.defaultMapColor())
         .instrument(NoteBlockInstrument.BASS)
         .forceSolidOn()
         .noCollision()
         .strength(1.0F)
   );
   public static final Block WARPED_WALL_SIGN = register(
      "warped_wall_sign",
      p_360144_ -> new WallSignBlock(WoodType.WARPED, p_360144_),
      wallVariant(WARPED_SIGN, true).mapColor(WARPED_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).forceSolidOn().noCollision().strength(1.0F)
   );
   public static final Block STRUCTURE_BLOCK = register(
      "structure_block",
      StructureBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
   );
   public static final Block JIGSAW = register(
      "jigsaw",
      JigsawBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable()
   );
   public static final Block TEST_BLOCK = register(
      "test_block", TestBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0F, 3600000.0F).noLootTable()
   );
   public static final Block TEST_INSTANCE_BLOCK = register(
      "test_instance_block",
      TestInstanceBlock::new,
      BlockBehaviour.Properties.of().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable().isViewBlocking(Blocks::never)
   );
   public static final Block COMPOSTER = register(
      "composter",
      ComposterBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.6F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block TARGET = register(
      "target", TargetBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.5F).sound(SoundType.GRASS)
   );
   public static final Block BEE_NEST = register(
      "bee_nest",
      BeehiveBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BASS).strength(0.3F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block BEEHIVE = register(
      "beehive",
      BeehiveBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(0.6F).sound(SoundType.WOOD).ignitedByLava()
   );
   public static final Block HONEY_BLOCK = register(
      "honey_block",
      HoneyBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).speedFactor(0.4F).jumpFactor(0.5F).noOcclusion().sound(SoundType.HONEY_BLOCK)
   );
   public static final Block HONEYCOMB_BLOCK = register(
      "honeycomb_block", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.6F).sound(SoundType.CORAL_BLOCK)
   );
   public static final Block NETHERITE_BLOCK = register(
      "netherite_block",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
   );
   public static final Block ANCIENT_DEBRIS = register(
      "ancient_debris",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(30.0F, 1200.0F).sound(SoundType.ANCIENT_DEBRIS)
   );
   public static final Block CRYING_OBSIDIAN = register(
      "crying_obsidian",
      CryingObsidianBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(50.0F, 1200.0F)
         .lightLevel(p_152651_ -> 10)
   );
   public static final Block RESPAWN_ANCHOR = register(
      "respawn_anchor",
      RespawnAnchorBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(50.0F, 1200.0F)
         .lightLevel(p_152639_ -> RespawnAnchorBlock.getScaledChargeLevel(p_152639_, 15))
   );
   public static final Block POTTED_CRIMSON_FUNGUS = register(
      "potted_crimson_fungus", p_360273_ -> new FlowerPotBlock(CRIMSON_FUNGUS, p_360273_), flowerPotProperties()
   );
   public static final Block POTTED_WARPED_FUNGUS = register(
      "potted_warped_fungus", p_359976_ -> new FlowerPotBlock(WARPED_FUNGUS, p_359976_), flowerPotProperties()
   );
   public static final Block POTTED_CRIMSON_ROOTS = register(
      "potted_crimson_roots", p_360203_ -> new FlowerPotBlock(CRIMSON_ROOTS, p_360203_), flowerPotProperties()
   );
   public static final Block POTTED_WARPED_ROOTS = register(
      "potted_warped_roots", p_360288_ -> new FlowerPotBlock(WARPED_ROOTS, p_360288_), flowerPotProperties()
   );
   public static final Block LODESTONE = register(
      "lodestone",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .requiresCorrectToolForDrops()
         .strength(3.5F)
         .sound(SoundType.LODESTONE)
         .pushReaction(PushReaction.BLOCK)
   );
   public static final Block BLACKSTONE = register(
      "blackstone",
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
   );
   public static final Block BLACKSTONE_STAIRS = registerLegacyStair("blackstone_stairs", BLACKSTONE);
   public static final Block BLACKSTONE_WALL = register("blackstone_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).forceSolidOn());
   public static final Block BLACKSTONE_SLAB = register(
      "blackstone_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).strength(2.0F, 6.0F)
   );
   public static final Block POLISHED_BLACKSTONE = register("polished_blackstone", BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).strength(2.0F, 6.0F));
   public static final Block POLISHED_BLACKSTONE_BRICKS = register(
      "polished_blackstone_bricks", BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE).strength(1.5F, 6.0F)
   );
   public static final Block CRACKED_POLISHED_BLACKSTONE_BRICKS = register(
      "cracked_polished_blackstone_bricks", BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE_BRICKS)
   );
   public static final Block CHISELED_POLISHED_BLACKSTONE = register(
      "chiseled_polished_blackstone", BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE).strength(1.5F, 6.0F)
   );
   public static final Block POLISHED_BLACKSTONE_BRICK_SLAB = register(
      "polished_blackstone_brick_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE_BRICKS).strength(2.0F, 6.0F)
   );
   public static final Block POLISHED_BLACKSTONE_BRICK_STAIRS = registerLegacyStair("polished_blackstone_brick_stairs", POLISHED_BLACKSTONE_BRICKS);
   public static final Block POLISHED_BLACKSTONE_BRICK_WALL = register(
      "polished_blackstone_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE_BRICKS).forceSolidOn()
   );
   public static final Block GILDED_BLACKSTONE = register(
      "gilded_blackstone", BlockBehaviour.Properties.ofLegacyCopy(BLACKSTONE).sound(SoundType.GILDED_BLACKSTONE)
   );
   public static final Block POLISHED_BLACKSTONE_STAIRS = registerLegacyStair("polished_blackstone_stairs", POLISHED_BLACKSTONE);
   public static final Block POLISHED_BLACKSTONE_SLAB = register(
      "polished_blackstone_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE)
   );
   public static final Block POLISHED_BLACKSTONE_PRESSURE_PLATE = register(
      "polished_blackstone_pressure_plate",
      p_360182_ -> new PressurePlateBlock(BlockSetType.POLISHED_BLACKSTONE, p_360182_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_BLACK)
         .forceSolidOn()
         .instrument(NoteBlockInstrument.BASEDRUM)
         .noCollision()
         .strength(0.5F)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block POLISHED_BLACKSTONE_BUTTON = register(
      "polished_blackstone_button", p_360149_ -> new ButtonBlock(BlockSetType.STONE, 20, p_360149_), buttonProperties()
   );
   public static final Block POLISHED_BLACKSTONE_WALL = register(
      "polished_blackstone_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_BLACKSTONE).forceSolidOn()
   );
   public static final Block CHISELED_NETHER_BRICKS = register(
      "chiseled_nether_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.NETHER_BRICKS)
   );
   public static final Block CRACKED_NETHER_BRICKS = register(
      "cracked_nether_bricks",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.NETHER)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(2.0F, 6.0F)
         .sound(SoundType.NETHER_BRICKS)
   );
   public static final Block QUARTZ_BRICKS = register("quartz_bricks", BlockBehaviour.Properties.ofLegacyCopy(QUARTZ_BLOCK));
   public static final Block CANDLE = register("candle", CandleBlock::new, candleProperties(MapColor.SAND));
   public static final Block WHITE_CANDLE = register("white_candle", CandleBlock::new, candleProperties(MapColor.WOOL));
   public static final Block ORANGE_CANDLE = register("orange_candle", CandleBlock::new, candleProperties(MapColor.COLOR_ORANGE));
   public static final Block MAGENTA_CANDLE = register("magenta_candle", CandleBlock::new, candleProperties(MapColor.COLOR_MAGENTA));
   public static final Block LIGHT_BLUE_CANDLE = register("light_blue_candle", CandleBlock::new, candleProperties(MapColor.COLOR_LIGHT_BLUE));
   public static final Block YELLOW_CANDLE = register("yellow_candle", CandleBlock::new, candleProperties(MapColor.COLOR_YELLOW));
   public static final Block LIME_CANDLE = register("lime_candle", CandleBlock::new, candleProperties(MapColor.COLOR_LIGHT_GREEN));
   public static final Block PINK_CANDLE = register("pink_candle", CandleBlock::new, candleProperties(MapColor.COLOR_PINK));
   public static final Block GRAY_CANDLE = register("gray_candle", CandleBlock::new, candleProperties(MapColor.COLOR_GRAY));
   public static final Block LIGHT_GRAY_CANDLE = register("light_gray_candle", CandleBlock::new, candleProperties(MapColor.COLOR_LIGHT_GRAY));
   public static final Block CYAN_CANDLE = register("cyan_candle", CandleBlock::new, candleProperties(MapColor.COLOR_CYAN));
   public static final Block PURPLE_CANDLE = register("purple_candle", CandleBlock::new, candleProperties(MapColor.COLOR_PURPLE));
   public static final Block BLUE_CANDLE = register("blue_candle", CandleBlock::new, candleProperties(MapColor.COLOR_BLUE));
   public static final Block BROWN_CANDLE = register("brown_candle", CandleBlock::new, candleProperties(MapColor.COLOR_BROWN));
   public static final Block GREEN_CANDLE = register("green_candle", CandleBlock::new, candleProperties(MapColor.COLOR_GREEN));
   public static final Block RED_CANDLE = register("red_candle", CandleBlock::new, candleProperties(MapColor.COLOR_RED));
   public static final Block BLACK_CANDLE = register("black_candle", CandleBlock::new, candleProperties(MapColor.COLOR_BLACK));
   public static final Block CANDLE_CAKE = register(
      "candle_cake", p_360102_ -> new CandleCakeBlock(CANDLE, p_360102_), BlockBehaviour.Properties.ofLegacyCopy(CAKE).lightLevel(litBlockEmission(3))
   );
   public static final Block WHITE_CANDLE_CAKE = register(
      "white_candle_cake", p_360060_ -> new CandleCakeBlock(WHITE_CANDLE, p_360060_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block ORANGE_CANDLE_CAKE = register(
      "orange_candle_cake", p_360022_ -> new CandleCakeBlock(ORANGE_CANDLE, p_360022_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block MAGENTA_CANDLE_CAKE = register(
      "magenta_candle_cake", p_360138_ -> new CandleCakeBlock(MAGENTA_CANDLE, p_360138_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block LIGHT_BLUE_CANDLE_CAKE = register(
      "light_blue_candle_cake", p_360106_ -> new CandleCakeBlock(LIGHT_BLUE_CANDLE, p_360106_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block YELLOW_CANDLE_CAKE = register(
      "yellow_candle_cake", p_360201_ -> new CandleCakeBlock(YELLOW_CANDLE, p_360201_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block LIME_CANDLE_CAKE = register(
      "lime_candle_cake", p_359988_ -> new CandleCakeBlock(LIME_CANDLE, p_359988_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block PINK_CANDLE_CAKE = register(
      "pink_candle_cake", p_360297_ -> new CandleCakeBlock(PINK_CANDLE, p_360297_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block GRAY_CANDLE_CAKE = register(
      "gray_candle_cake", p_360310_ -> new CandleCakeBlock(GRAY_CANDLE, p_360310_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block LIGHT_GRAY_CANDLE_CAKE = register(
      "light_gray_candle_cake", p_359973_ -> new CandleCakeBlock(LIGHT_GRAY_CANDLE, p_359973_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block CYAN_CANDLE_CAKE = register(
      "cyan_candle_cake", p_360043_ -> new CandleCakeBlock(CYAN_CANDLE, p_360043_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block PURPLE_CANDLE_CAKE = register(
      "purple_candle_cake", p_360139_ -> new CandleCakeBlock(PURPLE_CANDLE, p_360139_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block BLUE_CANDLE_CAKE = register(
      "blue_candle_cake", p_360131_ -> new CandleCakeBlock(BLUE_CANDLE, p_360131_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block BROWN_CANDLE_CAKE = register(
      "brown_candle_cake", p_360104_ -> new CandleCakeBlock(BROWN_CANDLE, p_360104_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block GREEN_CANDLE_CAKE = register(
      "green_candle_cake", p_360370_ -> new CandleCakeBlock(GREEN_CANDLE, p_360370_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block RED_CANDLE_CAKE = register(
      "red_candle_cake", p_360382_ -> new CandleCakeBlock(RED_CANDLE, p_360382_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block BLACK_CANDLE_CAKE = register(
      "black_candle_cake", p_360097_ -> new CandleCakeBlock(BLACK_CANDLE, p_360097_), BlockBehaviour.Properties.ofLegacyCopy(CANDLE_CAKE)
   );
   public static final Block AMETHYST_BLOCK = register(
      "amethyst_block",
      AmethystBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops()
   );
   public static final Block BUDDING_AMETHYST = register(
      "budding_amethyst",
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
      "amethyst_cluster",
      p_360360_ -> new AmethystClusterBlock(7.0F, 10.0F, p_360360_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_PURPLE)
         .forceSolidOn()
         .noOcclusion()
         .sound(SoundType.AMETHYST_CLUSTER)
         .strength(1.5F)
         .lightLevel(p_152632_ -> 5)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block LARGE_AMETHYST_BUD = register(
      "large_amethyst_bud",
      p_360398_ -> new AmethystClusterBlock(5.0F, 10.0F, p_360398_),
      BlockBehaviour.Properties.ofLegacyCopy(AMETHYST_CLUSTER).sound(SoundType.MEDIUM_AMETHYST_BUD).lightLevel(p_152629_ -> 4)
   );
   public static final Block MEDIUM_AMETHYST_BUD = register(
      "medium_amethyst_bud",
      p_360055_ -> new AmethystClusterBlock(4.0F, 10.0F, p_360055_),
      BlockBehaviour.Properties.ofLegacyCopy(AMETHYST_CLUSTER).sound(SoundType.LARGE_AMETHYST_BUD).lightLevel(p_152617_ -> 2)
   );
   public static final Block SMALL_AMETHYST_BUD = register(
      "small_amethyst_bud",
      p_360389_ -> new AmethystClusterBlock(3.0F, 8.0F, p_360389_),
      BlockBehaviour.Properties.ofLegacyCopy(AMETHYST_CLUSTER).sound(SoundType.SMALL_AMETHYST_BUD).lightLevel(p_187409_ -> 1)
   );
   public static final Block TUFF = register(
      "tuff",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_GRAY)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .sound(SoundType.TUFF)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 6.0F)
   );
   public static final Block TUFF_SLAB = register("tuff_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF));
   public static final Block TUFF_STAIRS = register(
      "tuff_stairs", p_360336_ -> new StairBlock(TUFF.defaultBlockState(), p_360336_), BlockBehaviour.Properties.ofLegacyCopy(TUFF)
   );
   public static final Block TUFF_WALL = register("tuff_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF).forceSolidOn());
   public static final Block POLISHED_TUFF = register("polished_tuff", BlockBehaviour.Properties.ofLegacyCopy(TUFF).sound(SoundType.POLISHED_TUFF));
   public static final Block POLISHED_TUFF_SLAB = register("polished_tuff_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_TUFF));
   public static final Block POLISHED_TUFF_STAIRS = register(
      "polished_tuff_stairs", p_359995_ -> new StairBlock(POLISHED_TUFF.defaultBlockState(), p_359995_), BlockBehaviour.Properties.ofLegacyCopy(POLISHED_TUFF)
   );
   public static final Block POLISHED_TUFF_WALL = register(
      "polished_tuff_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_TUFF).forceSolidOn()
   );
   public static final Block CHISELED_TUFF = register("chiseled_tuff", BlockBehaviour.Properties.ofLegacyCopy(TUFF));
   public static final Block TUFF_BRICKS = register("tuff_bricks", BlockBehaviour.Properties.ofLegacyCopy(TUFF).sound(SoundType.TUFF_BRICKS));
   public static final Block TUFF_BRICK_SLAB = register("tuff_brick_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS));
   public static final Block TUFF_BRICK_STAIRS = register(
      "tuff_brick_stairs", p_360284_ -> new StairBlock(TUFF_BRICKS.defaultBlockState(), p_360284_), BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS)
   );
   public static final Block TUFF_BRICK_WALL = register("tuff_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS).forceSolidOn());
   public static final Block CHISELED_TUFF_BRICKS = register("chiseled_tuff_bricks", BlockBehaviour.Properties.ofLegacyCopy(TUFF_BRICKS));
   public static final Block CALCITE = register(
      "calcite",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_WHITE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .sound(SoundType.CALCITE)
         .requiresCorrectToolForDrops()
         .strength(0.75F)
   );
   public static final Block TINTED_GLASS = register(
      "tinted_glass",
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
      "powder_snow",
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
      "sculk_sensor",
      SculkSensorBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_CYAN)
         .strength(1.5F)
         .sound(SoundType.SCULK_SENSOR)
         .lightLevel(p_187406_ -> 1)
         .emissiveRendering((p_187412_, p_187413_, p_187414_) -> SculkSensorBlock.getPhase(p_187412_) == SculkSensorPhase.ACTIVE)
   );
   public static final Block CALIBRATED_SCULK_SENSOR = register(
      "calibrated_sculk_sensor", CalibratedSculkSensorBlock::new, BlockBehaviour.Properties.ofLegacyCopy(SCULK_SENSOR)
   );
   public static final Block SCULK = register(
      "sculk", SculkBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2F).sound(SoundType.SCULK)
   );
   public static final Block SCULK_VEIN = register(
      "sculk_vein",
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
      "sculk_catalyst",
      SculkCatalystBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 3.0F).sound(SoundType.SCULK_CATALYST).lightLevel(p_220873_ -> 6)
   );
   public static final Block SCULK_SHRIEKER = register(
      "sculk_shrieker",
      SculkShriekerBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 3.0F).sound(SoundType.SCULK_SHRIEKER)
   );
   public static final Block COPPER_BLOCK = register(
      "copper_block",
      p_360208_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_360208_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER)
   );
   public static final Block EXPOSED_COPPER = register(
      "exposed_copper",
      p_360017_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.EXPOSED, p_360017_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
   );
   public static final Block WEATHERED_COPPER = register(
      "weathered_copper",
      p_360301_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.WEATHERED, p_360301_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK).mapColor(MapColor.WARPED_STEM)
   );
   public static final Block OXIDIZED_COPPER = register(
      "oxidized_copper",
      p_360192_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.OXIDIZED, p_360192_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK).mapColor(MapColor.WARPED_NYLIUM)
   );
   public static final Block COPPER_ORE = register(
      "copper_ore", p_360224_ -> new DropExperienceBlock(ConstantInt.of(0), p_360224_), BlockBehaviour.Properties.ofLegacyCopy(IRON_ORE)
   );
   public static final Block DEEPSLATE_COPPER_ORE = register(
      "deepslate_copper_ore",
      p_360252_ -> new DropExperienceBlock(ConstantInt.of(0), p_360252_),
      BlockBehaviour.Properties.ofLegacyCopy(COPPER_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
   );
   public static final Block OXIDIZED_CUT_COPPER = register(
      "oxidized_cut_copper",
      p_359972_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.OXIDIZED, p_359972_),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER)
   );
   public static final Block WEATHERED_CUT_COPPER = register(
      "weathered_cut_copper",
      p_360250_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.WEATHERED, p_360250_),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER)
   );
   public static final Block EXPOSED_CUT_COPPER = register(
      "exposed_cut_copper",
      p_360239_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.EXPOSED, p_360239_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER)
   );
   public static final Block CUT_COPPER = register(
      "cut_copper",
      p_360281_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_360281_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK)
   );
   public static final Block OXIDIZED_CHISELED_COPPER = register(
      "oxidized_chiseled_copper",
      p_360234_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.OXIDIZED, p_360234_),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER)
   );
   public static final Block WEATHERED_CHISELED_COPPER = register(
      "weathered_chiseled_copper",
      p_360270_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.WEATHERED, p_360270_),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER)
   );
   public static final Block EXPOSED_CHISELED_COPPER = register(
      "exposed_chiseled_copper",
      p_360358_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.EXPOSED, p_360358_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER)
   );
   public static final Block CHISELED_COPPER = register(
      "chiseled_copper",
      p_360363_ -> new WeatheringCopperFullBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_360363_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK)
   );
   public static final Block WAXED_OXIDIZED_CHISELED_COPPER = register(
      "waxed_oxidized_chiseled_copper", BlockBehaviour.Properties.ofFullCopy(OXIDIZED_CHISELED_COPPER)
   );
   public static final Block WAXED_WEATHERED_CHISELED_COPPER = register(
      "waxed_weathered_chiseled_copper", BlockBehaviour.Properties.ofFullCopy(WEATHERED_CHISELED_COPPER)
   );
   public static final Block WAXED_EXPOSED_CHISELED_COPPER = register(
      "waxed_exposed_chiseled_copper", BlockBehaviour.Properties.ofFullCopy(EXPOSED_CHISELED_COPPER)
   );
   public static final Block WAXED_CHISELED_COPPER = register("waxed_chiseled_copper", BlockBehaviour.Properties.ofFullCopy(CHISELED_COPPER));
   public static final Block OXIDIZED_CUT_COPPER_STAIRS = register(
      "oxidized_cut_copper_stairs",
      p_360064_ -> new WeatheringCopperStairBlock(WeatheringCopper.WeatherState.OXIDIZED, OXIDIZED_CUT_COPPER.defaultBlockState(), p_360064_),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_CUT_COPPER)
   );
   public static final Block WEATHERED_CUT_COPPER_STAIRS = register(
      "weathered_cut_copper_stairs",
      p_360036_ -> new WeatheringCopperStairBlock(WeatheringCopper.WeatherState.WEATHERED, WEATHERED_CUT_COPPER.defaultBlockState(), p_360036_),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER)
   );
   public static final Block EXPOSED_CUT_COPPER_STAIRS = register(
      "exposed_cut_copper_stairs",
      p_360091_ -> new WeatheringCopperStairBlock(WeatheringCopper.WeatherState.EXPOSED, EXPOSED_CUT_COPPER.defaultBlockState(), p_360091_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER)
   );
   public static final Block CUT_COPPER_STAIRS = register(
      "cut_copper_stairs",
      p_360011_ -> new WeatheringCopperStairBlock(WeatheringCopper.WeatherState.UNAFFECTED, CUT_COPPER.defaultBlockState(), p_360011_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK)
   );
   public static final Block OXIDIZED_CUT_COPPER_SLAB = register(
      "oxidized_cut_copper_slab",
      p_360391_ -> new WeatheringCopperSlabBlock(WeatheringCopper.WeatherState.OXIDIZED, p_360391_),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_CUT_COPPER)
   );
   public static final Block WEATHERED_CUT_COPPER_SLAB = register(
      "weathered_cut_copper_slab",
      p_360025_ -> new WeatheringCopperSlabBlock(WeatheringCopper.WeatherState.WEATHERED, p_360025_),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_CUT_COPPER)
   );
   public static final Block EXPOSED_CUT_COPPER_SLAB = register(
      "exposed_cut_copper_slab",
      p_360090_ -> new WeatheringCopperSlabBlock(WeatheringCopper.WeatherState.EXPOSED, p_360090_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_CUT_COPPER)
   );
   public static final Block CUT_COPPER_SLAB = register(
      "cut_copper_slab",
      p_360113_ -> new WeatheringCopperSlabBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_360113_),
      BlockBehaviour.Properties.ofFullCopy(CUT_COPPER)
   );
   public static final Block WAXED_COPPER_BLOCK = register("waxed_copper_block", BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK));
   public static final Block WAXED_WEATHERED_COPPER = register("waxed_weathered_copper", BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER));
   public static final Block WAXED_EXPOSED_COPPER = register("waxed_exposed_copper", BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER));
   public static final Block WAXED_OXIDIZED_COPPER = register("waxed_oxidized_copper", BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER));
   public static final Block WAXED_OXIDIZED_CUT_COPPER = register("waxed_oxidized_cut_copper", BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER));
   public static final Block WAXED_WEATHERED_CUT_COPPER = register("waxed_weathered_cut_copper", BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER));
   public static final Block WAXED_EXPOSED_CUT_COPPER = register("waxed_exposed_cut_copper", BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER));
   public static final Block WAXED_CUT_COPPER = register("waxed_cut_copper", BlockBehaviour.Properties.ofFullCopy(COPPER_BLOCK));
   public static final Block WAXED_OXIDIZED_CUT_COPPER_STAIRS = registerStair("waxed_oxidized_cut_copper_stairs", WAXED_OXIDIZED_CUT_COPPER);
   public static final Block WAXED_WEATHERED_CUT_COPPER_STAIRS = registerStair("waxed_weathered_cut_copper_stairs", WAXED_WEATHERED_CUT_COPPER);
   public static final Block WAXED_EXPOSED_CUT_COPPER_STAIRS = registerStair("waxed_exposed_cut_copper_stairs", WAXED_EXPOSED_CUT_COPPER);
   public static final Block WAXED_CUT_COPPER_STAIRS = registerStair("waxed_cut_copper_stairs", WAXED_CUT_COPPER);
   public static final Block WAXED_OXIDIZED_CUT_COPPER_SLAB = register(
      "waxed_oxidized_cut_copper_slab", SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(WAXED_OXIDIZED_CUT_COPPER).requiresCorrectToolForDrops()
   );
   public static final Block WAXED_WEATHERED_CUT_COPPER_SLAB = register(
      "waxed_weathered_cut_copper_slab", SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(WAXED_WEATHERED_CUT_COPPER).requiresCorrectToolForDrops()
   );
   public static final Block WAXED_EXPOSED_CUT_COPPER_SLAB = register(
      "waxed_exposed_cut_copper_slab", SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(WAXED_EXPOSED_CUT_COPPER).requiresCorrectToolForDrops()
   );
   public static final Block WAXED_CUT_COPPER_SLAB = register(
      "waxed_cut_copper_slab", SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(WAXED_CUT_COPPER).requiresCorrectToolForDrops()
   );
   public static final Block COPPER_DOOR = register(
      "copper_door",
      p_360244_ -> new WeatheringCopperDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.UNAFFECTED, p_360244_),
      BlockBehaviour.Properties.of().mapColor(COPPER_BLOCK.defaultMapColor()).strength(3.0F, 6.0F).noOcclusion().pushReaction(PushReaction.DESTROY)
   );
   public static final Block EXPOSED_COPPER_DOOR = register(
      "exposed_copper_door",
      p_360223_ -> new WeatheringCopperDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.EXPOSED, p_360223_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_DOOR).mapColor(EXPOSED_COPPER.defaultMapColor())
   );
   public static final Block OXIDIZED_COPPER_DOOR = register(
      "oxidized_copper_door",
      p_360096_ -> new WeatheringCopperDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.OXIDIZED, p_360096_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_DOOR).mapColor(OXIDIZED_COPPER.defaultMapColor())
   );
   public static final Block WEATHERED_COPPER_DOOR = register(
      "weathered_copper_door",
      p_360255_ -> new WeatheringCopperDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.WEATHERED, p_360255_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_DOOR).mapColor(WEATHERED_COPPER.defaultMapColor())
   );
   public static final Block WAXED_COPPER_DOOR = register(
      "waxed_copper_door", p_360047_ -> new DoorBlock(BlockSetType.COPPER, p_360047_), BlockBehaviour.Properties.ofFullCopy(COPPER_DOOR)
   );
   public static final Block WAXED_EXPOSED_COPPER_DOOR = register(
      "waxed_exposed_copper_door", p_360395_ -> new DoorBlock(BlockSetType.COPPER, p_360395_), BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER_DOOR)
   );
   public static final Block WAXED_OXIDIZED_COPPER_DOOR = register(
      "waxed_oxidized_copper_door", p_360020_ -> new DoorBlock(BlockSetType.COPPER, p_360020_), BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER_DOOR)
   );
   public static final Block WAXED_WEATHERED_COPPER_DOOR = register(
      "waxed_weathered_copper_door", p_360328_ -> new DoorBlock(BlockSetType.COPPER, p_360328_), BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER_DOOR)
   );
   public static final Block COPPER_TRAPDOOR = register(
      "copper_trapdoor",
      p_360194_ -> new WeatheringCopperTrapDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.UNAFFECTED, p_360194_),
      BlockBehaviour.Properties.of()
         .mapColor(COPPER_BLOCK.defaultMapColor())
         .strength(3.0F, 6.0F)
         .requiresCorrectToolForDrops()
         .noOcclusion()
         .isValidSpawn(Blocks::never)
   );
   public static final Block EXPOSED_COPPER_TRAPDOOR = register(
      "exposed_copper_trapdoor",
      p_360227_ -> new WeatheringCopperTrapDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.EXPOSED, p_360227_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_TRAPDOOR).mapColor(EXPOSED_COPPER.defaultMapColor())
   );
   public static final Block OXIDIZED_COPPER_TRAPDOOR = register(
      "oxidized_copper_trapdoor",
      p_360345_ -> new WeatheringCopperTrapDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.OXIDIZED, p_360345_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_TRAPDOOR).mapColor(OXIDIZED_COPPER.defaultMapColor())
   );
   public static final Block WEATHERED_COPPER_TRAPDOOR = register(
      "weathered_copper_trapdoor",
      p_360186_ -> new WeatheringCopperTrapDoorBlock(BlockSetType.COPPER, WeatheringCopper.WeatherState.WEATHERED, p_360186_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_TRAPDOOR).mapColor(WEATHERED_COPPER.defaultMapColor())
   );
   public static final Block WAXED_COPPER_TRAPDOOR = register(
      "waxed_copper_trapdoor", p_360140_ -> new TrapDoorBlock(BlockSetType.COPPER, p_360140_), BlockBehaviour.Properties.ofFullCopy(COPPER_TRAPDOOR)
   );
   public static final Block WAXED_EXPOSED_COPPER_TRAPDOOR = register(
      "waxed_exposed_copper_trapdoor",
      p_360351_ -> new TrapDoorBlock(BlockSetType.COPPER, p_360351_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER_TRAPDOOR)
   );
   public static final Block WAXED_OXIDIZED_COPPER_TRAPDOOR = register(
      "waxed_oxidized_copper_trapdoor",
      p_360267_ -> new TrapDoorBlock(BlockSetType.COPPER, p_360267_),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER_TRAPDOOR)
   );
   public static final Block WAXED_WEATHERED_COPPER_TRAPDOOR = register(
      "waxed_weathered_copper_trapdoor",
      p_360195_ -> new TrapDoorBlock(BlockSetType.COPPER, p_360195_),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER_TRAPDOOR)
   );
   public static final Block COPPER_GRATE = register(
      "copper_grate",
      p_360052_ -> new WeatheringCopperGrateBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_360052_),
      BlockBehaviour.Properties.of()
         .strength(3.0F, 6.0F)
         .sound(SoundType.COPPER_GRATE)
         .mapColor(MapColor.COLOR_ORANGE)
         .noOcclusion()
         .requiresCorrectToolForDrops()
         .isValidSpawn(Blocks::never)
         .isRedstoneConductor(Blocks::never)
         .isSuffocating(Blocks::never)
         .isViewBlocking(Blocks::never)
   );
   public static final Block EXPOSED_COPPER_GRATE = register(
      "exposed_copper_grate",
      p_360116_ -> new WeatheringCopperGrateBlock(WeatheringCopper.WeatherState.EXPOSED, p_360116_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GRATE).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
   );
   public static final Block WEATHERED_COPPER_GRATE = register(
      "weathered_copper_grate",
      p_360282_ -> new WeatheringCopperGrateBlock(WeatheringCopper.WeatherState.WEATHERED, p_360282_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GRATE).mapColor(MapColor.WARPED_STEM)
   );
   public static final Block OXIDIZED_COPPER_GRATE = register(
      "oxidized_copper_grate",
      p_360089_ -> new WeatheringCopperGrateBlock(WeatheringCopper.WeatherState.OXIDIZED, p_360089_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GRATE).mapColor(MapColor.WARPED_NYLIUM)
   );
   public static final Block WAXED_COPPER_GRATE = register(
      "waxed_copper_grate", WaterloggedTransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(COPPER_GRATE)
   );
   public static final Block WAXED_EXPOSED_COPPER_GRATE = register(
      "waxed_exposed_copper_grate", WaterloggedTransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER_GRATE)
   );
   public static final Block WAXED_WEATHERED_COPPER_GRATE = register(
      "waxed_weathered_copper_grate", WaterloggedTransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER_GRATE)
   );
   public static final Block WAXED_OXIDIZED_COPPER_GRATE = register(
      "waxed_oxidized_copper_grate", WaterloggedTransparentBlock::new, BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER_GRATE)
   );
   public static final Block COPPER_BULB = register(
      "copper_bulb",
      p_360272_ -> new WeatheringCopperBulbBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_360272_),
      BlockBehaviour.Properties.of()
         .mapColor(COPPER_BLOCK.defaultMapColor())
         .strength(3.0F, 6.0F)
         .sound(SoundType.COPPER_BULB)
         .requiresCorrectToolForDrops()
         .isRedstoneConductor(Blocks::never)
         .lightLevel(litBlockEmission(15))
   );
   public static final Block EXPOSED_COPPER_BULB = register(
      "exposed_copper_bulb",
      p_360372_ -> new WeatheringCopperBulbBlock(WeatheringCopper.WeatherState.EXPOSED, p_360372_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BULB).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).lightLevel(litBlockEmission(12))
   );
   public static final Block WEATHERED_COPPER_BULB = register(
      "weathered_copper_bulb",
      p_360162_ -> new WeatheringCopperBulbBlock(WeatheringCopper.WeatherState.WEATHERED, p_360162_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BULB).mapColor(MapColor.WARPED_STEM).lightLevel(litBlockEmission(8))
   );
   public static final Block OXIDIZED_COPPER_BULB = register(
      "oxidized_copper_bulb",
      p_360320_ -> new WeatheringCopperBulbBlock(WeatheringCopper.WeatherState.OXIDIZED, p_360320_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_BULB).mapColor(MapColor.WARPED_NYLIUM).lightLevel(litBlockEmission(4))
   );
   public static final Block WAXED_COPPER_BULB = register("waxed_copper_bulb", CopperBulbBlock::new, BlockBehaviour.Properties.ofFullCopy(COPPER_BULB));
   public static final Block WAXED_EXPOSED_COPPER_BULB = register(
      "waxed_exposed_copper_bulb", CopperBulbBlock::new, BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER_BULB)
   );
   public static final Block WAXED_WEATHERED_COPPER_BULB = register(
      "waxed_weathered_copper_bulb", CopperBulbBlock::new, BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER_BULB)
   );
   public static final Block WAXED_OXIDIZED_COPPER_BULB = register(
      "waxed_oxidized_copper_bulb", CopperBulbBlock::new, BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER_BULB)
   );
   public static final Block COPPER_CHEST = register(
      "copper_chest",
      p_422065_ -> new WeatheringCopperChestBlock(
         WeatheringCopper.WeatherState.UNAFFECTED, SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, p_422065_
      ),
      BlockBehaviour.Properties.of().mapColor(COPPER_BLOCK.defaultMapColor()).strength(3.0F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops()
   );
   public static final Block EXPOSED_COPPER_CHEST = register(
      "exposed_copper_chest",
      p_422052_ -> new WeatheringCopperChestBlock(
         WeatheringCopper.WeatherState.EXPOSED, SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, p_422052_
      ),
      BlockBehaviour.Properties.ofFullCopy(COPPER_CHEST).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
   );
   public static final Block WEATHERED_COPPER_CHEST = register(
      "weathered_copper_chest",
      p_422046_ -> new WeatheringCopperChestBlock(
         WeatheringCopper.WeatherState.WEATHERED, SoundEvents.COPPER_CHEST_WEATHERED_OPEN, SoundEvents.COPPER_CHEST_WEATHERED_CLOSE, p_422046_
      ),
      BlockBehaviour.Properties.ofFullCopy(COPPER_CHEST).mapColor(MapColor.WARPED_STEM)
   );
   public static final Block OXIDIZED_COPPER_CHEST = register(
      "oxidized_copper_chest",
      p_422070_ -> new WeatheringCopperChestBlock(
         WeatheringCopper.WeatherState.OXIDIZED, SoundEvents.COPPER_CHEST_OXIDIZED_OPEN, SoundEvents.COPPER_CHEST_OXIDIZED_CLOSE, p_422070_
      ),
      BlockBehaviour.Properties.ofFullCopy(COPPER_CHEST).mapColor(MapColor.WARPED_NYLIUM)
   );
   public static final Block WAXED_COPPER_CHEST = register(
      "waxed_copper_chest",
      p_422057_ -> new CopperChestBlock(WeatheringCopper.WeatherState.UNAFFECTED, SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, p_422057_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_CHEST)
   );
   public static final Block WAXED_EXPOSED_COPPER_CHEST = register(
      "waxed_exposed_copper_chest",
      p_422074_ -> new CopperChestBlock(WeatheringCopper.WeatherState.EXPOSED, SoundEvents.COPPER_CHEST_OPEN, SoundEvents.COPPER_CHEST_CLOSE, p_422074_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER_CHEST)
   );
   public static final Block WAXED_WEATHERED_COPPER_CHEST = register(
      "waxed_weathered_copper_chest",
      p_422066_ -> new CopperChestBlock(
         WeatheringCopper.WeatherState.WEATHERED, SoundEvents.COPPER_CHEST_WEATHERED_OPEN, SoundEvents.COPPER_CHEST_WEATHERED_CLOSE, p_422066_
      ),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER_CHEST)
   );
   public static final Block WAXED_OXIDIZED_COPPER_CHEST = register(
      "waxed_oxidized_copper_chest",
      p_422062_ -> new CopperChestBlock(
         WeatheringCopper.WeatherState.OXIDIZED, SoundEvents.COPPER_CHEST_OXIDIZED_OPEN, SoundEvents.COPPER_CHEST_OXIDIZED_CLOSE, p_422062_
      ),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER_CHEST)
   );
   public static final Block COPPER_GOLEM_STATUE = register(
      "copper_golem_statue",
      p_422069_ -> new WeatheringCopperGolemStatueBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_422069_),
      BlockBehaviour.Properties.of()
         .mapColor(COPPER_BLOCK.defaultMapColor())
         .strength(3.0F, 6.0F)
         .sound(SoundType.COPPER_GOLEM_STATUE)
         .pushReaction(PushReaction.DESTROY)
         .noOcclusion()
   );
   public static final Block EXPOSED_COPPER_GOLEM_STATUE = register(
      "exposed_copper_golem_statue",
      p_422071_ -> new WeatheringCopperGolemStatueBlock(WeatheringCopper.WeatherState.EXPOSED, p_422071_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GOLEM_STATUE).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
   );
   public static final Block WEATHERED_COPPER_GOLEM_STATUE = register(
      "weathered_copper_golem_statue",
      p_422058_ -> new WeatheringCopperGolemStatueBlock(WeatheringCopper.WeatherState.WEATHERED, p_422058_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GOLEM_STATUE).mapColor(MapColor.WARPED_STEM)
   );
   public static final Block OXIDIZED_COPPER_GOLEM_STATUE = register(
      "oxidized_copper_golem_statue",
      p_422048_ -> new WeatheringCopperGolemStatueBlock(WeatheringCopper.WeatherState.OXIDIZED, p_422048_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GOLEM_STATUE).mapColor(MapColor.WARPED_NYLIUM)
   );
   public static final Block WAXED_COPPER_GOLEM_STATUE = register(
      "waxed_copper_golem_statue",
      p_422068_ -> new CopperGolemStatueBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_422068_),
      BlockBehaviour.Properties.ofFullCopy(COPPER_GOLEM_STATUE)
   );
   public static final Block WAXED_EXPOSED_COPPER_GOLEM_STATUE = register(
      "waxed_exposed_copper_golem_statue",
      p_422072_ -> new CopperGolemStatueBlock(WeatheringCopper.WeatherState.EXPOSED, p_422072_),
      BlockBehaviour.Properties.ofFullCopy(EXPOSED_COPPER_GOLEM_STATUE)
   );
   public static final Block WAXED_WEATHERED_COPPER_GOLEM_STATUE = register(
      "waxed_weathered_copper_golem_statue",
      p_422054_ -> new CopperGolemStatueBlock(WeatheringCopper.WeatherState.WEATHERED, p_422054_),
      BlockBehaviour.Properties.ofFullCopy(WEATHERED_COPPER_GOLEM_STATUE)
   );
   public static final Block WAXED_OXIDIZED_COPPER_GOLEM_STATUE = register(
      "waxed_oxidized_copper_golem_statue",
      p_422063_ -> new CopperGolemStatueBlock(WeatheringCopper.WeatherState.OXIDIZED, p_422063_),
      BlockBehaviour.Properties.ofFullCopy(OXIDIZED_COPPER_GOLEM_STATUE)
   );
   public static final Block LIGHTNING_ROD = register(
      "lightning_rod",
      p_422056_ -> new WeatheringLightningRodBlock(WeatheringCopper.WeatherState.UNAFFECTED, p_422056_),
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .forceSolidOn()
         .requiresCorrectToolForDrops()
         .strength(3.0F, 6.0F)
         .sound(SoundType.COPPER)
         .noOcclusion()
   );
   public static final Block EXPOSED_LIGHTNING_ROD = register(
      "exposed_lightning_rod",
      p_422067_ -> new WeatheringLightningRodBlock(WeatheringCopper.WeatherState.EXPOSED, p_422067_),
      BlockBehaviour.Properties.ofFullCopy(LIGHTNING_ROD).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
   );
   public static final Block WEATHERED_LIGHTNING_ROD = register(
      "weathered_lightning_rod",
      p_422049_ -> new WeatheringLightningRodBlock(WeatheringCopper.WeatherState.WEATHERED, p_422049_),
      BlockBehaviour.Properties.ofFullCopy(LIGHTNING_ROD).mapColor(MapColor.WARPED_STEM)
   );
   public static final Block OXIDIZED_LIGHTNING_ROD = register(
      "oxidized_lightning_rod",
      p_422047_ -> new WeatheringLightningRodBlock(WeatheringCopper.WeatherState.OXIDIZED, p_422047_),
      BlockBehaviour.Properties.ofFullCopy(LIGHTNING_ROD).mapColor(MapColor.WARPED_NYLIUM)
   );
   public static final Block WAXED_LIGHTNING_ROD = register("waxed_lightning_rod", LightningRodBlock::new, BlockBehaviour.Properties.ofFullCopy(LIGHTNING_ROD));
   public static final Block WAXED_EXPOSED_LIGHTNING_ROD = register(
      "waxed_exposed_lightning_rod", LightningRodBlock::new, BlockBehaviour.Properties.ofFullCopy(EXPOSED_LIGHTNING_ROD)
   );
   public static final Block WAXED_WEATHERED_LIGHTNING_ROD = register(
      "waxed_weathered_lightning_rod", LightningRodBlock::new, BlockBehaviour.Properties.ofFullCopy(WEATHERED_LIGHTNING_ROD)
   );
   public static final Block WAXED_OXIDIZED_LIGHTNING_ROD = register(
      "waxed_oxidized_lightning_rod", LightningRodBlock::new, BlockBehaviour.Properties.ofFullCopy(OXIDIZED_LIGHTNING_ROD)
   );
   public static final Block POINTED_DRIPSTONE = register(
      "pointed_dripstone",
      PointedDripstoneBlock::new,
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
   public static final Block DRIPSTONE_BLOCK = register(
      "dripstone_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.TERRACOTTA_BROWN)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .sound(SoundType.DRIPSTONE_BLOCK)
         .requiresCorrectToolForDrops()
         .strength(1.5F, 1.0F)
   );
   public static final Block CAVE_VINES = register(
      "cave_vines",
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
      "cave_vines_plant",
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
      "spore_blossom",
      SporeBlossomBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak().noCollision().sound(SoundType.SPORE_BLOSSOM).pushReaction(PushReaction.DESTROY)
   );
   public static final Block AZALEA = register(
      "azalea",
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
      "flowering_azalea",
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
      "moss_carpet",
      CarpetBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1F).sound(SoundType.MOSS_CARPET).pushReaction(PushReaction.DESTROY)
   );
   public static final Block PINK_PETALS = register(
      "pink_petals",
      FlowerBedBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().sound(SoundType.PINK_PETALS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block WILDFLOWERS = register(
      "wildflowers",
      FlowerBedBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().sound(SoundType.PINK_PETALS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block LEAF_LITTER = register(
      "leaf_litter",
      LeafLitterBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).replaceable().noCollision().sound(SoundType.LEAF_LITTER).pushReaction(PushReaction.DESTROY)
   );
   public static final Block MOSS_BLOCK = register(
      "moss_block",
      p_360241_ -> new BonemealableFeaturePlacerBlock(CaveFeatures.MOSS_PATCH_BONEMEAL, p_360241_),
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1F).sound(SoundType.MOSS).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BIG_DRIPLEAF = register(
      "big_dripleaf",
      BigDripleafBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).forceSolidOff().strength(0.1F).sound(SoundType.BIG_DRIPLEAF).pushReaction(PushReaction.DESTROY)
   );
   public static final Block BIG_DRIPLEAF_STEM = register(
      "big_dripleaf_stem",
      BigDripleafStemBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().strength(0.1F).sound(SoundType.BIG_DRIPLEAF).pushReaction(PushReaction.DESTROY)
   );
   public static final Block SMALL_DRIPLEAF = register(
      "small_dripleaf",
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
      "hanging_roots",
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
      "rooted_dirt", RootedDirtBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.ROOTED_DIRT)
   );
   public static final Block MUD = register(
      "mud",
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
      "deepslate",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.DEEPSLATE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(3.0F, 6.0F)
         .sound(SoundType.DEEPSLATE)
   );
   public static final Block COBBLED_DEEPSLATE = register("cobbled_deepslate", BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE).strength(3.5F, 6.0F));
   public static final Block COBBLED_DEEPSLATE_STAIRS = registerLegacyStair("cobbled_deepslate_stairs", COBBLED_DEEPSLATE);
   public static final Block COBBLED_DEEPSLATE_SLAB = register(
      "cobbled_deepslate_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE)
   );
   public static final Block COBBLED_DEEPSLATE_WALL = register(
      "cobbled_deepslate_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).forceSolidOn()
   );
   public static final Block POLISHED_DEEPSLATE = register(
      "polished_deepslate", BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.POLISHED_DEEPSLATE)
   );
   public static final Block POLISHED_DEEPSLATE_STAIRS = registerLegacyStair("polished_deepslate_stairs", POLISHED_DEEPSLATE);
   public static final Block POLISHED_DEEPSLATE_SLAB = register(
      "polished_deepslate_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_DEEPSLATE)
   );
   public static final Block POLISHED_DEEPSLATE_WALL = register(
      "polished_deepslate_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(POLISHED_DEEPSLATE).forceSolidOn()
   );
   public static final Block DEEPSLATE_TILES = register(
      "deepslate_tiles", BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.DEEPSLATE_TILES)
   );
   public static final Block DEEPSLATE_TILE_STAIRS = registerLegacyStair("deepslate_tile_stairs", DEEPSLATE_TILES);
   public static final Block DEEPSLATE_TILE_SLAB = register("deepslate_tile_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_TILES));
   public static final Block DEEPSLATE_TILE_WALL = register(
      "deepslate_tile_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_TILES).forceSolidOn()
   );
   public static final Block DEEPSLATE_BRICKS = register(
      "deepslate_bricks", BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.DEEPSLATE_BRICKS)
   );
   public static final Block DEEPSLATE_BRICK_STAIRS = registerLegacyStair("deepslate_brick_stairs", DEEPSLATE_BRICKS);
   public static final Block DEEPSLATE_BRICK_SLAB = register("deepslate_brick_slab", SlabBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_BRICKS));
   public static final Block DEEPSLATE_BRICK_WALL = register(
      "deepslate_brick_wall", WallBlock::new, BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_BRICKS).forceSolidOn()
   );
   public static final Block CHISELED_DEEPSLATE = register(
      "chiseled_deepslate", BlockBehaviour.Properties.ofLegacyCopy(COBBLED_DEEPSLATE).sound(SoundType.DEEPSLATE_BRICKS)
   );
   public static final Block CRACKED_DEEPSLATE_BRICKS = register("cracked_deepslate_bricks", BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_BRICKS));
   public static final Block CRACKED_DEEPSLATE_TILES = register("cracked_deepslate_tiles", BlockBehaviour.Properties.ofLegacyCopy(DEEPSLATE_TILES));
   public static final Block INFESTED_DEEPSLATE = register(
      "infested_deepslate",
      p_360133_ -> new InfestedRotatedPillarBlock(DEEPSLATE, p_360133_),
      BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).sound(SoundType.DEEPSLATE)
   );
   public static final Block SMOOTH_BASALT = register("smooth_basalt", BlockBehaviour.Properties.ofLegacyCopy(BASALT));
   public static final Block RAW_IRON_BLOCK = register(
      "raw_iron_block",
      BlockBehaviour.Properties.of().mapColor(MapColor.RAW_IRON).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
   );
   public static final Block RAW_COPPER_BLOCK = register(
      "raw_copper_block",
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.COLOR_ORANGE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .requiresCorrectToolForDrops()
         .strength(5.0F, 6.0F)
   );
   public static final Block RAW_GOLD_BLOCK = register(
      "raw_gold_block",
      BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
   );
   public static final Block POTTED_AZALEA = register("potted_azalea_bush", p_360386_ -> new FlowerPotBlock(AZALEA, p_360386_), flowerPotProperties());
   public static final Block POTTED_FLOWERING_AZALEA = register(
      "potted_flowering_azalea_bush", p_360315_ -> new FlowerPotBlock(FLOWERING_AZALEA, p_360315_), flowerPotProperties()
   );
   public static final Block OCHRE_FROGLIGHT = register(
      "ochre_froglight",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.3F).lightLevel(p_220871_ -> 15).sound(SoundType.FROGLIGHT)
   );
   public static final Block VERDANT_FROGLIGHT = register(
      "verdant_froglight",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.GLOW_LICHEN).strength(0.3F).lightLevel(p_220869_ -> 15).sound(SoundType.FROGLIGHT)
   );
   public static final Block PEARLESCENT_FROGLIGHT = register(
      "pearlescent_froglight",
      RotatedPillarBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3F).lightLevel(p_220867_ -> 15).sound(SoundType.FROGLIGHT)
   );
   public static final Block FROGSPAWN = register(
      "frogspawn",
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
      "reinforced_deepslate",
      BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.DEEPSLATE).strength(55.0F, 1200.0F)
   );
   public static final Block DECORATED_POT = register(
      "decorated_pot",
      DecoratedPotBlock::new,
      BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.0F, 0.0F).pushReaction(PushReaction.DESTROY).noOcclusion()
   );
   public static final Block CRAFTER = register("crafter", CrafterBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 3.5F));
   public static final Block TRIAL_SPAWNER = register(
      "trial_spawner",
      TrialSpawnerBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .lightLevel(p_309270_ -> p_309270_.getValue(TrialSpawnerBlock.STATE).lightLevel())
         .strength(50.0F)
         .sound(SoundType.TRIAL_SPAWNER)
         .isViewBlocking(Blocks::never)
         .noOcclusion()
   );
   public static final Block VAULT = register(
      "vault",
      VaultBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.STONE)
         .instrument(NoteBlockInstrument.BASEDRUM)
         .noOcclusion()
         .sound(SoundType.VAULT)
         .lightLevel(p_327249_ -> p_327249_.getValue(VaultBlock.STATE).lightLevel())
         .strength(50.0F)
         .isViewBlocking(Blocks::never)
   );
   public static final Block HEAVY_CORE = register(
      "heavy_core",
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
      "pale_moss_block",
      p_360325_ -> new BonemealableFeaturePlacerBlock(VegetationFeatures.PALE_MOSS_PATCH_BONEMEAL, p_360325_),
      BlockBehaviour.Properties.of()
         .ignitedByLava()
         .mapColor(MapColor.COLOR_LIGHT_GRAY)
         .strength(0.1F)
         .sound(SoundType.MOSS)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block PALE_MOSS_CARPET = register(
      "pale_moss_carpet",
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
      "pale_hanging_moss",
      HangingMossBlock::new,
      BlockBehaviour.Properties.of()
         .ignitedByLava()
         .mapColor(PALE_MOSS_BLOCK.defaultMapColor())
         .noCollision()
         .sound(SoundType.MOSS_CARPET)
         .pushReaction(PushReaction.DESTROY)
   );
   public static final Block OPEN_EYEBLOSSOM = register(
      "open_eyeblossom",
      p_375321_ -> new EyeblossomBlock(EyeblossomBlock.Type.OPEN, p_375321_),
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
      "closed_eyeblossom",
      p_375320_ -> new EyeblossomBlock(EyeblossomBlock.Type.CLOSED, p_375320_),
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
      "potted_open_eyeblossom", p_375327_ -> new FlowerPotBlock(OPEN_EYEBLOSSOM, p_375327_), flowerPotProperties().randomTicks()
   );
   public static final Block POTTED_CLOSED_EYEBLOSSOM = register(
      "potted_closed_eyeblossom", p_375325_ -> new FlowerPotBlock(CLOSED_EYEBLOSSOM, p_375325_), flowerPotProperties().randomTicks()
   );
   public static final Block FIREFLY_BUSH = register(
      "firefly_bush",
      FireflyBushBlock::new,
      BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .ignitedByLava()
         .lightLevel(p_390906_ -> 2)
         .noCollision()
         .instabreak()
         .sound(SoundType.SWEET_BERRY_BUSH)
         .pushReaction(PushReaction.DESTROY)
   );

   private static ToIntFunction<BlockState> litBlockEmission(int p_50760_) {
      return p_50763_ -> p_50763_.getValue(BlockStateProperties.LIT) ? p_50760_ : 0;
   }

   private static Function<BlockState, MapColor> waterloggedMapColor(MapColor p_332831_) {
      return p_327248_ -> p_327248_.getValue(BlockStateProperties.WATERLOGGED) ? MapColor.WATER : p_332831_;
   }

   private static Boolean never(BlockState p_50779_, BlockGetter p_50780_, BlockPos p_50781_, EntityType<?> p_50782_) {
      return false;
   }

   private static Boolean always(BlockState p_50810_, BlockGetter p_50811_, BlockPos p_50812_, EntityType<?> p_50813_) {
      return true;
   }

   private static Boolean ocelotOrParrot(BlockState p_50822_, BlockGetter p_50823_, BlockPos p_50824_, EntityType<?> p_50825_) {
      return p_50825_ == EntityType.OCELOT || p_50825_ == EntityType.PARROT;
   }

   private static Block registerBed(String p_362471_, DyeColor p_360977_) {
      return register(
         p_362471_,
         p_360339_ -> new BedBlock(p_360977_, p_360339_),
         BlockBehaviour.Properties.of()
            .mapColor(p_284863_ -> p_284863_.getValue(BedBlock.PART) == BedPart.FOOT ? p_360977_.getMapColor() : MapColor.WOOL)
            .sound(SoundType.WOOD)
            .strength(0.2F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
      );
   }

   private static BlockBehaviour.Properties logProperties(MapColor p_367100_, MapColor p_365916_, SoundType p_366098_) {
      return BlockBehaviour.Properties.of()
         .mapColor(p_152624_ -> p_152624_.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? p_367100_ : p_365916_)
         .instrument(NoteBlockInstrument.BASS)
         .strength(2.0F)
         .sound(p_366098_)
         .ignitedByLava();
   }

   private static BlockBehaviour.Properties netherStemProperties(MapColor p_369138_) {
      return BlockBehaviour.Properties.of().mapColor(p_152620_ -> p_369138_).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM);
   }

   private static boolean always(BlockState p_50775_, BlockGetter p_50776_, BlockPos p_50777_) {
      return true;
   }

   private static boolean never(BlockState p_50806_, BlockGetter p_50807_, BlockPos p_50808_) {
      return false;
   }

   private static Block registerStainedGlass(String p_367585_, DyeColor p_367654_) {
      return register(
         p_367585_,
         p_360218_ -> new StainedGlassBlock(p_367654_, p_360218_),
         BlockBehaviour.Properties.of()
            .mapColor(p_367654_)
            .instrument(NoteBlockInstrument.HAT)
            .strength(0.3F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(Blocks::never)
            .isSuffocating(Blocks::never)
            .isViewBlocking(Blocks::never)
      );
   }

   private static BlockBehaviour.Properties leavesProperties(SoundType p_361048_) {
      return BlockBehaviour.Properties.of()
         .mapColor(MapColor.PLANT)
         .strength(0.2F)
         .randomTicks()
         .sound(p_361048_)
         .noOcclusion()
         .isValidSpawn(Blocks::ocelotOrParrot)
         .isSuffocating(Blocks::never)
         .isViewBlocking(Blocks::never)
         .ignitedByLava()
         .pushReaction(PushReaction.DESTROY)
         .isRedstoneConductor(Blocks::never);
   }

   private static BlockBehaviour.Properties shulkerBoxProperties(MapColor p_365058_) {
      return BlockBehaviour.Properties.of()
         .mapColor(p_365058_)
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

   private static BlockBehaviour.Properties candleProperties(MapColor p_361309_) {
      return BlockBehaviour.Properties.of()
         .mapColor(p_361309_)
         .noOcclusion()
         .strength(0.1F)
         .sound(SoundType.CANDLE)
         .lightLevel(CandleBlock.LIGHT_EMISSION)
         .pushReaction(PushReaction.DESTROY);
   }

   @Deprecated
   private static Block registerLegacyStair(String p_361419_, Block p_365308_) {
      return register(p_361419_, p_360127_ -> new StairBlock(p_365308_.defaultBlockState(), p_360127_), BlockBehaviour.Properties.ofLegacyCopy(p_365308_));
   }

   private static Block registerStair(String p_362163_, Block p_367131_) {
      return register(p_362163_, p_360344_ -> new StairBlock(p_367131_.defaultBlockState(), p_360344_), BlockBehaviour.Properties.ofFullCopy(p_367131_));
   }

   private static BlockBehaviour.Properties wallVariant(Block p_364015_, boolean p_361131_) {
      BlockBehaviour.Properties blockbehaviour$properties = BlockBehaviour.Properties.of().overrideLootTable(p_364015_.getLootTable());
      if (p_361131_) {
         blockbehaviour$properties = blockbehaviour$properties.overrideDescription(p_364015_.getDescriptionId());
      }

      return blockbehaviour$properties;
   }

   private static Block register(ResourceKey<Block> p_309992_, Function<BlockBehaviour.Properties, Block> p_365267_, BlockBehaviour.Properties p_360839_) {
      Block block = p_365267_.apply(p_360839_.setId(p_309992_));
      return Registry.register(BuiltInRegistries.BLOCK, p_309992_, block);
   }

   private static Block register(ResourceKey<Block> p_361493_, BlockBehaviour.Properties p_365827_) {
      return register(p_361493_, Block::new, p_365827_);
   }

   private static ResourceKey<Block> vanillaBlockId(String p_360731_) {
      return ResourceKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(p_360731_));
   }

   private static Block register(String p_362355_, Function<BlockBehaviour.Properties, Block> p_369791_, BlockBehaviour.Properties p_370078_) {
      return register(vanillaBlockId(p_362355_), p_369791_, p_370078_);
   }

   private static Block register(String p_50796_, BlockBehaviour.Properties p_362760_) {
      return register(p_50796_, Block::new, p_362760_);
   }

   static {
      for (Block block : BuiltInRegistries.BLOCK) {
         UnmodifiableIterator var2 = block.getStateDefinition().getPossibleStates().iterator();

         while (var2.hasNext()) {
            BlockState blockstate = (BlockState)var2.next();
            Block.BLOCK_STATE_REGISTRY.add(blockstate);
            blockstate.initCache();
         }
      }
   }
}
