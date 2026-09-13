package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(
        final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
    ) {
        debugValues.forEachBlock(DebugSubscriptions.VILLAGE_SECTIONS, (pos, ignored) -> {
            SectionPos villageSection = SectionPos.of(pos);
            long cx = villageSection.centerXLong();
            long cy = villageSection.centerYLong();
            long cz = villageSection.centerZLong();
            Gizmos.cuboid(new AABB(cx, cy, cz, cx + 1, cy + 1, cz + 1), GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 0.2F, 1.0F, 0.2F)));
        });
    }
}