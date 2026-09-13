package net.minecraft.client.particle;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public abstract class SingleQuadParticle extends Particle {
   protected float quadSize;
   protected float rCol = 1.0F;
   protected float gCol = 1.0F;
   protected float bCol = 1.0F;
   protected float alpha = 1.0F;
   protected float roll;
   protected float oRoll;
   protected TextureAtlasSprite sprite;

   protected SingleQuadParticle(final ClientLevel level, final double x, final double y, final double z, final TextureAtlasSprite sprite) {
      super(level, x, y, z);
      this.sprite = sprite;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   }

   protected SingleQuadParticle(
      final ClientLevel level,
      final double x,
      final double y,
      final double z,
      final double xa,
      final double ya,
      final double za,
      final TextureAtlasSprite sprite
   ) {
      super(level, x, y, z, xa, ya, za);
      this.sprite = sprite;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
   }

   public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
      return SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ;
   }

   public void extract(final QuadParticleRenderState particleTypeRenderState, final Camera camera, final float partialTickTime) {
      Quaternionf rotation = new Quaternionf();
      this.getFacingCameraMode().setRotation(rotation, camera, partialTickTime);
      if (this.roll != 0.0F) {
         rotation.rotateZ(Mth.lerp(partialTickTime, this.oRoll, this.roll));
      }

      this.extractRotatedQuad(particleTypeRenderState, camera, rotation, partialTickTime);
   }

   protected void extractRotatedQuad(
      final QuadParticleRenderState particleTypeRenderState, final Camera camera, final Quaternionf rotation, final float partialTickTime
   ) {
      Vec3 pos = camera.position();
      float x = (float)(Mth.lerp(partialTickTime, this.xo, this.x) - pos.x());
      float y = (float)(Mth.lerp(partialTickTime, this.yo, this.y) - pos.y());
      float z = (float)(Mth.lerp(partialTickTime, this.zo, this.z) - pos.z());
      this.extractRotatedQuad(particleTypeRenderState, rotation, x, y, z, partialTickTime);
   }

   protected void extractRotatedQuad(
      final QuadParticleRenderState particleTypeRenderState,
      final Quaternionf rotation,
      final float x,
      final float y,
      final float z,
      final float partialTickTime
   ) {
      particleTypeRenderState.add(
         this.getLayer(),
         x,
         y,
         z,
         rotation.x,
         rotation.y,
         rotation.z,
         rotation.w,
         this.getQuadSize(partialTickTime),
         this.getU0(),
         this.getU1(),
         this.getV0(),
         this.getV1(),
         ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
         this.getLightCoords(partialTickTime)
      );
   }

   public float getQuadSize(final float a) {
      return this.quadSize;
   }

   @Override
   public Particle scale(final float scale) {
      this.quadSize *= scale;
      return super.scale(scale);
   }

   @Override
   public ParticleRenderType getGroup() {
      return ParticleRenderType.SINGLE_QUADS;
   }

   public void setSpriteFromAge(final SpriteSet sprites) {
      if (!this.removed) {
         this.setSprite(sprites.get(this.age, this.lifetime));
      }
   }

   protected void setSprite(final TextureAtlasSprite icon) {
      this.sprite = icon;
   }

   protected float getU0() {
      return this.sprite.getU0();
   }

   protected float getU1() {
      return this.sprite.getU1();
   }

   protected float getV0() {
      return this.sprite.getV0();
   }

   protected float getV1() {
      return this.sprite.getV1();
   }

   protected abstract SingleQuadParticle.Layer getLayer();

   public void setColor(final float r, final float g, final float b) {
      this.rCol = r;
      this.gCol = g;
      this.bCol = b;
   }

   protected void setAlpha(final float alpha) {
      this.alpha = alpha;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName()
         + ", Pos ("
         + this.x
         + ","
         + this.y
         + ","
         + this.z
         + "), RGBA ("
         + this.rCol
         + ","
         + this.gCol
         + ","
         + this.bCol
         + ","
         + this.alpha
         + "), Age "
         + this.age;
   }

   public interface FacingCameraMode {
      SingleQuadParticle.FacingCameraMode LOOKAT_XYZ = (target, camera, partialTickTime) -> target.set(camera.rotation());
      SingleQuadParticle.FacingCameraMode LOOKAT_Y = (target, camera, partialTickTime) -> target.set(0.0F, camera.rotation().y, 0.0F, camera.rotation().w);

      void setRotation(final Quaternionf target, final Camera camera, final float partialTickTime);
   }

   public record Layer(boolean translucent, Identifier textureAtlasLocation, RenderPipeline pipeline, @Nullable OitPipelineSet oitPipelineSet) {
      public static final SingleQuadParticle.Layer OPAQUE_TERRAIN = new SingleQuadParticle.Layer(
         false, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.OPAQUE_PARTICLE
      );
      public static final SingleQuadParticle.Layer TRANSLUCENT_TERRAIN = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_BLOCKS, RenderPipelines.TRANSLUCENT_PARTICLE, RenderPipelines.OIT_PARTICLE
      );
      public static final SingleQuadParticle.Layer OPAQUE_ITEMS = new SingleQuadParticle.Layer(
         false, TextureAtlas.LOCATION_ITEMS, RenderPipelines.OPAQUE_PARTICLE
      );
      public static final SingleQuadParticle.Layer TRANSLUCENT_ITEMS = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_ITEMS, RenderPipelines.TRANSLUCENT_PARTICLE, RenderPipelines.OIT_PARTICLE
      );
      public static final SingleQuadParticle.Layer OPAQUE = new SingleQuadParticle.Layer(
         false, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.OPAQUE_PARTICLE
      );
      public static final SingleQuadParticle.Layer TRANSLUCENT = new SingleQuadParticle.Layer(
         true, TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE, RenderPipelines.OIT_PARTICLE
      );

      public Layer(final boolean translucent, final Identifier textureAtlasLocation, final RenderPipeline pipeline) {
         this(translucent, textureAtlasLocation, pipeline, null);
      }

      public static SingleQuadParticle.Layer bySprite(final TextureAtlasSprite sprite) {
         boolean translucent = sprite.transparency().hasTranslucent();
         if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            return translucent ? TRANSLUCENT_TERRAIN : OPAQUE_TERRAIN;
         } else if (sprite.atlasLocation().equals(TextureAtlas.LOCATION_ITEMS)) {
            return translucent ? TRANSLUCENT_ITEMS : OPAQUE_ITEMS;
         } else {
            return translucent ? TRANSLUCENT : OPAQUE;
         }
      }
   }
}
