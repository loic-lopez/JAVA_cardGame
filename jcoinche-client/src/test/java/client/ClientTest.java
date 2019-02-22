package client;

import Protobuf.MsgProtocol;
import gameCore.Board;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import server.Server;

import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientTest {
    private static Client client = null;
    private static Server server = null;
    private static Thread serverThread = null;
    private static boolean initialized = false;
    private static boolean shutdown = false;
    private static final String username = "UnitBot";


    @Before
    public void launchTestClient() throws Exception {
        if (initialized)
            return;

        initialized = true;
        client = new Client();
        server = new Server();
        serverThread = new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        serverThread.start();
        client.setUsername(username);
        client.setUnitTesting(true);
        Thread.sleep(1000);
    }

    @Test
    public void test1_start() throws Exception {
        client.launchConnection();
        assertTrue(client.getChannel().isRegistered());
        assertTrue(client.getChannel().isWritable());
        assertTrue(client.getChannel().isActive());
        assertTrue(client.getChannel().isOpen());
    }

    @Test
    public void test2_writeOnServerAndVerifyResponse() {
        int it = 0;

        while (it < Board.MAX_PLAYERS)
        {
            if (it + 1 == Board.MAX_PLAYERS)
                break;
            client.getClientHandler().registerServer(username + it);
            assertTrue(this.getResponse() == MsgProtocol.Response.Type.SUCCESS_REGISTER);
            it++;
        }

        client.getClientHandler().registerServer(username + '0');
        assertTrue(this.getResponse() == MsgProtocol.Response.Type.USERNAME_ALREADY_TAKEN);
        client.getClientHandler().registerServer(username + it + 1);
        assertTrue(this.getResponse() == MsgProtocol.Response.Type.SUCCESS_REGISTER);
        assertTrue(this.getResponse() == MsgProtocol.Response.Type.SUCCESS);
    }

    @Test
    public void test3_registerWithAlreadyUsernameTaken() {
        client.getClientHandler().registerServer(username);

        assertTrue(this.getResponse() == MsgProtocol.Response.Type.GAME_ALREADY_STARTED);
    }

    private MsgProtocol.Response.Type getResponse() {
        MsgProtocol.Response.Type type = client.getClientHandler().getResponse();

        while (type == MsgProtocol.Response.Type.REFRESH)
            type = client.getClientHandler().getResponse();

        return type;
    }

    // this method is used to shutdown unit testing
    @Test
    public void xShutDown() {
        shutdown = true;
    }

    @After
    public void down() {
        if (!shutdown)
            return;

        client.getWorkerGroup().shutdownGracefully();
        server.getServerChannel().channel().close();
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}