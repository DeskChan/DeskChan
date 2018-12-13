package info.deskchan.talking_system;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public interface EmotionsController {

	String getCurrentEmotionName();

	void reset();

	void raiseRandomEmotion();

	void raiseEmotion(String emotionName);

	void raiseEmotion(String emotionName, int value);

	boolean phraseMatches(Phrase phrase);

	CharacterController construct(CharacterController target);

	interface UpdateHandler{
		void onUpdate();
	}

	void setUpdater(UpdateHandler handler);

	String getDefaultSpriteType();

	void setFromJSON(JSONObject json);

	JSONObject toJSON();

	List<String> getEmotionsList();

}
