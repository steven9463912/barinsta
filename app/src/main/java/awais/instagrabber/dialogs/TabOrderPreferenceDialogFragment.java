package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectUsersAdapter;
import awais.instagrabber.adapters.TabsAdapter;
import awais.instagrabber.adapters.viewholder.TabViewHolder;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.models.Tab;
import awais.instagrabber.utils.NavigationHelperKt;
import awais.instagrabber.utils.Utils;
import kotlin.Pair;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG;
import static androidx.recyclerview.widget.ItemTouchHelper.DOWN;
import static androidx.recyclerview.widget.ItemTouchHelper.UP;

public class TabOrderPreferenceDialogFragment extends DialogFragment {
    private Callback callback;
    private Context context;
    private List<Tab> tabsInPref;
    private ItemTouchHelper itemTouchHelper;
    private AlertDialog dialog;
    private List<Tab> newOrderTabs;
    private List<Tab> newOtherTabs;

    private final TabsAdapter.TabAdapterCallback tabAdapterCallback = new TabsAdapter.TabAdapterCallback() {
        @Override
        public void onStartDrag(TabViewHolder viewHolder) {
            if (TabOrderPreferenceDialogFragment.this.itemTouchHelper == null || viewHolder == null) return;
            TabOrderPreferenceDialogFragment.this.itemTouchHelper.startDrag(viewHolder);
        }

        @Override
        public void onOrderChange(List<Tab> newOrderTabs) {
            if (newOrderTabs == null || TabOrderPreferenceDialogFragment.this.tabsInPref == null || TabOrderPreferenceDialogFragment.this.dialog == null) return;
            TabOrderPreferenceDialogFragment.this.newOrderTabs = newOrderTabs;
            this.setSaveButtonState(newOrderTabs);
        }

        @Override
        public void onAdd(Tab tab) {
            // Add this tab to newOrderTabs
            TabOrderPreferenceDialogFragment.this.newOrderTabs = ImmutableList.<Tab>builder()
                    .addAll(TabOrderPreferenceDialogFragment.this.newOrderTabs)
                    .add(tab)
                    .build();
            // Remove this tab from newOtherTabs
            if (TabOrderPreferenceDialogFragment.this.newOtherTabs != null) {
                TabOrderPreferenceDialogFragment.this.newOtherTabs = TabOrderPreferenceDialogFragment.this.newOtherTabs.stream()
                                           .filter(t -> !t.equals(tab))
                                           .collect(Collectors.toList());
            }
            this.setSaveButtonState(TabOrderPreferenceDialogFragment.this.newOrderTabs);
            // submit these tab lists to adapter
            if (TabOrderPreferenceDialogFragment.this.adapter == null) return;
            TabOrderPreferenceDialogFragment.this.adapter.submitList(TabOrderPreferenceDialogFragment.this.newOrderTabs, TabOrderPreferenceDialogFragment.this.newOtherTabs, () -> TabOrderPreferenceDialogFragment.this.list.postDelayed(() -> TabOrderPreferenceDialogFragment.this.adapter.notifyDataSetChanged(), 300));
        }

        @Override
        public void onRemove(Tab tab) {
            // Remove this tab from newOrderTabs
            TabOrderPreferenceDialogFragment.this.newOrderTabs = TabOrderPreferenceDialogFragment.this.newOrderTabs.stream()
                                       .filter(t -> !t.equals(tab))
                                       .collect(Collectors.toList());
            // Add this tab to newOtherTabs
            if (TabOrderPreferenceDialogFragment.this.newOtherTabs != null) {
                TabOrderPreferenceDialogFragment.this.newOtherTabs = ImmutableList.<Tab>builder()
                        .addAll(TabOrderPreferenceDialogFragment.this.newOtherTabs)
                        .add(tab)
                        .build();
            }
            this.setSaveButtonState(TabOrderPreferenceDialogFragment.this.newOrderTabs);
            // submit these tab lists to adapter
            if (TabOrderPreferenceDialogFragment.this.adapter == null) return;
            TabOrderPreferenceDialogFragment.this.adapter.submitList(TabOrderPreferenceDialogFragment.this.newOrderTabs, TabOrderPreferenceDialogFragment.this.newOtherTabs, () -> TabOrderPreferenceDialogFragment.this.list.postDelayed(() -> {
                TabOrderPreferenceDialogFragment.this.adapter.notifyDataSetChanged();
                if (tab.getNavigationRootId() == R.id.direct_messages_nav_graph) {
                    ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                            111, 0, R.string.dm_remove_warning, R.string.ok, 0, 0
                    );
                    dialogFragment.show(TabOrderPreferenceDialogFragment.this.getChildFragmentManager(), "dm_warning_dialog");
                }
            }, 500));
        }

        private void setSaveButtonState(List<Tab> newOrderTabs) {
            TabOrderPreferenceDialogFragment.this.dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                  .setEnabled(!newOrderTabs.equals(TabOrderPreferenceDialogFragment.this.tabsInPref));
        }
    };
    private final ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(UP | DOWN, 0) {
        private int movePosition = RecyclerView.NO_POSITION;

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof DirectUsersAdapter.HeaderViewHolder) return 0;
            if (viewHolder instanceof TabViewHolder && !((TabViewHolder) viewHolder).isDraggable()) return 0;
            return super.getMovementFlags(recyclerView, viewHolder);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c,
                                @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX,
                                float dY,
                                int actionState,
                                boolean isCurrentlyActive) {
            if (actionState != ACTION_STATE_DRAG) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                return;
            }
            TabsAdapter adapter = (TabsAdapter) recyclerView.getAdapter();
            if (adapter == null) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                return;
            }
            // Do not allow dragging into 'Other tabs' category
            float edgeY = dY;
            int lastPosition = adapter.getCurrentCount() - 1;
            View view = viewHolder.itemView;
            // final int topEdge = recyclerView.getTop();
            int bottomEdge = view.getHeight() * adapter.getCurrentCount() - view.getBottom();
            // if (movePosition == 0 && dY < topEdge) {
            //     edgeY = topEdge;
            // } else
            if (this.movePosition >= lastPosition && dY >= bottomEdge) {
                edgeY = bottomEdge;
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, edgeY, actionState, isCurrentlyActive);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            TabsAdapter adapter = (TabsAdapter) recyclerView.getAdapter();
            if (adapter == null) return false;
            this.movePosition = target.getBindingAdapterPosition();
            if (this.movePosition >= adapter.getCurrentCount()) {
                return false;
            }
            int from = viewHolder.getBindingAdapterPosition();
            int to = target.getBindingAdapterPosition();
            adapter.moveItem(from, to);
            // adapter.notifyItemMoved(from, to);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (!(viewHolder instanceof TabViewHolder)) {
                this.movePosition = RecyclerView.NO_POSITION;
                return;
            }
            if (actionState == ACTION_STATE_DRAG) {
                ((TabViewHolder) viewHolder).setDragging(true);
                this.movePosition = viewHolder.getBindingAdapterPosition();
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            ((TabViewHolder) viewHolder).setDragging(false);
            this.movePosition = RecyclerView.NO_POSITION;
        }
    };
    private TabsAdapter adapter;
    private RecyclerView list;

    public static TabOrderPreferenceDialogFragment newInstance() {
        Bundle args = new Bundle();
        TabOrderPreferenceDialogFragment fragment = new TabOrderPreferenceDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public TabOrderPreferenceDialogFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            this.callback = (Callback) this.getParentFragment();
        } catch (final ClassCastException e) {
            // throw new ClassCastException("Calling fragment must implement TabOrderPreferenceDialogFragment.Callback interface");
        }
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(this.context)
                .setView(this.createView())
                .setPositiveButton(R.string.save, (d, w) -> {
                    boolean hasChanged = this.newOrderTabs != null && !this.newOrderTabs.equals(this.tabsInPref);
                    if (hasChanged) {
                        this.saveNewOrder();
                    }
                    if (this.callback == null) return;
                    this.callback.onSave(hasChanged);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (this.callback == null) return;
                    this.callback.onCancel();
                })
                .create();
    }

    private void saveNewOrder() {
        String newOrderString = this.newOrderTabs
                .stream()
                .map(tab -> NavigationHelperKt.getNavGraphNameForNavRootId(tab.getNavigationRootId()))
                .collect(Collectors.joining(","));
        Utils.settingsHelper.putString(PreferenceKeys.PREF_TAB_ORDER, newOrderString);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = this.getDialog();
        if (!(dialog instanceof AlertDialog)) return;
        this.dialog = (AlertDialog) dialog;
        this.dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }

    @NonNull
    private View createView() {
        this.list = new RecyclerView(this.context);
        this.list.setLayoutManager(new LinearLayoutManager(this.context));
        this.itemTouchHelper = new ItemTouchHelper(this.simpleCallback);
        this.itemTouchHelper.attachToRecyclerView(this.list);
        this.adapter = new TabsAdapter(this.tabAdapterCallback);
        this.list.setAdapter(this.adapter);
        Pair<List<Tab>, List<Tab>> navTabListPair = NavigationHelperKt.getLoggedInNavTabs(this.context);
        this.tabsInPref = navTabListPair.getFirst();
        // initially set newOrderTabs and newOtherTabs same as current tabs
        this.newOrderTabs = navTabListPair.getFirst();
        this.newOtherTabs = navTabListPair.getSecond();
        this.adapter.submitList(navTabListPair.getFirst(), navTabListPair.getSecond());
        return this.list;
    }

    public interface Callback {
        void onSave(boolean orderHasChanged);

        void onCancel();
    }
}
