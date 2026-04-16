# fcitx5-android

[Fcitx5](https://github.com/fcitx/fcitx5) input method framework and engines ported to Android.

首先感谢原fcitx5-android开发团队和fxliang佬的定制版本，本仓库基于fxliang佬版 fcitx5-android 通过VibeCoding进一步定制优化，增加了一些实用的功能，希望大家能够喜欢，包名为 `org.fcitx.fcitx5.android.pt`，可与原版共存。

## 在fxliang佬基础上做了些更改，主要也是通过主要也是通过VibeCoding实现

如下：
- 增加了一些常用的定制，基本所有的按键现在都能自定义上下左右滑动功能。退格键新增上滑删除全部，空格键支持上下滑动功能定制，例如切换输入法。
<img width="126" height="280" alt="1a30427abd2371cb19ba68b98d055daa" src="https://github.com/user-attachments/assets/8af6b319-d33a-4120-b282-f83f0f72f5e8" />
<img width="126" height="280" alt="508099c9f34c952565dc20fa145b3814" src="https://github.com/user-attachments/assets/b329ae08-8848-412f-a406-76cb1be585db" />
<img width="126" height="280" alt="e3581919d729a49b330858ae8e29ac04" src="https://github.com/user-attachments/assets/6e6a699c-6c06-4144-aad3-e57a615cfbcf" />

- 然后就是一些界面上的优化，主要是便于更好地设置。
<img width="126" height="280" alt="e1e02ebc641275835867aea393ad1d71" src="https://github.com/user-attachments/assets/5c792b2a-5aea-43d4-b40f-0f671c2eedfa" />
<img width="126" height="280" alt="be62f39fc089d73f0f528a355c1b87d9" src="https://github.com/user-attachments/assets/51e45596-a70c-4337-8864-f07609c45e4f" />
<img width="126" height="280" alt="ce93c9ce0d6cdca5e4b4394f747dfebf" src="https://github.com/user-attachments/assets/f4e4b065-3762-4275-8e2c-ae3216beb5a4" />

- 支持多语言设置。
<img width="126" height="280" alt="4f132eacf53323db31d7464468483785" src="https://github.com/user-attachments/assets/f05b07db-8cba-47c8-af4a-c094da23cc7e" />

- 按键支持设置字母下方文字提示。支持对每个方向滑动单独设置气泡提示 
<img width="126" height="280" alt="c972b64df92dd99634e75fddba3554f3" src="https://github.com/user-attachments/assets/95e88975-038a-4c96-8b09-aa4789b64ab3" />

** 大体上就这些，目前可能仍有bug，如果发现问题欢迎提出来，现在有些显示仍有问题，我会慢慢修复。 **

## Download

[<img src="https://github.com/rubenpgrady/get-it-on-github/raw/refs/heads/main/get-it-on-github.png" alt="Git it on GitHub" width="207" height="80">](https://github.com/fcitx5-android/fcitx5-android/releases/latest)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" width="207" height="80">](https://f-droid.org/packages/org.fcitx.fcitx5.android)
[<img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="207" height="80">](https://play.google.com/store/apps/details?id=org.fcitx.fcitx5.android)

You can also download the **latest CI build** on our Jeninks server: [![build status](https://img.shields.io/jenkins/build.svg?jobUrl=https://jenkins.fcitx-im.org/job/android/job/fcitx5-android/)](https://jenkins.fcitx-im.org/job/android/job/fcitx5-android/)

## Project status

### Supported Languages

- English (with spell check)
- Chinese
  - Pinyin, Shuangpin, Wubi, Cangjie and custom tables (built-in, powered by [fcitx5-chinese-addons](https://github.com/fcitx/fcitx5-chinese-addons))
  - Zhuyin/Bopomofo (via [Chewing Plugin](./plugin/chewing))
  - Jyutping (via [Jyutping Plugin](./plugin/jyutping/), powered by [libime-jyutping](https://github.com/fcitx/libime-jyutping))
- Vietnamese (via [UniKey Plugin](./plugin/unikey), supports Telex, VNI and VIQR)
- Japanese (via [Anthy Plugin](./plugin/anthy))
- Korean (via [Hangul Plugin](./plugin/hangul))
- Sinhala (via [Sayura Plugin](./plugin/sayura))
- Thai (via [Thai Plugin](./plugin/thai))
- Generic (via [RIME Plugin](./plugin/rime), supports importing custom schemas)

### Implemented Features

- Virtual Keyboard (layout not customizable yet)
- Expandable candidate view
- Clipboard management (plain text only)
- Theming (custom color scheme, background image and dynamic color aka monet color after Android 12)
- Popup preview on key press
- Long press popup keyboard for convenient symbol input
- Symbol and Emoji picker
- Plugin System for loading addons from other installed apk
- Floating candidates panel when using physical keyboard

### Planned Features

- Customizable keyboard layout
- More input methods (via plugin)

## Build

### Dependencies

- Android SDK Platform & Build-Tools 35.
- Android NDK (Side by side) 25 & CMake 3.22.1, they can be installed using SDK Manager in Android Studio or `sdkmanager` command line.
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)
- GNU Gettext >= 0.20 (for `msgfmt` binary; or install `appstream` if you really have to use gettext <= 0.19.)

### How to set up development environment

<details>
<summary>Prerequisites for Windows</summary>

- Enable [Developer Mode](https://learn.microsoft.com/en-us/windows/apps/get-started/enable-your-device-for-development) so that symlinks can be created without administrator privilege.

- Enable symlink support for `git`:

    ```shell
    git config --global core.symlinks true
    ```

</details>

First, clone this repository and fetch all submodules:

```shell
git clone <your-repo-url>
git submodule update --init --recursive
```

Build the pt variant:

```shell
./gradlew :app:assemblePtDebug
./gradlew :app:assemblePtRelease
```

Build the rime plugin for pt variant:

```shell
./gradlew :plugin:rime:assembleRelease
```

### Trouble-shooting

- Android Studio indexing takes forever to complete and consumes a lot of memory.

    Switch to "Project" view in the "Project" tool window (namely the file tree side bar), right click `lib/fcitx5/src/main/cpp/prebuilt` directory, then select "Mark Directory as > Excluded". You may also need to restart the IDE to interrupt ongoing indexing process.

- Gradle error: "No variants found for ':app'. Check build files to ensure at least one variant exists." or "[CXX1210] <whatever>/CMakeLists.txt debug|arm64-v8a : No compatible library found"

    Examine if there are environment variables set such as `_JAVA_OPTIONS` or `JAVA_TOOL_OPTIONS`. You might want to clear them (maybe in the startup script `studio.sh` of Android Studio), as some gradle plugin treats anything in stderr as errors and aborts.
