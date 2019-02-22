package initializer;

import Protobuf.MsgProtocol;
import handler.ServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    //    @Override
    public void initChannel(SocketChannel sock) throws Exception {
        sock.pipeline().addLast(new ProtobufVarint32FrameDecoder());
        sock.pipeline().addLast(new ProtobufDecoder(MsgProtocol.Request.getDefaultInstance()));
        sock.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
        sock.pipeline().addLast(new ProtobufEncoder());
        sock.pipeline().addLast(new ServerHandler());
    }

}
