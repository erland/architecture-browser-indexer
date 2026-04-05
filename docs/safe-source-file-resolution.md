# Safe source file resolution and text-file validation

This step adds the internal safety seam that later source-read endpoints will rely on.

## Added components

- `RetainedSourceFileAccessService`
- `RetainedSourceResolvedFile`

## What the service now enforces

- source lookup starts from a valid active `sourceHandle`
- requested paths must be repository-relative
- absolute paths are rejected
- `..` traversal is rejected
- resolved paths must remain inside the retained source root
- only regular files are allowed
- symlink escapes are rejected by validating the real path against the retained root real path
- oversized files are rejected
- likely binary files are rejected using a simple NUL-byte sniff

## Current limits

- file-inventory allowlisting is not implemented yet
- binary detection is intentionally conservative and simple
- UTF-8 text reading support is included for the next endpoint step, but no HTTP endpoint is added yet

## Why this step comes before the endpoint

Adding the resolution and validation seam first makes the next HTTP step smaller and safer. The endpoint can delegate to this service rather than re-implementing filesystem checks inside transport code.
