# FlashCards Light

A minimalist flashcard app for the Light Phone III, powered by Anki's real spaced-repetition engine.

<img width="360" height="412" alt="flashcardlightvideo-ezgif com-video-to-gif-converter" src="https://github.com/user-attachments/assets/39d44272-e4dc-47e8-b578-c838c9b2a51e" />

## Features

<<<<<<< HEAD
- Create and manage multiple decks
- Add, edit, and delete flashcards (front/back)
- Review cards using Anki's real spaced-repetition scheduler (Hard / Good / Very Good)
- Deck-scoped review — each deck tracks its own due/new cards independently
- Rename and delete decks and cards, with confirmation before deleting
- Minimalist UI following LightOS's native design language (icons, layout, confirmation patterns)
=======
## Quickstart
>>>>>>> upstream/main

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


