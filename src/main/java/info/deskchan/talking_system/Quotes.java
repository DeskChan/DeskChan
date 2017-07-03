package info.deskchan.talking_system;

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

class QuotePack{
	private Path packName;
	private String packNameString;
	private ArrayList<Quote> quotes = new ArrayList<Quote>();

	public QuotePack(String file){
        packNameString=file;
		packName=Paths.get(file).normalize();

        if(!packName.getFileName().toString().contains("."))
            packName=Paths.get(file+".quotes").normalize();
		if(!packName.isAbsolute())
			packName=Main.getDataDirPath().resolve(packName);
        else if(packName.startsWith(Main.getDataDirPath()))
            packNameString=packName.getFileName().toString();

		DocumentBuilder builder;
		try {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			f.setValidating(false);
			builder = f.newDocumentBuilder();
		} catch (Exception e) {
			Main.log("Error while starting quotes parser: " + e);
			return;
		}
		try {
			InputStream inputStream = Files.newInputStream(packName);
			Document doc = builder.parse(inputStream);
			inputStream.close();
			Node mainNode = doc.getChildNodes().item(0);
			NodeList list = mainNode.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				if (!list.item(i).getNodeName().equals("quote")) {
					continue;
				}
				try {
					add(Quote.create(list.item(i)));
				} catch (Exception e2) {
					Main.log(e2);
				}
			}
		} catch (Exception e) {
			Main.log("Error while parsing quotes file " + file + ": " + e);
            return;
		}
	}
	public void add(Quote quote) {
		if (quote == null) return;
		//for (Quote q : quotes)
		//	if (q.toString().equals(quote.toString())) return;
		quotes.add(quote);
	}
	public int size(){
		return quotes.size();
	}

	public Quote get(int i){
		return quotes.get(i);
	}
	public String getFileName(){
		return packNameString;
	}
}

public class Quotes {
	CharacterDefinite current = new CharacterDefinite();
	private int queueLength = 30;
	private int curPos = 0;
	private Quote[] lastUsed = new Quote[queueLength];
	private ArrayList<QuotePack> packs = new ArrayList<QuotePack>();
	private ArrayList<Quote> suitableQuotes = new ArrayList<Quote>();

	public void update(CharacterDefinite newCharacter) {
		if (current.equal(newCharacter)) {
			return;
		}
		current = newCharacter;
		update();
	}
	
	public void update() {
		suitableQuotes = new ArrayList<Quote>();
		HashMap<String,Object> quotesToSend=new HashMap<>();
		ArrayList<HashMap<String,Object>> list=new ArrayList<>();
		quotesToSend.put("quotes",list);
		for (QuotePack pack : packs) {
			for (int i = 0, l = pack.size(); i < l; i++)
				if (pack.get(i).matchToCharacter(current)){
					suitableQuotes.add(pack.get(i));
					list.add(pack.get(i).toMap());
				}
		}
		Main.getPluginProxy().sendMessage("talk:remove-quote",quotesToSend,
		(sender, dat) -> {
			HashMap<String,Object> data=(HashMap<String,Object>)dat;
			ArrayList<HashMap<String,Object>> quotes_list=(ArrayList<HashMap<String,Object>>)data.getOrDefault("quotes",null);
			if(quotes_list==null) return;
			for (HashMap<String,Object> map : quotes_list) {
				int hash=(int)map.getOrDefault("hash",0);
				for(int i=0;i<suitableQuotes.size();i++){
					if(suitableQuotes.get(i).hashCode()==hash){
						System.out.println("remove: "+suitableQuotes.get(i));
						suitableQuotes.remove(i);
						break;
					}
				}
			}
		});
	}
	public Quote[] toArray(){
		LinkedList<Quote> q=new LinkedList<>();
		for (QuotePack pack : packs)
			for (int i = 0, l = pack.size(); i < l; i++)
				q.add(pack.get(i));
		return q.toArray(new Quote[q.size()]);
	}
	public void load(List<String> files){
        for(int i=0;i<files.size();i++){
            QuotePack p;
            try{
                p=new QuotePack(files.get(i));
            } catch (Exception e){
                Main.log("Error while reading file "+files.get(i)+": "+e.getMessage());
                files.remove(i);
                i--;
                continue;
            }
            boolean found=false;
            for(QuotePack pack : packs)
                if(pack.getFileName().equals(files.get(i))){
                    found=true;
                    break;
                }
            if(!found && p.size()>0){
                packs.add(p);
                files.set(i,p.getFileName());
                Main.log("Loaded quotes: " + p.getFileName()+" "+ p.size());
            }
        }
        files.clear();
        for(QuotePack pack : packs){
            files.add(pack.getFileName());
        }
        update();
    }
    public void setPacks(List<String> files){
        for(int i=0;i<packs.size();i++){
            boolean found=false;
            for(int k=0;k<files.size();k++)
                if(packs.get(i).getFileName().equals(files.get(k))){
                    found=true;
                    break;
                }
            if(found)continue;
            packs.remove(i);
            i--;
        }
        load(files);
    }

	public void requestRandomQuote(String purpose,GetQuoteCallback callback) {
		purpose = purpose.toUpperCase();
		if (suitableQuotes.size() == 0) {
			callback.call(new Quote("Я не знаю, что сказать."));
			return;
		}

		LinkedList<Quote> sq = new LinkedList<>();

		HashMap<String,Object> quotesToSend=new HashMap<>();
		ArrayList<HashMap<String,Object>> list=new ArrayList<>();
		quotesToSend.put("quotes",list);
		Quote q;
		for (int i = 0; i < suitableQuotes.size(); i++) {
			q = suitableQuotes.get(i);
			if (q.noTimeout() && q.purposeType.equals(purpose)){
				sq.add(q);
				list.add(q.toMap());
			}
		}
		Main.getPluginProxy().sendMessage("talk:reject-quote",quotesToSend,
				(sender, dat) -> {
					System.out.println("i got answer");
  					HashMap<String,Object> data=(HashMap<String,Object>)dat;
					ArrayList<HashMap<String,Object>> quotes_list=(ArrayList<HashMap<String,Object>>)data.getOrDefault("quotes",null);
					if(quotes_list==null) return;
					for (HashMap<String,Object> map : quotes_list) {
						int hash=(int)map.getOrDefault("hash",0);
						for(int i=0;i<sq.size();i++){
							if(sq.get(i).hashCode()==hash){
								System.out.println("reject: "+sq.get(i));
								sq.remove(i);
								break;
							}
						}
					}
				},
				(sender, dat) -> {
					if (sq.size() == 0) {
						callback.call(new Quote("Я не знаю, что сказать."));
						return;
					}
					int counter = queueLength + 1;
					int r;
					Quote quote;
					do {
						counter--;
						r = new Random().nextInt(sq.size());
						quote = sq.get(r);
						int i, j = curPos - 1;
						for (i = 0; i < counter; i++, j--) {
							if (j < 0) {
								j = j + queueLength;
							}
							if (lastUsed[j] == quote) {
								break;
							}
						}
						if (i == counter) {
							break;
						}
					} while (counter > 0);
					lastUsed[curPos] = quote;
					curPos = (curPos + 1) % queueLength;
					quote.UpdateLastUsage();
					callback.call(quote);
					return;
				}
		);
	}
	public interface GetQuoteCallback{
    	void call(Quote quote);
	}
	public Quote get(int index) {
		return suitableQuotes.get(index);
	}
	
	public int size() {
		return suitableQuotes.size();
	}
	
	public void clear() {
		packs = new ArrayList<>();
		suitableQuotes = new ArrayList<>();
		lastUsed = new Quote[queueLength];
	}

	public static void saveTo(String URL, String filename) {
		try {
			URL DATA_URL = new URL(URL);
			InputStream stream = DATA_URL.openStream();
			JSONObject json = new JSONObject(IOUtils.toString(stream, "UTF-8"));
			stream.close();
			JSONArray array = json.getJSONArray("values"), phrase;
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.newDocument();
			Node mainNode = doc.createElement("quotes");
			for (int i = 0; i < array.length(); i++) {
				try {
					phrase = array.getJSONArray(i);
					if (phrase.length() == 0) {
						break;
					}
					Quote next = null;
					int[][] range_values = new int[CharacterSystem.featureCount][2];
					for (int k = 0; k < CharacterSystem.featureCount; k++) {
						range_values[k] = new int[]{-10, 10};
					}
					for (int k = 0; k < phrase.length() && k < 11; k++) {
						switch (k) {
							case 0:
								next = new Quote(phrase.getString(k));
								break;
							case 1:
								if (phrase.getString(k).length() > 0) {
									next.spriteType = phrase.getString(k).replace("\n", "");
								}
								break;
							case 2:
								if (phrase.getString(k).length() > 0) {
									next.purposeType = phrase.getString(k).replace("\n", "");
								}
								break;
							case 3:
							case 4:
							case 5:
							case 6:
							case 7:
							case 8: {
								String[] sp = phrase.getString(k).split(" \\| ");
								int a1 = -10, a2 = 10;
								try {
									a1 = Integer.valueOf(sp[0]);
								} catch (Exception e1) {
									a1 = -10;
								}
								try {
									a2 = Integer.valueOf(sp[1]);
								} catch (Exception e2) {
									a2 = 10;
								}
								range_values[k - 3] = new int[]{a1, a2};
							}
							break;
							case 9:
								try {
									next.timeout = array.getInt(k);
								} catch (Exception u) { }
								break;
							case 10: {
								if (phrase.length()<14) continue;
								if (phrase.getString(13).length() == 0) continue;
								next.setTags(phrase.getString(13));
							} break;
						}
					}
					next.character = new CharacterRange(range_values);
					mainNode.appendChild(next.toXMLNode(doc));
				} catch (Exception u) {
					Main.log(u);
				}
			}
			doc.appendChild(mainNode);
			Path address = Main.getDataDirPath().resolve(filename + ".quotes");
			try {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				tr.transform(new DOMSource(doc), new StreamResult(Files.newOutputStream(address)));
			} catch (Exception er) {
				Main.log("Error while rewriting file " + filename + ".quotes" + ": " + er);
			}
		} catch (Exception e) {
			Main.log(e);
			return;
		}
	}
	
	public void saveTo(Quote quote, String file) {
		Document doc;
		Node mainNode;
		Path address = Main.getDataDirPath().resolve(file + ".quotes");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		boolean newFile = false;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			// create instance of DOM
			try {
				doc = db.parse(Files.newInputStream(address));
				mainNode = doc.getChildNodes().item(0);
			} catch (Exception er) {
				Main.log("Error while reading file " + file + ".quotes" + ": " + er);
				doc = db.newDocument();
				newFile = true;
				mainNode = doc.createElement("quotes");
			}
		} catch (Exception er) {
			Main.log("Error while creating parser for file " + file + ".quotes" + ": " + er);
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
			Main.log("Error while rewriting file " + file + ".quotes" + ": " + er);
		}
	}
}
