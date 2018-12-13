package info.deskchan.talking_system.speech_exchange;

import info.deskchan.talking_system.CharacterController;
import info.deskchan.talking_system.StandardCharacterController;

public class DialogLine {

    float[] character;
    Reaction reaction;

    public DialogLine(){
        character = CharacterController.asArray(new StandardCharacterController());
        reaction = new Reaction();
    }
    public DialogLine(Reaction reaction, CharacterController character, Author author){
        this(reaction, CharacterController.asArray(character), author);
    }
    public DialogLine(Reaction reaction, float[] character, Author author){
        this.character = character;
        this.reaction = reaction;
        this.author = author;
    }

    public Reaction getReaction(){ return reaction; }

    public void setReaction(Reaction reaction){ this.reaction = reaction; }

    public enum Author {
        CHARACTER, USER;
        public Author next(){
            return (this == CHARACTER ? USER : CHARACTER);
        }
    }
    Author author = Author.USER;

    @Override
    public boolean equals(Object other){
        if (!(other instanceof DialogLine)) return false;
        DialogLine line = (DialogLine) other;

        if (line.author != author) return false;

        for (int i = 0; i < character.length; i++)
            if (Math.abs(character[i] - line.character[i]) > 0.001) return false;

        return reaction != null ? reaction.equals(line.reaction) : line.reaction == null;
    }
}
