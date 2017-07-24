package info.deskchan.talking_system;

import info.deskchan.core.ResponseListener;
import info.deskchan.core_utils.TextOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class EmotionsController {
	private int emotionValue;
	private int emotionIndex;
	private static int emotion_limit = 3;
	
	EmotionsController() {
		Reset();
		(new RaiseNewEmotion()).start();
	}
	
	class RaiseNewEmotion implements ResponseListener {
		
		@Override
		public void handle(String sender, Object data) {
			if (new Random().nextFloat() > 0.8) {
				if (emotionValue > 0) {
					emotionValue += (new Random().nextFloat() > 0.5 ? 1 : -1);
					if (emotionValue > emotion_limit) {
						emotionValue = emotion_limit;
					}
					if (emotionValue < 0) {
						emotionValue = 0;
					}
				} else {
					Generate();
				}
			}
			if (emotionIndex >= 0) {
				Main.sendToProxy("talk:request", "EMOTION");
			} else {
				Reset();
			}
			start();
		}
		
		void start() {
			Main.getPluginProxy().sendMessage("core-utils:notify-after-delay", TextOperations.toMap("delay: 100000"),this);
		}
		
	}
	
	void Reset() {
		emotionValue = 0;
		emotionIndex = -1;
	}
	
	public int getEmotionValue() {
		return emotionValue;
	}
	
	public String getEmotionName() {
		if (emotionIndex < 0) {
			return null;
		}
		return Emotion.getEmotion(emotionIndex).name;
	}
	
	public void setEmotion(int emotionIndex, int value) {
		if (value < 1) {
			Reset();
			return;
		}
		value = Math.min(value, emotion_limit);
		emotionValue = value;
		this.emotionIndex = emotionIndex;
		RaiseMessage();
	}
	
	void Generate() {
		int len = Emotion.getEmotionsCount();
		emotionIndex = (int) Math.floor(Math.random() * len);
		emotionValue = 1 + new Random().nextInt(emotion_limit);
		
		RaiseMessage();
	}
	public boolean isTagsMatch(HashMap<String,Object> tagsToMatch){
		for(HashMap.Entry<String,Object> entry : tagsToMatch.entrySet()){
			if(!entry.getKey().equals("emotion")) continue;
			List<String> arglist=(List<String>) entry.getValue();
			if (arglist.size()==0) continue;
			String curEmotion=getEmotionName();
			if(curEmotion==null) return false;
			for(int i=0;i<arglist.size();i++)
				if(arglist.get(i).equals(curEmotion)) return true;
			return false;
		}
		return true;
	}
	public CharacterDefinite Construct(CharacterDefinite target) {
		if (emotionValue == 0) {
			return target;
		}
		CharacterDefinite New = target.Clone();
		Emotion cur = Emotion.getEmotion(emotionIndex);
		for (int i = 0; i < cur.dependentFeatures.length; i++) {
			New.setValue(cur.dependentFeatures[i][0], target.getValue(cur.dependentFeatures[i][0]) + cur.dependentFeatures[i][1] * emotionValue);
		}
		return New;
	}
	
	public void applyInfluence(Influence influence) {
		if (influence == null) {
			return;
		}
		int mult = (int) Math.floor(influence.getMultiplier());
		if (mult > emotionValue) {
			emotionValue = mult;
			if (emotionValue > emotion_limit) {
				emotionValue = emotion_limit;
			}
			emotionIndex = influence.getFeatureID();
		}
		RaiseMessage();
	}
	
	private void RaiseMessage() {
		if (emotionValue == 0 || emotionIndex < 0) {
			return;
		}
		Main.sendToProxy("talk:emotion-changed", TextOperations.toMap("emotion: "+Emotion.getEmotion(emotionIndex).name));
	}
	
	public String getSpriteType() {
		if (emotionIndex == 0 && emotionValue < 2) {
			return "SMILE";
		}
		if (emotionIndex == 0 && emotionValue >= 2) {
			return "HAPPY";
		}
		
		if (emotionIndex == 1 && emotionValue < 1) {
			return "THOUGHTFUL";
		}
		if (emotionIndex == 1 && emotionValue >= 1
				&& emotionValue < 3) {
			return "SAD";
		}
		if (emotionIndex == 1 && emotionValue >= 3) {
			return "CRY";
		}
		
		if (emotionIndex == 2 && emotionValue < 2) {
			return "SMILE";
		}
		if (emotionIndex == 2 && emotionValue >= 2) {
			return "LAUGH";
		}
		
		if (emotionIndex == 3 && emotionValue < 1) {
			return "SERIOUS";
		}
		if (emotionIndex == 3 && emotionValue >= 1
				&& emotionValue < 3) {
			return "ANGRY";
		}
		if (emotionIndex == 3 && emotionValue >= 3) {
			return "RAGE";
		}
		
		if (emotionIndex == 4 && emotionValue >= 1
				&& emotionValue < 3) {
			return "SHOCKED";
		}
		if (emotionIndex == 4 && emotionValue >= 3) {
			return "SCARED";
		}
		return null;
	}
}
