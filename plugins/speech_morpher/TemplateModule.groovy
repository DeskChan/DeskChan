class TemplateModule {

    // use this two lines to access other plugin tools like
    //  - Splitter: split textx to sentences and it's parts
    //  - Insertion: random insertion of word to sentence
    def bridge
    void initialize(bridge){  this.bridge = bridge  }

    // You receive all information about character and choose if morpher should be activated or deactivated
    boolean checkActive(Map preset){
        characteristicSum = Math.max(preset.get("energy"), 0) + Math.max(preset.get("impulsivity"), 0)
        return true
    }

    // Morph phrase here and return it back
    Map morphPhrase(Map phrase){

        return phrase
    }

    // Name of module to display in options
    String getName(String locale){
        switch (locale){
            case "ru": return "Модуль"
            default:   return "Module"
        }
    }
}