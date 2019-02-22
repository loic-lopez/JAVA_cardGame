package gameCore.cards;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Vector;

public class Card {
    private Object card;
    private String typeOfCard;
    private String cardName;
    private Color color;
    public Card(Color color, String cardName, String typeOfCard) throws Exception {
        this.color = color;
        this.cardName = cardName;
        this.typeOfCard = typeOfCard;
        this.instanciate();
    }

    static public Vector<String> getCardsName() {
        Vector<String> cardsName = new Vector<>();

        Class<?>[] instances = BasicCard.class.getClasses();
        for (Class<?> ins : instances)
            if (Character.isAlphabetic(ins.getSimpleName().codePointAt(0)))
                cardsName.add(ins.getSimpleName());

        return cardsName;
    }

    public static String[] getColors() {
        return Arrays.stream(Card.Color.class.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    public String getTypeOfCard() {
        return typeOfCard;
    }

    public String getCardName() {
        return cardName;
    }

    public Color getColor() {
        return color;
    }

    public void changeTypeOfCard(String newTypeOfCard) throws Exception {
        this.typeOfCard = newTypeOfCard;
        this.instanciate();
    }

    private String adaptCardName(String cardName) {
        String adaptedCardName = cardName;
        adaptedCardName = adaptedCardName.substring(0, 1).toUpperCase() + adaptedCardName.substring(1, cardName.length()).toLowerCase();

        Class<?>[] instances = Atout.class.getClasses();
        boolean found = false;
        for (Class<?> ins : instances) {
            if (adaptedCardName.compareTo(ins.getSimpleName()) == 0) {
                found = true;
                break;
            }
        }
        if (!found) {
            instances = NonAtout.class.getClasses();
            for (Class<?> ins : instances) {
                if (adaptedCardName.compareTo(ins.getSimpleName()) == 0) {
                    found = true;
                    break;
                }
            }

        }
        if (found)
            return adaptedCardName;
        else
            return null;
    }

    private String adaptTypeCard(String typeOfCard) throws Exception {
        String adaptedTypeOfCard = typeOfCard;

        if (typeOfCard.compareToIgnoreCase("basiccard") == 0)
            return "BasicCard";
        else if (typeOfCard.compareToIgnoreCase("atout") == 0)
            return "Atout";
        else if (typeOfCard.compareToIgnoreCase("nonatout") == 0)
            return "NonAtout";
        else
            return null;
    }

    public String instanciate() throws Exception {
        Class<?> instance;

        if ((this.typeOfCard = this.adaptTypeCard(this.typeOfCard)) == null)
            return "Wrong type of card.";
        if ((this.cardName = this.adaptCardName(this.cardName)) == null)
            return "Wrong card name.";

        try {
            instance = Class.forName("gameCore.cards." + this.typeOfCard + '$' + this.cardName);

            try {
                this.card = instance.newInstance();

            } catch (IllegalAccessException | InstantiationException e1) {
                e1.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "ok";
    }

    public int getCardValue() throws Exception{
        try {
            Method getValueMethod = this.card.getClass().getMethod("getValue");
            return (int) getValueMethod.invoke(this.card);
        } catch (NoSuchMethodException | IllegalAccessException  | InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public <Any> Any getInstance() {
        return (Any) this.card;
    }

    public enum Color {
        CLUB,
        HEART,
        PIQUE,
        TILE;
    }
}
