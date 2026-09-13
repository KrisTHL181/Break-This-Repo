package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
    private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
    private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
    private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
    private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_MEMBERS = true;
    private static final boolean SHOW_BLACKLISTS = true;
    private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
    private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final float TEXT_SCALE = 0.32F;
    private static final int ORANGE = -23296;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private final Minecraft minecraft;
    private @Nullable UUID lastLookedAtUuid;

    public BeeDebugRenderer(final Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(
        final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
    ) {
        this.doRender(debugValues);
        if (!this.minecraft.player.isSpectator()) {
            this.updateLastLookedAtUuid();
        }
    }

    private void doRender(final DebugValueAccess debugValues) {
        BlockPos playerPos = this.getCamera().blockPosition();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, beeInfo) -> {
            if (this.minecraft.player.closerThan(entity, 30.0)) {
                DebugGoalInfo goalInfo = debugValues.getEntityValue(DebugSubscriptions.GOAL_SELECTORS, entity);
                this.renderBeeInfo(entity, beeInfo, goalInfo);
            }
        });
        this.renderFlowerInfos(debugValues);
        Map<BlockPos, Set<UUID>> hiveBlacklistMap = this.createHiveBlacklistMap(debugValues);
        debugValues.forEachBlock(DebugSubscriptions.BEE_HIVES, (pos, hive) -> {
            if (playerPos.closerThan(pos, 30.0)) {
                highlightHive(pos);
                Set<UUID> beesWhoBlacklistThisHive = hiveBlacklistMap.getOrDefault(pos, Set.of());
                this.renderHiveInfo(pos, hive, beesWhoBlacklistThisHive, debugValues);
            }
        });
        this.getGhostHives(debugValues).forEach((ghostHivePos, value) -> {
            if (playerPos.closerThan(ghostHivePos, 30.0)) {
                this.renderGhostHive(ghostHivePos, (List<String>)value);
            }
        });
    }

    private Map<BlockPos, Set<UUID>> createHiveBlacklistMap(final DebugValueAccess debugValues) {
        Map<BlockPos, Set<UUID>> hiveBlacklistMap = new HashMap<>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, bee) -> {
            for (BlockPos blacklistedFlowerPos : bee.blacklistedHives()) {
                hiveBlacklistMap.computeIfAbsent(blacklistedFlowerPos, k -> new HashSet<>()).add(entity.getUUID());
            }
        });
        return hiveBlacklistMap;
    }

    private void renderFlowerInfos(final DebugValueAccess debugValues) {
        Map<BlockPos, Set<UUID>> beesPerFlower = new HashMap<>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, bee) -> {
            if (bee.flowerPos().isPresent()) {
                beesPerFlower.computeIfAbsent(bee.flowerPos().get(), k -> new HashSet<>()).add(entity.getUUID());
            }
        });
        beesPerFlower.forEach((flowerPos, beesWithThisFlower) -> {
            Set<String> beeNames = beesWithThisFlower.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
            int row = 1;
            Gizmos.billboardTextOverBlock(beeNames.toString(), flowerPos, row++, -256, 0.32F);
            Gizmos.billboardTextOverBlock("Flower", flowerPos, row++, -1, 0.32F);
            Gizmos.cuboid(flowerPos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.8F, 0.8F, 0.0F)));
        });
    }

    private static String getBeeUuidsAsString(final Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return "-";
        } else {
            return uuids.size() > 3
                ? uuids.size() + " bees"
                : uuids.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
        }
    }

    private static void highlightHive(final BlockPos hivePos) {
        float padding = 0.05F;
        Gizmos.cuboid(hivePos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
    }

    private void renderGhostHive(final BlockPos ghostHivePos, final List<String> hiveMemberNames) {
        float padding = 0.05F;
        Gizmos.cuboid(ghostHivePos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
        Gizmos.billboardTextOverBlock(hiveMemberNames.toString(), ghostHivePos, 0, -256, 0.32F);
        Gizmos.billboardTextOverBlock("Ghost Hive", ghostHivePos, 1, -65536, 0.32F);
    }

    private void renderHiveInfo(
        final BlockPos hivePos, final DebugHiveInfo hive, final Collection<UUID> beesWhoBlacklistThisHive, final DebugValueAccess debugValues
    ) {
        int row = 0;
        if (!beesWhoBlacklistThisHive.isEmpty()) {
            renderTextOverHive("Blacklisted by " + getBeeUuidsAsString(beesWhoBlacklistThisHive), hivePos, row++, -65536);
        }

        renderTextOverHive("Out: " + getBeeUuidsAsString(this.getHiveMembers(hivePos, debugValues)), hivePos, row++, -3355444);
        if (hive.occupantCount() == 0) {
            renderTextOverHive("In: -", hivePos, row++, -256);
        } else if (hive.occupantCount() == 1) {
            renderTextOverHive("In: 1 bee", hivePos, row++, -256);
        } else {
            renderTextOverHive("In: " + hive.occupantCount() + " bees", hivePos, row++, -256);
        }

        renderTextOverHive("Honey: " + hive.honeyLevel(), hivePos, row++, -23296);
        renderTextOverHive(hive.type().getName().getString() + (hive.sedated() ? " (sedated)" : ""), hivePos, row++, -1);
    }

    private void renderBeeInfo(final Entity entity, final DebugBeeInfo beeInfo, final @Nullable DebugGoalInfo goalInfo) {
        boolean selected = this.isBeeSelected(entity);
        int row = 0;
        Gizmos.billboardTextOverMob(entity, row++, beeInfo.toString(), -1, 0.48F);
        if (beeInfo.hivePos().isEmpty()) {
            Gizmos.billboardTextOverMob(entity, row++, "No hive", -98404, 0.32F);
        } else {
            Gizmos.billboardTextOverMob(entity, row++, "Hive: " + this.getPosDescription(entity, beeInfo.hivePos().get()), -256, 0.32F);
        }

        if (beeInfo.flowerPos().isEmpty()) {
            Gizmos.billboardTextOverMob(entity, row++, "No flower", -98404, 0.32F);
        } else {
            Gizmos.billboardTextOverMob(entity, row++, "Flower: " + this.getPosDescription(entity, beeInfo.flowerPos().get()), -256, 0.32F);
        }

        if (goalInfo != null) {
            for (DebugGoalInfo.DebugGoal goal : goalInfo.goals()) {
                if (goal.isRunning()) {
                    Gizmos.billboardTextOverMob(entity, row++, goal.name(), -16711936, 0.32F);
                }
            }
        }

        if (beeInfo.travelTicks() > 0) {
            int color = beeInfo.travelTicks() < 2400 ? -3355444 : -23296;
            Gizmos.billboardTextOverMob(entity, row++, "Travelling: " + beeInfo.travelTicks() + " ticks", color, 0.32F);
        }
    }

    private static void renderTextOverHive(final String text, final BlockPos hivePos, final int row, final int color) {
        Gizmos.billboardTextOverBlock(text, hivePos, row, color, 0.32F);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.mainCamera();
    }

    private String getPosDescription(final Entity entity, final BlockPos pos) {
        double dist = pos.distToCenterSqr(entity.position());
        double distRounded = Math.round(dist * 10.0) / 10.0;
        return pos.toShortString() + " (dist " + distRounded + ")";
    }

    private boolean isBeeSelected(final Entity entity) {
        return Objects.equals(this.lastLookedAtUuid, entity.getUUID());
    }

    private Collection<UUID> getHiveMembers(final BlockPos hivePos, final DebugValueAccess debugValues) {
        Set<UUID> hiveMembers = new HashSet<>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, beeInfo) -> {
            if (beeInfo.hasHive(hivePos)) {
                hiveMembers.add(entity.getUUID());
            }
        });
        return hiveMembers;
    }

    private Map<BlockPos, List<String>> getGhostHives(final DebugValueAccess debugValues) {
        Map<BlockPos, List<String>> ghostHives = new HashMap<>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, beeInfo) -> {
            if (beeInfo.hivePos().isPresent() && debugValues.getBlockValue(DebugSubscriptions.BEE_HIVES, beeInfo.hivePos().get()) == null) {
                ghostHives.computeIfAbsent(beeInfo.hivePos().get(), k -> Lists.newArrayList()).add(DebugEntityNameGenerator.getEntityName(entity));
            }
        });
        return ghostHives;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(entity -> this.lastLookedAtUuid = entity.getUUID());
    }
}