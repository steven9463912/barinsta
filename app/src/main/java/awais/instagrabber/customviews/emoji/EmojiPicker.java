package awais.instagrabber.customviews.emoji;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Collection;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.utils.emoji.EmojiParser;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class EmojiPicker extends LinearLayout {
    // private static final String TAG = EmojiPicker.class.getSimpleName();

    public EmojiPicker(Context context) {
        super(context);
        this.setup();
    }

    public EmojiPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setup();
    }

    public EmojiPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setup();
    }

    private void setup() {
        this.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        this.setOrientation(LinearLayout.VERTICAL);
    }

    public void init(@NonNull View rootView,
                     OnEmojiClickListener onEmojiClickListener,
                     OnBackspaceClickListener onBackspaceClickListener) {
        TabLayout tabLayout = new TabLayout(this.getContext());
        LayoutParams tabLayoutLayoutParam = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        tabLayout.setLayoutParams(tabLayoutLayoutParam);
        tabLayout.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_TOP);
        // tabLayout.setSelectedTabIndicatorColor(Utils.getThemeAccentColor(getContext()));
        tabLayout.setSelectedTabIndicatorColor(this.getResources().getColor(R.color.blue_500));

        ViewPager2 viewPager2 = new ViewPager2(this.getContext());
        LayoutParams viewPagerLayoutParam = new LayoutParams(MATCH_PARENT, 0);
        viewPagerLayoutParam.weight = 1;
        viewPager2.setLayoutParams(viewPagerLayoutParam);
        viewPager2.setAdapter(new EmojiPickerPageAdapter(rootView, onEmojiClickListener));
        viewPager2.setOffscreenPageLimit(1);

        Context context = this.getContext();
        if (context == null) return;
        EmojiParser emojiParser = EmojiParser.Companion.getInstance(context);
        List<EmojiCategory> categories = emojiParser.getEmojiCategories();

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
            tab.view.setPadding(0, 0, 0, 0);
            EmojiCategory emojiCategory = categories.get(position);
            if (emojiCategory == null) return;
            Collection<Emoji> emojis = emojiCategory.getEmojis().values();
            if (emojis.isEmpty()) return;
            AppCompatImageView imageView = this.getImageView();
            imageView.setImageResource(emojiCategory.getDrawableRes());
            tab.setCustomView(imageView);
        }).attach();

        TabLayout.Tab tab = tabLayout.newTab();
        tab.view.setPadding(0, 0, 0, 0);
        AppCompatImageView imageView = this.getImageView();
        imageView.setImageResource(R.drawable.ic_round_backspace_24);
        TypedValue outValue = new TypedValue();
        this.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        imageView.setBackgroundResource(outValue.resourceId);
        imageView.setOnClickListener(v -> {
            if (onBackspaceClickListener == null) return;
            onBackspaceClickListener.onClick();
        });
        tab.setCustomView(imageView);
        tab.view.setEnabled(false);
        tabLayout.addTab(tab);
        this.addView(viewPager2);
        this.addView(tabLayout);
    }

    @NonNull
    private AppCompatImageView getImageView() {
        AppCompatImageView imageView = new AppCompatImageView(this.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        ImageViewCompat.setImageTintList(imageView, ContextCompat.getColorStateList(this.getContext(), R.color.emoji_picker_tab_color));
        return imageView;
    }

    public interface OnEmojiClickListener {
        void onClick(View view, Emoji emoji);
    }

    public interface OnBackspaceClickListener {
        void onClick();
    }
}
