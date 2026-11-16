# Test report

## Automated (run by GitHub Actions on every push)
* `WavHeaderTest` – header fields, parse round-trip, garbage rejection
* `FormatTest` – duration and size formatting
* `EnhancerTest` – normalisation reaches about -1 dBFS, original file is byte-identical afterwards, non-WAV rejected

Check the latest run on the Actions tab for the real result.

## Manual device checklist (not yet executed – needs a physical phone)
- [ ] Record 10 s, save, play back in the app and in another player
- [ ] Record with screen off for 10+ min, file plays fully
- [ ] Pause/resume from the app and from the notification
- [ ] Receive a call during recording (expected: audio may be silent during the call)
- [ ] Connect/disconnect headset while recording
- [ ] Rotate the screen while recording
- [ ] Kill the app from recents while recording → reopen: file is recovered in the archive
- [ ] Fill storage → recording stops and is saved
- [ ] Enhance a file → new file appears, original unchanged
- [ ] Calls screen shows device info and the limitation text

## Honest status
Recording quality, long-duration stability and call behaviour have **not** been verified on hardware by the author of this commit; the code was built by CI only.
