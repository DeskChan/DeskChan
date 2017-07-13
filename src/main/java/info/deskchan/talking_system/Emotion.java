package info.deskchan.talking_system;

import java.awt.*;

public class Emotion {
	static int emotionsCount = 5;
	static Emotion[] Emotions = {
			new Emotion("happiness", new int[][]{{2, 2}, {3, 1}}),   /// радость
			new Emotion("sorrow", new int[][]{{2, -2}, {1, -3}}),    /// печаль
			new Emotion("fun", new int[][]{{1, 2}, {4, -1}}),        /// веселье
			new Emotion("anger", new int[][]{{3, -2}, {0, 1}}),      /// злость
			new Emotion("confusion", new int[][]{{0, -1}, {3, 1}}),  /// смущение
			new Emotion("affection", new int[][]{{1,  1}, {3, 1}})   /// нежность
	};
	
	public static Emotion getEmotion(int index) {
		return Emotions[index];
	}
	
	public static int getEmotionsCount() {
		return emotionsCount;
	}
	
	public static Point getInfluenceFromFeatureName(String featureName) {
		for (int i = 0; i < emotionsCount; i++) {
			if (featureName.equals(Emotions[i].name)) {
				return new Point(i, 1);
			}
		}
		return null;
	}
	
	public String name;
	public int dependentFeatures[][];
	
	public Emotion(String emotion_name, int dependent[][]) {
		name = emotion_name;
		dependentFeatures = dependent;
	}
}
