package info.deskchan.talking_system;

import java.util.*;

public class PhraseChooser {

    private static LimitArrayList<MatchingPhrase> lastUsed = new LimitArrayList<>();
    protected static List<MatchingPhrase> matchingPhrases;
    private static int presetHash = -1, pluginTagsHash = -1;

    protected static Map<String, TagListener> systemTagListeners = new HashMap<String, TagListener>(){{
        put("lastConversation", DefaultTagsListeners.lastConversationListener);
        put("possibleHour", DefaultTagsListeners.dateListener);
        put("possibleMinute", DefaultTagsListeners.dateListener);
        put("possibleDay", DefaultTagsListeners.dateListener);
        put("possibleDayOfWeek", DefaultTagsListeners.dateListener);
        put("possibleMonth", DefaultTagsListeners.dateListener);
        put("emotion", null);  // "emotion" tag is checked inside EmotionController
    }};

    protected static IntentList chatIntents = new IntentList(
            "CHAT", "WHAT_YOUR_STATE", "WHAT_ARE_YOU_DOING", "WHAT_YOUR_RELATION", "AFFECTION_REQUEST",
            "PROPOSAL", "THREAT", "DREAMING", "FEELING_REPORT", "WHAT_YOUR_RELATION",
            "JOKE", "ASK_EXIT", "RELATION", "AFFECTION_ACTION"	);

    public static Phrase get (
            IntentList requestedIntents,
            CharacterPreset character,
            TagsMap pluginsTags,
            TagsMap additionalTags
    ){
        int ph = character.hashCode(), pth = pluginsTags.dataHashCode();
        if (presetHash != ph || pluginTagsHash != pth){
            presetHash = ph;
            pluginTagsHash = pth;
            matchingPhrases = recalculateMatchingPhrases(character, pluginsTags);
        }

        if (requestedIntents == null || requestedIntents.size() == 0)
            requestedIntents = chatIntents;

        if (matchingPhrases.size() == 0)
            return new Phrase(Main.getString("phrase." + requestedIntents.get(0)));

        List<MatchingPhrase> matchingList = getCurrentlyMatchingPhrases(requestedIntents, character, pluginsTags, additionalTags);

        if (matchingList.size() == 0)
            return new Phrase(Main.getString("phrase." + requestedIntents.get(0)));

        int counter = LimitArrayList.LIMIT + 1;
        MatchingPhrase phrase;
        do {
            counter--;
            int r = new Random().nextInt(matchingList.size());
            phrase = matchingList.get(r);
        } while (counter > 0 && lastUsed.contains(phrase));

        lastUsed.add(phrase);
        phrase.updateLastUsage();
        return phrase.phrase;
    }

    protected static List<MatchingPhrase> getCurrentlyMatchingPhrases(
            IntentList requestedIntents,
            CharacterPreset character,
            TagsMap pluginsTags,
            TagsMap additionalTags
    ){
        List<MatchingPhrase> matchingList = new ArrayList<>();
        for (MatchingPhrase phrase : matchingPhrases) {

            if (additionalTags != null && !phrase.leftTags.match(additionalTags)) continue;
            TagsMap leftTags = new TagsMap(phrase.leftTags);
            if (additionalTags != null)
                for (String key : additionalTags.keySet())
                    leftTags.remove(key);
            Set<TagListener> applied = new HashSet<>();

            for (Map.Entry<String, TagListener> listener : systemTagListeners.entrySet()){
                if (listener.getValue() == null) continue;
                if (leftTags.containsKey(listener.getKey())){
                    leftTags.remove(listener.getKey());
                    if (applied.contains(listener.getValue())) continue;
                    if (listener.getValue().match(phrase.phrase)){
                        applied.add(listener.getValue());
                    } else {
                        applied = null;
                        break;
                    }
                }
            }

            if (leftTags.size() > 0 || applied == null) continue;

            for (String intent : requestedIntents) {
                if (!phrase.phrase.hasIntent(intent)) continue;

                matchingList.add(phrase);
                break;
            }
        }
        return matchingList;
    }

    protected static List<MatchingPhrase> recalculateMatchingPhrases(CharacterPreset character,
                                            TagsMap pluginsTags){
        List<MatchingPhrase> matchingPhrases = new LinkedList<>();

        List<Phrase> phrases = character.phrases.toPhrasesList(PhrasesPack.PackType.USER);
        CharacterController cc = character.emotionState.construct(character.character);
        EmotionsController ec = character.emotionState;

        for (Phrase phrase : phrases){
            MatchingPhrase matchingPhrase = MatchingPhrase.get(phrase, cc, ec, character.tags, pluginsTags);
            if (matchingPhrase != null)
                matchingPhrases.add(matchingPhrase);
        }
        return matchingPhrases;
    }

    interface TagListener {
        boolean match(Phrase phrase);
    }

    protected static class MatchingPhrase {
        Phrase phrase;
        Long lastUsage = null;
        TagsMap leftTags;
        int timeout;

        private MatchingPhrase(Phrase phrase, TagsMap nonMatchingTags){
            this.phrase = phrase;
            this.leftTags = nonMatchingTags;
            timeout = phrase.getTimeout();
        }

        public static MatchingPhrase get(
                Phrase phrase, CharacterController cc, EmotionsController ec, TagsMap... tags){

            if (!cc.phraseMatches(phrase) || !ec.phraseMatches(phrase)) return null;

            TagsMap nonMatching = checkTagsMatching(phrase.tags, tags);
            if (nonMatching == null) {
                return null;
            }
            return new MatchingPhrase(phrase, nonMatching);
        }

        /** Checks if origin is compatible with dest tags maps.
         *  If it has some incompatible tags, returns null.
         *  Else, if origin has tags that not specified in dest, returns these tags.
         */
        protected static TagsMap checkTagsMatching(TagsMap origin, TagsMap... dest){
            TagsMap nonMatching = new TagsMap();
            if (origin == null) return nonMatching;
            for (Map.Entry<String, Set<String>> entry : origin.entrySet()){
                boolean match = false;
                for (TagsMap v : dest){
                    if (v.match(entry.getKey(), entry.getValue())) {
                        match = true;
                        break;
                    } else {
                        if (v.containsKey(entry.getKey()) || v.containsKey("!" + entry.getKey()))
                            return null;
                    }
                }
                if (!match)
                    nonMatching.put(entry.getKey(), entry.getValue());
            }
            return nonMatching;
        }

        public boolean noTimeout() {
            if (lastUsage == null || timeout <= 0) {
                return true;
            }
            return (new Date().getTime() - lastUsage > timeout * 1000);
        }

        public void updateLastUsage() {
            lastUsage = new Date().getTime();
        }

    }

    private static class LimitArrayList<E> extends ArrayList<E>{
        protected static final int LIMIT = 30;
        @Override
        public boolean add(E object){
            while (size() >= LIMIT) remove(0);
            return super.add(object);
        }
    }
}
