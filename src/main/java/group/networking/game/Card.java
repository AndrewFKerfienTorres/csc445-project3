package group.networking.game;

public class Card {

    private int value;
    private String rank; // 2, 3, 4, 5, 6, 7, 8, 9, 10 J, Q, K
    private Suit suit;

    public Card(Suit suit, String rank){
        this.rank = rank;
        this.suit=suit;
        setValue();
    }

    public void setRank(String rank){
        this.rank = rank;
        setValue();
    }

    public void setSuit(Suit suit){
        this.suit = suit;
    }

    public Suit getSuit(){
        return suit;
    }

    public String getRank(){
        return rank;
    }

    public int getValue(){
        return value;
    }

    private void setValue(){
        if(
                rank.equalsIgnoreCase("j") ||
                        rank.equalsIgnoreCase("q") ||
                        rank.equalsIgnoreCase("k")
        ){
            value = 10;
        }else if (
                rank.replaceAll("\\D", "") == ""
        ){
            value = Integer.parseInt(rank);
        }else{
            value = 0;
        }
    }



}
