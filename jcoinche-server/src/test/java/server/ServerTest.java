package server;

import Protobuf.MsgProtocol;
import handler.ServerHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {
    private static final String username = "UnitBot";
    private static Server server = null;
    private static boolean initialized = false;
    private static EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() {
        if (!initialized) {
            initialized = true;
            server = new Server();
            server.setUnitTesting(true);

            embeddedChannel = new EmbeddedChannel();
            embeddedChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
            embeddedChannel.pipeline().addLast(new ProtobufDecoder(MsgProtocol.Request.getDefaultInstance()));
            embeddedChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
            embeddedChannel.pipeline().addLast(new ProtobufEncoder());
            embeddedChannel.pipeline().addLast(new ServerHandler());
        }
    }

    @Test
    public void test1_register() {
        MsgProtocol.Request.Builder request = MsgProtocol.Request.newBuilder()
                .setType(MsgProtocol.Request.Type.REGISTER)
                .setUsername(username);
        assertTrue(embeddedChannel.writeInbound(request));

        request = embeddedChannel.readInbound();
        assertTrue(request.getType() == MsgProtocol.Request.Type.REGISTER);
        assertTrue(request.getUsername().equals(username));
    }

    @Test
    public void test2_disconnect() {
        MsgProtocol.Request.Builder request = MsgProtocol.Request.newBuilder()
                .setType(MsgProtocol.Request.Type.DISCONNECT)
                .setUsername(username);
        assertTrue(embeddedChannel.writeInbound(request));

        request = embeddedChannel.readInbound();
        assertTrue(request.getType() == MsgProtocol.Request.Type.DISCONNECT);
        assertTrue(request.getUsername().equals(username));
    }
}