package info.deskchan.talking_system.presets;

import info.deskchan.talking_system.CharacterDefinite;
import info.deskchan.talking_system.EmotionsController;
import org.json.JSONObject;

import java.util.Random;

public class TTriggerEmotionalCharacterPreset extends TTriggerPreset {
	String targetEmotion;
	
	public TTriggerEmotionalCharacterPreset() {
		targetEmotion = "confusion";
	}
	
	public CharacterDefinite getCharacter(EmotionsController emo) {
		if (!targetEmotion.equals(emo.getEmotionName())) {
			return MainCharacter;
		}
		float mid = (10f - characterValueMiddle + MainCharacter.getValue(targetCharacterValue)) / 21f;
		if (new Random().nextFloat() > mid) {
			return emo.Construct(MainCharacter.Add(Left));
		}
		return emo.Construct(MainCharacter.Add(Right));
	}
	
	@Override
	public void fillFromJSON(JSONObject json) {
		super.fillFromJSON(json);
		targetEmotion = json.getString("targetEmotion");
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("targetEmotion", targetEmotion);
		return json;
	}
}
