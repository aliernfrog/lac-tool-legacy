package com.aliernfrog.lactoollegacy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.aliernfrog.laclib.data.LACMapObjectFilter
import com.aliernfrog.laclib.enum.LACMapOptionType
import com.aliernfrog.laclib.enum.LACMapType
import com.aliernfrog.laclib.map.LACMapEditor
import com.aliernfrog.laclib.util.DEFAULT_MAP_OBJECT_FILTERS
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
class MapsOptionsActivity : AppCompatActivity(), MapTypeListener {
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var generalLinear: LinearLayout
    private lateinit var serverNameLinear: LinearLayout
    private lateinit var serverName: EditText
    private lateinit var mapTypeLinear: LinearLayout
    private lateinit var mapTypeButton: Button
    private lateinit var optionsLinear: LinearLayout
    private lateinit var objectFilterLinear: LinearLayout
    private lateinit var objectFilterQuery: EditText
    private lateinit var objectFilterSuggestions: LinearLayout
    private lateinit var objectFilterCaseSensitive: SwitchCompat
    private lateinit var objectFilterExactMatch: SwitchCompat
    private lateinit var objectFilterRemove: Button
    private lateinit var editRolesButton: Button
    private lateinit var replaceOldObjectsButton: Button
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
        generalLinear = findViewById(R.id.mapsOptions_general_linear)
        serverNameLinear = findViewById(R.id.mapsOptions_serverName_linear)
        serverName = findViewById(R.id.mapsOptions_serverName_input)
        mapTypeLinear = findViewById(R.id.mapsOptions_mapType_linear)
        mapTypeButton = findViewById(R.id.mapsOptions_mapType_change)
        optionsLinear = findViewById(R.id.mapsOptions_options_linear)
        objectFilterLinear = findViewById(R.id.mapsOptions_filterObjects_linear)
        objectFilterQuery = findViewById(R.id.mapsOptions_filterObjects_query)
        objectFilterSuggestions = findViewById(R.id.mapsOptions_filterObjects_suggestions)
        objectFilterCaseSensitive = findViewById(R.id.mapsOptions_filterObjects_caseSensitive)
        objectFilterExactMatch = findViewById(R.id.mapsOptions_filterObjects_exactMatch)
        objectFilterRemove = findViewById(R.id.mapsOptions_filterObjects_remove)
        editRolesButton = findViewById(R.id.mapsOptions_roles_editRoles)
        replaceOldObjectsButton = findViewById(R.id.mapsOptions_replaceOldObjects)
        saveChanges = findViewById(R.id.mapsOptions_save_button)
        debugText = findViewById(R.id.mapsOptions_log)
        if (config.getBoolean("enableDebug", false)) debugText.visibility = View.VISIBLE

        getMap()
        loadObjectFilterSuggestions()
        filterObjects()
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
            Toast.makeText(applicationContext, e.stackTrace[0].toString(), Toast.LENGTH_SHORT).show()
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
        if (mapEditor.replacableObjects.isNotEmpty()) replaceOldObjectsButton.visibility = View.VISIBLE
        replaceOldObjectsButton.text = getString(R.string.mapEdit_replaceOldObjects).replace("{COUNT}", mapEditor.replacableObjects.size.toString())
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
                    val switchView: SwitchCompat = view.findViewById(R.id.option_bool_switch)
                    switchView.text = option.label
                    switchView.isChecked = option.value == "true"
                    switchView.setOnCheckedChangeListener { _, checked ->
                        option.value = checked.toString()
                    }
                    optionsLinear.addView(view)
                }
                LACMapOptionType.SWITCH -> {
                    val view = layoutInflater.inflate(R.layout.inflate_option_bool, optionsLinear, false) as View
                    val switchView: SwitchCompat = view.findViewById(R.id.option_bool_switch)
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

    private fun loadObjectFilterSuggestions() {
        DEFAULT_MAP_OBJECT_FILTERS.forEach { filter ->
            val view = layoutInflater.inflate(R.layout.inflate_suggestion, objectFilterSuggestions, false) as View
            val text: TextView = view.findViewById(R.id.suggestion)
            text.text = filter.filterName ?: "-"
            AppUtil.handleOnPressEvent(view) {
                objectFilterQuery.setText(filter.query)
                objectFilterCaseSensitive.isChecked = filter.caseSensitive
                objectFilterExactMatch.isChecked = filter.exactMatch
                filterObjects()
            }
            objectFilterSuggestions.addView(view)
        }
    }

    private fun getObjectFilter(): LACMapObjectFilter {
        return LACMapObjectFilter(
            query = objectFilterQuery.text.toString(),
            caseSensitive = objectFilterCaseSensitive.isChecked,
            exactMatch = objectFilterExactMatch.isChecked
        )
    }

    private fun filterObjects() {
        val matches = mapEditor.getObjectsMatchingFilter(getObjectFilter())
        objectFilterRemove.text = getString(R.string.mapEdit_filterObjects_remove).replace("{COUNT}", matches.size.toString())
        objectFilterRemove.visibility = if (matches.isEmpty()) View.GONE else View.VISIBLE
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
        intent.putExtra("roles", mapEditor.mapRoles!!.toTypedArray())
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
        if (requestCode == requestRoles) {
            data?.getStringArrayExtra("roles")?.let {
                mapEditor.mapRoles = it.toMutableList()
            }
        }
    }

    fun setListeners() {
        toolbar.setNavigationOnClickListener { finish() }
        AppUtil.handleOnPressEvent(generalLinear)
        AppUtil.afterTextChanged(serverName) { mapEditor.serverName = serverName.text.toString() }
        AppUtil.handleOnPressEvent(mapTypeButton) { openMapTypeView() }
        AppUtil.handleOnPressEvent(optionsLinear)
        AppUtil.handleOnPressEvent(editRolesButton) { editRoles() }
        AppUtil.handleOnPressEvent(objectFilterLinear)
        AppUtil.afterTextChanged(objectFilterQuery) { filterObjects() }
        objectFilterCaseSensitive.setOnCheckedChangeListener { _, _ -> filterObjects() }
        objectFilterExactMatch.setOnCheckedChangeListener { _, _ -> filterObjects() }
        AppUtil.handleOnPressEvent(objectFilterRemove) {
            val count = mapEditor.removeObjectsMatchingFilter(getObjectFilter())
            Toast.makeText(applicationContext, getString(R.string.mapEdit_filterObjects_removed).replace("{COUNT}", count.toString()), Toast.LENGTH_SHORT).show()
            filterObjects()
        }
        AppUtil.handleOnPressEvent(replaceOldObjectsButton) {
            val count = mapEditor.replaceOldObjects()
            Toast.makeText(applicationContext, getString(R.string.mapEdit_replacedOldObjects).replace("{COUNT}", count.toString()), Toast.LENGTH_SHORT).show()
            replaceOldObjectsButton.visibility = View.GONE
        }
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