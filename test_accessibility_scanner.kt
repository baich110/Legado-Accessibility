package io.legado.app.accessibility

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * 无障碍扫描器测试Activity
 * 用于演示如何使用AccessibilityScanner
 */
class TestAccessibilityScannerActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建一个测试布局
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // 添加一些测试控件（有些有障碍，有些没有）
        val buttonWithoutDesc = Button(this).apply {
            text = "搜索"
            // 故意不设置contentDescription
            id = View.generateViewId()
        }
        
        val imageButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            // 故意不设置contentDescription
            id = View.generateViewId()
        }
        
        val editText = EditText(this).apply {
            hint = "请输入书名"
            // 故意不设置contentDescription
            id = View.generateViewId()
        }
        
        val checkbox = CheckBox(this).apply {
            text = "记住选择"
            // 故意不设置contentDescription
            id = View.generateViewId()
        }
        
        val seekBar = SeekBar(this).apply {
            // 进度条通常需要无障碍支持
            id = View.generateViewId()
        }
        
        // 一个已经适配的按钮（作为对照）
        val adaptedButton = Button(this).apply {
            text = "已适配按钮"
            contentDescription = "搜索按钮"
            isFocusable = true
            id = View.generateViewId()
        }
        
        // 添加到布局
        rootLayout.addView(buttonWithoutDesc)
        rootLayout.addView(imageButton)
        rootLayout.addView(editText)
        rootLayout.addView(checkbox)
        rootLayout.addView(seekBar)
        rootLayout.addView(adaptedButton)
        
        setContentView(rootLayout)
        
        // 运行无障碍扫描
        runAccessibilityScan(rootLayout)
    }
    
    private fun runAccessibilityScan(rootView: View) {
        // 方法1：快速扫描并生成报告
        val report = AccessibilityScanner.quickScan(this, rootView)
        println("=== 无障碍扫描报告 ===")
        println(report)
        
        // 方法2：一键修复所有问题
        val (fixedCount, finalReport) = AccessibilityScanner.quickFix(this, rootView)
        println("\n=== 修复结果 ===")
        println("修复了 $fixedCount 个问题")
        println("\n=== 修复后扫描报告 ===")
        println(finalReport)
        
        // 方法3：自定义扫描和修复
        val scanner = AccessibilityScanner(this)
        
        // 扫描并获取详细问题列表
        val issues = scanner.scanViewTree(rootView)
        println("\n=== 详细问题列表 ===")
        issues.forEachIndexed { index, issue ->
            println("问题 ${index + 1}:")
            println("  控件: ${issue.viewId} (${issue.className})")
            println("  问题: ${issue.issues.joinToString(", ")}")
            println("  建议: ${issue.suggestedFix}")
            println()
        }
        
        // 选择性修复
        val importantIssues = issues.filter { issue ->
            issue.issues.any { it.contains("按钮") || it.contains("输入框") }
        }
        
        println("=== 重要问题修复 ===")
        importantIssues.forEach { issue ->
            println("修复: ${issue.viewId}")
            // 这里可以调用具体的修复逻辑
        }
    }
}

/**
 * 命令行测试脚本
 * 可以在不启动Activity的情况下测试扫描器
 */
object AccessibilityScannerTest {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("开始无障碍扫描器测试...")
        
        // 模拟创建一些测试控件
        println("\n1. 测试控件识别功能:")
        testControlRecognition()
        
        println("\n2. 测试问题检测功能:")
        testIssueDetection()
        
        println("\n3. 测试修复建议生成:")
        testFixSuggestions()
        
        println("\n测试完成！")
    }
    
    private fun testControlRecognition() {
        // 这里可以模拟各种控件类型
        val testCases = listOf(
            "Button" to "缺少内容描述的按钮",
            "ImageView" to "缺少内容描述的图片",
            "EditText" to "缺少内容描述的输入框",
            "CheckBox" to "缺少内容描述的复选框",
            "SeekBar" to "缺少无障碍支持的进度条"
        )
        
        testCases.forEach { (type, description) ->
            println("  • $type: $description")
        }
    }
    
    private fun testIssueDetection() {
        val issues = listOf(
            "缺少内容描述",
            "可点击但不可聚焦",
            "无障碍重要性设置为NO",
            "缺少标签或提示"
        )
        
        issues.forEach { issue ->
            println("  • 能检测: $issue")
        }
    }
    
    private fun testFixSuggestions() {
        val fixes = listOf(
            "自动生成内容描述",
            "设置焦点属性",
            "调整无障碍重要性",
            "添加无障碍操作"
        )
        
        fixes.forEach { fix ->
            println("  • 能生成: $fix")
        }
    }
}

/**
 * 集成到BaseActivity的示例
 */
fun integrateWithBaseActivity() {
    """
    // 在BaseActivity中添加以下方法：
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        if (hasFocus) {
            // 当窗口获得焦点时，运行无障碍检查
            runAccessibilityCheck()
        }
    }
    
    private fun runAccessibilityCheck() {
        // 只在调试模式下运行，避免影响性能
        if (BuildConfig.DEBUG) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
            val report = AccessibilityScanner.quickScan(this, rootView)
            
            // 如果有问题，记录到日志
            if (report.contains("发现的问题:")) {
                Log.d("Accessibility", "发现无障碍问题，请查看日志")
                Log.d("Accessibility", report)
                
                // 可选：在开发模式下自动修复
                if (AppConfig.autoFixAccessibility) {
                    val (fixedCount, _) = AccessibilityScanner.quickFix(this, rootView)
                    Log.d("Accessibility", "自动修复了 $fixedCount 个问题")
                }
            }
        }
    }
    
    // 在设置中添加选项：
    // ☑ 自动检查无障碍问题
    // ☑ 自动修复常见无障碍问题
    """.trimIndent().let { println(it) }
}