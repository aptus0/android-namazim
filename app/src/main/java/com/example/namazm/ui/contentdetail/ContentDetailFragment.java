package com.example.namazm.ui.contentdetail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.namazm.R;
import com.example.namazm.data.model.FavoriteContent;
import com.example.namazm.data.repository.NamazRepository;
import com.example.namazm.data.repository.ServiceLocator;
import com.example.namazm.databinding.FragmentContentDetailBinding;
import com.google.android.material.snackbar.Snackbar;

public class ContentDetailFragment extends Fragment {

    private FragmentContentDetailBinding binding;
    private FavoriteContent content;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentContentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String id = getArguments() == null ? "" : getArguments().getString("content_id", "");
        NamazRepository repository = ServiceLocator.provideRepository();
        content = repository.getFavoriteById(id);

        binding.textTitle.setText(content.getTitle());
        binding.textBody.setText(content.getFullText());
        binding.textSource.setText(getString(R.string.hadith_source, content.getSource()));

        binding.buttonShare.setOnClickListener(v -> shareContent());
        binding.buttonCopy.setOnClickListener(v -> copyContent(v));
    }

    private void shareContent() {
        if (content == null) {
            return;
        }

        String text = content.getFullText() + "\n(" + content.getSource() + ")";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.share_hadith_chooser)));
    }

    private void copyContent(View anchor) {
        if (content == null) {
            return;
        }

        ClipboardManager manager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            return;
        }

        manager.setPrimaryClip(ClipData.newPlainText("favorite_content", content.getFullText()));
        Snackbar.make(anchor, R.string.hadith_copied, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
