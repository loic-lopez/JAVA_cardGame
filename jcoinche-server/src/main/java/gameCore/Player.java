package gameCore;

import gameCore.cards.Card;

import java.util.Vector;

public class Player {
    Vector<Card> cards;
    Vector<Card> discards;
    int bet;
    boolean ableBet;
    boolean histurn;
    int     team;

    public Player() {
        cards = new Vector<>();
        discards = new Vector<>();
        ableBet = true;
        histurn = false;
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void makeABet(int bet) {
        this.bet = bet;
    }

    public boolean isAbleBet() {
        return ableBet;
    }

    public void setAbleBet(boolean ableBet) {
        this.ableBet = ableBet;
    }

    public void setHisturn(boolean histurn) {
        this.histurn = histurn;
    }

    public boolean isHisTurn() {
        return histurn;
    }

    public Vector<Card> getCards() {
        return cards;
    }

    public Vector<Card> getDiscards() { return discards; }


    public void regenerateCards(Card.Color mainColor) throws Exception
    {
        int index = 0;
        Vector<Card> newCards = new Vector<>();

        System.out.println(cards.size());
        while (index < cards.size())
        {
            Card card = cards.elementAt(index);
            if (cards.elementAt(index).getColor().toString() == mainColor.name()) {
                newCards.add(new Card(mainColor, card.getCardName(), "atout"));
            }
            else {
            newCards.add(new Card(card.getColor(), card.getCardName(), "nonatout"));
        }
            index++;
        }
        cards.clear();
        cards = newCards;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

}
