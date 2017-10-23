package info.deskchan.groovy_support

import java.util.function.Function


static Object when(obj, @DelegatesTo(CaseCollector) Closure cl) {
    def caseCollector = new CaseCollector()
    cl.delegate = caseCollector
    cl()
    return caseCollector.execute(obj)
}


class CaseCollector<T> {

    private LinkedList<Function<T, Boolean>> predicates = new LinkedList<>()
    private Map<Function<T, Boolean>, Function<T, Object>> matches = new HashMap<>()

    private def match_impl(Function<T, Boolean> predicate, Function<T, Object> action) {
        predicates += predicate
        matches[predicate] = action
    }

    def match(T obj, Function<T, Object> action) {
        if (obj instanceof GroovyCallable) {
            match_impl(obj as Function, action)
        } else {
            match_impl({ it == obj } as Function, action)
        }
    }

    def otherwise(Function<T, Object> action) {
        match_impl({ true } as Function, action)
    }

    Object execute(T key) {
        def action = predicates.find { it.apply(key) }
    	if (action != null) {
            return matches[action].apply(key)
        }
        return null
    }

}
