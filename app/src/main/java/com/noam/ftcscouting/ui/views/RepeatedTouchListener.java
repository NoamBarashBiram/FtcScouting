package com.noam.ftcscouting.ui.views;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public class RepeatedTouchListener implements View.OnTouchListener {
    private final long initialRepeatDelay;
    private final long repeatInterval;
    private final View.OnClickListener listener;

    private final Handler handler = new Handler();

    private View touchedView;

    private final Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            if(touchedView.isEnabled()) {
                handler.postDelayed(this, repeatInterval);
                listener.onClick(touchedView);
            } else {
                // if the view was disabled by the clickListener, remove the callback
                handler.removeCallbacks(handlerRunnable);
                touchedView.setPressed(false);
                touchedView = null;
            }
        }
    };

    public RepeatedTouchListener(View.OnClickListener listener) {
        this.initialRepeatDelay = 500;
        this.repeatInterval = 100;
        this.listener = listener;
    }

    public RepeatedTouchListener(long initialRepeatDelay, long repeatInterval, View.OnClickListener listener) {
        this.initialRepeatDelay = initialRepeatDelay;
        this.repeatInterval = repeatInterval;
        this.listener = listener;
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialRepeatDelay);
                touchedView = v;
                touchedView.setPressed(true);
                listener.onClick(v);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                touchedView.setPressed(false);
                touchedView = null;
                return true;
        }
        return true;
    }
}
