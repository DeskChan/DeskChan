class Nekonization {
    def bridge
    void initialize(bridge){  this.bridge = bridge  }

    static def insertions = [
            "ня", "нян", "мур", "мяу",
    ]

    boolean checkActive(Map preset){
        boolean active
        try {
            String species = (String) preset.get("tags").get("species")
            active = species.contains("neko") && !species.contains("!neko")
        } catch (Exception e) {
            active = false
        }
        return active
    }

    Map morphPhrase(Map phrase){
        def text = phrase.get("text")
        text = bridge.insert(text, insertions)
        phrase.put("text", text)
        return phrase
    }

    String getName(String locale){
        switch (locale){
            case "ru": return "Няканье"
            default:   return "Nyans"
        }
    }
}