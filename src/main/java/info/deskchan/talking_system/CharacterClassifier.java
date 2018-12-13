package info.deskchan.talking_system;

public interface CharacterClassifier {

    public CharacterRange getCharacterForPhrase(String text);

    public void add(Phrase phrase);

}
