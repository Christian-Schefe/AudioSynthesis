import effects.Effect
import effects.FourBandEqualizer
import effects.NoEffect
import instruments.*
import midi.abstraction.Midi
import nodes.*
import song.SongConverter
import util.json.*
import wav.*
import java.io.FileInputStream
import kotlin.time.measureTimedValue

/*fun main() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)
    val midi = Midi.readFromFile("test_data/blues.mid")
    val song = SongConverter().fromMidi(midi)

    var node: AudioNode? = null

    /*val factories = mapOf(
        0 to (InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }),
        1 to (InstrumentSettings(1.0, 0.15) to { violin(ctx.random) }),
        2 to (InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }),
        3 to (InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }),
    )*/

    val factories = mutableMapOf<Int, Pair<InstrumentSettings, () -> AudioNode>>()
    for (i in 0 until song.tracks.size) {
        factories[i] = InstrumentSettings(1.0, 0.0) to { piano(ctx.random) }
    }

    var duration = 0.0

    for ((track, data) in factories) {
        val (settings, factory) = data
        println("Track $track: ${song.tracks[track].name} with ${song.tracks[track].notes.size} notes")
        val trackNode = SongPlayer(factory, song, track, 36, settings)
        if (node == null) {
            node = trackNode
        } else {
            node += trackNode
        }
        duration = maxOf(duration, song.tracks[track].duration(song.tempoTrack))
        //println("notes: ${song.tracks[track].notes}")
    }

    if (node == null) {
        println("No tracks found in song")
        return
    }

    val renderer = AudioRenderer(ctx, node, sampleRate)

    println("Rendering ${song.tracks.size} tracks with $duration seconds of audio...")

    val (samples, timeTaken) = measureTimedValue {
        renderer.renderStereo(duration + 5, 2)
    }

    println("Time taken: $timeTaken")

    val wavFile = WavFile(
        AudioFormat.IEEE_FLOAT, samples, sampleRate
    ).withNormalizedSamples(1.0)

    println("Writing output.wav...")
    wavFile.writeToFile("output.wav")
    println("Done!")

    val readFile = WavFile.readFromFile("output.wav")
    val readSamples = readFile.samples

    for (i in 0..<readSamples.size) {
        for (j in 0..<readSamples[i].size) {
            if (readSamples[i][j] - wavFile.samples[i][j] > 0.0001) {
                println("Sample $i $j: ${readSamples[i][j]} != ${wavFile.samples[i][j]}")
            }
        }
    }
}*/

fun render(ctx: Context, node: AudioNode, duration: Double): WavFile {
    println("Rendering $duration seconds of audio...")

    val renderer = AudioRenderer(ctx, node)
    val (samples, timeTaken) = measureTimedValue {
        renderer.renderStereo(duration + 5, 2)
    }

    println("Time taken: $timeTaken")

    val wavFile = WavFile(
        AudioFormat.IEEE_FLOAT, samples, ctx.sampleRate
    ).withNormalizedSamples(1.0)

    return wavFile
}

fun save(wavFile: WavFile, filename: String) {
    println("Writing $filename...")
    wavFile.writeToFile(filename)
    println("Done!")
}

fun main() {
    val sampleRate = 44100
    val ctx = Context(0, sampleRate)

    val json = FileInputStream("test_data/blues.json").readAllBytes().decodeToString()
    val schema = ObjectSchema(
        "midiFile" to StringSchema() to false, "tracks" to ArraySchema(
            ObjectSchema(
                "instrument" to StringSchema() to false,
                "volume" to NumberSchema() to true,
                "pan" to NumberSchema() to true,
                "effects" to effectsSchema() to true
            )
        ) to false
    )
    val parsed = schema.safeParse(json).throwIfErr()

    val midiFile = parsed["midiFile"]!!.str().value
    val midi = Midi.readFromFile(midiFile)
    val song = SongConverter().fromMidi(midi)

    val tracks = parsed["tracks"]!!.arr().elements

    val instruments = readInstruments()

    var masterNode: AudioNode? = null

    for (trackNum in tracks.indices) {
        val track = tracks[trackNum].obj()
        val instrument = track["instrument"]!!.str().value
        val volume = track["volume"]?.num()?.value?.toDouble() ?: 1.0
        val pan = track["pan"]?.num()?.value?.toDouble() ?: 0.0
        val effectData = track["effects"]?.arr()?.elements ?: emptyList()
        println("Track with instrument $instrument, volume $volume, pan $pan and effects $effectData")

        val (instrumentSynth, instrumentEffects) = instruments[instrument]
            ?: throw IllegalArgumentException("Unknown instrument $instrument")


        val audioNode = instrumentSynth.buildAudio(song, trackNum, ctx.random)
        val effects = buildEffects(effectData)
        val node =
            applyEffects(applyEffects(audioNode, instrumentEffects), effects) * CustomNode.constant(volume, volume)

        if (masterNode == null) {
            masterNode = node
        } else {
            masterNode += node
        }
    }

    if (masterNode == null) {
        println("No tracks found in song")
        return
    }

    val wavFile = render(ctx, masterNode, song.duration())
    save(wavFile, "output.wav")
}

fun buildEffects(effects: List<SchemaData>): List<Effect> {
    val effectList = mutableListOf<Effect>()
    for (effect in effects) {
        val effectObj = effect.obj()
        val type = effectObj["type"]!!.str().value
        val params = effectObj["params"]!!.any().json
        val effectInstance = buildEffect(type, params)
        effectList.add(effectInstance)
    }
    return effectList
}

fun buildEffect(type: String, params: JsonElement): Effect {
    if (type == "EQ") {
        val schema = ObjectSchema(
            "low" to NumberSchema() to false,
            "lowMid" to NumberSchema() to false,
            "highMid" to NumberSchema() to false,
            "high" to NumberSchema() to false
        )
        val parsed = schema.safeConvert(params).throwIfErr()
        val low = parsed["low"]!!.num().value.toDouble()
        val lowMid = parsed["lowMid"]!!.num().value.toDouble()
        val highMid = parsed["highMid"]!!.num().value.toDouble()
        val high = parsed["high"]!!.num().value.toDouble()
        return FourBandEqualizer(low, lowMid, highMid, high)
    }
    return NoEffect()
}

fun effectsSchema(): JsonSchema {
    val schema = ArraySchema(
        ObjectSchema(
            "type" to StringSchema() to false, "params" to AnySchema() to false
        )
    )
    return schema
}

fun applyEffects(node: AudioNode, effects: List<Effect>): AudioNode {
    var result = node
    for (effect in effects) {
        result = result pipe effect.buildNode()
    }
    return result
}

fun readInstruments(): Map<String, Pair<Synth, List<Effect>>> {
    val json = FileInputStream("test_data/instruments.json").readAllBytes().decodeToString()
    val schema = ArraySchema(
        ObjectSchema(
            "name" to StringSchema() to false,
            "synth" to StringSchema() to false,
            "params" to AnySchema() to false,
            "effects" to effectsSchema() to true
        )
    )

    val parsed = schema.safeParse(json).throwIfErr()
    val instruments = mutableMapOf<String, Pair<Synth, List<Effect>>>()
    for (instrument in parsed.arr()) {
        val name = instrument["name"]!!.str().value
        val synthName = instrument["synth"]!!.str().value
        val params = instrument["params"]!!.any().json
        val effects = instrument["effects"]?.arr()?.elements ?: emptyList()
        println("Instrument $name: $synthName with params $params and effects $effects")
        val effectsList = buildEffects(effects)
        val synth = buildSynth(synthName, params)
        instruments[name] = synth to effectsList
    }
    return instruments
}

fun buildSynth(synthName: String, params: JsonElement): Synth {
    if (synthName == "simpleSynth") {
        val adsrSchema = ObjectSchema(
            "attack" to NumberSchema() to false,
            "decay" to NumberSchema() to false,
            "sustain" to NumberSchema() to false,
            "release" to NumberSchema() to false
        )

        val waveDataSchema = ObjectSchema(
            "amplitude" to NumberSchema() to false, "phase" to NumberSchema() to false
        )

        val schema = ObjectSchema(
            "vibrato" to NumberSchema() to false,
            "detune" to NumberSchema() to true,
            "adsr" to adsrSchema to false,
            "wave" to ObjectSchema(
                "sine" to waveDataSchema to true,
                "square" to waveDataSchema to true,
                "saw" to waveDataSchema to true,
                "triangle" to waveDataSchema to true,
                "soft square" to waveDataSchema to true,
                "soft saw" to waveDataSchema to true
            ) to false
        )

        val parsed = schema.safeConvert(params).throwIfErr()
        val vibrato = parsed["vibrato"]!!.num().value.toDouble()
        val detune = parsed["detune"]?.num()?.value?.toDouble() ?: 0.0
        val adsrObj = parsed["adsr"]!!.obj()
        val adsr = ADSR(
            adsrObj["attack"]!!.num().value.toDouble(),
            adsrObj["decay"]!!.num().value.toDouble(),
            adsrObj["sustain"]!!.num().value.toDouble(),
            adsrObj["release"]!!.num().value.toDouble()
        )
        println("Vibrato: $vibrato, detune: $detune, adsr: $adsr")
        val wave = parsed["wave"]!!.obj()
        val waves = mutableMapOf<WaveType, WaveData>()
        for (waveType in WaveType.entries) {
            val waveData = wave[waveType.id]?.obj()
            if (waveData != null) {
                val amplitude = waveData["amplitude"]!!.num().value.toDouble()
                val phase = waveData["phase"]!!.num().value.toDouble()
                waves[waveType] = WaveData(amplitude, phase)
            }
        }
        val synth = SimpleSynth(vibrato, adsr, detune, waves)
        return synth
    } else {
        throw IllegalArgumentException("Unknown synth $synthName")
    }
}