package io.legado.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍服务 - 为视障用户提供屏幕阅读和导航支持
 */
class AccessibilityService : AccessibilityService() {

    companion object {
        var instance: AccessibilityService? = null
            private set
        
        var isServiceRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true
        
        // 配置无障碍服务信息
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_DEFAULT or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            notificationTimeout = 100
            canRetrieveWindowContent = true
            canPerformGestures = true
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 处理无障碍事件
        event?.let {
            // 可以在这里添加自定义事件处理逻辑
            // 例如：朗读文本内容、 announced for accessibility
        }
    }

    override fun onInterrupt() {
        // 当需要中断当前朗读时调用
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceRunning = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        isServiceRunning = false
        return super.onUnbind(intent)
    }

    /**
     * 模拟点击手势
     */
    fun performClick(x: Float, y: Float): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(
                android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
                ),
                null, null
            )
        } else {
            false
        }
    }

    /**
     * 模拟滚动
     */
    fun performScroll(direction: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val action = when (direction) {
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> 
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> 
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else -> return false
            }
            dispatchGesture(
                android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(action),
                null, null
            )
        } else {
            false
        }
    }
}
