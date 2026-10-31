package dev.ryuya.galaxyupdateguard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import java.util.LinkedHashMap

class MainActivity : AppCompatActivity() {
    private var model: GuardModel? = null
    private var pages: SettingsTabs? = null
    private var title: MaterialTextView? = null
    private var detail: MaterialTextView? = null
    private var error: MaterialTextView? = null
    private var toggle: MaterialSwitch? = null
    private var restore: MaterialButton? = null
    private var refresh: MaterialButton? = null
    private var progress: LinearProgressIndicator? = null
    private val serviceRows: MutableMap<String?, MaterialTextView?> =
        LinkedHashMap<String?, MaterialTextView?>()
    private var binding: Boolean = false

    public override fun onCreate(saved: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(saved)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
        val root = Ui.column(this, 0)
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(
            root,
            OnApplyWindowInsetsListener { view: View?, insets: WindowInsetsCompat? ->
                val bars = insets!!.getInsets(Type.systemBars() or Type.displayCutout())
                view!!.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets!!
            },
        )
        ViewCompat.requestApplyInsets(root)
        val heading = Ui.column(this, 24)
        Ui.text(
            heading,
            getString(R.string.app_name),
            com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium,
        )
        root.addView(heading)
        pages = SettingsTabs(this, root, saved)
        addControlPage(pages!!.page(R.string.tab_control))
        addInfoPage(pages!!.page(R.string.tab_info))
        pages!!.restore()
        model = ViewModelProvider(this).get<GuardModel>(GuardModel::class.java)
        model!!.state().observe(this, Observer { state: GuardModel.State -> this.bind(state) })
        toggle!!.setOnCheckedChangeListener(
            OnCheckedChangeListener { button: CompoundButton?, checked: Boolean ->
                if (!binding) model!!.change(checked)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (model != null) model!!.refresh()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out!!)
        pages!!.save(out!!)
    }

    private fun addControlPage(body: LinearLayout?) {
        val hero = Ui.card(body!!, com.google.android.material.R.attr.colorPrimaryContainer)
        val icon = ImageView(this)
        icon.setImageResource(R.drawable.ic_guard)
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        hero.addView(icon, LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)))
        Ui.text(
            hero,
            getString(R.string.device_scope),
            com.google.android.material.R.style.TextAppearance_Material3_LabelLarge,
        )
        title =
            Ui.text(
                hero,
                getString(R.string.state_loading),
                com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall,
            )
        title!!.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE)
        detail =
            Ui.text(
                hero,
                "",
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
            )
        progress = LinearProgressIndicator(this)
        progress!!.setIndeterminate(true)
        progress!!.setVisibility(View.GONE)
        hero.addView(progress, LayoutParams(-1, -2))
        val control = Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        toggle = MaterialSwitch(this)
        toggle!!.setId(R.id.guard_switch)
        toggle!!.setText(R.string.block_title)
        toggle!!.setTextAppearance(
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium
        )
        toggle!!.setMinHeight(Ui.dp(this, 64))
        toggle!!.setEnabled(false)
        control.addView(toggle, LayoutParams(-1, -2))
        Ui.text(
            control,
            getString(R.string.root_note),
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        restore = button(control, R.string.restore, Runnable { model!!.change(false) })
        restore!!.setVisibility(View.GONE)
        error =
            Ui.text(
                control,
                "",
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
            )
        error!!.setTextColor(Ui.color(error, androidx.appcompat.R.attr.colorError))
        error!!.setVisibility(View.GONE)
        val services = Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        Ui.text(
            services,
            getString(R.string.services_title),
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
        )
        for (name in UpdatePackages.ALL!!) {
            val row =
                Ui.text(
                    services,
                    name,
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                )
            row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8))
            serviceRows.put(name, row)
        }
        refresh = button(services, R.string.refresh, Runnable { model!!.refresh() })
        val scope = Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        Ui.text(
            scope,
            getString(R.string.scope_title),
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
        )
        Ui.text(
            scope,
            getString(R.string.scope_body),
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        Ui.text(
            scope,
            getString(R.string.status_external_note),
            com.google.android.material.R.style.TextAppearance_Material3_BodySmall,
        )
    }

    private fun addInfoPage(body: LinearLayout?) {
        val appearance =
            Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        Ui.text(
            appearance,
            getString(R.string.appearance),
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
        )
        val field = TextInputLayout(this)
        field.setHint(R.string.language)
        field.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)
        field.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU)
        val choice = MaterialAutoCompleteTextView(field.getContext())
        choice.setInputType(0)
        val labels = arrayOf<String?>(getString(R.string.system_default), "日本語", "English")
        val tags = arrayOf<String?>("", "ja", "en")
        choice.setSimpleItems(labels)
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val selected: Int = if (current == "ja") 1 else if (current == "en") 2 else 0
        choice.setText(labels[selected], false)
        choice.setOnItemClickListener(
            OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tags[position])
                )
            }
        )
        field.addView(choice)
        appearance.addView(field, LayoutParams(-1, -2))
        val about = Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        Ui.text(
            about,
            getString(R.string.about_title),
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
        )
        Ui.text(
            about,
            getString(R.string.version, BuildConfig.VERSION_NAME),
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        Ui.text(
            about,
            getString(R.string.about_body),
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        button(
            about,
            R.string.legal_title,
            Runnable { startActivity(Intent(this, LicensesActivity::class.java)) },
        )
        val privacy = Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        Ui.text(
            privacy,
            getString(R.string.privacy_title),
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
        )
        Ui.text(
            privacy,
            getString(R.string.privacy_body),
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
        val removal = Ui.card(body!!, com.google.android.material.R.attr.colorSurfaceContainerLow)
        Ui.text(
            removal,
            getString(R.string.important_title),
            com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,
        )
        Ui.text(
            removal,
            getString(R.string.important_body),
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
        )
    }

    private fun button(parent: LinearLayout, label: Int, action: Runnable): MaterialButton {
        val button =
            MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            )
        button.setText(label)
        button.setMinHeight(Ui.dp(this, 48))
        button.setOnClickListener(OnClickListener { view: View? -> action.run() })
        val params = LayoutParams(-1, -2)
        params.topMargin = Ui.dp(this, 12)
        parent.addView(button, params)
        return button
    }

    private fun bind(state: GuardModel.State) {
        val owned: Boolean = state.journal != null
        val requested: Boolean = owned && state.journal.requested
        val stopped =
            state.targets.values.stream().filter({ t -> UpdatePackages.stopped(t.state) }).count()
        val complete: Boolean = state.targets.size == UpdatePackages.ALL!!.size
        val blocked: Boolean = complete && stopped == state.targets.size.toLong()
        val heading: Int
        val description: Int
        if (!PackageBackend.supportedDevice() || !complete) {
            heading = R.string.state_unsupported
            description =
                if (state.targets!!.isEmpty()) R.string.no_services else R.string.unsupported_detail
        } else if (owned && !requested) {
            heading = R.string.state_recovery
            description = R.string.recovery_detail
        } else if (blocked) {
            heading = if (owned) R.string.state_blocked else R.string.state_external
            description = R.string.blocked_detail
        } else if (stopped > 0 || requested) {
            heading = R.string.state_partial
            description = R.string.partial_detail
        } else {
            heading = R.string.state_open
            description = R.string.open_detail
        }
        title!!.setText(if (state.busy) R.string.working else heading)
        detail!!.setText(description)
        binding = true
        toggle!!.setChecked(requested)
        toggle!!.setEnabled(!state.busy && (complete || owned))
        binding = false
        restore!!.setVisibility(if (owned) View.VISIBLE else View.GONE)
        restore!!.setEnabled(!state.busy)
        refresh!!.setEnabled(!state.busy)
        progress!!.setVisibility(if (state.busy) View.VISIBLE else View.GONE)
        error!!.setVisibility(if (state.error != 0) View.VISIBLE else View.GONE)
        if (state.error != 0) error!!.setText(state.error)
        serviceRows.forEach { (name: String?, row: MaterialTextView?) ->
            val target = state.targets!!.get(name)
            if (target == null) row!!.setText(name + "\n" + getString(R.string.service_missing))
            else
                row!!.setText(
                    (target.label +
                        "  ·  " +
                        getString(
                            if (UpdatePackages.stopped(target.state)) R.string.service_disabled
                            else R.string.service_enabled
                        ) +
                        "\n" +
                        name +
                        "  ·  " +
                        target.version)
                )
        }
    }
}
