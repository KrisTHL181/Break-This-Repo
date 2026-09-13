package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int JUMP_TARGET_LINE_COLOR = ARGB.color(255, 255, 100, 255);
    private static final int TARGET_LINE_COLOR = ARGB.color(255, 100, 255, 255);
    private static final int INNER_CIRCLE_COLOR = ARGB.color(255, 0, 255, 0);
    private static final int MIDDLE_CIRCLE_COLOR = ARGB.color(255, 255, 165, 0);
    private static final int OUTER_CIRCLE_COLOR = ARGB.color(255, 255, 0, 0);
    private final Minecraft minecraft;

    public BreezeDebugRenderer(final Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(
        final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
    ) {
        ClientLevel level = this.minecraft.level;
        debugValues.forEachEntity(
            DebugSubscriptions.BREEZES,
            (entity, info) -> {
                info.attackTarget()
                    .map(level::getEntity)
                    .map(targetEntity -> targetEntity.getPosition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)))
                    .ifPresent(attackTargetPosition -> {
                        Gizmos.arrow(entity.position(), attackTargetPosition, TARGET_LINE_COLOR);
                        Vec3 drawCenter = attackTargetPosition.add(0.0, 0.01F, 0.0);
                        Gizmos.circle(drawCenter, 4.0F, GizmoStyle.stroke(INNER_CIRCLE_COLOR));
                        Gizmos.circle(drawCenter, 8.0F, GizmoStyle.stroke(MIDDLE_CIRCLE_COLOR));
                        Gizmos.circle(drawCenter, 24.0F, GizmoStyle.stroke(OUTER_CIRCLE_COLOR));
                    });
                info.jumpTarget().ifPresent(blockPos -> {
                    Gizmos.arrow(entity.position(), Vec3.atCenterOf(blockPos), JUMP_TARGET_LINE_COLOR);
                    Gizmos.cuboid(AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(blockPos)), GizmoStyle.fill(ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F)));
                });
            }
        );
    }
}