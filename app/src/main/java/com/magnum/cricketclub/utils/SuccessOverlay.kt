package com.magnum.cricketclub.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.magnum.cricketclub.R

object SuccessOverlay {
    
    /**
     * Shows a success overlay with animation similar to Google Pay
     * @param parentView The parent view to attach the overlay to (usually root view)
     * @param message The success message to display
     * @param duration How long to show the overlay (default 2000ms)
     * @param onDismiss Callback when overlay is dismissed
     */
    fun show(
        parentView: ViewGroup,
        message: String = "Saved Successfully!",
        duration: Long = 2000,
        onDismiss: (() -> Unit)? = null
    ) {
        android.util.Log.d("SuccessOverlay", "show() called with message: $message")
        try {
            // Inflate the overlay layout
            val overlayView = View.inflate(parentView.context, R.layout.layout_success_overlay, null)
            val container = overlayView.findViewById<FrameLayout>(R.id.successOverlayContainer)
            val cardView = overlayView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.successCardView)
            val iconBackground = overlayView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.successIconBackground)
            val checkmark = overlayView.findViewById<ImageView>(R.id.successCheckmark)
            val messageText = overlayView.findViewById<TextView>(R.id.successMessage)
            
            android.util.Log.d("SuccessOverlay", "Views found - container: ${container != null}, iconBg: ${iconBackground != null}, checkmark: ${checkmark != null}, message: ${messageText != null}")
            
            if (container == null) {
                android.util.Log.e("SuccessOverlay", "Container is null")
                return
            }
            if (cardView == null) {
                android.util.Log.e("SuccessOverlay", "cardView is null")
                return
            }
            if (iconBackground == null) {
                android.util.Log.e("SuccessOverlay", "iconBackground is null")
                return
            }
            if (checkmark == null) {
                android.util.Log.e("SuccessOverlay", "checkmark is null")
                return
            }
            if (messageText == null) {
                android.util.Log.e("SuccessOverlay", "messageText is null")
                return
            }
            
            android.util.Log.d("SuccessOverlay", "All views found successfully")
            
            // Set message text
            messageText.text = message
            
            // Set layout params to match parent
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            overlayView.layoutParams = layoutParams
            
            // Add overlay to parent
            parentView.addView(overlayView)
            container.visibility = View.VISIBLE
            
            // Prevent clicks from passing through
            container.setOnClickListener { /* Consume clicks */ }
            
            android.util.Log.d("SuccessOverlay", "Overlay added to parent view successfully")
            
            // Animation sequence
            val animatorSet = AnimatorSet()
            
            // 1. Fade in background
            val backgroundFadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f)
            backgroundFadeIn.duration = 200
            
            // 2. Scale up card with bounce
            val cardScaleX = ObjectAnimator.ofFloat(cardView, "scaleX", 0f, 1.1f, 1f)
            val cardScaleY = ObjectAnimator.ofFloat(cardView, "scaleY", 0f, 1.1f, 1f)
            cardScaleX.duration = 400
            cardScaleY.duration = 400
            cardScaleX.interpolator = OvershootInterpolator(2f)
            cardScaleY.interpolator = OvershootInterpolator(2f)
            
            // 3. Scale up icon background (circular expansion)
            // Icon background should start visible (it's green), just scale it
            val iconBgScaleX = ObjectAnimator.ofFloat(iconBackground, "scaleX", 0f, 1.1f, 1f)
            val iconBgScaleY = ObjectAnimator.ofFloat(iconBackground, "scaleY", 0f, 1.1f, 1f)
            iconBgScaleX.duration = 350
            iconBgScaleY.duration = 350
            iconBgScaleX.startDelay = 200
            iconBgScaleY.startDelay = 200
            iconBgScaleX.interpolator = OvershootInterpolator(1.2f)
            iconBgScaleY.interpolator = OvershootInterpolator(1.2f)
            
            // 4. Draw checkmark path animation (bounce in)
            // Start checkmark visible but scaled down
            checkmark.alpha = 1f
            checkmark.scaleX = 0f
            checkmark.scaleY = 0f
            val checkmarkScaleX = ObjectAnimator.ofFloat(checkmark, "scaleX", 0f, 1.3f, 1f)
            val checkmarkScaleY = ObjectAnimator.ofFloat(checkmark, "scaleY", 0f, 1.3f, 1f)
            checkmarkScaleX.duration = 500
            checkmarkScaleY.duration = 500
            checkmarkScaleX.startDelay = 450
            checkmarkScaleY.startDelay = 450
            checkmarkScaleX.interpolator = OvershootInterpolator(2f)
            checkmarkScaleY.interpolator = OvershootInterpolator(2f)
            
            // 5. Fade in message text
            // Start message visible but transparent
            messageText.alpha = 0f
            val messageFadeIn = ObjectAnimator.ofFloat(messageText, "alpha", 0f, 1f)
            messageFadeIn.duration = 300
            messageFadeIn.startDelay = 600
            
            // Play animations in sequence
            animatorSet.playTogether(
                backgroundFadeIn,
                cardScaleX,
                cardScaleY
            )
            animatorSet.play(iconBgScaleX).with(iconBgScaleY).after(cardScaleX)
            animatorSet.play(checkmarkScaleX).with(checkmarkScaleY).after(iconBgScaleX)
            animatorSet.play(messageFadeIn).after(checkmarkScaleX)
            
            android.util.Log.d("SuccessOverlay", "Starting animations")
            
            // Add listener to verify animations are running
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    android.util.Log.d("SuccessOverlay", "Animations started")
                }
                override fun onAnimationEnd(animation: Animator) {
                    android.util.Log.d("SuccessOverlay", "Animations ended")
                    // Ensure checkmark and message are visible after animation
                    checkmark.alpha = 1f
                    messageText.alpha = 1f
                }
            })
            
            animatorSet.start()
            
            // Fallback: Ensure checkmark and message are visible after animation delay
            checkmark.postDelayed({
                if (checkmark.alpha < 1f) {
                    android.util.Log.d("SuccessOverlay", "Fallback: Setting checkmark alpha to 1")
                    checkmark.alpha = 1f
                }
            }, 1000)
            
            messageText.postDelayed({
                if (messageText.alpha < 1f) {
                    android.util.Log.d("SuccessOverlay", "Fallback: Setting message alpha to 1")
                    messageText.alpha = 1f
                }
            }, 1000)
            
            // Auto dismiss after duration
            container.postDelayed({
                android.util.Log.d("SuccessOverlay", "Auto-dismissing overlay")
                dismiss(overlayView, parentView, onDismiss)
            }, duration)
        } catch (e: Exception) {
            android.util.Log.e("SuccessOverlay", "Error showing overlay: ${e.message}", e)
        }
    }
    
    private fun dismiss(
        overlayView: View,
        parentView: ViewGroup,
        onDismiss: (() -> Unit)?
    ) {
        val container = overlayView.findViewById<FrameLayout>(R.id.successOverlayContainer)
        val cardView = (container.getChildAt(0) as? ViewGroup)?.getChildAt(0) as? View
        
        // Fade out animation
        val fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f)
        val scaleDown = ObjectAnimator.ofFloat(cardView ?: container, "scaleX", 1f, 0.8f)
        val scaleDownY = ObjectAnimator.ofFloat(cardView ?: container, "scaleY", 1f, 0.8f)
        
        fadeOut.duration = 200
        scaleDown.duration = 200
        scaleDownY.duration = 200
        
        val dismissAnimator = AnimatorSet()
        dismissAnimator.playTogether(fadeOut, scaleDown, scaleDownY)
        dismissAnimator.interpolator = DecelerateInterpolator()
        
        dismissAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                parentView.removeView(overlayView)
                onDismiss?.invoke()
            }
        })
        
        dismissAnimator.start()
    }
}
