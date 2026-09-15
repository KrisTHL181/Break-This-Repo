//! Atomic Windows creation of a signing seed with a protected DACL.

use std::ffi::{c_void, OsStr};
use std::fs::File;
use std::io::Write;
use std::mem::size_of;
use std::os::windows::ffi::OsStrExt;
use std::os::windows::io::{AsRawHandle, FromRawHandle};
use std::path::Path;
use std::ptr::null_mut;

use windows_sys::Win32::Foundation::{
    CloseHandle, LocalFree, ERROR_ALREADY_EXISTS, ERROR_FILE_EXISTS, ERROR_INSUFFICIENT_BUFFER,
    ERROR_SUCCESS, HANDLE, INVALID_HANDLE_VALUE,
};
use windows_sys::Win32::Security::Authorization::{
    ConvertSidToStringSidW, ConvertStringSecurityDescriptorToSecurityDescriptorW, GetSecurityInfo,
    SDDL_REVISION_1, SE_FILE_OBJECT,
};
use windows_sys::Win32::Security::{
    CreateWellKnownSid, EqualSid, GetAce, GetLengthSid, GetSecurityDescriptorControl,
    GetTokenInformation, IsValidAcl, IsValidSid, TokenUser, WinLocalSystemSid, ACCESS_ALLOWED_ACE,
    ACE_HEADER, DACL_SECURITY_INFORMATION, OWNER_SECURITY_INFORMATION, PSECURITY_DESCRIPTOR, PSID,
    SECURITY_ATTRIBUTES, SECURITY_MAX_SID_SIZE, SE_DACL_PROTECTED, TOKEN_QUERY, TOKEN_USER,
};
use windows_sys::Win32::Storage::FileSystem::{
    CreateFileW, FileDispositionInfo, SetFileInformationByHandle, CREATE_NEW, DELETE,
    FILE_ALL_ACCESS, FILE_ATTRIBUTE_NORMAL, FILE_DISPOSITION_INFO, READ_CONTROL,
};
use windows_sys::Win32::System::SystemServices::ACCESS_ALLOWED_ACE_TYPE;
use windows_sys::Win32::System::Threading::{GetCurrentProcess, OpenProcessToken};

const SYSTEM_SID_WORDS: usize = (SECURITY_MAX_SID_SIZE as usize).div_ceil(size_of::<usize>());

struct OwnedHandle(HANDLE);

impl Drop for OwnedHandle {
    fn drop(&mut self) {
        if !self.0.is_null() && self.0 != INVALID_HANDLE_VALUE {
            // SAFETY: this wrapper uniquely owns a valid Win32 handle.
            unsafe {
                let _ = CloseHandle(self.0);
            }
        }
    }
}

struct LocalAllocation(*mut c_void);

impl Drop for LocalAllocation {
    fn drop(&mut self) {
        if !self.0.is_null() {
            // SAFETY: these buffers are allocated by LocalAlloc-backed Win32 APIs
            // and are owned exactly once by this guard.
            unsafe {
                let _ = LocalFree(self.0);
            }
        }
    }
}

/// Create `path` with a protected DACL granting full access only to the current
/// user and LocalSystem, verify that live descriptor through the opened handle,
/// then durably write the private seed. `CREATE_NEW` preserves no-overwrite
/// semantics without exposing the file under inherited parent permissions.
pub fn write_private_file(path: &Path, contents: &[u8]) -> Result<(), String> {
    create_and_finish_private_file(path, |file, user_sid| {
        verify_private_acl(file.as_raw_handle(), user_sid)
            .map_err(|error| format!("cannot verify private ACL on {}: {error}", path.display()))?;
        file.write_all(contents)
            .map_err(|error| format!("write failed: {error}"))?;
        file.sync_all()
            .map_err(|error| format!("sync failed: {error}"))?;
        Ok(())
    })
}

fn create_and_finish_private_file<F>(path: &Path, finish: F) -> Result<(), String>
where
    F: FnOnce(&mut File, PSID) -> Result<(), String>,
{
    let user_sid = current_user_sid()?;
    let user_sid_text = sid_to_string(user_sid.as_ptr().cast_mut().cast())?;
    let descriptor = private_security_descriptor(&user_sid_text)?;
    let attributes = SECURITY_ATTRIBUTES {
        nLength: size_of::<SECURITY_ATTRIBUTES>() as u32,
        lpSecurityDescriptor: descriptor.0,
        bInheritHandle: 0,
    };
    let wide_path = wide_nul(path.as_os_str())?;

    // SAFETY: the path and security descriptor remain live for the call. The
    // descriptor has a protected DACL and CREATE_NEW prevents replacement.
    let raw = unsafe {
        CreateFileW(
            wide_path.as_ptr(),
            windows_sys::Win32::Foundation::GENERIC_WRITE | READ_CONTROL | DELETE,
            0,
            &attributes,
            CREATE_NEW,
            FILE_ATTRIBUTE_NORMAL,
            null_mut(),
        )
    };
    if raw == INVALID_HANDLE_VALUE {
        let error = std::io::Error::last_os_error();
        return match error.raw_os_error().map(|code| code as u32) {
            Some(ERROR_FILE_EXISTS | ERROR_ALREADY_EXISTS) => Err(format!(
                "{} already exists — refusing to overwrite private key",
                path.display()
            )),
            _ => Err(format!(
                "cannot securely create {}: {error}",
                path.display()
            )),
        };
    }

    // SAFETY: ownership of the unique CreateFileW handle moves into File.
    let mut file = unsafe { File::from_raw_handle(raw) };
    match finish(&mut file, user_sid.as_ptr().cast_mut().cast()) {
        Ok(()) => Ok(()),
        Err(primary) => {
            // Mark this exact opened file for deletion before closing it. A
            // pathname-based cleanup could race a replacement; CREATE_NEW plus
            // share-mode zero and handle-based disposition bind cleanup to the
            // same object that failed verification/write/sync.
            let cleanup = mark_delete_on_close(file.as_raw_handle());
            drop(file);
            match cleanup {
                Ok(()) => Err(primary),
                Err(cleanup_error) => Err(format!(
                    "{primary}; additionally failed to delete incomplete private key: {cleanup_error}"
                )),
            }
        }
    }
}

fn mark_delete_on_close(handle: HANDLE) -> Result<(), String> {
    let disposition = FILE_DISPOSITION_INFO { DeleteFile: true };
    // SAFETY: handle names the newly created file and was opened with DELETE;
    // disposition is a correctly sized live FILE_DISPOSITION_INFO.
    if unsafe {
        SetFileInformationByHandle(
            handle,
            FileDispositionInfo,
            std::ptr::addr_of!(disposition).cast(),
            size_of::<FILE_DISPOSITION_INFO>() as u32,
        )
    } == 0
    {
        return Err(last_error(
            "SetFileInformationByHandle(FileDispositionInfo)",
        ));
    }
    Ok(())
}

fn current_user_sid() -> Result<Vec<usize>, String> {
    let mut raw_token = null_mut();
    // SAFETY: raw_token is a valid out-pointer and the pseudo-process handle is
    // valid for the duration of the call.
    if unsafe { OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &mut raw_token) } == 0 {
        return Err(last_error("OpenProcessToken"));
    }
    let token = OwnedHandle(raw_token);

    let mut required = 0u32;
    // First call discovers the variable TOKEN_USER allocation size.
    let first = unsafe { GetTokenInformation(token.0, TokenUser, null_mut(), 0, &mut required) };
    if first != 0
        || required == 0
        || std::io::Error::last_os_error().raw_os_error() != Some(ERROR_INSUFFICIENT_BUFFER as i32)
    {
        return Err(last_error("GetTokenInformation(size)"));
    }

    let word_count = (required as usize).div_ceil(size_of::<usize>());
    let mut buffer = vec![0usize; word_count];
    // SAFETY: the aligned allocation contains at least `required` writable bytes.
    if unsafe {
        GetTokenInformation(
            token.0,
            TokenUser,
            buffer.as_mut_ptr().cast(),
            required,
            &mut required,
        )
    } == 0
    {
        return Err(last_error("GetTokenInformation(TokenUser)"));
    }

    // SAFETY: GetTokenInformation initialized a TOKEN_USER at the buffer start.
    let sid = unsafe { (*(buffer.as_ptr().cast::<TOKEN_USER>())).User.Sid };
    if sid.is_null() || unsafe { IsValidSid(sid) } == 0 {
        return Err("current access token returned an invalid user SID".to_string());
    }
    // SAFETY: a validated SID has a finite length reported by GetLengthSid.
    let sid_len = unsafe { GetLengthSid(sid) } as usize;
    if sid_len == 0 || sid_len > SECURITY_MAX_SID_SIZE as usize {
        return Err("current access token returned an invalid user SID length".to_string());
    }
    let owned_words = sid_len.div_ceil(size_of::<usize>());
    let mut owned = vec![0usize; owned_words];
    // SAFETY: both allocations are valid for exactly sid_len bytes and do not overlap.
    unsafe {
        std::ptr::copy_nonoverlapping(sid.cast::<u8>(), owned.as_mut_ptr().cast(), sid_len);
    }
    Ok(owned)
}

fn sid_to_string(sid: PSID) -> Result<String, String> {
    let mut raw_text = null_mut();
    // SAFETY: sid is a validated SID and raw_text is a valid out-pointer.
    if unsafe { ConvertSidToStringSidW(sid, &mut raw_text) } == 0 || raw_text.is_null() {
        return Err(last_error("ConvertSidToStringSidW"));
    }
    let allocation = LocalAllocation(raw_text.cast());
    let mut length = 0usize;
    // SAFETY: ConvertSidToStringSidW returned a NUL-terminated UTF-16 string.
    unsafe {
        while *raw_text.add(length) != 0 {
            length += 1;
        }
    }
    // SAFETY: length was found within the API-owned NUL-terminated allocation.
    let units = unsafe { std::slice::from_raw_parts(raw_text, length) };
    let text = String::from_utf16(units).map_err(|_| "user SID is not valid UTF-16".to_string())?;
    drop(allocation);
    Ok(text)
}

fn private_security_descriptor(user_sid: &str) -> Result<LocalAllocation, String> {
    // D:P disables DACL inheritance. Both ACEs are explicit, non-inheriting, and
    // limited to the current user SID and LocalSystem.
    let sddl = format!("O:{user_sid}D:P(A;;FA;;;SY)(A;;FA;;;{user_sid})");
    let wide = wide_nul(OsStr::new(&sddl))?;
    let mut raw: PSECURITY_DESCRIPTOR = null_mut();
    // SAFETY: wide is NUL-terminated and raw is a valid out-pointer. The API
    // returns LocalAlloc-owned storage guarded below.
    if unsafe {
        ConvertStringSecurityDescriptorToSecurityDescriptorW(
            wide.as_ptr(),
            SDDL_REVISION_1,
            &mut raw,
            null_mut(),
        )
    } == 0
        || raw.is_null()
    {
        return Err(last_error(
            "ConvertStringSecurityDescriptorToSecurityDescriptorW",
        ));
    }
    Ok(LocalAllocation(raw))
}

fn verify_private_acl(handle: HANDLE, user_sid: PSID) -> Result<(), String> {
    let mut owner: PSID = null_mut();
    let mut dacl = null_mut();
    let mut raw_descriptor: PSECURITY_DESCRIPTOR = null_mut();
    // SAFETY: all out-pointers are valid and the file handle remains open.
    let status = unsafe {
        GetSecurityInfo(
            handle,
            SE_FILE_OBJECT,
            OWNER_SECURITY_INFORMATION | DACL_SECURITY_INFORMATION,
            &mut owner,
            null_mut(),
            &mut dacl,
            null_mut(),
            &mut raw_descriptor,
        )
    };
    if status != ERROR_SUCCESS || raw_descriptor.is_null() {
        return Err(format!(
            "GetSecurityInfo failed with Windows error {status}"
        ));
    }
    let descriptor = LocalAllocation(raw_descriptor);

    if owner.is_null() || unsafe { EqualSid(owner, user_sid) } == 0 {
        return Err("private key file is not owned by the current user".to_string());
    }
    if dacl.is_null() || unsafe { IsValidAcl(dacl) } == 0 {
        return Err("private key file has a missing or invalid DACL".to_string());
    }

    let mut control = 0u16;
    let mut revision = 0u32;
    // SAFETY: descriptor is a valid live security descriptor.
    if unsafe { GetSecurityDescriptorControl(descriptor.0, &mut control, &mut revision) } == 0 {
        return Err(last_error("GetSecurityDescriptorControl"));
    }
    if control & SE_DACL_PROTECTED == 0 {
        return Err("private key file DACL still permits inheritance".to_string());
    }

    let mut system_storage = [0usize; SYSTEM_SID_WORDS];
    let mut system_size = SECURITY_MAX_SID_SIZE;
    let system_sid = system_storage.as_mut_ptr().cast::<c_void>();
    // SAFETY: system_storage is aligned and sized to SECURITY_MAX_SID_SIZE.
    if unsafe { CreateWellKnownSid(WinLocalSystemSid, null_mut(), system_sid, &mut system_size) }
        == 0
    {
        return Err(last_error("CreateWellKnownSid(LocalSystem)"));
    }

    // The protected SDDL is intentionally exact. Reject any extra allow/deny,
    // inherited, callback, object-specific, or future ACE rather than guessing.
    // SAFETY: IsValidAcl succeeded and the descriptor owns dacl for this scope.
    let ace_count = unsafe { (*dacl).AceCount } as u32;
    if ace_count != 2 {
        return Err(format!(
            "private key file DACL has {ace_count} ACEs; expected exactly 2"
        ));
    }
    let mut saw_user = false;
    let mut saw_system = false;
    for index in 0..ace_count {
        let mut raw_ace = null_mut();
        // SAFETY: index is bounded by the validated ACL's AceCount.
        if unsafe { GetAce(dacl, index, &mut raw_ace) } == 0 || raw_ace.is_null() {
            return Err(last_error("GetAce"));
        }
        // SAFETY: GetAce returned a live ACE in the descriptor-owned ACL.
        let header = unsafe { &*(raw_ace.cast::<ACE_HEADER>()) };
        if header.AceType as u32 != ACCESS_ALLOWED_ACE_TYPE
            || header.AceFlags != 0
            || (header.AceSize as usize) < size_of::<ACCESS_ALLOWED_ACE>()
        {
            return Err("private key file DACL contains a non-canonical ACE".to_string());
        }
        // SAFETY: the size check covers the fixed ACCESS_ALLOWED_ACE fields.
        let ace = unsafe { &*(raw_ace.cast::<ACCESS_ALLOWED_ACE>()) };
        if ace.Mask != FILE_ALL_ACCESS {
            return Err("private key file ACE does not grant the expected full access".to_string());
        }
        let sid = std::ptr::addr_of!(ace.SidStart).cast_mut().cast::<c_void>();
        if unsafe { IsValidSid(sid) } == 0 {
            return Err("private key file DACL contains an invalid SID".to_string());
        }
        if unsafe { EqualSid(sid, user_sid) } != 0 {
            if saw_user {
                return Err("private key file DACL duplicates the current-user ACE".to_string());
            }
            saw_user = true;
        } else if unsafe { EqualSid(sid, system_sid) } != 0 {
            if saw_system {
                return Err("private key file DACL duplicates the LocalSystem ACE".to_string());
            }
            saw_system = true;
        } else {
            return Err("private key file DACL grants access to an unexpected SID".to_string());
        }
    }
    if !saw_user || !saw_system {
        return Err("private key file DACL is missing a required principal".to_string());
    }
    Ok(())
}

fn wide_nul(value: &OsStr) -> Result<Vec<u16>, String> {
    let mut wide = value.encode_wide().collect::<Vec<_>>();
    if wide.contains(&0) {
        return Err("Windows path or security descriptor contains an interior NUL".to_string());
    }
    wide.push(0);
    Ok(wide)
}

fn last_error(operation: &str) -> String {
    format!("{operation} failed: {}", std::io::Error::last_os_error())
}

#[cfg(test)]
mod tests {
    use super::{create_and_finish_private_file, write_private_file};
    use rand_core::{OsRng, RngCore};
    use std::io::Write as _;
    use std::path::PathBuf;

    struct TestDirectory(PathBuf);

    impl Drop for TestDirectory {
        fn drop(&mut self) {
            let _ = std::fs::remove_dir_all(&self.0);
        }
    }

    #[test]
    fn creates_verified_private_file_and_never_overwrites() {
        let mut nonce = [0u8; 16];
        OsRng.fill_bytes(&mut nonce);
        let suffix = nonce
            .iter()
            .map(|byte| format!("{byte:02x}"))
            .collect::<String>();
        let directory = TestDirectory(std::env::temp_dir().join(format!(
            "tirith-sign-private-acl-{}-{suffix}",
            std::process::id()
        )));
        std::fs::create_dir(&directory.0).expect("create isolated test directory");
        let path = directory.0.join("signing-seed.key");

        write_private_file(&path, b"private-seed").expect("secure creation must succeed");
        assert_eq!(std::fs::read(&path).unwrap(), b"private-seed");

        let error = write_private_file(&path, b"replacement").unwrap_err();
        assert!(error.contains("refusing to overwrite private key"));
        assert_eq!(std::fs::read(&path).unwrap(), b"private-seed");
    }

    #[test]
    fn post_creation_failure_deletes_exact_file_and_allows_retry() {
        let mut nonce = [0u8; 16];
        OsRng.fill_bytes(&mut nonce);
        let suffix = nonce
            .iter()
            .map(|byte| format!("{byte:02x}"))
            .collect::<String>();
        let directory = TestDirectory(std::env::temp_dir().join(format!(
            "tirith-sign-cleanup-{}-{suffix}",
            std::process::id()
        )));
        std::fs::create_dir(&directory.0).expect("create isolated test directory");
        let path = directory.0.join("signing-seed.key");

        let error = create_and_finish_private_file(&path, |file, _user_sid| {
            file.write_all(b"partial-seed").unwrap();
            Err("forced post-creation failure".to_string())
        })
        .unwrap_err();
        assert!(error.contains("forced post-creation failure"));
        assert!(
            !path.exists(),
            "the exact partially written file must be deleted on handle close"
        );

        write_private_file(&path, b"complete-seed")
            .expect("cleanup must leave the path available for a secure retry");
        assert_eq!(std::fs::read(path).unwrap(), b"complete-seed");
    }
}
