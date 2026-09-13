package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionOcclusionGraph {
    private static final int HALF_SECTION_SIZE = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final int MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE = SectionPos.blockToSectionCoord(60);
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    private @Nullable Future<?> fullUpdateTask;
    private @Nullable ViewArea viewArea;
    private final AtomicReference<
            SectionOcclusionGraph.@Nullable GraphState> currentGraph = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
    private final Set<SectionPos> emptySections = new HashSet<>();
    private final Set<ChunkPos> loadedChunks = new HashSet<>();
    private volatile @Nullable
    BlockingQueue<SectionRenderDispatcher.RenderSection> nextSectionsToPropagateFrom;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private int prevFov = Integer.MAX_VALUE;
    private boolean lastSmartCull = true;

    public void waitAndReset(final @Nullable ViewArea viewArea) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            } catch (Exception e) {
                LOGGER.warn("Full update failed", e);
            }
        }

        this.viewArea = viewArea;
        if (viewArea != null) {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(viewArea));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
            this.emptySections.clear();
            this.loadedChunks.clear();
        }
    }

    public Collection<ChunkPos> expectedChunks() {
        SectionOcclusionGraph.GraphState graphState = this.currentGraph.get();
        return graphState != null ? graphState.storage.sectionsWaitingForChunkLoads.keySet() : Collections.emptySet();
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void invalidateIfNeeded(final CameraRenderState camera, final int fov) {
        Vec3 cameraPos = camera.pos;
        double camX = Math.floor(cameraPos.x / 8.0);
        double camY = Math.floor(cameraPos.y / 8.0);
        double camZ = Math.floor(cameraPos.z / 8.0);
        if (camX != this.prevCamX || camY != this.prevCamY || camZ != this.prevCamZ || this.prevFov != fov || this.lastSmartCull != camera.smartCull) {
            this.invalidate();
        }

        this.prevCamX = camX;
        this.prevCamY = camY;
        this.prevCamZ = camZ;
        this.prevFov = fov;
        this.lastSmartCull = camera.smartCull;
    }

    public void addSectionsInFrustum(
            final Frustum frustum,
            final List<SectionRenderDispatcher.RenderSection> visibleSections,
            final List<SectionRenderDispatcher.RenderSection> nearbyVisibleSection) {
        Frustum offsetFrustum = offsetFrustum(frustum);
        this.currentGraph.get().storage().sectionTree.visitNodes((node, fullyVisible, depth, isClose) -> {
            SectionRenderDispatcher.RenderSection renderSection = node.getSection();
            if (renderSection != null) {
                visibleSections.add(renderSection);
                if (isClose) {
                    nearbyVisibleSection.add(renderSection);
                }
            }
        }, offsetFrustum, 32);
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void schedulePropagationFrom(final SectionRenderDispatcher.RenderSection section) {
        BlockingQueue<
                SectionRenderDispatcher.RenderSection> nextSectionsToPropagateFrom = this.nextSectionsToPropagateFrom;
        if (nextSectionsToPropagateFrom != null) {
            nextSectionsToPropagateFrom.add(section);
        }

        BlockingQueue<
                SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom = this.currentGraph.get().sectionsToPropagateFrom;
        if (sectionsToPropagateFrom != nextSectionsToPropagateFrom) {
            sectionsToPropagateFrom.add(section);
        }
    }

    public void update(final CameraRenderState camera, final int fov, final ChunkLoadingRenderState chunkLoadingRenderState) {
        this.updateLoadedChunks(chunkLoadingRenderState.addedLoadedChunks, chunkLoadingRenderState.removedLoadedChunks);
        this.updateEmptySections(chunkLoadingRenderState.addedEmptySections, chunkLoadingRenderState.removedEmptySections);
        if (!camera.isFrustumCaptured) {
            this.invalidateIfNeeded(camera, fov);
            if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
                this.scheduleFullUpdate(camera);
            }
            this.runPartialUpdate(camera, chunkLoadingRenderState.loadedExpectedChunks);
        }

        // MCRe 修复：全量遮挡图更新不阻塞主线程。
        // 原实现用 fullUpdateTask.get() 无限等待后台任务——回头/飞地下时大量区块变化导致
        // 后台全量重建慢（或 backgroundExecutor 线程池被占满）时，主线程永久冻结 = 卡死无日志。
        // currentGraph 是 AtomicReference，并发读写原子安全（主线程读到旧/新完整 GraphState），
        // 无需阻塞等待；任务完成后 needsFrustumUpdate 标志驱动可见性重算。
        if (this.fullUpdateTask != null && this.fullUpdateTask.isDone()) {
            this.fullUpdateTask = null;
        }
    }

    private void scheduleFullUpdate(final CameraRenderState camera) {
        this.needsFullUpdate = false;
        Set<SectionPos> clonedEmptySections = new HashSet<>(this.emptySections);
        Set<ChunkPos> clonedLoadedChunks = new HashSet<>(this.loadedChunks);
        this.fullUpdateTask = CompletableFuture.runAsync(() -> {
            SectionOcclusionGraph.GraphState newState = new SectionOcclusionGraph.GraphState(this.viewArea);
            this.nextSectionsToPropagateFrom = newState.sectionsToPropagateFrom;
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(camera.blockPos, queue);
            queue.forEach(node -> newState.storage.sectionToNodeMap.put(node.section, node));
            this.runUpdates(newState.storage, camera.pos, queue, camera.smartCull, node -> {}, clonedEmptySections, clonedLoadedChunks);
            this.currentGraph.set(newState);
            this.nextSectionsToPropagateFrom = null;
            this.needsFrustumUpdate.set(true);
        }, Util.backgroundExecutor());
    }

    private void runPartialUpdate(final CameraRenderState camera, final Set<
                    ChunkPos> loadedExpectedChunks) {
        SectionOcclusionGraph.GraphState state = this.currentGraph.get();
        loadedExpectedChunks.forEach(chunkNode -> {
            List<
                    SectionPos> waitingSections = state.storage.sectionsWaitingForChunkLoads.remove(chunkNode);
            if (waitingSections != null) {
                waitingSections.forEach(sectionNode -> {
                    SectionRenderDispatcher.RenderSection section = this.viewArea.getRenderSection(sectionNode);
                    if (section != null) {
                        this.schedulePropagationFrom(section);
                    }
                });
            }
        });
        if (!state.sectionsToPropagateFrom.isEmpty()) {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!state.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection renderSection = state.sectionsToPropagateFrom.poll();
                SectionOcclusionGraph.Node node = state.storage.sectionToNodeMap.get(renderSection);
                if (node != null && node.section == renderSection) {
                    queue.add(node);
                }
            }

            Frustum offsetFrustum = offsetFrustum(camera.cullFrustum);
            Consumer<SectionRenderDispatcher.RenderSection> onSectionAdded = section -> {
                if (offsetFrustum.isVisible(section.getBoundingBox())) {
                    this.needsFrustumUpdate.set(true);
                }
            };
            this.runUpdates(state.storage, camera.pos, queue, camera.smartCull, onSectionAdded, this.emptySections, this.loadedChunks);
        }
    }

    private void initializeQueueForFullUpdate(final BlockPos cameraPosition, final Queue<
                    SectionOcclusionGraph.Node> queue) {
        SectionPos cameraSectionNode = SectionPos.of(cameraPosition);
        int cameraSectionY = cameraSectionNode.y();
        SectionRenderDispatcher.RenderSection cameraSection = this.viewArea.getRenderSection(cameraSectionNode);
        if (cameraSection == null) {
            boolean isBelowTheWorld = cameraSectionY < this.viewArea.minSectionY();
            int sectionY = isBelowTheWorld ? this.viewArea.minSectionY() : this.viewArea.maxSectionY();
            int viewDistance = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> toAdd = Lists.newArrayList();
            int cameraSectionX = cameraSectionNode.x();
            int cameraSectionZ = cameraSectionNode.z();

            for (int sectionX = -viewDistance; sectionX <= viewDistance; sectionX++) {
                for (int sectionZ = -viewDistance; sectionZ <= viewDistance; sectionZ++) {
                    SectionRenderDispatcher.RenderSection renderSectionAt = this.viewArea
                            .getRenderSection(SectionPos.of(sectionX + cameraSectionX, sectionY, sectionZ + cameraSectionZ));
                    if (renderSectionAt != null && this.isInViewDistance(cameraSectionNode, renderSectionAt.getSectionNode())) {
                        Direction sourceDirection = isBelowTheWorld ? Direction.UP : Direction.DOWN;
                        SectionOcclusionGraph.Node node = new SectionOcclusionGraph.Node(renderSectionAt, sourceDirection, 0);
                        node.setDirections(node.directions, sourceDirection);
                        if (sectionX > 0) {
                            node.setDirections(node.directions, Direction.EAST);
                        } else if (sectionX < 0) {
                            node.setDirections(node.directions, Direction.WEST);
                        }

                        if (sectionZ > 0) {
                            node.setDirections(node.directions, Direction.SOUTH);
                        } else if (sectionZ < 0) {
                            node.setDirections(node.directions, Direction.NORTH);
                        }

                        toAdd.add(node);
                    }
                }
            }

            toAdd.sort(Comparator.comparingDouble(c -> {
                SectionPos node = c.section.getSectionNode();
                double dx = node.centerXLong() - cameraPosition.getX();
                double dy = node.centerYLong() - cameraPosition.getY();
                double dz = node.centerZLong() - cameraPosition.getZ();
                return dx * dx + dy * dy + dz * dz;
            }));
            queue.addAll(toAdd);
        } else {
            queue.add(new SectionOcclusionGraph.Node(cameraSection, null, 0));
        }
    }

    private void runUpdates(
            final SectionOcclusionGraph.GraphStorage storage,
            final Vec3 cameraPos,
            final Queue<SectionOcclusionGraph.Node> queue,
            final boolean smartCull,
            final Consumer<SectionRenderDispatcher.RenderSection> onSectionAdded,
            final Set<SectionPos> emptySections,
            final Set<ChunkPos> loadedChunks) {
        SectionPos cameraSectionPos = SectionPos.of(cameraPos);
        SectionPos cameraSectionNode = cameraSectionPos;
        // far lands：相机节中心用 long（section << 4 溢出防御）
        long cameraSectionCenterX = cameraSectionPos.centerXLong();
        long cameraSectionCenterY = cameraSectionPos.centerYLong();
        long cameraSectionCenterZ = cameraSectionPos.centerZLong();

        while (!queue.isEmpty()) {
            SectionOcclusionGraph.Node node = queue.poll();
            SectionRenderDispatcher.RenderSection currentSection = node.section;
            SectionPos sectionNode = currentSection.getSectionNode();
            ChunkPos chunkNode = new ChunkPos(sectionNode.x(), sectionNode.z());
            if (!loadedChunks.contains(chunkNode)) {
                storage.sectionsWaitingForChunkLoads.computeIfAbsent(chunkNode, var0 -> new ArrayList<>()).add(sectionNode);
            } else {
                if (!emptySections.contains(node.section.getSectionNode())) {
                    if (storage.sectionTree.add(node.section)) {
                        onSectionAdded.accept(node.section);
                    }
                } else {
                    node.section.sectionMesh.compareAndSet(CompiledSectionMesh.UNCOMPILED, CompiledSectionMesh.EMPTY);
                }

                boolean distantFromCamera = Math.abs(sectionNode.x() - cameraSectionPos.x()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                        || Math.abs(sectionNode.y() - cameraSectionPos.y()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                        || Math.abs(sectionNode.z() - cameraSectionPos.z()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE;

                for (Direction direction : DIRECTIONS) {
                    SectionRenderDispatcher.RenderSection renderSectionAt = this.getRelativeFrom(cameraSectionNode, currentSection, direction);
                    if (renderSectionAt != null && (!smartCull || !node.hasDirection(direction.getOpposite()))) {
                        if (smartCull && node.hasSourceDirections()) {
                            SectionMesh sectionMesh = currentSection.getSectionMesh();
                            boolean visible = false;

                            for (int i = 0; i < DIRECTIONS.length; i++) {
                                if (node.hasSourceDirection(i) && sectionMesh.facesCanSeeEachother(DIRECTIONS[
                                        i].getOpposite(), direction)) {
                                    visible = true;
                                    break;
                                }
                            }

                            if (!visible) {
                                continue;
                            }
                        }

                        if (smartCull && distantFromCamera) {
                            long renderSectionOriginX = sectionNode.minBlockXLong();
                            long renderSectionOriginY = sectionNode.minBlockYLong();
                            long renderSectionOriginZ = sectionNode.minBlockZLong();
                            boolean maxX = direction.getAxis() == Direction.Axis.X
                                    ? cameraSectionCenterX > renderSectionOriginX
                                    : cameraSectionCenterX < renderSectionOriginX;
                            boolean maxY = direction.getAxis() == Direction.Axis.Y
                                    ? cameraSectionCenterY > renderSectionOriginY
                                    : cameraSectionCenterY < renderSectionOriginY;
                            boolean maxZ = direction.getAxis() == Direction.Axis.Z
                                    ? cameraSectionCenterZ > renderSectionOriginZ
                                    : cameraSectionCenterZ < renderSectionOriginZ;
                            Vector3d checkPos = new Vector3d(
                            renderSectionOriginX + (maxX ? 16 : 0), renderSectionOriginY + (maxY ? 16 : 0), renderSectionOriginZ + (maxZ ? 16 : 0)
                            );
                            Vector3d step = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z).sub(checkPos).normalize().mul(CEILED_SECTION_DIAGONAL);
                            boolean visible = true;

                            while (checkPos.distanceSquared(cameraPos.x, cameraPos.y, cameraPos.z) > 3600.0) {
                                checkPos.add(step);
                                if (checkPos.y > this.viewArea.maxY() || checkPos.y < this.viewArea.minY()) {
                                    break;
                                }

                                SectionPos checkSectionNode = SectionPos.of(
                                        (int) (Mth.lfloor(checkPos.x) >> 4),
                                        (int) (Mth.lfloor(checkPos.y) >> 4),
                                        (int) (Mth.lfloor(checkPos.z) >> 4)
                                );
                                SectionRenderDispatcher.RenderSection checkSection = this.viewArea.getRenderSection(checkSectionNode);
                                if (checkSection == null || storage.sectionToNodeMap.get(checkSection) == null) {
                                    visible = false;
                                    break;
                                }
                            }

                            if (!visible) {
                                continue;
                            }
                        }

                        SectionOcclusionGraph.Node existingNode = storage.sectionToNodeMap.get(renderSectionAt);
                        if (existingNode != null) {
                            existingNode.addSourceDirection(direction);
                        } else {
                            SectionOcclusionGraph.Node newNode = new SectionOcclusionGraph.Node(renderSectionAt, direction, node.step + 1);
                            newNode.setDirections(node.directions, direction);
                            queue.add(newNode);
                            storage.sectionToNodeMap.put(renderSectionAt, newNode);
                        }
                    }
                }
            }
        }
    }

    private static Frustum offsetFrustum(final Frustum frustum) {
        return new Frustum(frustum).offsetToFullyIncludeCameraCube(8);
    }

    private boolean isInViewDistance(final SectionPos cameraSectionNode, final SectionPos sectionNode) {
        return ChunkTrackingView.isInViewDistance(
                cameraSectionNode.x(),
                cameraSectionNode.z(),
                this.viewArea.getViewDistance(),
                sectionNode.x(),
                sectionNode.z()
        );
    }

    private SectionRenderDispatcher.@Nullable RenderSection getRelativeFrom(
            final SectionPos cameraSectionNode, final SectionRenderDispatcher.RenderSection renderSection, final Direction direction) {
        SectionPos relative = renderSection.getNeighborSectionNode(direction);
        if (!this.isInViewDistance(cameraSectionNode, relative)) {
            return null;
        } else {
            return Mth.abs(cameraSectionNode.y() - relative.y()) > this.viewArea.getViewDistance()
                    ? null
                    : this.viewArea.getRenderSection(relative);
        }
    }

    @VisibleForDebug
    public SectionOcclusionGraph.@Nullable
            Node getNode(final SectionRenderDispatcher.RenderSection section) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(section);
    }

    public void updateEmptySections(final Set<SectionPos> added, final Set<SectionPos> removed) {
        this.emptySections.addAll(added);
        var iter = removed.iterator();

        while (iter.hasNext()) {
            SectionPos sectionNode = iter.next();
            if (this.emptySections.remove(sectionNode)) {
                SectionRenderDispatcher.RenderSection section = this.viewArea.getRenderSection(sectionNode);
                if (section != null) {
                    this.schedulePropagationFrom(section);
                    section.setWasPreviouslyEmpty(true);
                }
            }
        }
    }

    public void updateLoadedChunks(final Set<ChunkPos> added, final Set<ChunkPos> removed) {
        this.loadedChunks.addAll(added);
        this.loadedChunks.removeAll(removed);
    }

    public Octree getOctree() {
        return this.currentGraph.get().storage.sectionTree;
    }

    @OnlyIn(Dist.CLIENT)
    private record GraphState(SectionOcclusionGraph.GraphStorage storage, BlockingQueue<
                    SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        private GraphState(final ViewArea viewArea) {
            this(new SectionOcclusionGraph.GraphStorage(viewArea), new LinkedBlockingQueue<>());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class GraphStorage {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Octree sectionTree;
        public final Map<ChunkPos, List<SectionPos>> sectionsWaitingForChunkLoads;

        public GraphStorage(final ViewArea viewArea) {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(viewArea.size());
            this.sectionTree = new Octree(viewArea.getCameraSectionPos(), viewArea.getViewDistance(), viewArea.sectionCount(), viewArea.minY());
            this.sectionsWaitingForChunkLoads = new HashMap<>();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @VisibleForDebug
    public static class Node {
        @VisibleForDebug protected final SectionRenderDispatcher.RenderSection section;
        private byte sourceDirections;
        private byte directions;
        @VisibleForDebug public final int step;

        private Node(final SectionRenderDispatcher.RenderSection section, final @Nullable
                Direction sourceDirection, final int step) {
            this.section = section;
            if (sourceDirection != null) {
                this.addSourceDirection(sourceDirection);
            }

            this.step = step;
        }

        private void setDirections(final byte oldDirections, final Direction direction) {
            this.directions = (byte) (this.directions | oldDirections | 1 << direction.ordinal());
        }

        private boolean hasDirection(final Direction direction) {
            return (this.directions & 1 << direction.ordinal()) > 0;
        }

        private void addSourceDirection(final Direction direction) {
            this.sourceDirections = (byte) (this.sourceDirections | this.sourceDirections | 1 << direction.ordinal());
        }

        @VisibleForDebug
        public boolean hasSourceDirection(final int directionOrdinal) {
            return (this.sourceDirections & 1 << directionOrdinal) > 0;
        }

        private boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode() {
            return this.section.getSectionNode().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj
                            instanceof
                            SectionOcclusionGraph.Node other ? this.section.getSectionNode().equals(other.section.getSectionNode()) : false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class SectionToNodeMap {
        private final SectionOcclusionGraph.Node[] nodes;

        private SectionToNodeMap(final int sectionCount) {
            this.nodes = new SectionOcclusionGraph.Node[sectionCount];
        }

        public void put(final SectionRenderDispatcher.RenderSection renderSection, final SectionOcclusionGraph.Node node) {
            this.nodes[renderSection.index] = node;
        }

        public SectionOcclusionGraph.@Nullable
                Node get(final SectionRenderDispatcher.RenderSection renderSection) {
            int index = renderSection.index;
            return index >= 0 && index < this.nodes.length ? this.nodes[index] : null;
        }
    }
}