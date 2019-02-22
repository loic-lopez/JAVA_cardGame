package gameCore.cards;

import Protobuf.MsgProtocol;
import gameCore.Board;
import gameCore.Player;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BoardAndCardsTest {
    private static final String username = "UnitBot";
    static private Board board = new Board();

    @Test
    public void test1_initGame() throws Exception {
        assertTrue(board.newPlayer(username));
        assertTrue(board.newPlayer(username + '2'));
        assertTrue(board.newPlayer(username + '3'));
        assertTrue(board.newPlayer(username + '4'));
        assertFalse(board.newPlayer(username));
        board.createGameInstance();
    }

    @Test
    public void test2_instanciateCardAndVerifyInstance() throws Exception {
        Map<String, Player> playerMap = board.getPlayers();
        Vector<Card> cards = playerMap.get(username).getCards();

        assertTrue(playerMap.size() == 4);
        assertTrue(cards.size() == 8);

        String typeOfCard = cards.elementAt(0).getTypeOfCard();
        String cardName = cards.elementAt(0).getCardName();

        assertTrue(Class
                .forName("gameCore.cards." + typeOfCard + '$' + cardName)
                .isInstance(cards.elementAt(0).getInstance())
        );

        cards.elementAt(0).changeTypeOfCard("atout");

        typeOfCard = cards.elementAt(0).getTypeOfCard();
        cardName = cards.elementAt(0).getCardName();
        assertTrue(Class
                .forName("gameCore.cards." + typeOfCard + '$' + cardName)
                .isInstance(cards.elementAt(0).getInstance())
        );
    }

    @Test
    public void test3_fillHandResponse()
    {
        List<MsgProtocol.Card>  cards = board.getPlayersCardsInHand(username);

        MsgProtocol.Card it = cards.iterator().next();
        assertTrue(it.getCardName() != null);
        assertTrue(it.getColor() != null);
    }

    @After
    public void tearDown() throws Exception {
    }

}