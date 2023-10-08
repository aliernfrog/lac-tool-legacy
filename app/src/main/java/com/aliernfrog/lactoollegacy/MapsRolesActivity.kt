package com.aliernfrog.lactoollegacy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aliernfrog.lactoollegacy.utils.AppUtil
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MapsRolesActivity : AppCompatActivity() {
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var saveButton: FloatingActionButton
    private lateinit var addRoleLinear: LinearLayout
    private lateinit var addRoleNameInput: TextInputEditText
    private lateinit var addRoleColorInput: TextInputEditText
    private lateinit var rawRoleName: TextView
    private lateinit var addRoleButton: Button
    private lateinit var rolesRoot: LinearLayout
    private lateinit var debugText: TextView

    private lateinit var config: SharedPreferences
    private lateinit var roleList: ArrayList<String>

    private val rawRole: String
        get() {
            var raw = ""
            if (addRoleNameInput.text != null) {
                raw = addRoleNameInput.text.toString()
                if (raw != "") {
                    if (!raw.contains("[")) raw = "[$raw"
                    if (!raw.contains("]")) raw = "$raw]"
                    if (addRoleColorInput.text != null && addRoleColorInput.text.toString() != "") {
                        val color = addRoleColorInput.text.toString()
                        raw = "<color=$color>$raw</color>"
                    }
                }
            }
            rawRoleName.text = raw
            return raw
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_roles)

        config = getSharedPreferences("APP_CONFIG", MODE_PRIVATE)
        val mapName = intent.getStringExtra("mapName")

        collapsingToolbarLayout = findViewById(R.id.mapsRoles_collapsingToolbar)
        toolbar = findViewById(R.id.mapsRoles_toolbar)
        saveButton = findViewById(R.id.mapsRoles_save)
        addRoleLinear = findViewById(R.id.mapsRoles_addRole_linear)
        addRoleNameInput = findViewById(R.id.mapsRoles_addRole_nameInput)
        addRoleColorInput = findViewById(R.id.mapsRoles_addRole_colorInput)
        rawRoleName = findViewById(R.id.mapsRoles_addRole_rawText)
        addRoleButton = findViewById(R.id.mapsRoles_addRole_done)
        rolesRoot = findViewById(R.id.mapsRoles_root)
        debugText = findViewById(R.id.mapsRoles_debug)

        roleList = intent.extras!!.getStringArrayList("roles") ?: return finish()

        if (config.getBoolean("enableDebug", false)) debugText.visibility = View.VISIBLE
        if (mapName != null) collapsingToolbarLayout.title = mapName

        devLog("MapsRolesActivity started")
        devLog("roles: $roleList")
        readRoles()
        setListeners()
    }

    private fun readRoles() {
        devLog("attempting to read roles")
        rolesRoot.removeAllViews()
        for (i in roleList.indices) {
            val role = roleList[i]
            addRoleView(role)
        }
    }

    private fun addRoleView(role: String) {
        val viewGroup = layoutInflater.inflate(R.layout.inflate_role, rolesRoot, false) as ViewGroup
        val roleName = viewGroup.findViewById<TextView>(R.id.role_name)
        val delete = viewGroup.findViewById<Button>(R.id.role_delete)
        @Suppress("DEPRECATION")
        roleName.text = Html.fromHtml(roleToHtml(role))
        roleName.setOnLongClickListener {
            copyRoleToClipboard(role)
            true
        }
        AppUtil.handleOnPressEvent(delete) { deleteRole(viewGroup) }
        AppUtil.handleOnPressEvent(viewGroup)
        rolesRoot.addView(viewGroup)
    }

    private fun copyRoleToClipboard(role: String) {
        AppUtil.copyToClipboard(role, applicationContext)
        Toast.makeText(
            applicationContext,
            getString(R.string.mapRoles_copied) + ": " + role,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun deleteRole(view: View?) {
        val roleIndex = rolesRoot.indexOfChild(view)
        devLog("attempting to delete role $roleIndex")
        roleList.removeAt(roleIndex)
        rolesRoot.removeView(view)
        Toast.makeText(applicationContext, R.string.info_done, Toast.LENGTH_SHORT).show()
    }

    private fun addRole() {
        val role = rawRole
        if (role == "" || role == "[]") {
            Toast.makeText(applicationContext, R.string.mapRoles_add_noName, Toast.LENGTH_SHORT).show()
        } else if (role.contains(",") || role.contains(":")) {
            Toast.makeText(applicationContext, R.string.mapRoles_add_cantInclude, Toast.LENGTH_SHORT).show()
        } else {
            devLog("attempting to add role: $role")
            roleList.add(role)
            addRoleView(role)
            Toast.makeText(applicationContext, R.string.info_done, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRolesAndExit() {
        val intent = Intent()
        intent.putExtra("roles", roleList)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun roleToHtml(role: String): String {
        return role.replace("<color", "<font color").replace("</color>", "</font>")
    }

    private fun devLog(toLog: String?) {
        AppUtil.devLog(toLog, debugText)
    }

    fun setListeners() {
        toolbar.setNavigationOnClickListener { finish() }
        AppUtil.handleOnPressEvent(saveButton) { saveRolesAndExit() }
        AppUtil.handleOnPressEvent(addRoleLinear)
        AppUtil.afterTextChanged(addRoleNameInput) { rawRole }
        AppUtil.afterTextChanged(addRoleColorInput) { rawRole }
        AppUtil.handleOnPressEvent(addRoleButton) { addRole() }
    }
}