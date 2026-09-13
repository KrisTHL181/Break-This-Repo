package net.minecraft.client.gui.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GuiRenderer implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float MAX_GUI_Z = 10000.0F;
   public static final float MIN_GUI_Z = 0.0F;
   private static final float GUI_Z_NEAR = 1000.0F;
   public static final int GUI_3D_Z_FAR = 1000;
   public static final int GUI_3D_Z_NEAR = -1000;
   public static final int DEFAULT_ITEM_SIZE = 16;
   public static final Vector4fc CLEAR_COLOR = new Vector4f(0.0F);
   private static final Comparator<ScreenRectangle> SCISSOR_COMPARATOR = Comparator.nullsFirst(
      Comparator.comparing(ScreenRectangle::top)
         .thenComparing(ScreenRectangle::bottom)
         .thenComparing(ScreenRectangle::left)
         .thenComparing(ScreenRectangle::right)
   );
   private static final Comparator<TextureSetup> TEXTURE_COMPARATOR = Comparator.nullsFirst(Comparator.comparing(TextureSetup::getSortKey));
   private static final Comparator<GuiElementRenderState> ELEMENT_SORT_COMPARATOR = Comparator.comparing(GuiElementRenderState::scissorArea, SCISSOR_COMPARATOR)
      .thenComparing(GuiElementRenderState::pipeline, Comparator.comparing(RenderPipeline::getSortKey))
      .thenComparing(GuiElementRenderState::textureSetup, TEXTURE_COMPARATOR);
   private final Map<Object, OversizedItemRenderer> oversizedItemRenderers = new Object2ObjectOpenHashMap();
   private final GuiRenderState renderState;
   private final List<GuiRenderer.Draw> draws = new ArrayList<>();
   private final StagedVertexBuffer vertexBuffer = new StagedVertexBuffer(() -> "GUI Vertex Buffer", 786432);
   private int firstDrawIndexAfterBlur = Integer.MAX_VALUE;
   private final Projection guiProjection = new Projection();
   private final ProjectionMatrixBuffer guiProjectionMatrixBuffer = new ProjectionMatrixBuffer("gui");
   private final FeatureRenderDispatcher featureRenderDispatcher;
   private final Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;
   private @Nullable GuiItemAtlas itemAtlas;
   private int cachedGuiScale;
   private final CubeMap cubeMap = new CubeMap(Identifier.withDefaultNamespace("textures/gui/title/background/panorama"));
   private @Nullable ScreenRectangle previousScissorArea = null;
   private @Nullable RenderPipeline previousPipeline = null;
   private @Nullable TextureSetup previousTextureSetup = null;
   private StagedVertexBuffer.@Nullable Draw previousDraw;

   public GuiRenderer(
      final GuiRenderState renderState,
      final FeatureRenderDispatcher featureRenderDispatcher,
      final List<PictureInPictureRenderer<?>> pictureInPictureRenderers
   ) {
      this.renderState = renderState;
      this.featureRenderDispatcher = featureRenderDispatcher;
      Builder<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> builder = ImmutableMap.builder();

      for (PictureInPictureRenderer<?> pictureInPictureRenderer : pictureInPictureRenderers) {
         builder.put(pictureInPictureRenderer.getRenderStateClass(), pictureInPictureRenderer);
      }

      this.pictureInPictureRenderers = builder.buildOrThrow();
   }

   public void endFrame() {
      if (this.itemAtlas != null) {
         this.itemAtlas.endFrame();
      }
   }

   public void render() {
      ProfilerFiller profiler = Profiler.get();
      if (this.renderState.panoramaRenderState != null) {
         this.cubeMap.render(10.0F, this.renderState.panoramaRenderState.spin());
      }

      profiler.push("prepare");
      this.prepare();
      profiler.popPush("upload");
      this.vertexBuffer.upload();
      RenderSystem.resizeAllAutoStorageIndexBuffers();
      profiler.popPush("draw");
      this.draw();
      profiler.popPush("endFrame");
      this.vertexBuffer.endDraw();
      this.vertexBuffer.endFrame();
      this.draws.clear();
      this.renderState.reset();
      this.firstDrawIndexAfterBlur = Integer.MAX_VALUE;
      this.clearUnusedOversizedItemRenderers();
      if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
         RenderPipeline.updateSortKeySeed();
         TextureSetup.updateSortKeySeed();
      }

      profiler.pop();
   }

   private void clearUnusedOversizedItemRenderers() {
      Iterator<Entry<Object, OversizedItemRenderer>> oversizedItemRendererIterator = this.oversizedItemRenderers.entrySet().iterator();

      while (oversizedItemRendererIterator.hasNext()) {
         Entry<Object, OversizedItemRenderer> next = oversizedItemRendererIterator.next();
         OversizedItemRenderer renderer = next.getValue();
         if (!renderer.usedOnThisFrame()) {
            renderer.close();
            oversizedItemRendererIterator.remove();
         } else {
            renderer.resetUsedOnThisFrame();
         }
      }
   }

   private void prepare() {
      this.preparePictureInPicture();
      this.prepareItemElements();
      this.prepareText();
      this.renderState.sortElements(ELEMENT_SORT_COMPARATOR);
      this.addElementsToMeshes(GuiRenderState.TraverseRange.BEFORE_BLUR);
      this.firstDrawIndexAfterBlur = this.draws.size();
      this.addElementsToMeshes(GuiRenderState.TraverseRange.AFTER_BLUR);

      for (GuiRenderer.Draw draw : this.draws) {
         this.vertexBuffer.requestIndexCount(draw.draw);
      }
   }

   private void addElementsToMeshes(final GuiRenderState.TraverseRange range) {
      this.previousScissorArea = null;
      this.previousPipeline = null;
      this.previousTextureSetup = null;
      this.previousDraw = null;
      this.renderState.forEachElement(this::addElementToMesh, range);
   }

   private void draw() {
      if (!this.draws.isEmpty()) {
         Minecraft minecraft = Minecraft.getInstance();
         WindowRenderState windowState = minecraft.gameRenderer.gameRenderState().windowRenderState;
         this.guiProjection
            .setupOrtho(1000.0F, 11000.0F, (float)windowState.width / windowState.guiScale, (float)windowState.height / windowState.guiScale, true);
         RenderSystem.setProjectionMatrix(this.guiProjectionMatrixBuffer.getBuffer(this.guiProjection), ProjectionType.ORTHOGRAPHIC);
         RenderTarget mainRenderTarget = minecraft.gameRenderer.mainRenderTarget();
         GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F));
         if (this.firstDrawIndexAfterBlur > 0) {
            this.executeDrawRange(() -> "GUI before blur", mainRenderTarget, dynamicTransforms, 0, Math.min(this.firstDrawIndexAfterBlur, this.draws.size()));
         }

         if (this.draws.size() > this.firstDrawIndexAfterBlur) {
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainRenderTarget.getDepthTexture(), 0.0);
            minecraft.gameRenderer.processBlurEffect();
            this.executeDrawRange(() -> "GUI after blur", mainRenderTarget, dynamicTransforms, this.firstDrawIndexAfterBlur, this.draws.size());
         }
      }
   }

   private void executeDrawRange(
      final Supplier<String> label, final RenderTarget mainRenderTarget, final GpuBufferSlice dynamicTransforms, final int startIndex, final int endIndex
   ) {
      try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
               label,
               mainRenderTarget.getColorTextureView(),
               Optional.empty(),
               mainRenderTarget.hasDepth() ? mainRenderTarget.getDepthTextureView() : null,
               OptionalDouble.empty()
            )) {
         RenderSystem.bindDefaultUniforms(renderPass);
         renderPass.setUniform("DynamicTransforms", dynamicTransforms);

         for (int i = startIndex; i < endIndex; i++) {
            GuiRenderer.Draw draw = this.draws.get(i);
            this.executeDraw(draw, renderPass);
         }
      }
   }

   private void addElementToMesh(final GuiElementRenderState elementState) {
      RenderPipeline pipeline = elementState.pipeline();
      TextureSetup textureSetup = elementState.textureSetup();
      ScreenRectangle scissorArea = elementState.scissorArea();
      if (this.previousDraw == null
         || pipeline != this.previousPipeline
         || this.scissorChanged(scissorArea, this.previousScissorArea)
         || !textureSetup.equals(this.previousTextureSetup)) {
         this.previousPipeline = pipeline;
         this.previousTextureSetup = textureSetup;
         this.previousScissorArea = scissorArea;
         this.previousDraw = this.vertexBuffer.appendDraw(pipeline.getVertexFormatBinding(0), pipeline.getPrimitiveTopology());
         this.draws.add(new GuiRenderer.Draw(this.previousDraw, pipeline, textureSetup, scissorArea));
      }

      elementState.buildVertices(this.vertexBuffer.getVertexBuilder(Objects.requireNonNull(this.previousDraw)));
   }

   private void prepareText() {
      this.renderState.forEachText(text -> {
         final Matrix3x2fc pose = text.pose;
         final ScreenRectangle scissor = text.scissor;
         text.ensurePrepared().visit(new Font.GlyphVisitor() {
            @Override
            public void acceptRenderable(final TextRenderable renderable) {
               GuiRenderer.this.renderState.addGlyphToCurrentLayer(new GlyphRenderState(pose, renderable, scissor));
            }
         });
      });
   }

   private void prepareItemElements() {
      Set<Object> itemsInFrame = this.renderState.getItemModelIdentities();
      if (!itemsInFrame.isEmpty()) {
         int guiScale = this.getGuiScaleInvalidatingItemAtlasIfChanged();
         GuiItemAtlas itemAtlas = this.prepareItemAtlas(itemsInFrame, 16 * guiScale);
         MutableBoolean hasOversizedItems = new MutableBoolean(false);
         this.renderState.forEachItem(itemState -> {
            if (itemState.oversizedItemBounds() != null) {
               hasOversizedItems.setTrue();
            } else {
               GuiItemAtlas.SlotView slotView = itemAtlas.getOrUpdate(itemState.itemStackRenderState());
               if (slotView != null) {
                  this.submitBlitFromItemAtlas(itemState, slotView);
               }
            }
         });
         if (hasOversizedItems.booleanValue()) {
            this.renderState
               .forEachItem(
                  itemState -> {
                     if (itemState.oversizedItemBounds() != null) {
                        TrackingItemStackRenderState itemStackRenderState = itemState.itemStackRenderState();
                        OversizedItemRenderer oversizedItemRenderer = this.oversizedItemRenderers
                           .computeIfAbsent(itemStackRenderState.getModelIdentity(), var0 -> new OversizedItemRenderer());
                        ScreenRectangle actualItemBounds = itemState.oversizedItemBounds();
                        OversizedItemRenderState oversizedItemRenderState = new OversizedItemRenderState(
                           itemState, actualItemBounds.left(), actualItemBounds.top(), actualItemBounds.right(), actualItemBounds.bottom()
                        );
                        oversizedItemRenderer.prepare(oversizedItemRenderState, this.renderState, this.featureRenderDispatcher, guiScale);
                     }
                  }
               );
         }
      }
   }

   private void preparePictureInPicture() {
      int guiScale = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.guiScale;
      this.renderState.forEachPictureInPicture(pictureInPictureState -> this.preparePictureInPictureState(pictureInPictureState, guiScale));
   }

   private <T extends PictureInPictureRenderState> void preparePictureInPictureState(final T picturesInPictureState, final int guiScale) {
      PictureInPictureRenderer<T> renderer = (PictureInPictureRenderer<T>)this.pictureInPictureRenderers.get(picturesInPictureState.getClass());
      if (renderer != null) {
         renderer.prepare(picturesInPictureState, this.renderState, this.featureRenderDispatcher, guiScale);
      }
   }

   private void submitBlitFromItemAtlas(final GuiItemRenderState itemState, final GuiItemAtlas.SlotView slotView) {
      this.renderState
         .addBlitToCurrentLayer(
            new BlitRenderState(
               RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
               TextureSetup.singleTexture(slotView.textureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
               itemState.pose(),
               itemState.x(),
               itemState.y(),
               itemState.x() + 16,
               itemState.y() + 16,
               slotView.u0(),
               slotView.u1(),
               slotView.v0(),
               slotView.v1(),
               -1,
               itemState.scissorArea(),
               null
            )
         );
   }

   private GuiItemAtlas prepareItemAtlas(final Set<Object> itemsInFrame, final int slotTextureSize) {
      if (this.itemAtlas != null && this.itemAtlas.tryPrepareFor(itemsInFrame)) {
         return this.itemAtlas;
      }

      int newTextureSize = GuiItemAtlas.computeTextureSizeFor(slotTextureSize, itemsInFrame.size());
      if (this.itemAtlas != null && this.itemAtlas.textureSize() == newTextureSize) {
         LOGGER.warn(
            "Too many items ({}) in UI, some will be skipped! (Reached maximum texture size {}x{})",
            new Object[]{itemsInFrame.size(), newTextureSize, newTextureSize}
         );
         return this.itemAtlas;
      }

      if (this.itemAtlas != null) {
         this.itemAtlas.close();
      }

      this.itemAtlas = new GuiItemAtlas(this.featureRenderDispatcher, newTextureSize, slotTextureSize);
      return this.itemAtlas;
   }

   private int getGuiScaleInvalidatingItemAtlasIfChanged() {
      int guiScale = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.guiScale;
      if (guiScale != this.cachedGuiScale) {
         this.invalidateItemAtlas();

         for (OversizedItemRenderer renderer : this.oversizedItemRenderers.values()) {
            renderer.invalidateTexture();
         }

         this.cachedGuiScale = guiScale;
      }

      return guiScale;
   }

   private void invalidateItemAtlas() {
      if (this.itemAtlas != null) {
         this.itemAtlas.close();
         this.itemAtlas = null;
      }
   }

   private void executeDraw(final GuiRenderer.Draw draw, final RenderPass renderPass) {
      StagedVertexBuffer.ExecuteInfo executeInfo = this.vertexBuffer.getExecuteInfo(draw.draw);
      if (executeInfo != null) {
         RenderPipeline pipeline = draw.pipeline();
         renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
         renderPass.setVertexBuffer(0, executeInfo.vertexBuffer().slice());
         ScreenRectangle scissorArea = draw.scissorArea();
         if (scissorArea != null) {
            this.enableScissor(scissorArea, renderPass);
         } else {
            renderPass.disableScissor();
         }

         if (draw.textureSetup.texure0() != null) {
            renderPass.bindTexture("Sampler0", draw.textureSetup.texure0(), draw.textureSetup.sampler0());
         }

         if (draw.textureSetup.texure1() != null) {
            renderPass.bindTexture("Sampler1", draw.textureSetup.texure1(), draw.textureSetup.sampler1());
         }

         if (draw.textureSetup.texure2() != null) {
            renderPass.bindTexture("Sampler2", draw.textureSetup.texure2(), draw.textureSetup.sampler2());
         }

         renderPass.setIndexBuffer(executeInfo.indexBuffer(), executeInfo.indexType());
         renderPass.drawIndexed(executeInfo.indexCount(), 1, executeInfo.firstIndex(), executeInfo.baseVertex(), 0);
      }
   }

   private boolean scissorChanged(final @Nullable ScreenRectangle newScissor, final @Nullable ScreenRectangle oldScissor) {
      if (newScissor == oldScissor) {
         return false;
      } else {
         return newScissor != null ? !newScissor.equals(oldScissor) : true;
      }
   }

   private void enableScissor(final ScreenRectangle rectangle, final RenderPass renderPass) {
      WindowRenderState window = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
      int guiScale = window.guiScale;
      double left = rectangle.left() * guiScale;
      double top = rectangle.top() * guiScale;
      double right = Math.min(rectangle.right() * guiScale, window.width);
      double bottom = Math.min(rectangle.bottom() * guiScale, window.height);
      renderPass.enableScissor((int)left, window.height - (int)bottom, Math.max(0, (int)(right - left)), Math.max(0, (int)(bottom - top)));
   }

   public void registerPanoramaTextures(final TextureManager textureManager) {
      this.cubeMap.registerTextures(textureManager);
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.itemAtlas != null) {
         this.itemAtlas.close();
         this.itemAtlas = null;
      }

      this.pictureInPictureRenderers.values().forEach(PictureInPictureRenderer::close);
      this.guiProjectionMatrixBuffer.close();
      this.oversizedItemRenderers.values().forEach(PictureInPictureRenderer::close);
      this.cubeMap.close();
   }

   private record Draw(StagedVertexBuffer.Draw draw, RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea) {
   }
}
