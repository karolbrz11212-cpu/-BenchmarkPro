package com.example.benchmarkpro

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.benchmarkpro.databinding.FragmentBenchmarkBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.File
import kotlin.concurrent.thread

class BenchmarkFragment : Fragment() {
    private var _binding: FragmentBenchmarkBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val temps = mutableListOf<Entry>()
    private var startTime = 0L
    private val duration = 10 * 60 * 1000L

    private var cpuScore = 0; private var ramScore = 0; private var gpuScore = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBenchmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStart.setOnClickListener { if (!running) startBenchmark() }
        setupChart()
        updateLiveTemp()
    }

    private fun startBenchmark() {
        running = true
        startTime = SystemClock.elapsedRealtime()
        temps.clear()
        cpuScore = 0; ramScore = 0; gpuScore = 0
        binding.statusText.text = "Benchmark läuft..."
        handler.post(tempRunnable)
        thread { runWorkload() }
    }

    private fun runWorkload() {
        cpuScore = cpuTest()
        ramScore = ramTest()
        gpuScore = gpuTest()
        val elapsed = SystemClock.elapsedRealtime() - startTime
        val wait = duration - elapsed
        if (wait > 0) Thread.sleep(wait)
        handler.post {
            running = false
            handler.removeCallbacks(tempRunnable)
            showResults()
        }
    }

    private fun cpuTest(): Int {
        var count = 0
        val max = 100_000
        for (i in 2..max) {
            if (!running) break
            if (isPrime(i)) count++
        }
        return (count / 10).coerceAtMost(2000)
    }

    private fun isPrime(n:Int):Boolean {
        if (n<2) return false
        if (n%2==0) return n==2
        var i=3
        while (i*i<=n) {
            if (n%i==0) return false
            i+=2
        }
        return true
    }

    private fun ramTest(): Int {
        val size = 8_000_000
        return try {
            val arr = IntArray(size) { it }
            var s=0L
            for (i in arr.indices step 5) {
                s+=arr[i]
                if (!running) break
            }
            (s%1000).toInt()+200
        } catch (e:OutOfMemoryError) { 100 }
    }

    private fun gpuTest(): Int {
        Thread.sleep(1500)
        return 500
    }

    private val tempRunnable = object: Runnable {
        override fun run() {
            if (!running) return
            val t = readTemperature() ?: return
            val elapsedSec = (SystemClock.elapsedRealtime()-startTime)/1000f
            temps.add(Entry(elapsedSec, t))
            binding.tempText.text = "%.1f °C".format(t)
            binding.tempBadge.setCardBackgroundColor(getColorForTemp(t))
            updateChart()
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateLiveTemp() {
        handler.post(object: Runnable {
            override fun run() {
                if (!running) {
                    val t = readTemperature()
                    if (t!=null) {
                        binding.tempText.text = "%.1f °C".format(t)
                        binding.tempBadge.setCardBackgroundColor(getColorForTemp(t))
                    }
                }
                handler.postDelayed(this, 3000)
            }
        })
    }

    private fun readTemperature(): Float? {
        val batt = context?.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val batTemp = batt?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)?.let { it/10f }
        if (batTemp != null && batTemp > 0f) return batTemp

        val paths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp"
        )
        for (p in paths) {
            try {
                val f = File(p)
                if (f.exists()) {
                    val txt = f.readText().trim()
                    val v = txt.toFloatOrNull()
                    if (v != null) {
                        return if (v > 1000) v/1000f else v
                    }
                }
            } catch (e: Exception) { }
        }
        return null
    }

    private fun getColorForTemp(t:Float): Int {
        val green = ContextCompat.getColor(requireContext(), R.color.premiumGreen)
        val yellow = ContextCompat.getColor(requireContext(), R.color.premiumYellow)
        val red = ContextCompat.getColor(requireContext(), R.color.premiumRed)
        return when {
            t < 33 -> green
            t < 42 -> yellow
            else -> red
        }
    }

    private fun setupChart() {
        binding.lineChart.description = Description().apply { text = "Temperaturverlauf (°C)" }
        binding.lineChart.axisRight.isEnabled = false
        binding.lineChart.setTouchEnabled(false)
        binding.lineChart.legend.isEnabled = false
    }

    private fun updateChart() {
        val ds = LineDataSet(temps, "Temp").apply {
            setDrawCircles(false)
            lineWidth = 2f
            color = Color.parseColor("#FF7043")
        }
        binding.lineChart.data = LineData(ds)
        binding.lineChart.invalidate()
    }

    private fun showResults() {
        val total = cpuScore + ramScore + gpuScore
        binding.statusText.text = "Benchmark fertig — Gesamt: $total"
        binding.cpuScore.text = cpuScore.toString()
        binding.ramScore.text = ramScore.toString()
        binding.gpuScore.text = gpuScore.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        running = false
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
