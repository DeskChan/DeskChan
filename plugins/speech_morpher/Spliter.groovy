class Spliter {
    static final String marks = '.!?'
    static def split(text) {
        if (!marks.contains(text[text.length() - 1]))
            text += '.'
        text += " "
        def sentencePart = ""
        def sentence = []
        def sentences = []
        char last = 0
        for(char c : text) {
            if(c == "," || c == ";") {
                sentence += sentencePart.trim()
                sentence += c.toString()
                sentencePart = ""
                last = c
                continue
            } else if(marks.contains(c.toString())) {
                if (!marks.contains(last.toString())){
                    sentence += sentencePart.trim()
                    sentencePart = ""
                }
            } else if (marks.contains(last.toString())){
                sentence += sentencePart.trim()
                sentencePart = ""
                sentences.add(sentence)
                sentence = []
            }
            sentencePart += c
            last = c
        }
        return sentences
    }
}
