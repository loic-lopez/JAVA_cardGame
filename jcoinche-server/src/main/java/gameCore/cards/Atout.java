package gameCore.cards;

public class Atout {

    public static class Valet extends AbstractCard {
        public Valet() {
            value = 20;
        }
    }

    public static class Nine extends AbstractCard {
        public Nine() {
            value = 14;
        }
    }

    public static class As extends AbstractCard {
        public As() {
            value = 11;
        }
    }

    public static class Ten extends AbstractCard {
        public Ten() {
            value = 10;
        }

    }

    public static class King extends AbstractCard {
        public King() {
            value = 4;
        }

    }

    public static class Queen extends AbstractCard {
        public Queen() {
            value = 3;
        }

    }

    public static class Height extends AbstractCard {
        public Height() {
            value = 0;
        }
    }

    public static class Seven extends AbstractCard {
        public Seven() {
            value = 0;
        }
    }
}
