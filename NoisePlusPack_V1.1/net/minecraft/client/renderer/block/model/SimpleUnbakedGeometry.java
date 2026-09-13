package net.minecraft.client.renderer.block.model;

import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record SimpleUnbakedGeometry(List<BlockElement> elements) implements UnbakedGeometry {
   @Override
   public QuadCollection bake(TextureSlots p_397805_, ModelBaker p_395314_, ModelState p_394240_, ModelDebugName p_392934_) {
      return bake(this.elements, p_397805_, p_395314_, p_394240_, p_392934_);
   }

   public static QuadCollection bake(List<BlockElement> p_393173_, TextureSlots p_395401_, ModelBaker p_453836_, ModelState p_397074_, ModelDebugName p_393473_) {
      QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();

      for (BlockElement blockelement : p_393173_) {
         boolean flag = true;
         boolean flag1 = true;
         boolean flag2 = true;
         Vector3fc vector3fc = blockelement.from();
         Vector3fc vector3fc1 = blockelement.to();
         if (vector3fc.x() == vector3fc1.x()) {
            flag1 = false;
            flag2 = false;
         }

         if (vector3fc.y() == vector3fc1.y()) {
            flag = false;
            flag2 = false;
         }

         if (vector3fc.z() == vector3fc1.z()) {
            flag = false;
            flag1 = false;
         }

         if (flag || flag1 || flag2) {
            for (Entry<Direction, BlockElementFace> entry : blockelement.faces().entrySet()) {
               Direction direction = entry.getKey();
               BlockElementFace blockelementface = entry.getValue();

               boolean flag3 = switch (direction.getAxis()) {
                  case X -> flag;
                  case Y -> flag1;
                  case Z -> flag2;
               };
               if (flag3) {
                  TextureAtlasSprite textureatlassprite = p_453836_.sprites().resolveSlot(p_395401_, blockelementface.texture(), p_393473_);
                  BakedQuad bakedquad = FaceBakery.bakeQuad(
                     p_453836_.parts(),
                     vector3fc,
                     vector3fc1,
                     blockelementface,
                     textureatlassprite,
                     direction,
                     p_397074_,
                     blockelement.rotation(),
                     blockelement.shade(),
                     blockelement.lightEmission()
                  );
                  if (blockelementface.cullForDirection() == null) {
                     quadcollection$builder.addUnculledFace(bakedquad);
                  } else {
                     quadcollection$builder.addCulledFace(
                        Direction.rotate(p_397074_.transformation().getMatrix(), blockelementface.cullForDirection()), bakedquad
                     );
                  }
               }
            }
         }
      }

      return quadcollection$builder.build();
   }
}
