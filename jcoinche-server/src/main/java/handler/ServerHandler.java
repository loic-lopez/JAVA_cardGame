package handler;

import Protobuf.MsgProtocol;
import gameCore.Board;
import gameCore.cards.Card;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static Protobuf.MsgProtocol.Request.Type.*;

public class ServerHandler extends SimpleChannelInboundHandler<MsgProtocol.Request> {
    static private ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    static private Board board = new Board();

    static public void channelInterrupt() {
        if (channels.isEmpty())
            return;
        MsgProtocol.Response.Builder response = MsgProtocol.Response.newBuilder();

        response.setResponseMsg("Server shutting down!");
        response.setType(MsgProtocol.Response.Type.SERVER_SHUTDOWN);
        channels.writeAndFlush(response);
    }

    private List<MsgProtocol.HelpCommand> buildHelpCommands()
    {
        List<MsgProtocol.HelpCommand> commands = new ArrayList<>();

        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("help")
                .setDescription("Display this help.")
                .build());
        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("bet")
                .setDescription("Used to make a bet at the begin of a game: bet [COLOR] [BET_VALUE]")
                .build());
        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("bet pass")
                .setDescription("Used to pass your bet. (Be careful if you pass your bet you will not be able to make a bet anymore.)")
                .build());
        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("play")
                .setDescription("Used to play a card: play [COLOR] [TYPE_OF_CARD] (example play pique king).")
                .build());
        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("board")
                .setDescription("see played cards.")
                .build());
        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("see")
                .setDescription("see cards in your hand.")
                .build());
        commands.add(MsgProtocol.HelpCommand.newBuilder()
                .setName("disconnect")
                .setDescription("Disconnect from the server.")
                .build());

        return commands;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MsgProtocol.Request msg) throws Exception {
        if (msg.getCommandList() == null)
            return;

        MsgProtocol.Response.Builder response = MsgProtocol.Response.newBuilder();
        List<String> command = new ArrayList<>(msg.getCommandList());

        if (msg.getType() == MsgProtocol.Request.Type.DISCONNECT) {
            response.setResponseMsg("See you later!")
                    .setType(MsgProtocol.Response.Type.DISCONNECT);
            board.removePlayer(msg.getUsername());
            ctx.channel().writeAndFlush(response);
        }
        else if (msg.getType() == MsgProtocol.Request.Type.HELP)
        {
            response.setResponseMsg("Available commands: ")
                    .setType(MsgProtocol.Response.Type.HELP);
            response.addAllHelp(this.buildHelpCommands());
            ctx.channel().writeAndFlush(response);
        }
        else if (board.getNbrOfPlayers() < Board.MAX_PLAYERS) { //todo: remettre à 4
            if (msg.getType() == REGISTER) {
                if (!board.newPlayer(msg.getUsername())) {
                    response.setResponseMsg("Username already taken!")
                            .setType(MsgProtocol.Response.Type.USERNAME_ALREADY_TAKEN);
                } else
                    response.setResponseMsg("You have been registred. Welcome.")
                            .setType(MsgProtocol.Response.Type.SUCCESS_REGISTER);
                ctx.channel().writeAndFlush(response);
                if (board.getNbrOfPlayers() >= Board.MAX_PLAYERS) { //todo: remettre à 4
                    board.createGameInstance();
                    response.setResponseMsg("Let the game begins!")
                            .setType(MsgProtocol.Response.Type.SUCCESS);
                    channels.writeAndFlush(response);
                }
            } else {
                response.setResponseMsg("Not enough player.")
                        .setType(MsgProtocol.Response.Type.FAILED);
                ctx.channel().writeAndFlush(response);
            }
        } else {
            if (msg.getType() == REGISTER)
            {
                response.setResponseMsg("Game is already launched.")
                        .setType(MsgProtocol.Response.Type.GAME_ALREADY_STARTED);
                ctx.channel().writeAndFlush(response);
            }
            else if (msg.getType() == SEE)
            {
                response.setType(MsgProtocol.Response.Type.HAND);
                response.setResponseMsg("Your hands contains : ");
                response.addAllCardsInHand(board.getPlayersCardsInHand(msg.getUsername()));
                ctx.channel().writeAndFlush(response);
            }
            else if (msg.getType() == BOARD)
            {
                response.setType(MsgProtocol.Response.Type.BOARD);
                response.setResponseMsg("Board contains : ");
                response.addAllCardInBoard(board.getCardsInBoard());
                ctx.channel().writeAndFlush(response);
            }
            else if (checkTurn(msg)) {
                switch (msg.getType()) {
                    case PLAY_A_CARD: {
                        if (command.isEmpty() || command.size() < 2)
                        {
                            response.setResponseMsg("Play cannot be empty. USAGE: bet [COLOR] [BET_VALUE]");
                            response.setType(MsgProtocol.Response.Type.FAILED);
                            ctx.channel().writeAndFlush(response);
                            break;
                        }
                        response = board.playCard(msg.getUsername(), command.get(0), command.get(1));
                        if (response.getType() == MsgProtocol.Response.Type.FAILED)
                            ctx.channel().writeAndFlush(response);
                        else {
                            channels.writeAndFlush(response);
                            response = board.passTurn(msg.getUsername());
                            channels.writeAndFlush(response);
                        }
                        if (board.getNbrPlayedCards() == 4) {
                            String winner = board.whoWin();
                            channels.writeAndFlush(board.clearBoard(winner));
                        }
                        if (board.getRemainningCards() == 0) {
                            channels.writeAndFlush(board.endGame());
                        }
                        break;
                    }

                    case BET: {
                        if (board.getBetStatus() == Board.BetStatus.BET_CLOSED) {
                            response.setResponseMsg("Bet are closed!");
                            response.setType(MsgProtocol.Response.Type.FAILED);
                            ctx.channel().writeAndFlush(response);
                            break;
                        }
                        if (!board.canBet(msg.getUsername())) {
                            response.setResponseMsg("You've already passed.");
                            response.setType(MsgProtocol.Response.Type.FAILED);
                            ctx.channel().writeAndFlush(response);
                            break;
                        }

                        if (command.isEmpty() || command.size() < 2)
                        {
                            if (!(command.size() == 1 && command.get(0).toLowerCase().equals("pass")))
                            {
                                response.setResponseMsg("Bet cannot be empty. USAGE: play [COLOR] [TYPE_OF_CARD] (example play pique king).");
                                response.setType(MsgProtocol.Response.Type.FAILED);
                                ctx.channel().writeAndFlush(response);
                                break;
                            }
                        }

                        String[] colors = Card.getColors();
                        boolean found = false;
                        String colorReceived = command.get(0).toUpperCase();

                        if (colorReceived.toLowerCase().equals("pass")) {
                            response.setResponseMsg("User " + msg.getUsername() + " passed!");
                            response.setType(MsgProtocol.Response.Type.GAME_ACTION);
                            channels.writeAndFlush(response);
                            board.passBet(msg.getUsername());
                            response = board.passTurn(msg.getUsername());
                            channels.writeAndFlush(response);
                            if (board.getInBet() == 1) {
                                response = board.switchToPlay();
                                channels.writeAndFlush(response);
                            }
                            break;
                        }
                        for (String color : colors) {
                            if (Objects.equals(color, colorReceived)) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {

                            if (!command.get(1).matches("^\\d*$"))
                            {
                                response.setResponseMsg("You've to bet a positive number.");
                                response.setType(MsgProtocol.Response.Type.FAILED);
                                ctx.channel().writeAndFlush(response);
                                break;
                            }

                            if ((board.playerMakeABet(msg.getUsername(),
                                    Card.Color.valueOf(colorReceived),
                                    Integer.parseInt(command.get(1)))) == Board.BetStatus.BET_LOW) {
                                response.setResponseMsg("Your bet is not enough.");
                                response.setType(MsgProtocol.Response.Type.FAILED);
                                ctx.channel().writeAndFlush(response);
                            } else {
                                response.setResponseMsg("New bet from : " + msg.getUsername() +
                                        " -> " + Integer.parseInt(command.get(1)) + " on color -> " + colorReceived + ".");
                                response.setType(MsgProtocol.Response.Type.GAME_ACTION);
                                channels.writeAndFlush(response);
                                response = board.passTurn(msg.getUsername());
                                channels.writeAndFlush(response);
                            }
                        } else {
                            response.setResponseMsg("Invalid card color.");
                            response.setType(MsgProtocol.Response.Type.FAILED);
                            ctx.channel().writeAndFlush(response.build());
                        }

                        break;
                    }
                    default:
                        System.out.println("message failed");
                        break;
                }
            } else {
                response.setResponseMsg("Please wait your turn.");
                response.setType(MsgProtocol.Response.Type.FAILED);
                ctx.channel().writeAndFlush(response);
            }

        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private boolean checkTurn(MsgProtocol.Request msg) {
        if (!board.canPlay(msg.getUsername()))
            return false;
        return true;
    }
}
