package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

public interface ResolvedModel extends ModelDebugName {
   boolean DEFAULT_AMBIENT_OCCLUSION = true;
   UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

   UnbakedModel wrapped();

   @Nullable ResolvedModel parent();

   static TextureSlots findTopTextureSlots(final ResolvedModel top) {
      ResolvedModel current = top;
      TextureSlots.Resolver resolver = new TextureSlots.Resolver();

      while (current != null) {
         resolver.addLast(current.wrapped().textureSlots());
         current = current.parent();
      }

      return resolver.resolve(top);
   }

   default TextureSlots getTopTextureSlots() {
      return findTopTextureSlots(this);
   }

   static boolean findTopAmbientOcclusion(ResolvedModel current) {
      while (current != null) {
         Boolean hasAmbientOcclusion = current.wrapped().ambientOcclusion();
         if (hasAmbientOcclusion != null) {
            return hasAmbientOcclusion;
         }

         current = current.parent();
      }

      return true;
   }

   default boolean getTopAmbientOcclusion() {
      return findTopAmbientOcclusion(this);
   }

   static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel current) {
      while (current != null) {
         UnbakedModel.GuiLight guiLight = current.wrapped().guiLight();
         if (guiLight != null) {
            return guiLight;
         }

         current = current.parent();
      }

      return DEFAULT_GUI_LIGHT;
   }

   default UnbakedModel.GuiLight getTopGuiLight() {
      return findTopGuiLight(this);
   }

   static UnbakedGeometry findTopGeometry(ResolvedModel current) {
      while (current != null) {
         UnbakedGeometry geometry = current.wrapped().geometry();
         if (geometry != null) {
            return geometry;
         }

         current = current.parent();
      }

      return UnbakedGeometry.EMPTY;
   }

   default UnbakedGeometry getTopGeometry() {
      return findTopGeometry(this);
   }

   default QuadCollection bakeTopGeometry(final TextureSlots textureSlots, final ModelBaker baker, final ModelState state) {
      return this.getTopGeometry().bake(textureSlots, baker, state, this);
   }

   static Material.Baked resolveParticleMaterial(final TextureSlots textureSlots, final ModelBaker baker, final ModelDebugName resolvedModel) {
      return baker.materials().resolveSlot(textureSlots, "particle", resolvedModel);
   }

   default Material.Baked resolveParticleMaterial(final TextureSlots textureSlots, final ModelBaker baker) {
      return resolveParticleMaterial(textureSlots, baker, this);
   }

   static ItemTransform findTopTransform(ResolvedModel current, final ItemDisplayContext type) {
      while (current != null) {
         ItemTransforms transforms = current.wrapped().transforms();
         if (transforms != null) {
            ItemTransform transform = transforms.getTransform(type);
            if (transform != ItemTransform.NO_TRANSFORM) {
               return transform;
            }
         }

         current = current.parent();
      }

      return ItemTransform.NO_TRANSFORM;
   }

   static ItemTransforms findTopTransforms(final ResolvedModel top) {
      ItemTransform thirdPersonLeftHand = findTopTransform(top, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      ItemTransform thirdPersonRightHand = findTopTransform(top, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
      ItemTransform firstPersonLeftHand = findTopTransform(top, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
      ItemTransform firstPersonRightHand = findTopTransform(top, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
      ItemTransform head = findTopTransform(top, ItemDisplayContext.HEAD);
      ItemTransform gui = findTopTransform(top, ItemDisplayContext.GUI);
      ItemTransform ground = findTopTransform(top, ItemDisplayContext.GROUND);
      ItemTransform fixed = findTopTransform(top, ItemDisplayContext.FIXED);
      ItemTransform fixedFromBottom = findTopTransform(top, ItemDisplayContext.ON_SHELF);
      return new ItemTransforms(thirdPersonLeftHand, thirdPersonRightHand, firstPersonLeftHand, firstPersonRightHand, head, gui, ground, fixed, fixedFromBottom);
   }

   default ItemTransforms getTopTransforms() {
      return findTopTransforms(this);
   }
}
