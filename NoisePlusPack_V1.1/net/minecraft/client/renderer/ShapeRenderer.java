package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShapeRenderer {
   public static void renderShape(
      PoseStack p_362127_, VertexConsumer p_362290_, VoxelShape p_362784_, double p_360742_, double p_360770_, double p_368227_, int p_362030_, float p_456560_
   ) {
      PoseStack.Pose posestack$pose = p_362127_.last();
      p_362784_.forAllEdges(
         (p_448199_, p_448200_, p_448201_, p_448202_, p_448203_, p_448204_) -> {
            Vector3f vector3f = new Vector3f((float)(p_448202_ - p_448199_), (float)(p_448203_ - p_448200_), (float)(p_448204_ - p_448201_)).normalize();
            p_362290_.addVertex(posestack$pose, (float)(p_448199_ + p_360742_), (float)(p_448200_ + p_360770_), (float)(p_448201_ + p_368227_))
               .setColor(p_362030_)
               .setNormal(posestack$pose, vector3f)
               .setLineWidth(p_456560_);
            p_362290_.addVertex(posestack$pose, (float)(p_448202_ + p_360742_), (float)(p_448203_ + p_360770_), (float)(p_448204_ + p_368227_))
               .setColor(p_362030_)
               .setNormal(posestack$pose, vector3f)
               .setLineWidth(p_456560_);
         }
      );
   }
}
