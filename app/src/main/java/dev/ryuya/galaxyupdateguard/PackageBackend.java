package dev.ryuya.galaxyupdateguard;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import java.util.LinkedHashMap;
import java.util.Map;

final class PackageBackend implements UpdateBlocker.Backend {
  record Target(String name, String label, String version, int state) {}

  private final PackageManager packages;

  PackageBackend(Context context) {
    packages = context.getPackageManager();
  }

  static boolean supportedDevice() {
    return Build.MANUFACTURER.equalsIgnoreCase("samsung")
        && (Build.VERSION.SDK_INT == 36 || Build.VERSION.SDK_INT == 37)
        && Process.myUid() / 100000 == 0;
  }

  Map<String, Target> targets() throws Exception {
    if (!supportedDevice()) return Map.of();
    var result = new LinkedHashMap<String, Target>();
    for (String name : UpdatePackages.ALL) {
      try {
        var info = packages.getPackageInfo(name, PackageManager.MATCH_DISABLED_COMPONENTS);
        ApplicationInfo app = info.applicationInfo;
        if (app == null || (app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) continue;
        int state = packages.getApplicationEnabledSetting(name);
        result.put(
            name,
            new Target(
                name, packages.getApplicationLabel(app).toString(), info.versionName, state));
      } catch (PackageManager.NameNotFoundException missing) {
        // Carrier variants may omit one updater; never guess an unrelated replacement.
      }
    }
    return result;
  }

  @Override
  public Map<String, Integer> states() throws Exception {
    var result = new LinkedHashMap<String, Integer>();
    targets().forEach((name, target) -> result.put(name, target.state));
    return result;
  }

  @Override
  public void set(String name, int state) throws Exception {
    UpdatePackages.validate(name, state);
    if (!targets().containsKey(name)) throw new IllegalStateException("Updater identity changed");
    RootShell.run(UpdatePackages.command(name, state));
  }
}
