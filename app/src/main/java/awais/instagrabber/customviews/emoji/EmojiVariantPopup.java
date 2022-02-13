package awais.instagrabber.customviews.emoji;

/*
 * Copyright (C) 2016 - Niklas Baudy, Ruben Gees, Mario Đanić and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.databinding.ItemEmojiGridBinding;
import awais.instagrabber.databinding.LayoutEmojiVariantPopupBinding;
import awais.instagrabber.utils.AppExecutors;

import static android.view.View.MeasureSpec.makeMeasureSpec;

public final class EmojiVariantPopup {
    private static final int DO_NOT_UPDATE_FLAG = -1;

    private final View rootView;
    private final EmojiPicker.OnEmojiClickListener listener;

    private PopupWindow popupWindow;
    private View rootImageView;
    private final EmojiVariantManager emojiVariantManager;
    private final AppExecutors appExecutors;

    public EmojiVariantPopup(@NonNull final View rootView,
                             final EmojiPicker.OnEmojiClickListener listener) {
        this.rootView = rootView;
        this.listener = listener;
        this.emojiVariantManager = EmojiVariantManager.getInstance();
        this.appExecutors = AppExecutors.INSTANCE;
    }

    public void show(@NonNull View view, @NonNull Emoji emoji) {
        this.dismiss();

        this.rootImageView = view;

        View content = this.initView(view.getContext(), emoji, view.getWidth());

        this.popupWindow = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.popupWindow.setFocusable(true);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        this.popupWindow.setBackgroundDrawable(new BitmapDrawable(view.getContext().getResources(), (Bitmap) null));

        content.measure(makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        Point location = this.locationOnScreen(view);
        Point desiredLocation = new Point(
                location.x - content.getMeasuredWidth() / 2 + view.getWidth() / 2,
                location.y - content.getMeasuredHeight()
        );

        this.popupWindow.showAtLocation(this.rootView, Gravity.NO_GRAVITY, desiredLocation.x, desiredLocation.y);
        this.rootImageView.getParent().requestDisallowInterceptTouchEvent(true);
        this.fixPopupLocation(this.popupWindow, desiredLocation);
    }

    public void dismiss() {
        this.rootImageView = null;

        if (this.popupWindow != null) {
            this.popupWindow.dismiss();
            this.popupWindow = null;
        }
    }

    private View initView(@NonNull Context context, @NonNull Emoji emoji, int width) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        LayoutEmojiVariantPopupBinding binding = LayoutEmojiVariantPopupBinding.inflate(layoutInflater, null, false);
        List<Emoji> variants = new ArrayList<>(emoji.getVariants());
        // Add parent at start of list
        // variants.add(0, emoji);
        for (Emoji variant : variants) {
            ItemEmojiGridBinding itemBinding = ItemEmojiGridBinding.inflate(layoutInflater, binding.container, false);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) itemBinding.image.getLayoutParams();
            // Use the same size for Emojis as in the picker.
            layoutParams.width = width;
            itemBinding.image.setImageDrawable(variant.getDrawable());
            itemBinding.image.setOnClickListener(view -> {
                if (this.listener != null) {
                    if (!variant.getUnicode().equals(this.emojiVariantManager.getVariant(emoji.getUnicode()))) {
                        this.emojiVariantManager.setVariant(emoji.getUnicode(), variant.getUnicode());
                    }
                    this.listener.onClick(view, variant);
                }
                this.dismiss();
            });
            binding.container.addView(itemBinding.getRoot());
        }
        return binding.getRoot();
    }

    @NonNull
    private Point locationOnScreen(@NonNull View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Point(location[0], location[1]);
    }

    private void fixPopupLocation(@NonNull PopupWindow popupWindow, @NonNull Point desiredLocation) {
        popupWindow.getContentView().post(() -> {
            Point actualLocation = this.locationOnScreen(popupWindow.getContentView());

            if (!(actualLocation.x == desiredLocation.x && actualLocation.y == desiredLocation.y)) {
                int differenceX = actualLocation.x - desiredLocation.x;
                int differenceY = actualLocation.y - desiredLocation.y;

                int fixedOffsetX;
                int fixedOffsetY;

                if (actualLocation.x > desiredLocation.x) {
                    fixedOffsetX = desiredLocation.x - differenceX;
                } else {
                    fixedOffsetX = desiredLocation.x + differenceX;
                }

                if (actualLocation.y > desiredLocation.y) {
                    fixedOffsetY = desiredLocation.y - differenceY;
                } else {
                    fixedOffsetY = desiredLocation.y + differenceY;
                }

                popupWindow.update(fixedOffsetX, fixedOffsetY, EmojiVariantPopup.DO_NOT_UPDATE_FLAG, EmojiVariantPopup.DO_NOT_UPDATE_FLAG);
            }
        });
    }
}

