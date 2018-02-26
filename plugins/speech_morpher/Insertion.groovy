class Insertion {
    static final def random = new Random(System.currentTimeMillis())

    static String insert(text, insertions) {

        def textParts = Spliter.split(text)

        def sentencePos = random.nextInt(textParts.size())
        def sentence = textParts[sentencePos]

        def pos = random.nextInt((int) (sentence.size()/2 + 1))

        def insertion = insertions[random.nextInt(insertions.size())]
        if (pos == 0) {
            String ins = insertion[0].toUpperCase() + insertion.substring(1)
            sentence = [ins, ","] + sentence
        } else if (pos >= sentence.size() / 2)
            sentence = sentence[0..sentence.size()-2] + "," + insertion + sentence[sentence.size()-1]
        else
            sentence = sentence[0..pos*2-1] + insertion + "," + sentence[pos*2..sentence.size()-1]

        textParts[sentencePos] = sentence
        StringBuilder sb = new StringBuilder()
        for (String[] s : textParts) {
            boolean b = false
            for (String s1 : s) {
                sb.append(s1)
                if (b)
                    sb.append(" ")
                b = !b
            }
            sb.append(" ")
        }
        return sb.toString()
    }
}