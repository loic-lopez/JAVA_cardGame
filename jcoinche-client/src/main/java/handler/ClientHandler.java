package handler;

import Protobuf.MsgProtocol;
import client.Client;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.fusesource.jansi.AnsiConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

public class ClientHandler extends SimpleChannelInboundHandler<MsgProtocol.Response> {

    static private Channel channel;
    static private String username;
    private static Queue<MsgProtocol.Response> queue = new ConcurrentLinkedQueue<>();
    private MsgProtocol.Response resp;

    static public void channelInterrupt() {
        MsgProtocol.Request.Builder req =
                MsgProtocol.Request.newBuilder()
                        .setUsername(ClientHandler.username).setType(MsgProtocol.Request.Type.DISCONNECT);

        // Send request
        channel.writeAndFlush(req.build());
    }

    public void registerServer(String Username) {
        MsgProtocol.Request.Builder req =
                MsgProtocol.Request.newBuilder()
                        .setUsername(Username).setType(MsgProtocol.Request.Type.REGISTER);

        // Send request
        channel.writeAndFlush(req.build());
        username = Username;
    }

    public void sendRequest(String Username, MsgProtocol.Request.Type type, List<String> param) {

        MsgProtocol.Request.Builder req =
                MsgProtocol.Request.newBuilder()
                        .setUsername(Username)
                        .setType(type);

        if (param != null)
            req.addAllCommand(param);

        // Send request
        channel.writeAndFlush(req);
    }

    public MsgProtocol.Response.Type getResponse() {

        resp = queue.poll();
        if (resp != null) {
            switch (resp.getType()) {
                case SERVER_SHUTDOWN: {
                    this.printConsoleBuffered();
                    System.out.println(ansi().fg(YELLOW).a(resp.getResponseMsg()).reset());
                    return resp.getType();
                }
                case GAME_ALREADY_STARTED: {
                    this.printConsoleBuffered();
                    System.out.println(ansi().fg(RED).a(resp.getResponseMsg()).reset());
                    return resp.getType();
                }
                case GAME_ACTION: {
                    this.printConsoleBuffered();
                    System.out.println(ansi().fg(BLUE).a(resp.getResponseMsg()).reset());
                    return resp.getType();

                }
                case HAND: {
                    ListIterator<MsgProtocol.Card> cards = resp.getCardsInHandList().listIterator();
                    System.out.println(ansi().fg(BLUE).a("\n" + resp.getResponseMsg()).reset());
                    while (cards.hasNext())
                    {
                        MsgProtocol.Card card = cards.next();
                        System.out.print(ansi().fg(YELLOW).a(card.getColor() + ": ").reset());
                        System.out.println(ansi().fg(GREEN).a(card.getCardName()).reset());
                    }
                    return resp.getType();
                }
                case BOARD: {
                    ListIterator<MsgProtocol.CardInBoard> cards = resp.getCardInBoardList().listIterator();
                    System.out.println(ansi().fg(BLUE).a("\n" + resp.getResponseMsg()).reset());
                    while (cards.hasNext())
                    {
                        MsgProtocol.CardInBoard cardInBoard = cards.next();
                        System.out.print(ansi().fg(YELLOW).a(cardInBoard.getPlayerName() + ", " + cardInBoard.getColor() + ": ").reset());
                        System.out.println(ansi().fg(GREEN).a(cardInBoard.getCardName()).reset());
                    }
                    return resp.getType();
                }
                case HELP: {
                    ListIterator<MsgProtocol.HelpCommand> helpCommandListIterator = resp.getHelpList().listIterator();
                    System.out.println(ansi().fg(BLUE).a("\n" + resp.getResponseMsg()).reset());
                    while (helpCommandListIterator.hasNext())
                    {
                        MsgProtocol.HelpCommand helpCommand = helpCommandListIterator.next();
                        System.out.print(ansi().fg(GREEN).a(helpCommand.getName() + ": ").reset());
                        System.out.println(ansi().fg(YELLOW).a(helpCommand.getDescription()).reset());
                    }
                    return resp.getType();
                }
                case SUCCESS: {
                    this.printConsoleBuffered();
                    System.out.println(ansi().fg(GREEN).a(resp.getResponseMsg()).reset());
                    return resp.getType();
                }
                case FAILED:
                    System.out.println(ansi().fg(RED).a(resp.getResponseMsg()).reset());
                    return resp.getType();
                case SUCCESS_REGISTER:
                    this.printConsoleBuffered();
                    System.out.println(ansi().fg(MAGENTA).a(resp.getResponseMsg()).reset());
                    return resp.getType();
                case DISCONNECT:
                    System.out.println(ansi().fg(CYAN).a(resp.getResponseMsg()).reset());
                    return resp.getType();
                case USERNAME_ALREADY_TAKEN:
                    System.out.println(ansi().fg(YELLOW).a(resp.getResponseMsg()).reset());
                    return resp.getType();
            }
        }

        return MsgProtocol.Response.Type.REFRESH;
    }

    public void waitUserEntry(Client client, BufferedReader buffer) {
        String entry;
        boolean isPrinted = false;

        AnsiConsole.systemInstall();
        mainLoop:
        while (true) {
            try {
                MsgProtocol.Response.Type type = getResponse();
                switch (type) {
                    case GAME_ALREADY_STARTED:
                    case DISCONNECT:
                    case SERVER_SHUTDOWN: {
                        break mainLoop;
                    }
                    case USERNAME_ALREADY_TAKEN: {
                        AnsiConsole.systemUninstall();
                        client.getUserInfo();
                        username = client.getUsername();
                        this.registerServer(username);
                        AnsiConsole.systemInstall();
                        break;
                    }
                    default:
                        break;
                }
                if (type != MsgProtocol.Response.Type.REFRESH)
                    this.printConsole();
                if (buffer.ready()) {
                    entry = buffer.readLine();
                    if (entry.length() > 0) {
                        parseEntry(entry, username, type);
                    }
                    this.printConsole();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        AnsiConsole.systemUninstall();
    }

    private void parseEntry(String entry, String Username, MsgProtocol.Response.Type receivedType) {

        if (receivedType == MsgProtocol.Response.Type.USERNAME_ALREADY_TAKEN)
            return;

        List<String> com = new ArrayList<>(Arrays.asList(entry.split(" ", entry.length())));
        com.removeAll(Collections.singleton(null));
        com.removeAll(Collections.singleton(""));
        ListIterator<String> iterator = com.listIterator();
        while (iterator.hasNext())
            iterator.set(iterator.next().toLowerCase());

        String command = com.get(0);
        com.remove(0);
        switch (command) {
            case "play": {
                sendRequest(Username, MsgProtocol.Request.Type.PLAY_A_CARD, com);
                break;
            }
            case "bet": {
                sendRequest(Username, MsgProtocol.Request.Type.BET, com);
                break;
            }
            case "see": {
                sendRequest(Username, MsgProtocol.Request.Type.SEE, com);
                break;
            }
            case "disconnect": {
                sendRequest(Username, MsgProtocol.Request.Type.DISCONNECT, com);
                break;
            }
            case "board": {
                sendRequest(Username, MsgProtocol.Request.Type.BOARD, com);
                break;
            }
            default:
                sendRequest(Username, MsgProtocol.Request.Type.HELP, com);
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MsgProtocol.Response msg) throws Exception {
        queue.add(msg);
    }

    private void printConsole() {
        System.out.print(ansi().fg(YELLOW).a("Jcoinche -> ").reset());
        System.out.flush();
    }

    private void printConsoleBuffered() {
        System.out.print(ansi().fg(YELLOW).a("Jcoinche -> ").reset());
    }


}
