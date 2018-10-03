package info.deskchan.talking_system;

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
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PhrasesPack {

    public enum PackType { USER, PLUGIN, INTENT_DATABASE }

    protected PackType packType;
    protected File packFile;
    protected String packName;
    protected List<Phrase> phrases = new ArrayList<>();
    protected boolean loaded;

    public PhrasesPack(String file, PackType packType) {
        packName = file;
        packFile = new File(file);
        this.packType = packType;

        if (!packFile.isAbsolute())
            packFile = Main.getPhrasesDirPath().resolve(packFile.toString());

        if (!packFile.getName().contains(".")){
            if (new File(packFile.toString() + ".phrases").exists()) {
                packFile = new File(packFile.toString() + ".phrases");
            } else if (new File(packFile.toString() + ".database").exists()){
                packFile = new File(packFile.toString() + ".database");
            }

        }

        if (packName.contains(".")) {
            if (packName.endsWith(".database")) this.packType = PackType.INTENT_DATABASE;
            packName = packName.substring(0, packName.lastIndexOf("."));
        }

        loaded = false;
    }

    public PackType getPackType() {
        return packType;
    }

    public void load(){
        try {
            String defaultLanguage = "ru"; //Locale.getDefault().toLanguageTag();

            phrases.clear();
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setValidating(false);

            DocumentBuilder builder = f.newDocumentBuilder();
            InputStream inputStream = new FileInputStream(packFile);
            Document doc = builder.parse(inputStream);
            inputStream.close();
            Node mainNode = doc.getChildNodes().item(0);
            for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
                if (mainNode.getChildNodes().item(i).getNodeName().equals(defaultLanguage)){
                    mainNode = mainNode.getChildNodes().item(i);
                    break;
                }
            }
            NodeList list = mainNode.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                if (!list.item(i).getNodeName().equals("phrase")) continue;
                try {
                    add(Phrase.create(list.item(i)));
                } catch (Exception e2) {
                    Main.log(e2);
                }
            }
        } catch (FileNotFoundException e) {
            loaded = false;
            return;
        } catch (Exception e) {
            Main.log(e);
            loaded = false;
            return;
        }
        loaded = true;
    }

    public void save(){
        String defaultLanguage = "ru"; //Locale.getDefault().toLanguageTag();

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setValidating(false);
        DocumentBuilder builder;
        Document doc;
        Node phrasesNode;
        try {
            builder = f.newDocumentBuilder();
        } catch (Exception e){
            Main.log(e);
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(packFile);
            doc = builder.parse(inputStream);
            inputStream.close();
            phrasesNode = doc.getChildNodes().item(0);
            for (int i = 0; i < phrasesNode.getChildNodes().getLength(); i++) {
                if (phrasesNode.getChildNodes().item(i).getNodeName().equals(defaultLanguage)){
                    phrasesNode = phrasesNode.getChildNodes().item(i);
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            doc = builder.newDocument();
            Node p = doc.createElement("phrases");
            doc.appendChild(p);
            phrasesNode = doc.createElement(defaultLanguage);
            p.appendChild(phrasesNode);
        } catch (Exception e) {
            Main.log(e);
            return;
        }

        while (phrasesNode.getChildNodes().getLength() > 0)
            phrasesNode.removeChild(phrasesNode.getFirstChild());

        for (Phrase phrase : getPhrases())
            phrasesNode.appendChild(phrase.toXMLNode(doc));

        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            tr.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(packFile)));
        } catch (Exception er) {
            Main.log("Error while rewriting file " + packFile + ": " + er);
        }
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

    public List<Phrase> getPhrases(){ return phrases; }

    @Override
    public boolean equals(Object other){
        if (other.getClass() != this.getClass()) return false;
        return packFile.equals(((PhrasesPack) other).packFile);
    }

    public void printPhrasesLack(String intent){
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
                if (phrase.hasIntent(intent) && phrase.matchToCharacter(current)) matchingPhrasesCount++;

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
