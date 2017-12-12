package info.deskchan.talking_system;

public class CharacterFeatures {
	static final String[] FEATURES = {
			"empathy", "impulsivity", "selfconfidence", "energy", "attitude", "experience", "manner", "relationship"
	};

	protected static final int BORDER = 4;
	protected static final int LENGTH = 1 + BORDER * 2;

	public static int getFeatureCount() {
		return FEATURES.length;
	}

	public static int getFeatureIndex(String featureName){
		for (int i = 0; i < getFeatureCount(); i++)
			if (featureName.equals(FEATURES[i])) return i;

		return -1;
	}
	
	public static String getFeatureName(int index) {
		return FEATURES[index];
	}
}
