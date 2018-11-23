package info.deskchan.talking_system.speech_exchange;

import info.deskchan.talking_system.IntentList;

public class Reaction implements ICompatible {

    protected IExchangeable exchangeData;
    protected String emotion;

    protected static String NOTHING_INTENT = "NOTHING";

    public Reaction(){
        exchangeData = new IntentsData(NOTHING_INTENT);
        emotion = null;
    }
    public Reaction(IntentList exchangeData){
        this(exchangeData, null);
    }
    public Reaction(IntentList exchangeData, String emotion){
        if (exchangeData != null && exchangeData.size() > 0)
            this.exchangeData = new IntentsData(exchangeData);
        else
            this.exchangeData = new IntentsData(NOTHING_INTENT);
        this.emotion = emotion;
    }
    public Reaction(String exchangeData){
        this(exchangeData, null);
    }
    public Reaction(String exchangeData, String emotion){
        if (exchangeData != null && exchangeData.trim().length() > 0)
            this.exchangeData = new PhraseData(exchangeData);
        else
            this.exchangeData = new IntentsData(NOTHING_INTENT);
        this.emotion = emotion;
    }

    public Reaction(IExchangeable exchangeData, String emotion){
        this.exchangeData = exchangeData;
        this.emotion = emotion;
    }

    public IExchangeable getExchangeData(){ return exchangeData; }
    public String getEmotion() { return emotion; }

    @Override
    public double checkCompatibility(ICompatible other) {
        if (!(other instanceof Reaction))
            return 0;

        Reaction o = (Reaction) other;
        return (exchangeData.checkCompatibility(o.exchangeData) + (emotion.equals(o.emotion) ? 1 : 0)) / 2;
    }

    @Override
    public String toString() {
        return "Exchange Data: " + exchangeData + ", Emotion:" + (emotion != null ? emotion : " - ");
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Reaction)) return false;
        Reaction o = (Reaction) other;

        if (emotion == null) {
            if (o.emotion != null) return false;
        } else {
            if (!emotion.equals(o.emotion)) return false;
        }

        if (exchangeData == null) {
            return o.exchangeData == null;
        } else {
            return exchangeData.equals(o.exchangeData);
        }
    }
}