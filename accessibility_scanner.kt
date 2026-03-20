package io.legado.app.accessibility

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.legado.app.utils.printLog

/**
 * 智能无障碍检查与修复脚本
 * 
 * 功能：
 * 1. 扫描视图树，识别未适配的无障碍控件
 * 2. 生成详细的检查报告
 * 3. 提供一键修复建议
 * 4. 排除已适配的控件
 */
class AccessibilityScanner(private val context: Context) {
    
    data class AccessibilityIssue(
        val view: View,
        val viewId: String,
        val className: String,
        val issues: List<String>,
        val suggestedFix: String
    )
    
    private val excludedViewIds = mutableSetOf<String>()
    private val issues = mutableListOf<AccessibilityIssue>()
    
    /**
     * 扫描指定视图树，找出无障碍问题
     */
    fun scanViewTree(rootView: View): List<AccessibilityIssue> {
        issues.clear()
        traverseViewTree(rootView)
        return issues.toList()
    }
    
    /**
     * 递归遍历视图树
     */
    private fun traverseViewTree(view: View, depth: Int = 0) {
        // 跳过已排除的视图
        val viewId = getViewId(view)
        if (viewId in excludedViewIds) {
            return
        }
        
        // 检查当前视图的无障碍问题
        checkAccessibilityIssues(view)
        
        // 递归遍历子视图
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverseViewTree(view.getChildAt(i), depth + 1)
            }
        }
    }
    
    /**
     * 检查单个视图的无障碍问题
     */
    private fun checkAccessibilityIssues(view: View) {
        val viewId = getViewId(view)
        val className = view.javaClass.simpleName
        val detectedIssues = mutableListOf<String>()
        val suggestedFix = StringBuilder()
        
        // 检查是否已应用无障碍增强
        val isEnhanced = ViewCompat.getAccessibilityDelegate(view) != null
        
        // 如果已增强，添加到排除列表
        if (isEnhanced) {
            excludedViewIds.add(viewId)
            return
        }
        
        // 1. 检查内容描述
        if (view.contentDescription == null) {
            when (view) {
                is Button -> {
                    detectedIssues.add("缺少内容描述（按钮）")
                    suggestedFix.append("setContentDescription(\"${getButtonText(view)}\")")
                }
                is ImageButton -> {
                    detectedIssues.add("缺少内容描述（图片按钮）")
                    suggestedFix.append("setContentDescription(\"图标按钮\")")
                }
                is ImageView -> {
                    if (view.drawable != null) {
                        detectedIssues.add("缺少内容描述（图片）")
                        suggestedFix.append("setContentDescription(\"图片\")")
                    }
                }
                is EditText -> {
                    if (view.hint != null) {
                        detectedIssues.add("缺少内容描述（输入框）")
                        suggestedFix.append("setContentDescription(\"${view.hint}\")")
                    }
                }
                is CheckBox, is RadioButton, is Switch -> {
                    detectedIssues.add("缺少内容描述（选择控件）")
                    suggestedFix.append("setContentDescription(\"${getCompoundButtonText(view as CompoundButton)}\")")
                }
                else -> {
                    if (view.isClickable) {
                        detectedIssues.add("缺少内容描述（可点击控件）")
                        suggestedFix.append("setContentDescription(\"${className}控件\")")
                    }
                }
            }
        }
        
        // 2. 检查焦点和点击性
        if (view.isClickable && !view.isFocusable) {
            detectedIssues.add("可点击但不可聚焦")
            suggestedFix.append("\nsetFocusable(true)")
        }
        
        // 3. 检查重要标志
        if (view.isImportantForAccessibility == View.IMPORTANT_FOR_ACCESSIBILITY_NO) {
            detectedIssues.add("无障碍重要性设置为NO")
            suggestedFix.append("\nsetImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)")
        }
        
        // 4. 特殊控件检查
        when (view) {
            is SeekBar -> {
                detectedIssues.add("进度条缺少无障碍描述")
                suggestedFix.append("""
                    |ViewCompat.replaceAccessibilityAction(view,
                    |    AccessibilityNodeInfoCompat.ACTION_SET_PROGRESS,
                    |    "调整进度",
                    |    null)
                """.trimMargin())
            }
            is ListView, is RecyclerView -> {
                detectedIssues.add("列表缺少无障碍支持")
                suggestedFix.append("""
                    |// 需要为列表项添加无障碍支持
                    |view.isAccessibilityHeading = true
                    |// 为每个列表项设置内容描述
                """.trimMargin())
            }
        }
        
        // 如果有问题，添加到报告
        if (detectedIssues.isNotEmpty()) {
            issues.add(
                AccessibilityIssue(
                    view = view,
                    viewId = viewId,
                    className = className,
                    issues = detectedIssues,
                    suggestedFix = suggestedFix.toString()
                )
            )
        }
    }
    
    /**
     * 生成检查报告
     */
    fun generateReport(rootView: View): String {
        scanViewTree(rootView)
        
        val report = StringBuilder()
        report.append("""
            |===========================================
            |       无障碍检查报告
            |===========================================
            |扫描时间: ${java.util.Date()}
            |根视图: ${rootView.javaClass.name}
            |已排除已适配控件: ${excludedViewIds.size} 个
            |发现的问题: ${issues.size} 个
            |
            |""".trimMargin())
        
        if (issues.isEmpty()) {
            report.append("✅ 未发现无障碍问题！所有控件都已适配。\n")
        } else {
            report.append("⚠️ 发现以下无障碍问题：\n\n")
            
            issues.forEachIndexed { index, issue ->
                report.append("""
                    |[问题 ${index + 1}]
                    |控件ID: ${issue.viewId}
                    |控件类型: ${issue.className}
                    |问题列表:
                    |${issue.issues.joinToString("\n") { "  • $it" }}
                    |建议修复:
                    |${issue.suggestedFix}
                    |
                    |""".trimMargin())
            }
            
            // 添加一键修复代码
            report.append("""
                |===========================================
                |       一键修复代码（Kotlin）
                |===========================================
                |
                |fun fixAccessibilityIssues(rootView: View) {
                |    val scanner = AccessibilityScanner(context)
                |    val issues = scanner.scanViewTree(rootView)
                |    
                |    issues.forEach { issue ->
                |        when (issue.className) {
                |            "Button" -> {
                |                issue.view.contentDescription = "按钮"
                |                issue.view.isFocusable = true
                |            }
                |            "ImageView" -> {
                |                issue.view.contentDescription = "图片"
                |                issue.view.isFocusable = issue.view.isClickable
                |            }
                |            "EditText" -> {
                |                val editText = issue.view as EditText
                |                editText.contentDescription = editText.hint ?: "输入框"
                |            }
                |            // 添加更多控件类型的处理...
                |        }
                |        
                |        // 应用通用无障碍增强
                |        AccessibilityEnhancer.enhanceViewAccessibility(issue.view)
                |    }
                |}
                |
                |""".trimMargin())
        }
        
        return report.toString()
    }
    
    /**
     * 一键修复所有发现的问题
     */
    fun fixAllIssues(rootView: View): Int {
        scanViewTree(rootView)
        var fixedCount = 0
        
        issues.forEach { issue ->
            try {
                // 根据问题类型应用修复
                when {
                    issue.issues.any { it.contains("缺少内容描述") } -> {
                        // 智能生成内容描述
                        val description = generateContentDescription(issue.view)
                        issue.view.contentDescription = description
                    }
                    issue.issues.any { it.contains("不可聚焦") } -> {
                        issue.view.isFocusable = true
                    }
                    issue.issues.any { it.contains("重要性设置为NO") } -> {
                        issue.view.isImportantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    }
                }
                
                // 应用通用无障碍增强
                AccessibilityEnhancer.enhanceViewAccessibility(issue.view)
                fixedCount++
                
            } catch (e: Exception) {
                printLog("修复控件 ${issue.viewId} 时出错: ${e.message}")
            }
        }
        
        return fixedCount
    }
    
    /**
     * 智能生成内容描述
     */
    private fun generateContentDescription(view: View): String {
        return when (view) {
            is Button -> view.text?.toString() ?: "按钮"
            is ImageButton -> "图标按钮"
            is ImageView -> "图片"
            is EditText -> view.hint?.toString() ?: "输入框"
            is CheckBox -> (view as CheckBox).text?.toString() ?: "复选框"
            is RadioButton -> (view as RadioButton).text?.toString() ?: "单选按钮"
            is Switch -> (view as Switch).text?.toString() ?: "开关"
            else -> "${view.javaClass.simpleName}控件"
        }
    }
    
    private fun getViewId(view: View): String {
        return if (view.id != View.NO_ID) {
            try {
                context.resources.getResourceEntryName(view.id)
            } catch (e: Exception) {
                "id_${view.id}"
            }
        } else {
            "no_id_${System.identityHashCode(view)}"
        }
    }
    
    private fun getButtonText(button: Button): String {
        return button.text?.toString() ?: "按钮"
    }
    
    private fun getCompoundButtonText(button: CompoundButton): String {
        return button.text?.toString() ?: "选择控件"
    }
    
    companion object {
        /**
         * 快速扫描并生成报告
         */
        fun quickScan(context: Context, rootView: View): String {
            val scanner = AccessibilityScanner(context)
            return scanner.generateReport(rootView)
        }
        
        /**
         * 快速修复所有问题
         */
        fun quickFix(context: Context, rootView: View): Pair<Int, String> {
            val scanner = AccessibilityScanner(context)
            val fixedCount = scanner.fixAllIssues(rootView)
            val report = scanner.generateReport(rootView)
            return Pair(fixedCount, report)
        }
    }
}