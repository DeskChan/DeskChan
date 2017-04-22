package info.deskchan.talking_system;

import java.awt.*;

public class Influence {
	public static float globalMultiplier;
	private int featureID;
	private float multiplier;
	
	public int getFeatureID() {
		return featureID;
	}
	
	public float getMultiplier() {
		return multiplier;
	}
	
	public static Influence CreateCharacterInfluence(String feature, float multiplier) {
		Point a = CharacterSystem.getInfluenceFromFeatureName(feature);
		if (a == null) {
			return null;
		}
		return new Influence(a.x, a.y * multiplier);
	}
	
	public static Influence CreateEmotionInfluence(String feature, float multiplier) {
		Point a = Emotion.getInfluenceFromFeatureName(feature);
		if (a == null) {
			return null;
		}
		return new Influence(a.x, a.y * multiplier);
	}
	
	private Influence(int feId, float mult) {
		featureID = feId;
		multiplier = mult;
	}
}

