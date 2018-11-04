package info.deskchan.talking_system;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Set;

/** This class stores diapasons of features. **/
public class StandardCharacterController extends CharacterFeatures implements CharacterController {

	/** [feature][left speed, left border, current value, right border, right speed] **/
	protected float[][] value;
	private static final float[] DEFAULT = { 1.0f, -BORDER, 0.0f, BORDER, 1.0f };
	public StandardCharacterController() {
		value = new float[getFeatureCount()][];
		for (int i = 0; i < getFeatureCount(); i++) {
			value[i] = Arrays.copyOf(DEFAULT, 5);
		}
	}

	public StandardCharacterController(float[] values) {
		this();
		for (int i = 0; i < getFeatureCount() && i < values.length; i++)
			setValue(i, values[i]);
	}

	private static final int[][] POSITIONS = {
			{ 2 }, { 1, 3 }, { 1, 2, 3 }, { 0, 1, 3, 4 }, { 0, 1, 2, 3, 4 }
	};
	public StandardCharacterController(float[][] values) {
		this();
		for (int i = 0; i < getFeatureCount() && i < values.length; i++)
			setValue(i, values[i]);
	}
	
	public void setFromJSON(JSONObject json) {
		if (json == null) return;

		for (int i = 0; i < getFeatureCount(); i++) {
			try {
				Object obj = json.get(getFeatureName(i));
				if (obj instanceof JSONObject){
					JSONObject object = (JSONObject) obj;
					setValue(i, 0, (float) object.optDouble("leftspeed"));
					setValue(i, 1, (float) object.optDouble("left"));
					setValue(i, 2, (float) object.optDouble("value"));
					setValue(i, 3, (float) object.optDouble("right"));
					setValue(i, 4, (float) object.optDouble("rightspeed"));
				} else if (obj instanceof Number){
					setValue(i, 2, ((Number) obj).floatValue());
				}
			} catch (Exception o) {
				value[i] = Arrays.copyOf(DEFAULT, 5);
			}
		}
	}
	
	private float checkToBorders(float val, float left, float right) {
		if (val < left)
			return left;
		else if (val > right)
			return right;

		return val;
	}

	private float checkToBorders(float val) {
		return checkToBorders(val, -BORDER, BORDER);
	}

	public void setValue(int index, String values) {
		if (index < 0 || index >= getFeatureCount() || values == null || values.isEmpty())
			return;

		String[] ar = values.split(";");
		float[] far = new float[ar.length];
		for (int i = 0; i < ar.length; i++) {
			try {
				far[i] = Float.valueOf(ar[i].trim());
			} catch (Exception e) {
				far[i] = 0.0f;
			}
		}

		setValue(index, far);
	}

	public void setValue(int index, float[] values){
		if (index < 0 || index >= getFeatureCount()) return;

		int[] positions = POSITIONS[ Math.min(values.length, 5) - 1 ];
		for (int j = 0; j < positions.length; j++)
			setValue(index, positions[j], values[j]);

	}

	public void setValue(int index, float val) {
		setValue(index, 2, val);
	}

	private void setValue(int index, int pos, float val) {
		if (index < 0 || index >= getFeatureCount() || val != val) return;

		switch (pos){
			case 0: case 4:
				value[index][pos] = Math.abs(val);
				break;
			case 1: case 3:
				value[index][pos] = checkToBorders(val);
				break;
			case 2:
				value[index][pos] = checkToBorders(val, value[index][1], value[index][3]);
				break;
		}
	}
	
	public int getValue(int index) {
		return (int) ( value[index][2] > 0 ? Math.floor(value[index][2]) : Math.ceil(value[index][2]) );
	}

	public void setValue(String featureName, float val) {
		if (featureName == null) return;

		int index = getFeatureIndex(featureName);
		setValue(index, 2, val);
	}

	public void moveValue(String featureName, float val) {
		if (featureName == null) return;

		int index = getFeatureIndex(featureName);
		moveValue(index, val);
	}

	public void moveValue(int index, float val) {
		if (val > 0){
			float nv = value[index][2] + val * value[index][4];
			System.out.println(nv);
			value[index][2] = Math.min(nv, value[index][3]);
		} else {
			float nv = value[index][2] + val * value[index][0];
			System.out.println(nv);
			value[index][2] = Math.max(nv, value[index][1]);
		}
	}


	public int getValue(String featureName) {
		if (featureName == null) return 0;

		int index = getFeatureIndex(featureName);
		return (int) ( value[index][2] > 0 ? Math.floor(value[index][2]) : Math.ceil(value[index][2]) );
	}

	public boolean phraseMatches(Phrase phrase){
		for (int i = 0, l = CharacterFeatures.getFeatureCount(); i < l; i++)
			if (!phrase.character.range[i].match(getValue(i)))
				return false;

		return true;
	}

	public CharacterController mix(CharacterController other, float percent) {
		CharacterController cd = copy();
		for (int i = 0; i < getFeatureCount(); i++) {
			setValue(i, getValue(i) * percent + other.getValue(i) * (1 - percent));
		}
		return cd;
	}
	
	public String getValueString(int index) {
		StringBuilder string = new StringBuilder();
		for (int i=0; i < value[index].length; i++) {
			string.append(value[index][i]);
			string.append(";");
		}
		string.setLength(string.length()-1);
		return string.toString();
	}
	
	public void prettyPrint() {
		for (int i = 0; i < getFeatureCount(); i++)
			System.out.println(getFeatureName(i) + ": <-" + value[i][0] + " (" + value[i][1] + " ; " + value[i][2] + " ; " + value[i][3] + ") " + value[i][4] + "->");
	}

	public StandardCharacterController copy() {
		return new StandardCharacterController(value);
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() != this.getClass()) return false;

		CharacterController _other = (CharacterController) other;
		for (int i = 0; i < getFeatureCount(); i++) {
			if (getValue(i) != _other.getValue(i)) return false;
		}
		return true;
	}
	
	public JSONObject toJSON() {
		JSONObject save = new JSONObject();
		for (int i = 0; i < getFeatureCount(); i++) {
			JSONObject feature = new JSONObject();
			if (DEFAULT[0] != value[i][0]) feature.put("leftspeed", value[i][0]);
			if (DEFAULT[1] != value[i][1]) feature.put("left", value[i][1]);
			if (DEFAULT[2] != value[i][2]) feature.put("value", value[i][2]);
			if (DEFAULT[3] != value[i][3]) feature.put("right", value[i][3]);
			if (DEFAULT[4] != value[i][4]) feature.put("rightspeed", value[i][4]);
			if (feature.length() > 0) save.put(getFeatureName(i), feature);
		}
		return save;
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
	
	public CharacterController add(CharacterController other) {
		CharacterController cd = this.copy();
		for (int i = 0; i < getFeatureCount(); i++) {
			setValue(i, value[i][2] + other.getValue(i));
		}
		return cd;
	}

	public String getDefaultSpriteType() {
		String feature = null;
		boolean negative = false;
		for (int i = 0; i < getFeatureCount(); i++) {
			if (Math.abs(value[i][2]) >= BORDER / 2){
				if (feature == null) {
					feature = getFeatureName(i);
					negative = (value[i][2] < 0);
				} else break;
			}
		}
		if (feature == null) return null;

		switch (feature) {
			case "empathy":
				return (negative ? "ANGRY" : "HAPPY");
			case "impulsivity":
				return (negative ? "SERIOUS" : "EXCITEMENT");
			case "selfconfidence":
			case "experience":
				return (negative ? "CONFUSED" : "CONFIDENT");
			case "energy":
				return (negative ? "TIRED" : "EXCITEMENT");
			case "attitude":
				return (negative ? "DESPAIR" : "GRIN");
			case "relationship":
				return (negative ? "DISGUST" : "LOVE");
		}
		return null;
	}
}
