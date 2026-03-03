package com.app.maldroid

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.maldroid.data.ScanDataManager
import com.app.maldroid.homeactivities.ScanAllApkActivity
//import com.app.maldroid.homeactivities.AppSelection
import com.github.mikephil.charting.formatter.ValueFormatter
import com.app.maldroid.homeactivities.FileSelection
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"


    private lateinit var malwareCountText: TextView
    private lateinit var riskStatusText: TextView
    private lateinit var lineChart: LineChart


    private lateinit var scanApkButton: Button
    private lateinit var scanAppButton: Button
    private lateinit var scanAllApkButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initViews(view)
            setupListeners()
            updateDashboard()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateDashboard()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}")
        }
    }

    private fun initViews(view: View) {
        malwareCountText = view.findViewById(R.id.malwareCountNumber)
        riskStatusText = view.findViewById(R.id.statusText)
        lineChart = view.findViewById(R.id.lineChart)

        scanAllApkButton = view.findViewById(R.id.scanAllApKBtn)
//        scanAppButton = view.findViewById(R.id.scanApp)
        scanApkButton = view.findViewById(R.id.scanApk)
    }

    private fun setupListeners() {
        scanAllApkButton.setOnClickListener {
            val intent = Intent(requireContext(), ScanAllApkActivity::class.java)
            startActivity(intent)
        }

//        scanAppButton.setOnClickListener {
//            val intent = Intent(requireContext(), AppSelection::class.java)
//            startActivity(intent)
//        }

        scanApkButton.setOnClickListener {
            val intent = Intent(requireContext(), FileSelection::class.java)
            startActivity(intent)
        }
    }

    private fun updateDashboard() {
        val installedApps = ScanDataManager.getScannedApps()
        val apkFiles = ScanDataManager.getApkScanResults()
        val maliciousApps = installedApps.filter { it.isMalicious }
        val maliciousApks = apkFiles.filter { !it.isSafe }

        val totalThreats = maliciousApps.size + maliciousApks.size

        malwareCountText.text = totalThreats.toString()

        if (totalThreats > 0) {
            riskStatusText.text = "Risky Items Found"
            riskStatusText.setTextColor(Color.parseColor("#D32F2F"))
            malwareCountText.setTextColor(Color.parseColor("#D32F2F"))
        } else {
            riskStatusText.text = "No Threats Found"
            riskStatusText.setTextColor(Color.parseColor("#666666"))
            malwareCountText.setTextColor(Color.parseColor("#666666"))
        }

        updateChart(maliciousApps, maliciousApks)
    }

    private fun updateChart(
        maliciousApps: List<com.app.maldroid.data.model.AppScanResult>,
        maliciousApks: List<com.app.maldroid.data.model.ApkScanResult>
    ) {
        val allTimestamps = mutableListOf<Long>()
        allTimestamps.addAll(maliciousApps.map { it.scanTimestamp })
        allTimestamps.addAll(maliciousApks.map { it.scanTimestamp })

        if (allTimestamps.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No threats detected yet")
            lineChart.invalidate()
            return
        }

        allTimestamps.sort()
        val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        val threatsMap = allTimestamps.groupingBy {
            dateFormatter.format(Date(it))
        }.eachCount()

        val sortedThreats = threatsMap.toSortedMap()

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        var index = 0f
        sortedThreats.forEach { (date, count) ->
            entries.add(Entry(index, count.toFloat()))
            labels.add(date)
            index++
        }

        val dataSet = LineDataSet(entries, "Threats").apply {
            color = Color.parseColor("#D32F2F")
            lineWidth = 3f
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#D32F2F"))
            circleRadius = 5f
            setDrawFilled(true)
            fillColor = Color.parseColor("#D32F2F")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
            setDrawHighlightIndicators(true)
            highLightColor = Color.GRAY
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData


        lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelCount = labels.size
        }


        lineChart.axisLeft.apply {
            granularity = 1f
            axisMinimum = 0f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false

        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)

        lineChart.setVisibleXRangeMaximum(5f)
        if (entries.isNotEmpty()) {
            lineChart.moveViewToX(entries.size.toFloat())
        }

        val marker = CustomMarkerView(requireContext(), R.layout.marker_view, labels)
        marker.chartView = lineChart
        lineChart.marker = marker

        lineChart.animateY(1000)
        lineChart.invalidate()
    }

    inner class CustomMarkerView(context: Context, layoutResource: Int, private val labels: List<String>) : MarkerView(context, layoutResource) {
        private val tvContent: TextView = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e != null) {
                val index = e.x.toInt()
                val date = if (index >= 0 && index < labels.size) labels[index] else "?"
                val count = e.y.toInt()

                tvContent.text = "Date: $date\nThreats: $count"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }
    }
}