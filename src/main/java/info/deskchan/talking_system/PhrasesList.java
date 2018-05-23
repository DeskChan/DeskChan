package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class PhrasesPack {

	public enum PackType { USER, PLUGIN, DATABASE }

	protected PackType packType;
	protected Path packFile;
	protected String packName;
	protected ArrayList<Phrase> phrases = new ArrayList<>();
	protected boolean loaded;

	public PhrasesPack(String file, PackType packType) {
		packName = file;
		packFile = Paths.get(file).normalize();
		this.packType = packType;

		if (!packFile.getFileName().toString().contains("."))
			packFile = Paths.get(file + ".phrases").normalize();
		if (!packFile.isAbsolute())
			packFile = Main.getPhrasesDirPath().resolve(packFile);
		else if (packFile.startsWith(Main.getPhrasesDirPath().normalize()))
			packName = packFile.getFileName().toString();

		if (packName.contains(".")) {
			if (!packName.endsWith(".database")) this.packType = PackType.DATABASE;
			packName = packName.substring(0, packName.lastIndexOf("."));
		}

		loaded = false;
	}

	public PackType getPackType() {
		return packType;
	}

	public void load(){
		try {
			phrases.clear();
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			f.setValidating(false);

			DocumentBuilder builder = f.newDocumentBuilder();
			InputStream inputStream = Files.newInputStream(packFile);
			Document doc = builder.parse(inputStream);
			inputStream.close();
			Node mainNode = doc.getChildNodes().item(0);
			NodeList list = mainNode.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				if (!list.item(i).getNodeName().equals("phrase")) continue;
				try {
					add(Phrase.create(list.item(i)));
				} catch (Exception e2) {
					Main.log(e2);
				}
			}
		} catch (Exception e) {
			Main.log("Error while parsing phrases file " + packName + ": " + e.getMessage());
			loaded = false;
			return;
		}
		loaded = true;
	}

	public void add(Phrase quote) {
		if (quote != null) phrases.add(quote);
	}

	public int size(){
		return phrases.size();
	}

	public Phrase get(int i){
		return phrases.get(i);
	}

	public String getFile(){ return packFile.toString(); }

	public String getName(){
		return packName;
	}

	public String toString(){ return "[" + packName + " - " + packFile.toString() + "]" +
			(loaded ? "" : " -- " + Main.getString("error") + " -- "); }

	@Override
	public boolean equals(Object other){
		if (other.getClass() != this.getClass()) return false;
		return packFile.equals(((PhrasesPack) other).packFile);
	}

	public void printPhrasesLack(String purpose){
		System.out.println(packName);

		int minimumsCount = 10;
		CharacterController[] characters = new CharacterController[minimumsCount];
		int[] matchingPhrasesCounts = new int[minimumsCount];

		long charactersCount = (long) Math.pow(CharacterFeatures.LENGTH, CharacterFeatures.getFeatureCount());

		// generating start points
		for(int i = 0; i < minimumsCount; i++){
			characters[i] = CharacterPreset.getDefaultCharacterController();
			matchingPhrasesCounts[i] = 0;
			for(Phrase phrase : phrases)
				if(phrase.matchToCharacter(characters[i])) matchingPhrasesCounts[i]++;
		}

		// iterating over other points to find minimum
		for (int i = minimumsCount; i < charactersCount; i+=2) {
			if(i % 1000000 == 0) System.out.println(i*1./charactersCount);

			// generating new point
			CharacterController current = CharacterPreset.getDefaultCharacterController();

			for (int j = 0, num = i; j < CharacterFeatures.getFeatureCount(); j++){
				current.setValue(j, num % CharacterFeatures.LENGTH - CharacterFeatures.BORDER);
				num /= CharacterFeatures.LENGTH;
			}

			// comparing to minimum points. if current and any of minimum are too close, skip current point
			boolean close = false, ct;
			for (int k = 0; k < minimumsCount; k++) {
				ct = true;

				for (int j = 0; j < CharacterFeatures.getFeatureCount(); j++) {
					if (Math.abs(characters[k].getValue(j) - current.getValue(j)) > 2) {
						ct = false;
						break;
					}
				}
				if (ct) {
					close = true;
					break;
				}
			}

			if (close) continue;

			// counting matching phrases
			int matchingPhrasesCount = 0;
			for (Phrase phrase : phrases)
				if (phrase.purposeEquals(purpose) && phrase.matchToCharacter(current)) matchingPhrasesCount++;

			// if count of some's minimum is more than to current, replacing it
			for (int k = 0; k < minimumsCount; k++)
				if (matchingPhrasesCounts[k] > matchingPhrasesCount) {
					matchingPhrasesCounts[k] = matchingPhrasesCount;
					characters[k] = current;
					break;
				}
		}

		// printing points
		for(int k=0;k<minimumsCount;k++)
			System.out.println(k + " " + characters[k].toString() + " " + matchingPhrasesCounts[k]);
	}

}

public class PhrasesList {

	private CharacterController current;
	private LimitArrayList<Phrase> lastUsed = new LimitArrayList<>();
	private ArrayList<PhrasesPack> packs = new ArrayList<>();
	private ArrayList<Phrase> matchingPhrases = new ArrayList<>();

	PhrasesList(CharacterController character){
		current = character.copy();
	}

	public void update(CharacterController newCharacter) {
		if (newCharacter.equals(current)) return;
		current = newCharacter.copy();
		update();
	}
	
	public void update() {
		matchingPhrases = new ArrayList<>();
		ArrayList<Map<String,Object>> checkList = new ArrayList<>();

		for (PhrasesPack pack : packs) {
			if (pack.packType == PhrasesPack.PackType.DATABASE) continue;
			for (Phrase phrase : pack.phrases)
				if (phrase.matchToCharacter(current)) {
					matchingPhrases.add(phrase);
					checkList.add(phrase.toMap());
				}
		}

		Main.getPluginProxy().sendMessage("talk:remove-quote", checkList, (sender, data) -> {
			List<Map<String, Object>> phrasesList = (ArrayList) data;
			if(phrasesList == null) return;

			for (Map<String,Object> phrase : phrasesList) {
				if (!phrase.containsKey("hash")) continue;

				for(int i = 0, hash = (int) phrase.get("hash"); i < matchingPhrases.size(); i++){
					if(matchingPhrases.get(i).hashCode() == hash){
						matchingPhrases.remove(i);
						break;
					}
				}
			}
		});
	}

	public static PhrasesList getDefault(CharacterController character){
		PhrasesList list = new PhrasesList(character);
		List<String> standard = new ArrayList<>();
		standard.add("main");
		list.set(standard);
		return list;
	}

	public List<String> toList(PhrasesPack.PackType... packTypes){
		List<String> list = new LinkedList<>();
		List<PhrasesPack.PackType> types = Arrays.asList(packTypes);
		for (PhrasesPack pack : packs) {
			if (types.contains(pack.packType))
				list.add(pack.getFile());
		}

		return list;
	}

	public void add(List<String> files){
		add(files, PhrasesPack.PackType.USER);
	}

	public void add(List<String> files, PhrasesPack.PackType type){
		for (String file : files)
			add(file, type, true);

        update();
    }

	public PhrasesPack add(String file, PhrasesPack.PackType packType) {
		return add(file, packType, true);
	}

	private PhrasesPack add(String file, PhrasesPack.PackType packType, boolean update) {
		PhrasesPack pack;
		try {
			pack = new PhrasesPack(file, packType);
		} catch (Exception e){
			Main.log("Error while reading file " + file + ": " + e.getMessage());
			return null;
		}

		if(packs.contains(pack)) return null;

		pack.load();

		packs.add(pack);
		Main.log("Loaded phrases: " + pack.getName()+" "+ pack.size());

		if (update) update();

		return pack;
	}

	public void set(List<String> files){
		set(files, PhrasesPack.PackType.USER);
	}

    public void set(List<String> files, PhrasesPack.PackType type){
		ArrayList<PhrasesPack> dummyPacks = new ArrayList();
		for(int k = 0;k < files.size(); k++)
			dummyPacks.add(new PhrasesPack(files.get(k), type));

        for(int i = 0; i < packs.size(); i++){
           	if (dummyPacks.contains(packs.get(i))) continue;
            packs.remove(i);
            i--;
        }

        add(files);
    }

    public List<Phrase> getAllPhrases(){
    	List<Phrase> list = new ArrayList<>();
    	for (PhrasesPack pack : packs)
    		list.addAll(pack.phrases);
    	return list;
	}

	public void reload(){
		for(PhrasesPack pack : packs){
			pack.load();
		}
		update();
	}

	public void requestRandomQuote(String purpose, PhraseGetterCallback callback) {
		requestRandomQuote(purpose, null, callback);
	}
	public void requestRandomQuote(String purpose, Map info, PhraseGetterCallback callback) {
		purpose = purpose.toUpperCase();
		if (matchingPhrases.size() == 0) {
			callback.call(new Phrase(Main.getString("phrase." + purpose)));
			return;
		}

		List<Phrase> currentlySuitable = new LinkedList<>();
		if (info != null && info.get("tags") != null) {
			if (info.get("tags") instanceof Map)
				info = new TextOperations.TagsMap((Map) info.get("tags"));
			else
				info = new TextOperations.TagsMap(info.get("tags").toString());
		} else info = null;

		List<Map> matchingList = new ArrayList<>();
		for (Phrase phrase : matchingPhrases)
			if (phrase.noTimeout() && phrase.purposeEquals(purpose)){
				if (info == null || (phrase.getTags() != null && phrase.getTags().match(info))){
					currentlySuitable.add(phrase);
					matchingList.add(phrase.toMap());
				}
			}

		final String fPurpose = purpose;
		Main.getPluginProxy().sendMessage("talk:reject-quote", matchingList,
				(sender, data) -> {
					List<Map<String, Object>> phrasesList = (ArrayList<Map<String,Object>>) data;
					if(phrasesList == null) return;

					for (Map<String, Object> phrase : phrasesList) {
						if (!phrase.containsKey("hash")) continue;

						for(int i = 0, hash = (int) phrase.get("hash"); i < currentlySuitable.size(); i++){
							if(currentlySuitable.get(i).hashCode() == hash){
								currentlySuitable.remove(i);
								break;
							}
						}
					}
				},
				(sender, dat) -> {
					if (currentlySuitable.size() == 0) {
						callback.call(new Phrase(Main.getString("phrase."+fPurpose)));
						return;
					}
					int counter = LimitArrayList.LIMIT + 1;
					Phrase phrase;
					do {
						counter--;
						int r = new Random().nextInt(currentlySuitable.size());
						phrase = currentlySuitable.get(r);
					} while (counter > 0 && lastUsed.contains(phrase));

					lastUsed.add(phrase);
					phrase.updateLastUsage();
					callback.call(phrase);
				}
		);
	}

	public void printPhrasesLack(String purpose){
    	purpose = purpose.toUpperCase();
		for(PhrasesPack pack : packs)
			pack.printPhrasesLack(purpose);
	}

	public interface PhraseGetterCallback {
    	void call(Phrase phrase);
	}

	public Phrase get(int index) {
		return matchingPhrases.get(index);
	}
	
	public int size() {
		return matchingPhrases.size();
	}
	
	public void clear() {
		packs = new ArrayList<>();
		matchingPhrases = new ArrayList<>();
		lastUsed = new LimitArrayList<>();
	}

	public static boolean saveTo(String URL, String filename) {
		return saveToImpl(URL, filename+".phrases");
	}

	public static boolean saveDatabaseTo(String URL, String filename) {
		return saveToImpl(URL, filename+".database");
	}

	private static boolean saveToImpl(String URL, String filename) {
		try {
			JSONObject json;
			try {
				URL DATA_URL = new URL(URL);
				InputStream stream = DATA_URL.openStream();
				json = new JSONObject(IOUtils.toString(stream, "UTF-8"));
				stream.close();
			} catch (Exception u) {
				Main.log("Cannot download phrases at "+URL+", no connection.");
				return false;
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Node mainNode = doc.createElement("phrases");

			try {
				JSONArray array = json.getJSONArray("values"), phrase;
				for (int i = 0; i < array.length(); i++) {
					try {
						phrase = array.getJSONArray(i);
						if (phrase.length() == 0) break;

						Phrase next = Phrase.fromJSONArray(phrase);
						mainNode.appendChild(next.toXMLNode(doc));
					} catch (Exception u) {
						Main.log(u);
						break;
					}
				}
				doc.appendChild(mainNode);
			} catch (Exception e){
				//Main.log(e);
			}
			Path address = Main.getPhrasesDirPath();

			if (!address.toFile().exists())
				if (!address.toFile().mkdir())
					throw new Exception("Can't create folder at assets/phrases");

			address = address.resolve(filename);

			try {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				tr.transform(new DOMSource(doc), new StreamResult(Files.newOutputStream(address)));
			} catch (Exception er) {
				throw new Exception("Error while rewriting file " + filename + ".phrases", er);
			}
		} catch (Exception e) {
			Main.log(e);
			return false;
		}
		Main.log("Phrases pack \""+filename+"\" successfully loaded");
		return true;
	}
	
	public void saveTo(Phrase quote, String file) {
		Document doc;
		Node mainNode;
		Path address = Main.getPhrasesDirPath();
		if (!address.toFile().exists()){
			if (!address.toFile().mkdir()) {
				Main.log(new Exception("Can't create folder at assets/phrases"));
				return;
			}
		}
		address = address.resolve(file + ".phrases");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		boolean newFile = false;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			// create instance of DOM
			try {
				doc = db.parse(Files.newInputStream(address));
				mainNode = doc.getChildNodes().item(0);
			} catch (Exception er) {
				Main.log("Error while reading file " + file + ".phrases" + ": " + er);
				doc = db.newDocument();
				newFile = true;
				mainNode = doc.createElement("phrases");
			}
		} catch (Exception er) {
			Main.log("Error while creating parser for file " + file + ".phrases" + ": " + er);
			return;
		}
		mainNode.appendChild(quote.toXMLNode(doc));
		if (newFile) {
			doc.appendChild(mainNode);
		}
		try {
			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT, "yes");
			tr.setOutputProperty(OutputKeys.METHOD, "xml");
			tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			tr.transform(new DOMSource(doc), new StreamResult(Files.newOutputStream(address)));
		} catch (Exception er) {
			Main.log("Error while rewriting file " + file + ".phrases" + ": " + er);
		}
	}

	class LimitArrayList<E> extends ArrayList<E>{
		protected static final int LIMIT = 30;
		@Override
		public boolean add(E object){
			while (size() >= LIMIT) remove(0);
			return super.add(object);
		}
	}
}
