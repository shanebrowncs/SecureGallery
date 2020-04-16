package ca.shanebrown.securegallery.util;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.shanebrown.securegallery.R;

public class ImageGridRecyclerAdapter extends RecyclerView.Adapter<ImageGridRecyclerAdapter.ImageGridRecyclerViewHolder> {
    private ImagePathBundle[] images;
    private final SparseBooleanArray selected;
    private final Context context;

    private final OnImageClickListener listener;

    public ImageGridRecyclerAdapter(ImagePathBundle[] images, Context context, OnImageClickListener listener) {
        this.images = images;
        this.context = context;
        this.listener = listener;

        selected = new SparseBooleanArray();
    }

    @NonNull
    @Override
    public ImageGridRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View customView = inflater.inflate(R.layout.grid_recycler_item, parent, false);
        return new ImageGridRecyclerViewHolder(customView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageGridRecyclerViewHolder holder, int position) {
        holder.imageView.setImageBitmap(this.images[position].getImage());
        holder.imageView.invalidate();

        // Set image overlay to indicate selection
        if(isSelected(position)){
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                holder.imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.border_selected));
            }else{
                holder.imageView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            }
        }else{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                holder.imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.border));
            }else{
                holder.imageView.clearColorFilter();
            }
        }
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    /**
     * Refresh entire recycler image set
     * @param images New image dataset
     */
    public void refreshImages(ImagePathBundle[] images){
        this.images = images;
        this.notifyDataSetChanged();
    }

    /**
     * Refresh entire recycler image set
     * @param images New image dataset
     * @param notifyAdapter Notify adapter of dataset change, default true
     */
    public void refreshImages(ImagePathBundle[] images, boolean notifyAdapter){
        this.images = images;
        if(notifyAdapter)
            this.notifyDataSetChanged();
    }

    /**
     * Get list of selected image indices
     * @return List of indices
     */
    public List<Integer> getSelectedItems(){
        List<Integer> items = new ArrayList<>(selected.size());
        for(int i = 0; i < selected.size(); i++){
            items.add(selected.keyAt(i));
        }

        return items;
    }

    /**
     * Get amount of selected images
     * @return Number of selected items
     */
    public int getSelectedItemCount(){
        return selected.size();
    }

    /**
     * Is adapter dataset position selected
     * @param position Position to check
     * @return Selection state
     */
    boolean isSelected(int position){
        return getSelectedItems().contains(position);
    }

    /**
     * Clears all items from selection
     */
    public void clearSelection(){
        List<Integer> selection = getSelectedItems();
        selected.clear();
        for(Integer i : selection){
            notifyItemChanged(i);
        }
    }

    /**
     * Toggle selection on/off for given position
     * @param position Adapter dataset position to toggle
     */
    public void toggleSelection(int position){
        if(selected.get(position, false)){
            selected.delete(position);
        }else{
            selected.put(position, true);
        }

        notifyItemChanged(position);
    }

    /**
     * Selects all items in adapter dataset
     */
    public void selectAll(){
        for(int i = 0; i < getItemCount(); i++){
            selected.put(i, true);
            notifyItemChanged(i);
        }
    }

    public static class ImageGridRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener {
        private final ImageView imageView;
        final OnImageClickListener listener;
        ImageGridRecyclerViewHolder(View v, OnImageClickListener listener) {
            super(v);
            imageView = v.findViewById(R.id.grid_image_view);

            this.listener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onImageClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            listener.onImageLongClick(getAdapterPosition());

            return false;
        }
    }

    public interface OnImageClickListener{
        void onImageClick(int position);
        void onImageLongClick(int position);
    }
}