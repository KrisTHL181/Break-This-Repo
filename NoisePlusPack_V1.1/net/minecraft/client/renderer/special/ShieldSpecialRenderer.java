package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShieldSpecialRenderer implements SpecialModelRenderer<DataComponentMap> {
   private final MaterialSet materials;
   private final ShieldModel model;

   public ShieldSpecialRenderer(MaterialSet p_424916_, ShieldModel p_460454_) {
      this.materials = p_424916_;
      this.model = p_460454_;
   }

   public @Nullable DataComponentMap extractArgument(ItemStack p_376303_) {
      return p_376303_.immutableComponents();
   }

   public void submit(
      @Nullable DataComponentMap p_422937_,
      ItemDisplayContext p_430896_,
      PoseStack p_426812_,
      SubmitNodeCollector p_429185_,
      int p_428957_,
      int p_428422_,
      boolean p_429283_,
      int p_431879_
   ) {
      BannerPatternLayers bannerpatternlayers = p_422937_ != null
         ? p_422937_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
         : BannerPatternLayers.EMPTY;
      DyeColor dyecolor = p_422937_ != null ? p_422937_.get(DataComponents.BASE_COLOR) : null;
      boolean flag = !bannerpatternlayers.layers().isEmpty() || dyecolor != null;
      p_426812_.pushPose();
      p_426812_.scale(1.0F, -1.0F, -1.0F);
      Material material = flag ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
      p_429185_.submitModelPart(
         this.model.handle(),
         p_426812_,
         this.model.renderType(material.atlasLocation()),
         p_428957_,
         p_428422_,
         this.materials.get(material),
         false,
         false,
         -1,
         null,
         p_431879_
      );
      if (flag) {
         BannerRenderer.submitPatterns(
            this.materials,
            p_426812_,
            p_429185_,
            p_428957_,
            p_428422_,
            this.model,
            Unit.INSTANCE,
            material,
            false,
            Objects.requireNonNullElse(dyecolor, DyeColor.WHITE),
            bannerpatternlayers,
            p_429283_,
            null,
            p_431879_
         );
      } else {
         p_429185_.submitModelPart(
            this.model.plate(),
            p_426812_,
            this.model.renderType(material.atlasLocation()),
            p_428957_,
            p_428422_,
            this.materials.get(material),
            false,
            p_429283_,
            -1,
            null,
            p_431879_
         );
      }

      p_426812_.popPose();
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_451958_) {
      PoseStack posestack = new PoseStack();
      posestack.scale(1.0F, -1.0F, -1.0F);
      this.model.root().getExtentsForGui(posestack, p_451958_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final ShieldSpecialRenderer.Unbaked INSTANCE = new ShieldSpecialRenderer.Unbaked();
      public static final MapCodec<ShieldSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

      @Override
      public MapCodec<ShieldSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_425130_) {
         return new ShieldSpecialRenderer(p_425130_.materials(), new ShieldModel(p_425130_.entityModelSet().bakeLayer(ModelLayers.SHIELD)));
      }
   }
}
