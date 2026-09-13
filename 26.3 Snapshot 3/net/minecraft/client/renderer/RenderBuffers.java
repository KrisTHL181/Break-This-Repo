package net.minecraft.client.renderer;

public class RenderBuffers implements AutoCloseable {
   private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
   private final SectionBufferBuilderPool sectionBufferPool;
   private final StagedVertexBuffer stagedVertexBuffer;

   public RenderBuffers(final int maxSectionBuilders) {
      this.sectionBufferPool = SectionBufferBuilderPool.allocate(maxSectionBuilders);
      this.stagedVertexBuffer = new StagedVertexBuffer(() -> "Shared Buffer", 4194304);
   }

   public SectionBufferBuilderPack fixedBufferPack() {
      return this.fixedBufferPack;
   }

   public SectionBufferBuilderPool sectionBufferPool() {
      return this.sectionBufferPool;
   }

   public StagedVertexBuffer stagedVertexBuffer() {
      return this.stagedVertexBuffer;
   }

   public void endFrame() {
      this.stagedVertexBuffer.endFrame();
   }

   @Override
   public void close() {
      this.sectionBufferPool.close();
      this.stagedVertexBuffer.close();
   }
}
