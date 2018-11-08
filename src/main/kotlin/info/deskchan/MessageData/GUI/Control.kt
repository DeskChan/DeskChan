package info.deskchan.MessageData.GUI

open class Control : HashMap<String, Any> {

    protected constructor()

    constructor(type: ControlType) : this(type, null, null)

    constructor(type: ControlType, data: Map<String, Any>) : this(type, null, null, data)

    constructor(type: ControlType, id: String) : this(type, id, null)

    constructor(type: ControlType, id: String?, value: Any?){
        setType(type)
        setId(id)
        setValue(value)
    }

    constructor(type: ControlType, id: String, value: Any?, data: Map<String, Any>) : this(type, id, value){
        data.forEach { t, u -> if (u != null) put(t,u) }
    }

    constructor(type: ControlType, id: String?, value: Any?, vararg data: Any?) : this(type, id, null) {
        var i = 0
        while (i < data.size) {
            if (data[i+1] != null)
                put(data[i].toString(), data[i + 1]!!)
            i += 2
        }
    }

    fun getId() = get("id").toString()
    fun setId(value: String?){
        if (value != null) put("id", value)
    }

    fun getValue() = get("value")
    fun setValue(value: Any?){
        if (value != null) put("value", value)
    }

    fun getLabel() = get("label").toString()
    fun setLabel(value: String?){
        if (value != null) put("label", value)
    }

    fun getHint() = get("hint").toString()
    fun setHint(value: String?){
        if (value != null) put("hint", value)
    }

    fun getType() = if (containsKey("type")) ControlType.valueOf(get("type").toString()) else null
    fun setType(value: ControlType?){
        put("type", value.toString())
    }

    enum class ControlType {

        /** Label
         * - value: String
         * - font: String
         * - align: String -> javafx.geometry.Pos **/
        Label,

        /** TextField
         * - value: String
         * - hideText: boolean
         * - editable: boolean
         * - enterTag: String -> MessageListener (String)
         * - onFocusLostTag: String -> MessageListener (String)
         * - onChangeTag: String -> MessageListener (String) */
        TextField,

        /** Spinner (or IntSpinner)
         * - value: int
         * - min: int
         * - max: int
         * - step: int
         * - msgTag: String -> MessageListener (Int) */
        IntSpinner,
        Spinner,

        /** FloatSpinner
         * - value: float
         * - min: float
         * - max: float
         * - step: float
         * - msgTag: String -> MessageListener (Double) */
        FloatSpinner,

        /** Slider
         * - value: float
         * - min: float
         * - max: float
         * - step: double
         * - msgTag: String -> MessageListener (Double) */
        Slider,

        /** CheckBox
         * - value: boolean - Is checked
         * - msgTag: String -> MessageListener (Boolean) */
        CheckBox,

        /** ComboBox
         * - value: int
         * - values: List of String
         * - valuesNames: List of String
         * - msgTag: String -> MessageListener (String) */
        ComboBox,

        /** ListBox
         * - value: int or List of int - One or more selected items
         * - values: List of String
         * - msgTag: String -> MessageListener */
        ListBox,

        /** Button
         * - value: String - Button text
         * - msgTag: String -> MessageListener (msdData)
         * - msgData: Any
         * - dstPanel: String */
        Button,

        /** FileField
         * - value: String - File name
         * - msgTag: String -> MessageListener (String)
         * - initialDirectory: String -> File
         * - filters: List of
         *     * extensions: List of String
         *     * description: String */
        FileField,

        /** DirectoryField
         * - value: String - Directory name
         * - msgTag: String -> MessageListener (String)
         * - initialDirectory: String -> File */
        DirectoryField,

        /** DatePicker
         * - value: String - Date in format specified. Format by default: ISO_LOCAL_DATE
         * - msgTag: String -> MessageListener (String)
         * - format: String -> DateTimeFormatter */
        DatePicker,

        /** FilesManager
         * - value: List of String - Files selected
         * - multiple: Boolean
         * - onChange: String -> MessageListener (String or String[]) */
        FilesManager,

        /** AssetsManager
         * - value: List of String - Assets selected
         * - multiple: Boolean
         * - folder: String
         * - acceptedExtensions: List of String
         * - moreURL: String -> URL
         * - onChange: String -> MessageListener (String or String[]) */
        AssetsManager,

        /** TextArea
         * - value: String - Text
         * - rowCount: int
         * - enterTag: String -> MessageListener (String)
         * - onFocusLostTag: String -> MessageListener (String)
         * - onChangeTag: String -> MessageListener (String) */
        TextArea,

        /** CustomizableTextArea
         * - value: List of
         *     * text: String
         *     * color: String -> javafx.scene.paint.Paint
         *     * font: String -> Font
         *     * id: String
         *     * style: String -> Font */
        CustomizableTextArea,

        /** ColorPicker
         * - value: String -> javafx.scene.paint.Color
         * - msgTag: String -> MessageListener (String) */
        ColorPicker,

        /** FontPicker
         * - value: String -> "Font Family, Size"
         * - msgTag: String -> MessageListener (String) */
        FontPicker,

        /** Separator */
        Separator,

        /** Hyperlink
         * - value: String -> URL
         * - msgTag: String -> MessageListener (null) */
        Hyperlink
    }
}