package net.minecraft.client.sounds;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface FiniteAudioStream extends AudioStream {
   ByteBuffer readAll() throws IOException;
}
