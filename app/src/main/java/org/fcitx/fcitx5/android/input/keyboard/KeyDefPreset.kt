/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Border
import org.fcitx.fcitx5.android.input.keyboard.KeyDef.Appearance.Variant
import org.fcitx.fcitx5.android.input.picker.PickerWindow

// Import Macro types
import org.fcitx.fcitx5.android.input.keyboard.MacroAction
import org.fcitx.fcitx5.android.input.keyboard.MacroStep
import org.fcitx.fcitx5.android.input.keyboard.KeyRef

val NumLockState = KeyStates(KeyState.NumLock, KeyState.Virtual)

class SymbolKey(
    val symbol: String,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.Text(
        displayText = symbol,
        textSize = 23f,
        percentWidth = percentWidth,
        variant = variant,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(symbol))
    ) + extraBehaviors,
    popup ?: arrayOf(
        Popup.Preview(symbol),
        Popup.Keyboard(symbol)
    )
)

class AlphabetKey(
    val character: String,
    val punctuation: String,
    val displayText: String = character,
    val hintText: String? = null,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null,
    weight: Float? = null,
    textColor: Int? = null,
    textColorMonet: String? = null,
    altTextColor: Int? = null,
    altTextColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    val swipeUpPopupText: String? = null,
    val swipeDownPopupText: String? = null,
    val swipeLeftPopupText: String? = null,
    val swipeRightPopupText: String? = null,
    val longPressPopupText: String? = null,
    val swipeUpPopupEnabled: Boolean = true,
    val swipeDownPopupEnabled: Boolean = true,
    val swipeLeftPopupEnabled: Boolean = true,
    val swipeRightPopupEnabled: Boolean = true,
    val longPressPopupEnabled: Boolean = true,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.AltText(
        displayText = displayText,
        altText = punctuation,
        character = character,
        hintText = hintText,
        textSize = 23f,
        variant = variant,
        percentWidth = weight ?: 0.1f,
        textColor = textColor,
        textColorMonet = textColorMonet,
        altTextColor = altTextColor,
        altTextColorMonet = altTextColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.Swipe(KeyAction.FcitxKeyAction(punctuation))
    ) + extraBehaviors,
    popup ?: buildAlphabetPopup(character, punctuation, swipeUpPopupText, swipeDownPopupText, swipeLeftPopupText, swipeRightPopupText, longPressPopupText, swipeUpPopupEnabled, swipeDownPopupEnabled, swipeLeftPopupEnabled, swipeRightPopupEnabled, longPressPopupEnabled)
) {
    companion object {
        private fun buildAlphabetPopup(
            character: String,
            punctuation: String,
            swipeUpPopupText: String?,
            swipeDownPopupText: String?,
            swipeLeftPopupText: String?,
            swipeRightPopupText: String?,
            longPressPopupText: String?,
            swipeUpPopupEnabled: Boolean,
            swipeDownPopupEnabled: Boolean,
            swipeLeftPopupEnabled: Boolean,
            swipeRightPopupEnabled: Boolean,
            longPressPopupEnabled: Boolean
        ): Array<Popup> {
            val hasAnyPopupText = swipeUpPopupText != null || swipeDownPopupText != null ||
                    swipeLeftPopupText != null || swipeRightPopupText != null || longPressPopupText != null

            val popupList = mutableListOf<Popup>()

            if (hasAnyPopupText) {
                // Use CustomAltPreview which bypasses the global popupOnKeyPress toggle
                popupList.add(Popup.CustomAltPreview(
                    content = character,
                    swipeUpPopup = swipeUpPopupText,
                    swipeDownPopup = swipeDownPopupText,
                    swipeLeftPopup = swipeLeftPopupText,
                    swipeRightPopup = swipeRightPopupText,
                    longPressPopup = longPressPopupText,
                    swipeUpEnabled = swipeUpPopupEnabled,
                    swipeDownEnabled = swipeDownPopupEnabled,
                    swipeLeftEnabled = swipeLeftPopupEnabled,
                    swipeRightEnabled = swipeRightPopupEnabled,
                    longPressEnabled = longPressPopupEnabled
                ))
            } else {
                popupList.add(Popup.AltPreview(character, punctuation))
            }

            // Only add Keyboard popup for long-press candidate display
            // Do NOT add popup texts to the long-press candidate area
            popupList.add(Popup.Keyboard(character))

            return popupList.toTypedArray()
        }
    }
}

class AlphabetDigitKey(
    val character: String,
    altText: String,
    val sym: Int,
    popup: Array<Popup>? = null
) : KeyDef(
    Appearance.AltText(
        displayText = character,
        altText = altText,
        character = character,
        textSize = 23f
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(character)),
        Behavior.Swipe(KeyAction.SymAction(KeySym(sym), NumLockState))
    ),
    popup ?: arrayOf(
        Popup.AltPreview(character, altText),
        Popup.Keyboard(character)
    )
) {
    constructor(
        char: String,
        digit: Int,
        popup: Array<Popup>? = null
    ) : this(
        char,
        digit.toString(),
        FcitxKeyMapping.FcitxKey_KP_0 + digit,
        popup
    )
}

class CapsKey(
    percentWidth: Float = 0.15f,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_capslock_none,
        viewId = R.id.button_caps,
        percentWidth = percentWidth,
        variant = Variant.Alternative,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.CapsAction(false)),
        Behavior.LongPress(KeyAction.CapsAction(true)),
        Behavior.DoubleTap(KeyAction.CapsAction(true))
    ) + extraBehaviors
)

class LayoutSwitchKey(
    displayText: String,
    val to: String = "",
    percentWidth: Float = 0.15f,
    variant: Variant = Variant.Alternative,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = 16f,
        textStyle = Typeface.BOLD,
        percentWidth = percentWidth,
        variant = variant,
        viewId = R.id.button_layout_switch,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.LayoutSwitchAction(to))
    ) + extraBehaviors,
    arrayOf(
       Popup.Menu(
        // PickerWindow symbols or numberkeyboard switch
        arrayOf(
            Popup.Menu.Item(
                "Symbols",
                R.drawable.ic_baseline_emoji_symbols_24,
                KeyAction.LayoutSwitchAction(PickerWindow.Key.Symbol.name)
            ),
            Popup.Menu.Item(
                "NumPad",
                R.drawable.ic_number_pad,
                KeyAction.LayoutSwitchAction(NumberKeyboard.Name)
            )
        )
       )
    )
)

class BackspaceKey(
    percentWidth: Float = 0.15f,
    variant: Variant = Variant.Alternative,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_backspace_24,
        percentWidth = percentWidth,
        variant = variant,
        viewId = R.id.button_backspace,
        soundEffect = InputFeedbacks.SoundEffect.Delete,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace))),
        Behavior.Repeat(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace))),
        Behavior.SwipeDir(KeyAction.DeleteAllAction, Behavior.SwipeDirection.Up)
    ) + extraBehaviors
)

class QuickPhraseKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_format_quote_24,
        variant = Variant.Alternative,
        viewId = R.id.button_quickphrase
    ),
    setOf(
        Behavior.Press(KeyAction.QuickPhraseAction),
        Behavior.LongPress(KeyAction.UnicodeAction)
    )
)

class CommaKey(
    percentWidth: Float,
    variant: Variant,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.ImageText(
        displayText = ",",
        textSize = 23f,
        percentWidth = percentWidth,
        variant = variant,
        src = R.drawable.ic_baseline_tag_faces_24,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.FcitxKeyAction(","))
    ) + extraBehaviors,
    arrayOf(
        Popup.Preview(","),
        Popup.Menu(
            arrayOf(
                Popup.Menu.Item(
                    "Emoji",
                    R.drawable.ic_baseline_tag_faces_24,
                    KeyAction.PickerSwitchAction()
                ),
                Popup.Menu.Item(
                    "QuickPhrase",
                    R.drawable.ic_baseline_format_quote_24,
                    KeyAction.QuickPhraseAction
                ),
                Popup.Menu.Item(
                    "Unicode",
                    R.drawable.ic_logo_unicode,
                    KeyAction.UnicodeAction
                )
            )
        )
    )
)

class LanguageKey(
    percentWidth: Float = 0.1f,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_language_24,
        variant = Variant.AltForeground,
        viewId = R.id.button_lang,
        percentWidth = percentWidth,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.LangSwitchAction),
        Behavior.LongPress(KeyAction.ShowInputMethodPickerAction)
    ) + extraBehaviors
)

class SpaceKey(
    percentWidth: Float = 0f,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet(),
    val swipeUp: MacroAction? = null,
    val swipeDown: MacroAction? = null,
    val swipeLeft: MacroAction? = null,
    val swipeRight: MacroAction? = null
) : KeyDef(
    Appearance.Text(
        displayText = " ",
        textSize = 13f,
        percentWidth = percentWidth,
        border = Border.Special,
        viewId = R.id.button_space,
        soundEffect = InputFeedbacks.SoundEffect.SpaceBar,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    buildSpaceBehaviors(swipeUp, swipeDown, swipeLeft, swipeRight) + extraBehaviors
) {
    companion object {
        private fun buildSpaceBehaviors(
            swipeUp: MacroAction?,
            swipeDown: MacroAction?,
            swipeLeft: MacroAction?,
            swipeRight: MacroAction?
        ): Set<Behavior> {
            val behaviors = mutableSetOf(
                Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_space))),
                Behavior.LongPress(KeyAction.SpaceLongPressAction)
            )
            if (swipeUp != null) {
                behaviors.add(Behavior.SwipeDir(swipeUp, Behavior.SwipeDirection.Up))
            } else {
                behaviors.add(Behavior.SwipeDir(KeyAction.SpaceSwipeUpAction, Behavior.SwipeDirection.Up))
            }
            if (swipeDown != null) {
                behaviors.add(Behavior.SwipeDir(swipeDown, Behavior.SwipeDirection.Down))
            } else {
                behaviors.add(Behavior.SwipeDir(KeyAction.SpaceSwipeDownAction, Behavior.SwipeDirection.Down))
            }
            swipeLeft?.let { behaviors.add(Behavior.SwipeDir(it, Behavior.SwipeDirection.Left)) }
            swipeRight?.let { behaviors.add(Behavior.SwipeDir(it, Behavior.SwipeDirection.Right)) }
            return behaviors
        }
    }
}

class ReturnKey(
    percentWidth: Float = 0.15f,
    textColor: Int? = null,
    textColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    extraBehaviors: Set<Behavior> = emptySet()
) : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_keyboard_return_24,
        percentWidth = percentWidth,
        variant = Variant.Accent,
        border = Border.Special,
        viewId = R.id.button_return,
        soundEffect = InputFeedbacks.SoundEffect.Return,
        textColor = textColor,
        textColorMonet = textColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Return)))
    ) + extraBehaviors,
    arrayOf(
        Popup.Menu(
            arrayOf(
                Popup.Menu.Item(
                    "Emoji", R.drawable.ic_baseline_tag_faces_24, KeyAction.PickerSwitchAction()
                )
            )
        )
    ),
)

class ImageLayoutSwitchKey(
    @DrawableRes
    icon: Int,
    to: String,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId
    ),
    setOf(
        Behavior.Press(KeyAction.LayoutSwitchAction(to))
    )
)

class ImagePickerSwitchKey(
    @DrawableRes
    icon: Int,
    to: PickerWindow.Key,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Image(
        src = icon,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId
    ),
    setOf(
        Behavior.Press(KeyAction.PickerSwitchAction(to))
    )
)

class TextPickerSwitchKey(
    text: String,
    to: PickerWindow.Key,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.AltForeground,
    viewId: Int = -1
) : KeyDef(
    Appearance.Text(
        displayText = text,
        textSize = 16f,
        percentWidth = percentWidth,
        variant = variant,
        viewId = viewId,
        textStyle = Typeface.BOLD
    ),
    setOf(
        Behavior.Press(KeyAction.PickerSwitchAction(to))
    )
)

class MiniSpaceKey : KeyDef(
    Appearance.Image(
        src = R.drawable.ic_baseline_space_bar_24,
        percentWidth = 0.15f,
        variant = Variant.Alternative,
        viewId = R.id.button_mini_space
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_space)))
    )
)

class NumPadKey(
    displayText: String,
    val sym: Int,
    textSize: Float = 16f,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal
) : KeyDef(
    Appearance.Text(
        displayText,
        textSize = textSize,
        percentWidth = percentWidth,
        variant = variant
    ),
    setOf(
        Behavior.Press(KeyAction.SymAction(KeySym(sym), NumLockState))
    )
)

/**
 * Macro 按键，支持自定义 tap/swipe/longPress 行为
 * @param label 显示文本（点击行为）
 * @param altLabel 备选显示文本（划动行为，可选）
 * @param longPressLabel 长按时在 Popup 选单中显示的标签文本（可选）
 * @param tap 点击时执行的 macro
 * @param swipe 划动时执行的 macro（可选）
 * @param longPress 长按时执行的 macro（可选）
 * @param percentWidth 按键宽度比例
 * @param variant 样式变体
 * @param popup 弹出菜单（可选）
 */
class MacroKey(
    val label: String,
    val altLabel: String? = null,
    val longPressLabel: String? = null,
    val tap: MacroAction,
    val swipe: MacroAction? = null,
    val swipeUp: MacroAction? = null,
    val swipeDown: MacroAction? = null,
    val swipeLeft: MacroAction? = null,
    val swipeRight: MacroAction? = null,
    val longPress: MacroAction? = null,
    percentWidth: Float = 0.1f,
    variant: Variant = Variant.Normal,
    popup: Array<Popup>? = null,
    textColor: Int? = null,
    textColorMonet: String? = null,
    altTextColor: Int? = null,
    altTextColorMonet: String? = null,
    backgroundColor: Int? = null,
    backgroundColorMonet: String? = null,
    shadowColor: Int? = null,
    shadowColorMonet: String? = null,
    val hintText: String? = null,
    val swipeUpPopupText: String? = null,
    val swipeDownPopupText: String? = null,
    val swipeLeftPopupText: String? = null,
    val swipeRightPopupText: String? = null,
    val longPressPopupText: String? = null,
    val swipeUpPopupEnabled: Boolean = true,
    val swipeDownPopupEnabled: Boolean = true,
    val swipeLeftPopupEnabled: Boolean = true,
    val swipeRightPopupEnabled: Boolean = true,
    val longPressPopupEnabled: Boolean = true
) : KeyDef(
    Appearance.AltText(
        displayText = label,
        altText = altLabel ?: "",
        character = label,
        textSize = 23f,
        percentWidth = percentWidth,
        variant = variant,
        textColor = textColor,
        textColorMonet = textColorMonet,
        altTextColor = altTextColor,
        altTextColorMonet = altTextColorMonet,
        backgroundColor = backgroundColor,
        backgroundColorMonet = backgroundColorMonet,
        shadowColor = shadowColor,
        shadowColorMonet = shadowColorMonet,
        hintText = hintText
    ),
    buildBehaviors(tap, swipe, swipeUp, swipeDown, swipeLeft, swipeRight, longPress),
    buildPopup(popup, tap, label, longPress, longPressLabel, swipeUpPopupText, swipeDownPopupText, swipeLeftPopupText, swipeRightPopupText, longPressPopupText, swipeUpPopupEnabled, swipeDownPopupEnabled, swipeLeftPopupEnabled, swipeRightPopupEnabled, longPressPopupEnabled)
) {
    private companion object {
        private val FCITX_SYMBOL_LABELS = mapOf(
            "grave" to "`",
            "asciitilde" to "~",
            "tilde" to "~",
            "minus" to "-",
            "underscore" to "_",
            "equal" to "=",
            "plus" to "+",
            "bracketleft" to "[",
            "braceleft" to "{",
            "bracketright" to "]",
            "braceright" to "}",
            "backslash" to "\\",
            "bar" to "|",
            "semicolon" to ";",
            "colon" to ":",
            "apostrophe" to "'",
            "quotedbl" to "\"",
            "comma" to ",",
            "less" to "<",
            "period" to ".",
            "greater" to ">",
            "slash" to "/",
            "question" to "?",
            "exclam" to "!",
            "at" to "@",
            "numbersign" to "#",
            "dollar" to "$",
            "percent" to "%",
            "asciicircum" to "^",
            "ampersand" to "&",
            "asterisk" to "*",
            "parenleft" to "(",
            "parenright" to ")",
            "bracket_l" to "[",
            "bracket_r" to "]",
            "multiply" to "*",
            "add" to "+",
            "subtract" to "-",
            "divide" to "/",
            "separator" to ",",
            "kp_0" to "0",
            "kp_1" to "1",
            "kp_2" to "2",
            "kp_3" to "3",
            "kp_4" to "4",
            "kp_5" to "5",
            "kp_6" to "6",
            "kp_7" to "7",
            "kp_8" to "8",
            "kp_9" to "9",
            "kp_add" to "+",
            "kp_subtract" to "-",
            "kp_multiply" to "*",
            "kp_divide" to "/",
            "kp_decimal" to ".",
            "kp_equal" to "=",
            "kp_separator" to ",",
            "kp_tab" to "Tab",
            "kp_space" to "Space",
            "kp_enter" to "Enter"
        )

        fun buildBehaviors(
            tap: MacroAction,
            swipe: MacroAction?,
            swipeUp: MacroAction?,
            swipeDown: MacroAction?,
            swipeLeft: MacroAction?,
            swipeRight: MacroAction?,
            longPress: MacroAction?
        ): Set<Behavior> {
            return buildSet {
                add(Behavior.Press(tap))
                swipe?.let { add(Behavior.Swipe(it)) }
                swipeUp?.let { add(Behavior.SwipeDir(it, Behavior.SwipeDirection.Up)) }
                swipeDown?.let { add(Behavior.SwipeDir(it, Behavior.SwipeDirection.Down)) }
                swipeLeft?.let { add(Behavior.SwipeDir(it, Behavior.SwipeDirection.Left)) }
                swipeRight?.let { add(Behavior.SwipeDir(it, Behavior.SwipeDirection.Right)) }
                longPress?.let { add(Behavior.LongPress(it)) }
            }
        }

        /**
         * Build popup based on macro content
         * - If swipe popup texts are configured, use CustomAltPreview
         * - If longPress macro is configured, it appears as the first candidate in the popup
         * - If tap macro has only one tap key, generate popup for that key
         * - Otherwise, generate popup based on label (preview on press, menu on long press)
         */
        fun buildPopup(
            explicitPopup: Array<Popup>?,
            tap: MacroAction,
            label: String,
            longPress: MacroAction? = null,
            longPressLabel: String? = null,
            swipeUpPopupText: String? = null,
            swipeDownPopupText: String? = null,
            swipeLeftPopupText: String? = null,
            swipeRightPopupText: String? = null,
            longPressPopupText: String? = null,
            swipeUpPopupEnabled: Boolean = true,
            swipeDownPopupEnabled: Boolean = true,
            swipeLeftPopupEnabled: Boolean = true,
            swipeRightPopupEnabled: Boolean = true,
            longPressPopupEnabled: Boolean = true
        ): Array<Popup>? {
            // If explicit popup is provided, use it
            if (explicitPopup != null) {
                return explicitPopup
            }

            val popupList = mutableListOf<Popup>()

            val hasAnyPopupText = swipeUpPopupText != null || swipeDownPopupText != null ||
                    swipeLeftPopupText != null || swipeRightPopupText != null || longPressPopupText != null

            if (hasAnyPopupText) {
                // Use CustomAltPreview for direction-specific popup hints
                popupList.add(Popup.CustomAltPreview(
                    content = label,
                    swipeUpPopup = swipeUpPopupText,
                    swipeDownPopup = swipeDownPopupText,
                    swipeLeftPopup = swipeLeftPopupText,
                    swipeRightPopup = swipeRightPopupText,
                    longPressPopup = longPressPopupText,
                    swipeUpEnabled = swipeUpPopupEnabled,
                    swipeDownEnabled = swipeDownPopupEnabled,
                    swipeLeftEnabled = swipeLeftPopupEnabled,
                    swipeRightEnabled = swipeRightPopupEnabled,
                    longPressEnabled = longPressPopupEnabled
                ))
            } else {
                // Check if there's exactly one tap step with one key
                val singleTapKey = if (tap.steps.size == 1 && tap.steps[0] is MacroStep.Tap) {
                    val tapStep = tap.steps[0] as MacroStep.Tap
                    if (tapStep.keys.size == 1) tapStep.keys[0] else null
                } else null

                val otherPopups = if (singleTapKey != null) {
                    // Generate popup based on the single key
                    when (singleTapKey) {
                        is KeyRef.Fcitx -> {
                            val display = FCITX_SYMBOL_LABELS[singleTapKey.code.lowercase()] ?: singleTapKey.code
                            // Keep AlphabetKey-like alt preview for letters.
                            if (display.length == 1 && display[0].isLetter()) {
                                val upper = display.uppercase()
                                arrayOf(
                                    Popup.AltPreview(display, upper),
                                    Popup.Keyboard(display)
                                )
                            } else {
                                // Symbol/emoji/non-letter keys should still expose popup keyboard like SymbolKey.
                                arrayOf(
                                    Popup.Preview(display),
                                    Popup.Keyboard(display)
                                )
                            }
                        }
                        is KeyRef.Android -> {
                            // Android key codes show as numbers
                            arrayOf(Popup.Preview(singleTapKey.code.toString()))
                        }
                    }
                } else {
                    // Non-single-tap case: generate popup based on label
                    // If label is single letter, generate same popup as AlphabetKey
                    if (label.length == 1 && label[0].isLetter()) {
                        val upper = label.uppercase()
                        arrayOf(
                            Popup.AltPreview(label, upper),
                            Popup.Keyboard(label)
                        )
                    } else {
                        // Non-letter labels should still expose popup keyboard.
                        arrayOf(
                            Popup.Preview(label),
                            Popup.Keyboard(label)
                        )
                    }
                }
                popupList.addAll(otherPopups)
            }

            // Register LongPressKeyboard after previews so its gesture listener runs first on key-up.
            // The longPress macro still appears as the first candidate inside popup keyboard UI.
            if (longPress != null) {
                val displayLabel = longPressLabel?.takeIf { it.isNotBlank() } ?: label
                popupList.add(Popup.LongPressKeyboard(displayLabel, longPress, label))
            }
            return popupList.toTypedArray()
        }
    }
}
