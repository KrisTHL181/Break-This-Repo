package rip.ysm.api.client.forge;

import com.mojang.blaze3d.vertex.BufferBuilder;

import java.nio.ByteBuffer;

public final class BufferBuilderBridgeImpl {

    private BufferBuilderBridgeImpl() {
    }

    public static boolean putBulkData(BufferBuilder builder, ByteBuffer buffer) {
        builder.putBulkData(buffer);
        return true;
    }

    public static boolean supportsDirectTransfer() {
        return true;
    }
}
