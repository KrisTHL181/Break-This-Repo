package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class NameTagFeatureRenderer {
   public void render(SubmitNodeCollection p_423188_, MultiBufferSource.BufferSource p_424217_, Font p_425377_) {
      NameTagFeatureRenderer.Storage nametagfeaturerenderer$storage = p_423188_.getNameTagSubmits();
      nametagfeaturerenderer$storage.nameTagSubmitsSeethrough.sort(Comparator.comparing(SubmitNodeStorage.NameTagSubmit::distanceToCameraSq).reversed());

      for (SubmitNodeStorage.NameTagSubmit submitnodestorage$nametagsubmit : nametagfeaturerenderer$storage.nameTagSubmitsSeethrough) {
         p_425377_.drawInBatch(
            submitnodestorage$nametagsubmit.text(),
            submitnodestorage$nametagsubmit.x(),
            submitnodestorage$nametagsubmit.y(),
            submitnodestorage$nametagsubmit.color(),
            false,
            submitnodestorage$nametagsubmit.pose(),
            p_424217_,
            Font.DisplayMode.SEE_THROUGH,
            submitnodestorage$nametagsubmit.backgroundColor(),
            submitnodestorage$nametagsubmit.lightCoords()
         );
      }

      for (SubmitNodeStorage.NameTagSubmit submitnodestorage$nametagsubmit1 : nametagfeaturerenderer$storage.nameTagSubmitsNormal) {
         p_425377_.drawInBatch(
            submitnodestorage$nametagsubmit1.text(),
            submitnodestorage$nametagsubmit1.x(),
            submitnodestorage$nametagsubmit1.y(),
            submitnodestorage$nametagsubmit1.color(),
            false,
            submitnodestorage$nametagsubmit1.pose(),
            p_424217_,
            Font.DisplayMode.NORMAL,
            submitnodestorage$nametagsubmit1.backgroundColor(),
            submitnodestorage$nametagsubmit1.lightCoords()
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Storage {
      final List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsSeethrough = new ArrayList<>();
      final List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsNormal = new ArrayList<>();

      public void add(
         PoseStack p_427595_,
         @Nullable Vec3 p_424386_,
         int p_429503_,
         Component p_424960_,
         boolean p_429043_,
         int p_424634_,
         double p_427806_,
         CameraRenderState p_428843_
      ) {
         if (p_424386_ != null) {
            Minecraft minecraft = Minecraft.getInstance();
            p_427595_.pushPose();
            p_427595_.translate(p_424386_.x, p_424386_.y + 0.5, p_424386_.z);
            p_427595_.mulPose(p_428843_.orientation);
            p_427595_.scale(0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = new Matrix4f(p_427595_.last().pose());
            float f = -minecraft.font.width(p_424960_) / 2.0F;
            int i = (int)(minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            if (p_429043_) {
               this.nameTagSubmitsNormal
                  .add(
                     new SubmitNodeStorage.NameTagSubmit(
                        matrix4f, f, p_429503_, p_424960_, LightTexture.lightCoordsWithEmission(p_424634_, 2), -1, 0, p_427806_
                     )
                  );
               this.nameTagSubmitsSeethrough.add(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, p_429503_, p_424960_, p_424634_, -2130706433, i, p_427806_));
            } else {
               this.nameTagSubmitsNormal.add(new SubmitNodeStorage.NameTagSubmit(matrix4f, f, p_429503_, p_424960_, p_424634_, -2130706433, i, p_427806_));
            }

            p_427595_.popPose();
         }
      }

      public void clear() {
         this.nameTagSubmitsNormal.clear();
         this.nameTagSubmitsSeethrough.clear();
      }
   }
}
