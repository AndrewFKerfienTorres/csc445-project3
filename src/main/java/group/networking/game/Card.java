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

    /*
     *  2-10 are valued just as the number suggests.
     *  J, Q, and K are worth the same as 10.
     */

    private void setValue(){
        if(
                rank.equalsIgnoreCase("j") ||
                        rank.equalsIgnoreCase("q") ||
                        rank.equalsIgnoreCase("k")
        ){
            value = 10;
        }else{
            try{
                value = Integer.parseInt(rank);
            } catch (NumberFormatException e){
                value = 0;
            }
        }
    }

    // not useful except for the ace

    public void setHigh(){}
    public void setLow(){};

}
