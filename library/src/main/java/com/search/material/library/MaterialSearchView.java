package com.search.material.library;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class MaterialSearchView extends FrameLayout {
    public static final int REQUEST_VOICE = 9999;

    private MenuItem mMenuItem;
    private boolean mIsSearchOpen = false;
    private boolean mClearingFocus;

    //Views
    private View mSearchLayout;
    private View mTintView;
    private CardView cvItems;
    private RecyclerView rvItems;
    private EditText etSearch;
    private ImageButton btnBack;
    private ImageButton btnVoice;
    private ImageButton btnClear;
    private RelativeLayout mSearchTopBar;
    private ProgressBar progressBar;

    private CharSequence mOldQueryText;
    private CharSequence mUserQuery;

    private SearchView.OnQueryTextListener mOnQueryChangeListener;
    private SearchViewListener mSearchViewListener;

    private RecyclerView.Adapter adapter;

    private SavedState mSavedState;

    private Context mContext;

    private boolean shouldAnimate;

    public MaterialSearchView(Context context) {
        this(context, null);
    }

    public MaterialSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        mContext = context;

        shouldAnimate = true;

        initiateView();

        initStyle(attrs, defStyleAttr);
    }

    private void initStyle(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.MaterialSearchView, defStyleAttr, 0);

        if (a != null) {
            if (a.hasValue(R.styleable.MaterialSearchView_searchBackground)) {
                setBackground(a.getDrawable(R.styleable.MaterialSearchView_searchBackground));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_android_textColor)) {
                setTextColor(a.getColor(R.styleable.MaterialSearchView_android_textColor, 0));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_android_textColorHint)) {
                setHintTextColor(a.getColor(R.styleable.MaterialSearchView_android_textColorHint, 0));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_android_hint)) {
                setHint(a.getString(R.styleable.MaterialSearchView_android_hint));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_searchVoiceIcon)) {
                setVoiceIcon(a.getDrawable(R.styleable.MaterialSearchView_searchVoiceIcon));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_searchCloseIcon)) {
                setCloseIcon(a.getDrawable(R.styleable.MaterialSearchView_searchCloseIcon));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_searchBackIcon)) {
                setBackIcon(a.getDrawable(R.styleable.MaterialSearchView_searchBackIcon));
            }

            if (a.hasValue(R.styleable.MaterialSearchView_searchSuggestionBackground)) {
                setSuggestionBackground(a.getDrawable(R.styleable.MaterialSearchView_searchSuggestionBackground));
            }

            a.recycle();
        }
    }

    private void initiateView() {
        LayoutInflater.from(mContext).inflate(R.layout.search_view, this, true);
        mSearchLayout = findViewById(R.id.search_layout);

        mSearchTopBar = (RelativeLayout) mSearchLayout.findViewById(R.id.searchTopBar);
        rvItems = (RecyclerView) mSearchLayout.findViewById(R.id.rvItems);
        etSearch = (EditText) mSearchLayout.findViewById(R.id.etSearch);
        btnBack = (ImageButton) mSearchLayout.findViewById(R.id.btnBack);
        btnVoice = (ImageButton) mSearchLayout.findViewById(R.id.btnVoice);
        btnClear = (ImageButton) mSearchLayout.findViewById(R.id.btnClear);
        mTintView = mSearchLayout.findViewById(R.id.transparentView);
        progressBar = (ProgressBar) mSearchLayout.findViewById(R.id.progressBar);

        etSearch.setOnClickListener(mOnClickListener);
        btnBack.setOnClickListener(mOnClickListener);
        btnVoice.setOnClickListener(mOnClickListener);
        btnClear.setOnClickListener(mOnClickListener);
        mTintView.setOnClickListener(mOnClickListener);

        showVoice(true);

        initSearchView();

        cvItems.setVisibility(GONE);
    }

    private void initSearchView() {
        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                onSubmitQuery();
                return true;
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mUserQuery = s;
                MaterialSearchView.this.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etSearch.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(etSearch);
                    showSuggestions();
                }
            }
        });
    }

    public void showProgress() {
        progressBar.setVisibility(VISIBLE);
        btnClear.setVisibility(INVISIBLE);
    }

    public void hideProgress() {
        progressBar.setVisibility(INVISIBLE);
        invalidateClearAndVoiceBtnVisibility();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {

        public void onClick(View v) {
            if (v == btnBack) {
                closeSearch();
            } else if (v == btnVoice) {
                onVoiceClicked();
            } else if (v == btnClear) {
                etSearch.setText(null);
            } else if (v == etSearch) {
                showSuggestions();
            } else if (v == mTintView) {
                closeSearch();
            }
        }
    };

    private void onVoiceClicked() {
        mSearchViewListener.onVoiceClicked();
    }

    private void onTextChanged(CharSequence newText) {
        mUserQuery = etSearch.getText();
        invalidateClearAndVoiceBtnVisibility();

        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    private void invalidateClearAndVoiceBtnVisibility() {
        boolean hasText = !TextUtils.isEmpty(etSearch.getText());
        if (hasText) {
            btnClear.setVisibility(VISIBLE);
            showVoice(false);
        } else {
            btnClear.setVisibility(GONE);
            showVoice(true);
        }
    }

    private void onSubmitQuery() {
        CharSequence query = etSearch.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                closeSearch();
                etSearch.setText(null);
            }
        }
    }

    private boolean isVoiceAvailable() {
        if (isInEditMode())
            return false;
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showKeyboard(View view) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1 && view.hasFocus()) {
            view.clearFocus();
        }
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }



    @Override
    public void setBackground(Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mSearchTopBar.setBackground(background);
        } else {
            mSearchTopBar.setBackgroundDrawable(background);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        mSearchTopBar.setBackgroundColor(color);
    }

    public void setTextColor(int color) {
        etSearch.setTextColor(color);
    }

    public void setHintTextColor(int color) {
        etSearch.setHintTextColor(color);
    }

    public void setHint(CharSequence hint) {
        etSearch.setHint(hint);
    }

    public void setVoiceIcon(Drawable drawable) {
        btnVoice.setImageDrawable(drawable);
    }

    public void setCloseIcon(Drawable drawable) {
        btnClear.setImageDrawable(drawable);
    }

    public void setBackIcon(Drawable drawable) {
        btnBack.setImageDrawable(drawable);
    }

    public void setSuggestionBackground(Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rvItems.setBackground(background);
        } else {
            rvItems.setBackgroundDrawable(background);
        }
    }

    public void setShouldAnimate(boolean animate) {
        shouldAnimate = animate;
    }



    /**
     * Call this method to show suggestions list.
     * This shows up when adapter is set.
     * Call {@link #setAdapter(RecyclerView.Adapter)} before calling this.
     */
    public void showSuggestions() {
        if (adapter != null && adapter.getItemCount() > 0 && cvItems.getVisibility() == GONE) {
            cvItems.setVisibility(VISIBLE);
        }
    }


    /**
     * Set Adapter for suggestions list. Should implement Filterable.
     *
     * @param adapter
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
        rvItems.setAdapter(adapter);
    }

    /**
     * Dissmiss the suggestions list.
     */
    public void dismissSuggestions() {
        if (cvItems.getVisibility() == VISIBLE) {
            cvItems.setVisibility(GONE);
        }
    }


    /**
     * Calling this will set the query to search text box. if submit is true, it'll submit the query.
     *
     * @param query
     * @param submit
     */
    public void setQuery(CharSequence query, boolean submit) {
        etSearch.setText(query);
        if (query != null) {
            etSearch.setSelection(etSearch.length());
            mUserQuery = query;
        }
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /**
     * if show is true, this will enable voice search. If voice is not available on the device, this method call has not effect.
     *
     * @param show
     */
    public void showVoice(boolean show) {
        if (show && isVoiceAvailable()) {
            btnVoice.setVisibility(VISIBLE);
        } else {
            btnVoice.setVisibility(GONE);
        }
    }

    /**
     * Call this method and pass the menu item so this class can handle click events for the Menu Item.
     *
     * @param menuItem
     */
    public void setMenuItem(MenuItem menuItem) {
        this.mMenuItem = menuItem;
        mMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showSearch();
                return true;
            }
        });
    }

    /**
     * Return true if search is open
     *
     * @return
     */
    public boolean isSearchOpen() {
        return mIsSearchOpen;
    }

    /**
     * Open Search View. This will animate the showing of the view.
     */
    public void showSearch() {
        if (shouldAnimate) {
            showSearch(true);
        }
        else {
            showSearch(false);
        }
    }

    /**
     * Open Search View. if animate is true, Animate the showing of the view.
     *
     * @param animate
     */
    public void showSearch(boolean animate) {
        if (isSearchOpen()) {
            return;
        }

        //Request Focus
        etSearch.setText(null);
        etSearch.requestFocus();

        if (animate) {
            AnimationUtil.fadeInView(mSearchLayout, AnimationUtil.ANIMATION_DURATION_SHORT, new AnimationUtil.AnimationListener() {
                @Override
                public boolean onAnimationStart(View view) {
                    return false;
                }

                @Override
                public boolean onAnimationEnd(View view) {
                    if (mSearchViewListener != null) {
                        mSearchViewListener.onSearchViewShown();
                    }
                    return false;
                }

                @Override
                public boolean onAnimationCancel(View view) {
                    return false;
                }
            });
        } else {
            mSearchLayout.setVisibility(VISIBLE);
            if (mSearchViewListener != null) {
                mSearchViewListener.onSearchViewShown();
            }
        }
        mIsSearchOpen = true;
    }

    /**
     * Close search view.
     */
    public void closeSearch() {
        if (!isSearchOpen()) {
            return;
        }

        etSearch.setText(null);
        dismissSuggestions();
        clearFocus();

        mSearchLayout.setVisibility(GONE);
        if (mSearchViewListener != null) {
            mSearchViewListener.onSearchViewClosed();
        }
        mIsSearchOpen = false;

    }

    /**
     * Set this listener to listen to Query Change events.
     *
     * @param listener
     */
    public void setOnQueryTextListener(SearchView.OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Set this listener to listen to Search View open and close events
     *
     * @param listener
     */
    public void setOnSearchViewListener(SearchViewListener listener) {
        mSearchViewListener = listener;
    }


    public void update() {
        if (adapter.getItemCount() > 0) {
            showSuggestions();
        } else {
            dismissSuggestions();
        }
    }

    /**
     * @hide
     */
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        return etSearch.requestFocus(direction, previouslyFocusedRect);
    }

    /**
     * @hide
     */
    @Override
    public void clearFocus() {
        mClearingFocus = true;
        hideKeyboard(this);
        super.clearFocus();
        etSearch.clearFocus();
        mClearingFocus = false;
    }


    @Override
    public Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        mSavedState = new SavedState(superState);
        //end
        mSavedState.query = mUserQuery != null ? mUserQuery.toString() : null;
        mSavedState.isSearchOpen = this.mIsSearchOpen;

        return mSavedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        mSavedState = (SavedState) state;

        if (mSavedState.isSearchOpen) {
            showSearch(false);
            setQuery(mSavedState.query, false);
        }

        super.onRestoreInstanceState(mSavedState.getSuperState());
    }

    static class SavedState extends BaseSavedState {
        String query;
        boolean isSearchOpen;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.query = in.readString();
            this.isSearchOpen = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(query);
            out.writeInt(isSearchOpen ? 1 : 0);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }


    public interface SearchViewListener {
        void onSearchViewShown();

        void onSearchViewClosed();

        void onVoiceClicked();
    }

}
