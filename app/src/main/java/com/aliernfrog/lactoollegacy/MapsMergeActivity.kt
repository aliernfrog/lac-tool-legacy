package com.aliernfrog.lactoollegacy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aliernfrog.laclib.data.LACMapToMerge
import com.aliernfrog.laclib.map.LACMapMerger
import com.aliernfrog.lactoollegacy.fragments.MapDownloadSheet.MapDownloadListener
import com.aliernfrog.lactoollegacy.fragments.MapPickerSheet
import com.aliernfrog.lactoollegacy.fragments.MapPickerSheet.MapPickerListener
import com.aliernfrog.lactoollegacy.utils.AppUtil
import com.aliernfrog.lactoollegacy.utils.FileUtil
import com.google.android.material.textfield.TextInputEditText
import java.io.File

@Suppress("DEPRECATION")
class MapsMergeActivity : AppCompatActivity(), MapPickerListener, MapDownloadListener {
    private lateinit var toolbar: Toolbar
    private lateinit var baseMapLinear: LinearLayout
    private lateinit var baseMapName: TextView
    private lateinit var mapToAddLinear: LinearLayout
    private lateinit var mapToAddName: TextView
    private lateinit var mapToAddIncludeSpawns: CheckBox
    private lateinit var mapToAddIncludeTdmSpawns: CheckBox
    private lateinit var mapToAddIncludeCheckpoints: CheckBox
    private lateinit var mapToAddPos: TextInputEditText
    private lateinit var outputLinear: LinearLayout
    private lateinit var outputMapName: TextInputEditText
    private lateinit var startMerge: Button
    private lateinit var debugText: TextView
    private lateinit var prefsConfig: SharedPreferences

    private var mapsPath: String? = null
    private var primaryMapPath: String? = ""
    private var secondaryMapPath: String? = ""
    private var currentPickRequest: String? = null

    private val primaryMap = "primary"
    private val secondaryMap = "secondary"
    private val pickMapRequest = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_merge)
        mapsPath = intent.getStringExtra("mapsPath")

        toolbar = findViewById(R.id.mapsMerge_toolbar)
        baseMapLinear = findViewById(R.id.mapsMerge_baseMap_linear)
        baseMapName = findViewById(R.id.mapsMerge_baseMap_name)
        mapToAddLinear = findViewById(R.id.mapsMerge_mapToAdd_linear)
        mapToAddName = findViewById(R.id.mapsMerge_mapToAdd_name)
        mapToAddIncludeSpawns = findViewById(R.id.mapsMerge_mapToAdd_includeSpawns)
        mapToAddIncludeTdmSpawns = findViewById(R.id.mapsMerge_mapToAdd_includeTdmSpawns)
        mapToAddIncludeCheckpoints = findViewById(R.id.mapsMerge_mapToAdd_includeCheckpoints)
        mapToAddPos = findViewById(R.id.mapsMerge_mapToAdd_pos)
        outputLinear = findViewById(R.id.mapsMerge_output_linear)
        outputMapName = findViewById(R.id.mapsMerge_output_name)
        startMerge = findViewById(R.id.mapsMerge_output_merge)
        debugText = findViewById(R.id.mapsMerge_debug)

        prefsConfig = getSharedPreferences("APP_CONFIG", MODE_PRIVATE)
        if (prefsConfig.getBoolean("enableDebug", false)) debugText.visibility = View.VISIBLE

        devLog("MapsMergeActivity started")
        setListeners()
    }

    private fun validateAndMerge() {
        if (primaryMapPath == "" || secondaryMapPath == "")
            return Toast.makeText(applicationContext, R.string.mapMerge_warning_noMapSelected, Toast.LENGTH_SHORT).show()
        if (getOutputName() == null)
            return Toast.makeText(applicationContext, R.string.mapMerge_warning_noOutputName, Toast.LENGTH_SHORT).show()
        val check = File("$mapsPath/${getOutputName()}.txt")
        devLog("checking path: " + check.path)
        if (check.exists())
            return Toast.makeText(applicationContext, R.string.denied_alreadyExists, Toast.LENGTH_SHORT).show()
        devLog("passed checks, merging")
        mergeMap()
    }

    private fun mergeMap() {
        try {
            val primaryMapRaw = FileUtil.readFile(primaryMapPath)
            val secondaryMapRaw = FileUtil.readFile(secondaryMapPath)
            val mapMerger = LACMapMerger(mutableListOf(
                LACMapToMerge(
                    mapName = primaryMapPath!!,
                    content = primaryMapRaw,
                    mergeSpawnpoints = true,
                    mergeRacingCheckpoints = true,
                    mergeTDMSpawnpoints = true
                ),
                LACMapToMerge(
                    mapName = secondaryMapPath!!,
                    content = secondaryMapRaw,
                    mergeSpawnpoints = mapToAddIncludeSpawns.isChecked,
                    mergeRacingCheckpoints = mapToAddIncludeCheckpoints.isChecked,
                    mergeTDMSpawnpoints = mapToAddIncludeTdmSpawns.isChecked,
                    mergePosition = mapToAddPos.text.toString()
                )
            ))
            val mergedContent = mapMerger.mergeMaps(
                onNoEnoughMaps = { /* Imposible */ }
            )!!
            saveMergedMap(mergedContent)
        } catch (e: Exception) {
            e.printStackTrace()
            devLog(e.toString())
        }
    }

    private fun saveMergedMap(content: String) {
        try {
            val fileName = "${getOutputName()}.txt"
            devLog("attempting to save merged map with name: $fileName")
            FileUtil.saveFile(mapsPath, fileName, content)
            Toast.makeText(applicationContext, R.string.info_done, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            devLog(e.toString())
        }
    }

    private fun getMap(path: String) {
        devLog("$currentPickRequest: setting path: $path")
        val file = File(path)
        val name = FileUtil.removeExtension(file.name)
        if (currentPickRequest == primaryMap) {
            primaryMapPath = path
            baseMapName.text = name
        } else if (currentPickRequest == secondaryMap) {
            secondaryMapPath = path
            mapToAddName.text = name
        }
    }

    private fun pickMap(request: String) {
        currentPickRequest = request
        val bundle = Bundle()
        bundle.putString("mapsPath", mapsPath)
        val mapPickerSheet = MapPickerSheet()
        mapPickerSheet.arguments = bundle
        mapPickerSheet.show(supportFragmentManager, "map_pick")
    }

    private fun getOutputName(): String? {
        if (outputMapName.text == null) return null
        val name = outputMapName.text.toString()
        return if (name == "") null else name
    }

    private fun devLog(toLog: String) {
        AppUtil.devLog(toLog, debugText)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val hasData = data != null
        devLog("$requestCode: hasData = $hasData")
        if (!hasData) return
        if (requestCode == pickMapRequest) {
            val path = data!!.getStringExtra("path")
            devLog("received path: $path")
            getMap(path!!)
        }
    }

    fun setListeners() {
        toolbar.setNavigationOnClickListener { finish() }
        AppUtil.handleOnPressEvent(baseMapLinear) { pickMap(primaryMap) }
        AppUtil.handleOnPressEvent(mapToAddLinear) { pickMap(secondaryMap) }
        AppUtil.handleOnPressEvent(outputLinear)
        AppUtil.handleOnPressEvent(startMerge) { validateAndMerge() }
    }

    override fun onMapPicked(path: String) {
        getMap(path)
    }

    override fun onFilePickRequested() {
        devLog("attempting to pick a map file")
        val intent = Intent(this, FilePickerActivity::class.java)
        intent.putExtra("FILE_TYPE_SAF", "text/*")
        intent.putExtra("FILE_TYPE_INAPP", arrayOf("txt"))
        startActivityForResult(intent, pickMapRequest)
    }

    override fun onMapDownloaded(path: String) {
        getMap(path)
    }
}