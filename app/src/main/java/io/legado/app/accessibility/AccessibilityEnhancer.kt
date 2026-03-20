package io.legado.app.accessibility

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * 无障碍增强工具类
 * 为读屏用户提供更好的导航支持
 */
object AccessibilityEnhancer {
    
    /**
     * 检查无障碍服务是否启用
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
                as android.view.accessibility.AccessibilityManager
            am.isEnabled
        } catch (e: Exception) { 
            false 
        }
    }
    
    /**
     * 增强视图组的无障碍支持
     */
    fun enhanceViewGroup(viewGroup: ViewGroup, context: Context) {
        if (!isAccessibilityEnabled(context)) return
        
        // 设置importantForAccessibility
        viewGroup.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        
        // 遍历子视图
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child != null) {
                enhanceView(child, context)
            }
        }
    }
    
    /**
     * 增强单个视图的无障碍支持
     */
    fun enhanceView(view: View, context: Context) {
        if (!isAccessibilityEnabled(context)) return
        
        // 确保视图可被无障碍服务访问
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    /**
     * 增强阅读页面视图的无障碍支持
     */
    fun enhanceReadingPageView(
        view: View,
        context: Context,
        pageNumber: Int,
        totalPages: Int
    ) {
        if (!isAccessibilityEnabled(context)) return
        
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        view.contentDescription = "第${pageNumber}页，共${totalPages}页"
    }
    
    /**
     * 为ReadView设置无障碍支持
     */
    fun enhanceReadView(view: View, context: Context, showMenuLabel: String) {
        if (!isAccessibilityEnabled(context)) return
        
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        view.contentDescription = "阅读视图，$showMenuLabel"
    }
}
