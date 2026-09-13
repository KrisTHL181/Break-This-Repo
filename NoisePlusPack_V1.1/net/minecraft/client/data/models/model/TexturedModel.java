package net.minecraft.client.data.models.model;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TexturedModel {
   public static final TexturedModel.Provider CUBE = createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL);
   public static final TexturedModel.Provider CUBE_INNER_FACES = createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL_INNER_FACES);
   public static final TexturedModel.Provider CUBE_MIRRORED = createDefault(TextureMapping::cube, ModelTemplates.CUBE_MIRRORED_ALL);
   public static final TexturedModel.Provider COLUMN = createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN);
   public static final TexturedModel.Provider COLUMN_HORIZONTAL = createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
   public static final TexturedModel.Provider CUBE_TOP_BOTTOM = createDefault(TextureMapping::cubeBottomTop, ModelTemplates.CUBE_BOTTOM_TOP);
   public static final TexturedModel.Provider CUBE_TOP = createDefault(TextureMapping::cubeTop, ModelTemplates.CUBE_TOP);
   public static final TexturedModel.Provider ORIENTABLE_ONLY_TOP = createDefault(TextureMapping::orientableCubeOnlyTop, ModelTemplates.CUBE_ORIENTABLE);
   public static final TexturedModel.Provider ORIENTABLE = createDefault(TextureMapping::orientableCube, ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM);
   public static final TexturedModel.Provider CARPET = createDefault(TextureMapping::wool, ModelTemplates.CARPET);
   public static final TexturedModel.Provider MOSSY_CARPET_SIDE = createDefault(TextureMapping::side, ModelTemplates.MOSSY_CARPET_SIDE);
   public static final TexturedModel.Provider FLOWERBED_1 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_1);
   public static final TexturedModel.Provider FLOWERBED_2 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_2);
   public static final TexturedModel.Provider FLOWERBED_3 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_3);
   public static final TexturedModel.Provider FLOWERBED_4 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_4);
   public static final TexturedModel.Provider LEAF_LITTER_1 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_1);
   public static final TexturedModel.Provider LEAF_LITTER_2 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_2);
   public static final TexturedModel.Provider LEAF_LITTER_3 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_3);
   public static final TexturedModel.Provider LEAF_LITTER_4 = createDefault(TextureMapping::defaultTexture, ModelTemplates.LEAF_LITTER_4);
   public static final TexturedModel.Provider GLAZED_TERRACOTTA = createDefault(TextureMapping::pattern, ModelTemplates.GLAZED_TERRACOTTA);
   public static final TexturedModel.Provider CORAL_FAN = createDefault(TextureMapping::fan, ModelTemplates.CORAL_FAN);
   public static final TexturedModel.Provider ANVIL = createDefault(TextureMapping::top, ModelTemplates.ANVIL);
   public static final TexturedModel.Provider LEAVES = createDefault(TextureMapping::cube, ModelTemplates.LEAVES);
   public static final TexturedModel.Provider LANTERN = createDefault(TextureMapping::lantern, ModelTemplates.LANTERN);
   public static final TexturedModel.Provider HANGING_LANTERN = createDefault(TextureMapping::lantern, ModelTemplates.HANGING_LANTERN);
   public static final TexturedModel.Provider CHAIN = createDefault(TextureMapping::defaultTexture, ModelTemplates.CHAIN);
   public static final TexturedModel.Provider SEAGRASS = createDefault(TextureMapping::defaultTexture, ModelTemplates.SEAGRASS);
   public static final TexturedModel.Provider COLUMN_ALT = createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN);
   public static final TexturedModel.Provider COLUMN_HORIZONTAL_ALT = createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
   public static final TexturedModel.Provider TOP_BOTTOM_WITH_WALL = createDefault(TextureMapping::cubeBottomTopWithWall, ModelTemplates.CUBE_BOTTOM_TOP);
   public static final TexturedModel.Provider COLUMN_WITH_WALL = createDefault(TextureMapping::columnWithWall, ModelTemplates.CUBE_COLUMN);
   private final TextureMapping mapping;
   private final ModelTemplate template;

   private TexturedModel(TextureMapping p_378768_, ModelTemplate p_377015_) {
      this.mapping = p_378768_;
      this.template = p_377015_;
   }

   public ModelTemplate getTemplate() {
      return this.template;
   }

   public TextureMapping getMapping() {
      return this.mapping;
   }

   public TexturedModel updateTextures(Consumer<TextureMapping> p_378734_) {
      p_378734_.accept(this.mapping);
      return this;
   }

   public Identifier create(Block p_377060_, BiConsumer<Identifier, ModelInstance> p_377616_) {
      return this.template.create(p_377060_, this.mapping, p_377616_);
   }

   public Identifier createWithSuffix(Block p_377474_, String p_378409_, BiConsumer<Identifier, ModelInstance> p_378475_) {
      return this.template.createWithSuffix(p_377474_, p_378409_, this.mapping, p_378475_);
   }

   private static TexturedModel.Provider createDefault(Function<Block, TextureMapping> p_378290_, ModelTemplate p_378044_) {
      return p_376579_ -> new TexturedModel(p_378290_.apply(p_376579_), p_378044_);
   }

   public static TexturedModel createAllSame(Identifier p_457641_) {
      return new TexturedModel(TextureMapping.cube(p_457641_), ModelTemplates.CUBE_ALL);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Provider {
      TexturedModel get(Block var1);

      default Identifier create(Block p_377562_, BiConsumer<Identifier, ModelInstance> p_377737_) {
         return this.get(p_377562_).create(p_377562_, p_377737_);
      }

      default Identifier createWithSuffix(Block p_378618_, String p_377692_, BiConsumer<Identifier, ModelInstance> p_378446_) {
         return this.get(p_378618_).createWithSuffix(p_378618_, p_377692_, p_378446_);
      }

      default TexturedModel.Provider updateTexture(Consumer<TextureMapping> p_377721_) {
         return p_378681_ -> this.get(p_378681_).updateTextures(p_377721_);
      }
   }
}
