package info.deskchan.talking_system;

import org.json.JSONObject;

import java.util.Map;

public interface EmotionsController {

	String getCurrentEmotionName();

	void reset();

	void raiseEmotion(String emotionName);

	void raiseEmotion(String emotionName, int value);

	boolean tagsMatch(Map<String,Object> tags);

	CharacterController construct(CharacterController target);

	interface UpdateHandler{
		void onUpdate();
	}

	void setUpdater(UpdateHandler handler);

	String getDefaultSpriteType();

	void setFromJSON(JSONObject json);

	JSONObject toJSON();

}
