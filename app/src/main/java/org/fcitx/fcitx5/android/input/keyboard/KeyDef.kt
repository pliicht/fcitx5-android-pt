/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.graphics.Typeface
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.data.InputFeedbacks

open class KeyDef(
    val appearance: Appearance,
    val behaviors: Set<Behavior>,
    val popup: Array<Popup>? = null
) {
    sealed class Appearance(
        val percentWidth: Float,
        val variant: Variant,
        val border: Border,
        val margin: Boolean,
        val viewId: Int,
        val soundEffect: InputFeedbacks.SoundEffect,
        val textColor: Int? = null,
        val textColorMonet: String? = null,
        val altTextColor: Int? = null,
        val altTextColorMonet: String? = null,
        val backgroundColor: Int? = null,
        val backgroundColorMonet: String? = null,
        val shadowColor: Int? = null,
        val shadowColorMonet: String? = null
    ) {
        enum class Variant {
            Normal, AltForeground, Alternative, Accent
        }

        enum class Border {
            Default, On, Off, Special
        }

        open class Text(
            val displayText: String,
            val textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            val textStyle: Int = Typeface.NORMAL,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
            soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard,
            textColor: Int? = null,
            textColorMonet: String? = null,
            altTextColor: Int? = null,
            altTextColorMonet: String? = null,
            backgroundColor: Int? = null,
            backgroundColorMonet: String? = null,
            shadowColor: Int? = null,
            shadowColorMonet: String? = null
        ) : Appearance(
            percentWidth,
            variant,
            border,
            margin,
            viewId,
            soundEffect,
            textColor,
            textColorMonet,
            altTextColor,
            altTextColorMonet,
            backgroundColor,
            backgroundColorMonet,
            shadowColor,
            shadowColorMonet
        )

        class AltText(
            displayText: String,
            val altText: String,
            val character: String,
            val hintText: String? = null,
            textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            textStyle: Int = Typeface.NORMAL,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
            textColor: Int? = null,
            textColorMonet: String? = null,
            altTextColor: Int? = null,
            altTextColorMonet: String? = null,
            backgroundColor: Int? = null,
            backgroundColorMonet: String? = null,
            shadowColor: Int? = null,
            shadowColorMonet: String? = null
        ) : Text(
            displayText,
            textSize,
            textStyle,
            percentWidth,
            variant,
            border,
            margin,
            viewId,
            InputFeedbacks.SoundEffect.Standard,
            textColor,
            textColorMonet,
            altTextColor,
            altTextColorMonet,
            backgroundColor,
            backgroundColorMonet,
            shadowColor,
            shadowColorMonet
        )

        class Image(
            @DrawableRes
            val src: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
            soundEffect: InputFeedbacks.SoundEffect = InputFeedbacks.SoundEffect.Standard,
            textColor: Int? = null,
            textColorMonet: String? = null,
            altTextColor: Int? = null,
            altTextColorMonet: String? = null,
            backgroundColor: Int? = null,
            backgroundColorMonet: String? = null,
            shadowColor: Int? = null,
            shadowColorMonet: String? = null
        ) : Appearance(
            percentWidth,
            variant,
            border,
            margin,
            viewId,
            soundEffect,
            textColor,
            textColorMonet,
            altTextColor,
            altTextColorMonet,
            backgroundColor,
            backgroundColorMonet,
            shadowColor,
            shadowColorMonet
        )

        class ImageText(
            displayText: String,
            textSize: Float,
            /**
             * `Int` constants in [Typeface].
             * Can be `NORMAL`(default), `BOLD`, `ITALIC` or `BOLD_ITALIC`
             */
            textStyle: Int = Typeface.NORMAL,
            @DrawableRes
            val src: Int,
            percentWidth: Float = 0.1f,
            variant: Variant = Variant.Normal,
            border: Border = Border.Default,
            margin: Boolean = true,
            viewId: Int = -1,
            textColor: Int? = null,
            textColorMonet: String? = null,
            altTextColor: Int? = null,
            altTextColorMonet: String? = null,
            backgroundColor: Int? = null,
            backgroundColorMonet: String? = null,
            shadowColor: Int? = null,
            shadowColorMonet: String? = null
        ) : Text(
            displayText,
            textSize,
            textStyle,
            percentWidth,
            variant,
            border,
            margin,
            viewId,
            InputFeedbacks.SoundEffect.Standard,
            textColor,
            textColorMonet,
            altTextColor,
            altTextColorMonet,
            backgroundColor,
            backgroundColorMonet,
            shadowColor,
            shadowColorMonet
        )
    }

    sealed class Behavior {
        class Press(
            val action: KeyAction
        ) : Behavior()

        class LongPress(
            val action: KeyAction
        ) : Behavior()

        class Repeat(
            val action: KeyAction
        ) : Behavior()

        class Swipe(
            val action: KeyAction
        ) : Behavior()

        enum class SwipeDirection {
            Up, Down, Left, Right
        }

        class SwipeDir(
            val action: KeyAction,
            val direction: SwipeDirection
        ) : Behavior()

        class DoubleTap(
            val action: KeyAction
        ) : Behavior()
    }

    sealed class Popup {
        open class Preview(val content: String) : Popup()

        class AltPreview(content: String, val alternative: String) : Preview(content)

        /**
         * Custom direction popup preview for AlphabetKey.
         * Unlike AltPreview which is gated by the global popupOnKeyPress preference,
         * this always shows a preview when the key is pressed, using per-direction popup text.
         * Each direction has its own enabled flag controlled independently in the key editor.
         * @param content Default preview text (the character)
         * @param swipeUpPopup Text to show on swipe up, or null
         * @param swipeDownPopup Text to show on swipe down, or null
         * @param swipeLeftPopup Text to show on swipe left, or null
         * @param swipeRightPopup Text to show on swipe right, or null
         * @param longPressPopup Text to show on long press preview, or null
         * @param swipeUpEnabled Whether swipe up popup is enabled
         * @param swipeDownEnabled Whether swipe down popup is enabled
         * @param swipeLeftEnabled Whether swipe left popup is enabled
         * @param swipeRightEnabled Whether swipe right popup is enabled
         * @param longPressEnabled Whether long press popup preview is enabled
         */
        class CustomAltPreview(
            content: String,
            val swipeUpPopup: String?,
            val swipeDownPopup: String?,
            val swipeLeftPopup: String?,
            val swipeRightPopup: String?,
            val longPressPopup: String?,
            val swipeUpEnabled: Boolean = true,
            val swipeDownEnabled: Boolean = true,
            val swipeLeftEnabled: Boolean = true,
            val swipeRightEnabled: Boolean = true,
            val longPressEnabled: Boolean = true
        ) : Preview(content)

        class Keyboard(val label: String) : Popup()

        /**
         * Custom keyboard popup with explicit keys list.
         * Unlike [Keyboard] which looks up candidates from PopupPreset by label,
         * this provides the candidates directly.
         * @param extraKeys custom candidate texts to prepend before preset candidates
         * @param label base label to lookup remaining candidates from PopupPreset
         */
        class CustomKeyboard(
            val extraKeys: Array<String>,
            val label: String
        ) : Popup()

        class Menu(val items: Array<Item>) : Popup() {
            class Item(val label: String, @DrawableRes val icon: Int, val action: KeyAction)
        }

        /**
         * Represents a longPress macro action that appears in the popup keyboard
         * as the first candidate item.
         * @param displayLabel The text to display in the popup candidate
         * @param action The KeyAction to execute when selected
         * @param baseLabel The base label to lookup remaining candidates from PopupPreset
         */
        class LongPressKeyboard(
            val displayLabel: String,
            val action: KeyAction,
            val baseLabel: String
        ) : Popup()
    }
}
