import static SomeNumbers.*

['123', 'test', 'blah blah', 'abcdefghijklmno'].forEach {
    def foo = when(it) {
        match { it.length() > 10 } { "too long" }
        match("123") { "Numbers! $it" }
        match("test") { "test" }
        otherwise { "none of them" }
    }
    println(foo)
}


enum SomeNumbers { ONE, TWO, THREE }

[TWO, "Ping"].forEach {
    when(it) {
        match(ONE) { println(1) }
        match(TWO) { println(2) }
        match(THREE) { println(3) }
        match("Ping") { println("Pong") }
    }
}
