package name.blackcap.socialbutterfly.gui

import name.blackcap.socialbutterfly.lib.clear
import java.awt.Dimension
import javax.swing.*

/* Prompt for an encryption/decryption key. We ask twice when prompting the
   first time. A KeyChangeDialog is used for changing a key. We do not do
   in-dialog input validation here because doing so is troublesome in all
   cases (rejecting incorrect decryption keys requires doing I/O and
   attempting a decryption). */
class KeyDialog(owner: JFrame, askTwice: Boolean = false) : JDialog(owner) {
    private val keyField = JPasswordField(MAX_CHARS).apply {
        border = BorderFactory.createEmptyBorder(0, BW2, BW2, BW2)
        alignmentX = LEFT_ALIGNMENT
    }
    private var otherKeyField: JPasswordField? = null

    val key: CharArray
        get() = keyField.password
    val otherKey: CharArray
        get() = otherKeyField?.password ?: throw IllegalStateException("no other key in this dialog")

    init {
        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        isModal = true  /* ick, but really need a key */
        lateinit var keyType: String
        if (askTwice) {
            keyType = "encryption"
            title = "Enter Encryption Key"
            add(JLabel().apply {
                text = "To protect sensitive data, it must be encrypted."
                border = BorderFactory.createEmptyBorder(BW2, BW2, 0, BW2)
                alignmentX = LEFT_ALIGNMENT
            })
        } else {
            keyType = "decryption"
            title = "Enter Decryption Key"
        }
        add(JLabel("Enter $keyType key:").apply {
            border = BorderFactory.createEmptyBorder(BW2, BW2, 0, BW2)
            alignmentX = LEFT_ALIGNMENT
        })
        add(keyField)
        if (askTwice) {
            add(JLabel("Enter $keyType key again:").apply {
                border = BorderFactory.createEmptyBorder(0, BW2, 0, BW2)
                alignmentX = LEFT_ALIGNMENT
            })
            otherKeyField = JPasswordField(MAX_CHARS).apply {
                border = BorderFactory.createEmptyBorder(0, BW2, BW2, BW2)
                alignmentX = LEFT_ALIGNMENT
            }
            add(otherKeyField)
        }
        add(JPanel(). apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, BW2, BW2, BW2)
            alignmentX = LEFT_ALIGNMENT
            add(Box.createHorizontalGlue())
            add(JButton("OK").apply {
                alignmentX = CENTER_ALIGNMENT
                addActionListener { dismiss() }
            })
            add(Box.createHorizontalGlue())
        })
        pack()
    }

    fun dismiss() {
        isVisible = false
    }

    fun reset() {
        keyField.password.clear()
        keyField.text = ""
        otherKeyField?.password?.clear()
        otherKeyField?.text = ""
    }

    override fun dispose() {
        reset()
        super.dispose()
    }

    companion object {
        const val MAX_CHARS = 50
        const val BW = 9
        const val BW2 = BW * 2
    }
}
