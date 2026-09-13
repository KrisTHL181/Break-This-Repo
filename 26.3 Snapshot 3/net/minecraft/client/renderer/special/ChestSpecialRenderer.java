package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Vector3fc;

public class ChestSpecialRenderer implements NoDataSpecialModelRenderer {
   public static final Identifier ENDER_CHEST = Identifier.withDefaultNamespace("ender");
   public static final MultiblockChestResources<Identifier> REGULAR = createDefaultTextures("normal");
   public static final MultiblockChestResources<Identifier> TRAPPED = createDefaultTextures("trapped");
   public static final MultiblockChestResources<Identifier> CHRISTMAS = createDefaultTextures("christmas");
   public static final WeatheringCopperCollection.ByState<MultiblockChestResources<Identifier>> COPPER = new WeatheringCopperCollection.ByState<>(
      createDefaultTextures("copper"),
      createDefaultTextures("copper_exposed"),
      createDefaultTextures("copper_weathered"),
      createDefaultTextures("copper_oxidized")
   );
   private final SpriteGetter sprites;
   private final ChestModel model;
   private final SpriteId sprite;
   private final float openness;

   public ChestSpecialRenderer(final SpriteGetter sprites, final ChestModel model, final SpriteId sprite, final float openness) {
      this.sprites = sprites;
      this.model = model;
      this.sprite = sprite;
      this.openness = openness;
   }

   private static MultiblockChestResources<Identifier> createDefaultTextures(final String prefix) {
      return new MultiblockChestResources<>(
         Identifier.withDefaultNamespace(prefix), Identifier.withDefaultNamespace(prefix + "_left"), Identifier.withDefaultNamespace(prefix + "_right")
      );
   }

   @Override
   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final int overlayCoords,
      final boolean hasFoil,
      final int outlineColor
   ) {
      submitNodeCollector.submitModel(this.model, this.openness, poseStack, lightCoords, overlayCoords, -1, this.sprite, this.sprites, outlineColor);
   }

   @Override
   public void getExtents(final Consumer<Vector3fc> output) {
      PoseStack poseStack = new PoseStack();
      this.model.setupAnim(this.openness);
      this.model.root().getExtentsForGui(poseStack, output);
   }

   public record Unbaked(Identifier texture, float openness, ChestType chestType) implements NoDataSpecialModelRenderer.Unbaked {
      public static final MapCodec<ChestSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Identifier.CODEC.fieldOf("texture").forGetter(ChestSpecialRenderer.Unbaked::texture),
               Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ChestSpecialRenderer.Unbaked::openness),
               ChestType.CODEC.optionalFieldOf("chest_type", ChestType.SINGLE).forGetter(ChestSpecialRenderer.Unbaked::chestType)
            )
            .apply(i, ChestSpecialRenderer.Unbaked::new)
      );

      public Unbaked(final Identifier texture, final ChestType chestType) {
         this(texture, 0.0F, chestType);
      }

      public Unbaked(final Identifier texture) {
         this(texture, 0.0F, ChestType.SINGLE);
      }

      @Override
      public MapCodec<ChestSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      public ChestSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
         ChestModel model = new ChestModel(context.entityModelSet().bakeLayer(ChestRenderer.LAYERS.select(this.chestType)));
         SpriteId fullTexture = Sheets.CHEST_MAPPER.apply(this.texture);
         return new ChestSpecialRenderer(context.sprites(), model, fullTexture, this.openness);
      }
   }
}
