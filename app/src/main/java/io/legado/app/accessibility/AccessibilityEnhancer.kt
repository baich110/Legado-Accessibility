package io.legado.app.accessibility

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

object AccessibilityEnhancer {
    
    fun enhanceViewGroup(viewGroup: ViewGroup, activity: Activity) {
        if (!isAccessibilityEnabled(activity)) return
        
        ViewCompat.setAccessibilityDelegate(viewGroup, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = viewGroup.javaClass.simpleName
            }
        })
        
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                enhanceViewGroup(child, activity)
            } else if (child != null) {
                enhanceView(child, activity)
            }
        }
    }
    
    fun enhanceView(view: View, activity: Activity) {
        if (!isAccessibilityEnabled(activity)) return
        if (ViewCompat.getAccessibilityDelegate(view) != null) return
        
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription = getViewRole(view)
                if (info.contentDescription.isNullOrEmpty()) {
                    val desc = generateContentDescription(view)
                    if (desc.isNotEmpty()) info.contentDescription = desc
                }
            }
        })
    }
    
    private fun isAccessibilityEnabled(activity: Activity): Boolean {
        return try {
            val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) 
                as android.view.accessibility.AccessibilityManager
            am.isEnabled
        } catch (e: Exception) { false }
    }
    
    private fun getViewRole(view: View): String = when (view) {
        is android.widget.Button -> "按钮"
        is android.widget.TextView -> "文本"
        is android.widget.ImageView -> "图片"
        is android.widget.EditText -> "输入框"
        is android.widget.CheckBox -> "复选框"
        else -> ""
    }
    
    private fun generateContentDescription(view: View): String = when (view) {
        is android.widget.ImageView -> if (!view.contentDescription.isNullOrEmpty()) 
            view.contentDescription.toString() else "[图片]"
        is android.widget.TextView -> view.text?.toString()?.trim()?.take(50) ?: ""
        else -> ""
    }
}
