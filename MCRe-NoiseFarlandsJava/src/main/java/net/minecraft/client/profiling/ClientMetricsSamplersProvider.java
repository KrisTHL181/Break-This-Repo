package net.minecraft.client.profiling;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsSamplerProvider;
import net.minecraft.util.profiling.metrics.profiling.ProfilerSamplerAdapter;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientMetricsSamplersProvider implements MetricsSamplerProvider {
    private final LevelRenderer levelRenderer;
    private final LevelExtractor levelExtractor;
    private final Set<MetricSampler> samplers = new ObjectOpenHashSet<>();
    private final ProfilerSamplerAdapter samplerFactory = new ProfilerSamplerAdapter();

    public ClientMetricsSamplersProvider(final LongSupplier wallTimeSource, final LevelRenderer levelRenderer, final LevelExtractor levelExtractor) {
        this.levelRenderer = levelRenderer;
        this.levelExtractor = levelExtractor;
        this.samplers.add(ServerMetricsSamplersProvider.tickTimeSampler(wallTimeSource));
        this.registerStaticSamplers();
    }

    private void registerStaticSamplers() {
        this.samplers.addAll(ServerMetricsSamplersProvider.runtimeIndependentSamplers());
        this.samplers.add(MetricSampler.createExtractSampler("totalChunks", MetricCategory.CHUNK_RENDERING, this.levelExtractor::totalSections));
        this.samplers.add(MetricSampler.createExtractSampler("renderedChunks", MetricCategory.CHUNK_RENDERING, this.levelExtractor::countRenderedSections));
        this.samplers.add(MetricSampler.createExtractSampler("lastViewDistance", MetricCategory.CHUNK_RENDERING, this.levelExtractor::lastViewDistance));
        SectionRenderDispatcher sectionRenderDispatcher = this.levelRenderer.sectionRenderDispatcher();
        if (sectionRenderDispatcher != null) {
            this.samplers
                .add(
                    MetricSampler.createExtractSampler(
                        "freeBufferCount", MetricCategory.CHUNK_RENDERING_DISPATCHING, sectionRenderDispatcher::getFreeBufferCount
                    )
                );
            this.samplers
                .add(
                    MetricSampler.createExtractSampler(
                        "compileQueueSize", MetricCategory.CHUNK_RENDERING_DISPATCHING, sectionRenderDispatcher::getCompileQueueSize
                    )
                );
        }

        this.samplers.add(MetricSampler.createExtractSampler("gpuUtilization", MetricCategory.GPU, Minecraft.getInstance()::getGpuUtilization));
    }

    @Override
    public Set<MetricSampler> samplers(final Supplier<ProfileCollector> singleTickProfiler) {
        this.samplers.addAll(this.samplerFactory.newSamplersFoundInProfiler(singleTickProfiler));
        return this.samplers;
    }
}