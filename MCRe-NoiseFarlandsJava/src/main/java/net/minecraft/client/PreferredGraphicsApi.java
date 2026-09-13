package net.minecraft.client;

import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.vulkan.VulkanBackend;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum PreferredGraphicsApi implements StringRepresentable {
    DEFAULT("default", "options.graphicsApi.default"),
    OPENGL("opengl", "options.graphicsApi.opengl"),
    VULKAN("vulkan", "options.graphicsApi.vulkan");

    public static final Codec<PreferredGraphicsApi> CODEC = StringRepresentable.fromEnum(PreferredGraphicsApi::values);
    private final String serializedName;
    private final Component key;

    PreferredGraphicsApi(final String serializedName, final String key) {
        this.serializedName = serializedName;
        this.key = Component.translatable(key);
    }

    public Component caption() {
        return this.key;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public GpuBackend[] getBackendsToTry() {
        GlBackend gl = new GlBackend();
        VulkanBackend vulkan = new VulkanBackend();
        return this == VULKAN ? new GpuBackend[]{vulkan, gl} : new GpuBackend[]{gl, vulkan};
    }
}