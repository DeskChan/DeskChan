package info.deskchan.talking_system;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class CharacterDefinite extends CharacterSystem {
	protected float[][] value;
	
	public CharacterDefinite() {
		value = new float[featureCount][3];
		for (int i = 0; i < featureCount; i++) {
			value[i] = new float[]{-10, 0, 10};
		}
	}
	public CharacterDefinite(long number){
		value = new float[featureCount][3];
		for (int i = 0; i < featureCount; i++){
			value[i] = new float[]{-10, 0, 10};
			value[i][1]=number%21-10;
			number/=21;
		}
	}
	public CharacterDefinite(float[][] values) {
		value = new float[featureCount][3];
		for (int i = 0; i < featureCount && i < values.length; i++) {
			for (int j = 0; j < 3 && j < values[i].length; j++) {
				value[i][j] = values[i][j];
			}
		}
	}
	
	public CharacterDefinite(JSONObject json) {
		value = new float[featureCount][3];
		if (json == null) {
			for (int i = 0; i < featureCount; i++) {
				value[i] = new float[]{-10, 0, 10};
			}
			return;
		}
		for (int i = 0; i < featureCount; i++) {
			try {
				Object o = json.get(getFeatureName(i));
				int val1 = -10, val2 = 0, val3 = 10;
				if (o instanceof String) {
					setValues(i, (String) o);
					continue;
				}
				if (o instanceof JSONArray) {
					JSONArray ar = (JSONArray) o;
					val1 = ar.getInt(0);
					val2 = ar.getInt(1);
					val3 = ar.getInt(2);
				}
				if (o instanceof Integer) {
					val2 = (Integer) o;
				}
				setValues(i, val1, val2, val3);
			} catch (Exception o) {
			}
		}
	}
	
	public float checkToDiapason(float val) {
		if (val > 10) {
			return 10;
		} else if (val < -10) {
			return -10;
		}
		return val;
	}
	
	protected void setValues(int index, String values) {
		if (values.isEmpty()) {
			return;
		}
		float val1 = -10, val2 = 0, val3 = 10;
		String[] ar = values.split(";");
		Float[] far = new Float[ar.length];
		for (int i = 0; i < ar.length; i++) {
			try {
				far[i] = Float.valueOf(ar[i].trim());
			} catch (Exception e) {
			}
		}
		
		if (ar.length == 1) {
			val2 = far[0];
		}
		if (ar.length == 2) {
			val1 = far[0];
			val3 = far[1];
		}
		if (ar.length == 3) {
			val1 = far[0];
			val2 = far[1];
			val3 = far[2];
		}
		setValues(index, val1, val2, val3);
	}
	
	protected void setValues(int index, float val1, float val2, float val3) {
		if (index >= featureCount) {
			return;
		}
		float t;
		val1 = checkToDiapason(val1);
		val2 = checkToDiapason(val2);
		val3 = checkToDiapason(val3);
		if (val1 > val2) {
			t = val1;
			val1 = val2;
			val2 = t;
		}
		if (val2 > val3) {
			t = val2;
			val2 = val3;
			val3 = t;
		}
		if (val1 > val2) {
			t = val1;
			val1 = val2;
			val2 = t;
		}
		val1 = Math.round(val1);
		val3 = Math.round(val3);
		value[index] = new float[]{val1, val2, val3};
	}
	
	public void setValue(int index, float val) {
		if (index >= featureCount) {
			return;
		}
		float t;
		if (val <= value[index][2] && val >= value[index][0]) {
			value[index][1] = val;
		}
	}
	
	public int getValue(int index) {
		if (value[index][1] > 0) {
			return (int) Math.floor(value[index][1]);
		}
		return (int) Math.ceil(value[index][1]);
	}
	
	public CharacterDefinite Mix(CharacterDefinite other, float percent) {
		CharacterDefinite cd = this.Clone();
		for (int i = 0; i < getFeatureCount(); i++) {
			setValue(i, value[i][1] * percent + other.value[i][1] * (1 - percent));
		}
		return cd;
	}
	
	public String getValueString(int index) {
		return value[index][0] + " ; " + value[index][1] + " ; " + value[index][2];
	}
	
	public void Print() {
		for (int i = 0; i < featureCount; i++) {
			System.out.println(features[i][1] + " <" + value[i][0] + " ; " + value[i][1] + " ; " + value[i][2] + "> " + features[i][2]);
		}
	}
	
	public CharacterDefinite Clone() {
		return new CharacterDefinite(value);
	}
	
	public boolean equal(CharacterDefinite sec) {
		for (int i = 0; i < featureCount; i++) {
			if (getValue(i) != sec.getValue(i)) {
				return false;
			}
		}
		return true;
	}
	
	public JSONObject toJSON() {
		JSONObject save = new JSONObject();
		for (int i = 0; i < featureCount; i++) {
			save.put(features[i][2], getValueString(i));
		}
		/*JSONArray ar;
		for (int i = 0; i < featureCount; i++) {
			ar = new JSONArray();
			ar.put(0, value[i][0]);
			ar.put(1, value[i][1]);
			ar.put(2, value[i][2]);
			save.put(features[i][2], ar);
		}*/
		return save;
	}
	
	@Override
	public String toString() {
		JSONObject save = new JSONObject();
		for (int i = 0; i < featureCount; i++) {
			save.put(features[i][2], value[i][0] + " / " + value[i][1] + " / " + value[i][2]);
		}
		return save.toString();
	}
	
	public CharacterDefinite Add(CharacterDefinite other) {
		CharacterDefinite cd = this.Clone();
		for (int i = 0; i < getFeatureCount(); i++) {
			setValue(i, value[i][1] + other.value[i][1]);
		}
		return cd;
	}
	
	public void applyInfluence(ArrayList<Influence> influences) {
		float val;
		for (Influence in : influences) {
			setValue(in.getFeatureID(), value[in.getFeatureID()][1] + in.getMultiplier() * Influence.globalMultiplier);
		}
	}
	
	public void applyInfluence(Influence in) {
		if (in == null) {
			return;
		}
		setValue(in.getFeatureID(), value[in.getFeatureID()][1] + in.getMultiplier() * Influence.globalMultiplier);
	}
	
	public String getSpriteType() {
		int feature = -1;
		boolean negative = false;
		for (int i = 0; i < featureCount; i++) {
			if (Math.abs(value[i][1]) >= 4) {
				if (feature == -1) {
					feature = i;
					negative = (value[i][1] < 0);
				} else {
					feature = -2;
				}
			}
		}
		switch (feature) {
			case 0:
				return (negative ? "SHY" : "CONFIDENT");
			case 1:
				return (negative ? "NORMAL" : "EXCITEMENT");
			case 2:
				return (negative ? "SAD" : "HAPPY");
			case 3:
			case 5:
				return (negative ? "DISGUSTED" : "LOVE");
			case 4:
				return (negative ? "VULGAR" : "SERIOUS");
		}
		return null;
	}
}
