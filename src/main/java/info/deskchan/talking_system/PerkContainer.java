package info.deskchan.talking_system;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PerkContainer {
	class Perk implements Comparable<Perk> {
		String name;
		int priority;
		
		public Perk(String name, int priority) {
			this.name = name;
			this.priority = priority;
		}
		
		@Override
		public int compareTo(Perk perk) {
			return perk.priority - priority;
		}
	}
	
	private LinkedList<Perk> perks = new LinkedList<>();
	
	public void add(String name, Map<String, Object> data) {
		int priority = (Integer) data.getOrDefault("priority", 5);
		for (int i = 0; i < perks.size(); i++) {
			if (perks.get(i).name.equals(name)) {
				perks.get(i).priority = priority;
				return;
			}
		}
		perks.add(new Perk(name, priority));
	}
	
	public void remove(String name) {
		for (int i = 0; i < perks.size(); i++) {
			if (perks.get(i).name.equals(name)) {
				perks.remove(i);
				return;
			}
		}
	}
	
	private void sort() {
		Collections.sort(perks);
	}
	
	public int size() {
		return perks.size();
	}
	
	public String getPerk(int index) {
		return perks.get(index).name;
	}
	
	private String buffer;
	private int blocker = 0;
	private String senderToWait;
	CountDownLatch latch;
	
	public void getAnswerFromPerk(String name, Map<String, Object> data) {
		switch (blocker) {
			case 1: {
				if (data.getOrDefault("answer", "true").equals("false")) {
					buffer = "false";
				}
			}
			break;
			case 2: {
				if (data.getOrDefault("answer", "true").equals("true")) {
					buffer = "true";
				}
			}
			break;
			case 3: {
				if (!senderToWait.equals(name)) {
					return;
				}
				buffer = (String) data.getOrDefault("answer", null);
			}
			break;
		}
		
		latch.countDown();
	}
	
	public boolean ifAllAgree(String type) {
		if (blocker != 0) {
			return false;
		}
		blocker = 1;
		latch = new CountDownLatch(perks.size());
		HashMap<String, Object> map = new HashMap<>();
		map.put("type", type);
		buffer = "true";
		for (int i = 0; i < perks.size(); i++) {
			Main.sendToProxy("perk:operate", map);
		}
		try {
			latch.wait(1000);
		} catch (Exception e) {
		}
		blocker = 0;
		return buffer.equals("true");
	}
	
	public boolean ifAnyoneAgree(String type) {
		if (blocker != 0) {
			return false;
		}
		blocker = 2;
		latch = new CountDownLatch(perks.size());
		HashMap<String, Object> map = new HashMap<>();
		map.put("type", type);
		buffer = "false";
		for (int i = 0; i < perks.size(); i++) {
			Main.sendToProxy("perk:operate", map);
		}
		try {
			latch.wait(1000);
		} catch (Exception e) {
		}
		
		blocker = 0;
		return buffer.equals("true");
	}
	
	String send(String type, String data) {
		if (blocker != 0) {
			return null;
		}
		blocker = 3;
		for (int i = 0; i < perks.size(); i++) {
			latch = new CountDownLatch(1);
			senderToWait = perks.get(i).name;
			HashMap<String, Object> map = new HashMap<>();
			map.put("type", type);
			map.put("type", data);
			Main.sendToProxy("perk:operate", map);
			try {
				latch.wait(300);
			} catch (Exception e) {
			}
			if (buffer != null) {
				data = buffer;
			}
		}
		blocker = 0;
		return data;
	}
}
