package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class RenderTypeFeatureRenderer<Submit extends SubmitNode> implements FeatureRenderer<Submit> {
    private RenderTypeFeatureRenderer.@Nullable Group currentGroup;
    private final List<RenderTypeFeatureRenderer.Group> groups = new ArrayList<>();

    protected abstract void buildGroup(FeatureFrameContext context, List<Submit> submits);

    protected final VertexConsumer getVertexBuilder(final RenderType renderType) {
        return this.currentGroup().getVertexBuilder(renderType);
    }

    private RenderTypeFeatureRenderer.Group currentGroup() {
        return Objects.requireNonNull(this.currentGroup, "Not preparing group");
    }

    @Override
    public final void prepareGroup(final FeatureFrameContext context, final List<Submit> submits, final boolean strictlyOrdered) {
        this.currentGroup = new RenderTypeFeatureRenderer.Group(context.stagedVertexBuffer(), !strictlyOrdered);
        this.buildGroup(context, submits);
        this.groups.add(this.currentGroup);
        this.currentGroup = null;
    }

    @Override
    public void executeGroup(final FeatureFrameContext context, final int groupIndex, final List<Submit> submits, final boolean strictlyOrdered) {
        RenderTypeFeatureRenderer.Group group = this.groups.get(groupIndex);

        for (int i = 0; i < group.draws.size(); i++) {
            PreparedRenderType renderType = group.drawRenderTypes.get(i);
            StagedVertexBuffer.ExecuteInfo info = context.stagedVertexBuffer().getExecuteInfo(group.draws.get(i));
            if (info != null) {
                renderType.drawFromBuffer(info);
            }
        }
    }

    @Override
    public void finishExecute(final FeatureFrameContext context) {
        this.groups.clear();
    }

    @OnlyIn(Dist.CLIENT)
    private static class Group {
        private final StagedVertexBuffer stagedBuffer;
        private final boolean canReorder;
        private final List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
        private final List<PreparedRenderType> drawRenderTypes = new ArrayList<>();
        private @Nullable RenderType lastRenderType;
        private StagedVertexBuffer.@Nullable Draw lastDraw;

        private Group(final StagedVertexBuffer stagedBuffer, final boolean canReorder) {
            this.stagedBuffer = stagedBuffer;
            this.canReorder = canReorder;
        }

        public VertexConsumer getVertexBuilder(final RenderType renderType) {
            if (this.lastDraw == null || this.lastRenderType != renderType || !renderType.canConsolidateConsecutiveGeometry()) {
                this.lastDraw = this.getOrAddDraw(renderType);
                this.lastRenderType = renderType;
            }

            return this.stagedBuffer.getVertexBuilder(this.lastDraw);
        }

        private StagedVertexBuffer.Draw getOrAddDraw(final RenderType renderType) {
            PreparedRenderType preparedRenderType = renderType.prepare();
            int existingIndex = this.canReorder && renderType.canConsolidateConsecutiveGeometry() ? this.drawRenderTypes.indexOf(preparedRenderType) : -1;
            if (existingIndex != -1) {
                return this.draws.get(existingIndex);
            }

            VertexSorting quadSorting = renderType.sortOnUpload() ? RenderSystem.getProjectionType().vertexSorting() : null;
            StagedVertexBuffer.Draw draw = this.stagedBuffer.appendDraw(renderType.format(), renderType.primitiveTopology(), quadSorting);
            this.draws.add(draw);
            this.drawRenderTypes.add(preparedRenderType);
            return draw;
        }
    }
}