# Export of "site manager" and "authorization" (design, nothing wired)

Reference (video 5, OBSERVED): the export dialog lists six entries, including site manager and authorization. What the reference file contains is NOT visible, so file-format parity is UNKNOWN.

## What the app stores (read from code)
- Site manager (`sites` table): domain, username, user agent are plain; password and custom cookies are sealed with a device Keystore key (they cannot be read on another device).
- Authorization (per download): saved cookies and headers, sealed the same way.
Because of the Keystore key, a raw copy of the database values is useless on a new device, and writing them decrypted would put credentials in clear.

## Options (user's choice, none implemented in the export yet)
A. Passphrase-sealed secrets. The user types a passphrase at export; secrets go into a section sealed with it (core/security/PassphraseEnvelope.kt: PBKDF2-SHA256 310k iterations, AES-256-GCM, random salt and IV; a wrong passphrase or any change fails). Import asks for the passphrase. Works across devices. Risk: a weak passphrase can be guessed offline from a stolen file; a lost passphrase means lost secrets.
B. Sites without secrets. Domain, username and user agent only; passwords and cookies are left out and the user re-enters them. Safest, loses convenience.
C. Keep the entries but disabled with a clear label until A or B is chosen. Not a working option, must not be shown as one.

Rules kept: no plaintext credential export, ever. Authorization (cookies/headers) is only ever exported under option A.
Open question for the user: A, B, or A for sites plus B for authorization?

## Status
- PassphraseEnvelope + 5 tests: WRITTEN-UNBUILT, tests not run.
- ExportSelection.writable() still drops AUTHORIZATION; SITE_MANAGER is not exported yet.
- The dialog shows four working entries until a choice is made. This is a tracked gap, not a scope decision.
