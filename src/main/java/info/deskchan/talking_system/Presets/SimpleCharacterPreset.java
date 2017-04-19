package info.deskchan.talking_system.presets;

import info.deskchan.talking_system.CharacterDefinite;
import info.deskchan.talking_system.CharacterPreset;
import info.deskchan.talking_system.EmotionsController;

public class SimpleCharacterPreset extends CharacterPreset {
	public CharacterDefinite getCharacter(EmotionsController emo) {
		return emo.Construct(MainCharacter);
	}
}
