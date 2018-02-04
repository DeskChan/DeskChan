class Spliter {
    static def split(text) {
        def textParts = []
        def partNumber = 0
        textParts[partNumber] = ""
        // This variable have true, if last symbol is "." or "!" or "?".
        def lastIsNotLetter = false
        for(def i : text) {
                if(i == "," || i == ";") {
                    textParts[partNumber] += i
                    partNumber++
                    textParts[partNumber] = ""
                    lastIsNotLetter = false
                }
                else if(i == ".") {
                    if(!lastIsNotLetter) {
                        textParts[partNumber] += i
                        partNumber++
                        textParts[partNumber] = ""
                        lastIsNotLetter = true
                    }
                    else
                        textParts[partNumber - 1] += i
                }
                else if(i == "!") {
                    if(!lastIsNotLetter) {
                        textParts[partNumber] += i
                        partNumber++
                        textParts[partNumber] = ""
                        lastIsNotLetter = true
                    }
                    else
                        textParts[partNumber - 1] += i
                }
                else if(i == "?") {
                    if(!lastIsNotLetter) {
                        textParts[partNumber] += i
                        partNumber++
                        textParts[partNumber] = ""
                        lastIsNotLetter = true
                    }
                    else
                        textParts[partNumber - 1] += i
                }
                else {
                    textParts[partNumber] += i
                    lastIsNotLetter = false
                }
            }
        return textParts
    }
}
