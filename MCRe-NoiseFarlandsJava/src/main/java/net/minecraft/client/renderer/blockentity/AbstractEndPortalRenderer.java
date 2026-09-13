package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractEndPortalRenderer<T extends TheEndPortalBlockEntity, S extends EndPortalRenderState> implements BlockEntityRenderer<T, S> {
    private static final Vector3fc FROM = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3fc TO = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final Map<Direction, List<Vector3fc>> FACES = Util.makeEnumMap(
        Direction.class,
        direction -> {
            FaceInfo faceInfo = FaceInfo.fromFacing(direction);
            return List.of(
                faceInfo.getVertexInfo(0).select(FROM, TO),
                faceInfo.getVertexInfo(1).select(FROM, TO),
                faceInfo.getVertexInfo(2).select(FROM, TO),
                faceInfo.getVertexInfo(3).select(FROM, TO)
            );
        }
    );
    public static final Identifier END_SKY_LOCATION = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
    public static final Identifier END_PORTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_portal/end_portal.png");
    private static final List<Direction> ALL_FACES = List.of(Direction.values());

    public void extractRenderState(
        final T blockEntity,
        final S state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facesToShow.clear();

        for (Direction direction : Direction.values()) {
            if (blockEntity.shouldRenderFace(direction)) {
                state.facesToShow.add(direction);
            }
        }
    }

    protected static void submitCube(
        final Collection<Direction> facesToShow, final RenderType renderType, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector
    ) {
        if (!facesToShow.isEmpty()) {
            submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                for (Direction direction : facesToShow) {
                    for (Vector3fc faceVertex : FACES.get(direction)) {
                        buffer.addVertex(pose, faceVertex);
                    }
                }
            });
        }
    }

    public static void submitSpecial(final RenderType renderType, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector) {
        submitCube(ALL_FACES, renderType, poseStack, submitNodeCollector);
    }

    public static void getExtents(final Consumer<Vector3fc> output) {
        FACES.values().forEach(vertices -> vertices.forEach(output));
    }
}