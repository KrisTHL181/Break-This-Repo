package net.minecraft.server.packs.resources;

public interface CloseableResourceManager extends ResourceManager, AutoCloseable {
    @Override
    void close();
}