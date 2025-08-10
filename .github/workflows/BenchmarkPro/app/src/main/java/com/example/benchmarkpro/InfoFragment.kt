package com.example.benchmarkpro

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.benchmarkpro.databinding.FragmentInfoBinding
import java.io.BufferedReader
import java.io.FileReader

class InfoFragment : Fragment() {
    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.infoText.text = getInfoString(requireContext())
    }

    private fun getInfoString(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("Model: ${Build.MODEL ?: "n/a"}")
        sb.appendLine("Manufacturer: ${Build.MANUFACTURER ?: "n/a"}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("CPU ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        sb.appendLine("CPU: ${getCPUModel()}")
        sb.appendLine("Total RAM: ${getTotalRAM(context)} MB")
        sb.appendLine("Free Storage: ${getFreeInternalStorage()} MB")
        sb.appendLine("Battery Temp: ${getBatteryTemp() ?: "n/a"}")
        return sb.toString()
    }

    private fun getCPUModel(): String {
        return try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { reader ->
                reader.lineSequence().firstOrNull { it.startsWith("Hardware") }?.split(":")?.getOrNull(1)?.trim() ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getTotalRAM(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem / (1024 * 1024)
    }

    private fun getFreeInternalStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
    }

    private fun getBatteryTemp(): String? {
        val intent = context?.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)?.let { "%.1f °C".format(it/10f) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
