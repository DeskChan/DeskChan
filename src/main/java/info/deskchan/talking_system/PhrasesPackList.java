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
import java.util.concurrent.CopyOnWriteArrayList;

public class PhrasesPackList extends CopyOnWriteArrayList<PhrasesPack> {

	PhrasesPackList(){
		super();
	}

	public List<Phrase> toPhrasesList(){
		List<Phrase> list = new ArrayList<>();
		for (PhrasesPack pack : this)
			list.addAll(pack);
		return list;
	}

	public List<Phrase> toPhrasesList(PhrasesPack.PackType... packTypes){
		List<Phrase> list = new LinkedList<>();
		List<PhrasesPack.PackType> types = Arrays.asList(packTypes);
		for (PhrasesPack pack : this) {
			if (types.contains(pack.packType))
				list.addAll(pack);
		}

		return list;
	}

	public List<String> toPacksList(){
		List<String> list = new ArrayList<>();
		for (PhrasesPack pack : this)
			list.add(pack.getFile());
		return list;
	}

	public List<String> toPacksList(PhrasesPack.PackType... packTypes){
		List<String> list = new LinkedList<>();
		List<PhrasesPack.PackType> types = Arrays.asList(packTypes);
		for (PhrasesPack pack : this) {
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
			add(file, type);
    }

	public PhrasesPack add(String file, PhrasesPack.PackType packType) {
		PhrasesPack pack;
		try {
			pack = new PhrasesPack(file, packType);
		} catch (Exception e){
			Main.log("Error while reading file " + file + ": " + e.getMessage());
			return null;
		}

		if(contains(pack)) return null;

		pack.load();

		add(pack);
		Main.log("Loaded phrases: " + pack.getName()+" "+ pack.size());

		return pack;
	}

	public synchronized void set(List<String> files){
		set(files, PhrasesPack.PackType.USER);
	}

    public synchronized void set(List<String> files, PhrasesPack.PackType type){
		ArrayList<PhrasesPack> dummyPacks = new ArrayList();
		for(int k = 0;k < files.size(); k++)
			dummyPacks.add(new PhrasesPack(files.get(k), type));

        for(int i = 0; i < size(); i++){
           	if (dummyPacks.contains(get(i))) continue;
            remove(i);
            i--;
        }

        add(files);
    }

	public synchronized void reload(){
		for(PhrasesPack pack : this){
			pack.load();
		}
	}

	public PhrasesPack getUserDatabasePack(){
		String dbname = Main.getProperties().getString("user-phrases");
		for (PhrasesPack pack : this){
			if (pack.packName.equals(dbname)){
				return pack;
			}
		}
		PhrasesPack userDatabase = new PhrasesPack(Main.getPhrasesDirPath().resolve(dbname+".phrases").toString(), PhrasesPack.PackType.INTENT_DATABASE);
		userDatabase.load();
		add(userDatabase);
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

}
