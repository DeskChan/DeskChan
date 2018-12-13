package info.deskchan.MessageData.Core

import info.deskchan.core.MessageData

/**
 * Set program language
 * Due to architecture problems, program needs to be rerun to apply changes
 *
 * @property value Language tag
 */
@MessageData.Tag("core:set-language")
class SetLanguage : MessageData {

    private val value: String

    constructor(value: LanguageTag){
        this.value = value.repr
    }

    enum class LanguageTag(val repr: String) {
        RU("ru"), EN("en")
    }

    fun getValue(){
        LanguageTag.valueOf(value.toUpperCase())
    }
}