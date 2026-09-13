package net.minecraft.client.sounds;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import org.jspecify.annotations.Nullable;

public class JOrbisAudioStream implements FloatSampleSource {
   private static final int BUFSIZE = 8192;
   private static final int PAGEOUT_RECAPTURE = -1;
   private static final int PAGEOUT_NEED_MORE_DATA = 0;
   private static final int PAGEOUT_OK = 1;
   private static final int PACKETOUT_ERROR = -1;
   private static final int PACKETOUT_NEED_MORE_DATA = 0;
   private static final int PACKETOUT_OK = 1;
   private final SyncState syncState = new SyncState();
   private final Page page = new Page();
   private final StreamState streamState = new StreamState();
   private final Packet packet = new Packet();
   private final Info info = new Info();
   private final DspState dspState = new DspState();
   private final Block block = new Block(this.dspState);
   private final AudioFormat audioFormat;
   private final InputStream input;
   private long samplesWritten;
   private long totalSamplesInStream = Long.MAX_VALUE;

   public JOrbisAudioStream(final InputStream input) throws IOException {
      this.input = input;
      Comment comment = new Comment();
      Page firstPage = this.readPage();
      if (firstPage == null) {
         throw new IOException("Invalid Ogg file - can't find first page");
      }

      Packet firstPacket = this.readIdentificationPacket(firstPage);
      if (isError(this.info.synthesis_headerin(comment, firstPacket))) {
         throw new IOException("Invalid Ogg identification packet");
      }

      for (int headerPacketCount = 0; headerPacketCount < 2; headerPacketCount++) {
         firstPacket = this.readPacket();
         if (firstPacket == null) {
            throw new IOException("Unexpected end of Ogg stream");
         }

         if (isError(this.info.synthesis_headerin(comment, firstPacket))) {
            throw new IOException("Invalid Ogg header packet " + headerPacketCount);
         }
      }

      this.dspState.synthesis_init(this.info);
      this.block.init(this.dspState);
      this.audioFormat = new AudioFormat(this.info.rate, 16, this.info.channels, true, false);
   }

   private static boolean isError(final int value) {
      return value < 0;
   }

   @Override
   public AudioFormat getFormat() {
      return this.audioFormat;
   }

   private boolean readToBuffer() throws IOException {
      int offset = this.syncState.buffer(8192);
      byte[] buffer = this.syncState.data;
      int bytes = this.input.read(buffer, offset, 8192);
      if (bytes == -1) {
         return false;
      }

      this.syncState.wrote(bytes);
      return true;
   }

   private @Nullable Page readPage() throws IOException {
      while (true) {
         int pageOutResult = this.syncState.pageout(this.page);
         switch (pageOutResult) {
            case -1:
               throw new IOException("Corrupt or missing data in bitstream");
            case 0:
               if (this.readToBuffer()) {
                  break;
               }

               return null;
            case 1:
               if (this.page.eos() != 0) {
                  this.totalSamplesInStream = this.page.granulepos();
               }

               return this.page;
            default:
               throw new IllegalStateException("Unknown page decode result: " + pageOutResult);
         }
      }
   }

   private Packet readIdentificationPacket(final Page firstPage) throws IOException {
      this.streamState.init(firstPage.serialno());
      if (isError(this.streamState.pagein(firstPage))) {
         throw new IOException("Failed to parse page");
      } else {
         int result = this.streamState.packetout(this.packet);
         if (result != 1) {
            throw new IOException("Failed to read identification packet: " + result);
         } else {
            return this.packet;
         }
      }
   }

   private @Nullable Packet readPacket() throws IOException {
      while (true) {
         int packetOutResult = this.streamState.packetout(this.packet);
         switch (packetOutResult) {
            case -1:
               throw new IOException("Failed to parse packet");
            case 0:
               Page page = this.readPage();
               if (page == null) {
                  return null;
               }

               if (!isError(this.streamState.pagein(page))) {
                  break;
               }

               throw new IOException("Failed to parse page");
            case 1:
               return this.packet;
            default:
               throw new IllegalStateException("Unknown packet decode result: " + packetOutResult);
         }
      }
   }

   private long getSamplesToWrite(final int samples) {
      long samplesAfterWrite = this.samplesWritten + samples;
      long samplesToWrite;
      if (samplesAfterWrite > this.totalSamplesInStream) {
         samplesToWrite = this.totalSamplesInStream - this.samplesWritten;
         this.samplesWritten = this.totalSamplesInStream;
      } else {
         this.samplesWritten = samplesAfterWrite;
         samplesToWrite = samples;
      }

      return samplesToWrite;
   }

   @Override
   public boolean readChunk(final FloatConsumer consumer) throws IOException {
      float[][][] pcmSampleOutput = new float[1][][];
      int[] pcmOffsetOutput = new int[this.info.channels];
      Packet packet = this.readPacket();
      if (packet == null) {
         return false;
      }

      if (isError(this.block.synthesis(packet))) {
         throw new IOException("Can't decode audio packet");
      }

      this.dspState.synthesis_blockin(this.block);

      int samples;
      while ((samples = this.dspState.synthesis_pcmout(pcmSampleOutput, pcmOffsetOutput)) > 0) {
         float[][] channelSamples = pcmSampleOutput[0];
         long samplesToWrite = this.getSamplesToWrite(samples);
         switch (this.info.channels) {
            case 1:
               copyMono(channelSamples[0], pcmOffsetOutput[0], samplesToWrite, consumer);
               break;
            case 2:
               copyStereo(channelSamples[0], pcmOffsetOutput[0], channelSamples[1], pcmOffsetOutput[1], samplesToWrite, consumer);
               break;
            default:
               copyAnyChannels(channelSamples, this.info.channels, pcmOffsetOutput, samplesToWrite, consumer);
         }

         this.dspState.synthesis_read(samples);
      }

      return true;
   }

   private static void copyAnyChannels(final float[][] samples, final int channelCount, final int[] offsets, final long count, final FloatConsumer output) {
      for (int j = 0; j < count; j++) {
         for (int channel = 0; channel < channelCount; channel++) {
            int offset = offsets[channel];
            float val = samples[channel][offset + j];
            output.accept(val);
         }
      }
   }

   private static void copyMono(final float[] samples, final int offset, final long count, final FloatConsumer output) {
      for (int i = offset; i < offset + count; i++) {
         output.accept(samples[i]);
      }
   }

   private static void copyStereo(
      final float[] samples1, final int offset1, final float[] samples2, final int offset2, final long count, final FloatConsumer output
   ) {
      for (int i = 0; i < count; i++) {
         output.accept(samples1[offset1 + i]);
         output.accept(samples2[offset2 + i]);
      }
   }

   @Override
   public void close() throws IOException {
      this.input.close();
   }
}
