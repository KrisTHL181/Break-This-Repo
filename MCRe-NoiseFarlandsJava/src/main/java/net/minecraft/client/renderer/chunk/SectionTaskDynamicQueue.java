package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SectionTaskDynamicQueue {
    private static final int MAX_RECOMPILE_QUOTA = 2;
    private int recompileQuota = 2;
    private final List<SectionRenderDispatcher.RenderSection.SectionTask> tasks = new ObjectArrayList<>();

    public synchronized void add(final SectionRenderDispatcher.RenderSection.SectionTask task) {
        this.tasks.add(task);
    }

    public synchronized SectionRenderDispatcher.RenderSection.@Nullable SectionTask poll(final Vec3 cameraPos) {
        int bestInitialCompileTaskIndex = -1;
        int bestRecompileTaskIndex = -1;
        double bestInitialCompileDistance = Double.MAX_VALUE;
        double bestRecompileDistance = Double.MAX_VALUE;
        ListIterator<SectionRenderDispatcher.RenderSection.SectionTask> iterator = this.tasks.listIterator();

        while (iterator.hasNext()) {
            int taskIndex = iterator.nextIndex();
            SectionRenderDispatcher.RenderSection.SectionTask task = iterator.next();
            if (task.isCancelled.get()) {
                iterator.remove();
            } else {
                Vec3 renderOrigin = task.getRenderOrigin();
                double dx = renderOrigin.x - cameraPos.x;
                double dy = renderOrigin.y - cameraPos.y;
                double dz = renderOrigin.z - cameraPos.z;
                double distance = dx * dx + dy * dy + dz * dz;
                if (!task.isRecompile() && distance < bestInitialCompileDistance) {
                    bestInitialCompileDistance = distance;
                    bestInitialCompileTaskIndex = taskIndex;
                }

                if (task.isRecompile() && distance < bestRecompileDistance) {
                    bestRecompileDistance = distance;
                    bestRecompileTaskIndex = taskIndex;
                }
            }
        }

        boolean hasRecompileTask = bestRecompileTaskIndex >= 0;
        boolean hasInitialCompileTask = bestInitialCompileTaskIndex >= 0;
        if (!hasRecompileTask || hasInitialCompileTask && (this.recompileQuota <= 0 || !(bestRecompileDistance < bestInitialCompileDistance))) {
            this.recompileQuota = 2;
            return this.removeTaskByIndex(bestInitialCompileTaskIndex);
        } else {
            this.recompileQuota--;
            return this.removeTaskByIndex(bestRecompileTaskIndex);
        }
    }

    public int size() {
        return this.tasks.size();
    }

    private SectionRenderDispatcher.RenderSection.@Nullable SectionTask removeTaskByIndex(final int taskIndex) {
        return taskIndex >= 0 ? this.tasks.remove(taskIndex) : null;
    }

    public synchronized void clear() {
        for (SectionRenderDispatcher.RenderSection.SectionTask task : this.tasks) {
            task.cancel();
        }

        this.tasks.clear();
    }
}