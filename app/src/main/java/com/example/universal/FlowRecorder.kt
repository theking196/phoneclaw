package com.example.universal

import android.util.Log

object FlowRecorder {
    private const val TAG = "FlowRecorder"
    data class RecordedStep(val type: String, val x: Float = 0f, val y: Float = 0f, val text: String = "", val appPackage: String = "", val timestampMs: Long = System.currentTimeMillis())

    private val steps = mutableListOf<RecordedStep>()
    var isRecording = false; private set
    var onStepRecorded: ((RecordedStep) -> Unit)? = null

    fun startRecording() { steps.clear(); isRecording = true; Log.d(TAG, "Recording started") }
    fun stopRecording(): List<RecordedStep> { isRecording = false; Log.d(TAG, "Stopped: ${steps.size} steps"); return steps.toList() }
    fun recordClick(x: Float, y: Float) { if (!isRecording) return; val s = RecordedStep("click", x=x, y=y); steps.add(s); onStepRecorded?.invoke(s) }
    fun recordType(text: String) { if (!isRecording) return; val s = RecordedStep("type", text=text); steps.add(s); onStepRecorded?.invoke(s) }
    fun recordDelay(ms: Long = 2000) { if (!isRecording) return; val s = RecordedStep("delay", text=ms.toString()); steps.add(s); onStepRecorded?.invoke(s) }
    fun getStepCount() = steps.size
    fun clear() { steps.clear() }

    fun generateScript(flowName: String = "Recorded Flow"): String {
        val sb = StringBuilder()
        sb.appendLine("// PhoneClaw Flow Recorder — $flowName"); sb.appendLine()
        var lastTime = 0L
        steps.forEachIndexed { i, step ->
            if (i > 0 && step.timestampMs - lastTime > 800) sb.appendLine("delay(${minOf(step.timestampMs - lastTime, 3000)});")
            lastTime = step.timestampMs
            when (step.type) {
                "click" -> sb.appendLine("magicClicker(\"element at ${step.x.toInt()},${step.y.toInt()}\"); // (${step.x.toInt()},${step.y.toInt()})")
                "type" -> sb.appendLine("// typeInField(\"${step.text.take(50)}\")")
                "delay" -> sb.appendLine("delay(${step.text});")
                "launch" -> sb.appendLine("launchAppByPackage(\"${step.appPackage}\");")
            }
        }
        return sb.toString()
    }
}
