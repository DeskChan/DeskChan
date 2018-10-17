package info.deskchan.MessageData.GUI

open class Control : HashMap<String, Any> {

    protected constructor()

    constructor(type: ControlType) : this(type, null, null, null, null)

    constructor(type: ControlType, id: String) : this(type, id, null, null, null)

    constructor(type: ControlType, id: String?, value: Any) : this(type, id, value, null, null)

    constructor(type: ControlType, id: String?, value: Any?, label: String) : this(type, id, value, label, null)

    constructor(type: ControlType, id: String?, value: Any?, label: String?, hint: String?){
        setType(type)
        setId(id)
        setValue(value)
        setLabel(label)
        setHint(hint)
    }

    constructor(type: ControlType, data: Map<String, Any>) : this(type){
        data.forEach { t, u -> if (u != null) put(t,u) }
    }

    constructor(type: ControlType, id: String, value: Any?, label: String?, hint: String?, data: Map<String, Any>) : this(type, id, value, label, hint){
        data.forEach { t, u -> if (u != null) put(t,u) }
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
         * - enterTag: String -> MessageListener
         * - onFocusLostTag: String -> MessageListener
         * - onChangeTag: String -> MessageListener */
        TextField,

        /** Spinner (or IntSpinner)
         * - value: int
         * - min: int
         * - max: int
         * - step: int
         * - msgTag: String -> MessageListener */
        IntSpinner,
        Spinner,

        /** FloatSpinner
         * - value: float
         * - min: float
         * - max: float
         * - step: float
         * - msgTag: String -> MessageListener */
        FloatSpinner,

        /** Slider
         * - value: float
         * - min: float
         * - max: float
         * - step: double
         * - msgTag: String -> MessageListener */
        Slider,

        /** CheckBox
         * - value: boolean - Is checked
         * - msgTag: String -> MessageListener */
        CheckBox,

        /** ComboBox
         * - value: int
         * - values: List of String
         * - valuesNames: List of String
         * - msgTag: String -> MessageListener */
        ComboBox,

        /** ListBox
         * - value: int or List of int - One or more selected items
         * - values: List of String
         * - msgTag: String -> MessageListener */
        ListBox,

        /** Button
         * - value: String - Button text
         * - msgTag: String -> MessageListener
         * - msgData: Any
         * - dstPanel: String */
        Button,

        /** FileField
         * - value: String - File name
         * - msgTag: String -> MessageListener
         * - initialDirectory: String -> File
         * - filters: List of
         *     * extensions: List of String
         *     * description: String */
        FileField,

        /** DirectoryField
         * - value: String - Directory name
         * - msgTag: String -> MessageListener
         * - initialDirectory: String -> File */
        DirectoryField,

        /** DatePicker
         * - value: String - Date in format specified. Format by default: ISO_LOCAL_DATE
         * - msgTag: String -> MessageListener
         * - format: String -> DateTimeFormatter */
        DatePicker,

        /** FilesManager
         * - value: List of String - Files selected
         * - multiple: Boolean
         * - onChange: String -> MessageListener */
        FilesManager,

        /** AssetsManager
         * - value: List of String - Assets selected
         * - multiple: Boolean
         * - folder: String
         * - acceptedExtensions: List of String
         * - moreURL: String -> URL
         * - onChange: String -> MessageListener */
        AssetsManager,

        /** TextArea
         * - value: String - Text
         * - rowCount: int */
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
         * - msgTag: String -> MessageListener */
        ColorPicker,

        /** FontPicker
         * - value: String -> "Font Family, Size"
         * - msgTag: String -> MessageListener */
        FontPicker,

        /** Separator */
        Separator,

        /** Hyperlink
         * - value: String -> URL
         * - msgTag: String -> MessageListener */
        Hyperlink
    }
}