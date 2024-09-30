package gui

import app.applyEffects
import app.readInstruments
import nodes.*
import playback.AudioPlayer
import java.awt.Graphics
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.math.pow

class InteractivePlayer(val ctx: Context, node: AudioNode) : JFrame() {
    private val keyComponents = mutableListOf<KeyComponent>()
    private val settingsNodes = mutableListOf<SettingsNode>()
    private var octave = 0

    init {
        title = "Interactive Player"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = null
        setSize(1000, 600)
        setLocationRelativeTo(null)

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                keyDown(e.keyCode)
            }

            override fun keyReleased(e: KeyEvent) {
                keyUp(e.keyCode)
            }
        })

        val keys = listOf(
            KeyEvent.VK_A to (0 to 0),
            KeyEvent.VK_W to (0 to 1),
            KeyEvent.VK_S to (1 to 0),
            KeyEvent.VK_E to (1 to 1),
            KeyEvent.VK_D to (2 to 0),
            KeyEvent.VK_F to (3 to 0),
            KeyEvent.VK_T to (3 to 1),
            KeyEvent.VK_G to (4 to 0),
            KeyEvent.VK_Z to (4 to 1),
            KeyEvent.VK_H to (5 to 0),
            KeyEvent.VK_U to (5 to 1),
            KeyEvent.VK_J to (6 to 0),
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

        val fullNodes = settingsNodes.map { Pipeline(listOf(it, node.clone())) }
        val mixer = MixerNode()
        fullNodes.forEach { mixer.addNode(it, 1.0, 0.0) }

        thread {
            val player = AudioPlayer()
            player.renderAndPlay(mixer, ctx, 1000.0)
        }
    }

    fun keyDown(key: Int) {
        keyComponents.forEach { it.setPressed(key, true) }
        if (key == KeyEvent.VK_UP) {
            octave++
        } else if (key == KeyEvent.VK_DOWN) {
            octave--
        }
    }

    fun keyUp(key: Int) {
        keyComponents.forEach { it.setPressed(key, false) }
    }

    private fun midiNoteToFreq(note: Int): Double {
        return 440.0 * 2.0.pow((note - 69.0) / 12.0)
    }
}

class KeyComponent(
    private val key: Int, index: Pair<Int, Int>, private val onPressed: (Boolean) -> Unit
) : JPanel() {
    private var isPressed = false
    private val x = 100 * index.first + 50 * index.second + 25
    private val y = 100 * (1 - index.second) + 25

    init {
        setSize(100, 100)
        setBounds(x, y, 100, 100)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        g?.color = java.awt.Color.BLACK
        if (isPressed) {
            g?.fillRect(1, 1, 98, 98)
        } else {
            g?.drawRect(1, 1, 98, 98)
        }
    }

    fun setPressed(key: Int, pressed: Boolean) {
        if (key != this.key) return
        isPressed = pressed
        onPressed(isPressed)
        repaint()
    }
}

fun main() {
    val instruments = readInstruments()

    val (synth, effects) = instruments["guitar"] ?: error("Instrument not found")

    val ctx = Context(0, 44100)
    val node = applyEffects(synth.buildNode(ctx.random), effects)

    val player = InteractivePlayer(ctx, node)
    player.isVisible = true
}