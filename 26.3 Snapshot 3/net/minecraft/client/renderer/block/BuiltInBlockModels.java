package net.minecraft.client.renderer.block;

import com.google.common.collect.UnmodifiableIterator;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.client.renderer.block.model.CompositeBlockModel;
import net.minecraft.client.renderer.block.model.ConditionalBlockModel;
import net.minecraft.client.renderer.block.model.EmptyBlockModel;
import net.minecraft.client.renderer.block.model.SpecialBlockModelWrapper;
import net.minecraft.client.renderer.block.model.properties.conditional.IsXmas;
import net.minecraft.client.renderer.block.model.properties.select.DisplayContext;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.CopperGolemStatueBlockRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.CopperGolemRenderer;
import net.minecraft.client.renderer.special.BannerSpecialRenderer;
import net.minecraft.client.renderer.special.BellSpecialRenderer;
import net.minecraft.client.renderer.special.BookSpecialRenderer;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.special.ConduitSpecialRenderer;
import net.minecraft.client.renderer.special.CopperGolemStatueSpecialRenderer;
import net.minecraft.client.renderer.special.DecoratedPotSpecialRenderer;
import net.minecraft.client.renderer.special.EndCubeSpecialRenderer;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.PlayerWallHeadBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector3f;

public class BuiltInBlockModels {
   private static void addDefaults(final BuiltInBlockModels.Builder builder) {
      createAir(builder, Blocks.AIR);
      createAir(builder, Blocks.CAVE_AIR);
      createAir(builder, Blocks.VOID_AIR);
      createMobHeads(builder, SkullBlock.Types.SKELETON, Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL);
      createMobHeads(builder, SkullBlock.Types.ZOMBIE, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD);
      createMobHeads(builder, SkullBlock.Types.CREEPER, Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD);
      createMobHeads(builder, SkullBlock.Types.DRAGON, Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD);
      createMobHeads(builder, SkullBlock.Types.PIGLIN, Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD);
      createMobHeads(builder, SkullBlock.Types.WITHER_SKELETON, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL);
      builder.put(createPlayerHead(), Blocks.PLAYER_HEAD);
      builder.put(createPlayerWallHead(), Blocks.PLAYER_WALL_HEAD);
      ColorCollection.zipApply(ColorCollection.VALUES, Blocks.BANNER, (color, banner) -> builder.put(createBanner(color), banner));
      ColorCollection.zipApply(ColorCollection.VALUES, Blocks.WALL_BANNER, (color, wallBanner) -> builder.put(createWallBanner(color), wallBanner));
      builder.put(createShulkerBox(), Blocks.SHULKER_BOX);
      ColorCollection.zipApply(ColorCollection.VALUES, Blocks.DYED_SHULKER_BOX, (color, box) -> builder.put(createDyedShulkerBox(color), box));
      builder.put(createSingletonChest(ChestSpecialRenderer.ENDER_CHEST), Blocks.ENDER_CHEST);
      builder.put(createXmasChest(ChestSpecialRenderer.REGULAR), Blocks.CHEST);
      builder.put(createXmasChest(ChestSpecialRenderer.TRAPPED), Blocks.TRAPPED_CHEST);
      WeatheringCopper.WeatherState.forEach(
         state -> {
            builder.put(
               createChest(ChestSpecialRenderer.COPPER.pick(state)), Blocks.COPPER_CHEST.weathering().pick(state), Blocks.COPPER_CHEST.waxed().pick(state)
            );
            builder.put(createCopperGolem(state), Blocks.COPPER_GOLEM_STATUE.weathering().pick(state), Blocks.COPPER_GOLEM_STATUE.waxed().pick(state));
         }
      );
      builder.put(special(new BellSpecialRenderer.Unbaked()), Blocks.BELL);
      builder.put(special(new ConduitSpecialRenderer.Unbaked(), ConduitRenderer.DEFAULT_TRANSFORMATION), Blocks.CONDUIT);
      builder.put(createDecoratedPot(), Blocks.DECORATED_POT);
      builder.put(createEnchantingTable(), Blocks.ENCHANTING_TABLE);
      builder.put(special(new EndCubeSpecialRenderer.Unbaked(EndCubeSpecialRenderer.Type.GATEWAY)), Blocks.END_GATEWAY);
      builder.put(special(new EndCubeSpecialRenderer.Unbaked(EndCubeSpecialRenderer.Type.PORTAL), TheEndPortalRenderer.TRANSFORMATION), Blocks.END_PORTAL);
      builder.put(BuiltInBlockModels::createFlowerBedModel, Blocks.WILDFLOWERS, Blocks.PINK_PETALS);
   }

   private static void createAir(final BuiltInBlockModels.Builder builder, final Block block) {
      builder.put(new EmptyBlockModel.Unbaked(), block);
   }

   private static BlockModel.Unbaked special(final SpecialModelRenderer.Unbaked<?> model) {
      return new SpecialBlockModelWrapper.Unbaked<>(model, Optional.empty());
   }

   private static BlockModel.Unbaked special(final SpecialModelRenderer.Unbaked<?> model, final Transformation transformation) {
      return new SpecialBlockModelWrapper.Unbaked<>(model, Optional.of(transformation));
   }

   private static BuiltInBlockModels.SpecialModelFactory createMobHead(final SkullBlock.Types type) {
      return specialModelWithPropertyDispatch(
         SkullBlock.ROTATION, rotation -> special(new SkullSpecialRenderer.Unbaked(type), SkullBlockRenderer.TRANSFORMATIONS.freeTransformations(rotation))
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createMobWallHead(final SkullBlock.Types type) {
      return specialModelWithPropertyDispatch(
         WallSkullBlock.FACING, facing -> special(new SkullSpecialRenderer.Unbaked(type), SkullBlockRenderer.TRANSFORMATIONS.wallTransformation(facing))
      );
   }

   private static void createMobHeads(final BuiltInBlockModels.Builder builder, final SkullBlock.Types type, final Block ground, final Block wall) {
      builder.put(createMobHead(type), ground);
      builder.put(createMobWallHead(type), wall);
   }

   private static BuiltInBlockModels.SpecialModelFactory createPlayerHead() {
      return specialModelWithPropertyDispatch(
         PlayerHeadBlock.ROTATION,
         rotation -> special(new PlayerHeadSpecialRenderer.Unbaked(), SkullBlockRenderer.TRANSFORMATIONS.freeTransformations(rotation))
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createPlayerWallHead() {
      return specialModelWithPropertyDispatch(
         PlayerWallHeadBlock.FACING, facing -> special(new PlayerHeadSpecialRenderer.Unbaked(), SkullBlockRenderer.TRANSFORMATIONS.wallTransformation(facing))
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createBanner(final DyeColor color) {
      return specialModelWithPropertyDispatch(
         BannerBlock.ROTATION,
         rotation -> special(
            new BannerSpecialRenderer.Unbaked(color, BannerBlock.AttachmentType.GROUND), BannerRenderer.TRANSFORMATIONS.freeTransformations(rotation)
         )
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createWallBanner(final DyeColor color) {
      return specialModelWithPropertyDispatch(
         WallBannerBlock.FACING,
         facing -> special(new BannerSpecialRenderer.Unbaked(color, BannerBlock.AttachmentType.WALL), BannerRenderer.TRANSFORMATIONS.wallTransformation(facing))
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createShulkerBox() {
      return specialModelWithPropertyDispatch(
         ShulkerBoxBlock.FACING, facing -> special(new ShulkerBoxSpecialRenderer.Unbaked(), ShulkerBoxRenderer.modelTransform(facing))
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createDyedShulkerBox(final DyeColor color) {
      return specialModelWithPropertyDispatch(
         ShulkerBoxBlock.FACING, facing -> special(new ShulkerBoxSpecialRenderer.Unbaked(color), ShulkerBoxRenderer.modelTransform(facing))
      );
   }

   private static BlockModel.Unbaked createChest(final Identifier texture, final ChestType chestType, final Direction facing) {
      return special(new ChestSpecialRenderer.Unbaked(texture, chestType), ChestRenderer.modelTransformation(facing));
   }

   private static BuiltInBlockModels.SpecialModelFactory createSingletonChest(final Identifier texture) {
      return specialModelWithPropertyDispatch(ChestBlock.FACING, facing -> createChest(texture, ChestType.SINGLE, facing));
   }

   private static BuiltInBlockModels.SpecialModelFactory createChest(final MultiblockChestResources<Identifier> textures) {
      return specialModelWithPropertyDispatch(ChestBlock.FACING, ChestBlock.TYPE, (facing, type) -> createChest(textures.select(type), type, facing));
   }

   private static BuiltInBlockModels.SpecialModelFactory createXmasChest(final MultiblockChestResources<Identifier> textures) {
      return specialModelWithPropertyDispatch(
         ChestBlock.FACING,
         ChestBlock.TYPE,
         (facing, type) -> new ConditionalBlockModel.Unbaked(
            Optional.empty(),
            new IsXmas(),
            createChest(ChestSpecialRenderer.CHRISTMAS.select(type), type, facing),
            createChest(textures.select(type), type, facing)
         )
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createCopperGolem(final WeatheringCopper.WeatherState weatherState) {
      return specialModelWithPropertyDispatch(
         CopperGolemStatueBlock.FACING,
         CopperGolemStatueBlock.POSE,
         (facing, pose) -> special(new CopperGolemStatueSpecialRenderer.Unbaked(weatherState, pose), CopperGolemStatueBlockRenderer.modelTransformation(facing))
      );
   }

   private static BuiltInBlockModels.SpecialModelFactory createDecoratedPot() {
      return specialModelWithPropertyDispatch(
         DecoratedPotBlock.HORIZONTAL_FACING, facing -> special(new DecoratedPotSpecialRenderer.Unbaked(), DecoratedPotRenderer.modelTransformation(facing))
      );
   }

   private static BlockStateModelWrapper.Unbaked createBlockStateModelWrapper(final BlockColors blockColors, final BlockState blockState) {
      return new BlockStateModelWrapper.Unbaked(blockState, blockColors.getTintSources(blockState), Optional.empty());
   }

   private static CompositeBlockModel.Unbaked combineSpecialAndBlockModels(
      final BlockModel.Unbaked specialModel, final BlockColors blockColors, final BlockState blockState
   ) {
      return new CompositeBlockModel.Unbaked(createBlockStateModelWrapper(blockColors, blockState), specialModel, Optional.empty());
   }

   private static SelectBlockModel.Unbaked createFlowerBedModel(final BlockColors blockColors, final BlockState blockState) {
      List<BlockTintSource> tintSources = blockColors.getTintSources(blockState);
      Transformation customFlowerTransform = new Transformation(new Vector3f(0.25F, 0.0F, 0.25F), null, null, null);
      BlockStateModelWrapper.Unbaked customTransformModel = new BlockStateModelWrapper.Unbaked(blockState, tintSources, Optional.of(customFlowerTransform));
      BlockStateModelWrapper.Unbaked normalTransformModel = new BlockStateModelWrapper.Unbaked(blockState, tintSources, Optional.empty());
      return new SelectBlockModel.Unbaked(
         Optional.empty(),
         new SelectBlockModel.UnbakedSwitch<>(
            new DisplayContext(), List.of(new SelectBlockModel.SwitchCase<>(List.of(CopperGolemRenderer.BLOCK_DISPLAY_CONTEXT), customTransformModel))
         ),
         Optional.of(normalTransformModel)
      );
   }

   private static BlockModel.Unbaked createEnchantingTable() {
      return special(
         new BookSpecialRenderer.Unbaked(0.0F, 0.0F, 0.0F),
         new Transformation(new Vector3f(0.5F, 0.8125F, 0.5F), Axis.ZP.rotationDegrees(180.0F), null, Axis.XP.rotationDegrees(90.0F))
      );
   }

   private static <P extends Comparable<P>> BuiltInBlockModels.SpecialModelFactory specialModelWithPropertyDispatch(
      final Property<P> property, final Function<P, BlockModel.Unbaked> blockModel
   ) {
      return state -> {
         P value = state.getValue(property);
         return blockModel.apply(value);
      };
   }

   private static <P1 extends Comparable<P1>, P2 extends Comparable<P2>> BuiltInBlockModels.SpecialModelFactory specialModelWithPropertyDispatch(
      final Property<P1> property1, final Property<P2> property2, final BiFunction<P1, P2, BlockModel.Unbaked> blockModel
   ) {
      return state -> {
         P1 value1 = state.getValue(property1);
         P2 value2 = state.getValue(property2);
         return blockModel.apply(value1, value2);
      };
   }

   public static Map<BlockState, BlockModel.Unbaked> createBlockModels(final BlockColors blockColors) {
      BuiltInBlockModels.Builder builder = new BuiltInBlockModels.Builder(blockColors);
      addDefaults(builder);
      return builder.build();
   }

   private static class Builder {
      private final BlockColors blockColors;
      private final Map<BlockState, BlockModel.Unbaked> result = new HashMap<>();

      private Builder(final BlockColors blockColors) {
         this.blockColors = blockColors;
      }

      private void put(final BuiltInBlockModels.ModelFactory factory, final Block a, final Block b) {
         this.put(factory, a);
         this.put(factory, b);
      }

      private void put(final BlockModel.Unbaked specialModel, final Block block) {
         this.put((BuiltInBlockModels.SpecialModelFactory)var1 -> specialModel, block);
      }

      private void put(final BuiltInBlockModels.ModelFactory factory, final Block block) {
         UnmodifiableIterator var3 = block.getStateDefinition().getPossibleStates().iterator();

         while (var3.hasNext()) {
            BlockState blockState = (BlockState)var3.next();
            this.result.put(blockState, factory.create(this.blockColors, blockState));
         }
      }

      public Map<BlockState, BlockModel.Unbaked> build() {
         return Map.copyOf(this.result);
      }
   }

   @FunctionalInterface
   private interface ModelFactory {
      BlockModel.Unbaked create(BlockColors colors, BlockState state);
   }

   @FunctionalInterface
   private interface SpecialModelFactory extends BuiltInBlockModels.ModelFactory {
      @Override
      default BlockModel.Unbaked create(final BlockColors colors, final BlockState state) {
         return BuiltInBlockModels.combineSpecialAndBlockModels(this.createSpecial(state), colors, state);
      }

      BlockModel.Unbaked createSpecial(BlockState state);
   }
}
