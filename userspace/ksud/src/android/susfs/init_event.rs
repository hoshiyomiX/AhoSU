use anyhow::{Result, bail};
use std::{
    fs,
    io::{self, Read, Seek},
    os::fd::AsRawFd,
};

use crate::android::{
    susfs::{api::prelude as api, config::model::Config},
    utils::create_daemon,
};

fn is_fuse_mounted() -> bool {
    fs::metadata("/sdcard/Android").is_ok()
}

fn wait_for_fuse_mounted() -> Result<()> {
    if is_fuse_mounted() {
        return Ok(());
    }

    let mut file = fs::File::open("/proc/mounts")?;
    let fd = file.as_raw_fd();

    let mut pfd = libc::pollfd {
        fd,
        events: libc::POLLPRI | libc::POLLERR,
        revents: 0,
    };

    log::info!("Polling mount event");

    loop {
        let mut dummy = String::new();
        let _ = file.seek(io::SeekFrom::Start(0));
        let _ = file.read_to_string(&mut dummy);

        if is_fuse_mounted() {
            break;
        }

        let ret = unsafe { libc::poll(&mut pfd, 1, -1) };
        if ret < 0 {
            bail!("Poll failed");
        }
    }

    Ok(())
}

fn handle_result<T>(ret: Result<T>, msg: &str) {
    match ret {
        Ok(_) => log::info!("successfully {msg}!"),
        Err(e) => log::warn!("{msg} failed: {e}!"),
    }
}

pub fn on_boot_completed() {
    if !api::supercall_supported() {
        // The kernel has no SuSFS supercall surface (an LKM built without
        // CONFIG_KSU_SUSFS). Persisted SuSFS config cannot be applied to the
        // kernel on this build, so skip the apply instead of warning for
        // every single command.
        log::info!(
            "SuSFS supercalls are not supported by this kernel; skipping boot-completed apply"
        );
        return;
    }

    log::info!("Start daemonize...");
    // B1: use create_daemon instead of daemonize so the CALLING process
    // survives (daemonize _exit(0)s the caller once the daemon child is
    // spawned). The jailbreak late-load path calls this handler and still has
    // its own work to do afterwards (manager restart, post-magica adb
    // cleanup); the daemon-side behavior is unchanged (detach + reset_std +
    // double fork), and the previous callers only lose a redundant exit(0).
    match create_daemon(false) {
        // We are the daemon: continue with the FUSE wait and the config apply.
        Ok(true) => {}
        // Original process: the daemon was spawned and reaped; nothing to do.
        Ok(false) => return,
        Err(e) => {
            log::error!("unable to daemonize: {e}");
            return;
        }
    }
    log::info!("Daemonize success.");

    log::info!("Waiting /sdcard to be mounted.");
    if let Err(e) = wait_for_fuse_mounted() {
        log::error!("Wait FUSE mount failed: {e}");
    }
    log::info!("Processing SUSFS.");

    let config = Config::read_or_default();
    if !config.is_enabled() {
        log::info!("SUSFS persisted configuration is disabled; skipping boot-completed apply");
        return;
    }

    handle_result(config.apply_cmdline_or_bootconfig(), "sus_cmdline");
    handle_result(config.final_sus_kstat(), "finalize sus_kstat");
    handle_result(config.apply_sus_map(), "sus_map");
    handle_result(config.apply_open_redirect(), "open_redirect");
    handle_result(config.apply_sus_path(), "sus_path and sus_path_loop");

    log::info!("SUSFS finished");
}

pub fn on_post_fs_data() {
    if !api::supercall_supported() {
        // Same rationale as on_boot_completed: without kernel SuSFS support
        // every config command would fail with ERR_CMD_NOT_SUPPORTED.
        log::info!(
            "SuSFS supercalls are not supported by this kernel; skipping post-fs-data apply"
        );
        return;
    }

    let config = Config::read_or_default();

    log::info!("on_post_fs_data triggered!");

    if !config.is_enabled() {
        log::info!("SUSFS persisted configuration is disabled; skipping post-fs-data apply");
        return;
    }

    handle_result(config.apply_avc_log_spoofing(), "avc_config");
    handle_result(config.init_sus_kstat(), "initialize sus_kstat");
    handle_result(config.apply_logging(), "sus_log");
    handle_result(
        config.apply_hide_sus_mnts_for_non_su_procs(),
        "hide sus mnts for non su procs",
    );
    handle_result(config.apply_uname(), "sus_uname");

    log::info!("on_post_fs_data finished!");
}
