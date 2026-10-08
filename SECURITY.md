# Security

Newt Phone handles phone calls, contacts and the call log, so security
reports are welcome.

## Reporting a problem

Please report security problems **privately**: on this repository, open the
**Security** tab and choose **Report a vulnerability**. Please don't open a
public issue for them.

This is a one-person hobby project, so replies may take a few days. I'll fix
confirmed problems as soon as I can and credit you if you'd like.

## How the app protects your data

- **No internet permission.** The app can't send data off the phone. A check
  in every build fails if any permission outside `config/allowed-permissions.txt`
  is added.
- **Only Android can reach the call parts.** The call service, the spam
  screening service and the call-notification buttons accept requests only
  from Android itself or from the app. The one screen other apps can open
  (for "dial this number" links) only fills in the dialpad; it never places a
  call.
- **Ringing announcements are double-checked** with Android, so another app
  can't fake a ringing call to make the phone speak.
- **Data stays private.** Settings, the block list and the screening log live
  in the app's private storage and are excluded from backups. Logs never
  include phone numbers or names.

## How releases are protected

- Releases are built and signed by GitHub Actions from the `main` branch only,
  with a signing key stored as a GitHub secret. Pull requests from forks never
  see that key.
- Every change passes tests, Android lint (which includes security checks),
  the permission check and a scan for leaked passwords or keys before release.
- The build tools are pinned to exact versions, and Dependabot proposes
  updates, which go through the same checks.
