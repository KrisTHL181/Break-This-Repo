package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerHeadSpecialRenderer implements SpecialModelRenderer<PlayerSkinRenderCache.RenderInfo> {
   private final PlayerSkinRenderCache playerSkinRenderCache;
   private final SkullModelBase modelBase;

   PlayerHeadSpecialRenderer(PlayerSkinRenderCache p_429139_, SkullModelBase p_459162_) {
      this.playerSkinRenderCache = p_429139_;
      this.modelBase = p_459162_;
   }

   public void submit(
      PlayerSkinRenderCache.@Nullable RenderInfo p_431143_,
      ItemDisplayContext p_426307_,
      PoseStack p_423357_,
      SubmitNodeCollector p_427840_,
      int p_425799_,
      int p_425391_,
      boolean p_427474_,
      int p_431901_
   ) {
      RenderType rendertype = p_431143_ != null ? p_431143_.renderType() : PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
      SkullBlockRenderer.submitSkull(null, 180.0F, 0.0F, p_423357_, p_427840_, p_425799_, this.modelBase, rendertype, p_431901_, null);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_450459_) {
      PoseStack posestack = new PoseStack();
      posestack.translate(0.5F, 0.0F, 0.5F);
      posestack.scale(-1.0F, -1.0F, 1.0F);
      this.modelBase.root().getExtentsForGui(posestack, p_450459_);
   }

   public PlayerSkinRenderCache.@Nullable RenderInfo extractArgument(ItemStack p_407162_) {
      ResolvableProfile resolvableprofile = p_407162_.get(DataComponents.PROFILE);
      return resolvableprofile == null ? null : this.playerSkinRenderCache.getOrDefault(resolvableprofile);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<PlayerHeadSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(PlayerHeadSpecialRenderer.Unbaked::new);

      @Override
      public MapCodec<PlayerHeadSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_426480_) {
         SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(p_426480_.entityModelSet(), SkullBlock.Types.PLAYER);
         return skullmodelbase == null ? null : new PlayerHeadSpecialRenderer(p_426480_.playerSkinRenderCache(), skullmodelbase);
      }
   }
}
