package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;

public interface SimpleUnboundProtocol<T extends PacketListener, B extends ByteBuf> extends ProtocolInfo.DetailsProvider {
   ProtocolInfo<T> bind(Function<ByteBuf, B> var1);
}
