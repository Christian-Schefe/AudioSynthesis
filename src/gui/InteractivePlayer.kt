package gui

import app.applyEffects
import app.readInstruments
import effects.Effect
import node.*
import node.composite.Pipeline
import playback.AudioPlayer
import java.awt.Graphics
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.math.pow

class InteractivePlayer(val ctx: Context, node: AudioNode, effects: List<Effect>) : JFrame() {
    private val keyComponents = mutableListOf<KeyComponent>()
    private val settingsNodes = mutableListOf<SettingsNode>()
    private var octave = 0

    init {
        title = "Interactive Player"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = null
        setSize(1200, 400)
        setLocationRelativeTo(null)

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                keyDown(e.keyCode, e.keyChar.lowercaseChar())
            }

            override fun keyReleased(e: KeyEvent) {
                keyUp(e.keyChar.lowercaseChar())
            }
        })

        val keys = listOf(
            'a' to (0 to 0),
            'w' to (0 to 1),
            's' to (1 to 0),
            'e' to (1 to 1),
            'd' to (2 to 0),
            'f' to (3 to 0),
            't' to (3 to 1),
            'g' to (4 to 0),
            'z' to (4 to 1),
            'h' to (5 to 0),
            'u' to (5 to 1),
            'j' to (6 to 0),
            'k' to (7 to 0),
            'o' to (7 to 1),
            'l' to (8 to 0),
            'p' to (8 to 1),
            'ö' to (9 to 0),
        )

        keys.forEachIndexed { index, key ->
            val settingsNode = SettingsNode(doubleArrayOf(0.0, 0.0, 0.3, 0.0))
            val component = KeyComponent(key.first, key.second) {
                settingsNodes[index][0] = midiNoteToFreq(60 + index + 12 * octave)
                settingsNodes[index][1] = if (it) 1.0 else 0.0
            }
            settingsNodes.add(settingsNode)
            keyComponents.add(component)
            add(component)
        }

        val fullNodes = settingsNodes.map { Pipeline(listOf(it, node.cloneSettings())) }
        val mixer = MixerNode()
        fullNodes.forEach { mixer.addNode(it, 0.3, 0.0) }
        val effectAppliedNode = applyEffects(mixer, effects)


        thread {
            val player = AudioPlayer()
            player.renderAndPlay(effectAppliedNode, ctx, 1000.0)
        }
    }

    fun keyDown(code: Int, key: Char) {
        keyComponents.forEach { it.setPressed(key, true) }
        if (code == KeyEvent.VK_UP) {
            octave++
        } else if (code == KeyEvent.VK_DOWN) {
            octave--
        }
    }

    fun keyUp(key: Char) {
        keyComponents.forEach { it.setPressed(key, false) }
    }

    private fun midiNoteToFreq(note: Int): Double {
        return 440.0 * 2.0.pow((note - 69.0) / 12.0)
    }
}

class KeyComponent(
    private val key: Char, index: Pair<Int, Int>, private val onPressed: (Boolean) -> Unit
) : JPanel() {
    private var isPressed = false
    private val x = 100 * index.first + 50 * index.second + 85
    private val y = 100 * (1 - index.second) + 80

    init {
        setSize(100, 100)
        setBounds(x, y, 100, 100)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.color = java.awt.Color.BLACK
        if (isPressed) {
            g?.fillRect(2, 2, 96, 96)
        } else {
            g?.drawRect(2, 2, 96, 96)
        }
    }

    fun setPressed(key: Char, pressed: Boolean) {
        if (key != this.key) return
        isPressed = pressed
        onPressed(isPressed)
        repaint()
    }
}

fun askForInstrument(): String {
    println("Enter instrument name:")
    return readlnOrNull() ?: error("No input")
}

fun main() {
    val instruments = readInstruments()

    val instrumentName = askForInstrument()
    val (synth, effects) = instruments[instrumentName] ?: error("Instrument not found")

    val ctx = Context(0, 44100)
    val node = synth.buildNode(ctx.random)

    val player = InteractivePlayer(ctx, node, effects)
    player.isVisible = true
}