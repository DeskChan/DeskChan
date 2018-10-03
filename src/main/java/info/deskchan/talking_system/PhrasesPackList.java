package info.deskchan.talking_system;

import info.deskchan.core.Path;
import info.deskchan.core_utils.TextOperations;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class PhrasesPackList {

	private CharacterController current;
	private LimitArrayList<Phrase> lastUsed = new LimitArrayList<>();
	private volatile ArrayList<PhrasesPack> packs = new ArrayList<>();
	private ArrayList<Phrase> matchingPhrases = new ArrayList<>();

	PhrasesPackList(CharacterController character){
		current = character.copy();
	}

	public synchronized void update(CharacterController newCharacter) {
		if (newCharacter.equals(current)) return;
		current = newCharacter.copy();
		update();
	}

	public synchronized void update() {
		matchingPhrases = new ArrayList<>();
		ArrayList<Map<String,Object>> checkList = new ArrayList<>();

		for (PhrasesPack pack : packs) {
			if (pack.packType == PhrasesPack.PackType.INTENT_DATABASE) continue;
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

	public static PhrasesPackList getDefault(CharacterController character){
		PhrasesPackList list = new PhrasesPackList(character);
		List<String> standard = new ArrayList<>();
		standard.add("main");
		standard.add("database");
		standard.add(Main.getProperties().getString("user-phrases"));
		list.set(standard);
		return list;
	}

	public List<Phrase> toPhrasesList(){
		List<Phrase> list = new ArrayList<>();
		for (PhrasesPack pack : packs)
			list.addAll(pack.phrases);
		return list;
	}

	public List<Phrase> toPhrasesList(PhrasesPack.PackType... packTypes){
		List<Phrase> list = new LinkedList<>();
		List<PhrasesPack.PackType> types = Arrays.asList(packTypes);
		for (PhrasesPack pack : packs) {
			if (types.contains(pack.packType))
				list.addAll(pack.phrases);
		}

		return list;
	}

	public List<String> toPacksList(){
		List<String> list = new ArrayList<>();
		for (PhrasesPack pack : packs)
			list.add(pack.getFile());
		return list;
	}

	public List<String> toPacksList(PhrasesPack.PackType... packTypes){
		List<String> list = new LinkedList<>();
		List<PhrasesPack.PackType> types = Arrays.asList(packTypes);
		for (PhrasesPack pack : packs) {
			if (types.contains(pack.packType))
				list.add(pack.getFile());
		}

		return list;
	}

	public synchronized void add(List<String> files){
		add(files, PhrasesPack.PackType.USER);
	}

	public synchronized void add(List<String> files, PhrasesPack.PackType type){
		for (String file : files)
			add(file, type, true);

        update();
    }

	public synchronized PhrasesPack add(String file, PhrasesPack.PackType packType) {
		return add(file, packType, true);
	}

	private synchronized PhrasesPack add(String file, PhrasesPack.PackType packType, boolean update) {
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

	public synchronized void set(List<String> files){
		set(files, PhrasesPack.PackType.USER);
	}

    public synchronized void set(List<String> files, PhrasesPack.PackType type){
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

	public synchronized void reload(){
		for(PhrasesPack pack : packs){
			pack.load();
		}
		update();
	}

	private List<PhraseRequest> phraseRequestQueue = new LinkedList<>();
	public void requestRandomPhrase(String intent, PhraseGetterCallback callback) {
		if (intent != null)
		requestRandomPhrase(intent, null, callback);
	}
	public void requestRandomPhrase(String intent, Map info, PhraseGetterCallback callback) {
		phraseRequestQueue.add(new PhraseRequest(intent, info, callback));
		if (phraseRequestQueue.size() == 1)
			requestRandomPhrase_impl(phraseRequestQueue.get(0).intents, info, callback);
	}
	public void requestRandomPhrase(Collection<String> intents, PhraseGetterCallback callback) {
		requestRandomPhrase(intents, null, callback);
	}
	public void requestRandomPhrase(Collection<String> intents, Map info, PhraseGetterCallback callback) {
		phraseRequestQueue.add(new PhraseRequest(intents, info, callback));
		if (phraseRequestQueue.size() == 1)
			requestRandomPhrase_impl(intents, info, callback);
	}

	private void requestRandomPhrase_impl(Collection<String> intents, Map info, PhraseGetterCallback callback){
		final String finalIntent = intents.iterator().next();

		if (matchingPhrases.size() == 0) {
			callback.call(new Phrase(Main.getString("phrase." + finalIntent)));
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
		for (Phrase phrase : matchingPhrases) {
			if (!phrase.noTimeout()) continue;
			for (String intent : intents) {
				if (phrase.hasIntent(intent)) {
					if (info == null || (phrase.getTags() != null && phrase.getTags().match(info))) {
						currentlySuitable.add(phrase);
						matchingList.add(phrase.toMap());
					}
					break;
				}
			}
		}



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
						callback.call(new Phrase(Main.getString("phrase."+finalIntent)));
					} else {
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

					phraseRequestQueue.remove(0);
					if (phraseRequestQueue.size() > 0) {
						PhraseRequest next = phraseRequestQueue.get(0);
						requestRandomPhrase_impl(next.intents, next.msgData, next.callback);
					}
				}
		);
	}

	public synchronized void clear() {
		packs = new ArrayList<>();
		matchingPhrases = new ArrayList<>();
		lastUsed = new LimitArrayList<>();
	}

	public PhrasesPack getUserDatabasePack(){
		String dbname = Main.getProperties().getString("user-phrases");
		for (PhrasesPack pack : packs){
			if (pack.packName.equals(dbname)){
				return pack;
			}
		}
		PhrasesPack userDatabase = new PhrasesPack(Main.getPhrasesDirPath().resolve(dbname+".phrases").toString(), PhrasesPack.PackType.INTENT_DATABASE);
		userDatabase.load();
		packs.add(userDatabase);
		return userDatabase;
	}

	public static boolean saveTo(String URL, String filename) {
		return saveToImpl(new HashMap(){{ put(Locale.getDefault(), URL); }}, filename+".phrases");
	}

	public static boolean saveDatabaseTo(String URL, String filename) {
		return saveToImpl(new HashMap(){{ put(Locale.getDefault(), URL); }}, filename+".database");
	}

	public static boolean saveTo(Map<String, String> URLs, String filename) {
		return saveToImpl(URLs, filename+".phrases");
	}

	public static boolean saveDatabaseTo(Map<String, String> URLs, String filename) {
		return saveToImpl(URLs, filename+".database");
	}


	private static boolean saveToImpl(Map<String, String> URLs, String filename) {
		try {
			JSONObject json = new JSONObject();
			for (Map.Entry<String, String> URL : URLs.entrySet()) {
				try {
					URL DATA_URL = new URL(URL.getValue());
					InputStream stream = DATA_URL.openStream();
					json.put(URL.getKey(), new JSONObject(IOUtils.toString(stream, "UTF-8")));
					stream.close();
				} catch (Exception u) {
					Main.log("Cannot download phrases at " + URL + ", no connection.");
					return false;
				}
			}

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Node mainNode = doc.createElement("phrases");

			try {
				for (Object name : json.names()) {
					Node nn = doc.createElement((String) name);
					JSONObject obj = json.getJSONObject((String) name);
					if (!obj.has("values")) continue;
					JSONArray array = obj.getJSONArray("values"), phrase;
					for (int i = 0; i < array.length(); i++) {
						try {
							phrase = array.getJSONArray(i);
							if (phrase.length() == 0) break;

							Phrase next = Phrase.fromJSONArray(phrase);
							nn.appendChild(next.toXMLNode(doc));
						} catch (Exception u) {
							Main.log(u);
							break;
						}
					}
					mainNode.appendChild(nn);
				}
				doc.appendChild(mainNode);
			} catch (Exception e){
				Main.log(e);
			}
			Path address = Main.getPhrasesDirPath();

			if (!address.exists())
				if (!address.mkdir())
					throw new Exception("Can't create folder at assets/phrases");

			address = address.resolve(filename);

			try {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				tr.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(address)));
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
		if (!address.exists()){
			if (!address.mkdir()) {
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
				doc = db.parse(new FileInputStream(address));
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
			tr.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(address)));
		} catch (Exception er) {
			Main.log("Error while rewriting file " + file + ".phrases" + ": " + er);
		}
	}

	private class LimitArrayList<E> extends ArrayList<E>{
		protected static final int LIMIT = 30;
		@Override
		public boolean add(E object){
			while (size() >= LIMIT) remove(0);
			return super.add(object);
		}
	}

	public interface PhraseGetterCallback {
		void call(Phrase phrase);
	}
	private class PhraseRequest {
		Collection<String> intents;
		Map msgData;
		PhraseGetterCallback callback;
		PhraseRequest(String intent, Map msgData, PhraseGetterCallback callback){
			this.intents = Collections.singletonList(intent.toUpperCase());
			this.msgData = msgData;
			this.callback = callback;
		}
		PhraseRequest(Collection<String> intents, Map msgData, PhraseGetterCallback callback){
			this.intents = new LinkedList<>();
			for (String i : intents)
				this.intents.add(i.toUpperCase());
			this.intents = intents;
			this.msgData = msgData;
			this.callback = callback;
		}
	}
}
