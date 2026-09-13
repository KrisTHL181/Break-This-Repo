package net.minecraft.client.renderer.feature;

import com.mojang.renderpearl.api.commands.RenderPass;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

public class FeatureRenderDispatcher implements AutoCloseable {
   private final ModelManager modelManager;
   private final AtlasManager atlasManager;
   private final Font font;
   private final GameRenderState gameRenderState;
   private final StagedVertexBuffer stagedVertexBuffer;
   private final FeatureRendererMap featureRenderers = new FeatureRendererMap();
   private final FeatureRenderDispatcher.PreparedFrame preparedFrame = new FeatureRenderDispatcher.PreparedFrame();

   public FeatureRenderDispatcher(
      final RenderBuffers renderBuffers,
      final ModelManager modelManager,
      final AtlasManager atlasManager,
      final Font font,
      final GameRenderState gameRenderState
   ) {
      this.modelManager = modelManager;
      this.atlasManager = atlasManager;
      this.font = font;
      this.gameRenderState = gameRenderState;
      this.stagedVertexBuffer = renderBuffers.stagedVertexBuffer();
      this.featureRenderers.put(ShadowFeatureRenderer.TYPE, new ShadowFeatureRenderer());
      this.featureRenderers.put(FlameFeatureRenderer.TYPE, new FlameFeatureRenderer());
      this.featureRenderers.put(ModelFeatureRenderer.TYPE, new ModelFeatureRenderer());
      this.featureRenderers.put(NameTagFeatureRenderer.TYPE, new NameTagFeatureRenderer());
      this.featureRenderers.put(TextFeatureRenderer.TYPE, new TextFeatureRenderer());
      this.featureRenderers.put(LeashFeatureRenderer.TYPE, new LeashFeatureRenderer());
      this.featureRenderers.put(ItemFeatureRenderer.TYPE, new ItemFeatureRenderer());
      this.featureRenderers.put(CustomFeatureRenderer.TYPE, new CustomFeatureRenderer());
      this.featureRenderers.put(BlockModelFeatureRenderer.TYPE, new BlockModelFeatureRenderer());
      this.featureRenderers.put(MovingBlockFeatureRenderer.TYPE, new MovingBlockFeatureRenderer());
      this.featureRenderers.put(QuadParticleFeatureRenderer.TYPE, new QuadParticleFeatureRenderer());
      this.featureRenderers.put(ShapeOutlineFeatureRenderer.TYPE, new ShapeOutlineFeatureRenderer());
      this.featureRenderers.put(GizmoFeatureRenderer.TYPE, new GizmoFeatureRenderer());
   }

   public FeatureRenderDispatcher.PreparedFrame prepareFrame(final SubmitNodeStorage submitNodeStorage) {
      Minecraft minecraft = Minecraft.getInstance();
      return this.prepareFrameWithContext(
         new FeatureFrameContext(
            this.gameRenderState.optionsRenderState,
            this.font,
            this.modelManager.getBlockStateModelSet(),
            minecraft.getBlockColors(),
            minecraft.getTextureManager(),
            this.atlasManager,
            minecraft.gameRenderer.lightmap(),
            this.stagedVertexBuffer
         ),
         submitNodeStorage
      );
   }

   private FeatureRenderDispatcher.PreparedFrame prepareFrameWithContext(final FeatureFrameContext context, final SubmitNodeStorage submitNodeStorage) {
      FeatureRenderDispatcher.PreparedFrame frame = this.preparedFrame.begin(context, submitNodeStorage);
      ProfilerFiller profiler = Profiler.get();
      profiler.push("sort");
      submitNodeStorage.drainPhases(phase -> phase.sortInto(new FeatureRenderDispatcher.PhaseSubmitGrouper(frame, phase)));
      profiler.popPush("beginPrepare");

      for (FeatureRenderer<?> renderer : this.featureRenderers.values()) {
         renderer.beginPrepare(context);
      }

      profiler.popPush("prepare");

      for (Entry<FeatureRendererType<?>, List<FeatureRenderDispatcher.PreparedGroup<?>>> entry : frame.groupsByFeature.entrySet()) {
         profiler.push(entry.getKey().toString());

         for (FeatureRenderDispatcher.PreparedGroup<?> group : entry.getValue()) {
            group.prepare(context, this.featureRenderers, frame.allSubmits);
         }

         profiler.pop();
      }

      profiler.popPush("finishPrepare");

      for (FeatureRenderer<?> renderer : this.featureRenderers.values()) {
         renderer.finishPrepare(context);
      }

      profiler.popPush("uploadSharedVertexBuffer");
      this.stagedVertexBuffer.upload();
      profiler.pop();
      return frame;
   }

   public static void renderAllFeatures(final RenderPass renderPass, final FeatureRenderDispatcher.PreparedFrame frame) {
      frame.executeSolid(renderPass);
      frame.executeTranslucent(renderPass);
      frame.executeTranslucentAfterTerrain(renderPass);
      frame.executeAlwaysOnTop(renderPass);
   }

   @Override
   public void close() {
      this.featureRenderers.close();
   }

   private static class PhaseSubmitGrouper implements FeatureRenderPhase.Output {
      private final FeatureRenderDispatcher.PreparedFrame frame;
      private final List<SubmitNode> allSubmits;
      private final List<FeatureRenderDispatcher.PreparedGroup<?>> phaseGroups;
      private FeatureRenderDispatcher.@Nullable PreparedGroup<?> lastGroup;

      public PhaseSubmitGrouper(final FeatureRenderDispatcher.PreparedFrame frame, final FeatureRenderPhase<?> phase) {
         this.frame = frame;
         this.allSubmits = frame.allSubmits;
         this.phaseGroups = frame.groupsByPhase.computeIfAbsent(phase, var0 -> new ArrayList<>());
      }

      @Override
      public void accept(final SubmitNode submit, final boolean strictlyOrdered) {
         int index = this.allSubmits.size();
         this.allSubmits.add(submit);
         this.addOrExtendGroup(submit.featureType(), strictlyOrdered, index, index);
      }

      @Override
      public <Submit extends SubmitNode> void acceptFeatureGroup(
         final FeatureRendererType<Submit> featureType, final Collection<Submit> submits, final boolean strictlyOrdered
      ) {
         if (!submits.isEmpty()) {
            for (Submit submit : submits) {
               if (submit.featureType() != featureType) {
                  throw new IllegalArgumentException(submit + " was not of feature type " + featureType);
               }
            }

            int fromInclusive = this.allSubmits.size();
            this.allSubmits.addAll(submits);
            int toInclusive = this.allSubmits.size() - 1;
            this.addOrExtendGroup(featureType, strictlyOrdered, fromInclusive, toInclusive);
         }
      }

      private <Submit extends SubmitNode> void addOrExtendGroup(
         final FeatureRendererType<Submit> featureType, final boolean strictlyOrdered, final int fromInclusive, final int toInclusive
      ) {
         if (this.lastGroup != null && this.lastGroup.featureType == featureType && this.lastGroup.strictlyOrdered == strictlyOrdered) {
            this.lastGroup.toInclusive = toInclusive;
         } else {
            List<FeatureRenderDispatcher.PreparedGroup<?>> featureGroups = this.frame.groupsByFeature.computeIfAbsent(featureType, var0 -> new ArrayList<>());
            FeatureRenderDispatcher.PreparedGroup<Submit> group = new FeatureRenderDispatcher.PreparedGroup<>(
               featureGroups.size(), featureType, strictlyOrdered, fromInclusive, toInclusive
            );
            this.phaseGroups.add(group);
            featureGroups.add(group);
            this.lastGroup = group;
         }
      }
   }

   public class PreparedFrame implements AutoCloseable {
      private @Nullable FeatureFrameContext context;
      private @Nullable SubmitNodeStorage submitNodeStorage;
      private final List<SubmitNode> allSubmits = new ArrayList<>();
      private final Map<FeatureRenderPhase<?>, List<FeatureRenderDispatcher.PreparedGroup<?>>> groupsByPhase = new IdentityHashMap<>();
      private final Map<FeatureRendererType<?>, List<FeatureRenderDispatcher.PreparedGroup<?>>> groupsByFeature = new IdentityHashMap<>();

      private FeatureRenderDispatcher.PreparedFrame begin(final FeatureFrameContext context, final SubmitNodeStorage submitNodeStorage) {
         if (this.context != null) {
            throw new IllegalStateException("PreparedFrame already in use");
         }

         this.context = context;
         this.submitNodeStorage = submitNodeStorage;
         return this;
      }

      public void executeSolid(final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.solid, context, renderPass);
         }
      }

      public void executeTranslucent(final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.shadows, context, renderPass);
            this.executePhase(collection.translucentModels, context, renderPass);
            this.executePhase(collection.seeThroughNameTags, context, renderPass);
            this.executePhase(collection.nameTags, context, renderPass);
            this.executePhase(collection.texts, context, renderPass);
            this.executePhase(collection.translucentCustomGeometry, context, renderPass);
         }

         var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.shapeOutlines, context, renderPass);
            this.executePhase(collection.translucentGizmos, context, renderPass);
         }

         var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.translucentBlocksAndItems, context, renderPass);
            this.executePhase(collection.breakingOverlay, context, renderPass);
            this.executePhase(collection.waterMask, context, renderPass);
         }
      }

      public void executeWaterMask(final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.waterMask, context, renderPass);
         }
      }

      public void executeOit(final OitStage stage, final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var5 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var5.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var5.next();
            this.executePhase(collection.oitTranslucent, context, stage, renderPass);
         }
      }

      public void executeOutline(final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.outline, context, renderPass);
         }
      }

      public void executeTranslucentAfterTerrain(final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.afterTerrain, context, renderPass);
         }
      }

      public void executeAlwaysOnTop(final RenderPass renderPass) {
         FeatureFrameContext context = Objects.requireNonNull(this.context);
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var4 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var4.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var4.next();
            this.executePhase(collection.alwaysOnTopGizmos, context, renderPass);
         }
      }

      private void executePhase(final FeatureRenderPhase<?> phase, final FeatureFrameContext context, final RenderPass renderPass) {
         this.executePhase(phase, context, null, renderPass);
      }

      private void executePhase(
         final FeatureRenderPhase<?> phase, final FeatureFrameContext context, final @Nullable OitStage stage, final RenderPass renderPass
      ) {
         ProfilerFiller profiler = Profiler.get();

         for (FeatureRenderDispatcher.PreparedGroup<?> group : this.groupsByPhase.getOrDefault(phase, List.of())) {
            String featureTypeName = group.featureType.toString();
            profiler.push(featureTypeName);
            renderPass.pushDebugGroup(() -> featureTypeName);
            group.execute(context, stage, renderPass, FeatureRenderDispatcher.this.featureRenderers, this.allSubmits);
            renderPass.popDebugGroup();
            profiler.pop();
         }
      }

      public boolean hasAnyAlwaysOnTop() {
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var2 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var2.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var2.next();
            if (!this.groupsByPhase.getOrDefault(collection.alwaysOnTopGizmos, List.of()).isEmpty()) {
               return true;
            }
         }

         return false;
      }

      public boolean hasAnyOutline() {
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var2 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var2.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var2.next();
            if (!this.groupsByPhase.getOrDefault(collection.outline, List.of()).isEmpty()) {
               return true;
            }
         }

         return false;
      }

      public boolean hasAnyWaterMask() {
         SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
         ObjectIterator var2 = submitNodeStorage.getSubmitsPerOrder().values().iterator();

         while (var2.hasNext()) {
            SubmitNodeCollection collection = (SubmitNodeCollection)var2.next();
            if (!this.groupsByPhase.getOrDefault(collection.waterMask, List.of()).isEmpty()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public void close() {
         FeatureFrameContext context = Objects.requireNonNull(this.context, "Frame not in use");
         this.context = null;
         this.submitNodeStorage = null;

         for (FeatureRenderer<?> featureRenderer : FeatureRenderDispatcher.this.featureRenderers.values()) {
            featureRenderer.finishExecute(context);
         }

         FeatureRenderDispatcher.this.stagedVertexBuffer.endDraw();
         this.allSubmits.clear();
         clearGroups(this.groupsByPhase.values());
         clearGroups(this.groupsByFeature.values());
      }

      private static void clearGroups(final Collection<List<FeatureRenderDispatcher.PreparedGroup<?>>> groupsSet) {
         groupsSet.removeIf(groups -> {
            if (groups.isEmpty()) {
               return true;
            }

            groups.clear();
            return false;
         });
      }
   }

   private static class PreparedGroup<Submit extends SubmitNode> {
      private final int featureGroupIndex;
      private final FeatureRendererType<Submit> featureType;
      private final boolean strictlyOrdered;
      private final int fromInclusive;
      private int toInclusive;

      public PreparedGroup(
         final int featureGroupIndex,
         final FeatureRendererType<Submit> featureType,
         final boolean strictlyOrdered,
         final int fromInclusive,
         final int toInclusive
      ) {
         this.featureGroupIndex = featureGroupIndex;
         this.featureType = featureType;
         this.strictlyOrdered = strictlyOrdered;
         this.fromInclusive = fromInclusive;
         this.toInclusive = toInclusive;
      }

      public void prepare(final FeatureFrameContext context, final FeatureRendererMap featureRenderers, final List<SubmitNode> submits) {
         FeatureRenderer<Submit> featureRenderer = featureRenderers.getOrThrow(this.featureType);
         featureRenderer.prepareGroup(context, this.sliceUnchecked(submits), this.strictlyOrdered);
      }

      public void execute(
         final FeatureFrameContext context,
         final @Nullable OitStage stage,
         final RenderPass renderPass,
         final FeatureRendererMap featureRenderers,
         final List<SubmitNode> submits
      ) {
         FeatureRenderer<Submit> featureRenderer = featureRenderers.getOrThrow(this.featureType);
         featureRenderer.executeGroup(context, stage, renderPass, this.featureGroupIndex, this.sliceUnchecked(submits), this.strictlyOrdered);
      }

      private List<Submit> sliceUnchecked(final List<SubmitNode> submits) {
         return (List<Submit>)submits.subList(this.fromInclusive, this.toInclusive + 1);
      }
   }
}
