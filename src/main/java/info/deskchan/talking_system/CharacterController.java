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

    /** Get sum of controllers. **/
    CharacterController add(CharacterController other);

    /** Clone controller. **/
    CharacterController copy();

    void setFromJSON(JSONObject jsonObject);

    JSONObject toJSON();

    String getDefaultSpriteType();
}
