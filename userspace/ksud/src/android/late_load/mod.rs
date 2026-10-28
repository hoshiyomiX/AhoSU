pub mod magica;

use std::{process::Command, time::Instant};

use anyhow::{Context, Result};
use log::{info, warn};
use rustix::{cstr, process::chdir};

use crate::{
    android::{
        dynamic_manager, init_event, ksucalls,
        module::{ScriptWait, handle_updated_modules, metamodule, prune_modules},
        restorecon, utils,
    },
    assets, defs,
};

fn dump_process_info(label: &str) {
    use rustix::process::{getgid, getgroups, getpid, getuid};

    let pid = getpid().as_raw_nonzero();
    let uid = getuid().as_raw();
    let gid = getgid().as_raw();
    let groups: Vec<String> = getgroups()
        .unwrap_or_default()
        .iter()
        .map(|g| g.as_raw().to_string())
        .collect();
    let selinux = std::fs::read_to_string("/proc/self/attr/current")
        .unwrap_or_else(|_| "unknown".to_string());
    let seccomp = std::fs::read_to_string("/proc/self/status")
        .ok()
        .and_then(|s| {
            s.lines()
                .find(|l| l.starts_with("Seccomp:"))
                .map(|l| l.trim().to_string())
        })
        .unwrap_or_else(|| "unknown".to_string());

    info!(
        "[{label}] pid={pid}, uid={uid}, gid={gid}, groups=[{}], selinux={}, {seccomp}",
        groups.join(","),
        selinux.trim(),
    );
}

pub fn run(package_name: &String, kmi: Option<String>, allow_shell: bool) -> Result<()> {
    // L1: switch into init's mount namespace right after daemonizing (mirrors
    // soft_reboot) so module mounts are applied globally instead of staying
    // trapped inside the caller's (e.g. an app's) mount namespace. Best-effort
    // by design: on failure we keep the caller's namespace, matching the
    // pre-patch behavior instead of aborting a jailbreak's only root path.
    utils::daemonize_with(false, || -> Result<()> {
        if let Err(e) = utils::switch_mnt_ns(1) {
            warn!("switch to init mount namespace failed: {e}");
        }
        if let Err(e) = chdir("/") {
            warn!("chdir to / failed: {e}");
        }
        Ok(())
    })?;
    info!("late-load command triggered!");
    dump_process_info("late-load start");

    // 1. Check if KernelSU is already loaded
    if ksuinit::has_kernelsu() {
        info!("KernelSU already loaded, skip loading ko");
    } else {
        // 2. Detect current KMI version
        let kmi = kmi.map_or_else(
            || crate::boot_patch::get_current_kmi().context("Failed to detect current KMI version"),
            Ok,
        )?;
        info!("Detected KMI: {kmi}");

        // 3. Get kernelsu.ko from embedded assets
        let ko_name = format!("{kmi}_kernelsu.ko");
        let ko_data = assets::get_asset(&ko_name)
            .with_context(|| format!("Failed to get {ko_name} from assets"))?;

        // 4. Load kernelsu.ko from memory with manual relocation
        info!("Loading kernelsu.ko for KMI {kmi}...");
        // bundled flag is meaningless in jailbreak mode since we can't flash boot to update it.
        let params = if allow_shell {
            cstr!("allow_shell=1")
        } else {
            cstr!("")
        };
        ksuinit::load_module(&ko_data, params).context("Failed to load kernelsu.ko")?;
        info!("kernelsu.ko loaded successfully!");
        dump_process_info("after load_module");
    }

    // We need to reset stdin/stdout/stderr; otherwise, sending file descriptors via cmd transactions
    // will be blocked by SELinux because its fsec->sid is still u:r:su:s0 instead of u:r:ksu:s0.
    utils::reset_std()?;

    utils::umask(0);

    if let Err(e) = crate::android::module::module_config::clear_all_temp_configs() {
        warn!("clear temp configs failed: {e}");
    }

    utils::install(None, None).context("Failed to install ksud")?;

    // 5. Handle module updates
    if let Err(e) = handle_updated_modules() {
        warn!("handle updated modules failed: {e}");
    }

    if let Err(e) = prune_modules() {
        warn!("prune modules failed: {e}");
    }

    if let Err(e) = restorecon::restorecon() {
        warn!("restorecon failed: {e}");
    }

    // 6. Load SELinux rules
    if crate::android::module::load_sepolicy_rule().is_err() {
        warn!("load sepolicy.rule failed");
    }

    if let Err(e) = crate::android::profile::apply_sepolies() {
        warn!("apply root profile sepolicy failed: {e}");
    }

    // 7. Initialize features
    if let Err(e) = crate::android::feature::init_features() {
        warn!("init features failed: {e}");
    }

    // 8. Execute late-load stage scripts with a shared boot deadline
    let wait = ScriptWait::Until(Instant::now() + defs::BOOT_STAGE_TIMEOUT);

    // L2: in jailbreak mode no init hook ever ran, so the post-fs-data stage
    // was skipped entirely, while modules generally expect it to have completed
    // before the late-load/service stages. Run it explicitly here, mirroring
    // the stage ordering of init_event::on_post_fs_data.
    ksucalls::report_post_fs_data();
    // B1: apply the persisted SuSFS config that must capture metadata before
    // mounts and overlays, exactly like init_event::on_post_fs_data does on
    // the normal/soft-reboot paths. Without this, the jailbreak boot only
    // picked the config up after the first soft reboot, when
    // init_event::on_boot_completed finally ran the susfs handlers.
    crate::android::susfs::init_event::on_post_fs_data();
    init_event::run_stage("post-fs-data", wait);

    init_event::run_stage("late-load", wait);

    // 9. Load system.prop
    if let Err(e) = crate::android::module::load_system_prop() {
        warn!("load system.prop failed: {e}");
    }

    // 10. Execute metamodule mount script (OverlayFS)
    if let Err(e) = metamodule::exec_mount_script(defs::MODULE_DIR) {
        warn!("execute metamodule mount failed: {e}");
    } else if metamodule::has_metamodule() {
        // L6: modules are mounted — notify the kernel so event-gated features
        // become active, like the metamodule would on a normal boot.
        ksucalls::report_module_mounted();
    }

    // B1: load the manager umount config after the mounts are in place,
    // mirroring init_event::on_post_fs_data (applied right after the
    // metamodule mount script on normal/soft-reboot boots).
    if let Err(e) = crate::android::umount_config::load_umount_config() {
        warn!("load umount config failed: {e}");
    }
    // 11. Execute dynamic manager booted load
    if let Err(e) = dynamic_manager::booted_load() {
        warn!("set dynamic manager failed: {e}");
    }

    // 12. Execute post-mount stage scripts (blocking)
    init_event::run_stage("post-mount", wait);

    // 13. Execute service stage scripts (non-blocking)
    // L6: mirror init_event::on_services — report the services event to the
    // kernel first (it gates event-driven features); unlike on_services we
    // still run the stage on a report error, keeping the pre-patch behavior.
    let run_service_stage = match ksucalls::report_services() {
        Ok(true) => true,
        Ok(false) => {
            info!("services already started, skipping service stage");
            false
        }
        Err(e) => {
            warn!("Failed to report services: {e:#}");
            true
        }
    };
    if run_service_stage {
        init_event::run_stage("service", ScriptWait::NoWait);
    }

    // 14. Execute boot-completed stage scripts (non-blocking)
    // L6: mirror init_event::on_boot_completed — report boot completion first.
    ksucalls::report_boot_complete();
    init_event::run_stage("boot-completed", ScriptWait::NoWait);
    // B1: apply the SuSFS boot-completed config (sus_cmdline, sus_map,
    // sus_path, ...) on the jailbreak boot path too — previously this only
    // ran via init_event::on_boot_completed on normal/soft-reboot boots, so a
    // jailbreak boot stayed unconfigured until the first soft reboot. The
    // safe-mode guard mirrors init_event::on_boot_completed.
    if !utils::is_safe_mode() {
        crate::android::susfs::init_event::on_boot_completed();
    }

    // 15. Restart Manager so it gets a fresh ksu fd from the newly loaded kernel module
    info!("Restarting KernelSU Manager {package_name}...");
    let _ = Command::new("am")
        .args(["force-stop", package_name])
        .status();
    relaunch_manager(package_name);

    Ok(())
}

/// Relaunch the manager after the kernel module has been loaded.
///
/// The manifest namespace can differ from the application id (AhoSU ships
/// namespace `org.bakasu.bakasu` while the package stays
/// `com.resukisu.resukisu`, and PR builds can override the package entirely),
/// and the enabled launcher may be the icon alias instead of MainActivity.
/// Starting the legacy `{package}/.ui.MainActivity` component therefore hits
/// a class that does not exist, silently leaving the manager force-stopped
/// (the "failed to restart PID" seen after every late-load). Ask the package
/// manager for the currently enabled LAUNCHER-category activity first, then
/// fall back to `monkey` (launches by package, no activity class needed), and
/// only keep the legacy relative component as the last resort.
fn relaunch_manager(package_name: &str) {
    if let Some(component) = resolve_launch_component(package_name) {
        info!("Relaunching manager as {component}");
        let _ = Command::new("am")
            .args(["start", "-n", &component])
            .status();
        return;
    }

    let monkey = Command::new("monkey")
        .args([
            "-p",
            package_name,
            "-c",
            "android.intent.category.LAUNCHER",
            "1",
        ])
        .status();
    if monkey.is_ok_and(|s| s.success()) {
        return;
    }

    let legacy = format!("{package_name}/.ui.MainActivity");
    info!("Relaunching manager as {legacy} (legacy fallback)");
    let _ = Command::new("am").args(["start", "-n", &legacy]).status();
}

/// Query the package manager for the manager's enabled launcher activity.
fn resolve_launch_component(package_name: &str) -> Option<String> {
    let output = Command::new("cmd")
        .args([
            "package",
            "resolve-activity",
            "--brief",
            "-c",
            "android.intent.category.LAUNCHER",
            package_name,
        ])
        .output()
        .ok()?;
    if !output.status.success() {
        return None;
    }
    let text = String::from_utf8_lossy(&output.stdout);
    text.lines()
        .rev()
        .map(str::trim)
        .find(|line| line.starts_with(package_name) && line.contains('/'))
        .map(ToString::to_string)
}
