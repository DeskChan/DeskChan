class Culturing {
    def bridge
    void initialize(bridge){  this.bridge = bridge  }

    static def insertions = [
            "ня", "нян", "мур", "мяу",
    ]

    boolean active = true
    void setPreset(Map preset){
        try {
            String species = (String) preset.get("tags").get("species")
            active = species.contains("neko") && !species.contains("!neko")
        } catch (Exception e) {
            active = false
        }
    }

    Map morphPhrase(Map phrase){
        println(active)
        if (!active) return phrase
        def text = phrase.get("text")
        text = bridge.insert(text, insertions)
        println(text)
        phrase.put("text", text)
        return phrase
    }

    String getName(Locale locale){
        switch (locale){
            case new Locale("ru"): return "Няканье"
            case new Locale("en"): return "Nyans"
        }
    }
}