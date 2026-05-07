package com.example.kitty;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kitty.databinding.ItemAppBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private final List<ApplicationInfo> apps;
    private final PackageManager packageManager;
    private final Set<String> selectedPackages = new HashSet<>();

    public AppAdapter(List<ApplicationInfo> apps, PackageManager packageManager) {
        this.apps = apps;
        this.packageManager = packageManager;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppBinding binding = ItemAppBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AppViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ApplicationInfo appInfo = apps.get(position);
        holder.binding.appName.setText(appInfo.loadLabel(packageManager));
        holder.binding.appIcon.setImageDrawable(appInfo.loadIcon(packageManager));
        
        holder.binding.appCheckBox.setOnCheckedChangeListener(null);
        holder.binding.appCheckBox.setChecked(selectedPackages.contains(appInfo.packageName));
        
        holder.binding.appCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPackages.add(appInfo.packageName);
            } else {
                selectedPackages.remove(appInfo.packageName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public Set<String> getSelectedPackages() {
        return selectedPackages;
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ItemAppBinding binding;

        AppViewHolder(ItemAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
