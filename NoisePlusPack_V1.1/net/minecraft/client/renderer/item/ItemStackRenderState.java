package net.minecraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemStackRenderState {
   ItemDisplayContext displayContext = ItemDisplayContext.NONE;
   private int activeLayerCount;
   private boolean animated;
   private boolean oversizedInGui;
   private @Nullable AABB cachedModelBoundingBox;
   private ItemStackRenderState.LayerRenderState[] layers = new ItemStackRenderState.LayerRenderState[]{new ItemStackRenderState.LayerRenderState()};

   public void ensureCapacity(int p_378622_) {
      int i = this.layers.length;
      int j = this.activeLayerCount + p_378622_;
      if (j > i) {
         this.layers = Arrays.copyOf(this.layers, j);

         for (int k = i; k < j; k++) {
            this.layers[k] = new ItemStackRenderState.LayerRenderState();
         }
      }
   }

   public ItemStackRenderState.LayerRenderState newLayer() {
      this.ensureCapacity(1);
      return this.layers[this.activeLayerCount++];
   }

   public void clear() {
      this.displayContext = ItemDisplayContext.NONE;

      for (int i = 0; i < this.activeLayerCount; i++) {
         this.layers[i].clear();
      }

      this.activeLayerCount = 0;
      this.animated = false;
      this.oversizedInGui = false;
      this.cachedModelBoundingBox = null;
   }

   public void setAnimated() {
      this.animated = true;
   }

   public boolean isAnimated() {
      return this.animated;
   }

   public void appendModelIdentityElement(Object p_405951_) {
   }

   private ItemStackRenderState.LayerRenderState firstLayer() {
      return this.layers[0];
   }

   public boolean isEmpty() {
      return this.activeLayerCount == 0;
   }

   public boolean usesBlockLight() {
      return this.firstLayer().usesBlockLight;
   }

   public @Nullable TextureAtlasSprite pickParticleIcon(RandomSource p_376964_) {
      return this.activeLayerCount == 0 ? null : this.layers[p_376964_.nextInt(this.activeLayerCount)].particleIcon;
   }

   public void visitExtents(Consumer<Vector3fc> p_395514_) {
      Vector3f vector3f = new Vector3f();
      PoseStack.Pose posestack$pose = new PoseStack.Pose();

      for (int i = 0; i < this.activeLayerCount; i++) {
         ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = this.layers[i];
         itemstackrenderstate$layerrenderstate.transform.apply(this.displayContext.leftHand(), posestack$pose);
         Matrix4f matrix4f = posestack$pose.pose();
         Vector3fc[] avector3fc = itemstackrenderstate$layerrenderstate.extents.get();

         for (Vector3fc vector3fc : avector3fc) {
            p_395514_.accept(vector3f.set(vector3fc).mulPosition(matrix4f));
         }

         posestack$pose.setIdentity();
      }
   }

   public void submit(PoseStack p_426657_, SubmitNodeCollector p_429942_, int p_425602_, int p_423887_, int p_423368_) {
      for (int i = 0; i < this.activeLayerCount; i++) {
         this.layers[i].submit(p_426657_, p_429942_, p_425602_, p_423887_, p_423368_);
      }
   }

   public AABB getModelBoundingBox() {
      if (this.cachedModelBoundingBox != null) {
         return this.cachedModelBoundingBox;
      }

      AABB.Builder aabb$builder = new AABB.Builder();
      this.visitExtents(aabb$builder::include);
      AABB aabb = aabb$builder.build();
      this.cachedModelBoundingBox = aabb;
      return aabb;
   }

   public void setOversizedInGui(boolean p_406641_) {
      this.oversizedInGui = p_406641_;
   }

   public boolean isOversizedInGui() {
      return this.oversizedInGui;
   }

   @OnlyIn(Dist.CLIENT)
   public enum FoilType {
      NONE,
      STANDARD,
      SPECIAL;
   }

   @OnlyIn(Dist.CLIENT)
   public class LayerRenderState {
      private static final Vector3fc[] NO_EXTENTS = new Vector3fc[0];
      public static final Supplier<Vector3fc[]> NO_EXTENTS_SUPPLIER = () -> NO_EXTENTS;
      private final List<BakedQuad> quads = new ArrayList<>();
      boolean usesBlockLight;
      @Nullable TextureAtlasSprite particleIcon;
      ItemTransform transform = ItemTransform.NO_TRANSFORM;
      private @Nullable RenderType renderType;
      private ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.NONE;
      private int[] tintLayers = new int[0];
      private @Nullable SpecialModelRenderer<Object> specialRenderer;
      private @Nullable Object argumentForSpecialRendering;
      Supplier<Vector3fc[]> extents = NO_EXTENTS_SUPPLIER;

      public void clear() {
         this.quads.clear();
         this.renderType = null;
         this.foilType = ItemStackRenderState.FoilType.NONE;
         this.specialRenderer = null;
         this.argumentForSpecialRendering = null;
         Arrays.fill(this.tintLayers, -1);
         this.usesBlockLight = false;
         this.particleIcon = null;
         this.transform = ItemTransform.NO_TRANSFORM;
         this.extents = NO_EXTENTS_SUPPLIER;
      }

      public List<BakedQuad> prepareQuadList() {
         return this.quads;
      }

      public void setRenderType(RenderType p_455025_) {
         this.renderType = p_455025_;
      }

      public void setUsesBlockLight(boolean p_395823_) {
         this.usesBlockLight = p_395823_;
      }

      public void setExtents(Supplier<Vector3fc[]> p_392781_) {
         this.extents = p_392781_;
      }

      public void setParticleIcon(TextureAtlasSprite p_392776_) {
         this.particleIcon = p_392776_;
      }

      public void setTransform(ItemTransform p_395712_) {
         this.transform = p_395712_;
      }

      public <T> void setupSpecialModel(SpecialModelRenderer<T> p_375891_, @Nullable T p_375474_) {
         this.specialRenderer = eraseSpecialRenderer(p_375891_);
         this.argumentForSpecialRendering = p_375474_;
      }

      private static SpecialModelRenderer<Object> eraseSpecialRenderer(SpecialModelRenderer<?> p_377056_) {
         return (SpecialModelRenderer<Object>)p_377056_;
      }

      public void setFoilType(ItemStackRenderState.FoilType p_377629_) {
         this.foilType = p_377629_;
      }

      public int[] prepareTintLayers(int p_375742_) {
         if (p_375742_ > this.tintLayers.length) {
            this.tintLayers = new int[p_375742_];
            Arrays.fill(this.tintLayers, -1);
         }

         return this.tintLayers;
      }

      void submit(PoseStack p_427684_, SubmitNodeCollector p_427380_, int p_425208_, int p_423311_, int p_425742_) {
         p_427684_.pushPose();
         this.transform.apply(ItemStackRenderState.this.displayContext.leftHand(), p_427684_.last());
         if (this.specialRenderer != null) {
            this.specialRenderer
               .submit(
                  this.argumentForSpecialRendering,
                  ItemStackRenderState.this.displayContext,
                  p_427684_,
                  p_427380_,
                  p_425208_,
                  p_423311_,
                  this.foilType != ItemStackRenderState.FoilType.NONE,
                  p_425742_
               );
         } else if (this.renderType != null) {
            p_427380_.submitItem(
               p_427684_,
               ItemStackRenderState.this.displayContext,
               p_425208_,
               p_423311_,
               p_425742_,
               this.tintLayers,
               this.quads,
               this.renderType,
               this.foilType
            );
         }

         p_427684_.popPose();
      }
   }
}
