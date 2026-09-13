package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class ChunkCullingDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;

    public ChunkCullingDebugRenderer(final Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(
        final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
    ) {
        LevelRenderer levelRenderer = this.minecraft.levelRenderer;
        boolean sectionPath = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_PATHS);
        boolean sectionVisibility = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
        if (sectionPath || sectionVisibility) {
            SectionOcclusionGraph sectionOcclusionGraph = levelRenderer.sectionOcclusionGraph();

            for (SectionRenderDispatcher.RenderSection section : levelRenderer.visibleSections()) {
                SectionOcclusionGraph.Node node = sectionOcclusionGraph.getNode(section);
                if (node != null) {
                    Vec3 renderOffset = section.getRenderOrigin();
                    if (sectionPath) {
                        int color = node.step == 0 ? 0 : Mth.hsvToRgb(node.step / 50.0F, 0.9F, 0.9F);

                        for (int i = 0; i < DIRECTIONS.length; i++) {
                            if (node.hasSourceDirection(i)) {
                                Direction direction = DIRECTIONS[i];
                                Gizmos.line(
                                    renderOffset.add(8.0, 8.0, 8.0),
                                    renderOffset.add(
                                        8 - 16 * direction.getStepX(), 8 - 16 * direction.getStepY(), 8 - 16 * direction.getStepZ()
                                    ),
                                    ARGB.opaque(color)
                                );
                            }
                        }
                    }

                    if (sectionVisibility && section.getSectionMesh().hasRenderableLayers()) {
                        int c = 0;

                        for (Direction direction1 : DIRECTIONS) {
                            for (Direction direction2 : DIRECTIONS) {
                                boolean b = section.getSectionMesh().facesCanSeeEachother(direction1, direction2);
                                if (!b) {
                                    c++;
                                    Gizmos.line(
                                        renderOffset.add(
                                            8 + 8 * direction1.getStepX(), 8 + 8 * direction1.getStepY(), 8 + 8 * direction1.getStepZ()
                                        ),
                                        renderOffset.add(
                                            8 + 8 * direction2.getStepX(), 8 + 8 * direction2.getStepY(), 8 + 8 * direction2.getStepZ()
                                        ),
                                        ARGB.color(255, 255, 0, 0)
                                    );
                                }
                            }
                        }

                        if (c > 0) {
                            float delta = 0.5F;
                            float a = 0.2F;
                            Gizmos.cuboid(section.getBoundingBox().deflate(0.5), GizmoStyle.fill(ARGB.colorFromFloat(0.2F, 0.9F, 0.9F, 0.0F)));
                        }
                    }
                }
            }
        }

        Frustum capturedFrustum = this.minecraft.gameRenderer.mainCamera().getCapturedFrustum();
        if (capturedFrustum != null) {
            Vec3 offset = new Vec3(capturedFrustum.getCamX(), capturedFrustum.getCamY(), capturedFrustum.getCamZ());
            Vector4f[] frustumPoints = capturedFrustum.getFrustumPoints();
            this.addFrustumQuad(offset, frustumPoints, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(offset, frustumPoints, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(offset, frustumPoints, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(offset, frustumPoints, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(offset, frustumPoints, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(offset, frustumPoints, 1, 5, 6, 2, 1, 0, 1);
            this.addFrustumLine(offset, frustumPoints[0], frustumPoints[1]);
            this.addFrustumLine(offset, frustumPoints[1], frustumPoints[2]);
            this.addFrustumLine(offset, frustumPoints[2], frustumPoints[3]);
            this.addFrustumLine(offset, frustumPoints[3], frustumPoints[0]);
            this.addFrustumLine(offset, frustumPoints[4], frustumPoints[5]);
            this.addFrustumLine(offset, frustumPoints[5], frustumPoints[6]);
            this.addFrustumLine(offset, frustumPoints[6], frustumPoints[7]);
            this.addFrustumLine(offset, frustumPoints[7], frustumPoints[4]);
            this.addFrustumLine(offset, frustumPoints[0], frustumPoints[4]);
            this.addFrustumLine(offset, frustumPoints[1], frustumPoints[5]);
            this.addFrustumLine(offset, frustumPoints[2], frustumPoints[6]);
            this.addFrustumLine(offset, frustumPoints[3], frustumPoints[7]);
        }
    }

    private void addFrustumLine(final Vec3 offset, final Vector4f a, final Vector4f b) {
        Gizmos.line(new Vec3(offset.x + a.x, offset.y + a.y, offset.z + a.z), new Vec3(offset.x + b.x, offset.y + b.y, offset.z + b.z), -16777216);
    }

    private void addFrustumQuad(
        final Vec3 offset, final Vector4f[] frustumPoints, final int i0, final int i1, final int i2, final int i3, final int r, final int g, final int b
    ) {
        float a = 0.25F;
        Gizmos.rect(
            new Vec3(frustumPoints[i0].x(), frustumPoints[i0].y(), frustumPoints[i0].z()).add(offset),
            new Vec3(frustumPoints[i1].x(), frustumPoints[i1].y(), frustumPoints[i1].z()).add(offset),
            new Vec3(frustumPoints[i2].x(), frustumPoints[i2].y(), frustumPoints[i2].z()).add(offset),
            new Vec3(frustumPoints[i3].x(), frustumPoints[i3].y(), frustumPoints[i3].z()).add(offset),
            GizmoStyle.fill(ARGB.colorFromFloat(0.25F, r, g, b))
        );
    }
}