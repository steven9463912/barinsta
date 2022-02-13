package awais.instagrabber.adapters.viewholder.directmessages;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmActionLogBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemActionLog;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.TextRange;
import awais.instagrabber.utils.TextUtils;

public class DirectItemActionLogViewHolder extends DirectItemViewHolder {
    private static final String TAG = DirectItemActionLogViewHolder.class.getSimpleName();

    private final LayoutDmActionLogBinding binding;

    public DirectItemActionLogViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                         LayoutDmActionLogBinding binding,
                                         User currentUser,
                                         DirectThread thread,
                                         DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.setItemView(binding.getRoot());
        binding.tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void bindItem(DirectItem directItemModel, MessageDirection messageDirection) {
        DirectItemActionLog actionLog = directItemModel.getActionLog();
        String text = actionLog.getDescription();
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        List<TextRange> bold = actionLog.getBold();
        if (bold != null && !bold.isEmpty()) {
            for (TextRange textRange : bold) {
                StyleSpan boldStyleSpan = new StyleSpan(Typeface.BOLD);
                sb.setSpan(boldStyleSpan, textRange.getStart(), textRange.getEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
        List<TextRange> textAttributes = actionLog.getTextAttributes();
        if (textAttributes != null && !textAttributes.isEmpty()) {
            for (TextRange textAttribute : textAttributes) {
                if (!TextUtils.isEmpty(textAttribute.getColor())) {
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(this.itemView.getResources().getColor(R.color.deep_orange_400));
                    sb.setSpan(colorSpan, textAttribute.getStart(), textAttribute.getEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (!TextUtils.isEmpty(textAttribute.getIntent())) {
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            DirectItemActionLogViewHolder.this.handleDeepLink(textAttribute.getIntent());
                        }
                    };
                    sb.setSpan(clickableSpan, textAttribute.getStart(), textAttribute.getEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        }
        this.binding.tvMessage.setText(sb);
    }

    @Override
    protected boolean allowMessageDirectionGravity() {
        return false;
    }

    @Override
    protected boolean showUserDetailsInGroup() {
        return false;
    }

    @Override
    protected boolean showMessageInfo() {
        return false;
    }

    @Override
    protected boolean allowLongClick() {
        return false;
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }
}
