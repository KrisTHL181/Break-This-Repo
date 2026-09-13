package com.mojang.blaze3d.platform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public enum IconSet {
    RELEASE("icons"),
    SNAPSHOT("icons", "snapshot");

    private final String[] path;

    IconSet(final String... path) {
        this.path = path;
    }

    public List<IoSupplier<InputStream>> getStandardIcons(final PackResources resources) throws IOException {
        return List.of(
            this.getFile(resources, "icon_16x16.png"),
            this.getFile(resources, "icon_32x32.png"),
            this.getFile(resources, "icon_48x48.png"),
            this.getFile(resources, "icon_128x128.png"),
            this.getFile(resources, "icon_256x256.png")
        );
    }

    public IoSupplier<InputStream> getMacIcon(final PackResources resources) throws IOException {
        return this.getFile(resources, "minecraft.icns");
    }

    private IoSupplier<InputStream> getFile(final PackResources resources, final String fileName) throws IOException {
        String[] fullPath = ArrayUtils.add(this.path, fileName);
        IoSupplier<InputStream> resource = resources.getRootResource(fullPath);
        if (resource == null) {
            throw new FileNotFoundException(String.join("/", fullPath));
        } else {
            return resource;
        }
    }
}