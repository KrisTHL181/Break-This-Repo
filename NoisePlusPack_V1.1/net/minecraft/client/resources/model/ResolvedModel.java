package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ResolvedModel extends ModelDebugName {
   boolean DEFAULT_AMBIENT_OCCLUSION = true;
   UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

   UnbakedModel wrapped();

   @Nullable ResolvedModel parent();

   static TextureSlots findTopTextureSlots(ResolvedModel p_393648_) {
      ResolvedModel resolvedmodel = p_393648_;
      TextureSlots.Resolver textureslots$resolver = new TextureSlots.Resolver();

      while (resolvedmodel != null) {
         textureslots$resolver.addLast(resolvedmodel.wrapped().textureSlots());
         resolvedmodel = resolvedmodel.parent();
      }

      return textureslots$resolver.resolve(p_393648_);
   }

   default TextureSlots getTopTextureSlots() {
      return findTopTextureSlots(this);
   }

   static boolean findTopAmbientOcclusion(ResolvedModel p_393409_) {
      while (p_393409_ != null) {
         Boolean obool = p_393409_.wrapped().ambientOcclusion();
         if (obool != null) {
            return obool;
         }

         p_393409_ = p_393409_.parent();
      }

      return true;
   }

   default boolean getTopAmbientOcclusion() {
      return findTopAmbientOcclusion(this);
   }

   static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel p_392767_) {
      while (p_392767_ != null) {
         UnbakedModel.GuiLight unbakedmodel$guilight = p_392767_.wrapped().guiLight();
         if (unbakedmodel$guilight != null) {
            return unbakedmodel$guilight;
         }

         p_392767_ = p_392767_.parent();
      }

      return DEFAULT_GUI_LIGHT;
   }

   default UnbakedModel.GuiLight getTopGuiLight() {
      return findTopGuiLight(this);
   }

   static UnbakedGeometry findTopGeometry(ResolvedModel p_395357_) {
      while (p_395357_ != null) {
         UnbakedGeometry unbakedgeometry = p_395357_.wrapped().geometry();
         if (unbakedgeometry != null) {
            return unbakedgeometry;
         }

         p_395357_ = p_395357_.parent();
      }

      return UnbakedGeometry.EMPTY;
   }

   default UnbakedGeometry getTopGeometry() {
      return findTopGeometry(this);
   }

   default QuadCollection bakeTopGeometry(TextureSlots p_396041_, ModelBaker p_395367_, ModelState p_396505_) {
      return this.getTopGeometry().bake(p_396041_, p_395367_, p_396505_, this);
   }

   static TextureAtlasSprite resolveParticleSprite(TextureSlots p_391346_, ModelBaker p_396500_, ModelDebugName p_393309_) {
      return p_396500_.sprites().resolveSlot(p_391346_, "particle", p_393309_);
   }

   default TextureAtlasSprite resolveParticleSprite(TextureSlots p_397861_, ModelBaker p_395675_) {
      return resolveParticleSprite(p_397861_, p_395675_, this);
   }

   static ItemTransform findTopTransform(ResolvedModel p_393869_, ItemDisplayContext p_396067_) {
      while (p_393869_ != null) {
         ItemTransforms itemtransforms = p_393869_.wrapped().transforms();
         if (itemtransforms != null) {
            ItemTransform itemtransform = itemtransforms.getTransform(p_396067_);
            if (itemtransform != ItemTransform.NO_TRANSFORM) {
               return itemtransform;
            }
         }

         p_393869_ = p_393869_.parent();
      }

      return ItemTransform.NO_TRANSFORM;
   }

   static ItemTransforms findTopTransforms(ResolvedModel p_392780_) {
      ItemTransform itemtransform = findTopTransform(p_392780_, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      ItemTransform itemtransform1 = findTopTransform(p_392780_, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
      ItemTransform itemtransform2 = findTopTransform(p_392780_, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
      ItemTransform itemtransform3 = findTopTransform(p_392780_, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
      ItemTransform itemtransform4 = findTopTransform(p_392780_, ItemDisplayContext.HEAD);
      ItemTransform itemtransform5 = findTopTransform(p_392780_, ItemDisplayContext.GUI);
      ItemTransform itemtransform6 = findTopTransform(p_392780_, ItemDisplayContext.GROUND);
      ItemTransform itemtransform7 = findTopTransform(p_392780_, ItemDisplayContext.FIXED);
      ItemTransform itemtransform8 = findTopTransform(p_392780_, ItemDisplayContext.ON_SHELF);
      return new ItemTransforms(
         itemtransform, itemtransform1, itemtransform2, itemtransform3, itemtransform4, itemtransform5, itemtransform6, itemtransform7, itemtransform8
      );
   }

   default ItemTransforms getTopTransforms() {
      return findTopTransforms(this);
   }
}
