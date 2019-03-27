package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;
import org.json.JSONObject;

import java.util.*;

public class StandardEmotionsController implements EmotionsController{

	private static class Emotion{

		/** Emotion name. **/
		public String name;

		/** Array of pairs [feature index, force multiplier]. **/
		public int[][] influences;

		/** Current emotion strength. **/
		public int strength = 0;

		/** Chance of getting into this emotion state. **/
		public float chance = 1;

		public Emotion(String name, int[][] influences) {
			this.name = name;
			this.influences = influences;
		}

		public Emotion(Emotion copy) {
			this.name = copy.name;
			this.influences = new int[copy.influences.length][2];
			for (int i=0; i<influences.length; i++) {
				influences[i][0] = copy.influences[i][0];
				influences[i][1] = copy.influences[i][1];
			}
		}

		public String toString(){
			String print = name + ", chance = " + chance + ", strength = " + strength + "\n";
			for(int i=0; i<influences.length; i++)
				print += "[feature: " + CharacterFeatures.getFeatureName(influences[i][0]) + ", force=" + influences[i][1] + "\n";

			return print;
		}
	}

	private static final Emotion[] STANDARD_EMOTIONS = {
			new Emotion("happiness", new int[][]{{0,  1}, {1,  1}, {4,  2}, {7,  1}}),
			new Emotion("sorrow",    new int[][]{{2, -1}, {3, -2}, {4, -2}}),
			new Emotion("fun",       new int[][]{{1,  2}, {3,  2}, {4,  1}}),
			new Emotion("anger",     new int[][]{{0, -2}, {1,  2}, {2,  1}, {3,  2}, {4,  -1}, {7, -2}}),
			new Emotion("confusion", new int[][]{{1, -1}, {2, -1}, {5, -1}}),
			new Emotion("affection", new int[][]{{1,  2}, {3,  1}, {7,  1}})
	};

	private Emotion[] emotions = Arrays.copyOf(STANDARD_EMOTIONS, STANDARD_EMOTIONS.length);
	private Emotion currentEmotion = null;

	StandardEmotionsController() {
		normalize();
		reset();
	}

	private UpdateHandler onUpdate = null;

	public void setUpdater(UpdateHandler handler){
		onUpdate = handler;
	}

	private void tryInform(){
		if (onUpdate != null) onUpdate.onUpdate();
	}

	public void reset() {
		currentEmotion = null;
		tryInform();
	}
	
	public String getCurrentEmotionName() {
		return currentEmotion != null ? currentEmotion.name : null;
	}

	public void raiseEmotion(String emotionName) {
		raiseEmotion(emotionName, 1);
	}

	public void raiseEmotion(String emotionName, int value) {
		if (currentEmotion != null){
			currentEmotion.strength -= value;
			if (currentEmotion.strength <= 0)
				currentEmotion = null;
			else return;
		}
		for (Emotion emotion : emotions){
			if (emotion.name.equals(emotionName)){
				currentEmotion = emotion;
				emotion.strength = value;
				tryInform();
				return;
			}
		}
		Main.log("No emotion by name: " + emotionName);
	}

	public void raiseRandomEmotion(){
		if (currentEmotion != null){
			currentEmotion.strength += new Random().nextInt(2) - 1;
			if (currentEmotion.strength <= 0){
				currentEmotion = null;
			} else {
				return;
			}
		}

		float chance = (float) Math.random();
		for (Emotion emotion : emotions){
			if (emotion.chance > chance){
				currentEmotion = emotion;
				emotion.strength = 1 + new Random().nextInt(2);
				tryInform();
				return;
			}
			chance -= emotion.chance;
		}
	}

	public List<String> getEmotionsList(){
		List<String> res = new ArrayList<>();
		for (Emotion e : emotions)
			res.add(e.name);
		return res;
	}

	public boolean phraseMatches(Phrase phrase){
		Set<String> allowedEmotions = phrase.getTag("emotion");
		if (allowedEmotions == null || allowedEmotions.size() == 0){
			return true;
		}
		if (currentEmotion != null){
			return allowedEmotions.contains(currentEmotion.name);
		}
		return false;
	}

	public CharacterController construct(CharacterController target) {
		if (currentEmotion == null) return target;

		CharacterController New = target.copy();
		for (int i = 0; i < currentEmotion.influences.length; i++) {
			int index =  currentEmotion.influences[i][0], multiplier = currentEmotion.influences[i][1];
			New.setValue(currentEmotion.influences[i][0], target.getValue(index) + currentEmotion.strength * multiplier);
		}

		return New;
	}

	public void setFromJSON(JSONObject json) {
		if (json == null || json.keySet().size() == 0) return;

		List<Emotion> newEmotions = new ArrayList<>();
		for (String emotionName : json.keySet()) {
			if (!(json.get(emotionName) instanceof JSONObject)) continue;

			JSONObject obj = json.getJSONObject(emotionName);
			List<int[]> influencesList = new ArrayList<>();
			for (String feature : obj.keySet()) {
				int index = CharacterFeatures.getFeatureIndex(feature);
				if (index < 0) continue;
				try {
					int force = obj.getInt(feature);
					influencesList.add(new int[]{index, force});
				} catch (Exception e){ }
			}
			if (influencesList.size() > 0) {
				int[][] influences = new int[influencesList.size()][];
				for (int i = 0; i < influencesList.size(); i++)
					influences[i] = influencesList.get(i);

				Emotion emotion = new Emotion(emotionName, influences);
				if (obj.has("chance")) emotion.chance = (float) obj.getDouble("chance");
				newEmotions.add(emotion);
			} else {
				for (int i=0; i<emotions.length; i++)
					if (emotions[i].name.equals(emotionName)){
						Emotion emotion = new Emotion(emotions[i]);
						if (obj.has("chance")) emotion.chance = (float) obj.getDouble("chance");
						newEmotions.add(emotion);
						break;
					}
			}
		}
		emotions = newEmotions.toArray(new Emotion[newEmotions.size()]);
		normalize();
		reset();
	}

	public JSONObject toJSON() {
		return new JSONObject();
	}

	private void normalize(){
		float sum = 0;
		for(Emotion emotion : emotions)
			sum += emotion.chance;
		if (sum < 1) return;

		for(Emotion emotion : emotions)
			emotion.chance /= sum;
	}

	public String getDefaultSpriteType() {
		if (currentEmotion == null) return null;

		switch(currentEmotion.name){
			case "happiness": {
				if (currentEmotion.strength > 1)
					return "HAPPY";
				else
					return "SMILE";
			}
			case "sorrow" : {
				if (currentEmotion.strength > 1)
					return "DESPAIR";
				else
					return "SAD";
			}
			case "fun": {
				if (currentEmotion.strength > 1)
					return "LAUGH";
				else
					return "SMILE";
			}
			case "anger": {
				if (currentEmotion.strength > 1)
					return "ANGRY";
				else
					return "SERIOUS";
			}
			case "confusion": {
				return "SHOCKED";
			}
			case "affection": {
				return "LOVE";
			}
		}

		return null;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for (Emotion emotion : emotions){
			sb.append(emotion.name + ", chance=" + emotion.chance + "\n");
			for (int[] influence : emotion.influences){
				sb.append(" "+CharacterFeatures.getFeatureName(influence[0]));
				sb.append(" " + influence[1] + "\n");
			}
		}
		return sb.toString();
	}
}
