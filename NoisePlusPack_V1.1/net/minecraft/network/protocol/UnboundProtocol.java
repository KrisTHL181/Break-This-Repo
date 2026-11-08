package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.function.Function;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;

public interface UnboundProtocol<T extends PacketListener, B extends ByteBuf, C> extends ProtocolInfo.DetailsProvider {
   ProtocolInfo<T> bind(Function<ByteBuf, B> var1, C var2);
}
