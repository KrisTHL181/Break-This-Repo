package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SpecialModelRenderer<T> {
   void submit(@Nullable T var1, ItemDisplayContext var2, PoseStack var3, SubmitNodeCollector var4, int var5, int var6, boolean var7, int var8);

   void getExtents(Consumer<Vector3fc> var1);

   @Nullable T extractArgument(ItemStack var1);

   @OnlyIn(Dist.CLIENT)
   interface BakingContext {
      EntityModelSet entityModelSet();

      MaterialSet materials();

      PlayerSkinRenderCache playerSkinRenderCache();

      @OnlyIn(Dist.CLIENT)
      record Simple(EntityModelSet entityModelSet, MaterialSet materials, PlayerSkinRenderCache playerSkinRenderCache)
         implements SpecialModelRenderer.BakingContext {
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface Unbaked {
      @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext var1);

      MapCodec<? extends SpecialModelRenderer.Unbaked> type();
   }
}
