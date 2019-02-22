package gameCore;

import Protobuf.MsgProtocol;
import gameCore.cards.Card;

import java.util.*;

public class Board {
    public final static int MAX_PLAYERS = 4; //todo: change to 4
    private Map<String, Player> players;
    private Map<String, Status> waitingPlayers;
    private Vector<String> playerNames;
    private Map<String, Card> playedCards;
    public Card.Color colorBet;
    private String currentColor;
    private int bestBet;
    private String bestBetPlayer;
    private BetStatus betStatus;
    private String designedPlayer;
    private int inBet;
    private int id_team;
    public Board() {
        playerNames = new Vector<>();
        players = new HashMap<>();
        waitingPlayers = new HashMap<>();
        playedCards = new HashMap<>();
        bestBet = 0;
        betStatus = BetStatus.BET;
        designedPlayer = null;
        inBet = MAX_PLAYERS;
        id_team = 1;
    }

    public boolean newPlayer(String userName) {

        for (Map.Entry<String, Status> waitingPlayer : waitingPlayers.entrySet())
            if (waitingPlayer.getKey().compareTo(userName) == 0)
                return false;

        this.waitingPlayers.put(userName, Status.WAITING);
        this.playerNames.add(userName);
        return true;
    }

    public List<MsgProtocol.Card> getPlayersCardsInHand(final String playerName)
    {
        List<MsgProtocol.Card> cards = new ArrayList<>();

        Vector<Card> playerCards = this.players.get(playerName).getCards();
        for (Card playerCard : playerCards)
        {
            cards.add(MsgProtocol.Card.newBuilder()
                    .setCardName(playerCard.getCardName())
                    .setColor(playerCard.getColor().name())
                    .build());
        }

        return cards;
    }

    public int getNbrOfPlayers() {
        return this.waitingPlayers.size();
    }

    public void removePlayer(String playerName) {
        this.playerNames.remove(playerName);
        this.waitingPlayers.remove(playerName);
        this.players.remove(playerName);
    }

    public BetStatus playerMakeABet(final String playerName, final Card.Color color, final int bet) {
        if (bet > this.bestBet && this.betStatus == BetStatus.BET) {
            bestBet = bet;
            bestBetPlayer = playerName;
            colorBet = color;
            this.players.get(playerName).makeABet(bet);
            return BetStatus.BET;
        } else if (bet <= this.bestBet && this.betStatus == BetStatus.BET)
            return BetStatus.BET_LOW;
        else
            return BetStatus.BET_CLOSED;
    }

    //TODO: revoir le shuffle des couleurs de cartes lors de la distribution
    public void createGameInstance() throws Exception {
        Vector<Card> deck = new Vector<>();
        Vector<String> cardsName = Card.getCardsName();
        Vector<String> colors = new Vector<>(Arrays.asList(Card.getColors()));

        Collections.shuffle(colors);
        for (String color : colors) {
            Card.Color colorToPush = Card.Color.valueOf(color);

            for (String cardName : cardsName)
                deck.add(new Card(colorToPush, cardName, "BasicCard"));
        }
        Collections.shuffle(deck);

        for (String listPlayer : playerNames) {
            this.players.put(listPlayer, new Player());
            if (this.id_team == 1) {
                this.players.get(listPlayer).setTeam(1);
                this.id_team = 2;
            }
            else {
                this.players.get(listPlayer).setTeam(2);
                this.id_team = 1;
            }
            this.waitingPlayers.put(listPlayer, Status.INGAME);
        }

        this.players.get(playerNames.elementAt(0)).setHisturn(true);
        designedPlayer = playerNames.elementAt(0);

        int index = 0;
        for (Card entry : deck) {
                if (index < this.players.size())
                    this.players.get(this.playerNames.elementAt(index)).addCard(entry);
                else {
                    index = 0;
                    this.players.get(this.playerNames.elementAt(index)).addCard(entry);
                }
                index++;
        }
    }

    public void passBet(final String playerName) {
        players.get(playerName).setAbleBet(false);
        inBet--;
    }

    public boolean canBet(final String playerName) {
        if (players.get(playerName) == null)
            return false;
        return players.get(playerName).isAbleBet();
    }

    public MsgProtocol.Response.Builder passTurn(final String playerName) {
        MsgProtocol.Response.Builder turn = MsgProtocol.Response.newBuilder();

        players.get(playerName).setHisturn(false);
        int nextPlayerIndex = playerNames.indexOf(playerName) + 1;
        if (nextPlayerIndex >= MAX_PLAYERS)
            nextPlayerIndex = 0;
        players.get(playerNames.elementAt(nextPlayerIndex)).setHisturn(true);

        if (!players.get(playerNames.elementAt(nextPlayerIndex)).isAbleBet() && betStatus == BetStatus.BET)
            passTurn(playerNames.elementAt(nextPlayerIndex));

        turn.setResponseMsg("The game continue, we're waiting " + playerNames.elementAt(nextPlayerIndex) + " !");
        turn.setType(MsgProtocol.Response.Type.GAME_ACTION);
        return turn;
    }

    public boolean canPlay(final String playerName) {
        if (players.get(playerName) == null)
            return false;
        return players.get(playerName).isHisTurn();
    }

    public int getInBet() {
        return inBet;
    }

    public MsgProtocol.Response.Builder switchToPlay() {
        MsgProtocol.Response.Builder winner = MsgProtocol.Response.newBuilder();

        betStatus = BetStatus.BET_CLOSED;
        winner.setResponseMsg("The game start ! The best bet is " + bestBet + " on color " + colorBet + " took by " + bestBetPlayer + " ! Good Luck everyone");


        for (Map.Entry<String, Player> playerEntry : players.entrySet())
            try {
                playerEntry.getValue().regenerateCards(colorBet);
            } catch (Exception e) {
                e.printStackTrace();
            }

        winner.setType(MsgProtocol.Response.Type.GAME_ACTION);
        return winner;
    }

    public BetStatus getBetStatus() {
        return betStatus;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public enum Status {
        WAITING,
        INGAME;
    }

    public enum BetStatus {
        BET,
        BET_LOW,
        BET_CLOSED
    }

    public MsgProtocol.Response.Builder playCard(final String Username, final String color, final String type) {
        Vector<Card>    cards = players.get(Username).getCards();
        MsgProtocol.Response.Builder played = MsgProtocol.Response.newBuilder();

        played.setResponseMsg("this card " + color + " " + type + " doesn't belong to you.");
        played.setType(MsgProtocol.Response.Type.FAILED);
        if (playedCards.size() == 0) {
            for (Card card: cards) {
                if (card.getCardName().toUpperCase().equals(type.toUpperCase()) && card.getColor().toString().equals(color.toUpperCase())) {
                    played.setResponseMsg("Player " + Username + " played " + color + " " + type + "\nThe color for this turn is " + color);
                    played.setType(MsgProtocol.Response.Type.GAME_ACTION);
                    currentColor = color.toUpperCase();
                    playedCards.put(Username, card);
                    players.get(Username).cards.remove(card);
                    break;
                }
            }
        } else {
            for (Card card: cards) {
                if (card.getCardName().toUpperCase().equals(type.toUpperCase()) && card.getColor().toString().equals(color.toUpperCase())) {
                    if (!color.toUpperCase().equals(currentColor)) {
                        if (checkCards(Username, currentColor)) {
                            played.setResponseMsg("You can't play " + color + " because you still have some " + currentColor);
                            played.setType(MsgProtocol.Response.Type.FAILED);
                            break;
                        }
                        if (!color.toUpperCase().equals(colorBet.toString())) {
                            if (checkCards(Username, colorBet.toString())) {
                                played.setResponseMsg("You can't play " + color + " because you still have some " + colorBet.toString().toLowerCase());
                                played.setType(MsgProtocol.Response.Type.FAILED);
                                break;
                            }
                        }
                    }
                    played.setResponseMsg("Player " + Username + " played " + color + " " + type);
                    played.setType(MsgProtocol.Response.Type.GAME_ACTION);
                    playedCards.put(Username, card);
                    players.get(Username).cards.remove(card);
                    break;
                }
            }
        }
        return played;
    }

    public int  getNbrPlayedCards() { return playedCards.size(); }

    public int  getRemainningCards() {
        int     index = playerNames.size() - 1;
        return players.get(playerNames.elementAt(index)).getCards().size();
    }

    public String   whoWin() throws Exception{
        String      winner = playerNames.elementAt(0);
        Card        carteMaitresse = playedCards.get(playerNames.elementAt(0));

        for (int playerCartesIndex = 1; playerCartesIndex < playerNames.size() ; playerCartesIndex++) {
            if (playedCards.get(playerNames.elementAt(playerCartesIndex)).getTypeOfCard().equals(carteMaitresse.getTypeOfCard()))
            {
                if (playedCards.get(playerNames.elementAt(playerCartesIndex)).getCardValue() > carteMaitresse.getCardValue())
                {
                    carteMaitresse = playedCards.get(playerNames.elementAt(playerCartesIndex));
                    winner = playerNames.elementAt(playerCartesIndex);
                }
            }
            else
            {
                if (playedCards.get(playerNames.elementAt(playerCartesIndex)).getTypeOfCard().equals("Atout")
                        && carteMaitresse.getTypeOfCard().equals("NonAtout"))
                {
                    carteMaitresse = playedCards.get(playerNames.elementAt(playerCartesIndex));
                    winner = playerNames.elementAt(playerCartesIndex);
                }
            }

        }

        return winner;
    }

    public MsgProtocol.Response.Builder clearBoard(final String Username) {
        MsgProtocol.Response.Builder act = MsgProtocol.Response.newBuilder();
        Vector<Card>    discards;

        for (Map.Entry<String, Card> cardEntry: playedCards.entrySet()) {
            players.get(Username).discards.add(cardEntry.getValue());
        }
        currentColor = "";
        playedCards.clear();
        act.setResponseMsg(Username + " won the turn !");
        act.setType(MsgProtocol.Response.Type.GAME_ACTION);
        discards = players.get(Username).getDiscards();
        for (Card card: discards)
            System.out.println(card.getCardName() + " " + card.getColor().toString());
        return act;
    }

    public boolean  checkCards(String Username, String color) {
        Vector<Card>    cards = players.get(Username).getCards();

        for (Card card: cards) {
            if (color.toUpperCase().equals(card.getColor().toString()))
                return true;
        }
        return false;
    }
    public MsgProtocol.Response.Builder endGame() throws Exception {
        MsgProtocol.Response.Builder winner = MsgProtocol.Response.newBuilder();
        int scoresT1 = 0;
        int scoresT2 = 0;
        Vector<String>  winners = new Vector<>();
        Vector<Card>    discarded = new Vector<>();

        for (String player: playerNames) {
            if (players.get(player).getTeam() == 1) {
                discarded.addAll(players.get(player).getDiscards());
            }
        }
        for (Card card : discarded) {
            scoresT1 += card.getCardValue();
        }
        discarded.clear();
        for (String player: playerNames) {
            if (players.get(player).getTeam() == 2) {
                discarded.addAll(players.get(player).getDiscards());
            }
        }
        for (Card card : discarded) {
            scoresT2 += card.getCardValue();
        }
        if (scoresT1 > scoresT2) {
            for (String player: playerNames) {
                if (players.get(player).getTeam() == 1) {
                    winners.add(player);
                }
            }
            winner.setResponseMsg(winners.elementAt(0) + " and " + winners.elementAt(1) + " won the game !");
            winner.setType(MsgProtocol.Response.Type.SUCCESS);
        }
        else if (scoresT1 < scoresT2) {
            for (String player: playerNames) {
                if (players.get(player).getTeam() == 2) {
                    winners.add(player);
                }
            }
            winner.setResponseMsg(winners.elementAt(0) + " and " + winners.elementAt(1) + " won the game !");
            winner.setType(MsgProtocol.Response.Type.SUCCESS);
        }
        else {
            winner.setResponseMsg("Both teams got the sames scores! there is no winner congratulations !");
            winner.setType(MsgProtocol.Response.Type.SUCCESS);
        }

        return winner;
    }

    public List<MsgProtocol.CardInBoard> getCardsInBoard()
    {
        List<MsgProtocol.CardInBoard> cards = new ArrayList<>();

        for (Map.Entry<String, Card> entry : playedCards.entrySet())
        {
            cards.add(MsgProtocol.CardInBoard.newBuilder()
                    .setPlayerName(entry.getKey())
                    .setCardName(entry.getValue().getCardName())
                    .setColor(entry.getValue().getColor().name())
                    .build());
        }
        return cards;
    }

}
