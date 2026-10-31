package khaled.benzo.ui.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import khaled.benzo.core.sniffer.DetectedMedia
import khaled.benzo.databinding.DialogMediaSnifferBinding
import khaled.benzo.databinding.ItemMediaDetectedBinding

class MediaSnifferDialog(
    private val mediaList: List<DetectedMedia>,
    private val onMediaSelected: (DetectedMedia) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogMediaSnifferBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMediaSnifferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewMedia.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMedia.adapter = MediaAdapter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MediaAdapter : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemMediaDetectedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(mediaList[position])
        }

        override fun getItemCount(): Int = mediaList.size

        inner class ViewHolder(private val itemBinding: ItemMediaDetectedBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(item: DetectedMedia) {
                itemBinding.txtMediaTitle.text = item.title
                itemBinding.txtMediaUrl.text = item.url

                itemBinding.btnDownloadThisMedia.setOnClickListener {
                    onMediaSelected(item)
                    dismiss()
                }
            }
        }
    }
}
