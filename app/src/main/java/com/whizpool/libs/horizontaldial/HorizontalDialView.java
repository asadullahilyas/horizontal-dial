package com.whizpool.libs.horizontaldial;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class HorizontalDialView extends RelativeLayout
{
    private static final String TAG = "HDV";

    private static final int NO_OF_EXTRA_VIEWS = 16;

    private boolean isScrolledProgrammatically = false;
    private boolean isReady = false;

    private View startButton;
    private View resetButton;
    private View endButton;

    private RecyclerView recyclerView;

    private ValueChangeListener valueChangeListener;
    private ReadyCallback readyCallback;

    public HorizontalDialView(Context context)
    {
        super(context);
        init();
    }

    public HorizontalDialView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
        processAttrs(attrs);
    }

    public HorizontalDialView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
        processAttrs(attrs);
    }

    private void init()
    {
        View view = inflate(getContext(), R.layout.view_horizontal_dial, this);
        startButton = view.findViewById(R.id.startButton);
        resetButton = view.findViewById(R.id.resetButton);
        endButton = view.findViewById(R.id.endButton);
        recyclerView = view.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void processAttrs(AttributeSet attrs)
    {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.HorizontalDialView);
        int min = ta.getInt(R.styleable.HorizontalDialView_min, 0);
        int max = ta.getInt(R.styleable.HorizontalDialView_max, 10);
        ta.recycle();

        setRange(min, max);
    }

    private float toPixel(float dp)
    {
        return dp * (getContext().getResources().getDisplayMetrics().densityDpi / 160.0F);
    }

    public boolean isReady()
    {
        return isReady;
    }

    public void setValueChangeListener(ValueChangeListener valueChangeListener)
    {
        this.valueChangeListener = valueChangeListener;
    }

    public void setReadyCallback(ReadyCallback readyCallback)
    {
        this.readyCallback = readyCallback;
    }

    public void setRange(final int min, final int max)
    {
        if (min >= max)
        {
            throw new IllegalArgumentException("min must never be greater than or equal to max.");
        }

        recyclerView.post(new Runnable()
        {
            @Override
            public void run()
            {
                final RangeAdapter adapter = new RangeAdapter(recyclerView, min, max);
                recyclerView.setAdapter(adapter);

                setPosition(getCenterPosition(), false);

                LinearSnapHelper snapHelper = new LinearSnapHelper();
                snapHelper.attachToRecyclerView(recyclerView);

                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
                {
                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState)
                    {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (valueChangeListener == null || layoutManager == null)
                        {
                            return;
                        }

                        int newPosition = layoutManager.findFirstVisibleItemPosition() + min;

                        switch (newState)
                        {
                            case RecyclerView.SCROLL_STATE_DRAGGING:
                                valueChangeListener.onStartTouch(newPosition);
                                break;

                            case RecyclerView.SCROLL_STATE_IDLE:
                                valueChangeListener.onStopTouch(newPosition);
                                break;

                            case RecyclerView.SCROLL_STATE_SETTLING:
                                break;
                        }
                    }

                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy)
                    {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (valueChangeListener == null || layoutManager == null)
                        {
                            return;
                        }

                        int currentValue = layoutManager.findFirstVisibleItemPosition() + min;

                        if (currentValue >= min && currentValue <= max)
                        {
                            valueChangeListener.onChanged(currentValue);

                            if (isScrolledProgrammatically)
                            {
                                isScrolledProgrammatically = false;
                                valueChangeListener.onStopTouch(currentValue);
                            }
                        }
                    }
                });

                startButton.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        int nextPosition = getCurrentPosition() - 1;

                        if (adapter.isGivenValueInRange(nextPosition))
                        {
                            setPosition(nextPosition, false);
                        }
                    }
                });

                resetButton.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        setPosition(getCenterPosition(), true);
                    }
                });

                endButton.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        int nextPosition = getCurrentPosition() + 1;

                        if (adapter.isGivenValueInRange(nextPosition))
                        {
                            setPosition(nextPosition, false);
                        }
                    }
                });

                isReady = true;

                if (readyCallback != null)
                {
                    readyCallback.isReady();
                }
            }
        });
    }

    public int getCenterPosition()
    {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null)
        {
            throw new NullPointerException("How on Mars did you manage to get here?");
        }

        RangeAdapter adapter = (RangeAdapter) recyclerView.getAdapter();
        if (adapter == null)
        {
            throw new NullPointerException("setRange() must be called before getCenterPosition()");
        }

        int rangeTotal = adapter.getRangeTotal();

        return (rangeTotal / 2) + adapter.min;
    }

    public int getCurrentPosition()
    {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null)
        {
            throw new NullPointerException("How on Mars did you manage to get here?");
        }

        RangeAdapter adapter = (RangeAdapter) recyclerView.getAdapter();
        if (adapter == null)
        {
            throw new NullPointerException("setRange() must be called before getCenterPosition()");
        }

        return layoutManager.findFirstVisibleItemPosition() + adapter.min;
    }

    public void setPosition(int position, boolean shouldScrollSmoothly)
    {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null)
        {
            throw new NullPointerException("How on Mars did you manage to get here?");
        }

        RangeAdapter adapter = (RangeAdapter) recyclerView.getAdapter();
        if (adapter == null)
        {
            throw new NullPointerException("setRange() must be called before setPosition()");
        }

        if (!adapter.isGivenValueInRange(position))
        {
            throw new IllegalArgumentException("position (" + position + ") was outside of range. It must be between " + adapter.min + " and " + adapter.max + ", including " + adapter.min + " and " + adapter.max);
        }

        isScrolledProgrammatically = true;

        final int positionInRecyclerView = position - adapter.min;

        final int itemWidth = adapter.leftMargin + adapter.rightMargin + adapter.itemWidth;

        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        int scrollByPosition = positionInRecyclerView - (firstVisibleItemPosition == -1 ? 0 : firstVisibleItemPosition);
        int scrollBy = scrollByPosition * itemWidth;

        if (shouldScrollSmoothly)
        {
            recyclerView.smoothScrollBy(scrollBy, 0);
        }
        else
        {
            recyclerView.scrollBy(scrollBy, 0);
        }
    }

    public interface ValueChangeListener
    {
        void onChanged(int value);

        void onStartTouch(int value);

        void onStopTouch(int value);
    }

    public interface ReadyCallback
    {
        void isReady();
    }

    private class RangeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    {
        private int min;
        private int max;

        private int itemWidth;
        private int itemHeightLongMargin;
        private int itemHeightShortMargin;

        private int rightMargin;
        private int leftMargin;

        private int itemColor = Color.parseColor("#C5C5C5");

        RangeAdapter(RecyclerView recyclerView, int min, int max)
        {
            this.min = min;
            this.max = max;

            itemWidth = (int) toPixel(1.0F);
            itemHeightLongMargin = (int) toPixel(8.0F);
            itemHeightShortMargin = (int) toPixel(13.0F);

            float margin = (recyclerView.getWidth() / (float) (NO_OF_EXTRA_VIEWS + 1)) / 2.0F;

            DecimalFormat df = new DecimalFormat("#");
            df.setRoundingMode(RoundingMode.HALF_DOWN);

            rightMargin = Integer.parseInt(df.format(margin));
            leftMargin = Integer.parseInt(df.format(margin));
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = new View(getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            params.rightMargin = rightMargin;
            params.leftMargin = leftMargin;
            view.setLayoutParams(params);

            return new RecyclerView.ViewHolder(view)
            {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
        {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();

            if (position % 4 == 0)
            {
                params.topMargin = itemHeightLongMargin;
                params.bottomMargin = itemHeightLongMargin;
            }
            else
            {
                params.topMargin = itemHeightShortMargin;
                params.bottomMargin = itemHeightShortMargin;
            }

            holder.itemView.setBackgroundColor(itemColor);
        }

        @Override
        public int getItemCount()
        {
            return getRangeTotal() + NO_OF_EXTRA_VIEWS;
        }

        public boolean isGivenValueInRange(int givenValue)
        {
            return givenValue >= min && givenValue <= max;
        }

        private int getRangeTotal()
        {
            return max - min + 1;
        }
    }
}