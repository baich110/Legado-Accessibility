package io.legado.app.ui.book.read.config

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.appDb
import io.legado.app.databinding.DialogTtsVoiceSelectorBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 语音合成 - 语音选择器（重构版）
 * 
 * 设计特点：
 * 1. 使用进度条选择语音类型/参数，替代传统列表
 * 2. 添加试听按钮，即时反馈
 * 3. 精简界面控件，提高空间利用率
 * 4. 优化无障碍支持
 */
class TtsVoiceSelectorDialog : BaseDialogFragment() {
    
    private val binding by viewBinding(DialogTtsVoiceSelectorBinding::inflate)
    
    // 语音参数
    private var currentVoice: Voice? = null
    private var currentPitch = 1.0f
    private var currentSpeed = 1.0f
    private var availableVoices = emptyList<Voice>()
    
    // TTS引擎
    private var ttsEngine: TextToSpeech? = null
    
    // 试听文本
    private val previewTexts = listOf(
        "欢迎使用Legado阅读器",
        "这是一个语音合成测试",
        "当前语音效果试听",
        "您可以调整音调和语速"
    )
    private var previewIndex = 0
    
    companion object {
        fun show(ttsEngine: TextToSpeech): TtsVoiceSelectorDialog {
            return TtsVoiceSelectorDialog().apply {
                this.ttsEngine = ttsEngine
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initData()
        initView()
        setupAccessibility()
    }
    
    private fun initData() {
        ttsEngine?.let { engine ->
            // 获取可用语音列表
            availableVoices = engine.voices
                .filter { !it.name.contains("") && it.locale != null }
                .sortedBy { it.name }
            
            // 获取当前设置
            currentVoice = engine.voice
            currentPitch = engine.getPitch()
            currentSpeed = engine.getSpeechRate()
        }
    }
    
    private fun initView() {
        // 设置对话框大小
        dialog?.setLayout(0.9f, 0.7f)
        
        // 标题
        binding.title.text = getString(R.string.tts_voice_select)
        
        // 1. 语音类型选择（进度条）
        setupVoiceTypeSelector()
        
        // 2. 音调调节（进度条）
        setupPitchSelector()
        
        // 3. 语速调节（进度条）
        setupSpeedSelector()
        
        // 4. 试听按钮
        setupPreviewButton()
        
        // 5. 保存按钮
        setupSaveButton()
        
        // 6. 关闭按钮
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        // 更新初始状态
        updateVoiceInfo()
    }
    
    /**
     * 语音类型选择器（进度条实现）
     */
    private fun setupVoiceTypeSelector() {
        binding.voiceTypeLabel.text = getString(R.string.tts_voice_type)
        
        // 设置进度条范围
        binding.voiceTypeSeek.max = availableVoices.size - 1
        
        // 查找当前语音的索引
        val currentIndex = availableVoices.indexOfFirst { it == currentVoice }
        if (currentIndex >= 0) {
            binding.voiceTypeSeek.progress = currentIndex
        }
        
        // 进度条变化监听
        binding.voiceTypeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && progress < availableVoices.size) {
                    currentVoice = availableVoices[progress]
                    updateVoiceInfo()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 添加左右按钮用于微调
        binding.btnVoicePrev.setOnClickListener {
            val newProgress = maxOf(0, binding.voiceTypeSeek.progress - 1)
            binding.voiceTypeSeek.progress = newProgress
            currentVoice = availableVoices[newProgress]
            updateVoiceInfo()
        }
        
        binding.btnVoiceNext.setOnClickListener {
            val newProgress = minOf(availableVoices.size - 1, binding.voiceTypeSeek.progress + 1)
            binding.voiceTypeSeek.progress = newProgress
            currentVoice = availableVoices[newProgress]
            updateVoiceInfo()
        }
    }
    
    /**
     * 音调节选择器
     */
    private fun setupPitchSelector() {
        binding.pitchLabel.text = getString(R.string.tts_pitch)
        
        // 音调范围：0.5 - 2.0，步进0.1
        binding.pitchSeek.max = 15  // (2.0 - 0.5) / 0.1 = 15
        
        // 计算当前进度
        val pitchProgress = ((currentPitch - 0.5f) / 0.1f).toInt()
        binding.pitchSeek.progress = pitchProgress.coerceIn(0, 15)
        
        binding.pitchSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPitch = 0.5f + progress * 0.1f
                    updatePitchValue()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 显示当前值
        updatePitchValue()
    }
    
    /**
     * 语速选择器
     */
    private fun setupSpeedSelector() {
        binding.speedLabel.text = getString(R.string.tts_speed)
        
        // 语速范围：0.5 - 2.0，步进0.1
        binding.speedSeek.max = 15  // (2.0 - 0.5) / 0.1 = 15
        
        // 计算当前进度
        val speedProgress = ((currentSpeed - 0.5f) / 0.1f).toInt()
        binding.speedSeek.progress = speedProgress.coerceIn(0, 15)
        
        binding.speedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentSpeed = 0.5f + progress * 0.1f
                    updateSpeedValue()
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 显示当前值
        updateSpeedValue()
    }
    
    /**
     * 试听按钮设置
     */
    private fun setupPreviewButton() {
        binding.btnPreview.text = getString(R.string.tts_preview)
        binding.btnPreview.setOnClickListener {
            playPreview()
        }
        
        // 长按切换试听文本
        binding.btnPreview.setOnLongClickListener {
            previewIndex = (previewIndex + 1) % previewTexts.size
            showToast("试听文本已切换")
            true
        }
    }
    
    /**
     * 保存按钮设置
     */
    private fun setupSaveButton() {
        binding.btnSave.text = getString(R.string.save)
        binding.btnSave.setOnClickListener {
            saveSettings()
            dismiss()
        }
    }
    
    /**
     * 更新语音信息显示
     */
    private fun updateVoiceInfo() {
        currentVoice?.let { voice ->
            binding.voiceInfo.text = String.format(
                "%s (%s)",
                voice.name.replace("_", " "),
                voice.locale?.displayLanguage ?: "未知"
            )
            
            // 显示语音特性
            val features = mutableListOf<String>()
            if (voice.locale?.country != null) {
                features.add(voice.locale.country)
            }
            if (voice.quality == TextToSpeech.Engine.KEY_FEATURE_QUALITY_HIGH) {
                features.add("高质量")
            }
            
            if (features.isNotEmpty()) {
                binding.voiceFeatures.text = features.joinToString(" • ")
                binding.voiceFeatures.visibility = View.VISIBLE
            } else {
                binding.voiceFeatures.visibility = View.GONE
            }
        } ?: run {
            binding.voiceInfo.text = getString(R.string.tts_voice_not_selected)
            binding.voiceFeatures.visibility = View.GONE
        }
    }
    
    private fun updatePitchValue() {
        binding.pitchValue.text = String.format("%.1f", currentPitch)
    }
    
    private fun updateSpeedValue() {
        binding.speedValue.text = String.format("%.1f", currentSpeed)
    }
    
    /**
     * 播放试听
     */
    private fun playPreview() {
        val text = previewTexts[previewIndex]
        
        ttsEngine?.let { engine ->
            // 应用当前设置
            currentVoice?.let { voice ->
                engine.voice = voice
            }
            engine.setPitch(currentPitch)
            engine.setSpeechRate(currentSpeed)
            
            // 播放试听
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
            
            // 显示正在试听状态
            binding.btnPreview.text = getString(R.string.tts_playing)
            binding.btnPreview.isEnabled = false
            
            // 3秒后恢复按钮状态
            binding.btnPreview.postDelayed({
                binding.btnPreview.text = getString(R.string.tts_preview)
                binding.btnPreview.isEnabled = true
            }, 3000)
        } ?: run {
            showToast(getString(R.string.tts_engine_not_available))
        }
    }
    
    /**
     * 保存设置
     */
    private fun saveSettings() {
        launch {
            withContext(Dispatchers.IO) {
                // 保存到数据库
                currentVoice?.let { voice ->
                    appDb.ttsDao().updateVoice(voice.name)
                }
                
                // 保存音调和语速
                AppConfig.ttsPitch = currentPitch
                AppConfig.ttsSpeechRate = currentSpeed
                
                // 应用设置到TTS引擎
                ttsEngine?.let { engine ->
                    currentVoice?.let { voice ->
                        engine.voice = voice
                    }
                    engine.setPitch(currentPitch)
                    engine.setSpeechRate(currentSpeed)
                }
            }
            
            showToast(getString(R.string.save_success))
        }
    }
    
    /**
     * 设置无障碍支持
     */
    private fun setupAccessibility() {
        // 为进度条添加无障碍描述
        ViewCompat.replaceAccessibilityAction(
            binding.voiceTypeSeek,
            AccessibilityNodeInfoCompat.ACTION_SET_PROGRESS,
            getString(R.string.a11y_adjust_voice_type),
            null
        )
        
        ViewCompat.replaceAccessibilityAction(
            binding.pitchSeek,
            AccessibilityNodeInfoCompat.ACTION_SET_PROGRESS,
            getString(R.string.a11y_adjust_pitch),
            null
        )
        
        ViewCompat.replaceAccessibilityAction(
            binding.speedSeek,
            AccessibilityNodeInfoCompat.ACTION_SET_PROGRESS,
            getString(R.string.a11y_adjust_speed),
            null
        )
        
        // 为试听按钮添加详细描述
        binding.btnPreview.contentDescription = 
            getString(R.string.tts_preview) + "，长按切换试听文本"
        
        // 设置内容描述
        binding.voiceInfo.contentDescription = 
            getString(R.string.a11y_current_voice) + ": " + binding.voiceInfo.text
        
        // 设置焦点顺序
        val focusOrder = listOf(
            binding.voiceTypeSeek,
            binding.btnVoicePrev,
            binding.btnVoiceNext,
            binding.pitchSeek,
            binding.speedSeek,
            binding.btnPreview,
            binding.btnSave,
            binding.btnClose
        )
        
        focusOrder.forEachIndexed { index, view ->
            view.nextFocusForwardId = if (index < focusOrder.size - 1) {
                focusOrder[index + 1].id
            } else {
                focusOrder[0].id
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止TTS
        ttsEngine?.stop()
    }
}

/**
 * 无障碍字符串资源（需要添加到strings.xml）
 * 
 * 添加以下字符串：
 * <string name="tts_voice_select">语音选择</string>
 * <string name="tts_voice_type">语音类型</string>
 * <string name="tts_pitch">音调</string>
 * <string name="tts_speed">语速</string>
 * <string name="tts_preview">试听</string>
 * <string name="tts_playing">正在播放…</string>
 * <string name="tts_voice_not_selected">未选择语音</string>
 * <string name="tts_engine_not_available">TTS引擎不可用</string>
 * <string name="a11y_adjust_voice_type">调整语音类型</string>
 * <string name="a11y_adjust_pitch">调整音调</string>
 * <string name="a11y_adjust_speed">调整语速</string>
 * <string name="a11y_current_voice">当前语音</string>
 */