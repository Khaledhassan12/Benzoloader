package khaled.benzo.ui.site

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import khaled.benzo.data.model.SiteProfile
import khaled.benzo.databinding.ItemMediaDetectedBinding

class SiteAdapter(
    private val sites: MutableList<SiteProfile>,
    private val onDeleteClick: (SiteProfile) -> Unit
) : RecyclerView.Adapter<SiteAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaDetectedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sites[position])
    }

    override fun getItemCount(): Int = sites.size

    fun updateList(newList: List<SiteProfile>) {
        sites.clear()
        sites.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemMediaDetectedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(site: SiteProfile) {
            binding.txtMediaTitle.text = site.domain
            binding.txtMediaUrl.text = site.username
            binding.btnDownloadThisMedia.text = "Delete"
            binding.btnDownloadThisMedia.setOnClickListener {
                onDeleteClick(site)
            }
        }
    }
}
