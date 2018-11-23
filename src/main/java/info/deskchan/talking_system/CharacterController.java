package info.deskchan.talking_system;

import org.json.JSONObject;

public interface CharacterController {

    /** Get feature state by index. **/
    int getValue(int index);

    /** Get feature state by name. **/
    int getValue(String featureName);

    /** Set feature state by index. **/
    void setValue(int index, float values);

    /** Set feature state by index. **/
    void setValue(String featureName, float values);

    /** Move feature state by index. **/
    void moveValue(int index, float values);

    /** Move feature state by name. **/
    void moveValue(String featureName, float values);

    /** Set features from array. **/
    void setValues(float[] values);

    /** Get sum of controllers. **/
    CharacterController add(CharacterController other);

    /** Clone controller. **/
    CharacterController copy();

    boolean phraseMatches(Phrase phrase);

    void setFromJSON(JSONObject jsonObject);

    JSONObject toJSON();

    String getDefaultSpriteType();

    static float[] asArray(CharacterController controller){
        float[] ar = new float[CharacterRange.getFeatureCount()];
        for (int i = 0; i < CharacterRange.getFeatureCount(); i++){
            ar[i] = controller.getValue(i);
        }
        return ar;
    }
}
