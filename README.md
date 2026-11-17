# FlashCards Light

A minimalist flashcard app for the Light Phone III, powered by Anki's real spaced-repetition engine.

<img width="360" height="412" alt="flashcardlightvideo-ezgif com-video-to-gif-converter" src="https://github.com/user-attachments/assets/39d44272-e4dc-47e8-b578-c838c9b2a51e" />

## Features

- Create and manage multiple decks
- Add, edit, and delete flashcards (front/back)
- Review cards using Anki's real spaced-repetition scheduler (Hard / Good / Very Good)
- Deck-scoped review — each deck tracks its own due/new cards independently
- Rename and delete decks and cards, with confirmation before deleting
- Minimalist UI following LightOS's native design language (icons, layout, confirmation patterns)

## How it works

FlashCards Light doesn't reimplement spaced repetition — it runs the real Anki scheduling engine via [anki-android-backend](https://github.com/ankidroid/Anki-Android-Backend), the same library that powers AnkiDroid. The app starts completely empty: no pre-made decks or content. Everything — decks, cards, language, subject — is created by the user.

## Built with

- Kotlin + Jetpack Compose
- [LightOS SDK](https://github.com/lightphone/light-sdk)
- [anki-android-backend](https://github.com/ankidroid/Anki-Android-Backend) (JNI bridge to Anki's Rust core)

## Credits

Built on the shoulders of years of community work:

- [Anki](https://apps.ankiweb.net/) — the original spaced-repetition software and algorithm
- [anki-android-backend](https://github.com/ankidroid/Anki-Android-Backend) — the Android bridge that made this integration possible
- [AnkiDroid](https://github.com/ankidroid/Anki-Android) — the mobile Anki client this backend was extracted from

## License

This project is licensed under **GPL-3.0** (see [LICENSE](./LICENSE)), because it depends on `anki-android-backend`, which is also GPL-3.0 (with an AGPL-3.0 submodule for the Anki core itself). Any derivative of this code must remain under a compatible license.

---

This project is a fork of [lightphone/light-sdk](https://github.com/lightphone/light-sdk), the official scaffold for building tools for the Light Phone III.

### Submitting Your Tool
Given our relatively limited resources and desire to keep our users safe, we're requiring that all community tools be open source (including our own!). 
Tools are built and signed directly from a publicly available git commit, and we'll be archiving the source at build time.
You're free to build and share privately, but LightOS won't let you install tools that are not signed by us without acknowledging privacy and performance risks.
We won't block users from performing these "dangerous" sideloads, but we're not going to encourage it either. 
Using the Dashboard, developers are able to queue up a build of your tool on our servers, and if it follows our guidelines and compiles cleanly, we will hand you back a signed, shareable APK.

1. Review the [Tool Guidelines](TOOL_GUIDELINES.md) to get a sense of the kind of tools we're hoping to showcase.
2. Ensure your tool is built with the latest Light SDK version and is accessible via a public Github repo.
  - Keep the [files the build server requires](docs/repo/README.md#required-files) when trimming the repo down to your tool.
3. Enable developer mode on the [dashboard](https://dashboard.thelightphone.com/): Settings -> Account -> Developer Account.
4. From there, you can create your tool: Settings -> Account -> Developer Account -> Manage Custom Tools -> Submit New Tool:
  - The package name is unique for all tools and can't be changed later. It is pulled from the default branch of the Github repo submitted upon registering the tool.
  - You'll then have the opportunity to add images. You can add them later on as well, but we'll require at least one for a tool to be approved.
5. Now go in the tool detail page and press "submit build":
  - Enter the git ref of the version of the tool to be built and a small description of the changelog.
  - If your tool already has an active build, you won't be able to submit another.
  - The `versionCode` of the submitted build should be higher than the one of the last version listed in the Tool page.
6. Wait for the build to finish:
  - In case of an error, use the "view details" button on the "Builds" section of the Tool page to check the error message. The "retry build" can be used to re-attempt building from the same git ref if necessary.
  - If you believe the build error is due to an issue on our end (e.g. with light-sdk or our infrastructure), please feel free to open an Issue on this repo!
  - In case of success, the "download apk" button should appear. A new "Version" entry will be created with "pending approval" status.
7. Our team will review the builds and reach out to you via e-mail if we have any feedback regarding the approval process.
8. If you have any questions, please e-mail us at tools@thelightphone.com

### Sharing Your Tool
Once we release a version of LightOS that supports community tools, users will have an option to choose what kind of tools they want to be able to run on their device:
- **Light-approved tools**: These include tools that are either built internally by the Light team, or built by the community and officially tested/signed-off by the Light team. We don't know _exactly_ what that sign-off process is going to look like, but as a heads-up: we're going to be looking pretty hard at whether a submitted tool matches the Light ethos both functionally and aesthetically. We've included a UX/UI library to make this as easy as possible! From a technical standpoint, these approved tools are both signed by us _and_ added to an "allow-list" within LightOS. Phones with this option selected will only install and display tools that meet both criteria.
- **SDK-built tools**: This is a slightly more permissive choice. Phones with this option selected will install and launch any tool that was built and signed by Light. These don't require any manual approval by us (although we can block them in extreme cases). If a user wants to be able to install a tool that was shared locally or somewhere outside of Light's dashboard, but they still want to be confident that it will run well and integrate nicely with LightOS, they might choose this option.
- **Any tools**: A user will have the option to make any APK launchable from LightOS, but they will own the responsibility of getting them installed/uninstalled. When a user selects this option, we will warn them that they are potentially opening their device up to security risks, and that doing so will limit our ability to support them if something goes wrong.
