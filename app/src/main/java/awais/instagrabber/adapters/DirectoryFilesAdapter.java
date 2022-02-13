package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ItemDirListBinding;

public final class DirectoryFilesAdapter extends ListAdapter<File, DirectoryFilesAdapter.ViewHolder> {
    private final OnFileClickListener onFileClickListener;

    private static final DiffUtil.ItemCallback<File> DIFF_CALLBACK = new DiffUtil.ItemCallback<File>() {
        @Override
        public boolean areItemsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath());
        }
    };

    public DirectoryFilesAdapter(OnFileClickListener onFileClickListener) {
        super(DirectoryFilesAdapter.DIFF_CALLBACK);
        this.onFileClickListener = onFileClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemDirListBinding binding = ItemDirListBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = this.getItem(position);
        holder.bind(file, this.onFileClickListener);
    }

    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDirListBinding binding;

        private ViewHolder(ItemDirListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(File file, OnFileClickListener onFileClickListener) {
            if (file == null) return;
            if (onFileClickListener != null) {
                this.itemView.setOnClickListener(v -> onFileClickListener.onFileClick(file));
            }
            this.binding.text.setText(file.getName());
            if (file.isDirectory()) {
                this.binding.icon.setImageResource(R.drawable.ic_folder_24);
                return;
            }
            this.binding.icon.setImageResource(R.drawable.ic_file_24);
        }
    }
}
