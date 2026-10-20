package dev.ryuya.galaxyupdateguard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.*;
import com.google.android.material.textview.MaterialTextView;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainActivity extends AppCompatActivity {
  private GuardModel model;
  private SettingsTabs pages;
  private MaterialTextView title, detail, error;
  private MaterialSwitch toggle;
  private MaterialButton restore, refresh;
  private LinearProgressIndicator progress;
  private final Map<String, MaterialTextView> serviceRows = new LinkedHashMap<>();
  private boolean binding;

  @Override
  public void onCreate(Bundle saved) {
    DynamicColors.applyToActivityIfAvailable(this);
    super.onCreate(saved);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    LinearLayout root = Ui.column(this, 0);
    setContentView(root);
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, insets) -> {
          var bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
          view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    ViewCompat.requestApplyInsets(root);
    var heading = Ui.column(this, 24);
    Ui.text(
        heading,
        getString(R.string.app_name),
        com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
    root.addView(heading);
    pages = new SettingsTabs(this, root, saved);
    addControlPage(pages.page(R.string.tab_control));
    addInfoPage(pages.page(R.string.tab_info));
    pages.restore();
    model = new ViewModelProvider(this).get(GuardModel.class);
    model.state().observe(this, this::bind);
    toggle.setOnCheckedChangeListener(
        (button, checked) -> {
          if (!binding) model.change(checked);
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (model != null) model.refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    pages.save(out);
  }

  private void addControlPage(LinearLayout body) {
    LinearLayout hero = Ui.card(body, com.google.android.material.R.attr.colorPrimaryContainer);
    var icon = new ImageView(this);
    icon.setImageResource(R.drawable.ic_guard);
    icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    hero.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)));
    Ui.text(
        hero,
        getString(R.string.device_scope),
        com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
    title =
        Ui.text(
            hero,
            getString(R.string.state_loading),
            com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);
    title.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    detail =
        Ui.text(hero, "", com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    progress = new LinearProgressIndicator(this);
    progress.setIndeterminate(true);
    progress.setVisibility(View.GONE);
    hero.addView(progress, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout control =
        Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    toggle = new MaterialSwitch(this);
    toggle.setId(R.id.guard_switch);
    toggle.setText(R.string.block_title);
    toggle.setTextAppearance(
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    toggle.setMinHeight(Ui.dp(this, 64));
    toggle.setEnabled(false);
    control.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
    Ui.text(
        control,
        getString(R.string.root_note),
        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    restore = button(control, R.string.restore, () -> model.change(false));
    restore.setVisibility(View.GONE);
    error =
        Ui.text(
            control, "", com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    error.setTextColor(Ui.color(error, androidx.appcompat.R.attr.colorError));
    error.setVisibility(View.GONE);
    LinearLayout services =
        Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    Ui.text(
        services,
        getString(R.string.services_title),
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    for (String name : UpdatePackages.ALL) {
      var row =
          Ui.text(
              services,
              name,
              com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
      row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
      serviceRows.put(name, row);
    }
    refresh = button(services, R.string.refresh, () -> model.refresh());
    LinearLayout scope = Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    Ui.text(
        scope,
        getString(R.string.scope_title),
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    Ui.text(
        scope,
        getString(R.string.scope_body),
        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    Ui.text(
        scope,
        getString(R.string.status_external_note),
        com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
  }

  private void addInfoPage(LinearLayout body) {
    LinearLayout appearance =
        Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    Ui.text(
        appearance,
        getString(R.string.appearance),
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    var field = new TextInputLayout(this);
    field.setHint(R.string.language);
    field.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
    field.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
    var choice = new MaterialAutoCompleteTextView(field.getContext());
    choice.setInputType(0);
    String[] labels = {getString(R.string.system_default), "日本語", "English"};
    String[] tags = {"", "ja", "en"};
    choice.setSimpleItems(labels);
    String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
    int selected = current.equals("ja") ? 1 : current.equals("en") ? 2 : 0;
    choice.setText(labels[selected], false);
    choice.setOnItemClickListener(
        (parent, view, position, id) ->
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(tags[position])));
    field.addView(choice);
    appearance.addView(field, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout about = Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    Ui.text(
        about,
        getString(R.string.about_title),
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    Ui.text(
        about,
        getString(R.string.version, BuildConfig.VERSION_NAME),
        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    Ui.text(
        about,
        getString(R.string.about_body),
        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    button(
        about, R.string.legal_title, () -> startActivity(new Intent(this, LicensesActivity.class)));
    LinearLayout privacy =
        Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    Ui.text(
        privacy,
        getString(R.string.privacy_title),
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    Ui.text(
        privacy,
        getString(R.string.privacy_body),
        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    LinearLayout removal =
        Ui.card(body, com.google.android.material.R.attr.colorSurfaceContainerLow);
    Ui.text(
        removal,
        getString(R.string.important_title),
        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
    Ui.text(
        removal,
        getString(R.string.important_body),
        com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
  }

  private MaterialButton button(LinearLayout parent, int label, Runnable action) {
    var button =
        new MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    button.setText(label);
    button.setMinHeight(Ui.dp(this, 48));
    button.setOnClickListener(view -> action.run());
    var params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = Ui.dp(this, 12);
    parent.addView(button, params);
    return button;
  }

  private void bind(GuardModel.State state) {
    boolean owned = state.journal() != null;
    boolean requested = owned && state.journal().requested();
    long stopped =
        state.targets().values().stream().filter(t -> UpdatePackages.stopped(t.state())).count();
    boolean complete = state.targets().size() == UpdatePackages.ALL.size();
    boolean blocked = complete && stopped == state.targets().size();
    int heading, description;
    if (!PackageBackend.supportedDevice() || !complete) {
      heading = R.string.state_unsupported;
      description = state.targets().isEmpty() ? R.string.no_services : R.string.unsupported_detail;
    } else if (owned && !requested) {
      heading = R.string.state_recovery;
      description = R.string.recovery_detail;
    } else if (blocked) {
      heading = owned ? R.string.state_blocked : R.string.state_external;
      description = R.string.blocked_detail;
    } else if (stopped > 0 || requested) {
      heading = R.string.state_partial;
      description = R.string.partial_detail;
    } else {
      heading = R.string.state_open;
      description = R.string.open_detail;
    }
    title.setText(state.busy() ? R.string.working : heading);
    detail.setText(description);
    binding = true;
    toggle.setChecked(requested);
    toggle.setEnabled(!state.busy() && (complete || owned));
    binding = false;
    restore.setVisibility(owned ? View.VISIBLE : View.GONE);
    restore.setEnabled(!state.busy());
    refresh.setEnabled(!state.busy());
    progress.setVisibility(state.busy() ? View.VISIBLE : View.GONE);
    error.setVisibility(state.error() != 0 ? View.VISIBLE : View.GONE);
    if (state.error() != 0) error.setText(state.error());
    serviceRows.forEach(
        (name, row) -> {
          var target = state.targets().get(name);
          if (target == null) row.setText(name + "\n" + getString(R.string.service_missing));
          else
            row.setText(
                target.label()
                    + "  ·  "
                    + getString(
                        UpdatePackages.stopped(target.state())
                            ? R.string.service_disabled
                            : R.string.service_enabled)
                    + "\n"
                    + name
                    + "  ·  "
                    + target.version());
        });
  }
}
