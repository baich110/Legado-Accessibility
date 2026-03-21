package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.databinding.DialogSpeakEngineBinding
import io.legado.app.databinding.ItemTtsEngineListBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.ReadAloud
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 语音引擎选择对话框 - 无障碍适配版
 * 提供两个下拉框：系统语音和第三方语音引擎
 * 集成第三方引擎管理界面，支持无障碍操作
 */
class SpeakEngineDialog : BaseDialogFragment(R.layout.dialog_speak_engine) {

    private val binding by viewBinding(DialogSpeakEngineBinding::bind)
    private val viewModel: SpeakEngineViewModel by viewModels()
    
    private var selectedSysEngine: String? = null  // 系统引擎名称
    private var selectedHttpTtsId: Long? = null    // 第三方引擎ID
    
    private var sysEngineAdapter: ArrayAdapter<String>? = null
    private var httpTtsAdapter: HttpTtsSelectAdapter? = null
    private var httpTtsListAdapter: HttpTtsListAdapter? = null
    
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        loadCurrentSelection()
        loadHttpTtsList()
    }

    private fun initView() {
        // 初始化系统语音下拉框
        setupSysTtsSpinner()
        
        // 初始化第三方语音下拉框
        setupHttpTtsSpinner()
        
        // 初始化第三方引擎列表
        setupHttpTtsList()
        
        // 添加按钮
        binding.btnAddEngine.setOnClickListener {
            showDialogFragment<HttpTtsEditDialog>()
        }
        
        // 导入默认按钮
        binding.btnImportDefault.setOnClickListener {
            viewModel.importDefault()
            toastOnUi(getString(R.string.engine_added))
        }
    }

    /**
     * 设置系统语音下拉框
     */
    private fun setupSysTtsSpinner() {
        val sysEngines = viewModel.sysEngines
        val engineNames = mutableListOf(getString(R.string.system_default))
        engineNames.addAll(sysEngines.map { it.label })
        
        sysEngineAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            engineNames
        )
        
        binding.spinnerSysTts.apply {
            setAdapter(sysEngineAdapter)
            setOnItemClickListener { _, _, position, _ ->
                selectedSysEngine = if (position == 0) {
                    null
                } else {
                    sysEngines.getOrNull(position - 1)?.name
                }
                // 选择系统语音时，清空第三方选择
                if (selectedSysEngine != null) {
                    binding.spinnerHttpTts.setText(getString(R.string.none), false)
                    selectedHttpTtsId = null
                }
                updateTtsEngine()
            }
        }
    }

    /**
     * 设置第三方语音引擎下拉框（用于选择）
     */
    private fun setupHttpTtsSpinner() {
        httpTtsAdapter = HttpTtsSelectAdapter(requireContext(), mutableListOf())
        binding.spinnerHttpTts.setAdapter(httpTtsAdapter)
        
        binding.spinnerHttpTts.setOnItemClickListener { _, _, position, _ ->
            val httpTts = httpTtsAdapter?.getItem(position)
            if (httpTts != null && httpTts.id != -1L) {
                selectedHttpTtsId = httpTts.id
                // 选择第三方语音时，清空系统选择
                binding.spinnerSysTts.setText(getString(R.string.none), false)
                selectedSysEngine = null
            } else {
                selectedHttpTtsId = null
            }
            updateTtsEngine()
        }
    }

    /**
     * 设置第三方引擎列表（用于管理）
     */
    private fun setupHttpTtsList() {
        httpTtsListAdapter = HttpTtsListAdapter(
            requireContext(),
            onEditClick = { httpTts ->
                showDialogFragment(HttpTtsEditDialog(httpTts.id))
            },
            onDeleteClick = { httpTts ->
                showDeleteConfirmDialog(httpTts)
            }
        )
        
        binding.rvHttpTtsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = httpTtsListAdapter
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(httpTts: HttpTTS) {
        alert(R.string.delete_confirm_title, getString(R.string.delete_confirm_message, httpTts.name)) {
            positiveButton(R.string.delete) {
                deleteHttpTts(httpTts)
            }
            negativeButton(R.string.cancel, null)
        }.show()
    }

    /**
     * 删除第三方引擎
     */
    private fun deleteHttpTts(httpTts: HttpTTS) {
        lifecycleScope.launch {
            appDb.httpTTSDao.delete(httpTts)
            // 如果删除的是当前选中的，清空选择
            if (selectedHttpTtsId == httpTts.id) {
                selectedHttpTtsId = null
                binding.spinnerHttpTts.setText(getString(R.string.none), false)
                updateTtsEngine()
            }
            toastOnUi(getString(R.string.engine_deleted))
        }
    }

    /**
     * 加载当前选择状态
     */
    private fun loadCurrentSelection() {
        val ttsEngine = ReadAloud.ttsEngine
        
        when {
            ttsEngine.isNullOrEmpty() -> {
                binding.spinnerSysTts.setText(getString(R.string.system_default), false)
                selectedSysEngine = null
                selectedHttpTtsId = null
            }
            ttsEngine.isJsonObject() -> {
                val item = GSON.fromJsonObject<SelectItem<String>>(ttsEngine).getOrNull()
                if (item != null) {
                    if (item.value.isNullOrEmpty()) {
                        binding.spinnerSysTts.setText(getString(R.string.system_default), false)
                    } else {
                        selectedSysEngine = item.value
                        binding.spinnerSysTts.setText(item.title, false)
                    }
                }
                selectedHttpTtsId = null
            }
            else -> {
                val id = ttsEngine.toLongOrNull()
                if (id != null) {
                    selectedHttpTtsId = id
                    lifecycleScope.launch {
                        val name = appDb.httpTTSDao.getName(id)
                        if (name != null) {
                            binding.spinnerHttpTts.setText(name, false)
                        } else {
                            binding.spinnerHttpTts.setText(getString(R.string.none), false)
                            selectedHttpTtsId = null
                        }
                    }
                }
                selectedSysEngine = null
            }
        }
    }

    /**
     * 加载第三方语音引擎列表
     */
    private fun loadHttpTtsList() {
        lifecycleScope.launch {
            appDb.httpTTSDao.flowAll()
                .catch { }
                .flowOn(IO)
                .collect { list ->
                    httpTtsAdapter?.updateData(list)
                    httpTtsListAdapter?.updateData(list)
                    // 更新空状态
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvHttpTtsList.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
        }
    }

    /**
     * 更新TTS引擎配置
     */
    private fun updateTtsEngine() {
        val ttsEngine = when {
            selectedSysEngine != null -> {
                val engine = viewModel.sysEngines.find { it.name == selectedSysEngine }
                GSON.toJson(SelectItem(engine?.label ?: selectedSysEngine, selectedSysEngine))
            }
            selectedHttpTtsId != null -> {
                selectedHttpTtsId.toString()
            }
            else -> {
                null
            }
        }
        
        ReadAloud.ttsEngine = ttsEngine
        AppConfig.ttsEngine = ttsEngine
    }

    /**
     * 第三方语音引擎选择适配器（用于下拉框）
     */
    inner class HttpTtsSelectAdapter(
        context: Context,
        private val items: MutableList<HttpTTS>
    ) : ArrayAdapter<HttpTTS>(context, android.R.layout.simple_dropdown_item_1line, items) {
        
        init {
            add(getNoneItem())
        }
        
        private fun getNoneItem(): HttpTTS {
            return HttpTTS().apply {
                id = -1L
                name = getString(R.string.none)
            }
        }
        
        fun updateData(newItems: List<HttpTTS>) {
            items.clear()
            items.add(getNoneItem())
            items.addAll(newItems)
            notifyDataSetChanged()
        }
        
        override fun getItem(position: Int): HttpTTS? {
            return if (position in 0 until count) items[position] else null
        }
    }

    /**
     * 第三方引擎列表适配器（用于管理界面）
     * 无障碍友好的RecyclerView适配器
     */
    inner class HttpTtsListAdapter(
        private val context: Context,
        private val onEditClick: (HttpTTS) -> Unit,
        private val onDeleteClick: (HttpTTS) -> Unit
    ) : RecyclerView.Adapter<HttpTtsListAdapter.ViewHolder>() {
        
        private val items = mutableListOf<HttpTTS>()
        
        fun updateData(newItems: List<HttpTTS>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemTtsEngineListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun getItemCount(): Int = items.size
        
        inner class ViewHolder(
            private val binding: ItemTtsEngineListBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(item: HttpTTS) {
                binding.tvEngineName.text = item.name
                
                // 无障碍：设置整行内容的描述，清晰告知用户这是哪个引擎以及可执行的操作
                binding.root.contentDescription = context.getString(
                    R.string.http_tts_item_desc, item.name
                )
                
                binding.btnEdit.setOnClickListener {
                    onEditClick(item)
                }
                
                binding.btnDelete.setOnClickListener {
                    onDeleteClick(item)
                }
            }
        }
    }

    companion object {
        const val TAG = "SpeakEngineDialog"
    }
}
