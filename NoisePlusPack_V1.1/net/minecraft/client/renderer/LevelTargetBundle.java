package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LevelTargetBundle implements PostChain.TargetBundle {
   public static final Identifier MAIN_TARGET_ID = PostChain.MAIN_TARGET_ID;
   public static final Identifier TRANSLUCENT_TARGET_ID = Identifier.withDefaultNamespace("translucent");
   public static final Identifier ITEM_ENTITY_TARGET_ID = Identifier.withDefaultNamespace("item_entity");
   public static final Identifier PARTICLES_TARGET_ID = Identifier.withDefaultNamespace("particles");
   public static final Identifier WEATHER_TARGET_ID = Identifier.withDefaultNamespace("weather");
   public static final Identifier CLOUDS_TARGET_ID = Identifier.withDefaultNamespace("clouds");
   public static final Identifier ENTITY_OUTLINE_TARGET_ID = Identifier.withDefaultNamespace("entity_outline");
   public static final Set<Identifier> MAIN_TARGETS = Set.of(MAIN_TARGET_ID);
   public static final Set<Identifier> OUTLINE_TARGETS = Set.of(MAIN_TARGET_ID, ENTITY_OUTLINE_TARGET_ID);
   public static final Set<Identifier> SORTING_TARGETS = Set.of(
      MAIN_TARGET_ID, TRANSLUCENT_TARGET_ID, ITEM_ENTITY_TARGET_ID, PARTICLES_TARGET_ID, WEATHER_TARGET_ID, CLOUDS_TARGET_ID
   );
   public ResourceHandle<RenderTarget> main = ResourceHandle.invalid();
   public @Nullable ResourceHandle<RenderTarget> translucent;
   public @Nullable ResourceHandle<RenderTarget> itemEntity;
   public @Nullable ResourceHandle<RenderTarget> particles;
   public @Nullable ResourceHandle<RenderTarget> weather;
   public @Nullable ResourceHandle<RenderTarget> clouds;
   public @Nullable ResourceHandle<RenderTarget> entityOutline;

   @Override
   public void replace(Identifier p_458753_, ResourceHandle<RenderTarget> p_364961_) {
      if (p_458753_.equals(MAIN_TARGET_ID)) {
         this.main = p_364961_;
      } else if (p_458753_.equals(TRANSLUCENT_TARGET_ID)) {
         this.translucent = p_364961_;
      } else if (p_458753_.equals(ITEM_ENTITY_TARGET_ID)) {
         this.itemEntity = p_364961_;
      } else if (p_458753_.equals(PARTICLES_TARGET_ID)) {
         this.particles = p_364961_;
      } else if (p_458753_.equals(WEATHER_TARGET_ID)) {
         this.weather = p_364961_;
      } else if (p_458753_.equals(CLOUDS_TARGET_ID)) {
         this.clouds = p_364961_;
      } else {
         if (!p_458753_.equals(ENTITY_OUTLINE_TARGET_ID)) {
            throw new IllegalArgumentException("No target with id " + p_458753_);
         }

         this.entityOutline = p_364961_;
      }
   }

   @Override
   public @Nullable ResourceHandle<RenderTarget> get(Identifier p_454329_) {
      if (p_454329_.equals(MAIN_TARGET_ID)) {
         return this.main;
      } else if (p_454329_.equals(TRANSLUCENT_TARGET_ID)) {
         return this.translucent;
      } else if (p_454329_.equals(ITEM_ENTITY_TARGET_ID)) {
         return this.itemEntity;
      } else if (p_454329_.equals(PARTICLES_TARGET_ID)) {
         return this.particles;
      } else if (p_454329_.equals(WEATHER_TARGET_ID)) {
         return this.weather;
      } else if (p_454329_.equals(CLOUDS_TARGET_ID)) {
         return this.clouds;
      } else {
         return p_454329_.equals(ENTITY_OUTLINE_TARGET_ID) ? this.entityOutline : null;
      }
   }

   public void clear() {
      this.main = ResourceHandle.invalid();
      this.translucent = null;
      this.itemEntity = null;
      this.particles = null;
      this.weather = null;
      this.clouds = null;
      this.entityOutline = null;
   }
}
