/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.graphics.drawable.ColorDrawable
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsResponse
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcaster
import org.fcitx.fcitx5.android.input.broadcast.PreeditEmptyStateComponent
import org.fcitx.fcitx5.android.input.broadcast.PunctuationComponent
import org.fcitx.fcitx5.android.input.broadcast.ReturnKeyDrawableComponent
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.picker.emojiPicker
import org.fcitx.fcitx5.android.input.picker.emoticonPicker
import org.fcitx.fcitx5.android.input.picker.symbolPicker
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.preedit.PreeditComponent
import android.graphics.Rect
import android.graphics.Region
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.unset
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.wrapToUniqueComponent
import org.mechdancer.dependency.plusAssign
import splitties.dimensions.dp
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerVertically
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.endToStartOf
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.startToEndOf
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.withTheme
import splitties.views.dsl.core.wrapContent
import splitties.views.imageDrawable

@SuppressLint("ViewConstructor")
class InputView(
    service: FcitxInputMethodService,
    fcitx: FcitxConnection,
    theme: Theme
) : BaseInputView(service, fcitx, theme) {

    private val keyBorder by ThemeManager.prefs.keyBorder

    private val customBackground = imageView {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val placeholderOnClickListener = OnClickListener { }

    // use clickable view as padding, so MotionEvent can be split to padding view and keyboard view
    private val leftPaddingSpace = view(::View) {
        setOnClickListener(placeholderOnClickListener)
    }
    private val rightPaddingSpace = view(::View) {
        setOnClickListener(placeholderOnClickListener)
    }
    private val bottomPaddingSpace = view(::View) {
        // height as keyboardBottomPadding
        // bottomMargin as WindowInsets (Navigation Bar) offset
        setOnClickListener(placeholderOnClickListener)
    }

    private val scope = DynamicScope()
    private val themedContext = context.withTheme(R.style.Theme_InputViewTheme)
    private val broadcaster = InputBroadcaster()
    private val popup = PopupComponent()
    private val punctuation = PunctuationComponent()
    private val returnKeyDrawable = ReturnKeyDrawableComponent()
    private val preeditEmptyState = PreeditEmptyStateComponent()
    private val preedit = PreeditComponent()
    private val commonKeyActionListener = CommonKeyActionListener()
    private val windowManager = InputWindowManager()
    private val kawaiiBar = KawaiiBarComponent()
    private val horizontalCandidate = HorizontalCandidateComponent()
    private val keyboardWindow = KeyboardWindow()
    private val symbolPicker = symbolPicker()
    private val emojiPicker = emojiPicker()
    private val emoticonPicker = emoticonPicker()

    private fun setupScope() {
        scope += this@InputView.wrapToUniqueComponent()
        scope += service.wrapToUniqueComponent()
        scope += fcitx.wrapToUniqueComponent()
        scope += theme.wrapToUniqueComponent()
        scope += themedContext.wrapToUniqueComponent()
        scope += broadcaster
        scope += popup
        scope += punctuation
        scope += returnKeyDrawable
        scope += preeditEmptyState
        scope += preedit
        scope += commonKeyActionListener
        scope += windowManager
        scope += kawaiiBar
        scope += horizontalCandidate
        broadcaster.onScopeSetupFinished(scope)
    }

    private val keyboardPrefs = AppPrefs.getInstance().keyboard

    private val focusChangeResetKeyboard by keyboardPrefs.focusChangeResetKeyboard

    private val keyboardHeightPercent = keyboardPrefs.keyboardHeightPercent
    private val keyboardHeightPercentLandscape = keyboardPrefs.keyboardHeightPercentLandscape
    private val keyboardSidePadding = keyboardPrefs.keyboardSidePadding
    private val keyboardSidePaddingLandscape = keyboardPrefs.keyboardSidePaddingLandscape
    private val keyboardBottomPadding = keyboardPrefs.keyboardBottomPadding
    private val keyboardBottomPaddingLandscape = keyboardPrefs.keyboardBottomPaddingLandscape

    private val keyboardSizePrefs = listOf(
        keyboardHeightPercent,
        keyboardHeightPercentLandscape,
        keyboardSidePadding,
        keyboardSidePaddingLandscape,
        keyboardBottomPadding,
        keyboardBottomPaddingLandscape,
    )

    var isFloating = false
        private set
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    var isDraggingOrResizing = false
        private set

    private fun startDragOrResize() {
        isDraggingOrResizing = true
        // Trigger insets update
        service.window.window?.decorView?.requestLayout()
    }
    
    private fun stopDragOrResize() {
        isDraggingOrResizing = false
        // Trigger insets update
        service.window.window?.decorView?.requestLayout()
    }

    private fun toggleFloatingMode() {
        popup.dismissAll()
        isFloating = !isFloating
        updateFloatingState()
        updateKeyboardSize() // Add this to refresh padding/height based on new state
        // service.setFullscreenMode(isFloating) // Removed
        service.updateFullscreenMode()
        // Force layout update
        requestLayout()
        // Trigger insets update
        service.window.window?.decorView?.requestLayout()
    }
    
    fun getFloatingKeyboardRegion(outRegion: Region) {
        if (!isFloating) return
        val rect = Rect()
        
        keyboardView.getHitRect(rect)
        
        if (preedit.ui.root.visibility == View.VISIBLE) {
             val preeditRect = Rect()
             preedit.ui.root.getHitRect(preeditRect)
             rect.union(preeditRect)
        }
        
        // Slightly expand the region to avoid rounding errors or touch slop issues at edges
        // especially at the bottom
        rect.inset(0, 0, 0, -dp(5))
        
        outRegion.set(rect)
    }

    private fun updateFloatingState() {
        val params = keyboardView.layoutParams as ConstraintLayout.LayoutParams
        if (isFloating) {
            // Floating mode
            params.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            params.bottomToBottom = params.unset
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = params.unset
            
            // Set InputView height to match parent (screen) to allow dragging anywhere
            layoutParams?.height = matchParent
            
            // In floating mode, we rely on translation.
            if (keyboardView.translationX == 0f && keyboardView.translationY == 0f) {
                keyboardView.translationX = (resources.displayMetrics.widthPixels * 0.1).toFloat()
                // Start a bit lower than center to avoid covering input field immediately if possible
                keyboardView.translationY = (resources.displayMetrics.heightPixels * 0.6).toFloat()
            }
            
            // Update preedit constraints for floating mode
            // It should be attached to top of keyboardView
            preedit.ui.root.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = 0 // match constraints
                // Remove centerHorizontally (startToStart=parent, endToEnd=parent) if present from initial setup
                startToStart = keyboardView.id
                endToEnd = keyboardView.id
                bottomToTop = keyboardView.id
                // Remove other vertical constraints
                topToBottom = unset
                topToTop = unset
                bottomToBottom = unset
            }
            preedit.ui.root.translationX = keyboardView.translationX
            preedit.ui.root.translationY = keyboardView.translationY
            
            // Apply text scale
            (windowManager.getEssentialWindow(KeyboardWindow) as? KeyboardWindow)?.setTextScale(0.8f)
            
        } else {
            // Docked mode
            params.width = matchParent
            params.matchConstraintMaxWidth = params.unset // Reset constraint limits
            params.matchConstraintMinWidth = params.unset
            
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = params.unset
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            
            // Reset InputView height to match parent to ensure bottom alignment
            layoutParams?.height = matchParent
            
            keyboardView.translationX = 0f
            keyboardView.translationY = 0f
            
            // Reset preedit constraints for docked mode
            preedit.ui.root.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = matchParent
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = keyboardView.id
                // Ensure translation is reset
            }
            preedit.ui.root.translationX = 0f
            preedit.ui.root.translationY = 0f
            
            // Reset text scale
            (windowManager.getEssentialWindow(KeyboardWindow) as? KeyboardWindow)?.setTextScale(1.0f)
        }
        keyboardView.layoutParams = params
        
        // Request layout to apply changes to self and children
        requestLayout()
    }

    private val keyboardHeightPx: Int
        get() {
            val percent = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardHeightPercentLandscape
                else -> keyboardHeightPercent
            }.getValue()
            val baseHeight = resources.displayMetrics.heightPixels * percent / 100
            if (isFloating) {
                return (baseHeight * 0.8).toInt()
            }
            return baseHeight
        }

    private val keyboardSidePaddingPx: Int
        get() {
            val value = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardSidePaddingLandscape
                else -> keyboardSidePadding
            }.getValue()
            val px = dp(value)
            if (isFloating) {
                return (px * 0.8).toInt()
            }
            return px
        }

    private val keyboardBottomPaddingPx: Int
        get() {
            val value = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> keyboardBottomPaddingLandscape
                else -> keyboardBottomPadding
            }.getValue()
            val px = dp(value)
            if (isFloating) {
                return (px * 0.8).toInt()
            }
            return px
        }

    @Keep
    private val onKeyboardSizeChangeListener = ManagedPreferenceProvider.OnChangeListener { key ->
        if (keyboardSizePrefs.any { it.key == key }) {
            updateKeyboardSize()
        }
    }

    val keyboardView: View

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isFloating) {
            // In floating mode, we only want to consume events if they hit the keyboardView
            // Otherwise return false to let them pass through (if the window allows it)
            // But since this is the root view of the IMS, returning false here might not pass it to the app
            // unless the window flags are set correctly.
            // For now, let's just ensure we don't consume events blindly.
            return super.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    private val resizeHandleRight = null
    private val resizeHandleBottom = null

    init {
        // MUST call before any operation
        setupScope()

        // restore punctuation mapping in case of InputView recreation
        fcitx.launchOnReady {
            punctuation.updatePunctuationMapping(it.statusAreaActionsCached)
        }

        // make sure KeyboardWindow's view has been created before it receives any broadcast
        windowManager.addEssentialWindow(keyboardWindow, createView = true)
        windowManager.addEssentialWindow(symbolPicker)
        windowManager.addEssentialWindow(emojiPicker)
        windowManager.addEssentialWindow(emoticonPicker)
        // show KeyboardWindow by default
        windowManager.attachWindow(KeyboardWindow)

        broadcaster.onImeUpdate(fcitx.runImmediately { inputMethodEntryCached })

        customBackground.imageDrawable = theme.backgroundDrawable(keyBorder)

        keyboardView = constraintLayout {
            id = View.generateViewId()
            // allow MotionEvent to be delivered to keyboard while pressing on padding views.
            // although it should be default for apps targeting Honeycomb (3.0, API 11) and higher,
            // but it's not the case on some devices ... just set it here
            isMotionEventSplittingEnabled = true
            add(customBackground, lParams {
                centerVertically()
                centerHorizontally()
            })
            add(kawaiiBar.view, lParams(matchParent, dp(KawaiiBarComponent.HEIGHT)) {
                topOfParent()
                centerHorizontally()
            })
            add(leftPaddingSpace, lParams {
                below(kawaiiBar.view)
                startOfParent()
                bottomOfParent()
            })
            add(rightPaddingSpace, lParams {
                below(kawaiiBar.view)
                endOfParent()
                bottomOfParent()
            })
            add(windowManager.view, lParams {
                below(kawaiiBar.view)
                above(bottomPaddingSpace)
                /**
                 * set start and end constrain in [updateKeyboardSize]
                 */
            })
            add(bottomPaddingSpace, lParams {
                startToEndOf(leftPaddingSpace)
                endToStartOf(rightPaddingSpace)
                bottomOfParent()
            })
        }

        updateKeyboardSize()

        add(preedit.ui.root, lParams(matchParent, wrapContent) {
            bottomToTop = keyboardView.id
            centerHorizontally()
        })
        add(keyboardView, lParams(matchParent, wrapContent) {
            centerHorizontally()
            bottomOfParent()
        })
        
        add(popup.root, lParams(matchParent, matchParent) {
            centerVertically()
            centerHorizontally()
        })
        
        keyboardPrefs.registerOnChangeListener(onKeyboardSizeChangeListener)

        kawaiiBar.onFloatingToggleListener = {
            toggleFloatingMode()
        }

        kawaiiBar.view.setOnTouchListener { v, event ->
            if (!isFloating) return@setOnTouchListener false
            
            // Allow child views (buttons) to consume touch if they want.
            // But if we return false for DOWN, we won't get subsequent events.
            // If we return true for DOWN, we consume it.
            // We want dragging to work if user drags on empty space.
            // But kawaiiBar is full of buttons usually?
            // Actually, kawaiiBar has buttons. If user drags on a button, button consumes click.
            // If user drags, button might consume DOWN, then MOVE?
            // Buttons usually consume DOWN and UP.
            
            // However, the previous implementation worked:
            /*
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    v.performClick()
                    true
                }
            */
            // It returned true for DOWN. This means buttons might NOT work if they are behind listener?
            // No, listener is on container. Buttons are children.
            // Dispatch order: specific child -> container listener -> container onTouchEvent.
            // If listener is on container (v), and returns true, does it steal from children?
            // No, listener on v is called "before the view's onTouchEvent".
            // It doesn't affect child dispatch.
            // BUT if child consumes event, listener on parent v won't get it?
            // No, parent v's onInterceptTouchEvent is called. Then child dispatch.
            // Listener is on v.
            
            // Wait, v is kawaiiBar.view (the container).
            // If I touch a button inside v:
            // 1. v.dispatchTouchEvent() called.
            // 2. v.onInterceptTouchEvent() called (default false).
            // 3. child.dispatchTouchEvent() called.
            // 4. child.onTouchEvent() called. If true, handled.
            // 5. If child returns false, v.onTouchEvent() called.
            // 6. v.mOnTouchListener.onTouch() called before v.onTouchEvent().
            
            // So if child handles it, listener on v is NOT called?
            // Correct.
            
            // So if I drag on a button, dragging won't happen.
            // But if I drag on empty space, dragging happens.
            
            // The user says "dragging stopped working".
            // Maybe previously I could drag on buttons?
            // If so, I need to intercept.
            
            // But wait, previous code was just setOnTouchListener on v.
            // So it only worked on empty space.
            // This is fine.
            
            // Why did it stop working?
            // Maybe something else is consuming events?
            
            // I will add requestDisallowInterceptTouchEvent just in case.
            
            v.parent?.requestDisallowInterceptTouchEvent(true)
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    // v.performClick() // Why was this here? Maybe for accessibility?
                    // It triggers onClick listener on v.
                    // v doesn't seem to have onClick listener set.
                    // I will remove it or keep it?
                    // Previous code had it. I will keep it but maybe it's harmless.
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastTouchX
                    val dy = event.rawY - lastTouchY
                    keyboardView.translationX += dx
                    keyboardView.translationY += dy
                    keyboardWindow.updateBounds()
                    // Sync preedit position
                    preedit.ui.root.translationX = keyboardView.translationX
                    preedit.ui.root.translationY = keyboardView.translationY
                    
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                     v.parent?.requestDisallowInterceptTouchEvent(false)
                     keyboardWindow.updateBounds() // Ensure bounds are correct after drag
                     true
                }
                else -> false
            }
        }
    }

    private fun updateKeyboardSize() {
        windowManager.view.updateLayoutParams {
            height = keyboardHeightPx
        }
        bottomPaddingSpace.updateLayoutParams {
            height = keyboardBottomPaddingPx
        }
        val sidePadding = keyboardSidePaddingPx
        if (sidePadding == 0) {
            // hide side padding space views when unnecessary
            leftPaddingSpace.visibility = GONE
            rightPaddingSpace.visibility = GONE
            windowManager.view.updateLayoutParams<LayoutParams> {
                startToEnd = unset
                endToStart = unset
                startOfParent()
                endOfParent()
            }
        } else {
            leftPaddingSpace.visibility = VISIBLE
            rightPaddingSpace.visibility = VISIBLE
            leftPaddingSpace.updateLayoutParams {
                width = sidePadding
            }
            rightPaddingSpace.updateLayoutParams {
                width = sidePadding
            }
            windowManager.view.updateLayoutParams<LayoutParams> {
                startToStart = unset
                endToEnd = unset
                startToEndOf(leftPaddingSpace)
                endToStartOf(rightPaddingSpace)
            }
        }
        preedit.ui.root.setPadding(sidePadding, 0, sidePadding, 0)
        kawaiiBar.view.setPadding(sidePadding, 0, sidePadding, 0)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        bottomPaddingSpace.updateLayoutParams<LayoutParams> {
            bottomMargin = getNavBarBottomInset(insets)
        }
        return insets
    }

    /**
     * called when [InputView] is about to show, or restart
     */
    fun startInput(info: EditorInfo, capFlags: CapabilityFlags, restarting: Boolean = false) {
        broadcaster.onStartInput(info, capFlags)
        returnKeyDrawable.updateDrawableOnEditorInfo(info)
        if (focusChangeResetKeyboard || !restarting) {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

    override fun onStartHandleFcitxEvent() {
        val inputPanelData = fcitx.runImmediately { inputPanelCached }
        val inputMethodEntry = fcitx.runImmediately { inputMethodEntryCached }
        val statusAreaActions = fcitx.runImmediately { statusAreaActionsCached }
        arrayOf(
            FcitxEvent.InputPanelEvent(inputPanelData),
            FcitxEvent.IMChangeEvent(inputMethodEntry),
            FcitxEvent.StatusAreaEvent(
                FcitxEvent.StatusAreaEvent.Data(statusAreaActions, inputMethodEntry)
            )
        ).forEach { handleFcitxEvent(it) }
    }

    override fun handleFcitxEvent(it: FcitxEvent<*>) {
        when (it) {
            is FcitxEvent.CandidateListEvent -> {
                broadcaster.onCandidateUpdate(it.data)
            }
            is FcitxEvent.ClientPreeditEvent -> {
                preeditEmptyState.updatePreeditEmptyState(clientPreedit = it.data)
                broadcaster.onClientPreeditUpdate(it.data)
            }
            is FcitxEvent.InputPanelEvent -> {
                preeditEmptyState.updatePreeditEmptyState(preedit = it.data.preedit)
                broadcaster.onInputPanelUpdate(it.data)
            }
            is FcitxEvent.IMChangeEvent -> {
                broadcaster.onImeUpdate(it.data)
            }
            is FcitxEvent.StatusAreaEvent -> {
                punctuation.updatePunctuationMapping(it.data.actions)
                broadcaster.onStatusAreaUpdate(it.data.actions)
            }
            else -> {}
        }
    }

    fun updateSelection(start: Int, end: Int) {
        broadcaster.onSelectionUpdate(start, end)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun handleInlineSuggestions(response: InlineSuggestionsResponse): Boolean {
        return kawaiiBar.handleInlineSuggestions(response)
    }

    override fun onDetachedFromWindow() {
        keyboardPrefs.unregisterOnChangeListener(onKeyboardSizeChangeListener)
        // clear DynamicScope, implies that InputView should not be attached again after detached.
        scope.clear()
        super.onDetachedFromWindow()
    }

}
