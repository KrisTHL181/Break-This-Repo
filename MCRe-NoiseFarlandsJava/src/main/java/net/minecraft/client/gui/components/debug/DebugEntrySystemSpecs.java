package net.minecraft.client.gui.components.debug;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySystemSpecs implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("system");

    @Override
    public void display(
        final DebugScreenDisplayer displayer,
        final @Nullable Level serverOrClientLevel,
        final @Nullable LevelChunk clientChunk,
        final @Nullable LevelChunk serverChunk
    ) {
        DeviceInfo deviceInfo = RenderSystem.getDevice().getDeviceInfo();
        displayer.addToGroup(
            GROUP,
            List.of(
                String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")),
                String.format(Locale.ROOT, "CPU: %s", GLX._getCpuInfo()),
                String.format(
                    Locale.ROOT,
                    "Display: %dx%d (%s)",
                    Minecraft.getInstance().getWindow().getWidth(),
                    Minecraft.getInstance().getWindow().getHeight(),
                    deviceInfo.vendorName()
                ),
                String.format(Locale.ROOT, "%s%s", deviceInfo.name(), this.typeName(deviceInfo.type())),
                String.format(Locale.ROOT, "%s %s", deviceInfo.backendName(), this.firstLine(deviceInfo.driverInfo()))
            )
        );
    }

    private String firstLine(final String value) {
        return value.lines().findFirst().orElse(value);
    }

    private String typeName(final DeviceType type) {
        return switch (type) {
            case OTHER -> "";
            case INTEGRATED -> " (iGPU)";
            case DISCRETE -> " (dGPU)";
            case VIRTUAL -> " (vGPU)";
            case CPU -> " (software)";
        };
    }

    @Override
    public boolean isAllowed(final boolean reducedDebugInfo) {
        return true;
    }
}