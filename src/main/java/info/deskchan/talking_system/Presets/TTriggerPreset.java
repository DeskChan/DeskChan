package info.deskchan.talking_system.presets;

import info.deskchan.talking_system.CharacterDefinite;
import info.deskchan.talking_system.CharacterPreset;
import info.deskchan.talking_system.CharacterSystem;
import org.json.JSONObject;

public abstract class TTriggerPreset extends CharacterPreset {
	CharacterDefinite Left, Right;
	int targetCharacterValue;
	int characterValueMiddle;
	
	public TTriggerPreset() {
		Left = new CharacterDefinite();
		Right = new CharacterDefinite();
		targetCharacterValue = 5;
		characterValueMiddle = 0;
	}
	
	@Override
	public void fillFromJSON(JSONObject json) {
		super.fillFromJSON(json);
		
		Left = new CharacterDefinite(json.getJSONObject("left"));
		Right = new CharacterDefinite(json.getJSONObject("right"));
		String s = json.getString("targetFeature");
		int in = CharacterSystem.getFeatureIndex(s);
		targetCharacterValue = (in < 0 || in >= CharacterSystem.getFeatureCount() ? 5 : in);
		characterValueMiddle = json.getInt("featureBarrier");
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("left", Left.toJSON());
		json.put("right", Right.toJSON());
		json.put("targetFeature", CharacterSystem.getFeatureName(targetCharacterValue));
		json.put("featureBarrier", characterValueMiddle);
		return json;
	}
}
