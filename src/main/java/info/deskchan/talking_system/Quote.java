package info.deskchan.talking_system;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;


public class Quote {
	public CharacterRange character;
	public String quote;
	public int timeout;
	public HashMap<String,List<String>> tags;
	public String purposeType;
	public String spriteType;
	
	public Quote(String quote) {
		this.quote = quote;
		purposeType = "CHAT";
		character = new CharacterRange();
		spriteType = "AUTO";
		timeout=0;
	}
	
	private static Node findInNode(NodeList list, String name) {
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeName().equals(name)) {
				return list.item(i);
			}
		}
		return null;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder("{" + quote + "} / Purpose: " + purposeType + " / Range: { " + character.toString() + " }");
		if (!spriteType.equals("AUTO")) {
			s.append(" / SpriteType: " + spriteType);
		}
		if (timeout > 0) {
			s.append(" / timeout: " + timeout);
		}
		for(Map.Entry<String,List<String>> tag : tags.entrySet()){
			s.append(" / "+tag.getKey()+":"+tag.getValue());
		}
		return s.toString();
	}
	
	public HashMap<String, Object> toMap() {
		HashMap<String, Object> map = new HashMap<>();
		map.put("text", quote);
		if (timeout != 0) {
			map.put("timeout", timeout);
		}
		map.put("characterImage", spriteType);
		map.put("purpose",purposeType);
		map.put("hash",this.hashCode());
		for(Map.Entry<String,List<String>> tag : tags.entrySet()) {
			if(tag.getValue().size()==0) continue;
			StringBuilder sb=new StringBuilder();
			for(String value : tag.getValue()) {
				sb.append("\"");
				sb.append(value);
				sb.append("\" ");
			}
			sb.deleteCharAt(sb.length()-1);
			map.put(tag.getKey(), sb.toString());
		}
		return map;
	}
	private static final String[] notTags={"text","purpose","sprite","timeout","range"};
	public static Quote create(Node node) {
		NodeList list = node.getChildNodes();
		String p;
		Node n;
		try {
			p = findInNode(list, "text").getTextContent();
		} catch (Exception e) {
			return null;
		}
		if (p.length() < 2) {
			return null;
		}
		
		Quote q = new Quote(p);
		
		try {
			q.purposeType = findInNode(list, "purpose").getTextContent();
			q.purposeType = q.purposeType.replace("\n", "");
		} catch (Exception e) {
			q.purposeType = "CHAT";
		}
		
		try {
			q.spriteType = findInNode(list, "sprite").getTextContent();
			q.spriteType = q.spriteType.replace("\n", "");
		} catch (Exception e) {
			q.spriteType = "AUTO";
		}
		try {
			q.timeout = Integer.valueOf(findInNode(list, "timeout").getTextContent());
		} catch (Exception e) {
			q.timeout = 0;
		}
		try {
			Node range = findInNode(list, "range");
			for (int i = 0; i < CharacterSystem.getFeatureCount(); i++) {
				try {
					p = findInNode(range.getChildNodes(), CharacterSystem.getFeatureName(i)).getTextContent();
					String[] sp = p.split(" \\| ");
					int a1, a2;
					try {
						a1 = Integer.valueOf(sp[0]);
					} catch (Exception e) {
						a1 = -10;
					}
					try {
						a2 = Integer.valueOf(sp[1]);
					} catch (Exception e) {
						a2 = 10;
					}
					q.character.range[i] = new Range(a1, a2);
				} catch (Exception e) { }
			}
		} catch (Exception e) { }
		q.tags=null;
		for (int i = 0,k=0; i < list.getLength(); i++) {
			if(list.item(i).getNodeName().charAt(0)=='#') continue;
			for (k = 0; k < notTags.length; k++)
				if (list.item(i).getNodeName().equals(notTags[k]))
					break;
			if(k<notTags.length) continue;
			q.fillTagFromString(list.item(i).getNodeName(),list.item(i).getTextContent());
		}
		return q;
	}
	public void fillTagFromString(String tagname,String text){
		text=text+" ";
		try{
			if(tags==null) tags=new HashMap<>();
			ArrayList<String> args=new ArrayList<String>();
			boolean inQuoteMarks=false;
			int st=0;
			for(int c=0;c<text.length();c++){
				if(text.charAt(c)=='"') inQuoteMarks=!inQuoteMarks;
				else if(text.charAt(c)==' ' && !inQuoteMarks){
					if(st==c) {
						st=c+1;
						continue;
					}
					if(text.charAt(st)=='"' && text.charAt(c-1)=='"')
						args.add(text.substring(st + 1, c - 1));
					else
						args.add(text.substring(st,c));
					st=c+1;
				}
			}
			tags.put(tagname,args);
		} catch(Exception e){ Main.log(e); }
	}
	private void AppendTo(Document doc, Node target, String name, String text) {
		Node n = doc.createElement(name);
		n.setTextContent(text);
		target.appendChild(n);
	}
	
	public Node toXMLNode(Document doc) {
		Node mainNode = doc.createElement("quote");
		try {
			AppendTo(doc, mainNode, "text", quote);
			if (timeout > 0) {
				AppendTo(doc, mainNode, "timeout", String.valueOf(timeout));
			}
			AppendTo(doc, mainNode, "purpose", purposeType);
			if (!spriteType.equals("AUTO")) {
				AppendTo(doc, mainNode, "sprite", spriteType);
			}
			if (tags!=null) {
				for(Map.Entry<String,List<String>> tag : tags.entrySet()) {
					if(tag.getValue().size()==0) continue;
					StringBuilder sb=new StringBuilder();
					for(String value : tag.getValue()) {
						sb.append("\"");
						sb.append(value);
						sb.append("\" ");
					}
					sb.deleteCharAt(sb.length()-1);
					AppendTo(doc, mainNode, tag.getKey(), sb.toString());
				}
			}
			Node c = character.toXMLNode(doc);
			if (c != null) {
				mainNode.appendChild(c);
			}
		} catch (Exception e) {
			//Main.log(e);
		}
		return mainNode;
	}
	
	public boolean matchToCharacter(CharacterDefinite target) {
		for (int i = 0, l = CharacterSystem.getFeatureCount(); i < l; i++) {
			if (!character.range[i].Match(target.getValue(i))) {
				return false;
			}
		}
		return true;
	}
	
	private Date last_usage;
	
	public void UpdateLastUsage() {
		last_usage = new Date();
	}
	
	public boolean noTimeout() {
		if (last_usage == null || timeout<=0) {
			return true;
		}
		return (new Date().getTime() - last_usage.getTime() > timeout * 1000);
	}
}
