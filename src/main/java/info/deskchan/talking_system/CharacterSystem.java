package info.deskchan.talking_system;

import java.awt.*;

public class CharacterSystem {
	static String[][] features = {
			///       покорность   стеснение       уверенность   доминантность
			{"obedience", "modesty", "confidence", "primacy"},
			///       равнодушие   пассивность     активность    назойливость
			{"disregard", "passivity", "activity", "annoyance"},
			///       уныние       пессимизм       оптимизм      радость
			{"gloominess", "pessimism", "optimism", "sunshine"},
			///       агрессия     пренебрежение   доброта       гиперопека
			{"aggression", "neglect", "kindness", "patronize"},
			///       пошлость     непристойность  утонченность  знатность
			{"dirty", "indecency", "elegance", "nobility"},
			///       ненависть    неприятие       симпатия      обожание
			{"hate", "rejection", "sympathy", "adoration"}
	};
	static int featureCount = 6;
	
	public static int getFeatureCount() {
		return featureCount;
	}
	
	public static Point getInfluenceFromFeatureName(String featureName) {
		for (int i = 0; i < featureCount; i++) {
			for (int j = 0; j < 4; j++) {
				if (featureName.equals(features[i][j])) {
					return new Point(i, j - 2);
				}
			}
		}
		return null;
	}
	
	public static int getFeatureIndex(String featureName) {
		for (int i = 0; i < featureCount; i++) {
			for (int j = 0; j < 4; j++) {
				if (featureName.equals(features[i][j])) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public static String getFeatureName(int index, int position) {
		return features[index][position];
	}
	
	public static String getFeatureName(int index) {
		return features[index][2];
	}
}
