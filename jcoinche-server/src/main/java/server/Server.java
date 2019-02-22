package server;

import handler.ServerHandler;
import initializer.ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Server {
    private int port = 8080;
    private ChannelFuture serverChannel;
    private boolean isUnitTesting = false;
    private EventLoopGroup clients;
    private EventLoopGroup serverGroup;
    private Server(int port) {
        this.port = port;
    }

    public Server() {

    }

    public static void main(String[] arg) throws Exception {
        int port;
        if (arg.length > 0) {
            port = Integer.parseInt(arg[0]);
            new Server(port).run();
        } else
            new Server().run();
    }

    public ChannelFuture getServerChannel() {
        return serverChannel;
    }

    public boolean isUnitTesting() {
        return isUnitTesting;
    }

    public void setUnitTesting(boolean unitTesting) {
        isUnitTesting = unitTesting;
    }

    public EventLoopGroup getClients() {
        return clients;
    }

    public void setClients(EventLoopGroup clients) {
        this.clients = clients;
    }

    public EventLoopGroup getServerGroup() {
        return serverGroup;
    }

    public void setServerGroup(EventLoopGroup serverGroup) {
        this.serverGroup = serverGroup;
    }

    public void run() throws Exception {
        clients = new NioEventLoopGroup();
        serverGroup = new NioEventLoopGroup(1);

        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(serverGroup, clients)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ServerChannelInitializer());
            serverChannel = server.bind(this.port).sync();

            Runtime.getRuntime().addShutdownHook(new Thread(ServerHandler::channelInterrupt));

            serverChannel.channel().closeFuture().sync();
        } finally {
            clients.shutdownGracefully();
            serverGroup.shutdownGracefully();
        }
    }
}