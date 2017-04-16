package info.deskchan.talking_system.presets;

import info.deskchan.talking_system.CharacterDefinite;
import info.deskchan.talking_system.CharacterPreset;
import info.deskchan.talking_system.EmotionsController;
import org.json.JSONObject;

public class EmotionalCharacterPreset extends CharacterPreset {
	String targetEmotion;
	CharacterDefinite Sub;
	
	public EmotionalCharacterPreset() {
		targetEmotion = "confusion";
		Sub = new CharacterDefinite();
	}
	
	public CharacterDefinite getCharacter(EmotionsController emo) {
		if (emo.getEmotionName() == targetEmotion) {
			return emo.Construct(Sub);
		}
		return emo.Construct(MainCharacter);
	}
	
	@Override
	public void fillFromJSON(JSONObject json) {
		super.fillFromJSON(json);
		Sub = new CharacterDefinite(json.getJSONObject("sub"));
		targetEmotion = json.getString("targetEmotion");
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("sub", Sub.toJSON());
		json.put("targetEmotion", targetEmotion);
		return json;
	}
}

