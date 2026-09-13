package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TpsDebugChart extends AbstractDebugChart {
    private static final int TICK_METHOD_COLOR = -6745839;
    private static final int TASK_COLOR = -4548257;
    private static final int OTHER_COLOR = -10547572;
    private final Supplier<Float> msptSupplier;

    public TpsDebugChart(final Font font, final SampleStorage sampleStorage, final Supplier<Float> msptSupplier) {
        super(font, sampleStorage);
        this.msptSupplier = msptSupplier;
    }

    @Override
    protected void extractAdditionalLinesAndLabels(final GuiGraphicsExtractor graphics, final int left, final int width, final int bottom) {
        float tps = (float)TimeUtil.MILLISECONDS_PER_SECOND / this.msptSupplier.get();
        this.extractStringWithShade(graphics, String.format(Locale.ROOT, "%.1f TPS", tps), left + 1, bottom - 60 + 1);
    }

    @Override
    protected void extractAdditionalSampleBars(final GuiGraphicsExtractor graphics, final int bottom, final int currentX, final int sampleIndex) {
        long tickMethodTime = this.sampleStorage.get(sampleIndex, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
        int tickMethodHeight = this.getSampleHeight(tickMethodTime);
        graphics.fill(currentX, bottom - tickMethodHeight, currentX + 1, bottom, -6745839);
        long tasksTime = this.sampleStorage.get(sampleIndex, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
        int tasksHeight = this.getSampleHeight(tasksTime);
        graphics.fill(currentX, bottom - tickMethodHeight - tasksHeight, currentX + 1, bottom - tickMethodHeight, -4548257);
        long otherTime = this.sampleStorage.get(sampleIndex)
            - this.sampleStorage.get(sampleIndex, TpsDebugDimensions.IDLE.ordinal())
            - tickMethodTime
            - tasksTime;
        int otherHeight = this.getSampleHeight(otherTime);
        graphics.fill(currentX, bottom - otherHeight - tasksHeight - tickMethodHeight, currentX + 1, bottom - tasksHeight - tickMethodHeight, -10547572);
    }

    @Override
    protected long getValueForAggregation(final int sampleIndex) {
        return this.sampleStorage.get(sampleIndex) - this.sampleStorage.get(sampleIndex, TpsDebugDimensions.IDLE.ordinal());
    }

    @Override
    protected String toDisplayString(final double nanos) {
        return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(nanos)));
    }

    @Override
    protected int getSampleHeight(final double nanos) {
        return (int)Math.round(toMilliseconds(nanos) * 60.0 / this.msptSupplier.get().floatValue());
    }

    @Override
    protected int getSampleColor(final long nanos) {
        float mspt = this.msptSupplier.get();
        return this.getSampleColor(toMilliseconds(nanos), mspt, -16711936, mspt * 1.125, -256, mspt * 1.25, -65536);
    }

    private static double toMilliseconds(final double nanos) {
        return nanos / 1000000.0;
    }
}