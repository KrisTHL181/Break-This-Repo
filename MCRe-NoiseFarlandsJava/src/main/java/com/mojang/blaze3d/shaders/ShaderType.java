package com.mojang.blaze3d.shaders;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public enum ShaderType {
    VERTEX("vertex", ".vsh"),
    FRAGMENT("fragment", ".fsh");

    private static final ShaderType[] TYPES = values();
    private final String name;
    private final String extension;

    ShaderType(final String name, final String extension) {
        this.name = name;
        this.extension = extension;
    }

    public static @Nullable ShaderType byLocation(final Identifier location) {
        for (ShaderType type : TYPES) {
            if (location.getPath().endsWith(type.extension)) {
                return type;
            }
        }

        return null;
    }

    public String getName() {
        return this.name;
    }

    public FileToIdConverter idConverter() {
        return new FileToIdConverter("shaders", this.extension);
    }
}