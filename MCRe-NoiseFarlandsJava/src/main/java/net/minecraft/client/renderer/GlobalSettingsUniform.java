package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class GlobalSettingsUniform implements AutoCloseable {
    public static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();
    private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

    public void update(
        final int width,
        final int height,
        final double glintAlpha,
        final long gameTime,
        final DeltaTracker deltaTracker,
        final int menuBlurRadius,
        final Vec3 cameraPos,
        final boolean useRgss
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // far lands：相机坐标可能越过 2^31，用 long 计算方块坐标（mod 2^32 的 int 表示），
            // 保证 CameraBlockPos 与 ChunkPosition 的 int 差值在 shader 中自洽。
            long cameraX = Mth.lfloor(cameraPos.x);
            long cameraY = Mth.lfloor(cameraPos.y);
            long cameraZ = Mth.lfloor(cameraPos.z);
            ByteBuffer data = Std140Builder.onStack(stack, UBO_SIZE)
                .putIVec3((int)cameraX, (int)cameraY, (int)cameraZ)
                .putVec3((float)(cameraX - cameraPos.x), (float)(cameraY - cameraPos.y), (float)(cameraZ - cameraPos.z))
                .putVec2(width, height)
                .putFloat((float)glintAlpha)
                .putFloat(((float)(gameTime % 24000L) + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24000.0F)
                .putInt(menuBlurRadius)
                .putInt(useRgss ? 1 : 0)
                .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), data);
        }

        RenderSystem.setGlobalSettingsUniform(this.buffer);
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}