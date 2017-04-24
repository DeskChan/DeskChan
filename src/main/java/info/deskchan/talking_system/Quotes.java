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
import java.util.*;

public class Quotes {
	CharacterDefinite current = new CharacterDefinite();
	private int queueLength = 30;
	private int curPos = 0;
	Quote[] lastUsed = new Quote[queueLength];
	ArrayList<Quote> quotes = new ArrayList();
	ArrayList<Quote> suitableQuotes = new ArrayList();
	
	public void add(Quote quote) {
		if (quote == null) {
			return;
		}
		for (Quote q : quotes) {
			if (q.toString().equals(quote.toString())) {
				return;
			}
		}
		quotes.add(quote);
		if (quote.matchToCharacter(current)) {
			suitableQuotes.add(quote);
		}
	}
	
	public void update(CharacterDefinite newCharacter) {
		if (current.equal(newCharacter)) {
			return;
		}
		current = newCharacter;
		update();
	}
	
	public void update() {
		suitableQuotes = new ArrayList();
		for (int i = 0, l = quotes.size(); i < l; i++) {
			if (quotes.get(i).matchToCharacter(current)) {
				suitableQuotes.add(quotes.get(i));
			}
		}
		
	}
	
	public ArrayList<Quote> GetMatching(CharacterDefinite target) {
		ArrayList<Quote> sq = new ArrayList();
		for (int i = 0, l = quotes.size(); i < l; i++) {
			if (quotes.get(i).matchToCharacter(target)) {
				sq.add(quotes.get(i));
			}
		}
		return sq;
	}
	
	public Quote getRandomQuote(String purpose) {
		purpose = purpose.toUpperCase();
		if (suitableQuotes.size() == 0) {
			return new Quote("Я не знаю, что сказать.");
		}
		int r;
		Quote q;
		LinkedList<Quote> sq = new LinkedList<>();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		for (int i = 0; i < suitableQuotes.size(); i++) {
			q = suitableQuotes.get(i);
			int dow = cal.get(Calendar.DAY_OF_WEEK);
			if (dow == 1) {
				dow = 7;
			} else {
				dow--;
			}
			if (q.noTimeout() && q.purposeType.equals(purpose) && q.possibleMonth.get(1 + cal.get(Calendar.MONTH)) && q.possibleHour.get(cal.get(Calendar.HOUR_OF_DAY)) && q.possibleWeekDay.get(dow)) {
				sq.add(q);
			}
		}
		
		if (sq.size() == 0) {
			return new Quote("Я не знаю, что сказать.");
		}
		int counter = queueLength + 1;
		do {
			counter--;
			r = new Random().nextInt(sq.size());
			q = sq.get(r);
			int i,j=curPos-1;
			for (i = 0; i < counter; i++,j--) {
				if(j<0)
					j=j+queueLength;
				if (lastUsed[j] == q)
					break;
			}
			if(i==counter) break;
		} while (counter > 0);
		lastUsed[curPos] = q;
		curPos = (curPos + 1) % queueLength;
		q.UpdateLastUsage();
		return q;
	}
	
	public Quote get(int index) {
		return suitableQuotes.get(index);
	}
	
	public int size() {
		return suitableQuotes.size();
	}
	
	public void clear() {
		quotes = new ArrayList();
		suitableQuotes = new ArrayList();
		lastUsed = new Quote[queueLength];
	}
	
	public void load(Path path, ArrayList<String> files) {
		DocumentBuilder builder;
		try {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			f.setValidating(false);
			builder = f.newDocumentBuilder();
		} catch (Exception e) {
			Main.log("Error while starting quotes parser: " + e);
			return;
		}
		
		for (String file : files) {
			try {
				InputStream inputStream = Files.newInputStream(path.resolve(file + ".quotes"));
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
				Main.log("Error while loading file " + file + ".quotes" + ": " + e);
			}
		}
		Main.log("Loaded quotes: " + quotes.size() + " " + suitableQuotes.size());
		update();
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
					for (int k = 0; k < phrase.length() && k < 13; k++) {
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
								} catch (Exception u) {
								}
								break;
							case 10:
								if (phrase.getString(k).length() > 0) {
									next.possibleHour.fillFromString(phrase.getString(k));
								}
								break;
							case 11:
								if (phrase.getString(k).length() > 0) {
									next.possibleWeekDay.fillFromString(phrase.getString(k));
								}
								break;
							case 12:
								if (phrase.getString(k).length() > 0) {
									next.possibleMonth.fillFromString(phrase.getString(k));
								}
								break;
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
