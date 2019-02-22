package client;

import handler.ClientHandler;
import initializer.ClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private String Username = null;
    private String Host;
    private int Port = 8080;
    private ClientHandler clientHandler;
    private boolean isUnitTesting = false;
    private Channel channel;
    private EventLoopGroup workerGroup;

    public Client() {
        Host = "127.0.0.1";
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();

        if (args.length == 1)
            client.setHost(args[0]);
        if (args.length == 2) {
            client.setHost(args[0]);
            client.setPort(Integer.parseInt(args[1]));
        }
        client.getUserInfo();
        client.launchConnection();
    }

    public Channel getChannel() {
        return channel;
    }

    public void setUnitTesting(boolean unitTesting) {
        isUnitTesting = unitTesting;
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    public void getUserInfo() {

        String s = "";
        System.out.print("Enter your username: ");
        try {
            while (s.length() == 0) {
                s = in.readLine();
            }
            this.Username = s;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public void launchConnection() throws IOException {
        workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ClientInitializer());

            channel = bootstrap.connect(Host, Port).sync().channel();
            System.out.print("Connection started\n");

            clientHandler = channel.pipeline().get(ClientHandler.class);

            //Runtime.getRuntime().addShutdownHook(new Thread(ClientHandler::channelInterrupt));

            if (!isUnitTesting) {
                clientHandler.registerServer(this.Username);
                clientHandler.waitUserEntry(this, in);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (!isUnitTesting) {
                workerGroup.shutdownGracefully();
                in.close();
            }
        }
    }

    private void setHost(String host) {
        this.Host = host;
    }

    private void setPort(int port) {
        Port = port;
    }
}