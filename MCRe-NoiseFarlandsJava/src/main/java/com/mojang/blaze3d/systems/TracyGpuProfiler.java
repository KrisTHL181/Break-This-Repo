package com.mojang.blaze3d.systems;

import com.mojang.jtracy.GpuApi;
import com.mojang.jtracy.GpuContext;
import com.mojang.jtracy.TracyClient;
import java.util.OptionalLong;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TracyGpuProfiler {
    private static final int MAX_QUERIES = 1024;
    private final GpuQueryPool queries;
    private final GpuContext context;
    private int head = 0;
    private int tail = 0;

    public TracyGpuProfiler(final GpuDevice device) {
        this.queries = device.createTimestampQueryPool(1024);
        float period = device.getDeviceInfo().timestampPeriod();
        this.context = TracyClient.createGpuContext(GpuApi.OPENGL, device.getTimestampNow(), period);
    }

    public void close() {
        this.queries.close();
    }

    public void pushZone(final CommandEncoder encoder, final String name) {
        int queryId = this.nextQueryId();
        encoder.writeTimestamp(this.queries, queryId);
        this.context.beginZone(queryId, name, "", "", 0);
    }

    public void popZone(final CommandEncoder encoder) {
        int queryId = this.nextQueryId();
        encoder.writeTimestamp(this.queries, queryId);
        this.context.endZone(queryId);
    }

    public void endFrame() {
        if (this.head < this.tail) {
            OptionalLong[] timestamps = this.queries.getValues(this.tail, 1024 - this.tail);

            for (int i = 0; i < timestamps.length; i++) {
                OptionalLong timestamp = timestamps[i];
                if (!timestamp.isPresent()) {
                    return;
                }

                this.context.submitQueryTimestamp(this.tail, timestamp.getAsLong());
                this.tail = (this.tail + 1) % 1024;
            }
        }

        if (this.tail < this.head) {
            OptionalLong[] timestamps = this.queries.getValues(this.tail, this.head - this.tail);

            for (int i = 0; i < timestamps.length; i++) {
                OptionalLong timestamp = timestamps[i];
                if (!timestamp.isPresent()) {
                    return;
                }

                this.context.submitQueryTimestamp(this.tail, timestamp.getAsLong());
                this.tail = (this.tail + 1) % 1024;
            }
        }
    }

    private int nextQueryId() {
        int id = this.head;
        this.head = (this.head + 1) % 1024;
        return id;
    }
}