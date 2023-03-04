package com.aliernfrog.lactoollegacy

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aliernfrog.laclib.enum.LACMapOptionType
import com.aliernfrog.laclib.enum.LACMapType
import com.aliernfrog.laclib.map.LACMapEditor
import com.aliernfrog.lactoollegacy.fragments.MapTypeSheet
import com.aliernfrog.lactoollegacy.fragments.MapTypeSheet.MapTypeListener
import com.aliernfrog.lactoollegacy.utils.AppUtil
import com.aliernfrog.lactoollegacy.utils.FileUtil
import com.aliernfrog.lactoollegacy.utils.extension.getName
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File


@Suppress("DEPRECATION")
@SuppressLint("ClickableViewAccessibility", "UseSwitchCompatOrMaterialCode")
class MapsOptionsActivity : AppCompatActivity(), MapTypeListener {
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var serverNameLinear: LinearLayout
    private lateinit var serverName: EditText
    private lateinit var mapTypeLinear: LinearLayout
    private lateinit var mapTypeButton: Button
    private lateinit var optionsLinear: LinearLayout
    private lateinit var otherOptionsLinear: LinearLayout
    private lateinit var editRolesButton: Button
    private lateinit var removeAllTdm: Button
    private lateinit var removeAllRace: Button
    private lateinit var fixMapButton: Button
    private lateinit var saveChanges: FloatingActionButton
    private lateinit var debugText: TextView

    private lateinit var config: SharedPreferences

    private lateinit var rawPath: String
    private lateinit var mapName: String

    private lateinit var mapEditor: LACMapEditor

    private val requestRoles = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_options)
        config = getSharedPreferences("APP_CONFIG", MODE_PRIVATE)
        rawPath = intent.getStringExtra("path") ?: return finish()
        mapName = intent.getStringExtra("name") ?: return finish()

        collapsingToolbarLayout = findViewById(R.id.mapsOptions_collapsingToolbar)
        toolbar = findViewById(R.id.mapsOptions_toolbar)
        serverNameLinear = findViewById(R.id.mapsOptions_serverName_linear)
        serverName = findViewById(R.id.mapsOptions_serverName_input)
        mapTypeLinear = findViewById(R.id.mapsOptions_mapType_linear)
        mapTypeButton = findViewById(R.id.mapsOptions_mapType_change)
        optionsLinear = findViewById(R.id.mapsOptions_options_linear)
        otherOptionsLinear = findViewById(R.id.mapsOptions_otherOptions_linear)
        editRolesButton = findViewById(R.id.mapsOptions_roles_editRoles)
        removeAllTdm = findViewById(R.id.mapsOptions_removeAll_tdm)
        removeAllRace = findViewById(R.id.mapsOptions_removeAll_race)
        fixMapButton = findViewById(R.id.mapsOptions_fix_button)
        saveChanges = findViewById(R.id.mapsOptions_save_button)
        debugText = findViewById(R.id.mapsOptions_log)
        if (config.getBoolean("enableDebug", false)) debugText.visibility = View.VISIBLE

        getMap()
        setListeners()
    }

    private fun getMap() {
        try {
            devLog("rawPath: $rawPath")
            devLog("mapName: $mapName")
            collapsingToolbarLayout.title = mapName
            loadMap(rawPath)
        } catch (e: Exception) {
            devLog(e.toString())
            Toast.makeText(applicationContext, R.string.info_error, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadMap(path: String) {
        val mapContent = FileUtil.readFile(path)
        mapEditor = LACMapEditor(mapContent)
        if (mapEditor.serverName != null) {
            serverName.setText(mapEditor.serverName)
            serverNameLinear.visibility = View.VISIBLE
        }
        if (mapEditor.mapType != null) {
            mapTypeButton.text = mapEditor.mapType!!.getName(applicationContext)
            mapTypeLinear.visibility = View.VISIBLE
        }
        if (mapEditor.mapRoles != null) editRolesButton.visibility = View.VISIBLE
        loadOptions()
    }

    private fun loadOptions() {
        val hasOptions = mapEditor.mapOptions.isNotEmpty()
        if (!hasOptions) return
        optionsLinear.visibility = View.VISIBLE
        mapEditor.mapOptions.forEach { option ->
            when(option.type) {
                LACMapOptionType.NUMBER -> {
                    val view = layoutInflater.inflate(R.layout.inflate_option_number, optionsLinear, false) as View
                    val textInputLayout: TextInputLayout = view.findViewById(R.id.option_number_layout)
                    val textInputEditText: TextInputEditText = view.findViewById(R.id.option_number_input)
                    textInputLayout.hint = option.label
                    textInputEditText.setText(option.value)
                    AppUtil.afterTextChanged(textInputEditText) {
                        option.value = textInputEditText.text.toString()
                    }
                    optionsLinear.addView(view)
                }
                LACMapOptionType.BOOLEAN -> {
                    val view = layoutInflater.inflate(R.layout.inflate_option_bool, optionsLinear, false) as View
                    val switchView: Switch = view.findViewById(R.id.option_bool_switch)
                    switchView.text = option.label
                    switchView.isChecked = option.value == "true"
                    switchView.setOnCheckedChangeListener { _, checked ->
                        option.value = checked.toString()
                    }
                    optionsLinear.addView(view)
                }
                LACMapOptionType.SWITCH -> {
                    val view = layoutInflater.inflate(R.layout.inflate_option_bool, optionsLinear, false) as View
                    val switchView: Switch = view.findViewById(R.id.option_bool_switch)
                    switchView.text = option.label
                    switchView.isChecked = option.value == "enabled"
                    switchView.setOnCheckedChangeListener { _, checked ->
                        option.value = if (checked) "enabled" else "disabled"
                    }
                    optionsLinear.addView(view)
                }
            }
        }
    }

    private fun saveMap() {
        try {
            devLog("attempting to save the map")
            val newContent = mapEditor.applyChanges()
            val currentFile = File(rawPath)
            FileUtil.saveFile(currentFile.parent, currentFile.name, newContent)
            Toast.makeText(applicationContext, R.string.info_done, Toast.LENGTH_SHORT).show()
            devLog("done saving the map")
            finish()
        } catch (e: Exception) {
            devLog(e.toString())
        }
    }

    private fun openMapTypeView() {
        val bundle = Bundle()
        bundle.putInt("mapTypeInt", mapEditor.mapType?.index ?: return devLog("map type was null"))
        val mapTypeSheet = MapTypeSheet()
        mapTypeSheet.arguments = bundle
        mapTypeSheet.show(supportFragmentManager, "map_type")
    }

    private fun editRoles() {
        val intent = Intent(this, MapsRolesActivity::class.java)
        intent.putExtra("roles", mapEditor.mapRoles!!.joinToString(","))
        intent.putExtra("mapName", mapName)
        startActivityForResult(intent, requestRoles)
    }

    private fun devLog(toLog: String?) {
        AppUtil.devLog(toLog, debugText)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        devLog(requestCode.toString() + ": hasData = " + (data != null))
        if (requestCode == requestRoles && data != null) {
            mapEditor.mapRoles = data.getStringExtra("roles")?.split(",")?.toMutableList()
        } else {
            devLog("data is null")
        }
    }

    fun setListeners() {
        toolbar.setNavigationOnClickListener { finish() }
        AppUtil.handleOnPressEvent(serverNameLinear)
        AppUtil.afterTextChanged(serverName) { mapEditor.serverName = serverName.text.toString() }
        AppUtil.handleOnPressEvent(mapTypeLinear)
        AppUtil.handleOnPressEvent(mapTypeButton) { openMapTypeView() }
        AppUtil.handleOnPressEvent(optionsLinear)
        AppUtil.handleOnPressEvent(otherOptionsLinear)
        AppUtil.handleOnPressEvent(editRolesButton) { editRoles() }
        //TODO implement object filtering and suggestions
        //AppUtil.handleOnPressEvent(removeAllTdm) { removeAllObjects("Team_") }
        //AppUtil.handleOnPressEvent(removeAllRace) { removeAllObjects("Checkpoint_Editor") }
        //AppUtil.handleOnPressEvent(fixMapButton) { fixMap() } TODO
        AppUtil.handleOnPressEvent(saveChanges) { saveMap() }
    }

    override fun onMapTypeChoose(type: Int) {
        val mapType = LACMapType.values().find { it.index == type }
            ?: return Toast.makeText(applicationContext, R.string.info_error, Toast.LENGTH_SHORT).show()
        devLog("setting map type to ${mapType.index}")
        mapEditor.mapType = mapType
        mapTypeButton.text = mapType.getName(applicationContext)
    }
}