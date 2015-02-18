package org.akvo.flow.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionGroupTab extends ScrollView {
    private QuestionGroup mQuestionGroup;
    private QuestionInteractionListener mQuestionListener;
    private SurveyListener mSurveyListener;

    private Map<String, QuestionView> mQuestionViews;
    private LinearLayout mContainer;
    private boolean mLoaded;

    private int mRepeatCount;
    private LayoutInflater mInflater;

    public QuestionGroupTab(Context context, QuestionGroup group,  SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        super(context);
        mQuestionGroup = group;
        mSurveyListener = surveyListener;
        mQuestionListener = questionListener;
        mQuestionViews = new HashMap<>();
        mLoaded = false;
        mRepeatCount = -1;
        mInflater = LayoutInflater.from(context);
        init();
    }

    private void init() {
        // Load question group view and set it as ScrollView's child
        // FIXME: Would it make more sense to initialize this attrs in the XML file?
        mInflater.inflate(R.layout.question_group_tab, this);
        mContainer = (LinearLayout)findViewById(R.id.question_list);

        findViewById(R.id.next_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSurveyListener.nextTab();
            }
        });

        if (mQuestionGroup.isRepeatable()) {
            View repeatBtn = findViewById(R.id.repeat_btn);
            repeatBtn.setVisibility(VISIBLE);// GONE by default
            repeatBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadGroup();// TODO
                }
            });
        }

        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * Pre-load all the QuestionViews in memory. getView() will simply
     * retrieve them from the corresponding position in mQuestionViews.
     */
    public void load() {
        mLoaded = true;
        loadGroup();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // We set the ScrollView focusable in order to catch the focus when scrolling.
        // This will prevent weird behaviors when errors are present in focused
        // QuestionViews (scroll gets stuck at that position)
        requestFocus();
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public void notifyOptionsChanged() {
        for (QuestionView qv : mQuestionViews.values()) {
            qv.notifyOptionsChanged();
        }
    }

    public void onQuestionComplete(String questionId, Bundle data) {
        QuestionView qv = mQuestionViews.get(questionId);
        if (qv != null) {
            qv.questionComplete(data);
        }
    }

    /**
     * Checks to make sure the mandatory questions in this tab have a response
     */
    public List<Question> checkInvalidQuestions() {
        List<Question> missingQuestions = new ArrayList<Question>();
        for (QuestionView qv : mQuestionViews.values()) {
            qv.checkMandatory();
            if (!qv.isValid() && qv.areDependenciesSatisfied()) {
                // Only considered invalid if the dependencies are fulfilled
                missingQuestions.add(qv.getQuestion());
            }
        }
        return missingQuestions;
    }

    public void loadState() {
        Map<String, QuestionResponse> responses = mSurveyListener.getResponses();
        for (QuestionView qv : mQuestionViews.values()) {
            qv.resetQuestion(false);// Clean start
            final String questionId = qv.getQuestion().getId();
            if (responses.containsKey(questionId)) {
                final QuestionResponse response = responses.get(questionId);
                // Update the question view to reflect the loaded data
                qv.rehydrate(response);
            }
        }
    }

    public QuestionView getQuestionView(String questionId) {
        return mQuestionViews.get(questionId);
    }

    public void onPause() {
        // Propagate onPause callback
        for (QuestionView qv : mQuestionViews.values()) {
            qv.onPause();
        }
    }

    public void onResume() {
        // Propagate onResume callback
        for (QuestionView qv : mQuestionViews.values()) {
            qv.onResume();
        }
    }

    public boolean isLoaded() {
        return mLoaded;
    }

    public void loadGroup() {
        if (mQuestionGroup.isRepeatable()) {
            mRepeatCount++;
            mContainer.addView(getRepeatHeader());
        }

        final Context context = getContext();
        for (Question q : mQuestionGroup.getQuestions()) {
            QuestionView questionView;
            if (ConstantUtil.OPTION_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new OptionQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.FREE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new FreetextQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.PHOTO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new MediaQuestionView(context, q, mSurveyListener,
                        ConstantUtil.PHOTO_QUESTION_TYPE);
            } else if (ConstantUtil.VIDEO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new MediaQuestionView(context, q, mSurveyListener,
                        ConstantUtil.VIDEO_QUESTION_TYPE);
            } else if (ConstantUtil.GEO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new GeoQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.SCAN_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new BarcodeQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.DATE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new DateQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.CASCADE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new CascadeQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.GEOSHAPE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new GeoshapeQuestionView(context, q, mSurveyListener);
            } else {
                questionView = new QuestionHeaderView(context, q, mSurveyListener);
            }

            // Add question interaction listener
            questionView.addQuestionInteractionListener(mQuestionListener);

            mQuestionViews.put(q.getId(), questionView);// Store the reference to the View

            // Add divider (within the View)
            mInflater.inflate(R.layout.divider, questionView);
            mContainer.addView(questionView);
        }
    }

    private View getRepeatHeader() {
        View header = LayoutInflater.from(getContext()).inflate(R.layout.question_header, null);// TODO: Refactor
        TextView tv = (TextView)header.findViewById(R.id.question_tv);
        tv.setText(mQuestionGroup.getHeading() + " - " + mRepeatCount);
        tv.setTextColor(getResources().getColor(R.color.text_color_orange));
        header.findViewById(R.id.tip_ib).setVisibility(VISIBLE);
        ((ImageButton)header.findViewById(R.id.tip_ib)).setImageResource(R.drawable.red_cross);

        int padding = (int) PlatformUtil.dp2Pixel(getContext(), 8);
        header.setPadding(padding, padding, padding, padding);
        header.setBackgroundColor(getResources().getColor(R.color.background_alternate));

        return header;
    }

}
